## 任务：将 Logout 从 gateway-service 迁移到 system-service

调整认证职责：`system-service` 负责认证状态的创建、修改和销毁；`gateway-service` 仅负责 JWT + Redis Session 二次验证。

### 要求

1. 删除 `gateway-service` 中现有 logout 接口。

   * 删除 `/api/auth/logout`。
   * 清理仅被 logout 使用的 Controller、方法、依赖和无效代码。

2. 收缩 gateway 的 Session 能力。

   * gateway 只保留二次认证所需的 Session **读取/验证**能力。
   * 删除 gateway 中不再使用的 `save / delete / invalidate / updateTokenVersion` 等写操作。
   * 不影响现有 `AuthSessionValidator` 行为。

3. 在 `system-service` 实现 logout：

   * 提供 `POST /api/auth/logout`。
   * 从当前 `SecurityContext` 获取 `userId`。
   * 从 `Authentication.details` 的 `SessionIdentity` 获取 `sid`。
   * 使用 `AuthCacheKeys.session(userId, sid)` 删除：
     `auth:{userId}:session:{sid}`。
   * Redis Session 不存在时也按登出成功处理，保证接口幂等。
   * 不删除该用户的 `authority` 和 `function` 缓存。

4. 保持当前安全模型：

   * JWT 无需加入黑名单。
   * logout 后 JWT 即使未过期，gateway 因 Redis Session 不存在必须返回 401。
   * 继续使用 `platform-base` 中已有的 `AuthCacheKeys`、`SessionIdentity` 和内部认证机制，不重复实现。

5. 调整测试：

   * system-service 增加 logout 测试。
   * 验证正确删除当前 sid 对应的 Session。
   * 验证不会误删同一用户其他 sid。
   * 验证重复 logout 可正常成功。
   * 保证 gateway 现有 JWT + Redis 二次认证测试继续通过。

完成后执行 Maven 测试/编译，清理所有因迁移产生的无引用 logout/session 写操作代码，不做登录功能及其他无关重构。
