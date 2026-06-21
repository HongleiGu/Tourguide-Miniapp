package com.tourguide.backend.booking;

import com.tourguide.backend.api.dto.AdminOrderView;
import com.tourguide.backend.common.BusinessException;
import com.tourguide.backend.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Admin 订单管理 (MIN-50): 查询/筛选 + Excel 导出 + 异常处理. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private static final String GROUP = "GROUP";
    private static final String[] HEADERS =
            {"订单号", "类型", "状态", "人数", "金额(元)", "出行日期", "讲解员ID", "联系人", "电话", "场次"};

    private final BookingOrderRepository orderRepo;
    private final ScenicSessionRepository sessionRepo;
    private final GroupBuyService groupBuyService;

    @Transactional(readOnly = true)
    public List<AdminOrderView> search(String status, String type, Long guideId, LocalDate from, LocalDate to) {
        return orderRepo.search(status, type, guideId, from, to).stream().map(this::view).toList();
    }

    /** Excel (.xlsx) of the filtered orders. */
    @Transactional(readOnly = true)
    public byte[] export(String status, String type, Long guideId, LocalDate from, LocalDate to) {
        List<AdminOrderView> rows = search(status, type, guideId, from, to);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("订单");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int r = 1;
            for (AdminOrderView o : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(o.orderNo());
                row.createCell(1).setCellValue(o.type());
                row.createCell(2).setCellValue(o.status());
                row.createCell(3).setCellValue(o.peopleCount());
                row.createCell(4).setCellValue(o.amountFen() / 100.0);
                row.createCell(5).setCellValue(o.visitDate() != null ? o.visitDate() : "");
                row.createCell(6).setCellValue(o.guideId() != null ? String.valueOf(o.guideId()) : "");
                row.createCell(7).setCellValue(o.contactName() != null ? o.contactName() : "");
                row.createCell(8).setCellValue(o.contactPhone() != null ? o.contactPhone() : "");
                row.createCell(9).setCellValue(o.sessionTitle() != null ? o.sessionTitle() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel export failed", e);
        }
    }

    /** 异常处理: CANCEL / REFUND / COMPLETE (with a reason, logged). */
    @Transactional
    public AdminOrderView handle(long orderId, String action, String reason) {
        BookingOrder o = orderRepo.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        boolean active = "PENDING_PAYMENT".equals(o.getStatus()) || "PAID".equals(o.getStatus());
        switch (action) {
            case "CANCEL" -> {
                releaseGroupIfNeeded(o, active);
                o.setStatus("CANCELLED");
            }
            case "REFUND" -> {
                releaseGroupIfNeeded(o, active);
                o.setStatus("REFUNDED");
            }
            case "COMPLETE" -> o.setStatus("COMPLETED");
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "非法处理动作");
        }
        log.info("admin handled order {} -> {} (reason: {})", orderId, action, reason);
        return view(orderRepo.save(o));
    }

    private void releaseGroupIfNeeded(BookingOrder o, boolean active) {
        if (active && GROUP.equals(o.getType())) {
            groupBuyService.release(o.getSessionId(), o.getPeopleCount() != null ? o.getPeopleCount() : 0);
        }
    }

    private AdminOrderView view(BookingOrder o) {
        String title = sessionRepo.findById(o.getSessionId()).map(ScenicSession::getTitle).orElse(null);
        return new AdminOrderView(
                o.getId(), o.getOrderNo(), o.getType(), o.getStatus(),
                o.getPeopleCount() != null ? o.getPeopleCount() : 0,
                o.getAmountFen() != null ? o.getAmountFen() : 0,
                o.getVisitDate() != null ? o.getVisitDate().toString() : null,
                o.getGuideId(), o.getUserId(), title, o.getContactName(), o.getContactPhone());
    }
}
