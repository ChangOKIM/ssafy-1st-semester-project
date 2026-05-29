package com.ssafy.dartservice.investor.dto;

import com.ssafy.dartservice.investor.InvestorProfile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record InvestorProfileResponse(
	Long id,
	Long userId,
	String investmentExperience,
	String riskTolerance,
	String investmentGoal,
	BigDecimal investableAmount,
	List<String> preferredSectors,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static InvestorProfileResponse from(InvestorProfile profile) {
		List<String> sectors = Arrays.stream(profile.getPreferredSectors().split(","))
			.map(String::trim)
			.filter(value -> !value.isBlank())
			.toList();

		return new InvestorProfileResponse(
			profile.getId(),
			profile.getUser().getId(),
			profile.getInvestmentExperience(),
			profile.getRiskTolerance(),
			profile.getInvestmentGoal(),
			profile.getInvestableAmount(),
			sectors,
			profile.getCreatedAt(),
			profile.getUpdatedAt()
		);
	}
}
