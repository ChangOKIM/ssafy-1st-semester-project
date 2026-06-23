package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
import com.ssafy.dartservice.stock.kis.KisMarketCapRankingResponse;
import com.ssafy.dartservice.stock.kis.KisPriceResponse;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockMapper stockMapper;
    private final RestTemplate restTemplate;
    private final KisTokenService kisTokenService;

    @Value("${kis.app-key}")
    private String kisAppKey;

    @Value("${kis.app-secret}")
    private String kisAppSecret;

    @Value("${kis.base-url}")
    private String kisBaseUrl;

    public StockPriceResponseDto getStockPrice(String stockCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(kisBaseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + kisTokenService.getAccessToken());
        headers.set("appkey", kisAppKey);
        headers.set("appsecret", kisAppSecret);
        headers.set("tr_id", "FHKST01010100");
        headers.set("custtype", "P");

        ResponseEntity<KisPriceResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), KisPriceResponse.class);

        KisPriceResponse body = response.getBody();
        if (body == null || body.getOutput() == null) {
            throw new RuntimeException("KIS 시세 조회 실패");
        }

        KisPriceResponse.Output output = body.getOutput();

        String htsAvls = output.getHtsAvls();
        if (htsAvls != null && !htsAvls.isBlank()) {
            try {
                stockMapper.updateMarketCap(stockCode, Long.parseLong(htsAvls.trim()));
            } catch (Exception e) {
                log.debug("시가총액 업데이트 실패 - {}: {}", stockCode, e.getMessage());
            }
        }

        return new StockPriceResponseDto(
                output.getStckPrpr(),
                output.getHtsAvls(),
                output.getPrdyVrss(),
                output.getPrdyCtrt(),
                output.getAcmlVol(),
                output.getW52Hgpr(),
                output.getW52Lwpr(),
                output.getPer(),
                output.getPbr(),
                output.getEps(),
                output.getBps()
        );
    }

    public int fetchAndStoreMarketCapRankings() {
        int updated = 0;
        updated += fetchRankingForMarket("J"); // KOSPI
        updated += fetchRankingForMarket("Q"); // KOSDAQ
        log.info("시가총액 순위 저장 완료: {}개 종목", updated);
        return updated;
    }

    private int fetchRankingForMarket(String marketCode) {
        String url = UriComponentsBuilder
                .fromHttpUrl(kisBaseUrl + "/uapi/domestic-stock/v1/ranking/market-cap")
                .queryParam("FID_COND_MRKT_DIV_CODE", marketCode)
                .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                .queryParam("FID_INPUT_ISCD", "0000")
                .queryParam("FID_DIV_CLS_CODE", "0")
                .queryParam("FID_BLNG_CLS_CODE", "0")
                .queryParam("FID_TRGT_CLS_CODE", "111111111")
                .queryParam("FID_TRGT_EXLS_CLS_CODE", "0000000000")
                .queryParam("FID_INPUT_PRICE_1", "")
                .queryParam("FID_INPUT_PRICE_2", "")
                .queryParam("FID_VOL_CNT", "")
                .queryParam("FID_INPUT_DATE_1", "")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + kisTokenService.getAccessToken());
        headers.set("appkey", kisAppKey);
        headers.set("appsecret", kisAppSecret);
        headers.set("tr_id", "FHPST01710000");
        headers.set("custtype", "P");

        try {
            ResponseEntity<KisMarketCapRankingResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), KisMarketCapRankingResponse.class);

            KisMarketCapRankingResponse body = response.getBody();
            if (body == null || body.getOutput() == null) {
                log.warn("시가총액 순위 응답 없음 - market: {}", marketCode);
                return 0;
            }

            List<KisMarketCapRankingResponse.Item> items = body.getOutput();
            int count = 0;
            for (KisMarketCapRankingResponse.Item item : items) {
                String code = item.getStockCode();
                String cap = item.getMarketCap();
                if (code == null || code.isBlank() || cap == null || cap.isBlank()) continue;
                try {
                    stockMapper.updateMarketCap(code.trim(), Long.parseLong(cap.trim()));
                    count++;
                } catch (Exception e) {
                    log.debug("시가총액 저장 실패 - {}: {}", code, e.getMessage());
                }
            }
            log.info("시가총액 순위 저장 - market: {}, {}개", marketCode, count);
            return count;
        } catch (Exception e) {
            log.error("시가총액 순위 조회 실패 - market: {}: {}", marketCode, e.getMessage());
            return 0;
        }
    }
}
