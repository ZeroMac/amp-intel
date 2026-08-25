## 任务目标

在当前 `amp-intel` 的 `gateway-service` JWT 认证基础上，引入 **Spring Data Redis Reactive**，建立服务端认证缓存体系。

Redis 统一按用户聚合：

```text
auth:{userId}:session:{sid}
auth:{userId}:authority
auth:{userId}:function
```

其中：

```text
session
    当前登录 Session 对应的用户身份快照

authority
    Spring Security 使用的权限字符串集合

function
    当前用户可访问的功能项完整对象集合
```

本阶段 Gateway **只使用 session 做 JWT 服务端二次确认**。

`authority` 和 `function` 本阶段完成缓存结构及基础读写能力，但不要在 Gateway 中进行业务权限判断。

---

## 一、JWT 结构

继续沿用：

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
sub = userId
sid = sessionId
ver = tokenVersion
```

不要把用户信息、角色、authority、function 塞入 JWT。

---

## 二、增加 Reactive Redis

在：

```text
services/gateway-service/pom.xml
```

增加：

```text
spring-boot-starter-data-redis-reactive
```

要求：

* 使用 Spring Boot 管理版本。
* 使用 Reactive Redis。
* 禁止 `RedisTemplate`。
* 禁止 `block()` / `subscribe()`。
* 禁止 Jedis / Redisson。
* 不手工管理 Lettuce 版本。

---

## 三、Redis 配置

本地 Redis：

```text
host: localhost
port: 6379
password: hlredispassword
```

使用标准配置：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:hlredispassword}
```

不要在 Java 代码中硬编码连接信息。

---

# 四、Redis Key 结构

统一使用：

```text
auth:{userId}:session:{sid}
auth:{userId}:authority
auth:{userId}:function
```

例如用户：

```text
userId = 10001
sid = abc-def
```

Redis 中：

```text
auth:10001:session:abc-def
auth:10001:authority
auth:10001:function
```

这样使用 Redis 查询工具执行：

```text
auth:10001:*
```

可以查看当前账号相关的全部认证与权限缓存。

不要再使用：

```text
auth:session:{sid}
auth:user:{userId}
auth:permission:{userId}
```

---

# 五、Session 用户快照

Key：

```text
auth:{userId}:session:{sid}
```

Value 使用 JSON。

建议最少：

```json
{
  "userId": "10001",
  "userName": "admin", 
  "tokenVersion": 1,
  "status": "ACTIVE"
}
```

模型建议：

```text
AuthSession
```

至少包含：

```java
String userId;
String userName; 
long tokenVersion;
String status;
```

可以根据当前用户模型增加少量核心身份字段，但不要缓存大量业务数据。

这里的 Session 同时承担：

```text
登录会话状态
+
当前 Session 用户身份快照
```

因此不再单独创建：

```text
auth:{userId}:user
```

---

# 六、Authority 缓存

Key：

```text
auth:{userId}:authority
```

Value 为 JSON 字符串数组：

```json
[
  "sys:func:read",
  "sys:func:add",
  "sys:func:update",
  "sys:user:read"
]
```

这些字符串未来直接对应：

```java
@PreAuthorize("hasAuthority('sys:func:read')")
```

本阶段要求建立明确的 Reactive 读写接口，但 **Gateway 暂时不读取 authority 做权限判断**。

建议接口类似：

```java
Mono<List<String>> getAuthorities(String userId);
Mono<Void> saveAuthorities(String userId, List<String> authorities, Duration ttl);
Mono<Boolean> deleteAuthorities(String userId);
```

---

# 七、Function 缓存

Key：

```text
auth:{userId}:function
```

Value 为 JSON 对象数组。

例如：

```json
[
  {
    "id": 1001,
    "code": "sys:func:read",
    "name": "功能查询",
    "path": "/system/func"
  }
]
```

具体字段以当前 `system-service` 的功能项模型为依据，不要另外创造完全独立的数据模型。

Function 缓存用于：

```text
菜单
功能树
前端可访问功能项
功能元数据
```

它与：

```text
authority
```

用途不同。

`authority` 是安全判断用的字符串 Token。

`function` 是完整功能对象。

本阶段只建立缓存模型及基础 Reactive 读写能力，不在 Gateway 中消费。

---

# 八、Session Key 查找规则

Gateway 已经可以从 JWT 获得：

```text
sub = userId
sid
```

所以 Redis Session Key 直接拼成：

