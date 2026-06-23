package com.ssafy.dartservice.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportInput(
        String 회사명,
        String 섹터가이드,
        String 투자경험,
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

        if (투자경험 != null && !투자경험.isBlank()) {
            sb.append("[독자 수준] ").append(level(투자경험)).append("\n\n");
        }

        sb.append("[기업]\n")
                .append("회사명: ").append(nz(회사명)).append("\n\n");

        if (섹터가이드 != null && !섹터가이드.isBlank()) {
            sb.append("[업종 해석 가이드]\n")
                    .append(섹터가이드).append("\n\n");
        }

        if (최신분기 != null) {
            sb.append("\"[최근 분기 — 현재 상태 참고용. 부분 기간이라 연간과 직접 비교 금지] ").append(nz(최신분기.분기라벨())).append("\n")
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

    private static String level(String experience) {
        return switch (experience) {
            case "NONE" -> """
            독자는 주식 투자를 한 번도 해본 적 없는 완전 입문자입니다.
            - 주식의 기본 개념(주가, 이익, 빚 등)도 처음 듣는다고 가정하세요.
            - 모든 용어를 일상어로 바꿔 설명하고, 용어 자체를 최대한 쓰지 마세요.
              (예: 'PER' 대신 "지금 주가가 회사가 버는 돈에 비해 어느 정도인지")
            - 친근한 비유로 그림이 그려지게 설명하세요.
              (예: "이자보상배율 10배 = 한 달 월급으로 이자를 10번 낼 만큼 여유로움")
            - 한 번에 한 가지만, 아주 천천히. 문장은 짧고 부담 없게.""";

            case "BEGINNER" -> """
            독자는 주식을 시작한 지 얼마 안 된 초보자입니다.
            - 주식 기본 개념은 들어봤지만, PER·PBR 같은 지표는 아직 낯섭니다.
            - 용어는 쓰되 반드시 한 줄로 풀어 설명하세요.
              (예: "PER 12배 (주가가 1년 이익의 12배 수준이라는 뜻)")
            - 일상 비유를 곁들여 이해를 도우세요.
            - 한 개념씩 차근차근, 문장은 쉽게.""";

            case "INTERMEDIATE" -> """
            독자는 기본 용어는 아는 중급 투자자입니다.
            - PER, PBR 등 용어는 그대로 써도 되지만, 짧은 한 줄 보충은 곁들이세요.
            - 비유는 최소화하고, 숫자의 '해석'에 무게를 두세요.
            - 지표 간 연결(예: 이익 추세 대비 PER 수준)을 적극 짚어주세요.""";

            case "ADVANCED" -> """
            독자는 투자 경험이 많은 고급 투자자입니다.
            - 용어 설명은 생략하세요. 풀어쓰기 없이 지표를 바로 언급해도 됩니다.
            - 수치와 그 해석을 압축해 핵심만 전달하세요. 군더더기 없이.
            - 지표 간 종합 해석과 주목할 변화 포인트 위주로 밀도 높게 쓰세요.""";

            default -> "독자는 주식 초보자입니다. 모든 용어를 쉽게 풀어 설명하세요.";
        };
    }

}