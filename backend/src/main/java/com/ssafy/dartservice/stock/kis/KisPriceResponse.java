package com.ssafy.dartservice.stock.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KisPriceResponse {

    @JsonProperty("output")
    private Output output;

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg1")
    private String msg1;

    @Data
    public static class Output {
        @JsonProperty("stck_prpr")
        private String stckPrpr;          // 주식 현재가

        @JsonProperty("prdy_vrss")
        private String prdyVrss;          // 전일 대비

        @JsonProperty("prdy_vrss_sign")
        private String prdyVrssSign;      // 전일 대비 부호

        @JsonProperty("prdy_ctrt")
        private String prdyCtrt;          // 전일 대비율

        @JsonProperty("acml_vol")
        private String acmlVol;           // 누적 거래량

        @JsonProperty("w52_hgpr")
        private String w52Hgpr;           // 52주 최고가

        @JsonProperty("w52_lwpr")
        private String w52Lwpr;           // 52주 최저가
    }
}