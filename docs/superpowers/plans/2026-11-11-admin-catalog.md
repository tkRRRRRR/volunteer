# 管理端专业与录取记录维护实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有管理端院校页面中完成专业与录取记录的新增、查询、编辑和删除，并保证院校归属、重复数据和推荐缓存的一致性。

**Architecture:** 后端新增按院校嵌套的管理端数据维护接口，由一个 `AdminCatalogService` 统一处理专业、录取记录、院校归属校验和引用保护；前端在院校列表打开数据维护抽屉，由专业页签和录取记录页签分别调用接口。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、Bean Validation、JUnit 5、Mockito、Vue 3、Element Plus、Vitest、Vite。

**Spec:** `docs/superpowers/specs/2026-11-11-admin-catalog-design.md`

## Global Constraints

- 管理端新增和编辑录取记录时必须绑定当前院校下的具体专业。
- 不修改数据库表结构，历史空专业记录只保留读取兼容。
- 接口使用 `/api/admin/universities/{universityId}/majors` 和 `/api/admin/universities/{universityId}/admission-records`。
- 本阶段不实现 CSV 导入、管理员统计、软删除和操作审计。
- 所有管理接口仅允许 `ADMIN` 角色访问。
- 修改院校、专业或录取记录后必须清除 `recommendations` 缓存。
- 必要业务逻辑使用中文注释，Git 提交信息使用中文“类型：中文简述”格式。
- 每完成一个阶段更新 `01_论文材料/08_过程记录/项目进度.md`。
- 使用项目内便携运行时，不提交 `dev-environment/runtime`、依赖缓存、日志或数据库数据。

## Review Focus

- 请求使用其他院校的专业编号时，必须被后端拒绝，不能仅依赖前端选择器。
- 更新一条录取记录且关键字段未变化时，重复检查必须排除记录自身。
- 删除专业存在录取记录或收藏引用时，必须返回业务提示，不能落到数据库外键异常。
- 任意院校、专业或录取记录写操作后，推荐结果不能继续命中旧缓存。
- 当前院校没有专业时，录取记录新增入口必须禁用并提示先新增专业。

---

### Task 1: 实现管理端专业维护接口

**Files:**

- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/dto/MajorSaveRequest.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/AdminCatalogService.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/AdminCatalogServiceImpl.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/controller/AdminCatalogController.java`
- Create: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/AdminCatalogServiceImplTest.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/UniversityServiceImpl.java`

**Interfaces:**

- Consumes: `UniversityMapper`、`MajorMapper`、`AdmissionRecordMapper`、`FavoriteMapper`
- Produces:
  - `List<MajorVO> listMajors(Long universityId, String keyword)`
  - `MajorVO createMajor(Long universityId, MajorSaveRequest request)`
  - `MajorVO updateMajor(Long universityId, Long majorId, MajorSaveRequest request)`
  - `void deleteMajor(Long universityId, Long majorId)`
  - `MajorSaveRequest(String name, String category, String subjectRequirement, String description)`

- [ ] **Step 1: 写专业维护失败测试**

在 `AdminCatalogServiceImplTest` 中初始化 `Major`、`AdmissionRecord` 和 `Favorite` 的 MyBatis 元数据，并断言：

```java
@Test
void shouldCreateMajorUnderExistingUniversity() {
    when(universityMapper.selectById(1L)).thenReturn(university(1L));

    MajorVO result = service.createMajor(1L, majorRequest("软件工程"));

    assertEquals("软件工程", result.name());
    verify(majorMapper).insert(argThat(major ->
            major.getUniversityId().equals(1L)
                    && major.getName().equals("软件工程")));
}

@Test
void shouldRejectMajorThatBelongsToAnotherUniversity() {
    Major major = major(9L, 2L, "人工智能");
    when(universityMapper.selectById(1L)).thenReturn(university(1L));
    when(majorMapper.selectById(9L)).thenReturn(major);

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.updateMajor(1L, 9L, majorRequest("人工智能"))
    );

    assertEquals("专业不属于当前院校", exception.getMessage());
}

@Test
void shouldRejectDeletingMajorReferencedByAdmissionOrFavorite() {
    when(universityMapper.selectById(1L)).thenReturn(university(1L));
    when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
    when(admissionRecordMapper.selectCount(any())).thenReturn(1L);

    BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.deleteMajor(1L, 1L)
    );

    assertEquals("该专业已有录取记录，不能删除", exception.getMessage());
}
```

