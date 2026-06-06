package com.ssafy.dartservice.stock.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KisChartResponse {

    @JsonProperty("output")
    private List<Output> output;

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg1")
    private String msg1;

    @Data
    public static class Output {
        @JsonProperty("stck_bsop_date")
        private String stckBsopDate;    // 영업 일자

        @JsonProperty("stck_oprc")
        private String stckOprc;        // 시가

        @JsonProperty("stck_hgpr")
        private String stckHgpr;        // 고가

        @JsonProperty("stck_lwpr")
        private String stckLwpr;        // 저가

        @JsonProperty("stck_clpr")
        private String stckClpr;        // 종가

        @JsonProperty("acml_vol")
        private String acmlVol;         // 거래량
    }
}