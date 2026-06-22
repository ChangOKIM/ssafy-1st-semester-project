package com.ssafy.dartservice.stock.kis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTokenService {

    private final RestTemplate restTemplate;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    private String cachedToken;
    private LocalDateTime tokenExpiredAt;

    public synchronized String getAccessToken() {
        if (cachedToken != null && tokenExpiredAt != null
                && LocalDateTime.now().isBefore(tokenExpiredAt.minusHours(1))) {
            log.info("캐시된 KIS 토큰 사용");
            return cachedToken;
        }

        log.info("KIS 토큰 새로 발급");
        return issueToken();
    }

    private String issueToken() {
        String url = baseUrl + "/oauth2/tokenP";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("appsecret", appSecret);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        KisTokenResponse response = restTemplate.postForObject(
                url,
                entity,
                KisTokenResponse.class
        );

        if (response == null || response.getAccessToken() == null) {
            throw new RuntimeException("KIS 토큰 발급 실패");
        }

        cachedToken = response.getAccessToken();
        tokenExpiredAt = LocalDateTime.now().plusSeconds(response.getExpiresIn());
        log.info("KIS 토큰 발급 완료. 만료시간: {}", tokenExpiredAt);

        return cachedToken;
    }
}