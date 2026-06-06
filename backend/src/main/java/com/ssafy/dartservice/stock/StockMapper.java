package com.ssafy.dartservice.stock;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockMapper {

    void insertStock(@Param("stockCode") String stockCode,
                     @Param("corpCode") String corpCode,
                     @Param("stockName") String stockName,
                     @Param("market") String market,
                     @Param("sector") String sector);
}