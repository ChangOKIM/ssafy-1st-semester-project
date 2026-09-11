package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockPriceSnapshotRepository {
    boolean isAvailable();
    Optional<StockPriceResponseDto> find(String stockCode);
    Map<String, StockPriceResponseDto> findAll(List<String> stockCodes);
    Optional<Instant> currentFetchedAt();
    void publish(Map<String, StockPriceResponseDto> prices, Instant fetchedAt);
}
