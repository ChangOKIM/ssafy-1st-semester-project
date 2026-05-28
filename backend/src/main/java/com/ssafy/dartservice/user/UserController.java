package com.ssafy.dartservice.user;

import com.ssafy.dartservice.global.response.ApiResponse;
import com.ssafy.dartservice.global.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	@GetMapping("/me")
	public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success(UserResponse.from(userDetails.getUser()));
	}
}
