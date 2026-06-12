package com.ssafy.dartservice.recommendation;

import com.ssafy.dartservice.recommendation.dto.RecommendItem;
import com.ssafy.dartservice.recommendation.RecommendationService;
import com.ssafy.dartservice.recommendation.dto.RecommendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    //수정 필요
    @GetMapping("/")
    public RecommendResponse recommend(@RequestParam Long userId) {
        return recommendationService.recommend(userId);
    }

}
