package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockPriceApiCallMeasurementTest {
    private static final int USERS = 100;
    private static final int RECOMMENDATIONS_PER_USER = 10;
    // Existing main-page behavior in commit 7a51053: initial load, 10s poll, 20s poll.
    private static final int LEGACY_FETCH_CYCLES_IN_30_SECONDS = 3;
    @Test
    void measuresExternalKisCallsBeforeAndAfterForTheSameDashboardScenario() throws Exception {
        CountingPriceProvider legacyKis = new CountingPriceProvider();
        var resource = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                getClass().getResourceAsStream("/stock-sectors.json"));
        List<String> serviceUniverse = new ArrayList<>();
        resource.forEach(item -> serviceUniverse.add(item.get("code").asText()));
        assertThat(serviceUniverse).hasSize(193);
        List<String> recommendations = serviceUniverse.subList(0, RECOMMENDATIONS_PER_USER);

        // Before: every browser poll invokes KIS once per recommended stock.
        for (int user = 0; user < USERS; user++) {
            for (int cycle = 0; cycle < LEGACY_FETCH_CYCLES_IN_30_SECONDS; cycle++) {
                recommendations.forEach(legacyKis::fetch);
            }
        }

        CountingPriceProvider cachedKis = new CountingPriceProvider();
        InMemorySnapshotRepository redisSubstitute = new InMemorySnapshotRepository();
        StockMapper stockMapper = mock(StockMapper.class);
        when(stockMapper.selectAllStockCodes()).thenReturn(serviceUniverse);
        StockPriceRefreshScheduler scheduler = new StockPriceRefreshScheduler(stockMapper, cachedKis, redisSubstitute);
        StockService cachedService = new StockService(redisSubstitute);

        // After: one global refresh fills Redis; all identical user reads are cache-only.
        scheduler.refresh();
        for (int user = 0; user < USERS; user++) {
            for (int cycle = 0; cycle < LEGACY_FETCH_CYCLES_IN_30_SECONDS; cycle++) {
                cachedService.getStockPrices(recommendations);
            }
        }

        int before = legacyKis.calls();
        int after = cachedKis.calls();
        double reductionRate = (before - after) * 100.0 / before;

        assertThat(before).isEqualTo(3_000);
        assertThat(after).isEqualTo(193);
        assertThat(before - after).isEqualTo(2_807);
        assertThat(reductionRate).isEqualTo(93.56666666666666);
        assertThat(redisSubstitute.reads()).isEqualTo(USERS * LEGACY_FETCH_CYCLES_IN_30_SECONDS);

        System.out.printf("MEASUREMENT users=%d before=%d after=%d reduced=%d rate=%.2f%%%n",
                USERS, before, after, before - after, reductionRate);
    }

    private static StockPriceResponseDto price() {
        return new StockPriceResponseDto("70000", "500", "0.72", "1000000", "80000", "50000", "12", "1.2", "5000", "58000");
    }

    private static final class CountingPriceProvider implements StockPriceProvider {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public StockPriceResponseDto fetch(String stockCode) {
            calls.incrementAndGet();
            return price();
        }

        int calls() {
            return calls.get();
        }
    }

    private static final class InMemorySnapshotRepository implements StockPriceSnapshotRepository {
        private Map<String, StockPriceResponseDto> prices = Map.of();
        private Instant fetchedAt;
        private final AtomicInteger reads = new AtomicInteger();

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public Optional<StockPriceResponseDto> find(String stockCode) {
            return Optional.ofNullable(prices.get(stockCode));
        }

        @Override
        public Map<String, StockPriceResponseDto> findAll(List<String> stockCodes) {
            reads.incrementAndGet();
            Map<String, StockPriceResponseDto> result = new LinkedHashMap<>();
            stockCodes.forEach(code -> Optional.ofNullable(prices.get(code)).ifPresent(price -> result.put(code, price)));
            return result;
        }

        @Override
        public Optional<Instant> currentFetchedAt() {
            return Optional.ofNullable(fetchedAt);
        }

        @Override
        public void publish(Map<String, StockPriceResponseDto> prices, Instant fetchedAt) {
            this.prices = Map.copyOf(prices);
            this.fetchedAt = fetchedAt;
        }

        int reads() {
            return reads.get();
        }
    }
}
