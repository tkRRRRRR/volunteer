# 管理端数据统计与学生账号查看实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为管理员提供数据规模、分布、趋势、收藏排行、数据完整度和学生账号分页查看。

**Architecture:** 后端新增一个只读统计概览接口和学生账号分页接口，一次性加载小规模基础数据进行内存聚合；前端新增独立管理员统计页面，使用 Element Plus 和 ECharts 展示，并用纯函数转换图表配置。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、JUnit 5、Mockito、Vue 3、Element Plus、ECharts、Vitest、Vite。

**Spec:** `docs/superpowers/specs/2026-11-11-admin-statistics-design.md`

## Global Constraints

- 不新增数据库表或字段。
- 所有统计和学生管理接口仅允许 `ADMIN`。
- 学生账号响应不得包含 `passwordHash`。
- 统计使用一次列表读取和内存聚合，禁止逐院校查询。
- 演示数据必须保留“非官方”边界，不形成录取概率结论。
- 页面必须具备加载、空数据、失败重试和刷新状态。
- 必要业务逻辑使用中文注释，Git 提交信息使用中文“类型：中文简述”格式。
- 每阶段更新 `01_论文材料/08_过程记录/项目进度.md`。

## Review Focus

- 没有任何院校、专业、录取记录或收藏时，概览接口应返回 0 和空数组。
- 收藏指向已不存在院校时，排行榜应跳过该记录而不是返回空名称或报错。
- 录取趋势按年份升序，缺少分数或位次的记录仍计入记录数，但不影响对应平均值。
- 学生关键词查询必须同时限制 `STUDENT` 角色，不能返回管理员。
- 学生账号与分页结果不能序列化密码哈希或其他敏感字段。

---

### Task 1: 实现统计概览服务

**Files:**

- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/StatisticItemVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/AdmissionYearTrendVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/FavoriteUniversityStatVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/DataCompletenessVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/AdminStatisticsOverviewVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/AdminStatisticsService.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/AdminStatisticsServiceImpl.java`
- Create: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/AdminStatisticsServiceImplTest.java`

**Interfaces:**

- Consumes: `UniversityMapper`、`MajorMapper`、`AdmissionRecordMapper`、`FavoriteMapper`、`UserMapper`
- Produces:
  - `AdminStatisticsOverviewVO getOverview()`
  - `StatisticItemVO(String label, long count)`
  - `AdmissionYearTrendVO(Integer year, long recordCount, Integer averageMinScore, Integer averageMinRank)`
  - `FavoriteUniversityStatVO(Long universityId, String universityName, long favoriteCount)`
  - `DataCompletenessVO(long universitiesWithoutMajors, long universitiesWithoutAdmissions, long majorsWithoutAdmissions)`

- [ ] **Step 1: 写统计概览失败测试**

构造两个院校、两个专业、三年录取记录和三个收藏，断言：

```java
@Test
void shouldAggregateStatisticsOverview() {
    AdminStatisticsOverviewVO result = service.getOverview();

    assertEquals(2, result.universityCount());
    assertEquals(2, result.majorCount());
    assertEquals(3, result.admissionRecordCount());
    assertEquals(1, result.studentCount());
    assertEquals(3, result.favoriteCount());
    assertEquals("本科", result.universityLevels().get(0).label());
    assertEquals(2, result.universityLevels().get(0).count());
    assertEquals(2025, result.admissionTrends().get(0).year());
    assertEquals("华北示例大学", result.topFavorites().get(0).universityName());
}

@Test
void shouldReturnEmptyStatisticsWhenDatabaseHasNoData() {
    assertTrue(service.getOverview().universityLevels().isEmpty());
    assertEquals(0, service.getOverview().universityCount());
}
```

另覆盖地区分布、平均最低分/位次四舍五入、收藏目标院校不存在时跳过、数据完整度计算。

- [ ] **Step 2: 运行统计测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-backend`）:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdminStatisticsServiceImplTest test
```

Expected: FAIL，原因是统计 VO 和服务尚不存在。

