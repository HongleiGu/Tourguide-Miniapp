package com.tourguide.backend.guide;

import com.tourguide.backend.api.dto.GuideMe;
import com.tourguide.backend.api.dto.GuideWorkbench;
import com.tourguide.backend.api.dto.ScheduleSegment;
import com.tourguide.backend.booking.BookingOrderRepository;
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
