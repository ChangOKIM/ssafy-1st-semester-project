package com.ssafy.dartservice.stock;

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
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;
    private final RestTemplate restTemplate;

    @Value("${krx.api-key}")
    private String apiKey;

    @Override
    public void fetchAndSaveStocks() {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://data-dbg.krx.co.kr/svc/apis/sto/stk_isu_base_info")
                .queryParam("basDd", "20260116")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<KrxStockResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                KrxStockResponse.class
        );

        KrxStockResponse body = response.getBody();

        if (body == null || body.getOutBlock1() == null) {
            log.error("KRX API 응답이 없습니다.");
            return;
        }

        List<KrxStockResponse.StockItem> items = body.getOutBlock1();
        log.info("KRX 종목 수: {}개", items.size());

        for (KrxStockResponse.StockItem item : items) {
            stockMapper.insertStock(
                    item.getIsuSrtCd(),
                    item.getIsuAbbrv(),
                    item.getMktTpNm(),
                    item.getSectTpNm()
            );
        }

        log.info("종목 저장 완료");
    }
}