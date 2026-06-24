package com.ssafy.dartservice.stock.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KisMarketCapTopResponse {

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg_cd")
    private String msgCd;

    @JsonProperty("msg1")
    private String msg1;

    @JsonProperty("output")
    private List<Item> output;

    @Data
    public static class Item {

        @JsonProperty("mksc_shrn_iscd")
        private String mkscShrnIscd;      // 종목코드

        @JsonProperty("data_rank")
        private String dataRank;          // 순위

        @JsonProperty("hts_kor_isnm")
        private String htsKorIsnm;        // 종목명

        @JsonProperty("stck_prpr")
        private String stckPrpr;          // 현재가

        @JsonProperty("prdy_vrss")
        private String prdyVrss;          // 전일 대비

        @JsonProperty("prdy_vrss_sign")
        private String prdyVrssSign;      // 전일 대비 부호 (1상한 2상승 3보합 4하한 5하락)

        @JsonProperty("prdy_ctrt")
        private String prdyCtrt;          // 전일 대비율

        @JsonProperty("acml_vol")
        private String acmlVol;           // 누적 거래량

        @JsonProperty("lstn_stcn")
        private String lstnStcn;          // 상장 주수

        @JsonProperty("stck_avls")
        private String stckAvls;          // 시가총액 (억원 추정)

        @JsonProperty("mrkt_whol_avls_rlim")
        private String mrktWholAvlsRlim;  // 시장 전체 시총 비중
    }
}
