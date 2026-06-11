package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.ChartResponseDto;
import com.ssafy.dartservice.stock.dto.FinancialResponseDto;
import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;
    private final ChartService chartService;
    private final FinancialService financialService;

    //@Hidden
    @PostMapping("/init")
    public ResponseEntity<String> initStocks() {
        stockService.fetchAndSaveStocks();
        return ResponseEntity.ok("종목 데이터 저장 완료");
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
