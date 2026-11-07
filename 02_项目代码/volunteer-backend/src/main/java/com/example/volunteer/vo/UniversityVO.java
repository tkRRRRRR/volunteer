package com.example.volunteer.vo;

import com.example.volunteer.entity.University;

public record UniversityVO(
        Long id,
        String name,
        String province,
        String city,
        String level,
        String type,
        String description,
        String website,
        String tags
) {
    public static UniversityVO from(University university) {
        return new UniversityVO(
                university.getId(),
                university.getName(),
                university.getProvince(),
                university.getCity(),
                university.getLevel(),
                university.getType(),
                university.getDescription(),
                university.getWebsite(),
                university.getTags()
        );
    }
}
