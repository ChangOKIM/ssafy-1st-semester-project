package com.ssafy.dartservice.auth.dto;

import com.ssafy.dartservice.user.UserResponse;

public record AuthResponse(
	String tokenType,
	String accessToken,
	long expiresIn,
	UserResponse user
) {
}
