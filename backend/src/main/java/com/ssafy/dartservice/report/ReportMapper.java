package com.ssafy.dartservice.report;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    List<StockSearchResponseDto> searchByKeyword(@Param("keyword") String keyword);
}