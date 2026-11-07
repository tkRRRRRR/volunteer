package com.example.volunteer.config;

import com.example.volunteer.common.BusinessException;
import com.example.volunteer.common.LoginUser;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.common.UserContext;
import com.example.volunteer.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BusinessException.unauthorized("请先登录");
        }

        try {
            Claims claims = jwtUtil.parseToken(authorization.substring(7));
            Object uid = claims.get("uid");
            Long userId = uid instanceof Number number ? number.longValue() : null;
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            if (userId == null || username == null || role == null) {
                throw BusinessException.unauthorized("登录凭证无效");
            }
            if (Arrays.stream(requireRole.value()).noneMatch(role::equals)) {
                throw BusinessException.forbidden("当前账号无权访问该功能");
            }
            UserContext.set(new LoginUser(userId, username, role));
            return true;
        } catch (BusinessException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw BusinessException.unauthorized("登录已失效，请重新登录");
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        UserContext.clear();
    }
}
