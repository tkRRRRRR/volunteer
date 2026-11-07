package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.volunteer.common.BusinessException;
import com.example.volunteer.dto.LoginRequest;
import com.example.volunteer.dto.ProfileUpdateRequest;
import com.example.volunteer.dto.RegisterRequest;
import com.example.volunteer.entity.User;
import com.example.volunteer.mapper.UserMapper;
import com.example.volunteer.service.AuthService;
import com.example.volunteer.util.JwtUtil;
import com.example.volunteer.vo.AuthVO;
import com.example.volunteer.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public AuthVO register(RegisterRequest request) {
        String username = request.getUsername().trim();
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setRole("STUDENT");
        user.setProvince(request.getProvince());
        user.setExamYear(request.getExamYear());
        user.setSubjectTrack(request.getSubjectTrack());
        user.setScore(request.getScore());
        user.setProvinceRank(request.getProvinceRank());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return buildAuth(user);
    }

    @Override
    public AuthVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        return buildAuth(user);
    }

    @Override
    public UserVO getProfile(Long userId) {
        return UserVO.from(requireUser(userId));
    }

    @Override
    @Transactional
    public UserVO updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = requireUser(userId);
        user.setNickname(request.getNickname().trim());
        user.setProvince(request.getProvince());
        user.setExamYear(request.getExamYear());
        user.setSubjectTrack(request.getSubjectTrack());
        user.setScore(request.getScore());
        user.setProvinceRank(request.getProvinceRank());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    private AuthVO buildAuth(User user) {
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthVO(token, UserVO.from(user));
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }
}
