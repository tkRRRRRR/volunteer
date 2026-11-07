package com.example.volunteer.common;

public final class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        CURRENT_USER.set(loginUser);
    }

    public static LoginUser get() {
        LoginUser loginUser = CURRENT_USER.get();
        if (loginUser == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return loginUser;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
