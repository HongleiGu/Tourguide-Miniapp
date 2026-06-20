package com.tourguide.backend.booking;

import com.tourguide.backend.api.dto.AnnouncementView;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.api.dto.SessionView;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Tourist-facing browse + booking. Real WeChat Pay replaces mock-pay in MIN-27. */
@Service
@RequiredArgsConstructor
public class TouristService {

    private static final String OPEN = "OPEN";

    private final ScenicSessionRepository sessionRepo;
    private final AnnouncementRepository announcementRepo;
    private final BookingOrderRepository orderRepo;

    @Transactional(readOnly = true)
    public List<SessionView> listSessions() {
        return sessionRepo.findByStatusOrderBySessionDateAscStartTimeAsc(OPEN).stream()
                .map(this::toSessionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementView> listAnnouncements() {
        return announcementRepo.findByActiveTrueOrderByIdDesc().stream()
                .map(a -> new AnnouncementView(a.getId(), a.getTitle(), a.getContent(), a.getType()))
                .toList();
    }

    @Transactional
    public OrderView createOrder(long userId, CreateOrderRequest req) {
        ScenicSession session = sessionRepo.findById(req.sessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "场次不存在"));
        if (!OPEN.equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该场次不可预约");
        }
        int remaining = remaining(session);
        if (req.peopleCount() > remaining) {
            throw new BusinessException(ErrorCode.CONFLICT, "剩余名额不足");
        }

        BookingOrder order = new BookingOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setSessionId(session.getId());
        order.setGuideId(session.getGuideId());
        order.setType(session.getType());
        order.setPeopleCount(req.peopleCount());
        order.setContactName(req.contactName());
        order.setContactPhone(req.contactPhone());
        order.setVisitDate(req.visitDate() != null ? req.visitDate() : session.getSessionDate());
        order.setAmountFen(session.getPriceFen() * req.peopleCount());
        order.setStatus("PENDING_PAYMENT");
        return toOrderView(orderRepo.save(order), session);
    }

    /** Dev-only stand-in for WeChat Pay: mark the order paid and issue a 核销码. */
    @Transactional
    public OrderView mockPay(long userId, long orderId) {
        BookingOrder order = ownedOrder(userId, orderId);
        if (!"PAID".equals(order.getStatus())) {
            order.setStatus("PAID");
            order.setPaidAt(Instant.now());
            order.setVerifyCode(generateVerifyCode());
            orderRepo.save(order);
        }
        return toOrderView(order, null);
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(long userId, long orderId) {
        return toOrderView(ownedOrder(userId, orderId), null);
    }

    private BookingOrder ownedOrder(long userId, long orderId) {
        BookingOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        return order;
    }

    private int remaining(ScenicSession session) {
        int cap = session.getCapacity() != null ? session.getCapacity() : 0;
        return Math.max(0, cap - orderRepo.sumBookedPeople(session.getId()));
    }

    private SessionView toSessionView(ScenicSession s) {
        return new SessionView(
                s.getId(), s.getTitle(), s.getType(),
                String.valueOf(s.getSessionDate()),
                s.getStartTime() != null ? s.getStartTime().toString() : null,
                s.getEndTime() != null ? s.getEndTime().toString() : null,
                s.getCapacity() != null ? s.getCapacity() : 0,
                remaining(s),
                s.getPriceFen() != null ? s.getPriceFen() : 0);
    }

    private OrderView toOrderView(BookingOrder o, ScenicSession known) {
        ScenicSession s = known != null ? known : sessionRepo.findById(o.getSessionId()).orElse(null);
        return new OrderView(
                o.getId(), o.getOrderNo(), o.getType(), o.getPeopleCount(),
                o.getAmountFen() != null ? o.getAmountFen() : 0,
                o.getStatus(), o.getVerifyCode(), o.getSessionId(),
                s != null ? s.getTitle() : null);
    }

    private String generateOrderNo() {
        return "TG" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String generateVerifyCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
