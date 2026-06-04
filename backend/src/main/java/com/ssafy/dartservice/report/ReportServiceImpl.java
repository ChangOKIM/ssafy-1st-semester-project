package com.ssafy.dartservice.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    @Override
    public List<StockSearchResponseDto> searchStocks(String keyword) {
        log.info("종목 검색 요청 - keyword: {}", keyword);
        List<StockSearchResponseDto> result = reportMapper.searchByKeyword(keyword);
        log.info("종목 검색 결과 - {}건", result.size());
        return result;
    }
}