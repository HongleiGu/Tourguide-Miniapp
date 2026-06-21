package com.tourguide.backend.api;

import com.tourguide.backend.api.dto.AdminOrderView;
import com.tourguide.backend.api.dto.OrderHandleRequest;
import com.tourguide.backend.booking.AdminOrderService;
import com.tourguide.backend.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Admin 订单管理 (MIN-50): 查询/筛选/导出/异常处理. */
@Tag(name = "Admin Orders")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService service;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW')")
    public ApiResponse<List<AdminOrderView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long guideId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.search(status, type, guideId, from, to));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('EXPORT')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long guideId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] xlsx = service.export(status, type, guideId, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('OPERATE')")
    public ApiResponse<AdminOrderView> handle(@PathVariable long id,
                                              @Valid @RequestBody OrderHandleRequest req) {
        return ApiResponse.ok(service.handle(id, req.action(), req.reason()));
    }
}
