package com.ssafy.dartservice.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchResponseDto {
    private String stockCode;
    private String stockName;
    private String market;
}