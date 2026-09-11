# system-service 权限验证链路

服务端通用认证由 `platform-base.security.PlatformSecurityAutoConfiguration` 自动装配。
`InternalAuthenticationFilter` 和独立公共模型 `SessionIdentity` 位于 platform-base；
system-service 只保留业务接口的 `@PreAuthorize` 权限声明，无需自行配置 Filter 或 SecurityConfig。

默认提供 `InternalAuthSigner`、`AuthorityCacheReader`、`InternalAuthenticationFilter`、
`AuthenticationEntryPoint`、`AccessDeniedHandler` 和无状态 `SecurityFilterChain`。
业务服务可声明同类型 Bean 覆盖默认实现；自定义 SecurityFilterChain 时由业务配置负责
选择是否加入公共 Filter。Filter 的 Servlet 容器自动注册已禁用，仅在 Security 链中执行。
方法安全和 Filter/安全链配置仅对 Servlet Web 应用生效，Gateway 的 WebFlux 安全链保持独立。
新增 Servlet/Security 编译依赖均为 optional，消费服务仍需声明自身所需的 Starter。

Gateway 验证 JWT 和 Redis session 后，从已认证 JWT 的 `sub/sid/ver` 写入
`X-Auth-User-Id`、`X-Auth-Sid`、`X-Auth-Token-Version`。转发前删除客户端所有
`X-Auth-*` Header（不区分大小写），不传 authority 列表。

两个服务必须配置相同的 `INTERNAL_AUTH_SECRET`（或 Nacos 中的
`security.internal.secret`），使用独立于 JWT 密钥的至少 32 字节随机秘密。
无默认密钥，缺失或过短时启动失败。不要将真实密钥提交到仓库或下发给客户端。

platform-base 的 `InternalAuthSigner` 统一签名协议：HMAC-SHA256 对版本前缀、
userId、sid、tokenVersion、时间戳按换行分隔签名，签名以 Base64URL 编码。
Gateway 增加 `X-Auth-Timestamp`、`X-Auth-Signature`；system-service 只接受单值身份
Header，校验签名和时间窗口（60 秒，允许未来时钟偏差 5 秒），然后读取
`AuthorityCacheReader.getAuthorities(userId)`。两端应保持时钟同步。

Redis 沿用 `auth:{userId}:authority`，例如 `auth:100:authority` 的内容为
`["sys:func:read"]`。缓存缺失或空列表视为无权限，不回源数据库。
权限转换为 `SimpleGrantedAuthority`，userId 为 Authentication principal，
sid/tokenVersion 为 details。SecurityContext 仅在当前请求有效，不创建 HTTP session，
请求结束后清理。权限由 `@EnableMethodSecurity` 执行。

`GET /sys/functions/{parentId}/children` 要求 `sys:func:read`：

| 场景 | 结果 |
| --- | --- |
| 身份验签成功且 Redis 含查询权限 | 200 |
| 缺少、重复、伪造、篡改或过期内部认证 Header | 401 |
| 身份有效但权限缺失 | 403 |
| 权限 Redis 不可用或缓存内容损坏 | 503，拒绝调用业务方法 |

内部签名只用于 Gateway 到服务的可信传输；部署时使用 TLS 和内部网络访问控制，
禁止向客户端暴露这些 Header。签名在时间窗口内可以被重放，因此它不替代传输加密；
system-service 不重复查询 Gateway 已校验的 session。

未新增或修改 login / refresh / logout。自动化测试使用 Mock Redis 操作和会话服务，
不依赖本机 Redis、PostgreSQL 或 Nacos；覆盖 Gateway Security 链、身份转发、Servlet
Security 链、真实 Redis authority 反序列化和方法权限拦截。

验证（JDK 21）：`./mvnw clean verify`，Windows 使用 `mvnw.cmd clean verify`。
