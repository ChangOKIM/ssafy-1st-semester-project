package com.ssafy.dartservice.investor;

import com.ssafy.dartservice.global.exception.BusinessException;
import com.ssafy.dartservice.global.exception.ErrorCode;
import com.ssafy.dartservice.investor.dto.InvestorProfileRequest;
import com.ssafy.dartservice.investor.dto.InvestorProfileResponse;
import com.ssafy.dartservice.user.User;
import com.ssafy.dartservice.user.UserRepository;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestorProfileService {

	private final InvestorProfileRepository investorProfileRepository;
	private final UserRepository userRepository;

	public InvestorProfileService(InvestorProfileRepository investorProfileRepository, UserRepository userRepository) {
		this.investorProfileRepository = investorProfileRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public InvestorProfileResponse save(User authenticatedUser, InvestorProfileRequest request) {
		// JWT 인증이 완전히 연결되면 authenticatedUser만 사용합니다.
		// 현재는 초기 테스트 편의를 위해 요청 body의 userId도 예비 경로로 허용합니다.
		User user = resolveUser(authenticatedUser, request.userId());

		// 현재 preferredSectors는 "반도체,금융,IT" 형태의 콤마 구분 문자열로 저장합니다.
		// 검색/통계/섹터별 추천이 중요해지면 별도 users_profile_sectors 테이블로 분리하는 편이 좋습니다.
		String preferredSectors = request.preferredSectors()
			.stream()
			.map(String::trim)
			.filter(value -> !value.isBlank())
			.distinct()
			.collect(Collectors.joining(","));

		InvestorProfile profile = investorProfileRepository.findByUser(user).orElse(null);

		if (profile == null) {
			profile = InvestorProfile.create(
				user,
				request.investmentExperience(),
				request.riskTolerance(),
				request.investmentGoal(),
				request.investableAmount(),
				preferredSectors
			);
			investorProfileRepository.insert(profile);
		} else {
			profile.update(
				request.investmentExperience(),
				request.riskTolerance(),
				request.investmentGoal(),
				request.investableAmount(),
				preferredSectors
			);
			investorProfileRepository.update(profile);
		}

		profile.setUser(user);
		return InvestorProfileResponse.from(profile);
	}

	private User resolveUser(User authenticatedUser, Long requestedUserId) {
		if (authenticatedUser != null) {
			return authenticatedUser;
		}

		if (requestedUserId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return userRepository.findById(requestedUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
}
