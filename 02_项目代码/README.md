# 高考志愿填报辅助系统

首版代码采用 Spring Boot 3 与 Vue 3 前后端分离结构，提供账号登录、院校查询、历史录取记录、冲稳保辅助推荐和收藏管理。

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、Redis、JWT、OpenAPI、JUnit 5。
- 前端：Vue 3、Vite、JavaScript、Vue Router、Pinia、Axios、Element Plus、ECharts。

## 目录

```text
volunteer-backend/   Spring Boot 后端
volunteer-frontend/  Vue 3 前端
database/            MySQL 建表脚本与演示数据
docs/                需求、设计、接口、测试和部署资料
scripts/             辅助脚本
```

## 本地启动

### 1. 启动 MySQL 和 Redis

安装 Docker Desktop 后，在 `02_项目代码` 目录执行：

```powershell
docker compose up -d
```

不使用 Docker 时，请自行安装 MySQL 8 和 Redis 7，并依次导入：

```text
database/schema/01_schema.sql
database/seed/02_seed.sql
```

### 2. 启动后端

确保本机已安装 JDK 17 和 Maven 3.9，然后执行：

```powershell
cd volunteer-backend
mvn spring-boot:run
```

默认数据库账号为 `root`，密码为 `root123456`。可通过环境变量 `DB_USERNAME`、`DB_PASSWORD` 和 `JWT_SECRET` 覆盖。

接口文档地址：`http://localhost:8080/swagger-ui.html`

### 3. 启动前端

安装 Node.js 20 或更高版本后执行：

```powershell
cd volunteer-frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。

## 演示账号

- 学生账号：`student` / `student123`
- 管理员账号：`admin` / `admin123`

演示院校名称和录取记录均为虚构数据，不代表任何学校的真实招生结果。

## Redis 模式

默认使用 Spring 本地缓存，便于单独启动。启动 Redis 后，用以下参数运行后端可启用 Redis 缓存：

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

## 推荐结果说明

推荐功能依据演示录取记录，按位次差异和分数差异计算匹配分，再划分冲刺、稳妥、保底和不建议。结果仅用于系统功能展示，不构成志愿填报建议，也不承诺录取。
