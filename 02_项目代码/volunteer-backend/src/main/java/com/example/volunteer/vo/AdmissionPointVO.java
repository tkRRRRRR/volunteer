package com.example.volunteer.vo;

/**
 * 单个专业某一年的录取数据点，趋势图直接使用年份、最低分和最低位次。
 */
public record AdmissionPointVO(
        Long majorId,
        String majorName,
        Integer year,
        Integer minScore,
        Integer minRank,
        String subjectTrack,
        String batch,
        String source
) {
}
