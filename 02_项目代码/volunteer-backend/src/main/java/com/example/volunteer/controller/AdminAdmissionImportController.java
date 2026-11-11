package com.example.volunteer.controller;

import com.example.volunteer.common.ApiResponse;
import com.example.volunteer.common.RequireRole;
import com.example.volunteer.service.AdmissionImportService;
import com.example.volunteer.vo.AdmissionImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Tag(name = "管理端录取记录批量导入")
@RestController
@RequestMapping("/api/admin/universities")
@RequireRole({"ADMIN"})
public class AdminAdmissionImportController {

    private final AdmissionImportService admissionImportService;

    public AdminAdmissionImportController(AdmissionImportService admissionImportService) {
        this.admissionImportService = admissionImportService;
    }

    @Operation(summary = "导入录取记录")
    @PostMapping(
            value = "/{universityId}/admission-records/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<AdmissionImportResultVO> importRecords(
            @PathVariable Long universityId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(admissionImportService.importRecords(universityId, file));
    }

    @Operation(summary = "下载录取记录 Excel 模板")
    @GetMapping("/{universityId}/admission-records/import-template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long universityId) {
        byte[] content = admissionImportService.buildTemplate(universityId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("录取记录导入模板.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(content);
    }
}
