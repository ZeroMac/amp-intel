## 任务目标

在当前 `amp-intel` 的 `gateway-service` 基础上实现第二阶段：

**Gateway 使用 Spring Security Resource Server 对外部请求携带的 JWT Bearer Token 做验签和基础认证。**

本阶段只解决：

```text
请求
  ↓
gateway-service
  ↓
是否公开接口？
  ├─ 是 → 放行
  └─ 否
      ↓
   JWT 是否存在
      ↓
   JWT 签名是否合法
      ↓
   JWT 是否过期
      ↓
   合法 → 转发
```

本阶段**不要实现 Redis 服务端二次确认和 RBAC 权限判断**。

## 当前代码状态

当前 Gateway 已经完成：

```text
/api/system/**
    ↓
lb://system-service
    ↓
StripPrefix=2
```

并已经存在：

```text
SecurityConfig
```

但当前配置为：

```java
anyExchange().permitAll()
```

同时 `gateway-service` 已经依赖：

```xml
spring-boot-starter-security-oauth2-resource-server
```

因此应直接基于 Spring Security WebFlux Resource Server 实现，不自行编写 JWT 解析框架。

---

## 1. 修改 SecurityConfig

把当前全放行改成：

```text
公开路径 → permitAll
其他请求 → authenticated
```

至少预留公开路径：

```text
/auth/**
/actuator/health
```

如果当前项目没有 `/auth/**`，仍可以作为后续登录接口的保留白名单。

不要把：

```text
/api/**
```

整体加入白名单。

开启：

```java
oauth2ResourceServer(...)
```

并配置 JWT 认证。

保持：

```java
csrf.disable()
```

因为 Gateway 是 REST API 网关。

---

## 2. JWT 签名方案

本项目阶段 2 先采用：

```text
HMAC SHA-256 / HS256
```

即 Gateway 和后续认证服务共享一个 JWT secret。

不要：

* 手写 JWT 解码器。
* 自己解析 Base64。
* 自己验证 `exp`。
* 引入 jjwt 等第二套 JWT 框架。

优先使用 Spring Security / Nimbus 已经提供的：

```text
ReactiveJwtDecoder
NimbusReactiveJwtDecoder
```

完成签名及标准 Claims 验证。

---

## 3. JWT Secret 配置

不要把真实密钥硬编码到 Java。

在：

```text
application.yml
```

增加类似：

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:}
```

生产代码禁止提供类似：

```text
123456
secret
amp-intel-secret
```

这样的默认密钥。

如果 `JWT_SECRET` 未提供，应让应用启动失败或明确报配置错误，而不是自动生成临时 Secret。

JWT secret 后续可以迁移到 Nacos 配置中心，本阶段先保持配置结构简单。

---

## 4. 配置 ReactiveJwtDecoder

创建合理的配置类，例如：

```text
config/JwtConfig.java
```

或者直接在现有 Security 配置中创建 Bean。

要求：

```java
@Bean
ReactiveJwtDecoder jwtDecoder(...)
```

使用 HS256。

注意 HMAC secret 长度必须满足 HS256 安全要求。

建议至少：

```text
256 bit
32 bytes
```

不要为了让短字符串工作而降低算法要求。

---

## 5. JWT Claims 约定

本阶段先确定最小 Claim 协议。

JWT 至少支持：

```json
{
  "sub": "12345",
  "sid": "xxxxxxxx",
  "ver": 1,
  "iat": 1780000000,
  "exp": 1780003600
}
```

含义：

```text
sub
    用户 ID

sid
    登录会话 ID

ver
    token/session version

iat
    JWT 签发时间

exp
    JWT 过期时间
