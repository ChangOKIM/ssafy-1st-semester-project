package com.ssafy.dartservice.recommendation;

import com.ssafy.dartservice.global.security.CustomUserDetails;
import com.ssafy.dartservice.recommendation.dto.RecommendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public RecommendResponse recommend(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return recommendationService.recommend(userDetails.getUser());
    }
}
