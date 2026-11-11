package com.example.volunteer.vo;

import java.util.List;

/** 管理端统计页面一次请求需要的全部概览数据。 */
public record AdminStatisticsOverviewVO(
        long universityCount,
        long majorCount,
        long admissionRecordCount,
        long studentCount,
        long favoriteCount,
        List<StatisticItemVO> universityLevels,
        List<StatisticItemVO> universityProvinces,
        List<AdmissionYearTrendVO> admissionTrends,
        List<FavoriteUniversityStatVO> topFavorites,
        DataCompletenessVO dataCompleteness
) {
    public AdminStatisticsOverviewVO {
        universityLevels = List.copyOf(universityLevels);
        universityProvinces = List.copyOf(universityProvinces);
        admissionTrends = List.copyOf(admissionTrends);
        topFavorites = List.copyOf(topFavorites);
    }
}
