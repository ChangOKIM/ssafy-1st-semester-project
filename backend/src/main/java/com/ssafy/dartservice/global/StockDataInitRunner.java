package com.ssafy.dartservice.global;

import com.ssafy.dartservice.stock.StockInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDataInitRunner implements ApplicationRunner {

    private final StockInitService stockInitService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== 초기 데이터 로딩 시작 ===");
        try {
            stockInitService.initAll();
            log.info("=== 초기 데이터 로딩 완료 ===");
        } catch (Exception e) {
            log.error("초기 데이터 로딩 실패 (앱은 계속 실행): {}", e.getMessage(), e);
        }
    }
}