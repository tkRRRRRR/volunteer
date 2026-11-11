# 学生端院校详情与收藏对比实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐院校专业与历年趋势详情、2 至 4 项收藏对比和可追溯的项目进度记录。

**Architecture:** 新增独立院校详情聚合接口，前端详情页调用该接口绘制趋势图；收藏对比复用已授权的收藏列表和公开院校详情，不在后端新增重复对比接口。后端保持现有 Controller-Service-Mapper 分层，前端按 API、纯数据转换工具和页面组件拆分。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、JUnit 5、Mockito、Vue 3、Element Plus、ECharts、Vitest。

**Spec:** `docs/superpowers/specs/2026-11-11-student-workflow-design.md`

## Global Constraints

- 不修改数据库表结构。
- 保持 `GET /api/universities/{id}` 兼容。
- 新院校详情接口为 `GET /api/universities/{id}/overview`。
- 收藏对比只允许当前用户选择 2 至 4 条收藏。
- 所有展示录取数据必须保留“演示数据（非官方）”来源提示。
- 中文注释只解释业务目的、数据口径和非直观逻辑。
- Git 提交信息使用中文，格式为“类型：中文简述”。
- 每次提交前更新 `01_论文材料/08_过程记录/项目进度.md`。

## Review Focus

- 学生档案没有省份或科类时，院校详情仍应返回全部录取记录。
- 院校没有专业或录取记录时，详情页应显示空状态而不是崩溃。
- 选择专业没有趋势数据时，图表应显示空提示并保留其他信息。
- 收藏选中数量为 0、1、2、4 或大于 4 时，对比按钮和提示必须符合限制。
- 一个收藏项的详情请求失败时，其他收藏项仍应显示，失败项应有明确标记。

---

### Task 1: 新增院校详情聚合接口

**Files:**

- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/MajorVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/AdmissionPointVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/UniversityDetailVO.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/UniversityService.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/UniversityServiceImpl.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/controller/UniversityController.java`
- Create: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/UniversityServiceImplTest.java`
- Create: `01_论文材料/08_过程记录/项目进度.md`

**Interfaces:**

- Produces: `UniversityDetailVO(UniversityVO university, List<MajorVO> majors, List<AdmissionPointVO> admissionPoints)`
- Produces: `MajorVO(Long id, String name, String category, String subjectRequirement, String description)`
- Produces: `AdmissionPointVO(Long majorId, String majorName, Integer year, Integer minScore, Integer minRank, String subjectTrack, String batch, String source)`
- Produces: `UniversityService.getOverview(Long id, String province, String subjectTrack)`
- Consumes: existing `UniversityMapper`、`MajorMapper`、`AdmissionRecordMapper`

- [x] **Step 1: 写院校详情服务失败测试**

在 `UniversityServiceImplTest` 中断言：

```java
@Test
void shouldAggregateUniversityMajorsAndAdmissionPoints() {
    assertEquals("华北示例大学", result.university().name());
    assertEquals(2, result.majors().size());
    assertEquals(3, result.admissionPoints().size());
    assertEquals("演示数据（非官方）", result.admissionPoints().get(0).source());
}

@Test
void shouldReturnEmptyCollectionsWhenUniversityHasNoRelatedData() {
    assertTrue(result.majors().isEmpty());
    assertTrue(result.admissionPoints().isEmpty());
}

@Test
void shouldRejectMissingUniversity() {
    assertThrows(BusinessException.class, () -> service.getOverview(999L, null, null));
}

@Test
void shouldIncludeOptionalProvinceAndSubjectTrackFilters() {
    assertTrue(queryCaptor.getValue().getSqlSegment().contains("province"));
    assertTrue(queryCaptor.getValue().getSqlSegment().contains("subject_track"));
}
```

- [x] **Step 2: 运行测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-backend`）:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=UniversityServiceImplTest test
```

Expected: FAIL，原因是详情 VO、服务和 Mapper 依赖尚不存在。

- [x] **Step 3: 实现聚合接口**

`UniversityServiceImpl` 注入三个 Mapper；按院校编号查询专业，并按可选省份、科类查询录取记录。录取记录按年份、专业编号升序。

- [x] **Step 4: 运行测试确认通过**

