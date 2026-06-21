package com.tourguide.backend.booking;

import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 拼团 engine. Seat claiming is an atomic, guarded conditional UPDATE in MySQL (no oversell,
 * no distributed lock); the order INSERT happens in the same transaction (see TouristService).
 */
@Service
@RequiredArgsConstructor
public class GroupBuyService {

    private static final int DEFAULT_MIN_SIZE = 2;

    private final GroupBuyRepository groupBuyRepo;

    /** The group-buy for a GROUP session, created on first access. */
    @Transactional
    public GroupBuy getOrCreate(ScenicSession session) {
        return groupBuyRepo.findBySessionId(session.getId()).orElseGet(() -> {
            GroupBuy g = new GroupBuy();
            g.setSessionId(session.getId());
            g.setMinSize(DEFAULT_MIN_SIZE);
            g.setMaxSize(session.getCapacity() != null ? session.getCapacity() : 10);
            g.setCurrentSize(0);
            g.setStatus("FORMING");
            return groupBuyRepo.save(g);
        });
    }

    /**
     * Atomically claim {@code n} seats; lock the group if it becomes full.
     * Throws {@link ErrorCode#CONFLICT} when there aren't enough seats / the group is closed.
     */
    @Transactional
    public GroupBuy claim(long groupBuyId, int n) {
        if (groupBuyRepo.claimSeats(groupBuyId, n) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "拼团名额不足或已截止");
        }
        groupBuyRepo.lockIfFull(groupBuyId);
        return groupBuyRepo.findById(groupBuyId).orElseThrow();
    }
}
