package com.example.volunteer.service.importing;

import com.example.volunteer.common.BusinessException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 将 CSV、XLSX 和 XLS 统一解析为文本行，文件格式差异不进入业务校验层。
 */
@Component
public class AdmissionFileParser {

    public static final Set<String> REQUIRED_HEADERS = Set.of(
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
    );

    public static final List<String> HEADER_ORDER = List.of(
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
    );
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final int MAX_DATA_ROWS = 1000;

    public List<ParsedAdmissionRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("导入文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("上传文件不能超过2MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        try {
            return switch (extension) {
                case "csv" -> parseCsv(file);
                case "xlsx", "xls" -> parseExcel(file);
                default -> throw BusinessException.badRequest("仅支持 CSV、XLSX 和 XLS 文件");
            };
        } catch (BusinessException exception) {
            throw exception;
        } catch (EncryptedDocumentException exception) {
            throw BusinessException.badRequest("Excel 文件受密码保护或无法读取");
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.badRequest("文件格式不正确，请检查模板和文件内容");
        }
    }

    private List<ParsedAdmissionRow> parseCsv(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(false)
                .setTrim(true)
                .build();
        try (CSVParser parser = new CSVParser(new StringReader(content), format)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw BusinessException.badRequest("导入文件不能为空");
            }
            List<String> headers = records.get(0).toList().stream()
                    .map(String::trim)
                    .toList();
            validateHeaders(headers);
            List<ParsedAdmissionRow> rows = new ArrayList<>();
            for (int index = 1; index < records.size(); index++) {
                CSVRecord record = records.get(index);
                List<String> values = record.toList();
                if (allBlank(values)) {
                    continue;
                }
                addRow(rows, index + 1, headers, values);
            }
            validateRowLimit(rows.size());
            return rows;
        }
    }

    private List<ParsedAdmissionRow> parseExcel(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw BusinessException.badRequest("Excel 文件没有工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw BusinessException.badRequest("导入文件不能为空");
            }
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw BusinessException.badRequest("缺少表头");
            }
            List<String> headers = excelRowValues(headerRow, formatter, evaluator);
            validateHeaders(headers);

            List<ParsedAdmissionRow> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                List<String> values = excelRowValues(row, formatter, evaluator);
                if (allBlank(values)) {
                    continue;
                }
                addRow(rows, rowIndex + 1, headers, values);
            }
            validateRowLimit(rows.size());
            return rows;
        }
    }

    private List<String> excelRowValues(
            Row row,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        List<String> values = new ArrayList<>();
        int lastCell = Math.max(row.getLastCellNum(), 0);
        for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            values.add(cell == null
                    ? ""
                    : formatter.formatCellValue(cell, evaluator).trim());
        }
        return values;
    }

    private void addRow(
            List<ParsedAdmissionRow> rows,
            int rowNumber,
            List<String> headers,
            List<String> values
    ) {
        Map<String, String> mapped = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < values.size() ? values.get(index) : "";
            mapped.put(headers.get(index), value == null ? "" : value.trim());
        }
        rows.add(new ParsedAdmissionRow(rowNumber, mapped));
    }

    private void validateHeaders(List<String> headers) {
        Set<String> seen = new LinkedHashSet<>();
        for (String header : headers) {
            if (!seen.add(header)) {
                throw BusinessException.badRequest("表头不能重复：" + header);
            }
        }
        for (String required : HEADER_ORDER) {
            if (!seen.contains(required)) {
                throw BusinessException.badRequest("缺少必需列：" + required);
            }
        }
    }

    private void validateRowLimit(int rowCount) {
        if (rowCount > MAX_DATA_ROWS) {
            throw BusinessException.badRequest("最多导入1000条记录");
        }
    }

    private boolean allBlank(List<String> values) {
        return values.stream().allMatch(value -> !StringUtils.hasText(value));
    }

    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        String normalized = StringUtils.cleanPath(fileName);
        int dot = normalized.lastIndexOf('.');
        if (dot < 0 || dot == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
