package com.example.volunteer.vo;

/** 文件中的单行导入错误。 */
public record AdmissionImportErrorVO(
        int rowNumber,
        String majorId,
        String majorName,
        String message
) {
}
