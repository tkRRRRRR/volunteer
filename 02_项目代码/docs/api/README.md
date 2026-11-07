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
| POST | `/api/admin/universities` | 管理员 | 新增院校 |
| PUT | `/api/admin/universities/{id}` | 管理员 | 更新院校 |
| DELETE | `/api/admin/universities/{id}` | 管理员 | 删除院校 |

## 推荐与收藏

| 方法 | 地址 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/recommendations` | 登录用户 | 生成冲稳保辅助结果 |
| GET | `/api/favorites` | 登录用户 | 查看我的收藏 |
| POST | `/api/favorites` | 登录用户 | 收藏院校或专业 |
| DELETE | `/api/favorites/{id}` | 登录用户 | 取消收藏 |

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
