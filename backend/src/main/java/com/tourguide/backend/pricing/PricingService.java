package com.tourguide.backend.pricing;

import com.tourguide.backend.api.dto.PricingRuleRequest;
import com.tourguide.backend.api.dto.PricingRuleView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 价格与拼团规则 (MIN-49). Day type is weekend-based for phase-1 (real holiday calendar later). */
@Service
@RequiredArgsConstructor
public class PricingService {

    public static final String WORKDAY = "WORKDAY";
    public static final String HOLIDAY = "HOLIDAY";

    private final PricingRuleRepository repo;

    @Transactional(readOnly = true)
    public List<PricingRuleView> list() {
        return repo.findAllByOrderBySessionTypeAscDayTypeAsc().stream().map(this::view).toList();
    }

    @Transactional
    public PricingRuleView upsert(PricingRuleRequest r) {
        PricingRule rule = repo.findBySessionTypeAndDayType(r.sessionType(), r.dayType())
                .orElseGet(PricingRule::new);
        rule.setSessionType(r.sessionType());
        rule.setDayType(r.dayType());
        rule.setPriceFen(r.priceFen());
        rule.setGroupMin(r.groupMin());
        rule.setGroupMax(r.groupMax());
        return view(repo.save(rule));
    }

    /** WORKDAY/HOLIDAY for a date (phase-1: weekend = 节假日). */
    public String dayType(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) ? HOLIDAY : WORKDAY;
    }

    /** The applicable rule for a session type on a date, if configured. */
    @Transactional(readOnly = true)
    public Optional<PricingRule> ruleFor(String sessionType, LocalDate date) {
        return repo.findBySessionTypeAndDayType(sessionType, dayType(date));
    }

    private PricingRuleView view(PricingRule r) {
        return new PricingRuleView(r.getId(), r.getSessionType(), r.getDayType(),
                r.getPriceFen() != null ? r.getPriceFen() : 0, r.getGroupMin(), r.getGroupMax());
    }
}
