package com.ssafy.dartservice.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RedisStockPriceSnapshotRepository implements StockPriceSnapshotRepository {
    private static final String CURRENT_KEY = "stock:prices:current";
    private static final String SNAPSHOT_PREFIX = "stock:prices:snapshot:";
    private static final String FETCHED_AT_FIELD = "_fetchedAt";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${stock.price.snapshot-ttl-seconds:120}")
    private long snapshotTtlSeconds;

    @Override
    public boolean isAvailable() {
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<StockPriceResponseDto> find(String stockCode) {
        String snapshotKey = currentSnapshotKey();
        if (snapshotKey == null) return Optional.empty();
        Object json = redisTemplate.opsForHash().get(snapshotKey, stockCode);
        return deserialize(json);
    }

    @Override
    public Map<String, StockPriceResponseDto> findAll(List<String> stockCodes) {
        String snapshotKey = currentSnapshotKey();
        if (snapshotKey == null || stockCodes.isEmpty()) return Map.of();

        List<Object> values = redisTemplate.opsForHash().multiGet(snapshotKey, stockCodes.stream().map(code -> (Object) code).toList());
        if (values == null) return Map.of();
        Map<String, StockPriceResponseDto> result = new LinkedHashMap<>();
        for (int i = 0; i < stockCodes.size(); i++) {
            String stockCode = stockCodes.get(i);
            deserialize(values.get(i)).ifPresent(price -> result.put(stockCode, price));
        }
        return result;
    }

    @Override
    public Optional<Instant> currentFetchedAt() {
        String snapshotKey = currentSnapshotKey();
        if (snapshotKey == null) return Optional.empty();
        Object value = redisTemplate.opsForHash().get(snapshotKey, FETCHED_AT_FIELD);
        if (value == null) return Optional.empty();
        return Optional.of(Instant.parse(value.toString()));
    }

    @Override
    public void publish(Map<String, StockPriceResponseDto> prices, Instant fetchedAt) {
        if (prices.isEmpty()) return;
        String snapshotId = fetchedAt.toEpochMilli() + "-" + UUID.randomUUID();
        String snapshotKey = SNAPSHOT_PREFIX + snapshotId;
        Map<String, String> serialized = new LinkedHashMap<>();
        prices.forEach((code, price) -> serialized.put(code, serialize(price)));
        serialized.put(FETCHED_AT_FIELD, fetchedAt.toString());

        redisTemplate.opsForHash().putAll(snapshotKey, serialized);
        redisTemplate.expire(snapshotKey, Duration.ofSeconds(snapshotTtlSeconds));
        // The pointer changes only after the complete snapshot has been written.
        redisTemplate.opsForValue().set(CURRENT_KEY, snapshotKey);
    }

    private String currentSnapshotKey() {
        return redisTemplate.opsForValue().get(CURRENT_KEY);
    }

    private String serialize(StockPriceResponseDto price) {
        try {
            return objectMapper.writeValueAsString(price);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("시세 스냅샷 직렬화 실패", e);
        }
    }

    private Optional<StockPriceResponseDto> deserialize(Object json) {
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json.toString(), StockPriceResponseDto.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("시세 스냅샷 역직렬화 실패", e);
        }
    }
}
