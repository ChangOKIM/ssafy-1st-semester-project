package com.ssafy.dartservice.recommendation;

import com.ssafy.dartservice.investor.InvestorProfile;
import com.ssafy.dartservice.investor.InvestorProfileRepository;
import com.ssafy.dartservice.recommendation.dto.*;
import com.ssafy.dartservice.recommendation.score.RecommendScorer;
import com.ssafy.dartservice.stock.FinancialService;
import com.ssafy.dartservice.stock.dto.FinancialResponseDto;
import com.ssafy.dartservice.user.User;
import com.ssafy.dartservice.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

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
        // 1. 사용자 프로필
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        InvestorProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("투자 프로필 없음"));

        List<String> userSectors = splitCsv(profile.getPreferredSectors());
        List<String> userGoals = splitCsv(profile.getInvestmentGoals());
        String userRisk = profile.getRiskTolerance();

        // 2. 전체 풀 점수 계산 (한 번만)
        List<StockInfo> pool = recommendationMapper.findAll();   // 금융 제외
        List<RecommendItem> allScored = new ArrayList<>();
        for (StockInfo stock : pool) {
            RecommendItem item = scoreStock(stock, userSectors, userRisk, userGoals);
            if (item != null) allScored.add(item);
        }
        // 점수순 정렬
        allScored.sort(Comparator.comparingDouble(RecommendItem::score).reversed());

        // 3. 블록 1 — 전체 TOP 10
        List<RecommendItem> overall = allScored.stream()
                .limit(OVERALL_TOP_N)
                .toList();

        // 4. 블록 2 — 관심섹터별 TOP 3
        List<SectorRecommend> bySector = new ArrayList<>();
        for (String sec : userSectors) {
            List<RecommendItem> top3 = allScored.stream()
                    .filter(item -> sec.equals(item.sector()))   // 그 섹터만
                    .limit(SECTOR_TOP_N)
                    .toList();
            if (!top3.isEmpty()) {
                bySector.add(new SectorRecommend(sec, top3));
            }
        }

        return new RecommendResponse(overall, bySector);
    }

    // 한 종목 점수 계산 (재무 없으면 null)
    private RecommendItem scoreStock(StockInfo stock, List<String> userSectors,
                                     String userRisk, List<String> userGoals) {
        try {
            int latestYear = LocalDate.now().getYear() - 1;
            FinancialResponseDto fin = latestFinancial(stock.getStockCode(), latestYear);
            if (fin == null) return null;

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
            log.warn("점수 계산 실패 - {}: {}", stock.getStockCode(), e.getMessage());
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
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).toList();
    }
}