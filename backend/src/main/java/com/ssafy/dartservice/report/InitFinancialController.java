package com.ssafy.dartservice.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InitFinancialController {  // ⚠️ 분포 분석용 임시 - 끝나면 삭제

    private final ReportService reportService;

    // 코스피 주요 종목 ~100개 (다양한 업종/재무 스펙트럼)
    private static final List<String> CODES = List.of(
            // 반도체/IT
            "005930","000660","000990","042700","402340","018260","012510","022100","034310","095340",
            // 자동차/부품
            "005380","000270","012330","161390","204320","011210","018880","204210","005490","004020",
            // 바이오/제약
            "207940","068270","000100","128940","185750","009420","069620","006280","000020","003520",
            // 2차전지/화학/에너지
            "373220","003670","096770","006400","051910","011170","010950","096775","285130","011780",
            // 플랫폼/게임/통신
            "035420","035720","259960","036570","251270","017670","030200","032640","078340","181710",
            // 소비재/유통/식품
            "097950","090430","051900","271560","004370","280360","007310","004990","000080","005300",
            // 산업재/조선/방산/기계
            "329180","012450","028260","034020","064350","009540","010140","042660","079550","267250",
            // 철강/소재
            "005490","004020","103140","014820","001230","002380","000880","011790","298050","010130",
            // 건설/유틸리티
            "000720","028050","006360","047040","015760","036460","051600","052690","000150","034220",
            // 금융(이번엔 포함 - 재무구조 다름, 분포 참고용)
            "105560","055550","086790","316140","024110","138930","071050","029780","006800","005940"
    );

    @PostMapping("/admin/init-financials")
    public String initFinancials() {
        int latestYear = LocalDate.now().getYear() - 1;
        int success = 0, fail = 0;
        StringBuilder failList = new StringBuilder();

        for (String code : CODES) {
            try {
                reportService.getFinancials(code, latestYear);
                success++;
                Thread.sleep(400);  // DART rate limit 방지
            } catch (Exception e) {
                log.error("실패 - {}: {}", code, e.getMessage());
                failList.append(code).append(" ");
                fail++;
            }
        }
        String result = String.format("완료 - 성공 %d, 실패 %d | 실패: %s",
                success, fail, failList);
        log.info(result);
        return result;
    }
}