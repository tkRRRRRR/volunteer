# 便携环境验证记录

验证日期：2026-11-07

## 运行版本

| 组件 | 实际版本 |
| --- | --- |
| JDK | OpenJDK 17.0.21 |
| Maven | 3.9.9 |
| Node.js | 20.18.1 |
| npm | 10.8.2 |
| MySQL | 8.0.34 |
| Redis for Windows | 5.0.14.1 |

## 服务端口

| 服务 | 地址 | 结果 |
| --- | --- | --- |
| MySQL | 127.0.0.1:3307 | 通过 |
| Redis | 127.0.0.1:6380 | 通过 |
| Spring Boot | 127.0.0.1:8080 | 通过 |
| Vue | 127.0.0.1:5173 | 通过 |

## 功能验证

- 数据库包含 `sys_user`、`university`、`major`、`admission_record`、`favorite` 共 5 张表。
- 演示数据包含 6 所虚构院校和 36 条录取记录。
- 学生账号 `student` 登录成功。
- 管理员账号 `admin` 登录成功。
- 院校查询返回 5 条分页记录。
- 冲稳保接口返回 12 条辅助匹配结果。
- 收藏接口可正常访问。
- 前端登录页正常加载。
- 浏览器实际登录后成功进入工作台，未出现页面脚本错误。
- `stop-all.ps1` 可以停止全部服务。
- 停止后重新执行 `start-all.ps1`，MySQL、Redis、后端、前端和完整验证再次通过。

## 构建验证

- PowerShell 5.1 和 PowerShell 7 均通过脚本语法检查。
- Maven 使用 `dev-environment/cache/maven-repository` 编译和测试后端。
- 前端使用便携 Node.js 构建成功。
- 当前项目路径包含中文目录 `02_项目代码`，环境脚本和 JAR 启动方式已验证兼容。
