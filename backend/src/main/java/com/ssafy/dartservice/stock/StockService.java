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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Slf4j
@Service
@RequiredArgsConstructor
public class StockService{

    private final StockMapper stockMapper;
    private final RestTemplate restTemplate;
    private final KisTokenService kisTokenService;

    @Value("${krx.api-key}")
    private String krxApiKey;

    @Value("${kis.app-key}")
    private String kisAppKey;

    @Value("${kis.app-secret}")
    private String kisAppSecret;

    @Value("${kis.base-url}")
    private String kisBaseUrl;

    @Value("${dart.api-key}")
    private String dartApiKey;

    public void fetchAndSaveStocks() {
        // 1. DART 고유번호 매핑 가져오기
        Map<String, String> corpCodeMap = fetchCorpCodeMap();
        log.info("DART 고유번호 매핑 수: {}개", corpCodeMap.size());

        // 2. KRX 종목 데이터 가져오기
        String url = UriComponentsBuilder
                .fromHttpUrl("https://data-dbg.krx.co.kr/svc/apis/sto/stk_isu_base_info")
                .queryParam("basDd", "20260116")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", krxApiKey);

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

        // 3. 본주 종목명 → corp_code 매핑 만들기 (우선주가 본주 찾을 때 사용)
        Map<String, String> nameToCorpCodeMap = new HashMap<>();
        for (KrxStockResponse.StockItem item : items) {
            if ("보통주".equals(item.getKindStkcertTpNm())) {
                String corpCode = corpCodeMap.get(item.getIsuSrtCd());
                if (corpCode != null) {
                    nameToCorpCodeMap.put(item.getIsuAbbrv(), corpCode);
                }
            }
        }

        // 4. 종목별로 corp_code 매핑해서 저장
        for (KrxStockResponse.StockItem item : items) {

            String stockCode = item.getIsuSrtCd();
            String stockName = item.getIsuAbbrv();
            String corpCode = corpCodeMap.get(stockCode);

            // for문 안쪽에 추가
            log.info("종목: {} | 종류: {} | corp_code: {}",
                    item.getIsuAbbrv(),
                    item.getKindStkcertTpNm(),
                    corpCode);

            // 우선주인데 corp_code 매핑이 안 됐으면 본주의 corp_code 찾기
            if (corpCode == null
                    && item.getKindStkcertTpNm() != null
                    && !item.getKindStkcertTpNm().contains("보통주")) {
                String baseName = extractBaseName(stockName);
                log.info("우선주 매칭 시도: {} → {} | 본주맵 keys: {}",
                        stockName, baseName, nameToCorpCodeMap.containsKey(baseName));
                corpCode = nameToCorpCodeMap.get(baseName);
            }

            stockMapper.insertStock(
                    stockCode,
                    corpCode,
                    stockName,
                    item.getMktTpNm(),
                    item.getSectTpNm()
            );
        }

        log.info("종목 저장 완료");
    }

    /**
     * 우선주 종목명에서 본주 종목명 추출
     * 예: "삼성전자우" → "삼성전자", "현대차2우B" → "현대차"
     */
    private String extractBaseName(String stockName) {
        // 끝에서부터 "우", "우B", "2우B" 같은 패턴 제거
        return stockName
                .replaceAll("\\d*우[A-Z]?(\\(전환\\))?$", "")
                .trim();
    }

    private Map<String, String> fetchCorpCodeMap() {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://opendart.fss.or.kr/api/corpCode.xml")
                .queryParam("crtfc_key", dartApiKey)
                .toUriString();

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                byte[].class
        );

        byte[] zipBytes = response.getBody();
        if (zipBytes == null) {
            throw new RuntimeException("DART 고유번호 API 응답 없음");
        }

        Map<String, String> map = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xml")) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(zis);

                    NodeList listNodes = doc.getElementsByTagName("list");
                    for (int i = 0; i < listNodes.getLength(); i++) {
                        Node node = listNodes.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) node;
                            String stockCode = getTagValue(element, "stock_code");
                            String corpCode = getTagValue(element, "corp_code");

                            // 상장사만 (종목코드가 있는 것만)
                            if (stockCode != null && !stockCode.trim().isEmpty()) {
                                map.put(stockCode.trim(), corpCode);
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("DART 고유번호 파싱 실패", e);
        }

        return map;
    }

    private String getTagValue(Element element, String tag) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

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
                output.getW52Lwpr(),
                output.getPer(),
                output.getPbr(),
                output.getEps(),
                output.getBps()
        );
    }
}