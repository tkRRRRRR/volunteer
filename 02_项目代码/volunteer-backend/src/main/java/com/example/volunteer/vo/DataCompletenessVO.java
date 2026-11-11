package com.example.volunteer.vo;

/** 管理端统计中需要管理员关注的数据缺失数量。 */
public record DataCompletenessVO(
        long universitiesWithoutMajors,
        long universitiesWithoutAdmissions,
        long majorsWithoutAdmissions
) {
}
