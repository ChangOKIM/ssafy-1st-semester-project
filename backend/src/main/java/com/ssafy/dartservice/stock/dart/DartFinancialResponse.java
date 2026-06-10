package com.ssafy.dartservice.stock.dart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DartFinancialResponse {
    private String status;
    private String message;
    private List<Item> list;

    @Data
    public static class Item {
        @JsonProperty("account_id")    private String accountId;
        @JsonProperty("thstrm_amount")    private String thstrmAmount;
        @JsonProperty("frmtrm_amount")    private String frmtrmAmount;     // 추가
        @JsonProperty("bfefrmtrm_amount") private String bfefrmtrmAmount;  // 추가
        @JsonProperty("sj_div") private String sjDiv;  // BS / IS / CIS / CF / SCE
    }
}
