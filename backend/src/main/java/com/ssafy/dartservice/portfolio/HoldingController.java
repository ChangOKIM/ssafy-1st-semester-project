package com.ssafy.dartservice.portfolio;

import com.ssafy.dartservice.global.response.ApiResponse;
import com.ssafy.dartservice.global.security.CustomUserDetails;
import com.ssafy.dartservice.portfolio.dto.HoldingExtractResponse;
import com.ssafy.dartservice.portfolio.dto.HoldingRequest;
import com.ssafy.dartservice.portfolio.dto.HoldingResponse;
import com.ssafy.dartservice.portfolio.dto.HoldingSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/holdings")
public class HoldingController {

	private final HoldingService holdingService;
	private final HoldingExtractService holdingExtractService;

	@GetMapping
	public ApiResponse<List<HoldingResponse>> getHoldings(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success(holdingService.getHoldings(userDetails.getUser()));
	}

	@PostMapping
	public ApiResponse<HoldingResponse> createHolding(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody HoldingRequest request
	) {
		return ApiResponse.success(holdingService.createHolding(userDetails.getUser(), request));
	}

	@PutMapping("/{id}")
	public ApiResponse<HoldingResponse> updateHolding(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable Long id,
		@Valid @RequestBody HoldingRequest request
	) {
		return ApiResponse.success(holdingService.updateHolding(userDetails.getUser(), id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> deleteHolding(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable Long id
	) {
		holdingService.deleteHolding(userDetails.getUser(), id);
		return ApiResponse.success(null);
	}

	@GetMapping("/diagnosis")
	public ApiResponse<HoldingSummaryResponse> diagnose(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success(holdingService.diagnose(userDetails.getUser()));
	}

	@PostMapping("/extract")
	public ApiResponse<List<HoldingExtractResponse>> extractFromImage(
		@RequestParam("image") MultipartFile file
	) {
		return ApiResponse.success(holdingExtractService.extract(file));
	}
}
