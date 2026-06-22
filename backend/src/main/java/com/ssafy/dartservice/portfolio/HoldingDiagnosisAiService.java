package com.ssafy.dartservice.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dartservice.investor.InvestorProfile;
import com.ssafy.dartservice.portfolio.dto.HoldingResponse;
import com.ssafy.dartservice.portfolio.dto.HoldingSummaryResponse.DiagnosisSection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldingDiagnosisAiService {

	private final ChatClient.Builder chatClientBuilder;
	private final ObjectMapper objectMapper;

	public List<DiagnosisSection> diagnose(
		InvestorProfile profile,
		List<HoldingResponse> holdings,
		BigDecimal totalPurchase,
		BigDecimal totalEvaluation,
		BigDecimal totalProfit,
		BigDecimal totalRate
	) {
		if (holdings.isEmpty()) {
			return List.of(new DiagnosisSection(
				"진단 준비",
				"보유 종목을 먼저 등록하면 투자성향과 실제 보유 종목을 함께 비교해 드립니다.",
				List.of("보유 종목 추가", "매입가와 수량 입력", "투자성향 저장")
			));
		}

		try {
			DiagnosisInput input = new DiagnosisInput(
				profile == null ? null : ProfileSnapshot.from(profile),
				new PortfolioSnapshot(totalPurchase, totalEvaluation, totalProfit, totalRate),
				holdings.stream().map(holding -> DiagnosisHolding.from(holding, totalEvaluation)).toList()
			);
			String json = objectMapper.writeValueAsString(input);
			String content = chatClientBuilder.build()
				.prompt()
				.system(SYSTEM_PROMPT)
				.user(json)
				.call()
				.content();

			DiagnosisResult result = objectMapper.readValue(content, DiagnosisResult.class);
			if (result.sections() == null || result.sections().isEmpty()) {
				return fallback(profile, totalRate);
			}
			return result.sections();
		} catch (Exception e) {
			log.warn("AI 투자 진단 실패: {}", e.getMessage());
			return fallback(profile, totalRate);
		}
	}

	private List<DiagnosisSection> fallback(InvestorProfile profile, BigDecimal totalRate) {
		String risk = profile == null ? "투자성향 미등록" : profile.getRiskTolerance();
		String sectors = profile == null ? "관심 분야 미등록" : String.join(", ", splitCsv(profile.getPreferredSectors()));
		return List.of(
			new DiagnosisSection(
				"성향 대비 적합성",
				"현재 위험 성향은 " + risk + "이고 관심 분야는 " + sectors + "입니다. 보유 종목의 업종과 손익 구조가 이 성향과 맞는지 우선 확인해야 합니다.",
				List.of("선호 업종과 실제 보유 업종 비교", "손실 종목 비중 확인", "감내 가능한 손실 폭과 현재 손익 비교")
			),
			new DiagnosisSection(
				"수익률 해석",
				"현재 보유 종목 기준 전체 수익률은 " + totalRate.toPlainString() + "%입니다. 전체 수익률 하나보다 어떤 종목과 업종이 성과를 만들었는지 나눠 보는 것이 중요합니다.",
				List.of("수익 기여 종목 확인", "손실 기여 종목 확인", "특정 종목 의존도 점검")
			),
			new DiagnosisSection(
				"다음 점검 가이드",
				"이 내용은 입력된 보유 종목과 투자성향을 바탕으로 한 참고용 진단입니다. 실제 투자 판단 전에는 종목별 비중, 투자 기간, 손실 감내 가능 범위를 함께 확인하세요.",
				List.of("종목별 평가금액 비중 계산", "투자 목표와 보유 기간 비교", "추가 매수/매도 전 최근 공시 확인")
			)
		);
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

	private static final String SYSTEM_PROMPT = """
		당신은 초보 투자자의 투자성향과 실제 보유 종목을 비교해서 설명하는 포트폴리오 진단가입니다.
		반드시 사용자가 제공한 JSON 데이터만 근거로 한국어 진단을 작성하세요.

		목표:
		- 단순 요약이 아니라 "이 사람의 투자성향 대비 현재 보유종목이 어떤 상태인지" 판단하세요.
		- 각 판단에는 JSON 데이터에서 확인 가능한 근거를 붙이세요.
		- 사용자가 다음에 무엇을 확인하면 좋은지 체크리스트 형태로 안내하세요.

		반드시 다룰 내용:
		1. 투자성향 적합성
		   - riskTolerance가 LOW면 손실/변동성 부담이 큰 상태인지 짚으세요.
		   - riskTolerance가 HIGH면 공격적 성향 대비 보유 종목이 너무 보수적인지 또는 쏠림이 큰지 짚으세요.
		   - investmentGoals와 현재 손익 구조가 어울리는지 판단하세요.
		2. 관심 업종과 실제 보유 업종 비교
		   - preferredSectors와 holdings[].sector를 비교하세요.
		   - 관심 업종이 아닌 종목이 많으면 그 사실을 말하세요.
		   - 같은 업종이 반복되면 업종 집중이라고 설명하세요.
		3. 손익 구조
		   - totalProfitRate만 말하지 말고 holdings별 profitRate, profitAmount를 비교하세요.
		   - 수익/손실을 가장 크게 만든 종목명을 언급하세요.
		   - currentPrice가 null이면 현재가 조회가 안 된 종목은 평가가 제한된다고 말하세요.
		4. 가이드라인
		   - 매수/매도 지시가 아니라 점검 질문을 주세요.
		   - 각 guideItems는 사용자가 바로 확인할 수 있는 행동이어야 합니다.

		금지:
		- 매수/매도 추천 금지
		- 목표가, 미래 가격 예측 금지
		- JSON에 없는 뉴스/공시/시장 상황 추측 금지
		- "분산투자하세요" 같은 일반론만 반복 금지
		- 투자 조언처럼 단정 금지

		응답은 반드시 아래 JSON 형식만 반환하세요. JSON 밖에 설명, 마크다운, 코드블록을 쓰지 마세요.
		{
		  "sections": [
		    {
		      "title": "성향 대비 적합성",
		      "content": "투자성향과 현재 보유종목이 얼마나 맞는지, 근거를 포함해 3~4문장으로 작성",
		      "guideItems": ["점검 행동 1", "점검 행동 2", "점검 행동 3"]
		    },
		    {
		      "title": "보유종목에서 보이는 특징",
		      "content": "업종 일치도, 업종 집중, 수익/손실 기여 종목을 3~4문장으로 작성",
		      "guideItems": ["점검 행동 1", "점검 행동 2", "점검 행동 3"]
		    },
		    {
		      "title": "다음 점검 가이드",
		      "content": "사용자가 다음에 확인할 기준을 3~4문장으로 작성하고, 참고용 진단이라는 점을 자연스럽게 포함",
		      "guideItems": ["점검 행동 1", "점검 행동 2", "점검 행동 3"]
		    }
		  ]
		}
		""";

	private record DiagnosisInput(
		ProfileSnapshot investorProfile,
		PortfolioSnapshot portfolio,
		List<DiagnosisHolding> holdings
	) {
	}

	private record ProfileSnapshot(
		String investmentExperience,
		String riskTolerance,
		List<String> investmentGoals,
		List<String> preferredSectors
	) {
		static ProfileSnapshot from(InvestorProfile profile) {
			return new ProfileSnapshot(
				profile.getInvestmentExperience(),
				profile.getRiskTolerance(),
				split(profile.getInvestmentGoals()),
				split(profile.getPreferredSectors())
			);
		}

		private static List<String> split(String csv) {
			if (csv == null || csv.isBlank()) {
				return List.of();
			}
			return Arrays.stream(csv.split(","))
				.map(String::trim)
				.filter(value -> !value.isBlank())
				.toList();
		}
	}

	private record PortfolioSnapshot(
		BigDecimal totalPurchaseAmount,
		BigDecimal totalEvaluationAmount,
		BigDecimal totalProfitAmount,
		BigDecimal totalProfitRate
	) {
	}

	private record DiagnosisHolding(
		String stockCode,
		String stockName,
		String sector,
		Integer quantity,
		BigDecimal purchaseAmount,
		BigDecimal evaluationAmount,
		BigDecimal portfolioWeight,
		BigDecimal currentPrice,
		BigDecimal profitAmount,
		BigDecimal profitRate
	) {
		static DiagnosisHolding from(HoldingResponse holding, BigDecimal totalEvaluation) {
			BigDecimal evaluation = holding.evaluationAmount() == null ? holding.purchaseAmount() : holding.evaluationAmount();
			BigDecimal weight = BigDecimal.ZERO;
			if (evaluation != null && totalEvaluation != null && totalEvaluation.compareTo(BigDecimal.ZERO) > 0) {
				weight = evaluation
					.multiply(BigDecimal.valueOf(100))
					.divide(totalEvaluation, 2, RoundingMode.HALF_UP);
			}
			return new DiagnosisHolding(
				holding.stockCode(),
				holding.stockName(),
				holding.sector(),
				holding.quantity(),
				holding.purchaseAmount(),
				evaluation,
				weight,
				holding.currentPrice(),
				holding.profitAmount(),
				holding.profitRate()
			);
		}
	}

	private record DiagnosisResult(
		List<DiagnosisSection> sections
	) {
	}
}
