# 管理端 CSV 与 Excel 批量导入实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为当前院校提供 CSV、`.xlsx` 和 `.xls` 录取记录导入、逐行错误反馈和 Excel 模板下载。

**Architecture:** 独立文件解析器负责 CSV/Excel 转成统一文本行，导入服务负责专业归属、字段、文件内重复和数据库重复校验后部分写入；前端在录取记录页签中通过独立导入弹窗上传文件并展示结果。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、Apache Commons CSV、Apache POI、JUnit 5、Mockito、Vue 3、Element Plus、Vitest、Vite。

**Spec:** `docs/superpowers/specs/2026-11-11-admin-csv-import-design.md`

## Global Constraints

- 支持 `.csv`、`.xlsx` 和 `.xls`，只读取 Excel 第一个工作表。
- 不接受 `.xlsm`、`.ods`、密码保护文件和多工作表自动识别。
- 文件不超过 2 MB，数据行不超过 1000 条。
- 必需列为 `majorId,majorName,province,year,subjectTrack,batch,minScore,minRank,planCount,source`。
- `planCount` 可为空，其余字段必填；`majorId` 与 `majorName` 必须相互匹配。
- 专业必须属于 URL 路径中的当前院校，不自动创建院校或专业。
- 合法行部分成功，错误行返回实际行号、专业信息和中文原因。
- 导入成功后清除 `recommendations` 缓存。
- 所有管理接口仅允许 `ADMIN` 角色。
- 必要业务逻辑使用中文注释，Git 提交信息使用中文“类型：中文简述”格式。
- 每阶段更新 `01_论文材料/08_过程记录/项目进度.md`。

## Review Focus

- 上传旧格式 `.xls` 时应按 Excel 解析，不能误判为不支持格式。
- Excel 数字单元格中的年份、专业编号、分数和位次转为文本时不能出现多余小数。
- 空行被跳过后，错误项仍应返回 Excel/CSV 中的真实行号。
- 同一文件内重复与数据库中已有记录应同时被拒绝，合法行仍可写入。
- 文件级错误（空文件、缺列、超限）与行级错误必须使用不同反馈，不能把整个上传显示成服务器异常。

---

### Task 1: 实现 CSV 与 Excel 文件解析器

**Files:**

- Modify: `02_项目代码/volunteer-backend/pom.xml`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/importing/ParsedAdmissionRow.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/importing/AdmissionFileParser.java`
- Create: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/importing/AdmissionFileParserTest.java`

**Interfaces:**

- Consumes: Spring `MultipartFile`
- Produces:
  - `List<ParsedAdmissionRow> parse(MultipartFile file)`
  - `ParsedAdmissionRow(int rowNumber, Map<String, String> values)`
  - `Set<String> REQUIRED_HEADERS`

- [ ] **Step 1: 写解析器失败测试**

测试使用 `MockMultipartFile` 构造 CSV，并使用 `XSSFWorkbook`、`HSSFWorkbook` 在内存中生成 `.xlsx` 和 `.xls`：

```java
@Test
void shouldParseCsvWithBomAndReorderedColumns() {
    List<ParsedAdmissionRow> rows = parser.parse(multipart(
            "records.csv",
            "\uFEFFsource,majorId,majorName,province,year,subjectTrack,batch,minScore,minRank,planCount\n"
                    + "演示数据（非官方）,1,软件工程,山西,2025,物理类,本科批,578,9600,38\n"
    ));

    assertEquals(2, rows.get(0).rowNumber());
    assertEquals("软件工程", rows.get(0).values().get("majorName"));
    assertEquals("578", rows.get(0).values().get("minScore"));
}

@Test
void shouldParseXlsxNumericAndFormulaCellsAsPlainText() {
    byte[] content = workbookBytes(false);

    List<ParsedAdmissionRow> rows = parser.parse(multipart("records.xlsx", content));

    assertEquals("2025", rows.get(0).values().get("year"));
    assertEquals("578", rows.get(0).values().get("minScore"));
}

@Test
void shouldParseLegacyXlsWorkbook() {
    List<ParsedAdmissionRow> rows = parser.parse(
            multipart("records.xls", workbookBytes(true))
    );

    assertEquals("软件工程", rows.get(0).values().get("majorName"));
}
```

