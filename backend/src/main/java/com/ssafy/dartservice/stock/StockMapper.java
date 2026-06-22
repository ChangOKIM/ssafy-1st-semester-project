package com.ssafy.dartservice.stock;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockMapper {

    int countStocks();
    int countFinancials();

    void insertStock(@Param("stockCode") String stockCode,
                     @Param("corpCode") String corpCode,
                     @Param("stockName") String stockName,
                     @Param("market") String market,
                     @Param("sector") String sector);

    void insertStockInfo(@Param("stockCode") String stockCode,
                         @Param("sector") String sector);
}