package com.ssafy.dartservice.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record HoldingExtractResponse(
	String name,
	String stockCode,
	Integer quantity,
	BigDecimal avgPrice,
	List<CandidateDto> candidates
) {
	public record CandidateDto(String code, String name) {
	}
}
