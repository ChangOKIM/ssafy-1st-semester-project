package com.ssafy.dartservice.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ScoringInput(
    String stockCode,
    String stockSector,
    // 재무
    BigDecimal debtRatio,
    BigDecimal interestCoverage,
    BigDecimal currentRatio,
    BigDecimal roe,
    BigDecimal operatingMargin,
    // 사용자
    List<String> userSectors,
    String userRisk,
    List<String> userGoals
) {}