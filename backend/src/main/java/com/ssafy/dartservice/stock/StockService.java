package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockPriceResponseDto;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

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

        return new StockPriceResponseDto(
                output.getStckPrpr(),
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
}
