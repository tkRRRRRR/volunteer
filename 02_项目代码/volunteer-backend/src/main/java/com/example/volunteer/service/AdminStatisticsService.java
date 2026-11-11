package com.example.volunteer.service;

import com.example.volunteer.common.PageResult;
import com.example.volunteer.dto.AdminStudentQuery;
import com.example.volunteer.vo.AdminStatisticsOverviewVO;
import com.example.volunteer.vo.StudentVO;

/** 管理端只读统计服务。 */
public interface AdminStatisticsService {

    AdminStatisticsOverviewVO getOverview();

    PageResult<StudentVO> pageStudents(AdminStudentQuery query);
}
