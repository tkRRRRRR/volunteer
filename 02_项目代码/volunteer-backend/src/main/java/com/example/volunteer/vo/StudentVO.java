package com.example.volunteer.vo;

import com.example.volunteer.entity.User;

import java.time.LocalDateTime;

/** 管理端学生账号视图，明确排除密码哈希。 */
public record StudentVO(
        Long id,
        String username,
        String nickname,
        String province,
        Integer examYear,
        String subjectTrack,
        Integer score,
        Integer provinceRank,
        LocalDateTime createdAt
) {
    public static StudentVO from(User user) {
        return new StudentVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getProvince(),
                user.getExamYear(),
                user.getSubjectTrack(),
                user.getScore(),
                user.getProvinceRank(),
                user.getCreatedAt()
        );
    }
}
