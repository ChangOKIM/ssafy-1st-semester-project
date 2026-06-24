package com.ssafy.dartservice.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dartservice.global.exception.BusinessException;
import com.ssafy.dartservice.global.exception.ErrorCode;
import com.ssafy.dartservice.investor.InvestorProfile;
import com.ssafy.dartservice.investor.InvestorProfileRepository;
import com.ssafy.dartservice.portfolio.dto.HoldingRequest;
import com.ssafy.dartservice.portfolio.dto.HoldingResponse;
import com.ssafy.dartservice.portfolio.dto.HoldingSummaryResponse;
import com.ssafy.dartservice.portfolio.dto.HoldingSummaryResponse.DiagnosisSection;
import com.ssafy.dartservice.stock.StockService;
import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import com.ssafy.dartservice.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldingService {

	private final HoldingRepository holdingRepository;
	private final StockService stockService;
	private final HoldingDiagnosisAiService holdingDiagnosisAiService;
	private final InvestorProfileRepository investorProfileRepository;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public List<HoldingResponse> getHoldings(User user) {
		validateUser(user);
		List<Holding> holdings = holdingRepository.findAllByUserId(user.getId());

		List<CompletableFuture<HoldingResponse>> futures = holdings.stream()
			.map(h -> CompletableFuture.supplyAsync(() -> toResponse(h)))
			.toList();

		return futures.stream()
			.map(CompletableFuture::join)
			.toList();
	}

	@Transactional
	public HoldingResponse createHolding(User user, HoldingRequest request) {
		validateUser(user);
		validateStock(request.stockCode());

		holdingRepository.deleteDiagnosis(user.getId());

		Optional<Holding> existing = holdingRepository.findByUserIdAndStockCode(
			user.getId(), request.stockCode().trim());

		if (existing.isPresent()) {
			Holding old = existing.get();
			int newQty = old.getQuantity() + request.quantity();
			BigDecimal weightedPrice = old.getPurchasePrice()
				.multiply(BigDecimal.valueOf(old.getQuantity()))
				.add(request.purchasePrice().multiply(BigDecimal.valueOf(request.quantity())))
				.divide(BigDecimal.valueOf(newQty), 0, RoundingMode.HALF_UP);

			old.setQuantity(newQty);
			old.setPurchasePrice(weightedPrice);
			holdingRepository.update(old);

			return holdingRepository.findByIdAndUserId(old.getId(), user.getId())
				.map(this::toResponse)
				.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
		}

		Holding holding = new Holding();
		holding.setUserId(user.getId());
		applyRequest(holding, request);
		holdingRepository.insert(holding);

		return holdingRepository.findByIdAndUserId(holding.getId(), user.getId())
			.map(this::toResponse)
			.orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	@Transactional
	public HoldingResponse updateHolding(User user, Long id, HoldingRequest request) {
		validateUser(user);
		validateStock(request.stockCode());

		holdingRepository.deleteDiagnosis(user.getId());

		Holding holding = holdingRepository.findByIdAndUserId(id, user.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
		applyRequest(holding, request);

		int updated = holdingRepository.update(holding);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
		}

		return holdingRepository.findByIdAndUserId(id, user.getId())
			.map(this::toResponse)
			.orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
	}

	@Transactional
	public void deleteHolding(User user, Long id) {
		validateUser(user);

		holdingRepository.deleteDiagnosis(user.getId());

		int deleted = holdingRepository.deleteByIdAndUserId(id, user.getId());
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
		}
	}

	@Transactional
	public HoldingSummaryResponse diagnose(User user) {
		List<HoldingResponse> holdings = getHoldings(user);
		BigDecimal totalPurchase = sum(holdings.stream().map(HoldingResponse::purchaseAmount).toList());
		BigDecimal totalEvaluation = sum(holdings.stream()
			.map(holding -> holding.evaluationAmount() == null ? holding.purchaseAmount() : holding.evaluationAmount())
			.toList());
		BigDecimal totalProfit = totalEvaluation.subtract(totalPurchase);
		BigDecimal totalRate = rate(totalProfit, totalPurchase);

		String summary = holdings.isEmpty()
			? "등록된 보유 종목이 없습니다."
			: String.format(
				"총 %d개 종목을 보유 중이며 매입금액은 %s원, 평가금액은 %s원, 전체 수익률은 %s%%입니다.",
				holdings.size(),
				totalPurchase.toPlainString(),
				totalEvaluation.toPlainString(),
				totalRate.toPlainString()
			);

		InvestorProfile profile = investorProfileRepository.findByUser(user).orElse(null);
		String hash = computeHash(holdings, profile);
		String cachedJson = holdingRepository.findCachedDiagnosis(user.getId(), hash);

		List<DiagnosisSection> sections;
		if (cachedJson != null) {
			try {
				sections = objectMapper.readValue(cachedJson,
					objectMapper.getTypeFactory().constructCollectionType(List.class, DiagnosisSection.class));
				log.info("AI 포트폴리오 진단 캐시 히트 - userId: {}", user.getId());
			} catch (Exception e) {
				log.warn("캐시 파싱 실패, AI 재호출 - userId: {}", user.getId());
				sections = generateAndCache(profile, holdings, totalPurchase, totalEvaluation, totalProfit, totalRate, user.getId(), hash);
			}
		} else {
			sections = generateAndCache(profile, holdings, totalPurchase, totalEvaluation, totalProfit, totalRate, user.getId(), hash);
		}

		return new HoldingSummaryResponse(
			holdings.size(),
			totalPurchase,
			totalEvaluation,
			totalProfit,
			totalRate,
			summary,
			sections
		);
	}

	private List<DiagnosisSection> generateAndCache(
		InvestorProfile profile, List<HoldingResponse> holdings,
		BigDecimal totalPurchase, BigDecimal totalEvaluation,
		BigDecimal totalProfit, BigDecimal totalRate,
		Long userId, String hash
	) {
		List<DiagnosisSection> sections = holdingDiagnosisAiService.diagnose(
			profile, holdings, totalPurchase, totalEvaluation, totalProfit, totalRate);
		try {
			String json = objectMapper.writeValueAsString(sections);
			holdingRepository.saveDiagnosis(userId, hash, json);
			log.info("AI 포트폴리오 진단 캐시 저장 - userId: {}", userId);
		} catch (Exception e) {
			log.warn("진단 캐시 저장 실패 - userId: {}: {}", userId, e.getMessage());
		}
		return sections;
	}

	private String computeHash(List<HoldingResponse> holdings, InvestorProfile profile) {
		String holdingsStr = holdings.stream()
			.sorted(Comparator.comparing(HoldingResponse::stockCode))
			.map(h -> h.stockCode() + ":" + h.quantity() + ":" + h.purchasePrice().toPlainString())
			.collect(Collectors.joining(","));

		String profileStr = profile == null ? "none"
			: profile.getInvestmentExperience() + ":" + profile.getRiskTolerance() + ":"
			+ profile.getInvestmentGoals() + ":" + profile.getPreferredSectors();

		String raw = holdingsStr + "|" + profileStr;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			return String.valueOf(raw.hashCode());
		}
	}

	private void validateUser(User user) {
		if (user == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private void validateStock(String stockCode) {
		if (!holdingRepository.existsStock(stockCode)) {
			throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
		}
	}

	private void applyRequest(Holding holding, HoldingRequest request) {
		holding.setStockCode(request.stockCode().trim());
		holding.setQuantity(request.quantity());
		holding.setPurchasePrice(request.purchasePrice());
		holding.setPurchaseDate(request.purchaseDate());
	}

	private HoldingResponse toResponse(Holding holding) {
		BigDecimal quantity = BigDecimal.valueOf(holding.getQuantity());
		BigDecimal purchaseAmount = holding.getPurchasePrice().multiply(quantity);
		BigDecimal currentPrice = currentPrice(holding.getStockCode());
		BigDecimal evaluationAmount = currentPrice == null ? null : currentPrice.multiply(quantity);
		BigDecimal profitAmount = evaluationAmount == null ? null : evaluationAmount.subtract(purchaseAmount);
		BigDecimal profitRate = profitAmount == null ? null : rate(profitAmount, purchaseAmount);

		return new HoldingResponse(
			holding.getId(),
			holding.getStockCode(),
			holding.getStockName(),
			holding.getMarket(),
			holding.getSector(),
			holding.getQuantity(),
			holding.getPurchasePrice(),
			holding.getPurchaseDate(),
			purchaseAmount,
			currentPrice,
			evaluationAmount,
			profitAmount,
			profitRate
		);
	}

	private BigDecimal currentPrice(String stockCode) {
		try {
			StockPriceResponseDto price = stockService.getStockPrice(stockCode);
			return parsePrice(price.getCurrentPrice());
		} catch (Exception e) {
			log.warn("현재가 조회 실패 - {}: {}", stockCode, e.getMessage());
			return null;
		}
	}

	private BigDecimal parsePrice(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return new BigDecimal(value.replace(",", "").trim());
	}

	private BigDecimal rate(BigDecimal profitAmount, BigDecimal purchaseAmount) {
		if (purchaseAmount == null || purchaseAmount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return profitAmount
			.multiply(BigDecimal.valueOf(100))
			.divide(purchaseAmount, 2, RoundingMode.HALF_UP);
	}

	private BigDecimal sum(List<BigDecimal> values) {
		return values.stream()
			.filter(value -> value != null)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
