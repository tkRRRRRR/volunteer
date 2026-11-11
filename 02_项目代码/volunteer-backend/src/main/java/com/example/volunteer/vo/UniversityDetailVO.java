package com.example.volunteer.vo;

import java.util.List;

/** 院校详情页所需的院校、专业和历年录取数据聚合结果。 */
public record UniversityDetailVO(
        UniversityVO university,
        List<MajorVO> majors,
        List<AdmissionPointVO> admissionPoints
) {
}
