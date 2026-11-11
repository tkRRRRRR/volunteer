# 首版接口说明

所有业务接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-11-07 10:00:00"
}
```

需要登录的接口在请求头中携带：

```text
Authorization: Bearer <token>
```

## 账号

| 方法 | 地址 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 公开 | 学生注册 |
| POST | `/api/auth/login` | 公开 | 登录并获取 JWT |
| GET | `/api/auth/profile` | 登录用户 | 获取个人档案 |
| PUT | `/api/auth/profile` | 登录用户 | 更新个人档案 |

## 院校

| 方法 | 地址 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/universities` | 公开 | 按关键词、省份、层次分页查询 |
| GET | `/api/universities/{id}` | 公开 | 查看院校详情 |
| GET | `/api/universities/{id}/overview` | 公开 | 查看专业和历年录取趋势 |
| POST | `/api/admin/universities` | 管理员 | 新增院校 |
| PUT | `/api/admin/universities/{id}` | 管理员 | 更新院校 |
| DELETE | `/api/admin/universities/{id}` | 管理员 | 删除院校 |

## 管理端专业与录取记录

所有接口必须由管理员登录后访问，院校编号从路径中取得。

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/universities/{universityId}/majors` | 查询当前院校专业，可传 `keyword` |
| POST | `/api/admin/universities/{universityId}/majors` | 新增专业 |
| PUT | `/api/admin/universities/{universityId}/majors/{majorId}` | 更新专业 |
| DELETE | `/api/admin/universities/{universityId}/majors/{majorId}` | 删除专业 |
| GET | `/api/admin/universities/{universityId}/admission-records` | 分页查询录取记录 |
| POST | `/api/admin/universities/{universityId}/admission-records` | 新增录取记录 |
| PUT | `/api/admin/universities/{universityId}/admission-records/{recordId}` | 更新录取记录 |
| DELETE | `/api/admin/universities/{universityId}/admission-records/{recordId}` | 删除录取记录 |

录取记录查询支持 `page`、`size`、`majorId`、`year`、`province` 和 `subjectTrack`。每条新增或更新的录取记录都必须绑定当前院校下的具体专业。

后端拒绝以下操作：

- 使用其他院校的专业编号新增录取记录。
- 同一院校、专业、年份、省份、科类和批次出现重复录取记录。
- 删除仍有录取记录或收藏引用的专业。
- 删除仍有专业的院校。

院校、专业和录取记录写操作完成后会清除推荐缓存，避免继续返回旧匹配结果。

## 推荐与收藏

| 方法 | 地址 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/recommendations` | 登录用户 | 生成冲稳保辅助结果 |
| GET | `/api/favorites` | 登录用户 | 查看我的收藏 |
| POST | `/api/favorites` | 登录用户 | 收藏院校或专业 |
| DELETE | `/api/favorites/{id}` | 登录用户 | 取消收藏 |

院校详情查询支持可选参数 `province` 和 `subjectTrack`。不传参数时返回全部记录，传入后仅返回匹配省份和科类的录取记录。返回结构包含基础院校信息、专业列表和按年份升序排列的录取数据点。

收藏对比由前端完成：先从收藏列表选择 2 至 4 项，再按院校编号请求详情接口。前端保留单个请求失败项并标记，不影响其他收藏项展示。

推荐请求示例：

```json
{
  "province": "山西",
  "examYear": 2026,
  "subjectTrack": "物理类",
  "score": 560,
  "provinceRank": 10000,
  "preferredProvince": "",
  "universityLevel": "",
  "keyword": ""
}
```

在线接口文档由 Springdoc 自动生成，后端启动后访问 `http://localhost:8080/swagger-ui.html`。
