package com.ssafy.dartservice.portfolio.dto;

import java.math.BigDecimal;

public record ExtractedHoldingDto(
	String name,
	Integer quantity,
	BigDecimal avgPrice
) {
}
