package com.ssafy.dartservice.report;

import com.ssafy.dartservice.report.dto.ChartResponseDto;
import com.ssafy.dartservice.report.dto.FinancialResponseDto;
import com.ssafy.dartservice.report.dto.StockSearchResponseDto;

import java.util.List;

public interface ReportService {
    List<StockSearchResponseDto> searchStocks(String keyword);
    List<ChartResponseDto> getStockChart(String stockCode, String period);
    List<FinancialResponseDto> getFinancials(String stockCode, int latestYear);
}