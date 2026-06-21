package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScenicSessionRepository extends JpaRepository<ScenicSession, Long> {

    List<ScenicSession> findByStatusOrderBySessionDateAscStartTimeAsc(String status);

    List<ScenicSession> findByGuideIdAndSessionDate(Long guideId, LocalDate sessionDate);
}
