package com.example.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 院校开设的专业信息，包含专业门类和选科要求。 */
@Data
@TableName("major")
public class Major {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long universityId;
    private String name;
    private String category;
    private String subjectRequirement;
    private String description;
    private LocalDateTime createdAt;
}
