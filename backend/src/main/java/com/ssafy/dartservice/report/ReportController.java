package com.ssafy.dartservice.report;

import com.ssafy.dartservice.report.dto.ChartResponseDto;
import com.ssafy.dartservice.report.dto.FinancialResponseDto;
import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{code}/chart")
    public ResponseEntity<List<ChartResponseDto>> getStockChart(
            @PathVariable String code,
            @RequestParam(defaultValue = "D") String period) {

        List<ChartResponseDto> chart = reportService.getStockChart(code, period);
        return ResponseEntity.ok(chart);
    }

    @GetMapping("/{code}/financial")
    public ResponseEntity<List<FinancialResponseDto>> getFinancials(
            @PathVariable String code,
            @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(reportService.getFinancials(code, year));
    }
}