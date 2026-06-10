package com.ssafy.dartservice.report;

import com.ssafy.dartservice.report.dto.StockSearchResponseDto;
import com.ssafy.dartservice.stock.dart.StockFinancial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    List<StockSearchResponseDto> searchByKeyword(@Param("keyword") String keyword);
    StockSearchResponseDto findById(@Param("stockCode") String stockCode);
    StockFinancial findFinancial(@Param("stockCode") String stockCode, @Param("baseYear") int baseYear);
    void insertFinancial(StockFinancial financial);
    String findCorpCode(@Param("stockCode") String stockCode);
}