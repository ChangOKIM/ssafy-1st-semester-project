package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.global.exception.BusinessException;
import com.ssafy.dartservice.global.exception.ErrorCode;
import com.ssafy.dartservice.stock.dto.StockPriceBatchResponse;
import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockPriceSnapshotRepository stockPriceSnapshotRepository;

    public StockPriceResponseDto getStockPrice(String stockCode) {
        try {
            ensureCacheAvailable();
            return stockPriceSnapshotRepository.find(stockCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_PRICE_NOT_READY));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STOCK_PRICE_NOT_READY);
        }
    }

    public StockPriceBatchResponse getStockPrices(java.util.List<String> stockCodes) {
        try {
            ensureCacheAvailable();
            var prices = stockPriceSnapshotRepository.findAll(stockCodes);
            if (prices.isEmpty()) {
                throw new BusinessException(ErrorCode.STOCK_PRICE_NOT_READY);
            }
            return new StockPriceBatchResponse(stockPriceSnapshotRepository.currentFetchedAt().orElse(null), prices);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STOCK_PRICE_NOT_READY);
        }
    }

    private void ensureCacheAvailable() {
        if (!stockPriceSnapshotRepository.isAvailable()) {
            throw new BusinessException(ErrorCode.STOCK_PRICE_NOT_READY);
        }
    }
}
