package com.ssafy.dartservice.stock;

import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
                         @Param("sector") String sector,
                         @Param("intro") String intro);

    List<String> getSectors();

    List<StockSearchResponseDto> listStocks(@Param("keyword") String keyword,
                                            @Param("sector") String sector,
                                            @Param("sort") String sort,
                                            @Param("size") int size,
                                            @Param("offset") int offset);

    int countStocksByKeyword(@Param("keyword") String keyword,
                             @Param("sector") String sector);

    List<String> selectAllStockCodes();

    List<StockSearchResponseDto> findByExactName(@Param("name") String name);

    List<StockSearchResponseDto> findByNameLike(@Param("name") String name);
}