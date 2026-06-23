package com.ssafy.dartservice.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchResponseDto {
    private String stockCode;
    private String corpCode;
    private String stockName;
    private String market;
    private String sector;
    private String intro;
}