package com.ssafy.dartservice.recommendation;

import com.ssafy.dartservice.investor.InvestorProfile;
import com.ssafy.dartservice.investor.InvestorProfileRepository;
import com.ssafy.dartservice.recommendation.dto.RecommendItem;
import com.ssafy.dartservice.recommendation.dto.RecommendResponse;
import com.ssafy.dartservice.recommendation.dto.ScoringInput;
import com.ssafy.dartservice.recommendation.dto.SectorRecommend;
import com.ssafy.dartservice.recommendation.dto.StockInfo;
import com.ssafy.dartservice.recommendation.score.RecommendScorer;
import com.ssafy.dartservice.stock.FinancialService;
import com.ssafy.dartservice.stock.dto.FinancialResponseDto;
import com.ssafy.dartservice.user.User;
import com.ssafy.dartservice.user.UserRepository;
import java.time.LocalDate;
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
    private final FinancialService financialService;
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

        List<StockInfo> pool = recommendationMapper.findAll();
        List<RecommendItem> allScored = new ArrayList<>();
        for (StockInfo stock : pool) {
            RecommendItem item = scoreStock(stock, userSectors, userRisk, userGoals);
            if (item != null) {
                allScored.add(item);
            }
        }

        allScored.sort(Comparator.comparingDouble(RecommendItem::score).reversed());

        List<RecommendItem> overall = allScored.stream()
                .limit(OVERALL_TOP_N)
                .toList();

        List<SectorRecommend> bySector = new ArrayList<>();
        for (String sector : userSectors) {
            List<RecommendItem> top3 = allScored.stream()
                    .filter(item -> sector.equals(item.sector()))
                    .limit(SECTOR_TOP_N)
                    .toList();
            if (!top3.isEmpty()) {
                bySector.add(new SectorRecommend(sector, top3));
            }
        }

        return new RecommendResponse(overall, bySector);
    }

    private RecommendItem scoreStock(
            StockInfo stock,
            List<String> userSectors,
            String userRisk,
            List<String> userGoals
    ) {
        try {
            int latestYear = LocalDate.now().getYear() - 1;
            FinancialResponseDto fin = latestFinancial(stock.getStockCode(), latestYear);
            if (fin == null) {
                return null;
            }

            ScoringInput input = new ScoringInput(
                    stock.getStockCode(),
                    stock.getSector(),
                    fin.getDebtRatio(),
                    fin.getInterestCoverage(),
                    fin.getCurrentRatio(),
                    fin.getRoe(),
                    fin.getOperatingMargin(),
                    userSectors,
                    userRisk,
                    userGoals
            );
            double score = scorer.score(input);
            return new RecommendItem(stock.getStockCode(), stock.getSector(), score);
        } catch (Exception e) {
            log.warn("추천 점수 계산 실패 - {}: {}", stock.getStockCode(), e.getMessage());
            return null;
        }
    }

    private FinancialResponseDto latestFinancial(String code, int year) {
        List<FinancialResponseDto> list = financialService.getFinancials(code, year);
        return list.stream()
                .max(Comparator.comparingInt(FinancialResponseDto::getBaseYear))
                .orElse(null);
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
