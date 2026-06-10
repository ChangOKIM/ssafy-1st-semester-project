package com.ssafy.dartservice.stock.dto;

import com.ssafy.dartservice.stock.dart.StockFinancial;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FinancialResponseDto {
    private int baseYear;
    private Long revenue;
    private Long operatingProfit;
    private Long netIncome;
    private BigDecimal debtRatio;
    private BigDecimal operatingMargin;
    private BigDecimal roe;
    private BigDecimal currentRatio;
    private BigDecimal interestCoverage;
    private Long operatingCashFlow;

    public static FinancialResponseDto from(StockFinancial f) {
        return FinancialResponseDto.builder()
                .baseYear(f.getBaseYear())
                .revenue(f.getRevenue())
                .operatingProfit(f.getOperatingProfit())
                .netIncome(f.getNetIncome())
                .debtRatio(f.getDebtRatio())
                .operatingMargin(f.getOperatingMargin())
                .roe(f.getRoe())
                .currentRatio(f.getCurrentRatio())
                .interestCoverage(f.getInterestCoverage())
                .operatingCashFlow(f.getOperatingCashFlow())
                .build();
    }
}