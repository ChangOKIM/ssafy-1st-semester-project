package com.ssafy.dartservice.user;

import com.ssafy.dartservice.global.response.ApiResponse;
import com.ssafy.dartservice.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return ApiResponse.success(UserResponse.from(userDetails.getUser()));
	}

	@PutMapping("/me")
	public ApiResponse<UserResponse> updateMe(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody UserUpdateRequest request
	) {
		return ApiResponse.success(userService.updateMe(userDetails.getUser(), request));
	}
}
