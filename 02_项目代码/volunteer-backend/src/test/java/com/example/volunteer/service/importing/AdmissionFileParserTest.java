package com.example.volunteer.service.importing;

import com.example.volunteer.common.BusinessException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdmissionFileParserTest {

    private AdmissionFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new AdmissionFileParser();
    }

    @Test
    void shouldParseCsvWithBomReorderedColumnsAndQuotedFields() {
        String csv = "\uFEFFsource,majorId,majorName,province,year,subjectTrack,batch,minScore,minRank,planCount\n"
                + "\"演示数据,非官方\",1,软件工程,山西,2025,物理类,本科批,578,9600,38\n";

        List<ParsedAdmissionRow> rows = parser.parse(multipart("records.csv", csv));

        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).rowNumber());
        assertEquals("1", rows.get(0).values().get("majorId"));
        assertEquals("软件工程", rows.get(0).values().get("majorName"));
        assertEquals("演示数据,非官方", rows.get(0).values().get("source"));
    }

    @Test
    void shouldSkipBlankCsvRowsWithoutRenumbering() {
        String csv = standardHeader() + "\n"
                + "1,软件工程,山西,2025,物理类,本科批,578,9600,38,演示数据（非官方）\n"
                + "\n"
                + "1,软件工程,山西,2024,物理类,本科批,572,10100,40,演示数据（非官方）\n";

        List<ParsedAdmissionRow> rows = parser.parse(multipart("records.csv", csv));

        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).rowNumber());
        assertEquals(4, rows.get(1).rowNumber());
    }

    @Test
    void shouldParseXlsxNumericAndFormulaCellsAsPlainText() throws Exception {
        List<ParsedAdmissionRow> rows = parser.parse(
                multipart("records.xlsx", workbookBytes(false))
        );

        assertEquals(1, rows.size());
        assertEquals("2025", rows.get(0).values().get("year"));
        assertEquals("578", rows.get(0).values().get("minScore"));
        assertEquals("9600", rows.get(0).values().get("minRank"));
    }

    @Test
    void shouldParseLegacyXlsWorkbook() throws Exception {
        List<ParsedAdmissionRow> rows = parser.parse(
                multipart("records.xls", workbookBytes(true))
        );

        assertEquals("软件工程", rows.get(0).values().get("majorName"));
    }

    @Test
    void shouldRejectEmptyFile() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parse(multipart("records.csv", ""))
        );

        assertEquals("导入文件不能为空", exception.getMessage());
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parse(multipart("records.xlsm", standardHeader()))
        );

        assertEquals("仅支持 CSV、XLSX 和 XLS 文件", exception.getMessage());
    }

    @Test
    void shouldRejectMissingRequiredHeader() {
        String csv = "majorId,majorName,province,year,subjectTrack,batch,minScore,minRank,source\n"
                + "1,软件工程,山西,2025,物理类,本科批,578,9600,演示数据（非官方）\n";

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parse(multipart("records.csv", csv))
        );

        assertEquals("缺少必需列：planCount", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateHeader() {
        String csv = standardHeader().replace("majorName", "majorId") + "\n"
                + "1,1,山西,2025,物理类,本科批,578,9600,38,演示数据（非官方）\n";

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parse(multipart("records.csv", csv))
        );

        assertEquals("表头不能重复：majorId", exception.getMessage());
    }

    @Test
    void shouldRejectMoreThanOneThousandDataRows() {
        StringBuilder csv = new StringBuilder(standardHeader()).append('\n');
        for (int index = 0; index < 1001; index++) {
            csv.append("1,软件工程,山西,2025,物理类,本科批,578,9600,38,演示数据（非官方）\n");
        }

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> parser.parse(multipart("records.csv", csv.toString()))
        );

        assertEquals("最多导入1000条记录", exception.getMessage());
    }

    private MockMultipartFile multipart(String fileName, String content) {
        return multipart(fileName, content.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile multipart(String fileName, byte[] content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "application/octet-stream",
                content
        );
    }

    private byte[] workbookBytes(boolean legacy) throws Exception {
        try (Workbook workbook = legacy ? new HSSFWorkbook() : new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("录取记录");
            writeHeader(sheet.createRow(0));
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue("软件工程");
            row.createCell(2).setCellValue("山西");
            row.createCell(3).setCellValue(2025);
            row.createCell(4).setCellValue("物理类");
            row.createCell(5).setCellValue("本科批");
            row.createCell(6).setCellFormula("578+0");
            row.createCell(7).setCellValue(9600);
            row.createCell(8).setCellValue(38);
            row.createCell(9).setCellValue("演示数据（非官方）");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void writeHeader(Row row) {
        String[] headers = {
                "majorId",
                "majorName",
                "province",
                "year",
                "subjectTrack",
                "batch",
                "minScore",
                "minRank",
                "planCount",
                "source"
        };
        for (int index = 0; index < headers.length; index++) {
            row.createCell(index).setCellValue(headers[index]);
        }
    }

    private String standardHeader() {
        return "majorId,majorName,province,year,subjectTrack,batch,minScore,minRank,planCount,source";
    }
}
