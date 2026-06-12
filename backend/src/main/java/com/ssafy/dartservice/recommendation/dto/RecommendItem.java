package com.ssafy.dartservice.recommendation.dto;

public record RecommendItem(
    String stockCode,
    String sector,
    double score
) {}