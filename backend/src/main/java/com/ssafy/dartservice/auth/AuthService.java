package com.ssafy.dartservice.auth;

import com.ssafy.dartservice.auth.dto.AuthResponse;
import com.ssafy.dartservice.auth.dto.LoginRequest;
import com.ssafy.dartservice.auth.dto.SignupRequest;
import com.ssafy.dartservice.global.exception.BusinessException;
import com.ssafy.dartservice.global.exception.ErrorCode;
import com.ssafy.dartservice.global.security.JwtTokenProvider;
import com.ssafy.dartservice.user.User;
import com.ssafy.dartservice.user.UserRepository;
import com.ssafy.dartservice.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		User user = User.create(email, passwordEncoder.encode(request.password()), request.name());
		User savedUser = userRepository.save(user);
		return createAuthResponse(savedUser);
	}

	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		return createAuthResponse(user);
	}

	private AuthResponse createAuthResponse(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(user);
		return new AuthResponse("Bearer", accessToken, jwtTokenProvider.getAccessTokenValiditySeconds(), UserResponse.from(user));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
