package com.example.volunteer.vo;

/** 管理端收藏排行中的院校统计。 */
public record FavoriteUniversityStatVO(
        Long universityId,
        String universityName,
        long favoriteCount
) {
}
