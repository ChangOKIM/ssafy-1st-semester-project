package com.ssafy.dartservice.portfolio;

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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
		int deleted = holdingRepository.deleteByIdAndUserId(id, user.getId());
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
		}
	}

	@Transactional(readOnly = true)
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
		List<DiagnosisSection> sections = holdingDiagnosisAiService.diagnose(
			profile,
			holdings,
			totalPurchase,
			totalEvaluation,
			totalProfit,
			totalRate
		);

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
