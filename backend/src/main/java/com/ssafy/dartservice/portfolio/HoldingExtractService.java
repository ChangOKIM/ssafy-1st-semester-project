package com.ssafy.dartservice.portfolio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dartservice.global.exception.BusinessException;
import com.ssafy.dartservice.global.exception.ErrorCode;
import com.ssafy.dartservice.portfolio.dto.ExtractedHoldingDto;
import com.ssafy.dartservice.portfolio.dto.HoldingExtractResponse;
import com.ssafy.dartservice.portfolio.dto.HoldingExtractResponse.CandidateDto;
import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import com.ssafy.dartservice.stock.StockMapper;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class HoldingExtractService {

	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

	private static final String SYSTEM_PROMPT = """
		당신은 증권사 앱 보유종목 스크린샷에서 정보를 추출하는 도구입니다.
		이미지에 있는 각 종목의 종목명, 보유수량, 매입단가(평균단가)를 추출하세요.

		규칙:
		- 평가금액, 손익, 수익률 등 매입 기준이 아닌 값은 무시하세요.
		- 읽을 수 없는 값은 null로 두세요.
		- 설명, 마크다운, 코드블록 없이 JSON 결과만 반환하세요.

		응답은 반드시 아래 JSON 배열 형식만 반환하세요:
		[
		  { "name": "삼성전자", "quantity": 10, "avgPrice": 75000 }
		]
		""";

	private final ChatClient chatClient;
	private final StockMapper stockMapper;
	private final ObjectMapper objectMapper;

	public HoldingExtractService(
			@Qualifier("openaiChatClient") ChatClient chatClient,
			StockMapper stockMapper,
			ObjectMapper objectMapper
	) {
		this.chatClient = chatClient;
		this.stockMapper = stockMapper;
		this.objectMapper = objectMapper;
	}

	public List<HoldingExtractResponse> extract(MultipartFile file) {
		validate(file);

		List<ExtractedHoldingDto> extracted = callLlm(file);
		if (extracted == null || extracted.isEmpty()) {
			throw new BusinessException(ErrorCode.EXTRACT_FAILED);
		}

		return extracted.stream()
				.map(this::mapToResponse)
				.toList();
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_IMAGE);
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new BusinessException(ErrorCode.INVALID_IMAGE);
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
		}
	}


	private List<ExtractedHoldingDto> callLlm(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			log.warn("[추출 실패] 요청된 파일이 비어있습니다.");
			throw new IllegalArgumentException("파일이 비어있거나 존재하지 않습니다.");
		}

		try {
			log.info("[추출] 파일 수신 완료: name={}, size={}, contentType={}",
					file.getOriginalFilename(), file.getSize(), file.getContentType());

			String contentTypeStr = file.getContentType();
			if (contentTypeStr == null || contentTypeStr.isBlank()) {
				log.error("[추출 실패] 파일의 Content-Type이 없습니다.");
				throw new IllegalArgumentException("파일의 Content-Type을 확인할 수 없습니다.");
			}

			MimeType mimeType = MimeType.valueOf(contentTypeStr);

			org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
				@Override
				public String getFilename() {
					return (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "image.png";
				}
			};

			log.info("[추출] LLM 호출 시작...");
			String content = chatClient.prompt()
					.advisors(new SimpleLoggerAdvisor())
					.system(SYSTEM_PROMPT)
					.user(u -> u
							.text("이 스크린샷에서 보유종목을 추출해줘. JSON 배열만 반환해.")
							.media(mimeType, resource))
					.call()
					.content();

			// 💡 디버깅용 로그 필수!
			log.info("[추출] LLM 원본 응답 완료");
			log.debug("[추출] LLM 원본 내용:\n{}", content);

			if (content == null || content.isBlank()) {
				throw new RuntimeException("LLM 응답 결과가 비어있습니다.");
			}

			String strippedContent = stripFences(content);
			log.debug("[추출] 정제된 JSON 내용:\n{}", strippedContent);

			return objectMapper.readValue(
					strippedContent,
					new TypeReference<List<ExtractedHoldingDto>>() {});

		} catch (IllegalArgumentException e) {
			throw e; // 잘못된 아규먼트는 그대로 전파
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("[추출 실패] JSON 파싱 실패", e);
			throw new RuntimeException("LLM 응답을 객체로 변환하는데 실패했습니다.", e);
		} catch (Exception e) {
			log.error("[추출 실패] 알 수 없는 서버 에러", e);
			throw new RuntimeException("종목 추출 중 서버 오류가 발생했습니다.", e);
		}
	}

	/** 모델이 가끔 ```json 으로 감싸는 경우 방어 */
	private String stripFences(String content) {
		if (content == null) {
			return "";
		}
		String trimmed = content.trim();
		if (trimmed.startsWith("```")) {
			trimmed = trimmed
					.replaceAll("(?s)^```(?:json)?\\s*", "")
					.replaceAll("\\s*```$", "");
		}
		return trimmed;
	}

	private HoldingExtractResponse mapToResponse(ExtractedHoldingDto dto) {
		List<StockSearchResponseDto> exact = stockMapper.findByExactName(dto.name());
		if (exact.size() == 1) {
			return new HoldingExtractResponse(dto.name(), exact.get(0).getStockCode(), dto.quantity(), dto.avgPrice(), null);
		}

		List<StockSearchResponseDto> like = stockMapper.findByNameLike(dto.name());
		if (like.size() == 1) {
			return new HoldingExtractResponse(dto.name(), like.get(0).getStockCode(), dto.quantity(), dto.avgPrice(), null);
		}

		List<CandidateDto> candidates = like.stream()
				.map(s -> new CandidateDto(s.getStockCode(), s.getStockName()))
				.toList();

		return new HoldingExtractResponse(dto.name(), null, dto.quantity(), dto.avgPrice(),
				candidates.isEmpty() ? null : candidates);
	}
}