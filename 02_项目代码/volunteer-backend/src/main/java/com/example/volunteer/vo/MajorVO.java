package com.example.volunteer.vo;

import com.example.volunteer.entity.Major;

/** 院校详情页展示的专业信息。 */
public record MajorVO(
        Long id,
        String name,
        String category,
        String subjectRequirement,
        String description
) {
    public static MajorVO from(Major major) {
        return new MajorVO(
                major.getId(),
                major.getName(),
                major.getCategory(),
                major.getSubjectRequirement(),
                major.getDescription()
        );
    }
}
