package com.ssafy.dartservice.report;

import com.ssafy.dartservice.report.dto.ChartResponseDto;
import com.ssafy.dartservice.report.dto.FinancialResponseDto;
import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/search")
    public ResponseEntity<List<StockSearchResponseDto>> searchStocks(
            @RequestParam String keyword) {

        if (keyword == null || keyword.length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(reportService.searchStocks(keyword));
    }

    @GetMapping("/{stockCode}/chart")
    public ResponseEntity<List<ChartResponseDto>> getStockChart(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "D") String period) {

        List<ChartResponseDto> chart = reportService.getChart(stockCode, period);
        return ResponseEntity.ok(chart);
    }

    @GetMapping("/{stockCode}/financial")
    public ResponseEntity<List<FinancialResponseDto>> getFinancials(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(reportService.getFinancials(stockCode, year));
    }

    @GetMapping("/{stockCode}/report")
    public ResponseEntity<String> getReport(@PathVariable String stockCode) {
        int latestYear = LocalDate.now().getYear() - 1;  // 작년 기준 (DART 연간보고서는 1년 지연)
        String report = reportService.getReport(stockCode, latestYear);
        return ResponseEntity.ok(report);
    }
}