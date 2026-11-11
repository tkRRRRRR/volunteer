package com.example.volunteer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 管理端新增或更新录取记录的表单数据。 */
@Data
public class AdmissionRecordSaveRequest {

    @NotNull(message = "请选择具体专业")
    private Long majorId;

    @NotBlank(message = "请输入招生省份")
    private String province;

    @NotNull(message = "请输入录取年份")
    private Integer year;

    @NotBlank(message = "请输入科类或选科组合")
    private String subjectTrack;

    @NotBlank(message = "请输入录取批次")
    private String batch;

    @NotNull(message = "请输入最低分")
    @Min(value = 0, message = "最低分不能小于0")
    @Max(value = 1000, message = "最低分不能大于1000")
    private Integer minScore;

    @NotNull(message = "请输入最低位次")
    @Min(value = 1, message = "最低位次必须大于0")
    private Integer minRank;

    @Min(value = 0, message = "计划数不能小于0")
    private Integer planCount;

    @NotBlank(message = "请输入数据来源")
    private String source;
}
