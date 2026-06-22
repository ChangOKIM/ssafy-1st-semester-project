package com.ssafy.dartservice.stock.dart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockFinancial {
    private Long id;
    private String stockCode;
    private String periodCode;   // 11011=연간, 11014=3분기, 11012=반기, 11013=1분기
    private int baseYear;
    private Long revenue;
    private Long operatingProfit;
    private Long netIncome;
    private Long totalAssets;
    private Long totalDebt;
    private Long totalEquity;
    private Long currentAssets;
    private Long currentLiabilities;
    private Long financeCosts;
    private Long operatingCashFlow;
    private BigDecimal debtRatio;
    private BigDecimal operatingMargin;
    private BigDecimal roe;
    private BigDecimal currentRatio;
    private BigDecimal interestCoverage;
}
