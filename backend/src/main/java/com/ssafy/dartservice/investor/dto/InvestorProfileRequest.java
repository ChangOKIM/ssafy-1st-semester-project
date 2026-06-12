package com.ssafy.dartservice.investor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record InvestorProfileRequest(
	Long userId,

	@NotBlank(message = "투자 경험을 선택해 주세요.")
	String investmentExperience,

	@NotBlank(message = "위험 감수 성향을 선택해 주세요.")
	String riskTolerance,

	@NotBlank(message = "투자 목표를 선택해 주세요.")
	String investmentGoal,

	BigDecimal investableAmount,

	@NotEmpty(message = "관심 분야를 하나 이상 선택해 주세요.")
	List<String> preferredSectors
) {
}
