package com.tourguide.backend.booking;

import com.tourguide.backend.guide.GuideProfile;
import com.tourguide.backend.guide.GuideProfileRepository;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import com.tourguide.backend.user.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Seeds sample sessions + announcements + a dev guide in dev so the apps show real data. */
@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevDataBootstrap implements ApplicationRunner {

    /** Openid of the seeded dev guide (also used by DevController's dev-login-guide). */
    public static final String DEV_GUIDE_OPENID = "dev-guide";

    private final ScenicSessionRepository sessionRepo;
    private final AnnouncementRepository announcementRepo;
    private final GroupBuyService groupBuyService;
    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final GuideProfileRepository guideRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (sessionRepo.count() == 0) {
            GuideProfile guide = seedDevGuide();
            LocalDate today = LocalDate.now();
            List<ScenicSession> toSave = new ArrayList<>(List.of(
                    session("故宫深度讲解·私人专场", "PRIVATE", today, LocalTime.of(9, 0), LocalTime.of(11, 0), 1, 30000L),
                    session("故宫拼团讲解（上午）", "GROUP", today, LocalTime.of(9, 30), LocalTime.of(11, 30), 10, 8000L),
                    session("珍宝馆专属时段讲解", "EXCLUSIVE", today.plusDays(1), LocalTime.of(14, 0), LocalTime.of(15, 30), 6, 12000L),
                    session("角楼日落讲解·拼团", "GROUP", today.plusDays(1), LocalTime.of(16, 30), LocalTime.of(18, 0), 12, 6000L)));
            toSave.forEach(s -> s.setGuideId(guide.getId()));
            List<ScenicSession> sessions = sessionRepo.saveAll(toSave);
            // Open a group-buy (min 2, max = capacity) with a deadline + scheduled timeout.
            for (ScenicSession s : sessions) {
                if ("GROUP".equals(s.getType())) {
                    groupBuyService.openGroup(s.getId(), 2, s.getCapacity());
                }
            }
            log.info("Seeded sample scenic sessions (assigned to dev guide {})", guide.getId());
        }
        if (announcementRepo.count() == 0) {
            announcementRepo.saveAll(List.of(
                    announcement("国庆假期限流", "10月1日-7日实行预约限流，请提前预约。", "限流"),
                    announcement("开放时间调整", "旺季开放时间 8:30-17:00，请合理安排行程。", "开放时间")));
            log.info("Seeded sample announcements");
        }
    }

    private ScenicSession session(String title, String type, LocalDate date,
                                  LocalTime start, LocalTime end, int capacity, long priceFen) {
        ScenicSession s = new ScenicSession();
        s.setTitle(title);
        s.setType(type);
        s.setSessionDate(date);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setCapacity(capacity);
        s.setPriceFen(priceFen);
        s.setStatus("OPEN");
        return s;
    }

    /** A dev guide (app_user with GUIDE role + guide_profile) so the guide app is testable. */
    private GuideProfile seedDevGuide() {
        AppUser user = userRepo.findByOpenId(DEV_GUIDE_OPENID).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setOpenId(DEV_GUIDE_OPENID);
            u.setNickname("Dev Guide");
            roleRepo.findByCode("GUIDE").ifPresent(u.getRoles()::add);
            return userRepo.save(u);
        });
        return guideRepo.findByUserId(user.getId()).orElseGet(() -> {
            GuideProfile p = new GuideProfile();
            p.setUserId(user.getId());
            return guideRepo.save(p);
        });
    }

    private Announcement announcement(String title, String content, String type) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setType(type);
        a.setActive(true);
        return a;
    }
}
