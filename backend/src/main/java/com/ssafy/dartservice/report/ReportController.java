package com.ssafy.dartservice.report;

import com.ssafy.dartservice.global.security.CustomUserDetails;
import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/{code}/info")
    public ResponseEntity<StockSearchResponseDto> getStockInfo(@PathVariable String code) {
        StockSearchResponseDto info = reportService.getStockInfo(code);
        if (info == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(info);
    }

    @GetMapping("/{code}/report")
    public ResponseEntity<String> getReport(
            @PathVariable String code,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        int latestYear = LocalDate.now().getYear() - 1;
        String report = reportService.getReport(code, latestYear, userDetails.getUser());
        return ResponseEntity.ok(report);
    }
}