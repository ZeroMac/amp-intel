## 任务目标

在 `amp-intel` 当前项目基础上，完成 `services/gateway-service` 的第一阶段建设：

**让 gateway-service 成为一个可独立启动、可注册到 Nacos、并可通过服务发现转发请求到 `system-service` 的基础 Spring Cloud Gateway 服务。**

本阶段只完成网关基础能力，**不要实现 JWT、Redis 会话校验、RBAC 权限判断等认证授权逻辑。**

## 当前情况

`services/gateway-service/pom.xml` 已存在，并已经包含：

* Spring Cloud Gateway WebFlux
* Nacos Discovery
* Spring Security OAuth2 Resource Server

请基于现有项目结构继续开发，不重构 Maven 整体结构。

## 实现要求

### 1. 创建 gateway-service 启动代码

在合理的 Java package 下创建：

```text
GatewayServiceApplication.java
```

要求：

* 使用标准 Spring Boot Application。
* gateway-service 可以独立启动。
* 不引入 MVC 相关组件。
* 保持 Gateway 的 WebFlux/reactive 技术栈。

### 2. 配置 Nacos 服务发现

创建：

```text
services/gateway-service/src/main/resources/application.yml
```

至少配置：

```yaml
spring:
  application:
    name: gateway-service
```

Nacos 参数沿用项目现有约定：

* 地址通过 `NACOS_SERVER_ADDR` 环境变量覆盖。
* 用户名通过 `NACOS_USERNAME` 覆盖。
* 密码通过 `NACOS_PASSWORD` 覆盖。
* 默认值与 `system-service` 当前 Nacos 配置保持一致。

如果项目当前 Nacos 使用了 namespace/group 等约定，应优先与仓库现状保持一致，不自行创造第二套配置规则。

### 3. 配置 system-service 路由

增加一条明确的 Gateway Route：

```text
/api/system/**
        ↓
lb://system-service
```

路由后去除：

```text
/api/system
```

这一外部前缀。

例如：

```text
客户端请求：

GET /api/system/sys/func/xxx
```

实际转发给：

```text
system-service:

GET /sys/func/xxx
```

必须使用：

```text
lb://system-service
```

通过 Nacos 服务发现和 Spring Cloud LoadBalancer 找实例。

禁止写死：

```text
http://localhost:xxxx
```

### 4. 暂时关闭 Gateway Security 拦截

当前 `gateway-service` 已存在 OAuth2 Resource Server 依赖，但阶段 1 不实现认证。

因此必须保证阶段 1 下：

```text
/api/**
```

请求可以正常经过 Gateway 转发。

可以增加最小化 WebFlux Security 配置：

```text
authorizeExchange -> permitAll
csrf -> disable
```

但：

* 不删除现有 Resource Server 依赖。
* 不实现 JWT Decoder。
* 不实现 Bearer Token 校验。
* 不实现 Redis。
* 不实现权限判断。

这部分将在后续阶段实现。

### 5. 不修改 system-service 业务代码

除非为了验证现有接口确实存在，否则本任务原则上：

```text
只修改 services/gateway-service
```

不要修改：

```text
system-service
trade-service
platform-bom
platform-parent
```

如果现有 gateway-service 缺少**运行所必须**且目前 BOM 已经管理版本的依赖，可以修改它自己的：

```text
services/gateway-service/pom.xml
```

但禁止在 gateway-service 内自行声明第三方依赖版本。

遵守仓库 `Agents.md` 中 Maven 规则。

## 建议最终结构

大致形成：

```text
services/gateway-service/
├─ pom.xml
└─ src/
   └─ main/
      ├─ java/
      │  └─ com/hl/platform/gateway/
      │     ├─ GatewayServiceApplication.java
      │     └─ config/
      │        └─ SecurityConfig.java
      └─ resources/
         └─ application.yml
```

具体 package 请根据仓库已有命名习惯确定，不要为了完全匹配此示例而破坏现有规范。

## 验收条件

完成后执行：

```bash
mvnw.cmd -pl services/gateway-service -am clean verify
```

必须编译通过。

在 Nacos、gateway-service、system-service 均启动后，应满足：

```text
Nacos
├─ gateway-service
└─ system-service
```

均能看到注册实例。

并验证：

```text
直接调用：

http://localhost:<system-port>/sys/func/...
```

能够正常访问的接口，通过：

```text
http://localhost:<gateway-port>/api/system/sys/func/...
```

应得到等价响应。

同时确认：

```text
不存在 401/403
```

即 Gateway 当前不会因为 Spring Security 阻断请求。

## 边界

本任务不要提前实现：

* 登录接口
* JWT 签发
* JWT 验签
* Refresh Token
* Redis
* tokenVersion
* 服务端 Session 二次确认
* 用户加载
* 角色加载
* 权限缓存
* `@PreAuthorize`
* Gateway 全局权限过滤器
* 自定义认证协议
* 公共 security module

只完成：

```text
启动
  ↓
注册 Nacos
  ↓
Gateway 匹配 /api/system/**
  ↓
Nacos 服务发现
  ↓
lb://system-service
  ↓
请求成功转发
```

完成代码修改后，给出：

1. 修改/新增文件列表。
2. 路由规则说明。
3. Maven 验证结果。
4. 实际启动验证如果因本地缺少 Nacos/system-service 无法完成，要明确说明，不要伪造验证结果。
