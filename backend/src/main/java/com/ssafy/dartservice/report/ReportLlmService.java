package com.ssafy.dartservice.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dartservice.report.dto.ReportInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportLlmService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;  // Spring이 자동으로 제공

    public String generateReport(ReportInput input) {
        try {
            // 1. DTO → JSON 문자열
            String json = objectMapper.writeValueAsString(input);

            // 2. 프롬프트(규칙) + JSON(데이터) 함께 LLM 호출
            ChatClient client = chatClientBuilder.build();
            return client.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("다음 데이터로 분석해주세요:\n" + json)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("LLM 리포트 생성 실패 - 회사명: {}", input.회사명(), e);
            throw new RuntimeException("리포트 생성 중 오류가 발생했습니다.", e);
        }
    }

    private static final String SYSTEM_PROMPT = """
            당신은 주식 초보자에게 기업을 쉽게 설명하는 분석가입니다.
            아래에 주어진 숫자 데이터만 사용해 설명하세요.

            [절대 규칙]
            1. 주어진 데이터에 없는 내용은 절대 지어내지 마세요. 특히 뉴스,
               신제품, 경영진, 미래 전망 등은 데이터에 없으므로 언급 금지.
            2. "사세요/팔지 마세요/투자 추천" 같은 매수·매도 권유는 절대 하지 마세요.
               "~한 특징이 있어요"처럼 사실 서술로만 표현하세요.
            3. 계산하지 마세요. 주어진 숫자를 그대로 인용해 설명만 하세요.
            4. 데이터가 null이거나 없는 항목은 "정보 없음"으로 처리하고 추측 금지.
            5. 전문 용어는 한 줄 쉬운 설명을 붙이세요. (예: PER = 이익 대비 주가 배수)

            [출력 형식] — 아래 4개 항목, 각 2~3문장, 총 350자 내외
            1. 돈을 잘 버나요?  : 매출·영업이익 3년 추세 + 영업이익률
            2. 재무는 튼튼한가요? : 부채비율, 이자보상배율을 쉬운 말로
            3. 지금 주가 수준은?  : PER·PBR을 "업종 평균 대비" 관점으로
            4. 한 줄 정리        : 특징 요약 (매수 권유 금지)

            [말투]
            - 친근한 존댓말, 어려운 표현 없이
            - 숫자는 "약 12조 원"처럼 읽기 쉽게

            마지막에 반드시 추가: "본 정보는 참고용이며, 투자 판단과 책임은 본인에게 있습니다."
            """;
}