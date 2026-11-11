package com.example.volunteer.service;

import com.example.volunteer.vo.AdmissionImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/** 管理端录取记录批量导入服务。 */
public interface AdmissionImportService {

    AdmissionImportResultVO importRecords(Long universityId, MultipartFile file);
}
