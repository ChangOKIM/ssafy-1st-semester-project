package com.ssafy.dartservice.recommendation.dto;

import java.util.List;

public record RecommendResponse(
    List<RecommendItem> overall,              // 전체 TOP 10
    List<SectorRecommend> bySector            // 관심섹터별 TOP 3
) {}