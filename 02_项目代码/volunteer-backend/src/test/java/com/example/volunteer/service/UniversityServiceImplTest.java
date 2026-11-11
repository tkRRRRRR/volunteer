package com.example.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.volunteer.common.BusinessException;
import com.example.volunteer.entity.AdmissionRecord;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.mapper.AdmissionRecordMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.service.impl.UniversityServiceImpl;
import com.example.volunteer.vo.UniversityDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                AdmissionRecord.class
        );
    }

    @Mock
    private UniversityMapper universityMapper;

    @Mock
    private MajorMapper majorMapper;

    @Mock
    private AdmissionRecordMapper admissionRecordMapper;

    @InjectMocks
    private UniversityServiceImpl service;

    @Test
    void shouldAggregateUniversityMajorsAndAdmissionPoints() {
        University university = university();
        when(universityMapper.selectById(1L)).thenReturn(university);
        when(majorMapper.selectList(any())).thenReturn(List.of(
                major(1L, "软件工程"),
                major(2L, "数据科学与大数据技术")
        ));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of(
                record(1L, 2023, 565, 10800),
                record(1L, 2024, 572, 10100),
                record(1L, 2025, 578, 9600)
        ));

        UniversityDetailVO result = service.getOverview(1L, "山西", "物理类");

        assertEquals("华北示例大学", result.university().name());
        assertEquals(2, result.majors().size());
        assertEquals(3, result.admissionPoints().size());
        assertEquals("软件工程", result.admissionPoints().get(0).majorName());
        assertEquals("演示数据（非官方）", result.admissionPoints().get(0).source());
    }

    @Test
    void shouldReturnEmptyCollectionsWhenUniversityHasNoRelatedData() {
        when(universityMapper.selectById(1L)).thenReturn(university());
        when(majorMapper.selectList(any())).thenReturn(List.of());
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());

        UniversityDetailVO result = service.getOverview(1L, null, null);

        assertTrue(result.majors().isEmpty());
        assertTrue(result.admissionPoints().isEmpty());
    }

    @Test
    void shouldRejectMissingUniversity() {
        when(universityMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getOverview(999L, null, null));
    }

    @Test
    void shouldIncludeOptionalProvinceAndSubjectTrackFilters() {
        when(universityMapper.selectById(1L)).thenReturn(university());
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of(record(1L, 2025, 578, 9600)));

        service.getOverview(1L, "山西", "物理类");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<AdmissionRecord>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(admissionRecordMapper).selectList(queryCaptor.capture());
        assertTrue(queryCaptor.getValue().getSqlSegment().contains("province"));
        assertTrue(queryCaptor.getValue().getSqlSegment().contains("subject_track"));
    }

    private University university() {
        University university = new University();
        university.setId(1L);
        university.setName("华北示例大学");
        university.setProvince("山西");
        university.setCity("太原");
        university.setLevel("本科");
        university.setType("综合");
        university.setDescription("用于系统测试的虚构院校");
        return university;
    }

    private Major major(Long id, String name) {
        Major major = new Major();
        major.setId(id);
        major.setUniversityId(1L);
        major.setName(name);
        major.setCategory("工学");
        major.setSubjectRequirement("物理+不限");
        major.setDescription("演示专业");
        return major;
    }

    private AdmissionRecord record(Long majorId, int year, int minScore, int minRank) {
        AdmissionRecord record = new AdmissionRecord();
        record.setUniversityId(1L);
        record.setMajorId(majorId);
        record.setProvince("山西");
        record.setYear(year);
        record.setSubjectTrack("物理类");
        record.setBatch("本科批");
        record.setMinScore(minScore);
        record.setMinRank(minRank);
        record.setSource("演示数据（非官方）");
        return record;
    }
}
