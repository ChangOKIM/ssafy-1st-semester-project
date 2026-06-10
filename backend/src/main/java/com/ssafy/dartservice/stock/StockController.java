package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;

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


}