```text
auth:{sub}:session:{sid}
```

例如：

```text
JWT.sub = 10001
JWT.sid = abc-def
```

查询：

```text
auth:10001:session:abc-def
```

不需要额外维护：

```text
sid -> userId
```

反向索引。

---

# 九、Gateway 服务端二次确认

JWT 首先继续由 Spring Security：

```text
ReactiveJwtDecoder
```

进行：

```text
签名验证
exp 验证
```

JWT 合法后：

```text
读取 sub
读取 sid
读取 ver
       ↓
拼 Redis Key
auth:{sub}:session:{sid}
       ↓
读取 AuthSession
       ↓
session.userId == JWT.sub
       ↓
session.tokenVersion == JWT.ver
       ↓
session.status == ACTIVE
       ↓
认证通过
```

不要重新解析 JWT。

---

# 十、认证失败规则

以下情况统一认证失败：

```text
JWT 缺少 sub
JWT 缺少 sid
JWT 缺少 ver

Session Key 不存在

session.userId != sub

session.tokenVersion != ver

session.status != ACTIVE

Session JSON 损坏
```

返回：

```text
401 Unauthorized
```

---

# 十一、Redis 故障

必须：

```text
fail closed
```

禁止：

```text
Redis 连接失败
    ↓
跳过 Session 校验
    ↓
仅凭 JWT 放行
```

建议区分：

```text
Session 不存在 / Session 无效
→ 401

Redis 服务不可用
→ 503
```

但无论具体状态码如何，Redis 故障时绝不能放行请求。

---

# 十二、Session TTL

Session 必须设置 TTL。

```text
auth:{userId}:session:{sid}
```

不能永久存在。

原则：

```text
Session TTL >= JWT 剩余有效时间
```

Service 创建 Session 必须显式传入：

```java
Duration ttl
```

---

# 十三、Authority / Function TTL

以下缓存也必须有 TTL：

```text
auth:{userId}:authority
auth:{userId}:function
```

但它们属于用户级缓存，不必和某一个 sid 完全绑定。

本阶段可以使用统一可配置 TTL。

不要创建永久权限缓存。

---

# 十四、Reactive Cache Service

可以按职责拆分，例如：

```text
security/
  AuthSession.java
  AuthSessionService.java

cache/
  AuthorityCacheService.java
  FunctionCacheService.java
```

或者采用当前工程更合适的结构。

Session API 至少包括：

```java
Mono<AuthSession> get(String userId, String sid);

Mono<Void> save(
    String userId,
    String sid,
    AuthSession session,
    Duration ttl
);

Mono<Boolean> delete(String userId, String sid);

Mono<Boolean> invalidate(String userId, String sid);
```

同时提供 tokenVersion 修改能力：

```java
updateTokenVersion(userId, sid, newVersion)
```

或：

```java
incrementTokenVersion(userId, sid)
```

整个实现保持 Reactive。

---

# 十五、Logout

实现：

```http
POST /auth/logout
Authorization: Bearer <jwt>
```

当前 `/auth/**` 不能再全部匿名。

调整为明确白名单，例如：

```text
/auth/login
/auth/refresh
```

允许匿名。

而：

```text
/auth/logout
```

必须 authenticated。

Logout：

```text
JWT.sub
JWT.sid
   ↓
DELETE
auth:{sub}:session:{sid}
```

只删除当前 Session。

不要删除：

```text
auth:{userId}:authority
auth:{userId}:function
```

因为这些属于用户级缓存，其他 Session 仍可能使用。

Logout 必须幂等。

---

# 十六、Logout 即时失效

必须满足：

```text
JWT 合法
Session 存在
     ↓
访问成功

POST /auth/logout
     ↓
删除
auth:{userId}:session:{sid}

原 JWT 再请求
     ↓
401
```

---

# 十七、tokenVersion

JWT：

```text
ver
```

必须等于：

```text
AuthSession.tokenVersion
```

例如：

```text
JWT.ver = 1
Redis.ver = 1
→ 有效
```

修改：

```text
Redis.ver = 2
```

后：

```text
原 JWT
→ 401
```

用于实现旧 Token 强制立即失效。

---

# 十八、强制失效基础能力

提供底层：

```text
invalidate(userId, sid)
```

可以直接删除：

```text
auth:{userId}:session:{sid}
```

以及 tokenVersion 更新能力。

本阶段不要开放管理员公网踢人接口。

---

# 十九、多 Session

