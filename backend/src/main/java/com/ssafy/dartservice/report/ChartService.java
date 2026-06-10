package com.ssafy.dartservice.report;

import com.ssafy.dartservice.report.dto.ChartResponseDto;
import com.ssafy.dartservice.stock.kis.KisChartResponse;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private final RestTemplate restTemplate;
    private final KisTokenService kisTokenService;

    @Value("${kis.app-key}")
    private String kisAppKey;

    @Value("${kis.app-secret}")
    private String kisAppSecret;

    @Value("${kis.base-url}")
    private String kisBaseUrl;

    public List<ChartResponseDto> getChart(String stockCode, String period) {
        log.info("차트 조회 요청 - stockCode: {}, period: {}", stockCode, period);
        if (!period.equals("D") && !period.equals("W") && !period.equals("M")) {
            period = "D";
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(kisBaseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .queryParam("FID_PERIOD_DIV_CODE", period)
                .queryParam("FID_ORG_ADJ_PRC", "1")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + kisTokenService.getAccessToken());
        headers.set("appkey", kisAppKey);
        headers.set("appsecret", kisAppSecret);
        headers.set("tr_id", "FHKST01010400");
        headers.set("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<KisChartResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                KisChartResponse.class
        );

        KisChartResponse body = response.getBody();
        if (body == null || body.getOutput() == null) {
            throw new RuntimeException("KIS 차트 조회 실패");
        }

        List<ChartResponseDto> result = new ArrayList<>();
        for (KisChartResponse.Output item : body.getOutput()) {
            result.add(new ChartResponseDto(
                    item.getStckBsopDate(),
                    item.getStckOprc(),
                    item.getStckHgpr(),
                    item.getStckLwpr(),
                    item.getStckClpr(),
                    item.getAcmlVol()
            ));
        }
        return result;
    }
}