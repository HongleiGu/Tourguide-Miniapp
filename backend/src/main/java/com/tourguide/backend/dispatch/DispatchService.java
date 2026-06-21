package com.tourguide.backend.dispatch;

import com.tourguide.backend.booking.BookingOrder;
import com.tourguide.backend.booking.BookingOrderRepository;
import com.tourguide.backend.guide.GuideProfile;
import com.tourguide.backend.guide.GuideProfileRepository;
import com.tourguide.backend.guide.GuideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 智能权重派单 (一期): assign an order to an eligible guide, weighted by admin-set dispatch_weight.
 * Phase-2 (review-driven weight auto-computation) is deferred.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final GuideProfileRepository guideRepo;
    private final GuideService guideService;
    private final BookingOrderRepository orderRepo;

    /**
     * Auto-assign a guide to {@code order} for a service at {@code date}/{@code time}.
     * No eligible guide -> left unassigned (falls to the manual 抢单 pool, MIN-42).
     */
    @Transactional
    public void assign(BookingOrder order, LocalDate date, LocalTime time) {
        List<GuideProfile> eligible = guideRepo.findByStatusAndAcceptingOrders("ENABLED", true).stream()
                .filter(g -> guideService.isAcceptingAt(g.getId(), date, time))
                .toList();
        if (eligible.isEmpty()) {
            log.info("no eligible guide for order {} at {} {} -> manual pool", order.getId(), date, time);
            return;
        }
        GuideProfile chosen = pickWeighted(eligible, ThreadLocalRandom.current().nextDouble());
        order.setGuideId(chosen.getId());
        orderRepo.save(order);
        log.info("dispatched order {} -> guide {} (weight {})", order.getId(), chosen.getId(), weight(chosen));
    }

    /**
     * Weighted pick: probability proportional to dispatch_weight (lower weight => fewer orders,
     * not zero unless weight 0). {@code r} is a uniform random in [0,1). Pure + deterministic.
     */
    static GuideProfile pickWeighted(List<GuideProfile> guides, double r) {
        int total = guides.stream().mapToInt(DispatchService::weight).sum();
        if (total <= 0) {
            return guides.get(0); // all-zero weights -> first eligible
        }
        double target = r * total;
        double cumulative = 0;
        for (GuideProfile g : guides) {
            cumulative += weight(g);
            if (target < cumulative) {
                return g;
            }
        }
        return guides.get(guides.size() - 1);
    }

    private static int weight(GuideProfile g) {
        return Math.max(0, g.getDispatchWeight() != null ? g.getDispatchWeight() : 0);
    }
}
