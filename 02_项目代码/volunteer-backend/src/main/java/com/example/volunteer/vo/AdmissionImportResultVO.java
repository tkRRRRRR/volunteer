package com.example.volunteer.vo;

import java.util.List;

/** 批量导入的总体结果，合法行与错误行可以同时存在。 */
public record AdmissionImportResultVO(
        int totalRows,
        int successCount,
        int failedCount,
        List<AdmissionImportErrorVO> errors
) {
    public AdmissionImportResultVO {
        errors = List.copyOf(errors);
    }
}
