package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.dto.RecommendationRequest;
import com.example.volunteer.service.RecommendationService;
import com.example.volunteer.vo.RecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "志愿辅助推荐")
@RestController
@RequestMapping("/api/recommendations")
@RequireRole({"STUDENT", "ADMIN"})
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "生成冲稳保辅助结果")
    @PostMapping
    public ApiResponse<List<RecommendationVO>> recommend(@Valid @RequestBody RecommendationRequest request) {
        return ApiResponse.ok(recommendationService.recommend(request));
    }
}
