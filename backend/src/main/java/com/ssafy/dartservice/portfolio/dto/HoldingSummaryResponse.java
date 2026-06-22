package com.ssafy.dartservice.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record HoldingSummaryResponse(
	int holdingCount,
	BigDecimal totalPurchaseAmount,
	BigDecimal totalEvaluationAmount,
	BigDecimal totalProfitAmount,
	BigDecimal totalProfitRate,
	String summary,
	List<DiagnosisSection> sections
) {
	public record DiagnosisSection(
		String title,
		String content,
		List<String> guideItems
	) {
	}
}
