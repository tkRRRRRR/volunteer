package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.dto.UniversitySaveRequest;
import com.example.volunteer.service.UniversityService;
import com.example.volunteer.vo.UniversityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端院校维护")
@RestController
@RequestMapping("/api/admin/universities")
@RequireRole({"ADMIN"})
public class AdminUniversityController {

    private final UniversityService universityService;

    public AdminUniversityController(UniversityService universityService) {
        this.universityService = universityService;
    }

    @Operation(summary = "新增院校")
    @PostMapping
    public ApiResponse<UniversityVO> create(@Valid @RequestBody UniversitySaveRequest request) {
        return ApiResponse.ok(universityService.create(request));
    }

    @Operation(summary = "更新院校")
    @PutMapping("/{id}")
    public ApiResponse<UniversityVO> update(
            @PathVariable Long id,
            @Valid @RequestBody UniversitySaveRequest request
    ) {
        return ApiResponse.ok(universityService.update(id, request));
    }

    @Operation(summary = "删除院校")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        universityService.delete(id);
        return ApiResponse.ok();
    }
}
