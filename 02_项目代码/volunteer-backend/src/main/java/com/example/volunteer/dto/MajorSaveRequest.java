package com.example.volunteer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理端新增或更新专业时使用的表单数据。 */
@Data
public class MajorSaveRequest {

    @NotBlank(message = "请输入专业名称")
    private String name;

    private String category;
    private String subjectRequirement;
    private String description;
}
