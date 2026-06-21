package com.tourguide.backend.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    Optional<PricingRule> findBySessionTypeAndDayType(String sessionType, String dayType);

    List<PricingRule> findAllByOrderBySessionTypeAscDayTypeAsc();
}
