package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.volunteer.common.BusinessException;
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
import com.example.volunteer.vo.MajorVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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

    private void copyMajorRequest(MajorSaveRequest request, Major major) {
        major.setName(request.getName().trim());
        major.setCategory(trimToNull(request.getCategory()));
        major.setSubjectRequirement(trimToNull(request.getSubjectRequirement()));
        major.setDescription(trimToNull(request.getDescription()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