另写测试覆盖空文件、未知扩展名、缺列、重复列、超过 1000 行和 Excel 空行跳过。

- [ ] **Step 2: 运行解析器测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-backend`）:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdmissionFileParserTest test
```

Expected: FAIL，原因是解析器和 Apache 文件处理依赖尚不存在。

- [ ] **Step 3: 添加依赖并实现文件解析**

在 `pom.xml` 添加稳定版本：

```text
org.apache.commons:commons-csv:1.11.0
org.apache.poi:poi-ooxml:5.3.0
```

`AdmissionFileParser` 按扩展名选择解析方式：

- CSV 使用 UTF-8 Reader 和 Commons CSV。
- `.xlsx`、`.xls` 使用 `WorkbookFactory.create`。
- Excel 只处理第一个工作表，使用 `DataFormatter` 和 `FormulaEvaluator`。
- 真实行号使用文件中的物理行号，空行跳过但不重新编号。
- 表头校验在返回数据之前完成。
- 文件级异常使用 `BusinessException.badRequest` 返回中文提示。

- [ ] **Step 4: 运行解析器测试确认通过**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdmissionFileParserTest test
```

Expected: PASS，CSV、`.xlsx`、`.xls` 和文件级错误测试全部通过。

- [ ] **Step 5: 提交解析器阶段**

```text
新增：实现 CSV 与 Excel 文件解析
```

---

### Task 2: 实现导入校验与部分写入

**Files:**

- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/AdmissionImportErrorVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/AdmissionImportResultVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/AdmissionImportService.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/AdmissionImportServiceImpl.java`
- Create: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/AdmissionImportServiceImplTest.java`

**Interfaces:**

- Consumes: `AdmissionFileParser.parse(MultipartFile)`、`UniversityMapper`、`MajorMapper`、`AdmissionRecordMapper`
- Produces:
  - `AdmissionImportResultVO importRecords(Long universityId, MultipartFile file)`
  - `AdmissionImportResultVO(int totalRows, int successCount, int failedCount, List<AdmissionImportErrorVO> errors)`
  - `AdmissionImportErrorVO(int rowNumber, String majorId, String majorName, String message)`

- [ ] **Step 1: 写导入服务失败测试**

```java
@Test
void shouldImportValidRowsAndReturnFailedRowDetails() {
    when(universityMapper.selectById(1L)).thenReturn(university(1L));
    when(majorMapper.selectList(any())).thenReturn(List.of(major(1L, "软件工程")));
    when(parser.parse(any())).thenReturn(List.of(
            parsed(2, "1", "软件工程", "2025", "578"),
            parsed(3, "999", "不存在专业", "2026", "590")
    ));

    AdmissionImportResultVO result = service.importRecords(
            1L,
            multipart("records.csv")
    );

    assertEquals(2, result.totalRows());
    assertEquals(1, result.successCount());
    assertEquals(1, result.failedCount());
    assertEquals(3, result.errors().get(0).rowNumber());
    assertEquals("专业不存在", result.errors().get(0).message());
}

@Test
void shouldRejectDuplicateInsideFileAndExistingDatabaseRecord() {
    AdmissionImportResultVO result = service.importRecords(1L, multipart("records.xlsx"));

    assertEquals(0, result.successCount());
    assertTrue(result.errors().stream()
            .anyMatch(error -> error.message().contains("文件内重复")));
    assertTrue(result.errors().stream()
            .anyMatch(error -> error.message().contains("已有录取记录")));
}
```

另覆盖专业名称与编号不匹配、年份范围、分数、位次、计划数、空专业和合法字段首尾空格。

- [ ] **Step 2: 运行导入服务测试确认失败**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdmissionImportServiceImplTest test
```

Expected: FAIL，原因是导入服务和结果对象尚不存在。

- [ ] **Step 3: 实现导入校验与写入**

`importRecords` 按以下顺序执行：

1. 校验院校存在。
2. 一次性加载当前院校专业和已有录取记录。
3. 解析文件，逐行转换和校验。
4. 收集合法记录与全部行错误。
5. 循环插入合法记录。
6. 返回总数、成功数和失败数。

文件内使用判定键集合检测重复；数据库已有记录使用同一判定键集合。错误消息按字段合并，多个原因用“；”连接。

