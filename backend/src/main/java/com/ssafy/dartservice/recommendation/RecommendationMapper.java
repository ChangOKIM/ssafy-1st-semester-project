package com.ssafy.dartservice.recommendation;

import com.ssafy.dartservice.recommendation.dto.StockInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendationMapper {
    List<StockInfo> findAll();           // 전체 (금융 제외)
    //List<StockInfo> findBySector(@Param("sector") String sector);  // 특정 섹터
}
