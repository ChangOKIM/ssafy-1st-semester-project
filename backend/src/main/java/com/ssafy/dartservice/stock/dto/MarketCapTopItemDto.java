package com.ssafy.dartservice.stock.dto;

public record MarketCapTopItemDto(
        int rank,
        String stockCode,
        String stockName,
        long currentPrice,
        long priceChange,       // 절댓값 (부호는 priceChangeSign으로 구분)
        String priceChangeSign, // "UP" / "DOWN" / "FLAT"
        double changeRate,
        long volume,
        long marketCap,         // 억원 단위 (KIS 원본 그대로)
        String marketShare      // 시장 전체 시총 비중 (%)
) {}
