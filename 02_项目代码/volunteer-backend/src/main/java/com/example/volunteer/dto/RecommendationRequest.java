package com.example.volunteer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Objects;

@Data
public class RecommendationRequest {
    @NotBlank(message = "请选择考试省份")
    private String province;

    @NotNull(message = "请输入考试年份")
    @Min(value = 2020, message = "考试年份不正确")
    @Max(value = 2035, message = "考试年份不正确")
    private Integer examYear;

    @NotBlank(message = "请选择科类或选科组合")
    private String subjectTrack;

    @NotNull(message = "请输入高考分数")
    @Min(value = 0, message = "分数不能小于0")
    @Max(value = 750, message = "分数不能大于750")
    private Integer score;

    @NotNull(message = "请输入全省位次")
    @Min(value = 1, message = "位次必须大于0")
    private Integer provinceRank;

    private String preferredProvince;
    private String universityLevel;
    private String keyword;

    public String cacheKey() {
        return String.join(":",
                Objects.toString(province, ""),
                Objects.toString(examYear, ""),
                Objects.toString(subjectTrack, ""),
                Objects.toString(score, ""),
                Objects.toString(provinceRank, ""),
                Objects.toString(preferredProvince, ""),
                Objects.toString(universityLevel, ""),
                Objects.toString(keyword, "")
        );
    }
}
