package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import com.ssafy.dartservice.stock.dto.ChartResponseDto;
import com.ssafy.dartservice.stock.dto.FinancialResponseDto;
import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;
    private final StockInitService stockInitService;
    private final ChartService chartService;
    private final FinancialService financialService;
    private final StockMapper stockMapper;

    @GetMapping("/sectors")
    public ResponseEntity<List<String>> getSectors() {
        return ResponseEntity.ok(stockMapper.getSectors());
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listStocks(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String sector,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = page * size;
        List<StockSearchResponseDto> stocks = stockMapper.listStocks(keyword, sector, sort, size, offset);
        int total = stockMapper.countStocksByKeyword(keyword, sector);
        return ResponseEntity.ok(Map.of(
                "stocks", stocks,
                "total", total,
                "page", page,
                "size", size
        ));
    }

    @PostMapping("/init")
    public ResponseEntity<String> initStocks() {
        stockInitService.initAll();
        return ResponseEntity.ok("종목 데이터 저장 완료");
    }

    @PostMapping("/init-marketcap")
    public ResponseEntity<String> initMarketCap() {
        int updated = stockService.fetchAndStoreMarketCapRankings();
        return ResponseEntity.ok("시가총액 저장 완료: " + updated + "개 종목");
    }

    @GetMapping("/{code}/price")
    public ResponseEntity<StockPriceResponseDto> getStockPrice(@PathVariable String code) {
        StockPriceResponseDto price = stockService.getStockPrice(code);
        return ResponseEntity.ok(price);
    }

    @GetMapping("/{code}/chart")
    public ResponseEntity<List<ChartResponseDto>> getStockChart(
            @PathVariable String code,
            @RequestParam(defaultValue = "D") String period) {
        return ResponseEntity.ok(chartService.getChart(code, period));
    }

    @GetMapping("/{code}/financial")
    public ResponseEntity<List<FinancialResponseDto>> getFinancials(
            @PathVariable String code,
            @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(financialService.getFinancials(code, year));
    }
}
