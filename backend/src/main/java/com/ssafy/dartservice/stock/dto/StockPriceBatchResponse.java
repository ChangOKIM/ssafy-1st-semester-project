package com.ssafy.dartservice.stock.dto;

import java.time.Instant;
import java.util.Map;

public record StockPriceBatchResponse(
        Instant fetchedAt,
        Map<String, StockPriceResponseDto> prices
) {}
