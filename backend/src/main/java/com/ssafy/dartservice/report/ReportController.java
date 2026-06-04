package com.ssafy.dartservice.report;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
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
}