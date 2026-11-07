# 环境版本与来源

所有压缩包下载后均计算 SHA-256，完整清单保存在 `environment.psd1`。下载日期为 2026-11-07。

| 组件 | 版本 | 下载来源 | 许可证 |
| --- | --- | --- | --- |
| Eclipse Temurin JDK | 17.0.21+9 | [Adoptium Temurin 17 发布页](https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.21%2B9) | GPLv2 with Classpath Exception |
| Apache Maven | 3.9.9 | [Maven Central](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/) | Apache License 2.0 |
| Node.js | 20.18.1 | [Node.js 官方发布目录](https://nodejs.org/dist/v20.18.1/) | MIT 及第三方许可证 |
| MySQL Community Server | 8.0.34 | [MySQL 官方归档](https://cdn.mysql.com/archives/mysql-8.0/mysql-8.0.34-winx64.zip) | GPLv2 或商业许可 |
| Redis for Windows | 5.0.14.1 | [tporadowski/redis 发布页](https://github.com/tporadowski/redis/releases/tag/v5.0.14.1) | BSD 3-Clause |

## SHA-256

```text
jdk17.zip                  98518ecf554465057ee5509462631deeee374bc368c12a411d225cd29381f84b
maven.zip                  4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c
node.zip                   56e5aacdeee7168871721b75819ccacf2367de8761b78eaceacdecd41e04ca03
mysql-8.0.34-winx64.zip    d0480039bceef97a558027bfb868b77be1717ec1097410c697db383315243895
Redis-x64-5.0.14.1.zip     018ea18a35876383cbb5f4cd0258adfc87747cf9d619bce1cf73a2e36f720ccf
```

## 注意

- Redis for Windows 不是 Redis 官方原生支持的 Windows 服务版本，仅用于本地开发与毕业设计演示。
- MySQL 和 JDK 的许可证条款应在公开分发或商业使用前再次核对。
- 下载地址若失效，应按相同版本从可信镜像获取，并更新清单中的地址和校验值。
