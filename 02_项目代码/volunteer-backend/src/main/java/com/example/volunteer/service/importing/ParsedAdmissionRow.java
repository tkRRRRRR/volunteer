package com.example.volunteer.service.importing;

import java.util.Map;

/** 文件解析后的原始文本行，字段转换和业务校验由导入服务完成。 */
public record ParsedAdmissionRow(int rowNumber, Map<String, String> values) {

    public ParsedAdmissionRow {
        values = Map.copyOf(values);
    }
}
