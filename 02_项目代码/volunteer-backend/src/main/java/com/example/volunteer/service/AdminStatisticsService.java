package com.example.volunteer.service;

import com.example.volunteer.vo.AdminStatisticsOverviewVO;

/** 管理端只读统计服务。 */
public interface AdminStatisticsService {

    AdminStatisticsOverviewVO getOverview();
}
