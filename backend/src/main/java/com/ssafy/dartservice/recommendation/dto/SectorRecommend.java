package com.ssafy.dartservice.recommendation.dto;

import java.util.List;

public record SectorRecommend(
    String sector,                // 섹터명 (예: "반도체")
    List<RecommendItem> items     // 그 섹터 TOP 3
) {}