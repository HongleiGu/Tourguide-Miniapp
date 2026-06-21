package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AnnouncementView;
import com.tourguide.backend.api.dto.CreateOrderRequest;
import com.tourguide.backend.api.dto.OrderView;
import com.tourguide.backend.api.dto.SessionView;
import com.tourguide.backend.booking.TouristService;
import com.tourguide.backend.common.ApiResponse;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import com.tourguide.backend.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tourist browse + booking. Sessions/announcements are public; orders require auth. */
@Tag(name = "Tourist")
@RestController
@RequestMapping("/api/tourist")
@RequiredArgsConstructor
public class TouristController {

    private final TouristService service;

    @GetMapping("/sessions")
    public ApiResponse<List<SessionView>> sessions() {
        return ApiResponse.ok(service.listSessions());
    }

    @GetMapping("/announcements")
    public ApiResponse<List<AnnouncementView>> announcements() {
        return ApiResponse.ok(service.listAnnouncements());
    }

    @PostMapping("/orders")
    public ApiResponse<OrderView> createOrder(@AuthenticationPrincipal AuthPrincipal principal,
                                              @Valid @RequestBody CreateOrderRequest req) {
        return ApiResponse.ok(service.createOrder(requireUser(principal), req));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderView>> myOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.listMyOrders(requireUser(principal)));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderView> getOrder(@AuthenticationPrincipal AuthPrincipal principal,
                                           @PathVariable long id) {
        return ApiResponse.ok(service.getOrder(requireUser(principal), id));
    }

    private long requireUser(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.userId();
    }
}
