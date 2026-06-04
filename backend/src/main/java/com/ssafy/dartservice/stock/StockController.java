package com.ssafy.dartservice.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;

    @PostMapping("/init")
    public ResponseEntity<String> initStocks() {
        stockService.fetchAndSaveStocks();
        return ResponseEntity.ok("종목 데이터 저장 완료");
    }
}