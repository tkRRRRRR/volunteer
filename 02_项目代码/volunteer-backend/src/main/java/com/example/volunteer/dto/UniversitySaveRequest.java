package com.example.volunteer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UniversitySaveRequest {
    @NotBlank(message = "请输入院校名称")
    private String name;

    @NotBlank(message = "请输入所在省份")
    private String province;

    @NotBlank(message = "请输入所在城市")
    private String city;

    @NotBlank(message = "请输入院校层次")
    private String level;

    private String type;
    private String description;
    private String website;
    private String tags;
}
