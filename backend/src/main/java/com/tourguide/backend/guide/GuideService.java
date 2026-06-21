package com.tourguide.backend.guide;

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
        String name = userRepo.findById(userId).map(AppUser::getNickname).orElse(null);
        return new GuideMe(p.getId(), name, p.getEmploymentType(),
                Boolean.TRUE.equals(p.getAcceptingOrders()), p.getStatus(),
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

        List<ScheduleSegment> segments = schedule.stream()
                .map(s -> new ScheduleSegment(s.getId(), s.getWorkDate().toString(), s.getType(),
                        time(s.getStartTime()), time(s.getEndTime())))
                .toList();

        return new GuideWorkbench(today.toString(), accepting, onDuty,
                pending, toVerify, completed, remaining, segments);
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
