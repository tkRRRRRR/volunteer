package com.example.volunteer.vo;

public record FavoriteVO(
        Long id,
        Long universityId,
        String universityName,
        String province,
        String city,
        String universityLevel,
        Long majorId,
        String majorName
) {
}
