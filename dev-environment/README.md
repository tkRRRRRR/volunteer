# 高考志愿填报辅助系统便携开发环境

本目录集中存放项目所需的 JDK、Maven、Node.js、MySQL 和 Redis。运行时使用免安装压缩包，不修改系统注册表，不注册 Windows 服务，也不要求手工设置全局环境变量。

## 1. 适用范围

- Windows 10 或 Windows 11，64 位。
- 建议至少 8 GB 内存，预留 3 GB 磁盘空间。
- 首次从 Git 仓库获取项目时，需要联网下载环境或安装项目依赖。
- 如果直接复制已经配置好的整个项目文件夹，可把 `runtime/`、`downloads/`、`data/` 和 `cache/` 一起复制，通常不需要重新下载运行时。

当前固定版本：

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Eclipse Temurin JDK | 17.0.21+9 | 编译和运行 Spring Boot |
| Apache Maven | 3.9.9 | 后端依赖管理与构建 |
| Node.js | 20.18.1 | 前端构建与开发服务器 |
| MySQL Community Server | 8.0.34 | 业务数据库 |
| Redis for Windows | 5.0.14.1 | 推荐结果缓存 |

Redis Windows 构建用于本地学习和开发；正式部署时应使用 Linux 官方 Redis 或兼容服务。下载地址、固定版本、SHA-256 和许可证记录在 `environment.psd1`，说明见 [VERSIONS.md](VERSIONS.md)。

## 2. 目录结构

```text
dev-environment/
├─ README.md                  本教程
├─ VERSIONS.md                版本、来源、校验值和许可证
├─ VERIFICATION-REPORT.md     本次实际验证结果
├─ environment.psd1           自动化下载与校验清单
├─ .env.example               环境变量示例
├─ setup-environment.ps1      首次安装、初始化和编译检查
├─ start-databases.ps1        启动 MySQL 与 Redis
├─ stop-databases.ps1         停止 MySQL 与 Redis
├─ start-backend.ps1          启动 Spring Boot
├─ start-frontend.ps1         启动 Vue
├─ start-all.ps1              一键启动全部服务
├─ stop-all.ps1               一键停止全部服务
├─ verify.ps1                 完整环境与主流程验证
├─ scripts/common.ps1         公共路径、配置和进程函数
├─ runtime/                   便携 JDK、Maven、Node、MySQL、Redis
├─ downloads/                 固定版本压缩包
├─ cache/maven-repository/    项目专用 Maven 本地仓库
├─ data/mysql/                MySQL 数据文件
├─ data/redis/                Redis 配置与持久化文件
└─ logs/                      运行日志和进程号
```

## 3. 首次安装

在项目根目录打开 PowerShell，执行：

```powershell
Set-Location .\dev-environment
.\setup-environment.ps1
```

脚本会依次完成：

1. 检查并安装固定版本的 JDK、Maven、Node.js、MySQL 和 Redis。
2. 校验压缩包 SHA-256。
3. 创建项目专用 MySQL 数据目录。
4. 启动 MySQL 和 Redis。
5. 导入建表脚本与虚构演示数据。
6. 缺少前端依赖时执行 `npm ci`。
7. 使用项目目录内的 Maven 仓库编译并测试后端。

如果 PowerShell 阻止执行本地脚本，可只对当前窗口临时放开：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

该命令不会修改系统永久执行策略。

## 4. 日常启动

一键启动：

```powershell
.\start-all.ps1
```

启动完成后访问：

- 前端：<http://127.0.0.1:5173>
- 后端接口文档：<http://127.0.0.1:8080/swagger-ui.html>

演示账号：

- 学生：`student` / `student123`
- 管理员：`admin` / `admin123`

不需要自动打开浏览器时：

```powershell
.\start-all.ps1 -NoBrowser
```

## 5. 分步启动

```powershell
.\start-databases.ps1
.\start-backend.ps1
.\start-frontend.ps1
```

`start-backend.ps1` 和 `start-frontend.ps1` 默认在前台显示日志。需要后台运行时增加 `-Background`：

```powershell
.\start-backend.ps1 -Background
.\start-frontend.ps1 -Background
```

## 6. 停止服务

停止全部服务：

```powershell
.\stop-all.ps1
```

只停止数据库：

```powershell
.\stop-databases.ps1
```

## 7. 环境验证

全部服务启动后执行：

```powershell
.\verify.ps1
```

验证内容包括：

- Java、Maven、Node.js、MySQL 和 Redis 版本。
- MySQL、Redis、后端和前端端口。
- 数据库至少包含 5 张业务表。
- 学生和管理员登录。
- 院校查询、推荐计算和收藏查询。
- 前端登录页能够正常响应。

任何一步失败时脚本会直接报错，不会把失败状态标记为通过。

本次配置的实际验证记录见 [VERIFICATION-REPORT.md](VERIFICATION-REPORT.md)。

## 8. 端口与配置

默认配置在 `dev-environment/.env`：

| 配置 | 默认值 |
| --- | --- |
| MySQL | `127.0.0.1:3307` |
| Redis | `127.0.0.1:6380` |
| Spring Boot | `127.0.0.1:8080` |
| Vue | `127.0.0.1:5173` |

端口与密码可修改 `.env`。修改后需重新执行 `start-all.ps1`。不要把真实生产密码提交到 Git。

## 9. 数据处理

- 首次初始化会自动创建数据库并导入演示数据。
- 再次运行 `setup-environment.ps1` 时，已存在的数据库不会被覆盖。
- MySQL 数据位于 `data/mysql/`，Redis 数据位于 `data/redis/`，可直接随项目目录迁移。
- 旧电脑上的 `data/` 建议先备份，再复制到新电脑。

## 10. 迁移到另一台电脑

### 方式一：复制整个项目文件夹

连同 `dev-environment/runtime/`、`downloads/`、`data/` 和 `cache/` 一起复制。进入新电脑的项目目录后执行：

```powershell
Set-Location .\dev-environment
.\start-all.ps1
```

这是离线迁移的首选方式。

### 方式二：从 Git 克隆

Git 仓库只保存脚本、版本清单和教程，不保存大型运行时。克隆后执行：

```powershell
Set-Location .\dev-environment
.\setup-environment.ps1
.\start-all.ps1
```

脚本会自动下载固定版本并完成配置。

## 11. 常见问题

### PowerShell 提示禁止运行脚本

执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

### 端口被占用

修改 `dev-environment/.env` 中的端口，或停止占用对应端口的程序。项目默认使用 MySQL `3307` 和 Redis `6380`，可减少与系统已有数据库冲突。

### MySQL 启动失败

查看：

```text
dev-environment/logs/mysql-error.log
dev-environment/logs/mysql-init.log
```

如果 `data/mysql/` 是其他 MySQL 版本的数据目录，请先备份并移走，不要直接混用。

### 后端启动失败

查看：

```text
dev-environment/logs/backend.err.log
dev-environment/logs/backend.out.log
```

同时确认 `start-databases.ps1` 已成功执行。

### 前端无法请求接口

确认后端已监听 `8080`，并查看 `dev-environment/logs/frontend.err.log`。如果修改了后端端口，确保 `.env` 中同步修改 `BACKEND_PORT`。

## 12. Git 与可移植性

运行时、数据库数据、日志、下载压缩包和 Maven 缓存体积较大，已排除在 Git 历史之外。必须提交以下内容：

- 所有 PowerShell 脚本。
- `.env.example`。
- `environment.psd1` 中的版本、下载地址和 SHA-256。
- 本教程和版本说明。

只需脚本和固定版本清单，就能在另一台同类 Windows 电脑上重建环境。
