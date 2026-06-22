package com.ssafy.dartservice.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class KrxStockResponse {

    @JsonProperty("OutBlock_1")
    private List<StockItem> outBlock1;

    @Data
    public static class StockItem {
        @JsonProperty("ISU_SRT_CD")
        private String isuSrtCd;    // 단축코드

        @JsonProperty("ISU_ABBRV")
        private String isuAbbrv;    // 한글 종목약명

        @JsonProperty("MKT_TP_NM")
        private String mktTpNm;     // 시장구분

        @JsonProperty("SECT_TP_NM")
        private String sectTpNm;    // 소속부 (업종)

        @JsonProperty("KIND_STKCERT_TP_NM")
        private String kindStkcertTpNm;   // 주식종류 (보통주/우선주)
    }
}