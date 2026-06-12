package com.ssafy.dartservice.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HoldingRequest(
	@NotBlank(message = "종목 코드를 입력해 주세요.")
	String stockCode,

	@NotNull(message = "수량을 입력해 주세요.")
	@Min(value = 1, message = "수량은 1주 이상이어야 합니다.")
	Integer quantity,

	@NotNull(message = "매입가를 입력해 주세요.")
	@DecimalMin(value = "0.01", message = "매입가는 0보다 커야 합니다.")
	BigDecimal purchasePrice,

	@NotNull(message = "매입일을 입력해 주세요.")
	LocalDate purchaseDate
) {
}
