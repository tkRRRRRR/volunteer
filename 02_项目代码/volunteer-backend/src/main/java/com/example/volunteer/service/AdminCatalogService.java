package com.example.volunteer.service;

import com.example.volunteer.dto.MajorSaveRequest;
import com.example.volunteer.vo.MajorVO;

import java.util.List;

/** 管理端院校专业与录取数据维护服务。 */
public interface AdminCatalogService {

    List<MajorVO> listMajors(Long universityId, String keyword);

    MajorVO createMajor(Long universityId, MajorSaveRequest request);

    MajorVO updateMajor(Long universityId, Long majorId, MajorSaveRequest request);

    void deleteMajor(Long universityId, Long majorId);
}
