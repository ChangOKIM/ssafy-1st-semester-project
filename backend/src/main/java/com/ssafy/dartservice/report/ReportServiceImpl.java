package com.ssafy.dartservice.report;

import com.ssafy.dartservice.report.dto.ChartResponseDto;
import com.ssafy.dartservice.report.dto.FinancialResponseDto;
import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import com.ssafy.dartservice.report.financial.DartClient;
import com.ssafy.dartservice.report.financial.DartFinancialResponse;
import com.ssafy.dartservice.report.financial.StockFinancial;
import com.ssafy.dartservice.stock.kis.KisChartResponse;
import com.ssafy.dartservice.stock.kis.KisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final RestTemplate restTemplate;
    private final KisTokenService kisTokenService;
    private final DartClient dartClient;

    @Value("${kis.app-key}")
    private String kisAppKey;

    @Value("${kis.app-secret}")
    private String kisAppSecret;

    @Value("${kis.base-url}")
    private String kisBaseUrl;

    @Override
    public List<StockSearchResponseDto> searchStocks(String keyword) {
        log.info("종목 검색 요청 - keyword: {}", keyword);
        List<StockSearchResponseDto> result = reportMapper.searchByKeyword(keyword);
        log.info("종목 검색 결과 - {}건", result.size());
        return result;
    }

    @Override
    public List<ChartResponseDto> getStockChart(String stockCode, String period) {
        log.info("차트 조회 요청 - stockCode: {}, period: {}", stockCode, period);
        // period 검증 (D, W, M만 허용)
        if (!period.equals("D") && !period.equals("W") && !period.equals("M")) {
            period = "D";  // 기본값
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(kisBaseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .queryParam("FID_PERIOD_DIV_CODE", period)
                .queryParam("FID_ORG_ADJ_PRC", "1")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + kisTokenService.getAccessToken());
        headers.set("appkey", kisAppKey);
        headers.set("appsecret", kisAppSecret);
        headers.set("tr_id", "FHKST01010400");
        headers.set("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<KisChartResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                KisChartResponse.class
        );

        KisChartResponse body = response.getBody();

        if (body == null || body.getOutput() == null) {
            throw new RuntimeException("KIS 차트 조회 실패");
        }

        List<ChartResponseDto> result = new ArrayList<>();
        for (KisChartResponse.Output item : body.getOutput()) {
            result.add(new ChartResponseDto(
                    item.getStckBsopDate(),
                    item.getStckOprc(),
                    item.getStckHgpr(),
                    item.getStckLwpr(),
                    item.getStckClpr(),
                    item.getAcmlVol()
            ));
        }

        return result;
    }

    @Override
    public List<FinancialResponseDto> getFinancials(String stockCode, int latestYear) {
        log.info("재무 데이터 조회 요청 - stockCode: {}, latestYear: {}", stockCode, latestYear);
        // 1. 캐시 3년치 다 있는지 확인
        List<StockFinancial> cached = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            StockFinancial c = reportMapper.findFinancial(stockCode, latestYear - i);
            if (c != null) cached.add(c);
        }
        if (cached.size() == 3) {
            log.info("재무 데이터 캐시 히트 - stockCode: {}", stockCode);
            return cached.stream().map(FinancialResponseDto::from).toList();
        }

        // 2. 캐시 미스 → DART 1번 호출 (3년치 다 받음)
        String corpCode = reportMapper.findCorpCode(stockCode);
        log.info("재무 데이터 캐시 미스 - DART API 호출 - stockCode: {}, corpCode: {}", stockCode, corpCode);
        DartFinancialResponse res = dartClient.fetch(corpCode, latestYear);
        if (res == null || !"000".equals(res.getStatus())) {
            log.error("DART 응답 오류 - stockCode: {}, status: {}", stockCode, res != null ? res.getStatus() : "null");
            throw new IllegalStateException("DART 응답 오류");
        }

        // 3. 연도별 맵 3개 만들기 (당기/전기/전전기)
        Map<String, Long> thstrm = new HashMap<>();
        Map<String, Long> frmtrm = new HashMap<>();
        Map<String, Long> bfefrmtrm = new HashMap<>();
        for (DartFinancialResponse.Item item : res.getList()) {
            String key = item.getSjDiv() + ":" + item.getAccountId();
            putIfValid(thstrm, key, item.getThstrmAmount());
            putIfValid(frmtrm, key, item.getFrmtrmAmount());
            putIfValid(bfefrmtrm, key, item.getBfefrmtrmAmount());
        }
        thstrm.entrySet().stream()
                .filter(e -> e.getKey().startsWith("IS:") || e.getKey().startsWith("CIS:"))
                .forEach(e -> log.info("{} = {}", e.getKey(), e.getValue()));
        // 4. 3년치 빌드 + 저장
        List<StockFinancial> all = List.of(
                build(stockCode, latestYear,     thstrm),
                build(stockCode, latestYear - 1, frmtrm),
                build(stockCode, latestYear - 2, bfefrmtrm)
        );
        for (StockFinancial f : all) reportMapper.insertFinancial(f);
        log.info("재무 데이터 저장 완료 - stockCode: {}, {}년~{}년", stockCode, latestYear - 2, latestYear);

        return all.stream().map(FinancialResponseDto::from).toList();
    }

    // --- 헬퍼: 빈 값 아니면 맵에 넣기 ---
    private void putIfValid(Map<String, Long> map, String key, String raw) {
        Long v = parseAmount(raw);
        if (v != null) map.put(key, v);
    }

    // --- 헬퍼: 연도별 맵 하나로 StockFinancial 하나 만들기 ---
    private StockFinancial build(String stockCode, int year, Map<String, Long> amt) {
        StockFinancial f = new StockFinancial();
        f.setStockCode(stockCode);
        f.setBaseYear(year);
        f.setRevenue(         getAmount(amt, "ifrs-full_Revenue",          "IS", "CIS"));
        f.setOperatingProfit( getAmount(amt, "dart_OperatingIncomeLoss",   "IS", "CIS"));
        f.setNetIncome(       getAmount(amt, "ifrs-full_ProfitLoss",       "IS", "CIS"));
        f.setFinanceCosts(    getAmount(amt, "ifrs-full_FinanceCosts",     "IS", "CIS"));
        f.setTotalAssets(amt.get("BS:ifrs-full_Assets"));
        f.setTotalDebt(amt.get("BS:ifrs-full_Liabilities"));
        f.setTotalEquity(amt.get("BS:ifrs-full_Equity"));
        f.setCurrentAssets(amt.get("BS:ifrs-full_CurrentAssets"));
        f.setCurrentLiabilities(amt.get("BS:ifrs-full_CurrentLiabilities"));
        f.setOperatingCashFlow(amt.get("CF:ifrs-full_CashFlowsFromUsedInOperatingActivities"));
        f.setOperatingMargin(ratio(f.getOperatingProfit(), f.getRevenue()));
        f.setRoe(ratio(f.getNetIncome(), f.getTotalEquity()));
        f.setDebtRatio(ratio(f.getTotalDebt(), f.getTotalEquity()));
        f.setCurrentRatio(ratio(f.getCurrentAssets(), f.getCurrentLiabilities()));
        f.setInterestCoverage(times(f.getOperatingProfit(), f.getFinanceCosts()));

        return f;
    }

    // --- 헬퍼 ---
    // 여러 sj_div를 순서대로 시도, 일부 회사는 IS 없이 CIS만 존재
    private Long getAmount(Map<String, Long> amt, String accountId, String... sjDivs) {
        for (String sj : sjDivs) {
            Long v = amt.get(sj + ":" + accountId);
            if (v != null) return v;
        }
        return null;
    }

    private Long parseAmount(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) return null;
        try { return Long.parseLong(raw.replace(",", "").trim()); }
        catch (NumberFormatException e) { return null; }
    }
    private BigDecimal ratio(Long a, Long b) {
        if (a == null || b == null || b == 0) return null;
        return BigDecimal.valueOf(a)
                .divide(BigDecimal.valueOf(b), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal times(Long a, Long b) {
        if (a == null || b == null || b == 0) return null;
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 2, RoundingMode.HALF_UP);
    }
}