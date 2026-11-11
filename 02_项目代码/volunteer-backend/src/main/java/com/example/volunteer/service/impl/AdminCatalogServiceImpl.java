package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.volunteer.common.BusinessException;
import com.example.volunteer.common.PageResult;
import com.example.volunteer.dto.AdminAdmissionQuery;
import com.example.volunteer.dto.AdmissionRecordSaveRequest;
import com.example.volunteer.dto.MajorSaveRequest;
import com.example.volunteer.entity.AdmissionRecord;
import com.example.volunteer.entity.Favorite;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.mapper.AdmissionRecordMapper;
import com.example.volunteer.mapper.FavoriteMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.service.AdminCatalogService;
import com.example.volunteer.vo.AdminAdmissionRecordVO;
import com.example.volunteer.vo.MajorVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端按院校维护专业数据，并在删除前保护已被业务引用的专业。
 */
@Service
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;
    private final AdmissionRecordMapper admissionRecordMapper;
    private final FavoriteMapper favoriteMapper;

    public AdminCatalogServiceImpl(
            UniversityMapper universityMapper,
            MajorMapper majorMapper,
            AdmissionRecordMapper admissionRecordMapper,
            FavoriteMapper favoriteMapper
    ) {
        this.universityMapper = universityMapper;
        this.majorMapper = majorMapper;
        this.admissionRecordMapper = admissionRecordMapper;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    public List<MajorVO> listMajors(Long universityId, String keyword) {
        requireUniversity(universityId);
        LambdaQueryWrapper<Major> query = new LambdaQueryWrapper<Major>()
                .eq(Major::getUniversityId, universityId);
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            // 管理端可按专业名称或门类快速定位，避免院校专业较多时反复滚动。
            query.and(item -> item.like(Major::getName, normalizedKeyword)
                    .or()
                    .like(Major::getCategory, normalizedKeyword));
        }
        query.orderByAsc(Major::getId);
        return majorMapper.selectList(query).stream().map(MajorVO::from).toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public MajorVO createMajor(Long universityId, MajorSaveRequest request) {
        requireUniversity(universityId);
        Major major = new Major();
        major.setUniversityId(universityId);
        copyMajorRequest(request, major);
        major.setCreatedAt(LocalDateTime.now());
        majorMapper.insert(major);
        return MajorVO.from(major);
    }

    @Override
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public MajorVO updateMajor(Long universityId, Long majorId, MajorSaveRequest request) {
        requireUniversity(universityId);
        Major major = requireMajor(universityId, majorId);
        copyMajorRequest(request, major);
        majorMapper.updateById(major);
        return MajorVO.from(major);
    }

    @Override
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public void deleteMajor(Long universityId, Long majorId) {
        requireUniversity(universityId);
        requireMajor(universityId, majorId);
        // 外键可以阻止误删，但主动检查能向管理员返回可理解的中文原因。
        long admissionCount = admissionRecordMapper.selectCount(
                new LambdaQueryWrapper<AdmissionRecord>()
                        .eq(AdmissionRecord::getMajorId, majorId)
        );
        if (admissionCount > 0) {
            throw BusinessException.badRequest("该专业已有录取记录，不能删除");
        }
        long favoriteCount = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getMajorId, majorId)
        );
        if (favoriteCount > 0) {
            throw BusinessException.badRequest("该专业已被收藏，不能删除");
        }
        majorMapper.deleteById(majorId);
    }

    @Override
    public PageResult<AdminAdmissionRecordVO> pageAdmissionRecords(
            Long universityId,
            AdminAdmissionQuery query
    ) {
        requireUniversity(universityId);
        if (query.getMajorId() != null) {
            requireMajor(universityId, query.getMajorId());
        }

        LambdaQueryWrapper<AdmissionRecord> wrapper = new LambdaQueryWrapper<AdmissionRecord>()
                .eq(AdmissionRecord::getUniversityId, universityId);
        if (query.getMajorId() != null) {
            wrapper.eq(AdmissionRecord::getMajorId, query.getMajorId());
        }
        if (query.getYear() != null) {
            wrapper.eq(AdmissionRecord::getYear, query.getYear());
        }
        if (StringUtils.hasText(query.getProvince())) {
            wrapper.eq(AdmissionRecord::getProvince, query.getProvince().trim());
        }
        if (StringUtils.hasText(query.getSubjectTrack())) {
            wrapper.eq(AdmissionRecord::getSubjectTrack, query.getSubjectTrack().trim());
        }
        wrapper.orderByDesc(AdmissionRecord::getYear)
                .orderByDesc(AdmissionRecord::getId);

        Page<AdmissionRecord> page = admissionRecordMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                wrapper
        );
        Set<Long> majorIds = page.getRecords().stream()
                .map(AdmissionRecord::getMajorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> majorNames = new HashMap<>();
        if (!majorIds.isEmpty()) {
            for (Major major : majorMapper.selectBatchIds(majorIds)) {
                majorNames.put(major.getId(), major.getName());
            }
        }
        List<AdminAdmissionRecordVO> records = page.getRecords().stream()
                .map(record -> AdminAdmissionRecordVO.from(
                        record,
                        majorNames.getOrDefault(record.getMajorId(), "院校专业组")
                ))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public AdminAdmissionRecordVO createAdmissionRecord(
            Long universityId,
            AdmissionRecordSaveRequest request
    ) {
        requireUniversity(universityId);
        Major major = requireAdmissionMajor(universityId, request.getMajorId());
        validateAdmissionRequest(universityId, null, request);
        AdmissionRecord record = new AdmissionRecord();
        record.setUniversityId(universityId);
        copyAdmissionRequest(request, record);
        record.setCreatedAt(LocalDateTime.now());
        admissionRecordMapper.insert(record);
        return AdminAdmissionRecordVO.from(record, major.getName());
    }

    @Override
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public AdminAdmissionRecordVO updateAdmissionRecord(
            Long universityId,
            Long recordId,
            AdmissionRecordSaveRequest request
    ) {
        requireUniversity(universityId);
        AdmissionRecord record = requireAdmissionRecord(universityId, recordId);
        Major major = requireAdmissionMajor(universityId, request.getMajorId());
        validateAdmissionRequest(universityId, recordId, request);
        copyAdmissionRequest(request, record);
        admissionRecordMapper.updateById(record);
        return AdminAdmissionRecordVO.from(record, major.getName());
    }

    @Override
    @Transactional
    @CacheEvict(value = "recommendations", allEntries = true)
    public void deleteAdmissionRecord(Long universityId, Long recordId) {
        requireUniversity(universityId);
        requireAdmissionRecord(universityId, recordId);
        admissionRecordMapper.deleteById(recordId);
    }

    private University requireUniversity(Long universityId) {
        University university = universityMapper.selectById(universityId);
        if (university == null) {
            throw BusinessException.notFound("院校不存在");
        }
        return university;
    }

    private Major requireMajor(Long universityId, Long majorId) {
        Major major = majorMapper.selectById(majorId);
        if (major == null) {
            throw BusinessException.notFound("专业不存在");
        }
        if (!universityId.equals(major.getUniversityId())) {
            throw BusinessException.badRequest("专业不属于当前院校");
        }
        return major;
    }

    private Major requireAdmissionMajor(Long universityId, Long majorId) {
        if (majorId == null) {
            throw BusinessException.badRequest("请选择具体专业");
        }
        return requireMajor(universityId, majorId);
    }

    private AdmissionRecord requireAdmissionRecord(Long universityId, Long recordId) {
        AdmissionRecord record = admissionRecordMapper.selectById(recordId);
        if (record == null) {
            throw BusinessException.notFound("录取记录不存在");
        }
        if (!universityId.equals(record.getUniversityId())) {
            throw BusinessException.badRequest("录取记录不属于当前院校");
        }
        return record;
    }

    private void validateAdmissionRequest(
            Long universityId,
            Long currentRecordId,
            AdmissionRecordSaveRequest request
    ) {
        int currentYear = Year.now().getValue();
        if (request.getYear() == null || request.getYear() < 2000 || request.getYear() > currentYear) {
            throw BusinessException.badRequest("录取年份不能晚于当前年份");
        }
        LambdaQueryWrapper<AdmissionRecord> duplicateQuery =
                new LambdaQueryWrapper<AdmissionRecord>()
                        .eq(AdmissionRecord::getUniversityId, universityId)
                        .eq(AdmissionRecord::getMajorId, request.getMajorId())
                        .eq(AdmissionRecord::getProvince, request.getProvince().trim())
                        .eq(AdmissionRecord::getYear, request.getYear())
                        .eq(AdmissionRecord::getSubjectTrack, request.getSubjectTrack().trim())
                        .eq(AdmissionRecord::getBatch, request.getBatch().trim());
        if (currentRecordId != null) {
            duplicateQuery.ne(AdmissionRecord::getId, currentRecordId);
        }
        if (admissionRecordMapper.selectCount(duplicateQuery) > 0) {
            throw BusinessException.badRequest(
                    "该专业在相同年份、省份、科类和批次已有录取记录"
            );
        }
    }

    private void copyMajorRequest(MajorSaveRequest request, Major major) {
        major.setName(request.getName().trim());
        major.setCategory(trimToNull(request.getCategory()));
        major.setSubjectRequirement(trimToNull(request.getSubjectRequirement()));
        major.setDescription(trimToNull(request.getDescription()));
    }

    private void copyAdmissionRequest(
            AdmissionRecordSaveRequest request,
            AdmissionRecord record
    ) {
        record.setMajorId(request.getMajorId());
        record.setProvince(request.getProvince().trim());
        record.setYear(request.getYear());
        record.setSubjectTrack(request.getSubjectTrack().trim());
        record.setBatch(request.getBatch().trim());
        record.setMinScore(request.getMinScore());
        record.setMinRank(request.getMinRank());
        record.setPlanCount(request.getPlanCount());
        record.setSource(request.getSource().trim());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
