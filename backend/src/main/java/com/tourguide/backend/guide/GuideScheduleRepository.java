package com.tourguide.backend.guide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GuideScheduleRepository extends JpaRepository<GuideSchedule, Long> {

    List<GuideSchedule> findByGuideIdAndWorkDateOrderByStartTimeAsc(Long guideId, LocalDate workDate);

    List<GuideSchedule> findByGuideIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(
            Long guideId, LocalDate from, LocalDate to);
}
