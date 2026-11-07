package com.example.volunteer.service;

import com.example.volunteer.common.PageResult;
import com.example.volunteer.dto.UniversityQuery;
import com.example.volunteer.dto.UniversitySaveRequest;
import com.example.volunteer.vo.UniversityVO;

public interface UniversityService {
    PageResult<UniversityVO> search(UniversityQuery query);

    UniversityVO getById(Long id);

    UniversityVO create(UniversitySaveRequest request);

    UniversityVO update(Long id, UniversitySaveRequest request);

    void delete(Long id);
}
