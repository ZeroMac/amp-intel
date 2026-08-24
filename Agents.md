# 项目说明

`amp-intel` 是一个 Maven 管理的 Java 微服务项目。所有服务位于 `services/` 下，并直接继承 `platform-parent`。

# Maven 规则

| POM | 职责 |
| --- | --- |
| `/pom.xml` | 聚合整个仓库的模块 |
| `/platform-bom/pom.xml` | 统一管理依赖版本 |
| `/platform-parent/pom.xml` | 统一管理编译、测试、打包和插件规则 |
| `/services/pom.xml` | 聚合所有微服务 |
| `/services/*/pom.xml` | 声明该服务实际需要的依赖 |

不要在业务服务中重复定义平台统一版本或编译规则。

# 编译

统一使用 Maven Wrapper，不依赖开发机安装的 Maven 版本。

```bash
# Linux / macOS
./mvnw clean verify

# Windows
mvnw.cmd clean verify
```

构建单个服务时，从仓库根目录执行：

```bash
./mvnw -pl services/<service-name> -am clean verify
```

## Service Structure

采用传统 MVC 分层：

`Controller -> Service -> MyBatis-Flex Mapper -> PostgreSQL`

目录约定：

```text
service-name/
└─ src/main/java/...
   ├─ controller/
   ├─ service/
   │  └─ impl/
   ├─ mapper/
   ├─ entity/
   ├─ dto/
   ├─ vo/
   ├─ config/
   └─ *Application.java
```

约束：

* Controller：处理 HTTP 请求和响应，不写业务逻辑。
* Service：业务逻辑、事务、跨 Mapper 组合。
* Mapper：继承 MyBatis-Flex `BaseMapper<T>`，负责数据库访问。
* Entity：数据库表映射。
* DTO：接口输入模型。
* VO：接口输出模型。
* 不额外增加 Repository/DAO 层，避免无意义封装。
* 复杂 SQL 放 Mapper 或对应 XML。

