package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.stock.dto.StockSectorEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInitService {

    private final FinancialService financialService;
    private final StockMapper stockMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final MarketCapRankingService marketCapRankingService;

    @Value("${krx.api-key}")
    private String krxApiKey;

    @Value("${dart.api-key}")
    private String dartApiKey;

    // === 전체 초기화 (순서 보장) ===
    public void initAll() {
        initStocks();
        initFinancials();
        initStockInfo();
        try {
            marketCapRankingService.getTopMarketCapStocks();
            log.info("4. 시총 상위 캐시 워밍업 완료");
        } catch (Exception e) {
            log.warn("4. 시총 상위 캐시 워밍업 실패 (앱 기동 계속): {}", e.getMessage());
        }
    }

    // === 1. 코스피 종목 마스터 ===
    public void initStocks() {
        if (stockMapper.countStocks() > 0) {
            log.info("1. 종목 마스터 이미 초기화됨 - 스킵");
            return;
        }
        fetchAndSaveStocks();
        log.info("1. 종목 마스터 완료");
    }

    private void fetchAndSaveStocks() {
        Map<String, String> corpCodeMap = fetchCorpCodeMap();
        log.info("DART 고유번호 매핑 수: {}개", corpCodeMap.size());

        String url = UriComponentsBuilder
                .fromHttpUrl("https://data-dbg.krx.co.kr/svc/apis/sto/stk_isu_base_info")
                .queryParam("basDd", "20260116")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", krxApiKey);

        ResponseEntity<KrxStockResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), KrxStockResponse.class);

        KrxStockResponse body = response.getBody();
        if (body == null || body.getOutBlock1() == null) {
            log.error("KRX API 응답이 없습니다.");
            return;
        }

        List<KrxStockResponse.StockItem> items = body.getOutBlock1();
        log.info("KRX 종목 수: {}개", items.size());

        Map<String, String> nameToCorpCodeMap = new HashMap<>();
        for (KrxStockResponse.StockItem item : items) {
            if ("보통주".equals(item.getKindStkcertTpNm())) {
                String corpCode = corpCodeMap.get(item.getIsuSrtCd());
                if (corpCode != null) {
                    nameToCorpCodeMap.put(item.getIsuAbbrv(), corpCode);
                }
            }
        }

        for (KrxStockResponse.StockItem item : items) {
            String stockCode = item.getIsuSrtCd();
            String stockName = item.getIsuAbbrv();
            String corpCode = corpCodeMap.get(stockCode);

            log.info("종목: {} | 종류: {} | corp_code: {}",
                    stockName, item.getKindStkcertTpNm(), corpCode);

            if (corpCode == null
                    && item.getKindStkcertTpNm() != null
                    && !item.getKindStkcertTpNm().contains("보통주")) {
                String baseName = extractBaseName(stockName);
                log.info("우선주 매칭 시도: {} → {} | 본주맵 keys: {}",
                        stockName, baseName, nameToCorpCodeMap.containsKey(baseName));
                corpCode = nameToCorpCodeMap.get(baseName);
            }

            stockMapper.insertStock(stockCode, corpCode, stockName,
                    item.getMktTpNm(), item.getSectTpNm());
        }

        log.info("종목 저장 완료");
    }

    private String extractBaseName(String stockName) {
        return stockName.replaceAll("\\d*우[A-Z]?(\\(전환\\))?$", "").trim();
    }

    private Map<String, String> fetchCorpCodeMap() {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://opendart.fss.or.kr/api/corpCode.xml")
                .queryParam("crtfc_key", dartApiKey)
                .toUriString();

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, null, byte[].class);

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

    // === 2. 주요종목 financial ===
    public void initFinancials() {
        if (stockMapper.countFinancials() >= 200 * 3.5) { // 추천 사용 종목은 코스피 200
            log.info("2. financial 이미 초기화됨 - 스킵");
            return;
        }
        int latestYear = LocalDate.now().getYear() - 1;
        List<StockSectorEntry> stocks = loadSectors();
        int success = 0, fail = 0;
        StringBuilder failList = new StringBuilder();

        for (StockSectorEntry s : stocks) {
            try {
                financialService.getFinancials(s.code(), latestYear);   // 연간 3년치
                financialService.getLatestQuarter(s.code());            // 최신 분기 ← 이 줄 추가
                success++;
                Thread.sleep(400);
            } catch (Exception e) {
                log.error("financial 실패 - {} {}: {}", s.code(), s.name(), e.getMessage());
                failList.append(s.code()).append(" ");
                fail++;
            }
        }
        log.info("2. financial 완료 - 성공 {}, 실패 {} | 실패: {}", success, fail, failList);
    }

    // === 3. 주요종목 stock_info (섹터) ===
    public void initStockInfo() {
        List<StockSectorEntry> stocks = loadSectors();
        int count = 0;
        for (StockSectorEntry s : stocks) {
            stockMapper.insertStockInfo(s.code(), s.sector(), s.intro());
            count++;
        }
        log.info("3. stock_info 완료 - {}건", count);
    }

    private List<StockSectorEntry> loadSectors() {
        try (InputStream is = new ClassPathResource("stock-sectors.json").getInputStream()) {
            return objectMapper.readValue(is,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, StockSectorEntry.class));
        } catch (Exception e) {
            throw new RuntimeException("stock-sectors.json 읽기 실패", e);
        }
    }
}