同时覆盖正常查询关键词、正常更新、正常删除，以及 `UniversityServiceImpl.delete` 在院校仍有专业时返回“请先删除该院校下的专业”。

- [ ] **Step 2: 运行专业测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-backend`）:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdminCatalogServiceImplTest,UniversityServiceImplTest test
```

Expected: FAIL，原因是 `AdminCatalogService`、`MajorSaveRequest` 和相关实现尚不存在。

- [ ] **Step 3: 实现专业维护服务和接口**

`AdminCatalogServiceImpl` 使用构造器注入上述四个 Mapper。专业新增和更新统一去除字符串首尾空格；更新和删除必须校验专业属于路径中的院校。删除前依次检查录取记录和收藏引用。

`AdminCatalogController` 使用 `@RequireRole({"ADMIN"})`，实现：

```java
@GetMapping("/{universityId}/majors")
ApiResponse<List<MajorVO>> listMajors(
        @PathVariable Long universityId,
        @RequestParam(required = false) String keyword
)

@PostMapping("/{universityId}/majors")
ApiResponse<MajorVO> createMajor(
        @PathVariable Long universityId,
        @Valid @RequestBody MajorSaveRequest request
)

@PutMapping("/{universityId}/majors/{majorId}")
ApiResponse<MajorVO> updateMajor(
        @PathVariable Long universityId,
        @PathVariable Long majorId,
        @Valid @RequestBody MajorSaveRequest request
)

@DeleteMapping("/{universityId}/majors/{majorId}")
ApiResponse<Void> deleteMajor(
        @PathVariable Long universityId,
        @PathVariable Long majorId
)
```

专业写操作增加：

```java
@CacheEvict(value = "recommendations", allEntries = true)
```

现有院校新增、更新和删除同样增加该注解；院校删除前先检查专业数量。

- [ ] **Step 4: 运行专业测试确认通过**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdminCatalogServiceImplTest,UniversityServiceImplTest test
```

Expected: PASS，专业归属、引用保护和院校删除保护测试全部通过。

- [ ] **Step 5: 提交专业维护阶段**

```text
新增：实现管理端专业维护接口
```

---

### Task 2: 实现管理端录取记录维护接口

**Files:**

- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/dto/AdminAdmissionQuery.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/dto/AdmissionRecordSaveRequest.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/AdminAdmissionRecordVO.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/AdminCatalogService.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/AdminCatalogServiceImpl.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/controller/AdminCatalogController.java`
- Modify: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/AdminCatalogServiceImplTest.java`

**Interfaces:**

- Consumes: Task 1 的 `AdminCatalogService`、Mapper 依赖和院校/专业归属校验
- Produces:
  - `PageResult<AdminAdmissionRecordVO> pageAdmissionRecords(Long universityId, AdminAdmissionQuery query)`
  - `AdminAdmissionRecordVO createAdmissionRecord(Long universityId, AdmissionRecordSaveRequest request)`
  - `AdminAdmissionRecordVO updateAdmissionRecord(Long universityId, Long recordId, AdmissionRecordSaveRequest request)`
  - `void deleteAdmissionRecord(Long universityId, Long recordId)`
  - `AdminAdmissionRecordVO(Long id, Long universityId, Long majorId, String majorName, String province, Integer year, String subjectTrack, String batch, Integer minScore, Integer minRank, Integer planCount, String source)`

- [ ] **Step 1: 写录取记录失败测试**

在 `AdminCatalogServiceImplTest` 中断言：

```java
@Test
void shouldCreateAdmissionRecordForMajorInSameUniversity() {
    when(universityMapper.selectById(1L)).thenReturn(university(1L));
    when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
    when(admissionRecordMapper.selectCount(any())).thenReturn(0L);

    AdminAdmissionRecordVO result =
            service.createAdmissionRecord(1L, admissionRequest(1L, 2025));

    assertEquals("软件工程", result.majorName());
    assertEquals(1L, result.universityId());
    verify(admissionRecordMapper).insert(any());
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
    AdmissionRecord existing = record(20L, 1L, 1L, 2025);
    when(universityMapper.selectById(1L)).thenReturn(university(1L));
    when(admissionRecordMapper.selectById(20L)).thenReturn(existing);
    when(majorMapper.selectById(1L)).thenReturn(major(1L, 1L, "软件工程"));
    when(admissionRecordMapper.selectCount(any())).thenReturn(0L);

    AdminAdmissionRecordVO result =
            service.updateAdmissionRecord(1L, 20L, admissionRequest(1L, 2025));

    assertEquals(20L, result.id());
    verify(admissionRecordMapper).updateById(any());
}
```

同时覆盖分页筛选、记录不存在或不属于当前院校、正常删除，以及当前专业为空时返回“请先为该院校添加专业”。

- [ ] **Step 2: 运行录取记录测试确认失败**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdminCatalogServiceImplTest test
```

