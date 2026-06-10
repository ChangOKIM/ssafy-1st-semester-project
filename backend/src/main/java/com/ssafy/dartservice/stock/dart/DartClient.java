package com.ssafy.dartservice.stock.dart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class DartClient {

    private final RestTemplate restTemplate;

    @Value("${dart.api-key}")
    private String apiKey;

    private static final String URL =
        "https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json";

    public DartFinancialResponse fetch(String corpCode, int year) {
        log.info("DART 재무 API 호출 - corpCode: {}, year: {}", corpCode, year);
        String url = UriComponentsBuilder.fromHttpUrl(URL)
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode)
                .queryParam("bsns_year", year)
                .queryParam("reprt_code", "11011")
                .queryParam("fs_div", "CFS")
                .toUriString();
        DartFinancialResponse result = restTemplate.getForObject(url, DartFinancialResponse.class);
        log.info("DART 재무 API 응답 - status: {}", result != null ? result.getStatus() : "null");
        return result;
    }
}
