package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.volunteer.common.BusinessException;
import com.example.volunteer.entity.AdmissionRecord;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.mapper.AdmissionRecordMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.service.AdmissionImportService;
import com.example.volunteer.service.importing.AdmissionFileParser;
import com.example.volunteer.service.importing.ParsedAdmissionRow;
import com.example.volunteer.vo.AdmissionImportErrorVO;
import com.example.volunteer.vo.AdmissionImportResultVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 对批量文件执行与单条录入一致的专业、范围和重复校验，合法行部分写入。
 */
@Service
public class AdmissionImportServiceImpl implements AdmissionImportService {

    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;
    private final AdmissionRecordMapper admissionRecordMapper;
    private final AdmissionFileParser parser;

    public AdmissionImportServiceImpl(
            UniversityMapper universityMapper,
            MajorMapper majorMapper,
            AdmissionRecordMapper admissionRecordMapper,
            AdmissionFileParser parser
    ) {
        this.universityMapper = universityMapper;
        this.majorMapper = majorMapper;
        this.admissionRecordMapper = admissionRecordMapper;
        this.parser = parser;
    }

    @Override
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public AdmissionImportResultVO importRecords(Long universityId, MultipartFile file) {
        requireUniversity(universityId);
        Map<Long, Major> majors = majorMapper.selectList(
                        new LambdaQueryWrapper<Major>()
                                .eq(Major::getUniversityId, universityId)
                ).stream()
                .collect(Collectors.toMap(Major::getId, Function.identity()));
        Set<AdmissionKey> databaseKeys = admissionRecordMapper.selectList(
                        new LambdaQueryWrapper<AdmissionRecord>()
                                .eq(AdmissionRecord::getUniversityId, universityId)
                ).stream()
                .map(AdmissionKey::from)
                .collect(Collectors.toSet());

        List<ParsedAdmissionRow> rows = parser.parse(file);
        List<AdmissionImportErrorVO> errors = new ArrayList<>();
        Set<AdmissionKey> fileKeys = new HashSet<>();
        List<AdmissionRecord> validRecords = new ArrayList<>();
        int currentYear = Year.now().getValue();

        for (ParsedAdmissionRow row : rows) {
            Map<String, String> values = normalizedValues(row.values());
            List<String> messages = new ArrayList<>();
            Long majorId = parseLong(values.get("majorId"), "专业编号", messages);
            String majorName = values.get("majorName");
            Integer year = parseInteger(values.get("year"), "录取年份", messages);
            Integer minScore = parseInteger(values.get("minScore"), "最低分", messages);
            Integer minRank = parseInteger(values.get("minRank"), "最低位次", messages);
            Integer planCount = parseOptionalInteger(values.get("planCount"), "计划数", messages);

            requireText(values, "province", "招生省份", messages);
            requireText(values, "subjectTrack", "科类或选科组合", messages);
            requireText(values, "batch", "录取批次", messages);
            requireText(values, "source", "数据来源", messages);

            Major major = majorId == null ? null : majors.get(majorId);
            if (majorId != null && major == null) {
                messages.add("专业不存在或不属于当前院校");
            }
            if (major != null && StringUtils.hasText(majorName)
                    && !major.getName().equals(majorName)) {
                messages.add("专业名称与编号不匹配");
            }
            if (year != null && (year < 2000 || year > currentYear)) {
                messages.add(year > currentYear
                        ? "录取年份不能晚于当前年份"
                        : "录取年份不能早于2000年");
            }
            if (minScore != null && minScore < 0) {
                messages.add("最低分不能小于0");
            } else if (minScore != null && minScore > 1000) {
                messages.add("最低分不能大于1000");
            }
            if (minRank != null && minRank <= 0) {
                messages.add("最低位次必须大于0");
            }
            if (planCount != null && planCount < 0) {
                messages.add("计划数不能小于0");
            }

            AdmissionKey key = keyOf(
                    majorId,
                    year,
                    values.get("province"),
                    values.get("subjectTrack"),
                    values.get("batch")
            );
            if (key != null && !messages.isEmpty()) {
                // 范围或专业有误时不参与重复判断，避免在同一条错误里叠加误导信息。
                key = null;
            }
            if (key != null && databaseKeys.contains(key)) {
                messages.add("数据库中已有录取记录");
            } else if (key != null && fileKeys.contains(key)) {
                messages.add("文件内重复：同一专业、年份、省份、科类和批次只能出现一次");
            }

            if (!messages.isEmpty()) {
                errors.add(new AdmissionImportErrorVO(
                        row.rowNumber(),
                        values.get("majorId"),
                        majorName,
                        String.join("；", messages)
                ));
                continue;
            }

            AdmissionRecord record = toRecord(universityId, values, majorId, year, minScore,
                    minRank, planCount);
            validRecords.add(record);
            fileKeys.add(Objects.requireNonNull(key));
        }

        validRecords.forEach(admissionRecordMapper::insert);
        return new AdmissionImportResultVO(
                rows.size(),
                validRecords.size(),
                errors.size(),
                errors
        );
    }