当前结构天然允许一个用户存在多个 Session：

```text
auth:10001:session:sid-a
auth:10001:session:sid-b
auth:10001:session:sid-c
```

因此暂时不需要额外：

```text
auth:user:sessions:{userId}
```

索引。

也不要实现：

```text
logout all
```

后续确有需求再增加。

---

# 二十、权限缓存边界

本阶段允许建立：

```text
auth:{userId}:authority
auth:{userId}:function
```

以及基础读写 Service。

但是 **不要提前实现 RBAC 执行逻辑**。

禁止 Gateway 出现：

```java
hasAuthority(...)
hasRole(...)
```

Gateway 当前职责仍然只有：

```text
JWT Authentication
+
Redis Session Validation
```

未来：

```text
system-service
```

再负责把：

```text
auth:{userId}:authority
```

转换成：

```text
GrantedAuthority
```

供：

```java
@PreAuthorize("hasAuthority('sys:func:read')")
```

使用。

---

# 二十一、缓存失效预留

Authority 或 Function 发生变化时，未来可以：

```text
DEL auth:{userId}:authority
DEL auth:{userId}:function
```

然后重新加载。

本阶段只需要保证 Service 层具备：

```text
get
save
delete
```

基础能力。

不要提前实现复杂事件总线或缓存同步机制。

---

# 二十二、测试

至少覆盖：

### Session 正常

```text
JWT:
sub=100
sid=session-a
ver=1

Redis:
auth:100:session:session-a

{
  "userId":"100",
  "tokenVersion":1,
  "status":"ACTIVE"
}
```

认证成功。

### Session 不存在

```text
auth:100:session:session-a
```

不存在：

```text
401
```

### userId 不一致

```text
JWT.sub=100
Session.userId=200
```

```text
401
```

### tokenVersion 不一致

```text
JWT.ver=1
Session.tokenVersion=2
```

```text
401
```

### Session 非 ACTIVE

```text
status=DISABLED
```

```text
401
```

### JWT 缺少 sid / ver / sub

```text
401
```

### Logout

删除：

```text
auth:100:session:session-a
```

原 JWT 再次访问：

```text
401
```

### tokenVersion 强制失效

修改 Redis tokenVersion 后：

```text
旧 JWT → 401
```

### Authority Cache

验证：

```text
auth:100:authority
```

能够正确保存/读取字符串数组。

### Function Cache

验证：

```text
auth:100:function
```

能够正确保存/读取 JSON 对象数组。

### Redis 不可用

验证：

```text
fail closed
```

绝不放行。

---

# 二十三、测试环境

普通：

```powershell
.\mvnw.cmd -pl services/gateway-service -am clean verify
```

不能强依赖开发机：

```text
localhost:6379
```

存在。

单元测试优先 Mock Reactive Redis / Cache Service。

本地 Redis 仅用于人工联调：

```text
localhost:6379
password=hlredispassword
```

---

# 最终 Redis 结构

阶段 3 最终统一为：

```text
auth:{userId}
│
├── session:{sid}
│     当前 Session 用户身份快照
│     tokenVersion
│     session status
│
├── authority
│     ["sys:func:read", ...]
│
└── function
      [{功能项对象}, ...]
```

例如：

```text
auth:10001:session:7ed7...
auth:10001:session:943a...
auth:10001:authority
auth:10001:function
```

使用 Redis 工具：

```text
auth:10001:*
```

即可查看该用户全部相关认证缓存。

---

# 明确禁止提前实现

本阶段不要实现：

* 正式 Login。
* 用户名密码认证。
* JWT 签发。
* Refresh Token。
* 数据库用户认证。
* RBAC 判断。
* `@PreAuthorize`。
* system-service Security 改造。
* Gateway 权限判断。
* 管理员踢人公网 API。
* logout all。
* Redis Session Set/ZSet 索引。
* 公共 security module。

本阶段完成：

```text
JWT
 +
auth:{userId}:session:{sid}
服务端二次确认
 +
logout / tokenVersion 强制失效
 +
authority / function 缓存结构及基础读写能力
```

完成后报告：

1. 新增/修改文件。
2. Redis Key 结构。
3. Session JSON 结构。
4. Authority JSON 结构。
5. Function JSON 结构。
6. JWT `sub/sid/ver` 二次确认流程。
7. Logout 与 tokenVersion 失效结果。
8. Redis 故障处理策略。
9. 自动化测试结果。
10. Maven verify 结果。