Expected: FAIL，原因是录取记录 DTO、VO 和服务方法尚不存在。

- [ ] **Step 3: 实现录取记录服务和接口**

`AdminAdmissionQuery` 使用 `@Min` 和 `@Max` 限制页码与每页数量。`AdmissionRecordSaveRequest` 要求 `majorId`、省份、年份、科类、批次、最低分、最低位次和来源非空，并限制分数、年份、位次与计划数范围。

服务层按以下键检查重复：

```text
universityId + majorId + province + year + subjectTrack + batch
```

更新时增加 `.ne(AdmissionRecord::getId, recordId)`。查询按年份倒序、记录编号倒序。保存时统一从路径中的 `universityId` 赋值，不接受请求体覆盖。

Controller 增加四条嵌套接口，四个写操作均添加：

```java
@CacheEvict(value = "recommendations", allEntries = true)
```

- [ ] **Step 4: 运行后端完整测试**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository test
```

Expected: PASS，现有 `UniversityServiceImplTest`、`RecommendationScorerTest` 和全部新增服务测试通过。

- [ ] **Step 5: 提交录取记录维护阶段**

```text
新增：实现管理端录取记录维护接口
```

---

### Task 3: 增加管理端前端接口与表单工具

**Files:**

- Create: `02_项目代码/volunteer-frontend/src/api/adminCatalog.js`
- Create: `02_项目代码/volunteer-frontend/src/utils/adminCatalog.js`
- Create: `02_项目代码/volunteer-frontend/src/utils/adminCatalog.test.js`

**Interfaces:**

- Consumes: 请求封装 `../utils/request`
- Produces:
  - `listMajors(universityId, params)`
  - `createMajor(universityId, data)`
  - `updateMajor(universityId, majorId, data)`
  - `deleteMajor(universityId, majorId)`
  - `getAdmissionRecords(universityId, params)`
  - `createAdmissionRecord(universityId, data)`
  - `updateAdmissionRecord(universityId, recordId, data)`
  - `deleteAdmissionRecord(universityId, recordId)`
  - `buildMajorPayload(form)`
  - `buildAdmissionPayload(form)`
  - `buildAdmissionQuery(filters)`

- [ ] **Step 1: 写前端工具失败测试**

```javascript
it('trims major text fields', () => {
  expect(buildMajorPayload({
    name: ' 软件工程 ',
    category: ' 工学 ',
    subjectRequirement: ' 物理+不限 ',
    description: ' 演示专业 '
  })).toEqual({
    name: '软件工程',
    category: '工学',
    subjectRequirement: '物理+不限',
    description: '演示专业'
  })
})

it('normalizes optional plan count and keeps required numbers', () => {
  expect(buildAdmissionPayload({
    majorId: 1,
    planCount: '',
    minScore: '578',
    minRank: '9600'
  })).toMatchObject({
    majorId: 1,
    planCount: null,
    minScore: 578,
    minRank: 9600
  })
})

