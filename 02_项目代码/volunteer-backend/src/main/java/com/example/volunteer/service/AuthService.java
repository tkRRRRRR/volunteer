package com.example.volunteer.service;

import com.example.volunteer.dto.LoginRequest;
import com.example.volunteer.dto.ProfileUpdateRequest;
import com.example.volunteer.dto.RegisterRequest;
import com.example.volunteer.vo.AuthVO;
import com.example.volunteer.vo.UserVO;

public interface AuthService {
    AuthVO register(RegisterRequest request);

    AuthVO login(LoginRequest request);

    UserVO getProfile(Long userId);

    UserVO updateProfile(Long userId, ProfileUpdateRequest request);
}
