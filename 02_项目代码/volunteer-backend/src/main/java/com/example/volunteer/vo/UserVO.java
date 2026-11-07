package com.example.volunteer.vo;

import com.example.volunteer.entity.User;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String role,
        String province,
        Integer examYear,
        String subjectTrack,
        Integer score,
        Integer provinceRank
) {
    public static UserVO from(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole(),
                user.getProvince(),
                user.getExamYear(),
                user.getSubjectTrack(),
                user.getScore(),
                user.getProvinceRank()
        );
    }
}