方法添加：

```java
@Transactional
@CacheEvict(value = "recommendations", allEntries = true)
```

- [ ] **Step 4: 运行导入服务测试确认通过**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdmissionImportServiceImplTest test
```

Expected: PASS，部分成功、文件内重复、数据库重复和字段校验全部通过。

- [ ] **Step 5: 提交导入服务阶段**

```text
新增：实现录取记录批量导入校验
```

---

### Task 3: 增加上传接口、Excel 模板与文件级异常处理

**Files:**

- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/controller/AdminAdmissionImportController.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/AdmissionImportService.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/AdmissionImportServiceImpl.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/common/GlobalExceptionHandler.java`
- Modify: `02_项目代码/volunteer-backend/src/main/resources/application.yml`
- Modify: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/AdmissionImportServiceImplTest.java`

**Interfaces:**

- Consumes: Task 2 的 `importRecords(Long, MultipartFile)`
- Produces:
  - `POST /api/admin/universities/{universityId}/admission-records/import`
  - `GET /api/admin/universities/{universityId}/admission-records/import-template`
  - `byte[] buildTemplate(Long universityId)`

- [ ] **Step 1: 写模板和控制器边界失败测试**

断言模板能够被 `WorkbookFactory.create` 重新打开，并且：

```java
assertEquals("录取记录导入", sheet.getSheetName());
assertEquals("majorId", sheet.getRow(0).getCell(0).getStringCellValue());
assertEquals("软件工程", sheet.getRow(1).getCell(1).getStringCellValue());
assertEquals("演示数据（非官方）", sheet.getRow(1).getCell(9).getStringCellValue());
```

测试当前院校没有专业时返回“请先为该院校添加专业”。

- [ ] **Step 2: 运行模板测试确认失败**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdmissionImportServiceImplTest test
```

Expected: FAIL，原因是模板生成方法尚不存在。

- [ ] **Step 3: 实现上传和模板接口**

上传接口使用：

```java
@PostMapping(value = "/{universityId}/admission-records/import",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
```

模板接口返回 `.xlsx` 字节和下载头，文件名为 `录取记录导入模板.xlsx`。Controller 使用 `@RequireRole({"ADMIN"})`。

在 `application.yml` 添加：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 3MB
```

`GlobalExceptionHandler` 捕获 `MaxUploadSizeExceededException`，返回 400 和“上传文件不能超过2MB”。

- [ ] **Step 4: 运行后端完整测试**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository test
```

Expected: PASS，现有 27 项测试和全部导入测试通过。

- [ ] **Step 5: 提交接口阶段**

```text
新增：提供录取记录导入与 Excel 模板接口
```

---

### Task 4: 增加前端导入请求与文件工具

**Files:**

- Modify: `02_项目代码/volunteer-frontend/src/api/adminCatalog.js`
- Modify: `02_项目代码/volunteer-frontend/src/utils/adminCatalog.js`
- Modify: `02_项目代码/volunteer-frontend/src/utils/adminCatalog.test.js`

**Interfaces:**

- Produces:
  - `importAdmissionRecords(universityId, file)`
  - `downloadAdmissionImportTemplate(universityId)`
  - `validateImportFile(file)`
  - `formatFileSize(bytes)`

- [ ] **Step 1: 写前端工具失败测试**

```javascript
it('accepts csv and Excel files within 2 MB', () => {
  expect(validateImportFile({ name: 'a.csv', size: 1024 }).valid).toBe(true)
  expect(validateImportFile({ name: 'a.xlsx', size: 1024 }).valid).toBe(true)
  expect(validateImportFile({ name: 'a.xls', size: 1024 }).valid).toBe(true)
})

it('rejects unsupported files and files over 2 MB', () => {
  expect(validateImportFile({ name: 'a.xlsm', size: 1024 }).message)
    .toBe('仅支持 CSV、XLSX 和 XLS 文件')
  expect(validateImportFile({ name: 'a.csv', size: 2 * 1024 * 1024 + 1 }).message)
    .toBe('文件不能超过 2 MB')
})

it('formats file sizes for display', () => {
  expect(formatFileSize(1536)).toBe('1.5 KB')
})
```

