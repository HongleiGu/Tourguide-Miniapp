package com.tourguide.backend.booking;

import com.tourguide.backend.api.dto.AdminSessionRequest;
import com.tourguide.backend.api.dto.AdminSessionView;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Admin 场次管理 (MIN-48): create / list / update / 锁场·停场, with group-buy setup for GROUP. */
@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private static final String GROUP = "GROUP";
    private static final Set<String> STATUSES = Set.of("OPEN", "LOCKED", "CLOSED", "CANCELLED");

    private final ScenicSessionRepository sessionRepo;
    private final GroupBuyRepository groupBuyRepo;
    private final GroupBuyService groupBuyService;

    @Transactional(readOnly = true)
    public List<AdminSessionView> list(LocalDate date) {
        List<ScenicSession> sessions = date != null
                ? sessionRepo.findBySessionDateOrderByStartTimeAsc(date)
                : sessionRepo.findAllByOrderBySessionDateAscStartTimeAsc();
        return sessions.stream().map(this::view).toList();
    }

    @Transactional
    public AdminSessionView create(AdminSessionRequest r) {
        ScenicSession s = new ScenicSession();
        s.setTitle(r.title());
        s.setType(r.type());
        s.setSessionDate(r.date());
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setGuideId(r.guideId());
        s.setPriceFen(r.priceFen());
        s.setStatus("OPEN");
        s.setCapacity(capacityFor(r));
        ScenicSession saved = sessionRepo.save(s);
        if (GROUP.equals(r.type())) {
            int min = r.groupMinSize() != null ? r.groupMinSize() : 2;
            groupBuyService.openGroup(saved.getId(), min, saved.getCapacity());
        }
        return view(saved);
    }

    @Transactional
    public AdminSessionView update(long id, AdminSessionRequest r) {
        ScenicSession s = find(id);
        s.setTitle(r.title());
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setGuideId(r.guideId());
        s.setPriceFen(r.priceFen());
        // capacity edits apply to non-GROUP (GROUP capacity is owned by its group-buy)
        if (!GROUP.equals(s.getType()) && r.capacity() != null) {
            s.setCapacity(r.capacity());
        }
        return view(sessionRepo.save(s));
    }

    /** 锁场 (LOCKED) / 停场 (CLOSED) / 开场 (OPEN). */
    @Transactional
    public AdminSessionView setStatus(long id, String status) {
        if (!STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法场次状态");
        }
        ScenicSession s = find(id);
        s.setStatus(status);
        return view(sessionRepo.save(s));
    }

    private int capacityFor(AdminSessionRequest r) {
        if (GROUP.equals(r.type())) {
            return r.groupMaxSize() != null ? r.groupMaxSize() : (r.capacity() != null ? r.capacity() : 10);
        }
        return r.capacity() != null ? r.capacity() : 1;
    }

    private ScenicSession find(long id) {
        return sessionRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "场次不存在"));
    }

    private AdminSessionView view(ScenicSession s) {
        Integer groupMin = null;
        Integer groupMax = null;
        Integer groupCurrent = null;
        if (GROUP.equals(s.getType())) {
            GroupBuy g = groupBuyRepo.findBySessionId(s.getId()).orElse(null);
            if (g != null) {
                groupMin = g.getMinSize();
                groupMax = g.getMaxSize();
                groupCurrent = g.getCurrentSize();
            }
        }
        return new AdminSessionView(
                s.getId(), s.getTitle(), s.getType(),
                String.valueOf(s.getSessionDate()),
                s.getStartTime() != null ? s.getStartTime().toString() : null,
                s.getEndTime() != null ? s.getEndTime().toString() : null,
                s.getCapacity() != null ? s.getCapacity() : 0,
                s.getPriceFen() != null ? s.getPriceFen() : 0,
                s.getGuideId(), s.getStatus(), groupMin, groupMax, groupCurrent);
    }
}
