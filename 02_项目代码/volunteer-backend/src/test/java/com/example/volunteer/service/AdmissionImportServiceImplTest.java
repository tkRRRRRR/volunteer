package com.example.volunteer.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.volunteer.common.BusinessException;
import com.example.volunteer.entity.AdmissionRecord;
import com.example.volunteer.entity.Major;
import com.example.volunteer.entity.University;
import com.example.volunteer.mapper.AdmissionRecordMapper;
import com.example.volunteer.mapper.MajorMapper;
import com.example.volunteer.mapper.UniversityMapper;
import com.example.volunteer.service.impl.AdmissionImportServiceImpl;
import com.example.volunteer.service.importing.AdmissionFileParser;
import com.example.volunteer.service.importing.ParsedAdmissionRow;
import com.example.volunteer.vo.AdmissionImportResultVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Year;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionImportServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Major.class);
        TableInfoHelper.initTableInfo(assistant, AdmissionRecord.class);
    }

    @Mock
    private UniversityMapper universityMapper;

    @Mock
    private MajorMapper majorMapper;

    @Mock
    private AdmissionRecordMapper admissionRecordMapper;

    @Mock
    private AdmissionFileParser parser;

    @InjectMocks
    private AdmissionImportServiceImpl service;

    @Test
    void shouldImportValidRowsAndReturnFailedRowDetails() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());
        when(parser.parse(any())).thenReturn(List.of(
                parsed(2, "1", "软件工程", "2025", "578", "9600"),
                parsed(3, "999", "不存在专业", "2026", "590", "8000")
        ));

        AdmissionImportResultVO result = service.importRecords(1L, multipart("records.csv"));

        assertEquals(2, result.totalRows());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failedCount());
        assertEquals(3, result.errors().get(0).rowNumber());
        assertEquals("专业不存在或不属于当前院校", result.errors().get(0).message());
        verify(admissionRecordMapper).insert(any(AdmissionRecord.class));
    }

    @Test
    void shouldRejectDuplicateInsideFileAndExistingDatabaseRecord() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of(
                admissionRecord(1L, 1L, 2025)
        ));
        when(parser.parse(any())).thenReturn(List.of(
                parsed(2, "1", "软件工程", "2025", "578", "9600"),
                parsed(3, "1", "软件工程", "2025", "578", "9600"),
                parsed(4, "1", "软件工程", "2025", "578", "9600")
        ));

        AdmissionImportResultVO result = service.importRecords(1L, multipart("records.xlsx"));

        assertEquals(0, result.successCount());
        assertEquals(3, result.failedCount());
        assertTrue(result.errors().stream()
                .allMatch(error -> error.message().contains("数据库中已有录取记录")));
    }

    @Test
    void shouldMarkLaterRowsInSameFileAsDuplicate() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());
        when(parser.parse(any())).thenReturn(List.of(
                parsed(2, "1", "软件工程", "2025", "578", "9600"),
                parsed(3, "1", "软件工程", "2025", "579", "9500")
        ));

        AdmissionImportResultVO result = service.importRecords(1L, multipart("records.csv"));

        assertEquals(1, result.successCount());
        assertEquals(1, result.failedCount());
        assertTrue(result.errors().get(0).message().contains("文件内重复"));
    }

    @Test
    void shouldValidateMajorNameAndFieldRangesTogether() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());
        Map<String, String> values = rowValues(
                "1",
                "大数据",
                String.valueOf(Year.now().getValue() + 1),
                "1001",
                "0"
        );
        values.put("planCount", "-1");
        when(parser.parse(any())).thenReturn(List.of(new ParsedAdmissionRow(2, values)));

        AdmissionImportResultVO result = service.importRecords(1L, multipart("records.csv"));

        assertEquals(0, result.successCount());
        String message = result.errors().get(0).message();
        assertTrue(message.contains("专业名称与编号不匹配"));
        assertTrue(message.contains("录取年份不能晚于当前年份"));
        assertTrue(message.contains("最低分不能大于1000"));
        assertTrue(message.contains("最低位次必须大于0"));
        assertTrue(message.contains("计划数不能小于0"));
    }

    @Test
    void shouldRejectRowWithoutMajorName() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());
        Map<String, String> values = rowValues("1", "", "2025", "578", "9600");
        when(parser.parse(any())).thenReturn(List.of(new ParsedAdmissionRow(2, values)));

        AdmissionImportResultVO result = service.importRecords(1L, multipart("records.csv"));

        assertEquals(0, result.successCount());
        assertTrue(result.errors().get(0).message().contains("专业名称不能为空"));
    }

    @Test
    void shouldRejectTextFieldsLongerThanDatabaseColumns() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());
        Map<String, String> values = rowValues("1", "软件工程", "2025", "578", "9600");
        values.put("province", "省".repeat(31));
        values.put("subjectTrack", "科".repeat(31));
        values.put("batch", "批".repeat(31));
        values.put("source", "源".repeat(256));
        when(parser.parse(any())).thenReturn(List.of(new ParsedAdmissionRow(2, values)));

        AdmissionImportResultVO result = service.importRecords(1L, multipart("records.xlsx"));

        assertEquals(0, result.successCount());
        String message = result.errors().get(0).message();
        assertTrue(message.contains("招生省份不能超过30个字符"));
        assertTrue(message.contains("科类或选科组合不能超过30个字符"));
        assertTrue(message.contains("录取批次不能超过30个字符"));
        assertTrue(message.contains("数据来源不能超过255个字符"));
    }

    @Test
    void shouldTrimTextFieldsBeforeInsert() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
        when(admissionRecordMapper.selectList(any())).thenReturn(List.of());
        Map<String, String> values = rowValues(" 1 ", " 软件工程 ", "2025", "578", "9600");
        values.put("province", " 山西 ");
        values.put("subjectTrack", " 物理类 ");
        values.put("batch", " 本科批 ");
        values.put("source", " 演示数据（非官方） ");
        when(parser.parse(any())).thenReturn(List.of(new ParsedAdmissionRow(2, values)));

        service.importRecords(1L, multipart("records.csv"));

        ArgumentCaptor<AdmissionRecord> captor = ArgumentCaptor.forClass(AdmissionRecord.class);
        verify(admissionRecordMapper).insert(captor.capture());
        assertEquals("山西", captor.getValue().getProvince());
        assertEquals("物理类", captor.getValue().getSubjectTrack());
        assertEquals("本科批", captor.getValue().getBatch());
    }

    @Test
    void shouldRejectMissingUniversity() {
        when(universityMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.importRecords(999L, multipart("records.csv"))
        );

        assertEquals("院校不存在", exception.getMessage());
    }

    @Test
    void shouldBuildExcelTemplateForCurrentMajors() throws Exception {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));

        byte[] content = service.buildTemplate(1L);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("录取记录导入", sheet.getSheetName());
            assertEquals("majorId", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("软件工程", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("演示数据（非官方）",
                    sheet.getRow(1).getCell(9).getStringCellValue());
        }
    }

    @Test
    void shouldRejectTemplateWhenUniversityHasNoMajors() {
        when(universityMapper.selectById(1L)).thenReturn(university(1L));
        when(majorMapper.selectList(any())).thenReturn(List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.buildTemplate(1L)
        );

        assertEquals("请先为该院校添加专业", exception.getMessage());
    }

    private ParsedAdmissionRow parsed(
            int rowNumber,
            String majorId,
            String majorName,
            String year,
            String minScore,
            String minRank
    ) {
        return new ParsedAdmissionRow(
                rowNumber,
                rowValues(majorId, majorName, year, minScore, minRank)
        );
    }

    private Map<String, String> rowValues(
            String majorId,
            String majorName,
            String year,
            String minScore,
            String minRank
    ) {
        Map<String, String> values = new HashMap<>();
        values.put("majorId", majorId);
        values.put("majorName", majorName);
        values.put("province", "山西");
        values.put("year", year);
        values.put("subjectTrack", "物理类");
        values.put("batch", "本科批");
        values.put("minScore", minScore);
        values.put("minRank", minRank);
        values.put("planCount", "38");
        values.put("source", "演示数据（非官方）");
        return values;
    }

    private University university(Long id) {
        University university = new University();
        university.setId(id);
        university.setName("华北示例大学");
        return university;
    }

    private Major major(Long id, String name) {
        Major major = new Major();
        major.setId(id);
        major.setUniversityId(1L);
        major.setName(name);
        return major;
    }

    private AdmissionRecord admissionRecord(Long majorId, Long universityId, int year) {
        AdmissionRecord record = new AdmissionRecord();
        record.setMajorId(majorId);
        record.setUniversityId(universityId);
        record.setProvince("山西");
        record.setYear(year);
        record.setSubjectTrack("物理类");
        record.setBatch("本科批");
        return record;
    }

    private MockMultipartFile multipart(String fileName) {
        return new MockMultipartFile("file", fileName, "application/octet-stream", new byte[]{1});
    }
}
