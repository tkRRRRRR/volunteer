package com.example.volunteer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.volunteer.common.PageResult;
import com.example.volunteer.dto.AdminStudentQuery;
import com.example.volunteer.entity.AdmissionRecord;
import com.example.volunteer.entity.Favorite;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.entity.User;
import com.example.volunteer.mapper.AdmissionRecordMapper;
import com.example.volunteer.mapper.FavoriteMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.mapper.UserMapper;
import com.example.volunteer.service.AdminStatisticsService;
import com.example.volunteer.vo.AdminStatisticsOverviewVO;
import com.example.volunteer.vo.AdmissionYearTrendVO;
import com.example.volunteer.vo.DataCompletenessVO;
import com.example.volunteer.vo.FavoriteUniversityStatVO;
import com.example.volunteer.vo.StatisticItemVO;
import com.example.volunteer.vo.StudentVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 读取小规模基础数据后在内存中完成统计，避免逐院校查询形成 N+1 请求。
 */
@Service
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;
    private final AdmissionRecordMapper admissionRecordMapper;
    private final FavoriteMapper favoriteMapper;
    private final UserMapper userMapper;

    public AdminStatisticsServiceImpl(
            UniversityMapper universityMapper,
            MajorMapper majorMapper,
            AdmissionRecordMapper admissionRecordMapper,
            FavoriteMapper favoriteMapper,
            UserMapper userMapper
    ) {
        this.universityMapper = universityMapper;
        this.majorMapper = majorMapper;
        this.admissionRecordMapper = admissionRecordMapper;
        this.favoriteMapper = favoriteMapper;
        this.userMapper = userMapper;
    }

    @Override
    public AdminStatisticsOverviewVO getOverview() {
        List<University> universities = universityMapper.selectList(
                new LambdaQueryWrapper<>()
        );
        List<Major> majors = majorMapper.selectList(new LambdaQueryWrapper<>());
        List<AdmissionRecord> records = admissionRecordMapper.selectList(
                new LambdaQueryWrapper<>()
        );
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<>());
        long studentCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT")
        );

        Map<Long, University> universityById = universities.stream()
                .collect(Collectors.toMap(University::getId, Function.identity()));
        Set<Long> universitiesWithMajors = majors.stream()
                .map(Major::getUniversityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> universitiesWithAdmissions = records.stream()
                .map(AdmissionRecord::getUniversityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> majorsWithAdmissions = records.stream()
                .map(AdmissionRecord::getMajorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        DataCompletenessVO completeness = new DataCompletenessVO(
                universities.stream().filter(item -> !universitiesWithMajors.contains(item.getId())).count(),
                universities.stream().filter(item -> !universitiesWithAdmissions.contains(item.getId())).count(),
                majors.stream().filter(item -> !majorsWithAdmissions.contains(item.getId())).count()
        );

        return new AdminStatisticsOverviewVO(
                universities.size(),
                majors.size(),
                records.size(),
                studentCount,
                favorites.size(),
                groupUniversities(universities, University::getLevel, "未分类"),
                groupUniversities(universities, University::getProvince, "未填写"),
                buildAdmissionTrends(records),
                buildFavoriteTop(favorites, universityById),
                completeness
        );
    }

    @Override
    public PageResult<StudentVO> pageStudents(AdminStudentQuery query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "STUDENT");
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt)
                .orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                wrapper
        );
        return new PageResult<>(
                page.getRecords().stream().map(StudentVO::from).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private List<StatisticItemVO> groupUniversities(
            List<University> universities,
            Function<University, String> classifier,
            String emptyLabel
    ) {
        return universities.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            String value = classifier.apply(item);
                            return StringUtils.hasText(value) ? value.trim() : emptyLabel;
                        },
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new StatisticItemVO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(StatisticItemVO::count)
                        .reversed()
                        .thenComparing(StatisticItemVO::label))
                .toList();
    }

    private List<AdmissionYearTrendVO> buildAdmissionTrends(List<AdmissionRecord> records) {
        Map<Integer, List<AdmissionRecord>> byYear = new TreeMap<>();
        for (AdmissionRecord record : records) {
            if (record.getYear() != null) {
                byYear.computeIfAbsent(record.getYear(), key -> new ArrayList<>()).add(record);
            }
        }
        List<AdmissionYearTrendVO> trends = new ArrayList<>();
        byYear.forEach((year, yearRecords) -> {
            List<Integer> scores = yearRecords.stream()
                    .map(AdmissionRecord::getMinScore)
                    .filter(Objects::nonNull)
                    .toList();
            List<Integer> ranks = yearRecords.stream()
                    .map(AdmissionRecord::getMinRank)
                    .filter(Objects::nonNull)
                    .toList();
            trends.add(new AdmissionYearTrendVO(
                    year,
                    yearRecords.size(),
                    average(scores),
                    average(ranks)
            ));
        });
        return trends;
    }

    private Integer average(List<Integer> values) {
        return values.isEmpty()
                ? null
                : (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private List<FavoriteUniversityStatVO> buildFavoriteTop(
            List<Favorite> favorites,
            Map<Long, University> universityById
    ) {
        return favorites.stream()
                .map(Favorite::getUniversityId)
                .filter(universityById::containsKey)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new FavoriteUniversityStatVO(
                        entry.getKey(),
                        universityById.get(entry.getKey()).getName(),
                        entry.getValue()
                ))
                .sorted(Comparator.comparingLong(FavoriteUniversityStatVO::favoriteCount)
                        .reversed()
                        .thenComparing(FavoriteUniversityStatVO::universityName))
                .limit(5)
                .toList();
    }
}
