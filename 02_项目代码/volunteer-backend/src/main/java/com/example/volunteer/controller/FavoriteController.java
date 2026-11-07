package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.common.UserContext;
import com.example.volunteer.dto.FavoriteRequest;
import com.example.volunteer.service.FavoriteService;
import com.example.volunteer.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收藏管理")
@RestController
@RequestMapping("/api/favorites")
@RequireRole({"STUDENT", "ADMIN"})
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "查看我的收藏")
    @GetMapping
    public ApiResponse<List<FavoriteVO>> list() {
        return ApiResponse.ok(favoriteService.list(UserContext.get().id()));
    }

    @Operation(summary = "收藏院校或专业")
    @PostMapping
    public ApiResponse<FavoriteVO> add(@Valid @RequestBody FavoriteRequest request) {
        return ApiResponse.ok(favoriteService.add(UserContext.get().id(), request));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        favoriteService.remove(UserContext.get().id(), id);
        return ApiResponse.ok();
    }
}