- [ ] **Step 3: 实现概览聚合**

一次读取院校、专业、录取记录和收藏列表；学生数量使用 `UserMapper.selectCount`。

聚合规则：

- 分布项按数量倒序、名称升序。
- 年度趋势按年份升序。
- 平均值使用四舍五入，缺少分数或位次时该平均值为 `null`。
- 收藏 Top 5 只保留仍存在的院校。
- 数据完整度通过院校/专业编号集合计算。

- [ ] **Step 4: 运行统计测试确认通过**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdminStatisticsServiceImplTest test
```

Expected: PASS。

- [ ] **Step 5: 提交概览服务阶段**

```text
新增：实现管理端数据统计聚合服务
```

---

### Task 2: 实现学生账号分页查询与统计接口

**Files:**

- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/dto/AdminStudentQuery.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/vo/StudentVO.java`
- Create: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/controller/AdminStatisticsController.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/AdminStatisticsService.java`
- Modify: `02_项目代码/volunteer-backend/src/main/java/com/example/volunteer/service/impl/AdminStatisticsServiceImpl.java`
- Modify: `02_项目代码/volunteer-backend/src/test/java/com/example/volunteer/service/AdminStatisticsServiceImplTest.java`

**Interfaces:**

- Produces:
  - `PageResult<StudentVO> pageStudents(AdminStudentQuery query)`
  - `GET /api/admin/statistics/overview`
  - `GET /api/admin/students`

- [ ] **Step 1: 写学生分页失败测试**

```java
@Test
void shouldQueryOnlyStudentsAndExcludePasswordHash() {
    PageResult<StudentVO> result = service.pageStudents(studentQuery(" demo "));

    assertEquals(1, result.records().size());
    assertEquals("student1", result.records().get(0).username());
    assertTrue(captor.getValue().getSqlSegment().contains("role"));
    assertTrue(captor.getValue().getSqlSegment().contains("username"));
}
```

验证 `StudentVO` 没有密码字段，并覆盖空关键词、分页参数默认值。

- [ ] **Step 2: 运行学生分页测试确认失败**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository -Dtest=AdminStatisticsServiceImplTest test
```

Expected: FAIL，原因是 DTO、VO 和分页方法尚不存在。

- [ ] **Step 3: 实现学生分页和 Controller**

`AdminStudentQuery` 使用与现有分页 DTO 相同的页码和每页限制。查询条件固定 `role = STUDENT`，关键词匹配用户名或昵称，按创建时间倒序、编号倒序。

Controller 使用 `/api/admin` 作为基础路径和 `@RequireRole({"ADMIN"})`。

- [ ] **Step 4: 运行后端完整测试**

Run:

```powershell
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository test
```

Expected: PASS，统计、学生分页和既有测试全部通过。

- [ ] **Step 5: 提交接口阶段**

```text
新增：提供管理端统计与学生账号接口
```

---

### Task 3: 增加前端统计 API 和图表工具

**Files:**

- Create: `02_项目代码/volunteer-frontend/src/api/adminStatistics.js`
- Create: `02_项目代码/volunteer-frontend/src/utils/adminStatistics.js`
- Create: `02_项目代码/volunteer-frontend/src/utils/adminStatistics.test.js`

**Interfaces:**

- Produces:
  - `getAdminStatistics()`
  - `getAdminStudents(params)`
  - `buildDistributionOption(items, axisName)`
  - `buildYearTrendOption(trends)`
  - `buildCompletenessItems(data)`

- [ ] **Step 1: 写图表工具失败测试**

```javascript
it('builds distribution series from statistics', () => {
  const option = buildDistributionOption([
    { label: '本科', count: 3 },
    { label: '专科', count: 1 }
  ], '院校数量')
  expect(option.xAxis.data).toEqual(['本科', '专科'])
  expect(option.series[0].data).toEqual([3, 1])
})

it('builds three trend series and empty state', () => {
  const option = buildYearTrendOption([
    { year: 2024, recordCount: 4, averageMinScore: 560, averageMinRank: 12000 }
  ])
  expect(option.series.map((item) => item.data)).toEqual([[4], [560], [12000]])
  expect(buildYearTrendOption([]).graphic[0].type).toBe('text')
})
```

