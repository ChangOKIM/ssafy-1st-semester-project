package com.ssafy.dartservice.stock;

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
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;
    private final RestTemplate restTemplate;
    private final KisTokenService kisTokenService;

    @Value("${krx.api-key}")
    private String apiKey;

    @Value("${kis.app-key}")
    private String kisAppKey;

    @Value("${kis.app-secret}")
    private String kisAppSecret;

    @Value("${kis.base-url}")
    private String kisBaseUrl;

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

    @Override
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

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<KisPriceResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                KisPriceResponse.class
        );

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
                output.getW52Lwpr()
        );
    }
}