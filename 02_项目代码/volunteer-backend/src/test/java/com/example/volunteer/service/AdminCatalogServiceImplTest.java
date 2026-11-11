package com.example.volunteer.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.example.volunteer.service.impl.AdminCatalogServiceImpl;
import com.example.volunteer.vo.AdminAdmissionRecordVO;
import com.example.volunteer.vo.MajorVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Major.class);
        TableInfoHelper.initTableInfo(assistant, AdmissionRecord.class);
        TableInfoHelper.initTableInfo(assistant, Favorite.class);
    }

    @Mock
    private UniversityMapper universityMapper;

    @Mock
    private MajorMapper majorMapper;

    @Mock
    private AdmissionRecordMapper admissionRecordMapper;

    @Mock
    private FavoriteMapper favoriteMapper;

    @InjectMocks
    private AdminCatalogServiceImpl service;

    @Test
    void shouldCreateMajorUnderExistingUniversity() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));

        MajorVO result = service.createMajor(1L, majorRequest(" 软件工程 "));

        assertEquals("软件工程", result.name());
        ArgumentCaptor<Major> captor = ArgumentCaptor.forClass(Major.class);
        verify(majorMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUniversityId());
        assertEquals("软件工程", captor.getValue().getName());
        assertEquals("工学", captor.getValue().getCategory());
    }

    @Test
    void shouldRejectCreatingMajorWhenUniversityDoesNotExist() {
        when(universityMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createMajor(999L, majorRequest("软件工程"))
        );

        assertEquals("院校不存在", exception.getMessage());
    }

    @Test
    void shouldRejectUpdatingMajorFromAnotherUniversity() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(9L)).thenReturn(major(9L, 2L, "人工智能"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateMajor(1L, 9L, majorRequest("人工智能"))
        );

        assertEquals("专业不属于当前院校", exception.getMessage());
    }

    @Test
    void shouldUpdateMajorOwnedByCurrentUniversity() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));

        MajorVO result = service.updateMajor(1L, 1L, majorRequest("软件工程（卓越班）"));

        assertEquals("软件工程（卓越班）", result.name());
        verify(majorMapper).updateById(any(Major.class));
    }

    @Test
    void shouldRejectDeletingMajorReferencedByAdmissionRecord() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
        when(admissionRecordMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteMajor(1L, 1L)
        );

        assertEquals("该专业已有录取记录，不能删除", exception.getMessage());
    }

    @Test
    void shouldRejectDeletingMajorReferencedByFavorite() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
        when(admissionRecordMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteMajor(1L, 1L)
        );

        assertEquals("该专业已被收藏，不能删除", exception.getMessage());
    }

    @Test
    void shouldDeleteUnreferencedMajor() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
        when(admissionRecordMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.selectCount(any())).thenReturn(0L);

        service.deleteMajor(1L, 1L);

        verify(majorMapper).deleteById(1L);
    }

    @Test
    void shouldFilterMajorsByNameOrCategory() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, 1L, "软件工程")));

        List<MajorVO> result = service.listMajors(1L, " 工学 ");

        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Major>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(majorMapper).selectList(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("name"));
        assertTrue(captor.getValue().getSqlSegment().contains("category"));
    }

    @Test
    void shouldCreateAdmissionRecordForMajorInSameUniversity() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
        when(admissionRecordMapper.selectCount(any())).thenReturn(0L);

        AdminAdmissionRecordVO result = service.createAdmissionRecord(
                1L,
                admissionRequest(1L, 2025)
        );

        assertEquals(1L, result.universityId());
        assertEquals(1L, result.majorId());
        assertEquals("软件工程", result.majorName());
        assertEquals("山西", result.province());
        ArgumentCaptor<AdmissionRecord> captor = ArgumentCaptor.forClass(AdmissionRecord.class);
        verify(admissionRecordMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUniversityId());
        assertEquals("本科批", captor.getValue().getBatch());
        assertEquals("演示数据（非官方）", captor.getValue().getSource());
    }

    @Test
    void shouldRejectAdmissionRecordWithoutMajor() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAdmissionRecord(1L, admissionRequest(null, 2025))
        );

        assertEquals("请选择具体专业", exception.getMessage());
    }

    @Test
    void shouldRejectAdmissionMajorFromAnotherUniversity() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(3L)).thenReturn(major(3L, 2L, "计算机科学与技术"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAdmissionRecord(1L, admissionRequest(3L, 2025))
        );

        assertEquals("专业不属于当前院校", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateAdmissionRecordOnCreate() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
        when(admissionRecordMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAdmissionRecord(1L, admissionRequest(1L, 2025))
        );

        assertEquals("该专业在相同年份、省份、科类和批次已有录取记录", exception.getMessage());
    }

    @Test
    void shouldAllowUpdatingRecordWhenDuplicateCheckExcludesItself() {
        AdmissionRecord existing = admissionRecord(20L, 1L, 1L, 2025);
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(admissionRecordMapper.selectById(20L)).thenReturn(existing);
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
        when(admissionRecordMapper.selectCount(any())).thenReturn(0L);

        AdminAdmissionRecordVO result = service.updateAdmissionRecord(
                1L,
                20L,
                admissionRequest(1L, 2025)
        );

        assertEquals(20L, result.id());
        assertEquals(578, result.minScore());
        verify(admissionRecordMapper).updateById(any(AdmissionRecord.class));
    }

    @Test
    void shouldPageAdmissionRecordsUsingCurrentUniversityAndFilters() {
        AdminAdmissionQuery query = new AdminAdmissionQuery();
        query.setPage(1);
        query.setSize(10);
        query.setMajorId(1L);
        query.setYear(2025);
        query.setProvince(" 山西 ");
        query.setSubjectTrack(" 物理类 ");
        Page<AdmissionRecord> resultPage = new Page<>(1, 10);
        resultPage.setRecords(List.of(admissionRecord(20L, 1L, 1L, 2025)));
        resultPage.setTotal(1);
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
        when(majorMapper.selectBatchIds(any())).thenReturn(List.of(major(1L, 1L, "软件工程")));
        when(admissionRecordMapper.selectPage(any(), any())).thenReturn(resultPage);

        PageResult<AdminAdmissionRecordVO> result =
                service.pageAdmissionRecords(1L, query);

        assertEquals(1, result.total());
        assertEquals("软件工程", result.records().get(0).majorName());
        assertEquals("山西", result.records().get(0).province());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<AdmissionRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(admissionRecordMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("university_id"));
        assertTrue(captor.getValue().getSqlSegment().contains("major_id"));
        assertTrue(captor.getValue().getSqlSegment().contains("province"));
        assertTrue(captor.getValue().getSqlSegment().contains("subject_track"));
    }

    @Test
    void shouldRejectAdmissionYearInTheFuture() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAdmissionRecord(
                        1L,
                        admissionRequest(1L, Year.now().getValue() + 1)
                )
        );

        assertEquals("录取年份不能晚于当前年份", exception.getMessage());
    }

    @Test
    void shouldDeleteAdmissionRecordOwnedByCurrentUniversity() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(admissionRecordMapper.selectById(20L))
                .thenReturn(admissionRecord(20L, 1L, 1L, 2025));

        service.deleteAdmissionRecord(1L, 20L);

        verify(admissionRecordMapper).deleteById(20L);
    }

    private University university(Long id) {
        University university = new University();
        university.setId(id);
        university.setName("华北示例大学");
        university.setProvince("山西");
        university.setCity("太原");
        university.setLevel("本科");
        return university;
    }

    private Major major(Long id, Long universityId, String name) {
        Major major = new Major();
        major.setId(id);
        major.setUniversityId(universityId);
        major.setName(name);
        major.setCategory("工学");
        major.setSubjectRequirement("物理+不限");
        major.setDescription("演示专业");
        return major;
    }

    private MajorSaveRequest majorRequest(String name) {
        MajorSaveRequest request = new MajorSaveRequest();
        request.setName(name);
        request.setCategory(" 工学 ");
        request.setSubjectRequirement(" 物理+不限 ");
        request.setDescription(" 演示专业 ");
        return request;
    }

    private AdmissionRecordSaveRequest admissionRequest(Long majorId, int year) {
        AdmissionRecordSaveRequest request = new AdmissionRecordSaveRequest();
        request.setMajorId(majorId);
        request.setProvince(" 山西 ");
        request.setYear(year);
        request.setSubjectTrack(" 物理类 ");
        request.setBatch(" 本科批 ");
        request.setMinScore(578);
        request.setMinRank(9600);
        request.setPlanCount(38);
        request.setSource(" 演示数据（非官方） ");
        return request;
    }

    private AdmissionRecord admissionRecord(Long id, Long universityId, Long majorId, int year) {
        AdmissionRecord record = new AdmissionRecord();
        record.setId(id);
        record.setUniversityId(universityId);
        record.setMajorId(majorId);
        record.setProvince("山西");
        record.setYear(year);
        record.setSubjectTrack("物理类");
        record.setBatch("本科批");
        record.setMinScore(578);
        record.setMinRank(9600);
        record.setPlanCount(38);
        record.setSource("演示数据（非官方）");
        return record;
    }
}
