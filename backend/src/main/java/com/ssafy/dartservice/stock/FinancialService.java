package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.report.ReportMapper;
import com.ssafy.dartservice.stock.dto.FinancialResponseDto;
import com.ssafy.dartservice.stock.dart.DartClient;
import com.ssafy.dartservice.stock.dart.DartFinancialResponse;
import com.ssafy.dartservice.stock.dart.StockFinancial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final ReportMapper reportMapper;
    private final DartClient dartClient;

    private static final String ANNUAL = "11011";                        // 사업보고서
    private static final String[] QUARTERS = {"11014", "11012", "11013"}; // 3분기→반기→1분기
    private static final String FS_DIV = "CFS";                           // 연결재무제표

    // ===== 연간 3년치 =====
    public List<FinancialResponseDto> getFinancials(String stockCode, int latestYear) {
        log.info("재무(연간) 조회 - stockCode: {}, latestYear: {}", stockCode, latestYear);

        List<StockFinancial> cached = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            StockFinancial c = reportMapper.findFinancialByPeriod(stockCode, latestYear - i, ANNUAL);
            if (c != null) cached.add(c);
        }
        if (cached.size() == 3) {
            log.info("연간 캐시 히트 - stockCode: {}", stockCode);
            return cached.stream().map(FinancialResponseDto::from).toList();
        }

        String corpCode = reportMapper.findCorpCode(stockCode);
        log.info("연간 캐시 미스 - DART 호출 - stockCode: {}, corpCode: {}", stockCode, corpCode);
        DartFinancialResponse res = dartClient.fetch(corpCode, latestYear, ANNUAL, FS_DIV);
        if (res == null || !"000".equals(res.getStatus())) {
            log.error("DART 응답 오류 - stockCode: {}, status: {}", stockCode, res != null ? res.getStatus() : "null");
            throw new IllegalStateException("DART 응답 오류");
        }

        Map<String, Long> thstrm = new HashMap<>();
        Map<String, Long> frmtrm = new HashMap<>();
        Map<String, Long> bfefrmtrm = new HashMap<>();
        for (DartFinancialResponse.Item item : res.getList()) {
            String key = item.getSjDiv() + ":" + item.getAccountId();
            putIfValid(thstrm, key, item.getThstrmAmount());
            putIfValid(frmtrm, key, item.getFrmtrmAmount());
            putIfValid(bfefrmtrm, key, item.getBfefrmtrmAmount());
        }

        List<StockFinancial> all = List.of(
                build(stockCode, latestYear,     thstrm),
                build(stockCode, latestYear - 1, frmtrm),
                build(stockCode, latestYear - 2, bfefrmtrm)
        );
        for (StockFinancial f : all) reportMapper.insertFinancial(f);
        log.info("연간 저장 완료 - stockCode: {}, {}~{}", stockCode, latestYear - 2, latestYear);

        return all.stream().map(FinancialResponseDto::from).toList();
    }

    // ===== 최신 분기 1건 (폴백) =====
    public FinancialResponseDto getLatestQuarter(String stockCode) {
        int thisYear = LocalDate.now().getYear();
        log.info("재무(분기) 조회 - stockCode: {}, year: {}", stockCode, thisYear);

        // 1) 캐시: 최신 분기부터 확인
        for (String code : QUARTERS) {
            StockFinancial c = reportMapper.findFinancialByPeriod(stockCode, thisYear, code);
            if (c != null) {
                log.info("분기 캐시 히트 - stockCode: {}, periodCode: {}", stockCode, code);
                return FinancialResponseDto.from(c);
            }
        }

        // 2) 캐시 미스: 최신 분기부터 호출, 첫 성공 채택
        String corpCode = reportMapper.findCorpCode(stockCode);
        for (String code : QUARTERS) {
            DartFinancialResponse res = dartClient.fetch(corpCode, thisYear, code, FS_DIV);
            if (res == null || !"000".equals(res.getStatus())) {
                log.info("분기 미공시 - stockCode: {}, periodCode: {}, status: {}",
                        stockCode, code, res != null ? res.getStatus() : "null");
                continue; // 013 등 → 다음 후보
            }

            Map<String, Long> thstrm = new HashMap<>(); // 분기는 당기(thstrm)만 사용
            for (DartFinancialResponse.Item item : res.getList()) {
                putIfValid(thstrm, item.getSjDiv() + ":" + item.getAccountId(), item.getThstrmAmount());
            }

            StockFinancial f = build(stockCode, thisYear, code, thstrm);
            reportMapper.insertFinancial(f);
            log.info("분기 채택·저장 - stockCode: {}, periodCode: {}", stockCode, code);
            return FinancialResponseDto.from(f);
        }

        log.info("올해 분기 공시 없음 - stockCode: {} (연간만 사용)", stockCode);
        return null; // 분기 없으면 연간만 사용
    }

    private void putIfValid(Map<String, Long> map, String key, String raw) {
        Long v = parseAmount(raw);
        if (v != null) map.put(key, v);
    }

    // 기존 3-arg → 연간(11011)으로 위임
    private StockFinancial build(String stockCode, int year, Map<String, Long> amt) {
        return build(stockCode, year, ANNUAL, amt);
    }

    // periodCode 받는 본체
    private StockFinancial build(String stockCode, int year, String periodCode, Map<String, Long> amt) {
        StockFinancial f = new StockFinancial();
        f.setStockCode(stockCode);
        f.setBaseYear(year);
        f.setPeriodCode(periodCode);   // ← report_code → period_code
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