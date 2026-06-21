package com.tourguide.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderReviewRepository extends JpaRepository<OrderReview, Long> {

    Optional<OrderReview> findByOrderId(Long orderId);
}
