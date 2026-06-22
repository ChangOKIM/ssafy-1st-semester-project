package com.ssafy.dartservice.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportInput(
        String 회사명,
        String 섹터가이드,
        List<FinancialYear> 연간,
        LatestQuarter 최신분기,
        String 현재가,
        String per,
        String pbr
) {
    public record FinancialYear(
            int 연도,
            String 매출,
            String 영업이익,
            BigDecimal 영업이익률,
            BigDecimal 부채비율,
            BigDecimal 이자보상배율
    ) {}

    public record LatestQuarter(
            String 분기라벨,
            String 매출,
            String 영업이익,
            BigDecimal 영업이익률,
            BigDecimal 부채비율
    ) {}

    public String toPromptText() {
        StringBuilder sb = new StringBuilder();

        sb.append("[기업]\n")
                .append("회사명: ").append(nz(회사명)).append("\n\n");

        if (섹터가이드 != null && !섹터가이드.isBlank()) {
            sb.append("[업종 해석 가이드]\n")
                    .append(섹터가이드).append("\n\n");
        }

        if (최신분기 != null) {
            sb.append("[최근 분기] ").append(nz(최신분기.분기라벨())).append("\n")
                    .append("매출: ").append(nz(최신분기.매출())).append("\n")
                    .append("영업이익: ").append(nz(최신분기.영업이익())).append("\n")
                    .append("영업이익률: ").append(pct(최신분기.영업이익률())).append("\n\n")
                    .append("부채비율: ").append(pct(최신분기.부채비율())).append("\n\n");
        }

        sb.append("[연간 재무 3년]\n");
        if (연간 == null || 연간.isEmpty()) {
            sb.append("정보 없음\n");
        } else {
            for (FinancialYear y : 연간) {
                sb.append(y.연도()).append("년")
                        .append(" | 매출 ").append(nz(y.매출()))
                        .append(" | 영업이익 ").append(nz(y.영업이익()))
                        .append(" | 영업이익률 ").append(pct(y.영업이익률()))
                        .append(" | 부채비율 ").append(pct(y.부채비율()))
                        .append(" | 이자보상배율 ").append(x(y.이자보상배율()))
                        .append("\n");
            }
        }
        sb.append("\n");

        sb.append("[밸류에이션]\n")
                .append("현재가: ").append(nz(현재가)).append("\n")
                .append("PER: ").append(per == null ? "정보 없음" : per + "배").append("\n")
                .append("PBR: ").append(pbr == null ? "정보 없음" : pbr + "배").append("\n");

        return sb.toString();
    }

    private static String nz(Object v) {
        return v == null ? "정보 없음" : v.toString();
    }

    private static String pct(BigDecimal v) {
        return v == null ? "정보 없음" : v + "%";
    }

    private static String x(BigDecimal v) {
        return v == null ? "정보 없음" : v + "배";
    }
}