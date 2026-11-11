package com.example.volunteer.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.example.volunteer.service.impl.AdminStatisticsServiceImpl;
import com.example.volunteer.vo.AdminStatisticsOverviewVO;
import com.example.volunteer.vo.StudentVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @Mock
    private UniversityMapper universityMapper;

    @Mock
    private MajorMapper majorMapper;

    @Mock
    private AdmissionRecordMapper admissionRecordMapper;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminStatisticsServiceImpl service;

    @Test
    void shouldAggregateStatisticsOverview() {
        when(universityMapper.selectList(any())).thenReturn(List.of(
                university(1L, "华北示例大学", "山西", "本科"),
                university(2L, "江南示例大学", "江苏", "本科"),
                university(3L, "中原示例大学", "河南", "本科")
        ));
        when(majorMapper.selectList(any())).thenReturn(List.of(
                major(1L, 1L, "软件工程"),
                major(2L, 2L, "计算机科学与技术")
        ));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of(
                record(1L, 1L, 2024, 560, 12000),
                record(1L, 1L, 2025, 570, 11000),
                record(1L, 1L, 2025, 580, 9000)
        ));
        when(favoriteMapper.selectList(any())).thenReturn(List.of(
                favorite(1L, 1L),
                favorite(1L, 1L),
                favorite(2L, 2L),
                favorite(1L, 999L)
        ));
        when(userMapper.selectCount(any())).thenReturn(1L);

        AdminStatisticsOverviewVO result = service.getOverview();

        assertEquals(3, result.universityCount());
        assertEquals(2, result.majorCount());
        assertEquals(3, result.admissionRecordCount());
        assertEquals(1, result.studentCount());
        assertEquals(4, result.favoriteCount());
        assertEquals("本科", result.universityLevels().get(0).label());
        assertEquals(3, result.universityLevels().get(0).count());
        assertEquals(3, result.universityProvinces().size());
        assertEquals(2024, result.admissionTrends().get(0).year());
        assertEquals(560, result.admissionTrends().get(0).averageMinScore());
        assertEquals(575, result.admissionTrends().get(1).averageMinScore());
        assertEquals(10000, result.admissionTrends().get(1).averageMinRank());
        assertEquals("华北示例大学", result.topFavorites().get(0).universityName());
        assertEquals(2, result.topFavorites().get(0).favoriteCount());
        assertEquals(1, result.dataCompleteness().universitiesWithoutMajors());
        assertEquals(2, result.dataCompleteness().universitiesWithoutAdmissions());
        assertEquals(1, result.dataCompleteness().majorsWithoutAdmissions());
    }

    @Test
    void shouldReturnEmptyStatisticsWhenDatabaseHasNoData() {
        when(universityMapper.selectList(any())).thenReturn(List.of());
        when(majorMapper.selectList(any())).thenReturn(List.of());
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());
        when(favoriteMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectCount(any())).thenReturn(0L);

        AdminStatisticsOverviewVO result = service.getOverview();

        assertEquals(0, result.universityCount());
        assertEquals(0, result.studentCount());
        assertTrue(result.universityLevels().isEmpty());
        assertTrue(result.admissionTrends().isEmpty());
        assertTrue(result.topFavorites().isEmpty());
    }

    @Test
    void shouldReturnNullAveragesWhenScoreAndRankAreMissing() {
        when(universityMapper.selectList(any())).thenReturn(List.of(
                university(1L, "华北示例大学", "山西", "本科")
        ));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, 1L, "软件工程")));
        AdmissionRecord record = record(1L, 1L, 2025, null, null);
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(favoriteMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectCount(any())).thenReturn(0L);

        AdminStatisticsOverviewVO result = service.getOverview();

        assertEquals(1, result.admissionTrends().get(0).recordCount());
        assertNull(result.admissionTrends().get(0).averageMinScore());
        assertNull(result.admissionTrends().get(0).averageMinRank());
    }

    @Test
    void shouldQueryOnlyStudentsAndExcludePasswordHash() {
        User student = student();
        Page<User> userPage = new Page<>(1, 10);
        userPage.setRecords(List.of(student));
        userPage.setTotal(1);
        when(userMapper.selectPage(any(), any())).thenReturn(userPage);
        AdminStudentQuery query = new AdminStudentQuery();
        query.setKeyword(" student ");

        PageResult<StudentVO> result = service.pageStudents(query);

        assertEquals(1, result.total());
        assertEquals("student", result.records().get(0).username());
        assertTrue(Arrays.stream(StudentVO.class.getRecordComponents())
                .noneMatch(component -> component.getName().toLowerCase().contains("password")));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<User>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("role"));
        assertTrue(sql.contains("username"));
        assertTrue(sql.contains("nickname"));
    }

    private University university(Long id, String name, String province, String level) {
        University university = new University();
        university.setId(id);
        university.setName(name);
        university.setProvince(province);
        university.setLevel(level);
        return university;
    }

    private Major major(Long id, Long universityId, String name) {
        Major major = new Major();
        major.setId(id);
        major.setUniversityId(universityId);
        major.setName(name);
        return major;
    }

    private AdmissionRecord record(
            Long universityId,
            Long majorId,
            int year,
            Integer minScore,
            Integer minRank
    ) {
        AdmissionRecord record = new AdmissionRecord();
        record.setUniversityId(universityId);
        record.setMajorId(majorId);
        record.setYear(year);
        record.setMinScore(minScore);
        record.setMinRank(minRank);
        return record;
    }

    private Favorite favorite(Long userId, Long universityId) {
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setUniversityId(universityId);
        return favorite;
    }

    private User student() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student");
        user.setPasswordHash("secret-hash");
        user.setNickname("示例学生");
        user.setRole("STUDENT");
        user.setProvince("山西");
        user.setExamYear(2026);
        user.setSubjectTrack("物理类");
        user.setScore(560);
        user.setProvinceRank(10000);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return user;
    }
}
