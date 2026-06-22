package com.ssafy.dartservice.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class RecommendItem {
    private String stockCode;
    private String stockName;
    private String sector;
    private double score;

    @JsonIgnore private BigDecimal debtRatio;
    @JsonIgnore private BigDecimal interestCoverage;
    @JsonIgnore private BigDecimal currentRatio;
    @JsonIgnore private BigDecimal roe;
    @JsonIgnore private BigDecimal operatingMargin;
}
