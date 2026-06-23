package com.ssafy.dartservice.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponseDto {
    private String currentPrice;      // 현재가
    private String avls;              // 시가총액
    private String priceChange;       // 전일 대비
    private String changeRate;        // 등락률 (%)
    private String volume;            // 거래량
    private String week52High;        // 52주 최고가
    private String week52Low;         // 52주 최저가
    private String per;               // PER(주가수익비율)
    private String pbr;               // PBR(주가순자산비율)
    private String eps;               // EPS (주당순이익)
    private String bps;               // BPS (주당순자산)
}