    private University requireUniversity(Long universityId) {
        University university = universityMapper.selectById(universityId);
        if (university == null) {
            throw BusinessException.notFound("院校不存在");
        }
        return university;
    }

    private AdmissionRecord toRecord(
            Long universityId,
            Map<String, String> values,
            Long majorId,
            Integer year,
            Integer minScore,
            Integer minRank,
            Integer planCount
    ) {
        AdmissionRecord record = new AdmissionRecord();
        record.setUniversityId(universityId);
        record.setMajorId(majorId);
        record.setProvince(values.get("province"));
        record.setYear(year);
        record.setSubjectTrack(values.get("subjectTrack"));
        record.setBatch(values.get("batch"));
        record.setMinScore(minScore);
        record.setMinRank(minRank);
        record.setPlanCount(planCount);
        record.setSource(values.get("source"));
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    private Map<String, String> normalizedValues(Map<String, String> values) {
        Map<String, String> normalized = new HashMap<>();
        values.forEach((key, value) -> normalized.put(
                key,
                value == null ? "" : value.trim()
        ));
        return normalized;
    }

    private void requireText(
            Map<String, String> values,
            String field,
            String label,
            List<String> messages
    ) {
        if (!StringUtils.hasText(values.get(field))) {
            messages.add(label + "不能为空");
        }
    }

    private Long parseLong(String value, String label, List<String> messages) {
        if (!StringUtils.hasText(value)) {
            messages.add(label + "不能为空");
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            messages.add(label + "格式不正确");
            return null;
        }
    }

    private Integer parseInteger(String value, String label, List<String> messages) {
        if (!StringUtils.hasText(value)) {
            messages.add(label + "不能为空");
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            messages.add(label + "格式不正确");
            return null;
        }
    }

    private Integer parseOptionalInteger(String value, String label, List<String> messages) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            messages.add(label + "格式不正确");
            return null;
        }
    }

    private AdmissionKey keyOf(
            Long majorId,
            Integer year,
            String province,
            String subjectTrack,
            String batch
    ) {
        if (majorId == null || year == null
                || !StringUtils.hasText(province)
                || !StringUtils.hasText(subjectTrack)
                || !StringUtils.hasText(batch)) {
            return null;
        }
        return new AdmissionKey(
                majorId,
                province.trim(),
                year,
                subjectTrack.trim(),
                batch.trim()
        );
    }

    private record AdmissionKey(
            Long majorId,
            String province,
            Integer year,
            String subjectTrack,
            String batch
    ) {
        private static AdmissionKey from(AdmissionRecord record) {
            return new AdmissionKey(
                    record.getMajorId(),
                    record.getProvince(),
                    record.getYear(),
                    record.getSubjectTrack(),
                    record.getBatch()
            );
        }
    }
}
