目标：为一个全新的 Java 21 / Spring Cloud 微服务项目创建 Maven 基础工程骨架。

要求：

1. 项目根目录名称为：

```text
amp-intel
```

2. 使用 Maven Wrapper，并固定 Maven 版本。

要求生成并纳入版本控制：

```text
.mvn/
mvnw
mvnw.cmd
```

优先使用当前稳定的 Maven 3.9.x 版本。

Wrapper 必须继续使用开发者本机的 Maven 用户配置和本地仓库.

不要创建项目私有 Maven repository，不要修改用户现有的 Maven settings。

3. 创建以下 Maven 项目结构：

```text
amp-intel/
├── pom.xml
├── .mvn/
├── mvnw
├── mvnw.cmd
│
├── platform-bom/
│   └── pom.xml
│
├── platform-parent/
│   └── pom.xml
│
└── services/
    ├── pom.xml
    │
    ├── gateway-service/
    │   └── pom.xml
    │
    ├── user-service/
    │   └── pom.xml
    │
    └── trade-service/
        └── pom.xml
```

本任务暂时不要创建 Java 源代码、Spring Boot Application、resources、Docker、数据库脚本或其他业务目录。

4. Maven POM 职责必须严格区分。

根 `pom.xml`：

* packaging 为 `pom`
* 仅作为整个仓库的 Maven reactor / aggregator
* 聚合：

  * `platform-bom`
  * `platform-parent`
  * `services`
* 不在这里定义具体业务依赖
* 不把根 POM 作为业务模块的 parent

`platform-bom/pom.xml`：

* packaging 为 `pom`
* 专门用于 `dependencyManagement`
* 后续负责统一管理第三方依赖版本
* 当前可以保持最小结构
* 不添加业务依赖
* 不创建 Java 源码

`platform-parent/pom.xml`：

* packaging 为 `pom`
* 作为所有 Java 服务模块的统一 Maven parent
* Java 版本统一定义为 21
* UTF-8
* 后续用于 Maven pluginManagement、Enforcer、Compiler、Surefire 等构建规范
* 当前只放建立 parent 所必须的基础配置，不要过度添加插件
* 不继承 `spring-boot-starter-parent`

`services/pom.xml`：

* packaging 为 `pom`
* 聚合所有可独立运行和部署的服务
* 当前包含：

  * `gateway-service`
  * `user-service`
  * `trade-service`

各具体服务 POM：

* `gateway-service`
* `user-service`
* `trade-service`

均继承：

```text
platform-parent
```

当前只创建合法、最小化的 Maven POM，不添加 Spring Boot、Spring Cloud 或业务依赖。

5. 坐标先统一使用：

```text
groupId: com.hl.platform
version: 0.1.0-SNAPSHOT
```

artifactId 分别为：

```text
amp-intel
platform-bom
platform-parent
services
gateway-service
user-service
trade-service
```

6. Maven Wrapper 和整个 reactor 必须能够正常工作。

完成后执行并确认：

```powershell
.\mvnw.cmd -version
```

以及：

```powershell
.\mvnw.cmd clean verify
```

Linux/macOS 结构也必须支持：

```bash
./mvnw clean verify
```

7. 不要做以下事情：

* 不生成业务 Java 代码
* 不引入 Spring Boot 依赖
* 不引入 Spring Cloud / Spring Cloud Alibaba 依赖
* 不引入 PostgreSQL
* 不创建 common 模块
* 不创建 api 模块
* 不创建 Docker 配置
* 不创建 CI/CD
* 不创建 README 之外的大量文档
* 不自行扩展当前目录结构
* 不修改本机 Maven settings.xml
* 不修改本机 Maven localRepository

8. 完成后输出简要结果，包括：

* 实际创建的目录树
* Maven Wrapper 固定的 Maven 版本
* 各 POM 的 parent / module 关系
* `mvnw clean verify` 是否成功

原则：保持结构最小、清晰、可运行，不做超出任务范围的架构设计。
