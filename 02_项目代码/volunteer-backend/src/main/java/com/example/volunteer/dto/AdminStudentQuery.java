package com.example.volunteer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 管理端学生账号分页筛选条件。 */
@Data
public class AdminStudentQuery {

    private String keyword;

    @Min(value = 1, message = "页码必须大于0")
    private long page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页最多100条")
    private long size = 10;
}