```

其中：

```text
sid
ver
```

本阶段只定义并允许读取，**暂时不要查询 Redis 校验**。

Gateway 当前真正必须验证的是：

```text
JWT 签名
exp 等 Spring Security 标准时间约束
```

不要在 JWT 中加入完整角色列表或权限列表。

---

## 6. Bearer Token 输入方式

严格使用标准 HTTP Header：

```http
Authorization: Bearer <jwt>
```

不要支持：

```text
?token=
X-Token
Cookie token
```

等额外方式。

避免形成多套认证入口。

---

## 7. 未认证响应

未携带 Token 或 Token 无效时：

```text
HTTP 401
```

返回 JSON，而不是 Spring 默认 HTML。

响应结构保持简单，例如：

```json
{
  "code": 401,
  "message": "Unauthorized"
}
```

不要返回：

* Java Exception。
* Stack Trace。
* JWT 解析失败的内部细节。
* 签名算法信息。

例如 Token 过期也统一返回：

```text
401 Unauthorized
```

不要向客户端暴露：

```text
JWT expired at ...
signature verification failed ...
```

---

## 8. 403 响应

同时配置统一的：

```text
AccessDeniedHandler
```

返回：

```http
HTTP 403
```

例如：

```json
{
  "code": 403,
  "message": "Forbidden"
}
```

虽然阶段 2 尚未实现业务 RBAC，但现在把 Gateway Security 的 401 / 403 JSON 边界建立好。

---

## 9. 不要在 Gateway 做 RBAC

阶段 2 明确禁止出现类似：

```java
.hasAuthority("sys:func:read")
.hasRole(...)
```

Gateway 目前只做：

```text
authentication
```

不做：

```text
authorization
```

除了公开路径与“必须登录”的区分。

后续权限判断由内部服务 Spring Security 完成。

---

## 10. 不实现 Redis

本任务禁止：

```text
Redis
ReactiveRedisTemplate
session lookup
tokenVersion lookup
kick-out
logout
blacklist
```

虽然 JWT 已经包含：

```text
sid
ver
```

但阶段 2 不使用它们进行服务端验证。

这是阶段 3 的任务。

---

## 11. 不修改 system-service

本任务原则上只修改：

```text
services/gateway-service
```

不要修改：

```text
system-service
trade-service
platform-parent
platform-bom
```

除非发现当前 BOM 对 Spring Security 官方依赖管理存在真实缺失；不得为了方便而改 Maven 总体结构。

---

## 12. 测试

至少增加 Gateway Security 相关自动化测试。

覆盖：

### 场景 A：公开接口

```text
/auth/**
```

无 Token：

```text
可以通过 Security
```

不要求下游一定存在对应服务。

### 场景 B：受保护接口无 Token

```http
GET /api/system/...
```

应返回：

```text
401
```

### 场景 C：无效 JWT

```http
Authorization: Bearer invalid-token
```

应返回：

```text
401
```

### 场景 D：签名错误

使用另一 secret 签发 JWT：

```text
401
```

### 场景 E：过期 JWT

```text
401
```

### 场景 F：有效 JWT

使用正确 secret 签发、未过期 JWT：

```text
通过 Gateway Security 认证
```

测试不要依赖外部 Nacos 才能完成 JWT 验证逻辑。

---

## 13. Maven 验证

从仓库根目录执行：

```bash
mvnw.cmd -pl services/gateway-service -am clean verify
```

必须通过。

---

## 验收结果

最终链路应达到：

```text
无 JWT
   ↓
Gateway
   ↓
401


错误 JWT
   ↓
Gateway
   ↓
401


有效 JWT
   ↓
Gateway
   ↓
Nacos 服务发现
   ↓
system-service
```

而：

```text
/auth/**
```

继续允许匿名访问。

---

## 阶段边界

本次只做：

```text
Gateway JWT Authentication
```

不要提前实现：

* 登录接口。
* 用户名密码认证。
* JWT 签发服务。
* Refresh Token。
* Redis Session。
* JWT + Redis 二次确认。
* Logout。
* 踢人。
* Token blacklist。
* RBAC。
* 用户角色查询。
* 权限缓存。
* `@PreAuthorize`。
* system-service Security 改造。
* 公共 security module。

完成后给出：

1. 新增/修改文件列表。
2. JWT 配置方式。
3. Claims 协议说明。
4. 白名单说明。
5. 401/403 处理方式。
6. 自动测试结果。
7. `mvnw.cmd -pl services/gateway-service -am clean verify` 结果。

不要伪造无法实际执行的 Nacos 联调结果。
