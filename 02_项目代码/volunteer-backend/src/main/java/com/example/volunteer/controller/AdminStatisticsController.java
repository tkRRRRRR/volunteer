package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.PageResult;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.dto.AdminStudentQuery;
import com.example.volunteer.service.AdminStatisticsService;
import com.example.volunteer.vo.AdminStatisticsOverviewVO;
import com.example.volunteer.vo.StudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端统计与学生账号")
@RestController
@RequestMapping("/api/admin")
@RequireRole({"ADMIN"})
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    public AdminStatisticsController(AdminStatisticsService adminStatisticsService) {
        this.adminStatisticsService = adminStatisticsService;
    }

    @Operation(summary = "获取管理端统计概览")
    @GetMapping("/statistics/overview")
    public ApiResponse<AdminStatisticsOverviewVO> overview() {
        return ApiResponse.ok(adminStatisticsService.getOverview());
    }

    @Operation(summary = "分页查看学生账号")
    @GetMapping("/students")
    public ApiResponse<PageResult<StudentVO>> students(
            @Valid AdminStudentQuery query
    ) {
        return ApiResponse.ok(adminStatisticsService.pageStudents(query));
    }
}
