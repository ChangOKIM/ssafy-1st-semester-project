package com.ssafy.dartservice.investor;

import com.ssafy.dartservice.global.response.ApiResponse;
import com.ssafy.dartservice.global.security.CustomUserDetails;
import com.ssafy.dartservice.investor.dto.InvestorProfileRequest;
import com.ssafy.dartservice.investor.dto.InvestorProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/user-profile")
public class InvestorProfileController {

	private final InvestorProfileService investorProfileService;

	public InvestorProfileController(InvestorProfileService investorProfileService) {
		this.investorProfileService = investorProfileService;
	}

	@GetMapping
	public ApiResponse<InvestorProfileResponse> get(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success(investorProfileService.get(userDetails == null ? null : userDetails.getUser()));
	}

	@PutMapping
	public ApiResponse<InvestorProfileResponse> save(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody InvestorProfileRequest request
	) {
		return ApiResponse.success(investorProfileService.save(userDetails == null ? null : userDetails.getUser(), request));
	}
}