- [ ] **Step 2: 运行前端测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: FAIL，原因是导入工具函数尚不存在。

- [ ] **Step 3: 实现请求与文件工具**

`importAdmissionRecords` 使用 `FormData` 上传参数 `file`，继续使用统一 Axios 实例处理 JSON 结果。

`downloadAdmissionImportTemplate` 使用原生 `fetch` 和 JWT 获取二进制文件，创建临时对象 URL 下载并释放。

`validateImportFile` 同时检查扩展名和大小，返回：

```text
{ valid: boolean, message: string }
```

- [ ] **Step 4: 运行前端测试确认通过**

Run:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: PASS，现有 17 项测试和新增文件工具测试全部通过。

- [ ] **Step 5: 提交前端基础阶段**

```text
新增：添加 CSV 与 Excel 导入请求工具
```

---

### Task 5: 实现批量导入弹窗

**Files:**

- Create: `02_项目代码/volunteer-frontend/src/components/admin/AdminAdmissionImportDialog.vue`
- Modify: `02_项目代码/volunteer-frontend/src/components/admin/AdminAdmissionTab.vue`

**Interfaces:**

- Consumes: Task 4 的四个前端函数
- Produces:
  - `AdminAdmissionImportDialog` 的 `visible`、`university-id`、`university-name` props
  - `update:visible` 和 `imported` events

- [ ] **Step 1: 接入批量导入入口**

在录取记录页签工具栏增加“批量导入”按钮。当前院校没有专业时禁用，并提示先添加专业。点击后打开独立导入弹窗。

- [ ] **Step 2: 实现文件选择、模板下载和提交**

弹窗使用 `el-upload` 的拖拽模式并关闭自动上传。文件选择后先执行 `validateImportFile`，不合法时清除选择并显示提示。

提交时：

- 创建 `FormData` 上传。
- 导入期间禁用按钮。
- 结果展示总数、成功数和失败数。
- 错误表展示行号、专业编号、专业名称和原因。
- 成功数大于 0 时触发 `imported`，关闭后刷新当前录取记录。

- [ ] **Step 3: 实现重置和错误恢复**

重新选择文件时清空上一次结果和文件级错误。模板下载失败显示提示但保留当前导入结果。全部行失败时弹窗不关闭。

- [ ] **Step 4: 运行前端测试和生产构建**

Run:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: 单元测试全部 PASS，Vite 构建退出码为 0。

- [ ] **Step 5: 提交导入页面阶段**

```text
新增：实现 CSV 与 Excel 批量导入弹窗
```

---

### Task 6: 更新文档、进度并完成端到端验证

**Files:**

- Modify: `02_项目代码/docs/api/README.md`
- Modify: `02_项目代码/docs/test/README.md`
- Modify: `01_论文材料/08_过程记录/项目进度.md`
- Modify: `README.md`

- [ ] **Step 1: 更新接口和测试文档**

记录上传格式、列定义、限制、模板下载、部分成功规则、错误返回结构和新增测试数量。

- [ ] **Step 2: 更新项目进度**

记录阶段目标、已完成功能、文件格式、接口、测试命令和实际结果。

- [ ] **Step 3: 运行完整自动化验证**

Run:

```powershell
cd 02_项目代码/volunteer-backend
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository clean test
cd ../volunteer-frontend
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: 后端和前端测试全部通过，构建成功。

- [ ] **Step 4: 启动便携环境并验证导入主流程**

Run（项目根目录）:

```powershell
.\dev-environment\start-all.ps1 -NoBrowser
.\dev-environment\verify.ps1
```

手工验证：

1. 下载 Excel 模板并确认列名和当前专业示例。
2. 上传全部合法的 CSV、`.xlsx` 和 `.xls`。
3. 上传包含合法行和错误行的文件，确认合法行写入且错误行显示真实行号。
4. 上传文件内重复和数据库重复，确认均被拒绝。
5. 上传缺列、空文件和超过 2 MB 文件，确认显示文件级提示。
6. 导入后检查学生端详情和推荐结果，确认缓存已刷新。
7. 浏览器控制台无错误。

- [ ] **Step 5: 更新最终进度并提交**

```text
文档：记录 CSV 与 Excel 导入阶段成果
```

