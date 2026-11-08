# VS Code 项目配置与启动指南

项目已经附带共享的 VS Code 配置。使用前请直接打开项目根目录：

```text
C:\Users\Administrator\Desktop\Thesis
```

不要只打开后端或前端子目录，否则任务中的相对路径和便携环境变量无法正确解析。

## 1. 安装推荐插件

VS Code 打开项目后，右下角会提示安装推荐插件。也可以打开扩展面板搜索：

- Language Support for Java by Red Hat
- Debugger for Java
- Maven for Java
- Project Manager for Java
- Vue - Official
- PowerShell

这些插件用于 Java 代码补全、断点调试、Maven 项目和 Vue 语法高亮。为避免 Java Test 插件的 ASM 依赖冲突，不推荐直接安装 Extension Pack for Java。

## 2. 首次配置

按 `Ctrl+Shift+P`，选择“Tasks: Run Task”，运行：

```text
环境：首次配置
```

也可以在终端执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\dev-environment\setup-environment.ps1
```

首次配置会安装或检查固定版本运行时、初始化数据库、导入演示数据、安装前端依赖并测试后端。

## 3. 一键启动

按 `Ctrl+Shift+B`，默认执行：

```text
项目：一键启动
```

启动完成后：

- 前端：http://127.0.0.1:5173
- 后端接口文档：http://127.0.0.1:8080/swagger-ui.html
- 学生账号：`student` / `student123`
- 管理员账号：`admin` / `admin123`

也可以使用任务面板运行“项目：一键启动”，或在终端执行：

```powershell
.\dev-environment\start-all.ps1
```

“项目：一键启动”会同时启动前端。只启动后端并不等于网页已经可以访问。

## 4. 停止和检查

停止全部服务：

```text
项目：停止全部
```

检查版本、端口、数据库、登录、院校查询、推荐流程和前端页面：

```text
项目：完整验证
```

注意：验证任务只检查服务，不会自动启动服务。

## 5. 分步开发

如果正在修改后端或前端，建议使用前台启动任务查看日志：

1. 运行“数据库：启动”。
2. 运行“后端：启动（前台）”。
3. 打开另一个终端，运行“前端：启动（前台）”。

VS Code 终端已经自动使用：

- `dev-environment/runtime/jdk-17`
- `dev-environment/runtime/maven-3.9.9`
- `dev-environment/runtime/node-20.18.1`

不需要手工修改系统 `PATH`。

## 6. 后端断点调试

首次使用 Java 调试前，需要让 Java 插件识别便携 JDK：

1. 按 `Ctrl+Shift+P`。
2. 执行“Java: Configure Java Runtime”。
3. 选择或添加本机目录：

```text
C:\Users\Administrator\Desktop\Thesis\dev-environment\runtime\jdk-17
```

4. 将它设为 Java 17 默认运行时。

随后：

1. 先运行“项目：停止全部”，避免 8080 端口冲突。
2. 打开“运行和调试”。
3. 选择“后端：调试 Spring Boot”。
4. 在 Java 代码中设置断点并按 `F5`。

调试配置会依次停止旧服务、启动 MySQL 和 Redis、启动后台前端，然后使用调试模式启动 Spring Boot。`F5` 完成后，后端和网页应同时可用。

如果暂时不需要断点，直接使用“项目：一键启动”更简单。

## 7. 常用任务

| 任务名称 | 作用 |
| --- | --- |
| 环境：首次配置 | 下载运行时、初始化数据库、安装依赖 |
| 项目：一键启动 | 启动数据库、后端和前端 |
| 项目：停止全部 | 关闭全部服务 |
| 项目：完整验证 | 验证版本、端口和主流程 |
| 数据库：启动 | 只启动 MySQL 和 Redis |
| 前端：启动（后台） | 后台启动 Vite，可用于网页访问 |
| 调试：准备前后端 | 停止旧服务，准备数据库和前端 |
| 后端：启动（前台） | 前台运行 Spring Boot，便于查看日志 |
| 前端：启动（前台） | 前台运行 Vite |
| 后端：编译并测试 | 使用便携 Maven 环境编译并测试 |

## 8. 常见问题

### 终端提示禁止运行脚本

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

只影响当前 VS Code 终端。

### 任务看不到

确认打开的是项目根目录，并且 `.vscode/tasks.json` 存在。然后执行“Tasks: Run Task”刷新任务列表。

### 后端调试提示端口占用

已有后台后端正在运行。先执行“项目：停止全部”，再按 `F5`。

### 后端调试成功但网页打不开

确认前端是否正在运行：

1. 打开任务面板，运行“前端：启动（后台）”。
2. 在浏览器访问 http://127.0.0.1:5173。
3. 如果仍无法访问，运行“项目：停止全部”，再按 `F5` 使用“后端：调试 Spring Boot”。

当前调试配置会自动启动前端，因此正常情况下不需要再手工运行前端。

### Java 插件仍要求选择 JDK

执行“Java: Configure Java Runtime”，选择 `dev-environment/runtime/jdk-17`。

### Java 项目导入失败或调试器无法启动

如果 Java 扩展日志出现：

```text
A bundle is already installed with the name "org.objectweb.asm"
```

说明 Java Test 插件与 Java 语言服务器的 ASM 依赖重复。请卸载 `Test Runner for Java` 和不再需要的 `Extension Pack for Java`，保留以下扩展：

- `redhat.java`
- `vscjava.vscode-java-debug`
- `vscjava.vscode-java-dependency`
- `vscjava.vscode-maven`

然后执行“Developer: Reload Window”，再运行“后端：调试 Spring Boot”。

## 9. 可移植性

工作区配置使用 `${workspaceFolder}` 定位便携工具，不写死用户目录。把整个项目移动到其他路径后，VS Code 任务和集成终端会自动切换到新路径。

从 Git 克隆到新电脑时，先运行“环境：首次配置”，再运行“项目：一键启动”。
