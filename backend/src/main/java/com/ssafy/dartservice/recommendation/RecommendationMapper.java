package com.ssafy.dartservice.recommendation;

import com.ssafy.dartservice.recommendation.dto.RecommendItem;
import com.ssafy.dartservice.recommendation.dto.RecommendSaveDto;
import com.ssafy.dartservice.recommendation.dto.StockInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendationMapper {
    List<StockInfo> findAll();
    List<RecommendItem> findAllWithFinancials();
    void deleteByUserId(@Param("userId") Long userId);
    void insertRecommendations(@Param("list") List<RecommendSaveDto> list);
}
