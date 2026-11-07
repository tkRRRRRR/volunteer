package com.example.volunteer.vo;

import java.io.Serializable;

public record RecommendationVO(
        Long universityId,
        String universityName,
        String province,
        String city,
        String universityLevel,
        Long majorId,
        String majorName,
        String subjectRequirement,
        Integer latestYear,
        Integer averageMinScore,
        Integer averageMinRank,
        Integer matchScore,
        String matchLevel,
        String reason,
        String source
) implements Serializable {
}
