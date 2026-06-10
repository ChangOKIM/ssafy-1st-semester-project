package com.ssafy.dartservice.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportInput(
        String 회사명,
        List<FinancialYear> 재무_3년,
        String 현재가,
        String PER,
        String PBR
) {
    public record FinancialYear(
            int 연도,
            String 매출,
            String 영업이익,
            BigDecimal 영업이익률,
            BigDecimal 부채비율,
            BigDecimal 이자보상배율
    ) {}
}