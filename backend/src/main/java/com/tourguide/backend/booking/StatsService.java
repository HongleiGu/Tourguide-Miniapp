package com.tourguide.backend.booking;

import com.tourguide.backend.api.dto.StatsView;
import lombok.RequiredArgsConstructor;
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

/** 基础统计 (MIN-51): order/visitor counts, 成团率, 核销率, 营收. 大屏 deferred to phase-2. */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final BookingOrderRepository orderRepo;
    private final ScenicSessionRepository sessionRepo;
    private final GroupBuyRepository groupBuyRepo;

    @Transactional(readOnly = true)
    public StatsView compute(LocalDate from, LocalDate to) {
        List<BookingOrder> orders = orderRepo.search(null, null, null, from, to);

        int paid = (int) orders.stream().filter(o -> "PAID".equals(o.getStatus())).count();
        int completed = (int) orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();
        int visitors = orders.stream()
                .filter(o -> "PAID".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus()))
                .mapToInt(o -> o.getPeopleCount() != null ? o.getPeopleCount() : 0).sum();
        long revenue = orders.stream()
                .filter(o -> "PAID".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus()))
                .mapToLong(o -> o.getAmountFen() != null ? o.getAmountFen() : 0).sum();

        // 核销率: of paid-or-completed, the fraction completed (= verified)
        double verificationRate = (paid + completed) == 0 ? 0 : (double) completed / (paid + completed);

        // 成团率: of resolved group-buys in range, fraction CONFIRMED (vs VOIDED)
        List<ScenicSession> sessions = (from != null && to != null)
                ? sessionRepo.findBySessionDateBetween(from, to)
                : sessionRepo.findAllByOrderBySessionDateAscStartTimeAsc();
        int confirmed = 0;
        int resolved = 0;
        for (ScenicSession s : sessions) {
            if (!"GROUP".equals(s.getType())) {
                continue;
            }
            GroupBuy g = groupBuyRepo.findBySessionId(s.getId()).orElse(null);
            if (g == null) {
                continue;
            }
            if ("CONFIRMED".equals(g.getStatus())) {
                confirmed++;
                resolved++;
            } else if ("VOIDED".equals(g.getStatus())) {
                resolved++;
            }
        }
        double groupFormationRate = resolved == 0 ? 0 : (double) confirmed / resolved;

        return new StatsView(
                from != null ? from.toString() : null,
                to != null ? to.toString() : null,
                orders.size(), paid, completed, visitors, revenue,
                groupFormationRate, verificationRate);
    }

    @Transactional(readOnly = true)
    public byte[] export(LocalDate from, LocalDate to) {
        StatsView s = compute(from, to);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("统计");
            String[][] kv = {
                    {"统计周期", (s.from() != null ? s.from() : "全部") + " ~ " + (s.to() != null ? s.to() : "全部")},
                    {"订单量", String.valueOf(s.totalOrders())},
                    {"待核销", String.valueOf(s.paidOrders())},
                    {"已完成", String.valueOf(s.completedOrders())},
                    {"游客量", String.valueOf(s.visitors())},
                    {"营收总额(元)", String.format("%.2f", s.revenueFen() / 100.0)},
                    {"成团率", String.format("%.1f%%", s.groupFormationRate() * 100)},
                    {"核销率", String.format("%.1f%%", s.verificationRate() * 100)},
            };
            for (int i = 0; i < kv.length; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(kv[i][0]);
                row.createCell(1).setCellValue(kv[i][1]);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel export failed", e);
        }
    }
}
