package com.ssafy.dartservice.user;

import com.ssafy.dartservice.global.exception.BusinessException;
import com.ssafy.dartservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional
	public UserResponse updateMe(User user, UserUpdateRequest request) {
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		String email = request.email().trim().toLowerCase();
		if (userRepository.existsByEmailExceptId(email, user.getId())) {
			throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
		}

		user.setEmail(email);
		user.setName(request.name().trim());
		userRepository.updateProfile(user);
		return UserResponse.from(user);
	}
}
