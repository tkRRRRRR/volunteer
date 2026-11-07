package com.example.volunteer.service;

import com.example.volunteer.dto.RecommendationRequest;
import com.example.volunteer.vo.RecommendationVO;

import java.util.List;

public interface RecommendationService {
    List<RecommendationVO> recommend(RecommendationRequest request);
}
