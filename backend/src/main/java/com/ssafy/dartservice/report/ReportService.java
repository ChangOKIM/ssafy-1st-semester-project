package com.ssafy.dartservice.report;

import com.ssafy.dartservice.report.dto.ReportInput;
import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import com.ssafy.dartservice.stock.ChartService;
import com.ssafy.dartservice.stock.FinancialService;
import com.ssafy.dartservice.stock.StockService;
import com.ssafy.dartservice.stock.dto.ChartResponseDto;
import com.ssafy.dartservice.stock.dto.FinancialResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final ChartService chartService;
    private final FinancialService financialService;
    private final ReportLlmService reportLlmService;
    private final StockService stockService;

    public List<StockSearchResponseDto> searchStocks(String keyword) {
        log.info("종목 검색 요청 - keyword: {}", keyword);
        List<StockSearchResponseDto> result = reportMapper.searchByKeyword(keyword);
        log.info("종목 검색 결과 - {}건", result.size());
        return result;
    }

    public List<ChartResponseDto> getChart(String stockCode, String period) {
        return chartService.getChart(stockCode, period);
    }

    public List<FinancialResponseDto> getFinancials(String stockCode, int latestYear) {
        return financialService.getFinancials(stockCode, latestYear);
    }

    // 원 단위 Long → "약 258.9조 원" 같은 읽기 쉬운 문자열
    private String toReadableWon(Long won) {
        if (won == null) return "정보 없음";

        long abs = Math.abs(won);
        if (abs >= 1_0000_0000_0000L) {          // 1조 이상
            double jo = won / 1_0000_0000_0000.0;
            return String.format("약 %.1f조 원", jo);
        } else if (abs >= 1_0000_0000L) {        // 1억 이상
            double eok = won / 1_0000_0000.0;
            return String.format("약 %.0f억 원", eok);
        } else {
            return String.format("%,d 원", won);  // 그 이하는 그대로
        }
    }

    public String getReport(String stockCode, int latestYear) {
        String cached = reportMapper.findCachedReport(stockCode);
        if (cached != null) {
            log.info("AI 리포트 캐시 히트 - stockCode: {}", stockCode);
            return cached;
        }
        log.info("AI 리포트 생성 요청 - stockCode: {}", stockCode);

        List<FinancialResponseDto> financials = financialService.getFinancials(stockCode, latestYear);
        FinancialResponseDto quarter = financialService.getLatestQuarter(stockCode); // null 가능
        var price = stockService.getStockPrice(stockCode);

        StockSearchResponseDto stock = reportMapper.findById(stockCode);
        String companyName = stock.getStockName();

        // 분기 → LatestQuarter (없으면 null)
        ReportInput.LatestQuarter latestQuarter = (quarter == null) ? null
                : new ReportInput.LatestQuarter(
                quarterLabel(quarter.getBaseYear(), quarter.getPeriodCode()),
                toReadableWon(quarter.getRevenue()),
                toReadableWon(quarter.getOperatingProfit()),
                quarter.getOperatingMargin(),
                quarter.getDebtRatio()
        );

        ReportInput input = new ReportInput(
                companyName,
                financials.stream().map(f -> new ReportInput.FinancialYear(
                        f.getBaseYear(),
                        toReadableWon(f.getRevenue()),
                        toReadableWon(f.getOperatingProfit()),
                        f.getOperatingMargin(),
                        f.getDebtRatio(),
                        f.getInterestCoverage()
                )).toList(),
                latestQuarter,
                price.getCurrentPrice(),
                price.getPer(),
                price.getPbr()
        );

        String report = reportLlmService.generateReport(input);
        reportMapper.saveReport(stockCode, report);
        log.info("AI 리포트 저장 완료 - stockCode: {}", stockCode);
        return report;
    }

    private String quarterLabel(int year, String periodCode) {
        String q = switch (periodCode) {
            case "11013" -> "1분기 누적";
            case "11012" -> "반기 누적";
            case "11014" -> "3분기 누적";
            default      -> "분기";
        };
        return year + "년 " + q;
    }
}