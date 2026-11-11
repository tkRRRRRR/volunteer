package com.example.volunteer.vo;

/** 某一年录取记录的数量和平均最低分、平均最低位次。 */
public record AdmissionYearTrendVO(
        Integer year,
        long recordCount,
        Integer averageMinScore,
        Integer averageMinRank
) {
}