Run（工作目录为 `02_项目代码/volunteer-backend`）:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=UniversityServiceImplTest test
```

Expected: PASS。

- [x] **Step 5: 创建进度记录并提交**

记录阶段目标、接口契约、涉及文件和测试结果，然后提交：

```text
新增：实现院校专业与录取趋势详情接口
```

---

### Task 2: 院校详情与趋势图

**Files:**

- Modify: `02_项目代码/volunteer-frontend/package.json`
- Modify: `02_项目代码/volunteer-frontend/package-lock.json`
- Modify: `02_项目代码/volunteer-frontend/src/api/university.js`
- Create: `02_项目代码/volunteer-frontend/src/utils/universityChart.js`
- Create: `02_项目代码/volunteer-frontend/src/utils/universityChart.test.js`
- Modify: `02_项目代码/volunteer-frontend/src/views/university/UniversityListView.vue`

**Interfaces:**

- Consumes: `GET /api/universities/{id}/overview`
- Produces: `buildAdmissionTrendOption(admissionPoints, majorId)`
- Produces: `getUniversityOverview(id, params)` in `api/university.js`

- [x] **Step 1: 写图表数据转换失败测试**

测试同一专业三年分数与位次序列、无数据返回空提示配置、不同专业过滤结果。

```javascript
it('builds three-year score and rank series for one major', () => {
  const option = buildAdmissionTrendOption(points, 1)
  expect(option.series[0].data).toEqual([565, 572, 578])
  expect(option.series[1].data).toEqual([10800, 10100, 9600])
})

it('returns an empty-state option when the selected major has no points', () => {
  expect(buildAdmissionTrendOption(points, 999).graphic[0].type).toBe('text')
})

it('filters points from other majors', () => {
  expect(buildAdmissionTrendOption(points, 1).series[0].data).toHaveLength(3)
})
```

- [x] **Step 2: 增加 Vitest 并确认失败**

在 `package.json` 增加 `test:unit`。Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: FAIL，原因是图表转换工具尚不存在。

- [x] **Step 3: 实现详情请求与趋势图**

抽屉打开后请求详情；展示院校信息、专业表、专业选择器和双轴折线图。请求失败显示重试入口，无专业或无记录显示空状态。

- [x] **Step 4: 运行前端测试和构建**

Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: 全部 PASS。

- [x] **Step 5: 更新进度并提交**

```text
新增：完善院校详情和历年录取趋势
```

---

### Task 3: 收藏多选与横向对比

**Files:**

- Create: `02_项目代码/volunteer-frontend/src/utils/favoriteComparison.js`
- Create: `02_项目代码/volunteer-frontend/src/utils/favoriteComparison.test.js`
- Modify: `02_项目代码/volunteer-frontend/src/views/user/FavoriteView.vue`

**Interfaces:**

- Consumes: `getFavorites()`、`getUniversityOverview(id, params)`
- Produces: `buildComparisonRows(selectedFavorites, detailByUniversityId)`
- Produces: `canCompareFavorites(selectionCount)`

- [x] **Step 1: 写收藏对比失败测试**

覆盖 0、1、2、4、5 项选择；同一院校不同专业；一个详情请求失败但其他项仍生成对比行。

```javascript
it.each([[0, false], [1, false], [2, true], [4, true], [5, false]])(
  'validates comparison selection count %i',
  (count, expected) => expect(canCompareFavorites(count)).toBe(expected)
)

it('keeps unavailable detail items while rendering successful comparisons', () => {
  const rows = buildComparisonRows(favorites, detailMap)
  expect(rows).toHaveLength(3)
  expect(rows.find((row) => row.universityId === 2)?.loadError).toBe(true)
})
```

- [x] **Step 2: 运行测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: FAIL，原因是收藏对比工具尚不存在。

- [x] **Step 3: 实现收藏选择与对比弹窗**

表格增加多选列和 4 项上限提示；对比弹窗展示院校、专业、地区、层次、最近年份、最低分、最低位次和数据来源。

- [x] **Step 4: 运行测试和构建**

Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: 全部 PASS。

- [x] **Step 5: 更新进度并提交**

```text
新增：实现收藏院校专业横向对比
```

---

### Task 4: 文档、完整验证与阶段记录

**Files:**

- Modify: `02_项目代码/docs/api/README.md`
- Modify: `02_项目代码/docs/test/README.md`
- Modify: `README.md`
- Modify: `01_论文材料/08_过程记录/项目进度.md`

- [x] **Step 1: 更新接口与测试文档**

记录院校详情接口参数、返回结构、收藏对比前端流程和本轮测试结果。

- [x] **Step 2: 运行完整验证**

Run:

```powershell
.\dev-environment\setup-environment.ps1 -SkipFrontendInstall
.\dev-environment\start-all.ps1 -NoBrowser
.\dev-environment\verify.ps1
```

Expected: 后端测试、前端构建、端口、数据库和主流程验证全部通过。

- [x] **Step 3: 浏览器手工验收**

验证院校详情、专业切换、趋势图、2 至 4 项收藏对比、空数据和失败提示。

- [x] **Step 4: 完成进度记录并提交**

```text
文档：记录学生端闭环阶段成果
```
