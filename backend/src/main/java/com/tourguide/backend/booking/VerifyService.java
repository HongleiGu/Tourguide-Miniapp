package com.tourguide.backend.booking;

import com.tourguide.backend.api.dto.VerifyResult;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 核销 (verification): a guide validates a tourist's 核销码 exactly once and completes the order. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final BookingOrderRepository orderRepo;
    private final OrderVerificationRepository verificationRepo;
    private final ScenicSessionRepository sessionRepo;

    @Transactional
    public VerifyResult verify(long guideUserId, String code) {
        BookingOrder order = orderRepo.findByVerifyCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "无效的核销码"));
        if (!"PAID".equals(order.getStatus())) {
            // Already COMPLETED (= 已核销) or otherwise not in a verifiable state.
            throw new BusinessException(ErrorCode.CONFLICT,
                    "COMPLETED".equals(order.getStatus()) ? "该订单已核销，不可重复核销" : "订单不可核销");
        }
        if (verificationRepo.findByOrderId(order.getId()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "该订单已核销，不可重复核销");
        }

        Instant now = Instant.now();
        OrderVerification record = new OrderVerification();
        record.setOrderId(order.getId());
        record.setGuideId(guideUserId);
        record.setVerifiedAt(now);
        try {
            verificationRepo.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            // uk_verification_order backstop against a concurrent double-scan.
            throw new BusinessException(ErrorCode.CONFLICT, "该订单已核销，不可重复核销");
        }
        order.setStatus("COMPLETED");
        orderRepo.save(order);
        log.info("order {} 核销 by user {}", order.getId(), guideUserId);

        String title = sessionRepo.findById(order.getSessionId()).map(ScenicSession::getTitle).orElse(null);
        return new VerifyResult(order.getId(), order.getOrderNo(), title,
                order.getPeopleCount(), order.getStatus(), now.toString());
    }
}
