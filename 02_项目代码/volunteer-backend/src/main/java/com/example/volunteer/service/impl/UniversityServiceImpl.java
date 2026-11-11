package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.volunteer.common.BusinessException;
import com.example.volunteer.common.PageResult;
import com.example.volunteer.dto.UniversityQuery;
import com.example.volunteer.dto.UniversitySaveRequest;
import com.example.volunteer.entity.AdmissionRecord;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.mapper.AdmissionRecordMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.service.UniversityService;
import com.example.volunteer.vo.AdmissionPointVO;
import com.example.volunteer.vo.MajorVO;
import com.example.volunteer.vo.UniversityDetailVO;
import com.example.volunteer.vo.UniversityVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 提供院校公开查询以及管理员端的院校基础信息维护。
 */
@Service
public class UniversityServiceImpl implements UniversityService {

    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;
    private final AdmissionRecordMapper admissionRecordMapper;

    public UniversityServiceImpl(
            UniversityMapper universityMapper,
            MajorMapper majorMapper,
            AdmissionRecordMapper admissionRecordMapper
    ) {
        this.universityMapper = universityMapper;
        this.majorMapper = majorMapper;
        this.admissionRecordMapper = admissionRecordMapper;
    }

    @Override
    public PageResult<UniversityVO> search(UniversityQuery query) {
        LambdaQueryWrapper<University> wrapper = new LambdaQueryWrapper<>();
        // 关键词同时匹配名称、简介和标签，便于学生用专业方向或院校特点检索。
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(University::getName, keyword)
                    .or()
                    .like(University::getDescription, keyword)
                    .or()
                    .like(University::getTags, keyword));
        }
        if (StringUtils.hasText(query.getProvince())) {
            wrapper.eq(University::getProvince, query.getProvince().trim());
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(University::getLevel, query.getLevel().trim());
        }
        wrapper.orderByDesc(University::getId);

        Page<University> page = universityMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        // 数据库实体转换为页面视图，避免直接向前端暴露持久层对象。
        List<UniversityVO> records = page.getRecords().stream().map(UniversityVO::from).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UniversityVO getById(Long id) {
        return UniversityVO.from(requireUniversity(id));
    }

    @Override
    public UniversityDetailVO getOverview(Long id, String province, String subjectTrack) {
        University university = requireUniversity(id);
        List<Major> majors = majorMapper.selectList(new LambdaQueryWrapper<Major>()
                .eq(Major::getUniversityId, id)
                .orderByAsc(Major::getId));

        LambdaQueryWrapper<AdmissionRecord> recordQuery = new LambdaQueryWrapper<AdmissionRecord>()
                .eq(AdmissionRecord::getUniversityId, id);
        // 详情页默认使用学生所在省份和科类，未填写时保留全部演示记录。
        if (StringUtils.hasText(province)) {
            recordQuery.eq(AdmissionRecord::getProvince, province.trim());
        }
        if (StringUtils.hasText(subjectTrack)) {
            recordQuery.eq(AdmissionRecord::getSubjectTrack, subjectTrack.trim());
        }
        recordQuery.orderByAsc(AdmissionRecord::getYear)
                .orderByAsc(AdmissionRecord::getMajorId);

        List<AdmissionRecord> records = admissionRecordMapper.selectList(recordQuery);
        Map<Long, String> majorNames = new HashMap<>();
        for (Major major : majors) {
            majorNames.put(major.getId(), major.getName());
        }
        List<AdmissionPointVO> admissionPoints = records.stream()
                .map(record -> new AdmissionPointVO(
                        record.getMajorId(),
                        majorNames.getOrDefault(record.getMajorId(), "院校专业组"),
                        record.getYear(),
                        record.getMinScore(),
                        record.getMinRank(),
                        record.getSubjectTrack(),
                        record.getBatch(),
                        record.getSource()
                ))
                .toList();
        return new UniversityDetailVO(
                UniversityVO.from(university),
                majors.stream().map(MajorVO::from).toList(),
                admissionPoints
        );
    }

    @Override
    @Transactional
    public UniversityVO create(UniversitySaveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        University university = new University();
        copyRequest(request, university);
        university.setCreatedAt(now);
        university.setUpdatedAt(now);
        universityMapper.insert(university);
        return UniversityVO.from(university);
    }

    @Override
    @Transactional
    public UniversityVO update(Long id, UniversitySaveRequest request) {
        University university = requireUniversity(id);
        copyRequest(request, university);
        university.setUpdatedAt(LocalDateTime.now());
        universityMapper.updateById(university);
        return UniversityVO.from(university);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireUniversity(id);
        universityMapper.deleteById(id);
    }

    private University requireUniversity(Long id) {
        University university = universityMapper.selectById(id);
        if (university == null) {
            throw BusinessException.notFound("院校不存在");
        }
        return university;
    }

    private void copyRequest(UniversitySaveRequest request, University university) {
        university.setName(request.getName().trim());
        university.setProvince(request.getProvince().trim());
        university.setCity(request.getCity().trim());
        university.setLevel(request.getLevel().trim());
        university.setType(request.getType());
        university.setDescription(request.getDescription());
        university.setWebsite(request.getWebsite());
        university.setTags(request.getTags());
    }
}
