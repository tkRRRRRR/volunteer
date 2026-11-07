package com.example.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 院校基础信息，名称、地区、层次和标签用于查询筛选。 */
@Data
@TableName("university")
public class University {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String province;
    private String city;
    private String level;
    private String type;
    private String description;
    private String website;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
