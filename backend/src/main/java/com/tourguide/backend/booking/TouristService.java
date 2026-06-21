package com.tourguide.backend.booking;

import com.tourguide.backend.api.dto.AnnouncementView;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.api.dto.SessionView;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Tourist-facing browse + booking. Real WeChat Pay replaces mock-pay in MIN-27. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TouristService {

    private static final String OPEN = "OPEN";
    private static final String GROUP = "GROUP";

    private final ScenicSessionRepository sessionRepo;
    private final AnnouncementRepository announcementRepo;
    private final BookingOrderRepository orderRepo;
    private final GroupBuyRepository groupBuyRepo;
    private final GroupBuyService groupBuyService;

    /** Scenic-wide cancellation policy (settable, not per-order): FREE or FEE. */
    @Value("${app.order.cancel-policy:FREE}")
    private String cancelPolicy;

    @Value("${app.order.cancel-fee-percent:20}")
    private int cancelFeePercent;

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
        if (GROUP.equals(session.getType())) {
            // 拼团: atomically claim seats (no oversell, locks at full). Throws if full/closed.
            GroupBuy group = groupBuyService.getOrCreate(session);
            groupBuyService.claim(group.getId(), req.peopleCount());
        } else if (req.peopleCount() > remaining(session)) {
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

    /** Tourist self-cancels an order: releases any 拼团 seat and refunds per the cancel policy. */
    @Transactional
    public OrderView cancelOrder(long userId, long orderId) {
        BookingOrder order = ownedOrder(userId, orderId);
        String status = order.getStatus();
        if (!"PENDING_PAYMENT".equals(status) && !"PAID".equals(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单当前状态不可取消");
        }
        if (GROUP.equals(order.getType())) {
            groupBuyService.release(order.getSessionId(), order.getPeopleCount());
        }
        if ("PAID".equals(status)) {
            long refund = "FEE".equalsIgnoreCase(cancelPolicy)
                    ? order.getAmountFen() * (100L - cancelFeePercent) / 100
                    : order.getAmountFen();
            log.info("order {} cancelled: refund {} fen (policy={})", order.getId(), refund, cancelPolicy);
            order.setStatus("REFUNDED");
        } else {
            order.setStatus("CANCELLED");
        }
        return toOrderView(orderRepo.save(order), null);
    }

    @Transactional(readOnly = true)
    public List<OrderView> listMyOrders(long userId) {
        return orderRepo.findByUserIdOrderByIdDesc(userId).stream()
                .map(o -> toOrderView(o, null))
                .toList();
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
        if (GROUP.equals(session.getType())) {
            // GROUP capacity is owned by the group-buy row (authoritative seat count).
            return groupBuyRepo.findBySessionId(session.getId())
                    .map(g -> Math.max(0, g.getMaxSize() - g.getCurrentSize()))
                    .orElseGet(() -> session.getCapacity() != null ? session.getCapacity() : 0);
        }
        int cap = session.getCapacity() != null ? session.getCapacity() : 0;
        return Math.max(0, cap - orderRepo.sumBookedPeople(session.getId()));
    }

    private SessionView toSessionView(ScenicSession s) {
        Integer joined = null;
        String groupStatus = null;
        if (GROUP.equals(s.getType())) {
            GroupBuy g = groupBuyRepo.findBySessionId(s.getId()).orElse(null);
            if (g != null) {
                joined = g.getCurrentSize();
                groupStatus = g.getStatus();
            }
        }
        return new SessionView(
                s.getId(), s.getTitle(), s.getType(),
                String.valueOf(s.getSessionDate()),
                s.getStartTime() != null ? s.getStartTime().toString() : null,
                s.getEndTime() != null ? s.getEndTime().toString() : null,
                s.getCapacity() != null ? s.getCapacity() : 0,
                remaining(s),
                s.getPriceFen() != null ? s.getPriceFen() : 0,
                joined, groupStatus);
    }

    private OrderView toOrderView(BookingOrder o, ScenicSession known) {
        ScenicSession s = known != null ? known : sessionRepo.findById(o.getSessionId()).orElse(null);
        return new OrderView(
                o.getId(), o.getOrderNo(), o.getType(), o.getPeopleCount(),
                o.getAmountFen() != null ? o.getAmountFen() : 0,
                o.getStatus(), o.getVerifyCode(), o.getSessionId(),
                s != null ? s.getTitle() : null,
                o.getVisitDate() != null ? o.getVisitDate().toString() : null);
    }

    private String generateOrderNo() {
        return "TG" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String generateVerifyCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
