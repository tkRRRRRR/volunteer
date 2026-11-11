package com.example.volunteer.vo;

import com.example.volunteer.entity.AdmissionRecord;

/** 管理端录取记录列表使用的完整视图。 */
public record AdminAdmissionRecordVO(
        Long id,
        Long universityId,
        Long majorId,
        String majorName,
        String province,
        Integer year,
        String subjectTrack,
        String batch,
        Integer minScore,
        Integer minRank,
        Integer planCount,
        String source
) {
    public static AdminAdmissionRecordVO from(AdmissionRecord record, String majorName) {
        return new AdminAdmissionRecordVO(
                record.getId(),
                record.getUniversityId(),
                record.getMajorId(),
                majorName,
                record.getProvince(),
                record.getYear(),
                record.getSubjectTrack(),
                record.getBatch(),
                record.getMinScore(),
                record.getMinRank(),
                record.getPlanCount(),
                record.getSource()
        );
    }
}
