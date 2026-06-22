package com.ssafy.dartservice.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportInput(
        String 회사명,
        List<FinancialYear> 연간,
        LatestQuarter 최신분기,
        String 현재가,      // Long → String
        String per,        // BigDecimal → String
        String pbr         // BigDecimal → String
) {
    public record FinancialYear(
            int 연도,
            String 매출,
            String 영업이익,
            BigDecimal 영업이익률,
            BigDecimal 부채비율,
            BigDecimal 이자보상배율
    ) {}

    // 분기: ROE 같은 누적성 지표 제외, 시점/기간 지표만
    public record LatestQuarter(
            String 기준,        // 예: "2026년 1분기 누적"
            String 매출,
            String 영업이익,
            BigDecimal 영업이익률,
            BigDecimal 부채비율   // 시점 지표라 분기에도 OK
    ) {}
}