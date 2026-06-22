package com.ssafy.dartservice.recommendation.score;

public class ScoreWeights {
    // 4대 요소 가중치
    public static final double SECTOR = 0.45;
    public static final double FINANCIAL = 0.30;
    public static final double RISK = 0.07;
    public static final double GOAL = 0.18;

    // 섹터 적합도
    public static final double SECTOR_MATCH = 100;
    public static final double SECTOR_NONMATCH = 40;  // 실험 조정

    // 재무 건전성 내부 가중치
    public static final double FIN_DEBT = 0.25;
    public static final double FIN_INTEREST = 0.40;
    public static final double FIN_CURRENT = 0.35;
}
