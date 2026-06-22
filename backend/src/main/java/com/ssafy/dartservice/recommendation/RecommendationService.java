package com.ssafy.dartservice.recommendation;

import com.ssafy.dartservice.investor.InvestorProfile;
import com.ssafy.dartservice.investor.InvestorProfileRepository;
import com.ssafy.dartservice.recommendation.dto.RecommendItem;
import com.ssafy.dartservice.recommendation.dto.RecommendResponse;
import com.ssafy.dartservice.recommendation.dto.RecommendSaveDto;
import com.ssafy.dartservice.recommendation.dto.ScoringInput;
import com.ssafy.dartservice.recommendation.dto.SectorRecommend;
import com.ssafy.dartservice.recommendation.score.RecommendScorer;
import com.ssafy.dartservice.user.User;
import com.ssafy.dartservice.user.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserRepository userRepository;
    private final InvestorProfileRepository profileRepository;
    private final RecommendationMapper recommendationMapper;
    private final RecommendScorer scorer;

    private static final int OVERALL_TOP_N = 10;
    private static final int SECTOR_TOP_N = 3;

    public RecommendResponse recommend(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return recommend(user);
    }

    public RecommendResponse recommend(User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        InvestorProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("투자자 프로필을 찾을 수 없습니다."));

        List<String> userSectors = splitCsv(profile.getPreferredSectors());
        List<String> userGoals = splitCsv(profile.getInvestmentGoals());
        String userRisk = profile.getRiskTolerance();

        List<RecommendItem> pool = recommendationMapper.findAllWithFinancials();
        log.info("추천 풀 로드 - 종목 수: {}개 (debtRatio null 제외 후)", pool.size());

        List<RecommendItem> allScored = new ArrayList<>();
        for (RecommendItem item : pool) {
            if (scoreItem(item, userSectors, userRisk, userGoals)) {
                allScored.add(item);
            }
        }
        log.info("점수 계산 완료 - 성공: {}개 / 전체: {}개", allScored.size(), pool.size());

        allScored.sort(Comparator.comparingDouble(RecommendItem::getScore).reversed());

        List<RecommendItem> overall = allScored.stream()
                .limit(OVERALL_TOP_N)
                .toList();
        log.info("전체 TOP{}: {}", OVERALL_TOP_N,
                overall.stream().map(i -> i.getStockCode() + "(" + String.format("%.1f", i.getScore()) + ")").toList());

        List<SectorRecommend> bySector = new ArrayList<>();
        for (String sector : userSectors) {
            List<RecommendItem> top3 = allScored.stream()
                    .filter(item -> sector.equals(item.getSector()))
                    .limit(SECTOR_TOP_N)
                    .toList();
            if (!top3.isEmpty()) {
                bySector.add(new SectorRecommend(sector, top3));
                log.info("섹터 [{}] TOP{}: {}", sector, SECTOR_TOP_N,
                        top3.stream().map(i -> i.getStockCode() + "(" + String.format("%.1f", i.getScore()) + ")").toList());
            } else {
                log.info("섹터 [{}] 해당 종목 없음", sector);
            }
        }

        saveRecommendations(user.getId(), overall, bySector);

        return new RecommendResponse(overall, bySector);
    }

    private boolean scoreItem(RecommendItem item, List<String> userSectors, String userRisk, List<String> userGoals) {
        try {
            ScoringInput input = new ScoringInput(
                    item.getStockCode(),
                    item.getSector(),
                    item.getDebtRatio(),
                    item.getInterestCoverage(),
                    item.getCurrentRatio(),
                    item.getRoe(),
                    item.getOperatingMargin(),
                    userSectors,
                    userRisk,
                    userGoals
            );
            item.setScore(scorer.score(input));
            return true;
        } catch (Exception e) {
            log.warn("추천 점수 계산 실패 - {}: {}", item.getStockCode(), e.getMessage());
            return false;
        }
    }

    private void saveRecommendations(Long userId, List<RecommendItem> overall, List<SectorRecommend> bySector) {
        List<RecommendSaveDto> saves = new ArrayList<>();
        for (RecommendItem item : overall) {
            saves.add(new RecommendSaveDto(userId, item.getStockCode(), "OVERALL", item.getScore()));
        }
        for (SectorRecommend sr : bySector) {
            for (RecommendItem item : sr.items()) {
                saves.add(new RecommendSaveDto(userId, item.getStockCode(), "SECTOR", item.getScore()));
            }
        }
        if (!saves.isEmpty()) {
            recommendationMapper.deleteByUserId(userId);
            recommendationMapper.insertRecommendations(saves);
            log.info("추천 저장 완료 - userId: {}, {}건", userId, saves.size());
        }
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
