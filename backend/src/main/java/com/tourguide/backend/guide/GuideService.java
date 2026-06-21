package com.tourguide.backend.guide;

import com.tourguide.backend.api.dto.AdminGuideView;
import com.tourguide.backend.api.dto.GuideIncome;
import com.tourguide.backend.api.dto.GuideMe;
import com.tourguide.backend.api.dto.GuideOrderView;
import com.tourguide.backend.api.dto.GuideWorkbench;
import com.tourguide.backend.api.dto.ScheduleSegment;
import com.tourguide.backend.booking.BookingOrder;
import com.tourguide.backend.booking.BookingOrderRepository;
import com.tourguide.backend.booking.ScenicSession;
import com.tourguide.backend.booking.ScenicSessionRepository;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/** Guide-facing workbench services (MIN-6). */
@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideProfileRepository guideRepo;
    private final GuideScheduleRepository scheduleRepo;
    private final AppUserRepository userRepo;
    private final BookingOrderRepository orderRepo;
    private final ScenicSessionRepository sessionRepo;

    /** The guide_profile for an app-user, or 403 if the account is not a guide. */
    public GuideProfile requireProfile(long userId) {
        return guideRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "非讲解员账号"));
    }

    @Transactional(readOnly = true)
    public GuideMe me(long userId) {
        GuideProfile p = requireProfile(userId);
        String name = userRepo.findById(p.getUserId()).map(AppUser::getNickname).orElse(null);
        return new GuideMe(p.getId(), name, p.getEmploymentType(),
                Boolean.TRUE.equals(p.getAcceptingOrders()), p.getStatus(),
                p.getRating() != null ? p.getRating().doubleValue() : 0,
                p.getStarLevel() != null ? p.getStarLevel() : 0);
    }

    /** Admin (MIN-43): set a guide's 派单权重. */
    @Transactional
    public AdminGuideView setDispatchWeight(long guideId, int weight) {
        GuideProfile p = guideRepo.findById(guideId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "讲解员不存在"));
        p.setDispatchWeight(weight);
        return adminView(guideRepo.save(p));
    }

    /** Admin (MIN-43): 暂停/恢复接单权限 (人工判断). SUSPENDED guides are ineligible for dispatch/grab. */
    @Transactional
    public AdminGuideView setSuspended(long guideId, boolean suspended) {
        GuideProfile p = guideRepo.findById(guideId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "讲解员不存在"));
        p.setStatus(suspended ? "SUSPENDED" : "ENABLED");
        return adminView(guideRepo.save(p));
    }

    private AdminGuideView adminView(GuideProfile p) {
        String name = userRepo.findById(p.getUserId()).map(AppUser::getNickname).orElse(null);
        return new AdminGuideView(p.getId(), name, p.getEmploymentType(), p.getStatus(),
                Boolean.TRUE.equals(p.getAcceptingOrders()),
                p.getDispatchWeight() != null ? p.getDispatchWeight() : 0,
                p.getRating() != null ? p.getRating().doubleValue() : 0,
                p.getStarLevel() != null ? p.getStarLevel() : 0);
    }

    /** Today's board for the logged-in guide: counts, schedule, and remaining capacity. */
    @Transactional(readOnly = true)
    public GuideWorkbench workbench(long userId) {
        GuideProfile p = requireProfile(userId);
        long gid = p.getId();
        LocalDate today = LocalDate.now();

        int pending = orderRepo.countByGuideIdAndVisitDateAndStatus(gid, today, "PENDING_PAYMENT");
        int toVerify = orderRepo.countByGuideIdAndVisitDateAndStatus(gid, today, "PAID");
        int completed = orderRepo.countByGuideIdAndVisitDateAndStatus(gid, today, "COMPLETED");

        List<GuideSchedule> schedule = scheduleRepo.findByGuideIdAndWorkDateOrderByStartTimeAsc(gid, today);
        boolean onDuty = schedule.stream().anyMatch(s -> "WORK".equals(s.getType()));
        boolean accepting = Boolean.TRUE.equals(p.getAcceptingOrders());
        int remaining = (accepting && onDuty) ? remainingCapacityToday(gid, today) : 0;

        List<ScheduleSegment> segments = schedule.stream().map(this::toSegment).toList();

        return new GuideWorkbench(today.toString(), accepting, onDuty,
                pending, toVerify, completed, remaining, segments);
    }

    /** 开启/关闭接单. */
    @Transactional
    public GuideMe setAccepting(long userId, boolean accepting) {
        GuideProfile p = requireProfile(userId);
        p.setAcceptingOrders(accepting);
        guideRepo.save(p);
        return me(userId);
    }

    /** 排班 between two dates (inclusive). */
    @Transactional(readOnly = true)
    public List<ScheduleSegment> schedule(long userId, LocalDate from, LocalDate to) {
        GuideProfile p = requireProfile(userId);
        return scheduleRepo.findByGuideIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(p.getId(), from, to)
                .stream().map(this::toSegment).toList();
    }

    /**
     * Whether a guide may take an order at a given time: enabled + accepting + within a WORK
     * window and not in a REST window. Reused by MIN-7 dispatch (排班时段外自动限制接单).
     */
    @Transactional(readOnly = true)
    public boolean isAcceptingAt(long guideId, LocalDate date, LocalTime time) {
        GuideProfile p = guideRepo.findById(guideId).orElse(null);
        if (p == null || !Boolean.TRUE.equals(p.getAcceptingOrders()) || !"ENABLED".equals(p.getStatus())) {
            return false;
        }
        List<GuideSchedule> day = scheduleRepo.findByGuideIdAndWorkDateOrderByStartTimeAsc(guideId, date);
        boolean inRest = day.stream().anyMatch(s -> "REST".equals(s.getType()) && covers(s, time));
        if (inRest) {
            return false;
        }
        return day.stream().anyMatch(s -> "WORK".equals(s.getType()) && covers(s, time));
    }

    private boolean covers(GuideSchedule s, LocalTime t) {
        LocalTime start = s.getStartTime();
        LocalTime end = s.getEndTime();
        if (start == null && end == null) {
            return true; // all-day segment
        }
        if (start != null && t.isBefore(start)) {
            return false;
        }
        return end == null || !t.isAfter(end);
    }

    private ScheduleSegment toSegment(GuideSchedule s) {
        return new ScheduleSegment(s.getId(), s.getWorkDate().toString(), s.getType(),
                time(s.getStartTime()), time(s.getEndTime()));
    }

    /** Orders assigned to the logged-in guide (newest first), optionally filtered by status. */
    @Transactional(readOnly = true)
    public List<GuideOrderView> listOrders(long userId, String status) {
        GuideProfile p = requireProfile(userId);
        return orderRepo.findByGuideIdOrderByIdDesc(p.getId()).stream()
                .filter(o -> status == null || status.isBlank() || status.equals(o.getStatus()))
                .map(this::toGuideOrderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuideOrderView getOrder(long userId, long orderId) {
        GuideProfile p = requireProfile(userId);
        BookingOrder o = orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (!Objects.equals(o.getGuideId(), p.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        return toGuideOrderView(o);
    }

    /** Gross income (订单数 + 金额总额 + 明细) for PAID/COMPLETED orders. 线上不做分佣. */
    @Transactional(readOnly = true)
    public GuideIncome income(long userId) {
        GuideProfile p = requireProfile(userId);
        List<BookingOrder> orders = orderRepo.findByGuideIdOrderByIdDesc(p.getId()).stream()
                .filter(o -> "PAID".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus()))
                .toList();
        long total = orders.stream().mapToLong(o -> o.getAmountFen() != null ? o.getAmountFen() : 0).sum();
        List<GuideIncome.Item> items = orders.stream().map(o -> {
            ScenicSession s = sessionRepo.findById(o.getSessionId()).orElse(null);
            return new GuideIncome.Item(o.getId(), o.getOrderNo(),
                    s != null ? s.getTitle() : null,
                    s != null ? String.valueOf(s.getSessionDate()) : String.valueOf(o.getVisitDate()),
                    o.getAmountFen() != null ? o.getAmountFen() : 0, o.getStatus());
        }).toList();
        return new GuideIncome(items.size(), total, items);
    }

    /** 抢单 pool: unassigned active orders this guide is eligible (on-duty) to serve. */
    @Transactional(readOnly = true)
    public List<GuideOrderView> pool(long userId) {
        GuideProfile p = requireProfile(userId);
        return orderRepo.findByGuideIdIsNullAndStatusInOrderByIdDesc(List.of("PENDING_PAYMENT", "PAID")).stream()
                .filter(o -> eligibleForOrder(p.getId(), o))
                .map(this::toGuideOrderView)
                .toList();
    }

    /** Claim an unassigned order. Guarded UPDATE => exactly one guide wins under contention. */
    @Transactional
    public GuideOrderView grab(long userId, long orderId) {
        GuideProfile p = requireProfile(userId);
        BookingOrder o = orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (!eligibleForOrder(p.getId(), o)) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前不在可接单时段或已关闭接单");
        }
        if (orderRepo.grab(orderId, p.getId()) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单已被抢或不可抢");
        }
        return toGuideOrderView(orderRepo.findById(orderId).orElseThrow());
    }

    private boolean eligibleForOrder(long guideId, BookingOrder o) {
        ScenicSession s = sessionRepo.findById(o.getSessionId()).orElse(null);
        if (s == null || !"OPEN".equals(s.getStatus())) {
            return false;
        }
        LocalTime time = s.getStartTime() != null ? s.getStartTime() : LocalTime.MIN;
        LocalDate date = o.getVisitDate() != null ? o.getVisitDate() : s.getSessionDate();
        return isAcceptingAt(guideId, date, time);
    }

    private GuideOrderView toGuideOrderView(BookingOrder o) {
        ScenicSession s = sessionRepo.findById(o.getSessionId()).orElse(null);
        return new GuideOrderView(
                o.getId(), o.getOrderNo(),
                s != null ? s.getTitle() : null,
                o.getType(),
                s != null ? String.valueOf(s.getSessionDate()) : String.valueOf(o.getVisitDate()),
                s != null && s.getStartTime() != null ? s.getStartTime().toString() : null,
                s != null && s.getEndTime() != null ? s.getEndTime().toString() : null,
                o.getPeopleCount() != null ? o.getPeopleCount() : 0,
                o.getStatus(), o.getContactName(), o.getContactPhone());
    }

    private int remainingCapacityToday(long guideId, LocalDate today) {
        return sessionRepo.findByGuideIdAndSessionDate(guideId, today).stream()
                .filter(s -> "OPEN".equals(s.getStatus()))
                .mapToInt(s -> Math.max(0,
                        (s.getCapacity() != null ? s.getCapacity() : 0) - orderRepo.sumBookedPeople(s.getId())))
                .sum();
    }

    private static String time(LocalTime t) {
        return t != null ? t.toString() : null;
    }
}
