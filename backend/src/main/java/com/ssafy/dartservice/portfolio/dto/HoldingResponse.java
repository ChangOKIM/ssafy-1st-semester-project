package com.ssafy.dartservice.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HoldingResponse(
	Long id,
	String stockCode,
	String stockName,
	String market,
	String sector,
	Integer quantity,
	BigDecimal purchasePrice,
	LocalDate purchaseDate,
	BigDecimal purchaseAmount,
	BigDecimal currentPrice,
	BigDecimal evaluationAmount,
	BigDecimal profitAmount,
	BigDecimal profitRate
) {
}
