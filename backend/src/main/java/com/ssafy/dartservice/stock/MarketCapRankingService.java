package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.MarketCapTopItemDto;
import com.ssafy.dartservice.stock.kis.KisMarketCapTopResponse;
import com.ssafy.dartservice.stock.kis.KisTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketCapRankingService {

    private final RestTemplate restTemplate;
    private final KisTokenService kisTokenService;
    private final StockMapper stockMapper;

    @Value("${kis.app-key}")
    private String kisAppKey;

    @Value("${kis.app-secret}")
    private String kisAppSecret;

    @Value("${kis.base-url}")
    private String kisBaseUrl;

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private volatile List<MarketCapTopItemDto> cache = List.of();
    private volatile Instant cacheUpdatedAt = Instant.EPOCH;

    public List<MarketCapTopItemDto> getTopMarketCapStocks() {
        if (!cache.isEmpty()
                && Duration.between(cacheUpdatedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cache;
        }
        return refresh();
    }

    private List<MarketCapTopItemDto> refresh() {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(kisBaseUrl + "/uapi/domestic-stock/v1/ranking/market-cap")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_COND_SCR_DIV_CODE", "20174")
                    .queryParam("FID_DIV_CLS_CODE", "0")
                    .queryParam("FID_INPUT_ISCD", "2001")   // 코스피200
                    .queryParam("FID_TRGT_CLS_CODE", "0")
                    .queryParam("FID_TRGT_EXLS_CLS_CODE", "0")
                    .queryParam("FID_INPUT_PRICE_1", "")
                    .queryParam("FID_INPUT_PRICE_2", "")
                    .queryParam("FID_VOL_CNT", "")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("content-type", "application/json; charset=utf-8");
            headers.set("authorization", "Bearer " + kisTokenService.getAccessToken());
            headers.set("appkey", kisAppKey);
            headers.set("appsecret", kisAppSecret);
            headers.set("tr_id", "FHPST01710000");
            headers.set("custtype", "P");

            ResponseEntity<KisMarketCapTopResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), KisMarketCapTopResponse.class);

            KisMarketCapTopResponse body = response.getBody();
            if (body == null || !"0".equals(body.getRtCd())
                    || body.getOutput() == null || body.getOutput().isEmpty()) {
                log.warn("KIS 시총 상위 응답 이상 - rtCd: {}", body != null ? body.getRtCd() : "null");
                return cache;
            }

            Set<String> knownCodes = new HashSet<>(stockMapper.selectAllStockCodes());

            List<MarketCapTopItemDto> result = body.getOutput().stream()
                    .filter(item -> item.getMkscShrnIscd() != null
                            && knownCodes.contains(item.getMkscShrnIscd().trim()))
                    .map(item -> new MarketCapTopItemDto(
                            parseInt(item.getDataRank()),
                            item.getMkscShrnIscd().trim(),
                            item.getHtsKorIsnm(),
                            parseLong(item.getStckPrpr()),
                            Math.abs(parseLong(item.getPrdyVrss())),
                            normalizeSign(item.getPrdyVrssSign()),
                            parseDouble(item.getPrdyCtrt()),
                            parseLong(item.getAcmlVol()),
                            parseLong(item.getStckAvls()),
                            item.getMrktWholAvlsRlim()
                    ))
                    .toList();

            cache = result;
            cacheUpdatedAt = Instant.now();
            log.info("시총 상위 캐시 갱신 - {}개 종목", result.size());
            return result;

        } catch (Exception e) {
            log.error("KIS 시총 상위 조회 실패: {}", e.getMessage());
            return cache;
        }
    }

    private static String normalizeSign(String sign) {
        if (sign == null) return "FLAT";
        return switch (sign) {
            case "1", "2" -> "UP";
            case "4", "5" -> "DOWN";
            default -> "FLAT";
        };
    }

    private static long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.replace(",", "").trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    private static int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
