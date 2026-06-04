package com.ssafy.dartservice.report;

import java.util.List;

public interface ReportService {
    List<StockSearchResponseDto> searchStocks(String keyword);
}