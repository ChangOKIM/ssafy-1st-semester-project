package com.ssafy.dartservice.auth;

import com.ssafy.dartservice.auth.dto.AuthResponse;
import com.ssafy.dartservice.auth.dto.LoginRequest;
import com.ssafy.dartservice.auth.dto.SignupRequest;
import com.ssafy.dartservice.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.success(authService.signup(request));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@GetMapping("/check-email")
	public ApiResponse<Map<String, Boolean>> checkEmail(@RequestParam String email) {
		return ApiResponse.success(Map.of("available", authService.isEmailAvailable(email)));
	}
}
