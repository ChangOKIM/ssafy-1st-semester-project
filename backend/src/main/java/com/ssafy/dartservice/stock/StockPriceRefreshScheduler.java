package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockPriceRefreshScheduler {
    private final StockMapper stockMapper;
    private final StockPriceProvider stockPriceProvider;
    private final StockPriceSnapshotRepository snapshotRepository;

    @Scheduled(fixedRateString = "${stock.price.refresh-interval-ms:30000}")
    public void refresh() {
        if (!snapshotRepository.isAvailable()) {
            log.error("시세 스냅샷 갱신 스킵 - Redis에 연결할 수 없습니다.");
            return;
        }
        List<String> stockCodes = stockMapper.selectAllStockCodes();
        if (stockCodes.isEmpty()) {
            log.warn("시세 스냅샷 갱신 스킵 - 대상 종목이 없습니다.");
            return;
        }

        Map<String, StockPriceResponseDto> prices = new LinkedHashMap<>();
        int failed = 0;
        for (String stockCode : stockCodes) {
            try {
                prices.put(stockCode, stockPriceProvider.fetch(stockCode));
            } catch (Exception e) {
                failed++;
                snapshotRepository.find(stockCode).ifPresent(previous -> prices.put(stockCode, previous));
                log.warn("시세 갱신 실패 - {}: {}", stockCode, e.getMessage());
            }
        }

        if (!prices.isEmpty()) {
            snapshotRepository.publish(prices, Instant.now());
        }
        log.info("시세 스냅샷 갱신 완료 - 대상 {}, 저장 {}, 실패 {}", stockCodes.size(), prices.size(), failed);
    }
}
