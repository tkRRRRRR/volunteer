package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.common.UserContext;
import com.example.volunteer.dto.LoginRequest;
import com.example.volunteer.dto.ProfileUpdateRequest;
import com.example.volunteer.dto.RegisterRequest;
import com.example.volunteer.service.AuthService;
import com.example.volunteer.vo.AuthVO;
import com.example.volunteer.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "账号")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResponse<AuthVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<AuthVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "获取当前用户资料")
    @RequireRole({"STUDENT", "ADMIN"})
    @GetMapping("/profile")
    public ApiResponse<UserVO> profile() {
        return ApiResponse.ok(authService.getProfile(UserContext.get().id()));
    }

    @Operation(summary = "更新当前用户资料")
    @RequireRole({"STUDENT", "ADMIN"})
    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(authService.updateProfile(UserContext.get().id(), request));
    }
}
