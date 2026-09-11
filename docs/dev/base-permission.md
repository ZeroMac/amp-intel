调整 `amp-intel` 的微服务认证结构，将 `system-service` 中通用的 Spring Security 内部认证逻辑下沉到 `platform-base`。

目标：

* 将 `InternalAuthenticationFilter` 从 `system-service` 移入 `platform-base.security`。
* 将 `SessionIdentity` 一并作为公共安全模型移入 `platform-base`。
* 扩展现有 `PlatformSecurityAutoConfiguration`，统一完成：

  * `InternalAuthSigner`
  * `InternalAuthenticationFilter`
  * 无状态 `SecurityFilterChain`
  * 401 / 403 JSON 响应
  * `@EnableMethodSecurity`
    的配置。
* 使用 `security.internal.secret` 创建 `InternalAuthSigner`。
* 保持现有认证流程不变：

  * Gateway Header 签名校验
  * 根据 `userId` 从 `AuthorityCacheReader` / Redis 获取 Authority
  * 构造 Spring Security `Authentication`
  * 支持业务代码继续使用 `@PreAuthorize("hasAuthority('xxx')")`
* 删除 `system-service` 中重复的 `InternalAuthenticationFilter` 和通用 `SecurityConfig`；`system-service` 只保留业务安全声明。
* 自动配置必须允许业务服务通过自定义 Bean 覆盖默认实现，优先使用 `@ConditionalOnMissingBean`，避免公共库写死扩展点。
* 不改变现有 Redis Key、Gateway Header、签名算法及 Authority 数据结构。
* 遵守项目现有 Maven/BOM/parent 管理规则，不在子模块单独指定依赖版本。
* 完成后执行 Maven 编译/测试，确保 `platform-base` 与 `system-service` 均通过。

最终结构原则：

`Gateway 负责认证入口和可信 Header → platform-base 负责服务端身份恢复、验签、Authority 装载和 Spring Security 上下文 → 业务 Service 只通过 @PreAuthorize 声明权限。`