it('removes empty admission filters', () => {
  expect(buildAdmissionQuery({
    page: '2',
    size: '10',
    majorId: '',
    year: null,
    province: ' 山西 '
  })).toEqual({ page: 2, size: 10, province: '山西' })
})
```

- [ ] **Step 2: 运行前端测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: FAIL，原因是 `adminCatalog.js` 尚不存在。

- [ ] **Step 3: 实现接口封装和纯数据工具**

API 封装保持现有 `request` 调用方式。工具函数只负责字符串清理、空值移除和数字转换，不包含 Element Plus 提示或页面状态。

- [ ] **Step 4: 运行前端测试确认通过**

Run:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: PASS，现有 11 项测试和新增测试全部通过。

- [ ] **Step 5: 提交前端基础阶段**

```text
新增：添加管理端数据维护请求工具
```

---

### Task 4: 实现专业与录取记录维护抽屉

**Files:**

- Create: `02_项目代码/volunteer-frontend/src/components/admin/AdminCatalogDrawer.vue`
- Create: `02_项目代码/volunteer-frontend/src/components/admin/AdminMajorTab.vue`
- Create: `02_项目代码/volunteer-frontend/src/components/admin/AdminAdmissionTab.vue`
- Modify: `02_项目代码/volunteer-frontend/src/views/admin/AdminUniversityView.vue`

**Interfaces:**

- Consumes: Task 3 的全部 API 和表单工具
- Produces:
  - `AdminCatalogDrawer` 的 `v-model:visible` 与 `university` props
  - `AdminMajorTab` 的 `university-id` prop 和 `changed` event
  - `AdminAdmissionTab` 的 `university-id` 与 `major-revision` props

- [ ] **Step 1: 接入院校列表的数据维护入口**

在 `AdminUniversityView.vue` 增加当前院校和数据抽屉可见状态，表格操作列增加“数据维护”，并将操作列宽度调整为可容纳三个按钮。保留现有新增、编辑和删除逻辑。

- [ ] **Step 2: 实现抽屉与专业页签**

`AdminCatalogDrawer` 显示院校名称和地区，两个页签分别渲染专业和录取记录组件。院校切换时重置到专业页签并刷新数据。

`AdminMajorTab` 实现：

- 关键词查询。
- 专业表格、空状态和失败重试。
- 新增与编辑共用表单，专业名称必填。
- 删除二次确认。
- 保存期间禁用重复提交。
- 专业数据改变后触发 `changed`，让录取记录页签重新加载专业选项。

- [ ] **Step 3: 实现录取记录页签**

`AdminAdmissionTab` 实现：

- 加载当前院校专业选项。
- 专业、年份、省份和科类筛选。
- 分页表格与空状态。
- 新增和编辑共用表单，专业必填，来源默认“演示数据（非官方）”。
- 院校没有专业时禁用新增按钮，并提示先添加专业。
- 保存期间禁用重复提交，失败后保留表单内容。

- [ ] **Step 4: 运行前端测试和生产构建**

Run:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: 单元测试全部 PASS，Vite 构建退出码为 0。

- [ ] **Step 5: 提交管理页面阶段**

```text
新增：实现管理端专业与录取数据维护页面
```

---

### Task 5: 更新文档、进度并完成端到端验证

**Files:**

- Modify: `02_项目代码/docs/api/README.md`
- Modify: `02_项目代码/docs/test/README.md`
- Modify: `README.md`
- Modify: `01_论文材料/08_过程记录/项目进度.md`

**Interfaces:**

- Consumes: Task 1 至 Task 4 已完成的管理端接口与页面
- Produces: 可追溯的接口说明、测试证据、项目进度和阶段验收记录

- [ ] **Step 1: 更新接口与测试文档**

记录专业和录取记录的嵌套接口、查询参数、必填规则、错误场景、推荐缓存失效策略，以及后端和前端实际测试数量。

- [ ] **Step 2: 更新项目进度**

在 `项目进度.md` 新增“管理端专业与录取记录维护阶段”，记录目标、接口、页面、数据校验、测试命令和结果、未完成事项。

- [ ] **Step 3: 运行完整自动化验证**

Run（工作目录为 `02_项目代码/volunteer-backend`）:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository clean test
```

Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: 后端和后端测试全部通过，前端测试全部通过，生产构建成功。

- [ ] **Step 4: 启动便携环境并验证主流程**

Run（项目根目录）:

```powershell
.\dev-environment\start-all.ps1 -NoBrowser
.\dev-environment\verify.ps1
```

手工验证：

1. 管理员登录并从院校列表打开“数据维护”。
2. 新增一个专业，确认学生端院校详情可见。
3. 在该专业下新增录取记录，确认学生端趋势图可见。
4. 重复提交同一专业、年份、省份、科类和批次，确认被拒绝。
5. 删除已有录取记录的专业，确认提示不能删除。
6. 修改录取记录后重新请求推荐，确认结果反映最新数据。
7. 检查浏览器控制台无错误。

- [ ] **Step 5: 更新最终进度并提交**

```text
文档：记录管理端数据维护阶段成果
```

