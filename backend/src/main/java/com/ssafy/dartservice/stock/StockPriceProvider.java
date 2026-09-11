package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;

public interface StockPriceProvider {
    StockPriceResponseDto fetch(String stockCode);
}
