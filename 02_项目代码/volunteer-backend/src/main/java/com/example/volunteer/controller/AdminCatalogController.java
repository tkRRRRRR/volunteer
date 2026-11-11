package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.dto.MajorSaveRequest;
import com.example.volunteer.service.AdminCatalogService;
import com.example.volunteer.vo.MajorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端院校数据维护")
@RestController
@RequestMapping("/api/admin/universities")
@RequireRole({"ADMIN"})
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    public AdminCatalogController(AdminCatalogService adminCatalogService) {
        this.adminCatalogService = adminCatalogService;
    }

    @Operation(summary = "查询院校专业")
    @GetMapping("/{universityId}/majors")
    public ApiResponse<List<MajorVO>> listMajors(
            @PathVariable Long universityId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(adminCatalogService.listMajors(universityId, keyword));
    }

    @Operation(summary = "新增院校专业")
    @PostMapping("/{universityId}/majors")
    public ApiResponse<MajorVO> createMajor(
            @PathVariable Long universityId,
            @Valid @RequestBody MajorSaveRequest request
    ) {
        return ApiResponse.ok(adminCatalogService.createMajor(universityId, request));
    }

    @Operation(summary = "更新院校专业")
    @PutMapping("/{universityId}/majors/{majorId}")
    public ApiResponse<MajorVO> updateMajor(
            @PathVariable Long universityId,
            @PathVariable Long majorId,
            @Valid @RequestBody MajorSaveRequest request
    ) {
        return ApiResponse.ok(adminCatalogService.updateMajor(universityId, majorId, request));
    }

    @Operation(summary = "删除院校专业")
    @DeleteMapping("/{universityId}/majors/{majorId}")
    public ApiResponse<Void> deleteMajor(
            @PathVariable Long universityId,
            @PathVariable Long majorId
    ) {
        adminCatalogService.deleteMajor(universityId, majorId);
        return ApiResponse.ok();
    }
}
