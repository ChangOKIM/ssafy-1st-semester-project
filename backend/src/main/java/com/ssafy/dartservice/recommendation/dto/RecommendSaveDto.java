package com.ssafy.dartservice.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendSaveDto {
    private Long userId;
    private String stockCode;
    private String recType;
    private double score;
}