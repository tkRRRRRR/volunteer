package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.volunteer.dto.RecommendationRequest;
import com.example.volunteer.entity.AdmissionRecord;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.mapper.AdmissionRecordMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.service.RecommendationScorer;
import com.example.volunteer.service.RecommendationService;
import com.example.volunteer.vo.RecommendationVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 读取近三年录取记录，按院校专业聚合后计算匹配分并组织推荐结果。
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final int MAX_RESULTS = 50;

    private final AdmissionRecordMapper admissionRecordMapper;
    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;

    public RecommendationServiceImpl(
            AdmissionRecordMapper admissionRecordMapper,
            UniversityMapper universityMapper,
            MajorMapper majorMapper
    ) {
        this.admissionRecordMapper = admissionRecordMapper;
        this.universityMapper = universityMapper;
        this.majorMapper = majorMapper;
    }

    @Override
    @Cacheable(value = "recommendations", key = "#request.cacheKey()")
    public List<RecommendationVO> recommend(RecommendationRequest request) {
        // 统一使用考试年份前三年的数据，避免把不同科类或省份的记录混在一起。
        List<AdmissionRecord> records = admissionRecordMapper.selectList(new LambdaQueryWrapper<AdmissionRecord>()
                .eq(AdmissionRecord::getProvince, request.getProvince())
                .eq(AdmissionRecord::getSubjectTrack, request.getSubjectTrack())
                .between(
                        AdmissionRecord::getYear,
                        request.getExamYear() - 3,
                        request.getExamYear() - 1
                ));
        if (records.isEmpty()) {
            return List.of();
        }

        // 同一院校专业可能有多年的录取记录，先聚合再参与匹配计算。
        Map<GroupKey, List<AdmissionRecord>> grouped = records.stream()
                .filter(record -> record.getMinScore() != null && record.getMinRank() != null)
                .collect(Collectors.groupingBy(record -> new GroupKey(record.getUniversityId(), record.getMajorId())));

        Set<Long> universityIds = grouped.keySet().stream()
                .map(GroupKey::universityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> majorIds = grouped.keySet().stream()
                .map(GroupKey::majorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, University> universities = universityIds.isEmpty()
                ? Map.of()
                : universityMapper.selectBatchIds(universityIds).stream()
                .collect(Collectors.toMap(University::getId, Function.identity()));
        Map<Long, Major> majors = majorIds.isEmpty()
                ? Map.of()
                : majorMapper.selectBatchIds(majorIds).stream()
                .collect(Collectors.toMap(Major::getId, Function.identity()));

        List<RecommendationVO> results = new ArrayList<>();
        for (Map.Entry<GroupKey, List<AdmissionRecord>> entry : grouped.entrySet()) {
            GroupKey key = entry.getKey();
            University university = universities.get(key.universityId());
            if (university == null || !matchesFilters(university, majors.get(key.majorId()), request)) {
                continue;
            }

            List<AdmissionRecord> groupRecords = entry.getValue();
            // 平均分和平均位次用于平滑单年异常波动。
            int averageMinScore = (int) Math.round(groupRecords.stream()
                    .mapToInt(AdmissionRecord::getMinScore)
                    .average()
                    .orElse(0));
            int averageMinRank = (int) Math.round(groupRecords.stream()
                    .mapToInt(AdmissionRecord::getMinRank)
                    .average()
                    .orElse(0));
            int latestYear = groupRecords.stream()
                    .map(AdmissionRecord::getYear)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            String source = groupRecords.stream()
                    .map(AdmissionRecord::getSource)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.joining("、"));

            RecommendationScorer.Result score = RecommendationScorer.score(
                    request.getScore(),
                    request.getProvinceRank(),
                    averageMinScore,
                    averageMinRank
            );
            Major major = majors.get(key.majorId());
            results.add(new RecommendationVO(
                    university.getId(),
                    university.getName(),
                    university.getProvince(),
                    university.getCity(),
                    university.getLevel(),
                    major == null ? null : major.getId(),
                    major == null ? "院校专业组" : major.getName(),
                    major == null ? null : major.getSubjectRequirement(),
                    latestYear,
                    averageMinScore,
                    averageMinRank,
                    score.matchScore(),
                    score.matchLevel(),
                    score.reason(),
                    source
            ));
        }

        // 匹配分高的结果优先展示，并限制数量以控制页面信息量。
        results.sort(Comparator.comparing(RecommendationVO::matchScore).reversed());
        return results.stream().limit(MAX_RESULTS).toList();
    }

    private boolean matchesFilters(University university, Major major, RecommendationRequest request) {
        // 空筛选项表示不限，只有用户明确选择时才排除不匹配的记录。
        if (StringUtils.hasText(request.getPreferredProvince())
                && !request.getPreferredProvince().equals(university.getProvince())) {
            return false;
        }
        if (StringUtils.hasText(request.getUniversityLevel())
                && !request.getUniversityLevel().equals(university.getLevel())) {
            return false;
        }
        if (!StringUtils.hasText(request.getKeyword())) {
            return true;
        }
        String keyword = request.getKeyword().trim();
        String majorName = major == null ? "" : major.getName();
        String tags = university.getTags() == null ? "" : university.getTags();
        return university.getName().contains(keyword)
                || tags.contains(keyword)
                || majorName.contains(keyword);
    }

    private record GroupKey(Long universityId, Long majorId) {
    }
}
