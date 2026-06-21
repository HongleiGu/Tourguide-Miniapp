package com.tourguide.backend.guide;

import com.tourguide.backend.api.dto.GuideMe;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.user.AppUser;
import com.tourguide.backend.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Guide-facing workbench services (MIN-6). */
@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideProfileRepository guideRepo;
    private final AppUserRepository userRepo;

    /** The guide_profile for an app-user, or 403 if the account is not a guide. */
    public GuideProfile requireProfile(long userId) {
        return guideRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "非讲解员账号"));
    }

    @Transactional(readOnly = true)
    public GuideMe me(long userId) {
        GuideProfile p = requireProfile(userId);
        String name = userRepo.findById(userId).map(AppUser::getNickname).orElse(null);
        return new GuideMe(p.getId(), name, p.getEmploymentType(),
                Boolean.TRUE.equals(p.getAcceptingOrders()), p.getStatus(),
                p.getRating() != null ? p.getRating().doubleValue() : 0,
                p.getStarLevel() != null ? p.getStarLevel() : 0);
    }
}
