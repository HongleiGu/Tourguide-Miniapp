package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenicSessionRepository extends JpaRepository<ScenicSession, Long> {

    List<ScenicSession> findByStatusOrderBySessionDateAscStartTimeAsc(String status);
}
