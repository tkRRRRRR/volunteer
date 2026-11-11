package com.example.volunteer.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.example.volunteer.service.impl.AdminCatalogServiceImpl;
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
}
