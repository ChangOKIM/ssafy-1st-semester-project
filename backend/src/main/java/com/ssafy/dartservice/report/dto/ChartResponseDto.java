package com.ssafy.dartservice.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartResponseDto {
    private String date;          // 날짜
    private String openPrice;     // 시가
    private String highPrice;     // 고가
    private String lowPrice;      // 저가
    private String closePrice;    // 종가
    private String volume;        // 거래량
}