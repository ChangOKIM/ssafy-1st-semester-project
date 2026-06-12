package com.ssafy.dartservice.recommendation.score;

import com.ssafy.dartservice.recommendation.dto.ScoringInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RecommendScorer {

    // 최종 점수 (0~100)
    public double score(ScoringInput in) {
        double sector = sectorFit(in.stockSector(), in.userSectors());
        double financial = financialHealth(in);
        double risk = riskFit(in.debtRatio(), in.userRisk());
        double goal = goalFit(in);

        return sector * ScoreWeights.SECTOR
             + financial * ScoreWeights.FINANCIAL
             + risk * ScoreWeights.RISK
             + goal * ScoreWeights.GOAL;
    }

    // ① 섹터 적합도
    private double sectorFit(String stockSector, List<String> userSectors) {
        return userSectors.contains(stockSector)
                ? ScoreWeights.SECTOR_MATCH
                : ScoreWeights.SECTOR_NONMATCH;
    }

    // ② 재무 건전성
    private double financialHealth(ScoringInput in) {
        double debt = debtScore(in.debtRatio());
        double interest = interestScore(in.interestCoverage());
        double current = currentScore(in.currentRatio());
        return debt * ScoreWeights.FIN_DEBT
             + interest * ScoreWeights.FIN_INTEREST
             + current * ScoreWeights.FIN_CURRENT;
    }

    // ③ 리스크 적합도 (성향 × 위험도 매트릭스)
    private double riskFit(BigDecimal debtRatio, String userRisk) {
        String stockRisk = riskGrade(debtRatio);  // 저/중/고
        return riskMatrix(userRisk, stockRisk);
    }

    // ④ 목표 적합도 (고른 목표만 평균)
    private double goalFit(ScoringInput in) {
        List<String> goals = in.userGoals();
        if (goals == null || goals.isEmpty()) return 50;  // 목표 없으면 중립

        double sum = 0;
        int n = 0;
        for (String goal : goals) {
            switch (goal) {
                case "CAPITAL_GAIN" -> { sum += capitalGainScore(in); n++; }
                case "LONG_TERM"    -> { sum += longTermScore(in);    n++; }
                case "DIVIDEND"     -> { sum += 50; n++; }  // V1.5 (배당 데이터 없음, 중립)
            }
        }
        return n == 0 ? 50 : sum / n;
    }

    // === 재무 건전성 구간 (실제 분포 검증) ===
    private double debtScore(BigDecimal v) {
        if (v == null) return 50;
        double d = v.doubleValue();
        if (d <= 50) return 100;
        if (d <= 100) return 80;
        if (d <= 170) return 60;
        if (d <= 250) return 40;
        return 20;
    }
    private double interestScore(BigDecimal v) {
        if (v == null) return 50;
        double i = v.doubleValue();
        if (i < 1) return 0;
        if (i < 2) return 40;
        if (i < 4) return 60;
        if (i < 7) return 80;
        return 100;
    }
    private double currentScore(BigDecimal v) {
        if (v == null) return 50;
        double c = v.doubleValue();
        if (c >= 200) return 100;
        if (c >= 150) return 80;
        if (c >= 100) return 60;
        if (c >= 70) return 40;
        return 20;
    }

    // === 리스크 ===
    private String riskGrade(BigDecimal debtRatio) {
        if (debtRatio == null) return "중";
        double d = debtRatio.doubleValue();
        if (d <= 100) return "저";
        if (d <= 200) return "중";
        return "고";
    }
    private double riskMatrix(String userRisk, String stockRisk) {
        // 행: 사용자성향, 열: 종목위험도
        return switch (userRisk) {
            case "LOW" -> switch (stockRisk) {
                case "저" -> 100; case "중" -> 60; default -> 20;
            };
            case "MEDIUM" -> switch (stockRisk) {
                case "저" -> 80; case "중" -> 100; default -> 60;
            };
            case "HIGH" -> switch (stockRisk) {
                case "저" -> 60; case "중" -> 80; default -> 100;
            };
            default -> 60;
        };
    }

    // === 목표: 수익성 점수 ===
    private double capitalGainScore(ScoringInput in) {
        return (roeScore(in.roe()) + marginScore(in.operatingMargin())) / 2;
    }
    private double longTermScore(ScoringInput in) {
        // 장기보유 = 재무건전성 위주
        return financialHealth(in) * 0.6 + roeScore(in.roe()) * 0.4;
    }
    private double roeScore(BigDecimal v) {
        if (v == null) return 40;
        double r = v.doubleValue();
        if (r >= 15) return 100;
        if (r >= 10) return 80;
        if (r >= 5) return 60;
        if (r >= 0) return 40;
        return 20;
    }
    private double marginScore(BigDecimal v) {
        if (v == null) return 40;
        double m = v.doubleValue();
        if (m >= 15) return 100;
        if (m >= 9) return 80;
        if (m >= 5) return 60;
        if (m >= 0) return 40;
        return 20;
    }
}
