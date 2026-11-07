-- 高考志愿填报辅助系统数据库结构。
-- 该脚本会删除并重建同名业务表，仅用于开发、演示和论文测试环境。
CREATE DATABASE IF NOT EXISTS gaokao_volunteer
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE gaokao_volunteer;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS admission_record;
DROP TABLE IF EXISTS major;
DROP TABLE IF EXISTS university;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(30) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  nickname VARCHAR(30) NOT NULL COMMENT '昵称',
  role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色',
  province VARCHAR(30) NULL COMMENT '考试省份',
  exam_year INT NULL COMMENT '考试年份',
  subject_track VARCHAR(30) NULL COMMENT '科类或选科组合',
  score INT NULL COMMENT '高考分数',
  province_rank INT NULL COMMENT '全省位次',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

CREATE TABLE university (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(100) NOT NULL COMMENT '院校名称',
  province VARCHAR(30) NOT NULL COMMENT '所在省份',
  city VARCHAR(30) NOT NULL COMMENT '所在城市',
  level VARCHAR(30) NOT NULL COMMENT '院校层次',
  type VARCHAR(30) NULL COMMENT '院校类型',
  description TEXT NULL COMMENT '简介',
  website VARCHAR(255) NULL COMMENT '官网',
  tags VARCHAR(255) NULL COMMENT '标签',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_university_name (name),
  KEY idx_university_region (province, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院校';

CREATE TABLE major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  university_id BIGINT NOT NULL COMMENT '院校 ID',
  name VARCHAR(100) NOT NULL COMMENT '专业名称',
  category VARCHAR(50) NULL COMMENT '专业门类',
  subject_requirement VARCHAR(100) NULL COMMENT '选科要求',
  description TEXT NULL COMMENT '专业简介',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_major_university (university_id),
  KEY idx_major_name (name),
  CONSTRAINT fk_major_university FOREIGN KEY (university_id) REFERENCES university (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业';

CREATE TABLE admission_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  university_id BIGINT NOT NULL COMMENT '院校 ID',
  major_id BIGINT NULL COMMENT '专业 ID',
  province VARCHAR(30) NOT NULL COMMENT '招生省份',
  year INT NOT NULL COMMENT '录取年份',
  subject_track VARCHAR(30) NOT NULL COMMENT '科类或选科组合',
  batch VARCHAR(30) NOT NULL COMMENT '录取批次',
  min_score INT NOT NULL COMMENT '最低分',
  min_rank INT NOT NULL COMMENT '最低位次',
  plan_count INT NULL COMMENT '计划数',
  source VARCHAR(255) NOT NULL COMMENT '数据来源',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_admission_query (province, subject_track, year),
  KEY idx_admission_university (university_id),
  KEY idx_admission_major (major_id),
  CONSTRAINT fk_admission_university FOREIGN KEY (university_id) REFERENCES university (id),
  CONSTRAINT fk_admission_major FOREIGN KEY (major_id) REFERENCES major (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历年录取记录';

-- 收藏既可以指向整所院校，也可以指向院校下的具体专业。
CREATE TABLE favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  university_id BIGINT NOT NULL COMMENT '院校 ID',
  major_id BIGINT NULL COMMENT '专业 ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_item (user_id, university_id, major_id),
  KEY idx_favorite_user (user_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_favorite_university FOREIGN KEY (university_id) REFERENCES university (id),
  CONSTRAINT fk_favorite_major FOREIGN KEY (major_id) REFERENCES major (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏';
