package com.example.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admission_record")
public class AdmissionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long universityId;
    private Long majorId;
    private String province;
    private Integer year;
    private String subjectTrack;
    private String batch;
    private Integer minScore;
    private Integer minRank;
    private Integer planCount;
    private String source;
    private LocalDateTime createdAt;
}
