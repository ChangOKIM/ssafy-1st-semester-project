package com.ssafy.dartservice.stock.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KisMarketCapRankingResponse {

    @JsonProperty("output")
    private List<Item> output;

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg1")
    private String msg1;

    @Data
    public static class Item {

        @JsonProperty("stck_shrn_iscd")
        private String stockCode;   // 종목코드 (단축)

        @JsonProperty("hts_kor_isnm")
        private String stockName;   // 종목명

        @JsonProperty("stck_avls")
        private String marketCap;   // 시가총액 (억원)

        @JsonProperty("data_rank")
        private String rank;        // 순위
    }
}