另覆盖数据完整度提示和收藏排行空状态。

- [ ] **Step 2: 运行前端测试确认失败**

Run（工作目录为 `02_项目代码/volunteer-frontend`）:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: FAIL，原因是统计图表工具尚不存在。

- [ ] **Step 3: 实现 API 与纯图表工具**

图表工具只负责把 VO 转换为 ECharts option 或提示文案，不访问 DOM。空数据统一返回文字空状态。

- [ ] **Step 4: 运行前端测试确认通过**

Run:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
```

Expected: PASS。

- [ ] **Step 5: 提交前端基础阶段**

```text
新增：添加管理端统计图表工具
```

---

### Task 4: 实现管理端数据统计页面

**Files:**

- Create: `02_项目代码/volunteer-frontend/src/views/admin/AdminStatisticsView.vue`
- Modify: `02_项目代码/volunteer-frontend/src/router/index.js`
- Modify: `02_项目代码/volunteer-frontend/src/layout/AppLayout.vue`

**Interfaces:**

- Consumes: Task 3 的 API 和图表工具
- Produces: `/admin/statistics` 管理员页面

- [ ] **Step 1: 增加路由和菜单**

新增 `admin-statistics` 路由，`roles: ['ADMIN']`，标题“数据统计”。管理菜单增加“数据统计”入口，保留“院校维护”。

- [ ] **Step 2: 实现指标、图表和完整度区域**

页面包含 5 个指标卡、层次柱状图、地区柱状图、年度趋势组合图、收藏 Top 5 和数据完整度卡片。图表在数据加载后绘制，窗口变化时 resize，组件卸载时 dispose。

- [ ] **Step 3: 实现学生账号表格**

学生表格提供关键词、分页、刷新和空状态。列不包含密码字段。接口失败时保留错误提示和重试入口。

- [ ] **Step 4: 运行前端测试和生产构建**

Run:

```powershell
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: PASS，构建成功。

- [ ] **Step 5: 提交页面阶段**

```text
新增：实现管理端数据统计页面
```

---

### Task 5: 更新文档、进度并完成端到端验证

**Files:**

- Modify: `02_项目代码/docs/api/README.md`
- Modify: `02_项目代码/docs/test/README.md`
- Modify: `01_论文材料/08_过程记录/项目进度.md`
- Modify: `README.md`

- [ ] **Step 1: 更新接口和测试文档**

记录统计字段、聚合口径、学生分页参数、敏感字段限制、新增测试数量和浏览器验证结果。

- [ ] **Step 2: 更新项目进度**

记录阶段目标、完成功能、实际测试结果、遗留事项和下一步。

- [ ] **Step 3: 运行完整自动化验证**

Run:

```powershell
cd 02_项目代码/volunteer-backend
& '..\..\dev-environment\runtime\maven-3.9.9\bin\mvn.cmd' -Dmaven.repo.local=..\..\dev-environment\cache\maven-repository clean test
cd ../volunteer-frontend
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run test:unit
& '..\..\dev-environment\runtime\node-20.18.1\npm.cmd' run build
```

Expected: 全部通过。

- [ ] **Step 4: 启动环境并验证页面**

Run:

```powershell
.\dev-environment\start-all.ps1 -NoBrowser
.\dev-environment\verify.ps1
```

浏览器验证：

1. 管理员能够进入数据统计页面。
2. 5 个指标与接口一致。
3. 层次、地区和趋势图正常渲染。
4. 收藏排行和数据完整度显示正确。
5. 学生账号关键词、分页和空状态正常。
6. 学生账号直接访问 `/admin/statistics` 会回到工作台。
7. 控制台无错误。

- [ ] **Step 5: 更新最终进度并提交**

```text
文档：记录管理端数据统计阶段成果
```

