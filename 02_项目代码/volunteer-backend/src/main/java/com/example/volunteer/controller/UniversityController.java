package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.PageResult;
import com.example.volunteer.dto.UniversityQuery;
import com.example.volunteer.service.UniversityService;
import com.example.volunteer.vo.UniversityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "院校查询")
@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    private final UniversityService universityService;

    public UniversityController(UniversityService universityService) {
        this.universityService = universityService;
    }

    @Operation(summary = "分页查询院校")
    @GetMapping
    public ApiResponse<PageResult<UniversityVO>> search(@Valid UniversityQuery query) {
        return ApiResponse.ok(universityService.search(query));
    }

    @Operation(summary = "查看院校详情")
    @GetMapping("/{id}")
    public ApiResponse<UniversityVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(universityService.getById(id));
    }
}
