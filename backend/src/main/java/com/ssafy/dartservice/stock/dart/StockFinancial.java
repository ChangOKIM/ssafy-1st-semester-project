package com.ssafy.dartservice.stock.dart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockFinancial {
    private Long id;
    private String stockCode;
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
