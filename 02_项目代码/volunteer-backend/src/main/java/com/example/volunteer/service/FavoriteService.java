package com.example.volunteer.service;

import com.example.volunteer.dto.FavoriteRequest;
import com.example.volunteer.vo.FavoriteVO;

import java.util.List;

public interface FavoriteService {
    List<FavoriteVO> list(Long userId);

    FavoriteVO add(Long userId, FavoriteRequest request);

    void remove(Long userId, Long favoriteId);
}
