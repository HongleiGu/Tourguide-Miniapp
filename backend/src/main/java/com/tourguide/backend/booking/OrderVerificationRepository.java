package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderVerificationRepository extends JpaRepository<OrderVerification, Long> {

    Optional<OrderVerification> findByOrderId(Long orderId);
}
