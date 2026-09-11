Maven项目规则.
| POM                             | 回答的问题                     |
| ------------------------------- | ------------------------- |
| `/pom.xml`                      | **这个代码仓库有哪些模块？**          |
| `/platform-bom/pom.xml`         | **这些库用什么版本？**             |
| `/platform-parent/pom.xml`      | **Java 项目按什么规则编译、测试、打包？** |
| `/services/pom.xml`             | **目前有哪些微服务？**             |
| `/services/xxx-service/pom.xml` | **这个服务具体需要哪些能力？**         |


单一项目编译
.\mvnw.cmd -pl services/system-service compile

mybatis生成代码
$env:CODEGEN_DB_URL="jdbc:postgresql://localhost:5432/hldb01"
$env:CODEGEN_DB_USERNAME="hl_user"
$env:CODEGEN_DB_PASSWORD="hlpassword"

.\tools\codegen.ps1 system

## 权限声明（认证逻辑已下沉到 platform-base）

### 统一匿名接口规则

匿名接口固定使用 Gateway 外部路径 `/api/*/public/*`、Service 内部路径
`/*/public/*`。每个 `*` 只匹配一层，`public` 的位置固定，不使用 `**`。

例如业务服务中声明：

```java
@RestController
@RequestMapping("/system/public")
public class PublicController {
    @GetMapping("/test")
    public String test() {
        return "ok";
    }
}
```

Gateway 路由需将 `/api/system/public/test` 转发为 `/system/public/test`
（例如剥离 `/api` 前缀）。安全规则不负责创建路由或重写路径。

| 路径 | 是否属于匿名规则 |
| --- | --- |
| Gateway `/api/system/public/test` | 是 |
| Service `/system/public/test` | 是 |
| Gateway `/api/system/user/public/test` | 否 |
| Gateway `/api/system/public/test/other` | 否 |
| Service `/system/user/public/test` | 否 |
| Service `/system/public/test/other` | 否 |

匹配的请求在 Gateway 不解析 Bearer Token、不查询 Redis Session；转发时仍清除客户端
内部身份 Header。base 使用同一 Service 路径匹配器放行并跳过内部 Header、签名和 Redis
Authority 校验，不恢复用户身份。无需添加匿名注解或服务白名单配置。
方法上的 `@PreAuthorize` 仍然有效，匿名路径不会绕过方法权限声明。
如业务自定义 SecurityFilterChain，需要自行保留该匿名路径的 `permitAll()` 规则。

### 业务权限声明

Gateway 验证 JWT 和 Redis Session，向下游传递带签名的 userId、sid、tokenVersion。
platform-base 自动完成 Header 验签、Redis Authority 装载、Authentication / SecurityContext
建立，并开启 `@EnableMethodSecurity`。业务服务直接通过 `@PreAuthorize` 声明权限。

例如，现有功能项查询接口要求 `sys:func:read`：

```java
import org.springframework.security.access.prepost.PreAuthorize;

@GetMapping("/{parentId}/children")
@PreAuthorize("hasAuthority('sys:func:read')")
public List<FunctionVO> listChildren(@PathVariable Long parentId) {
    return functionService.listChildrenByParentId(parentId);
}
```

权限注解也可以放在 Service 的 public 方法上，通过 Spring Bean 代理调用时生效；
同一个类中通过 `this` 调用自身方法不会触发方法权限拦截。

`hasAuthority` 使用完整权限字符串精确匹配。例如，用户 `100` 的 Redis Key
`auth:100:authority` 内容为：

```json
["sys:func:read", "sys:func:add"]
```

该用户可以通过上述查询权限检查。权限缓存缺失或不包含对应权限时返回 403；
内部身份缺失或验签失败返回 401；权限缓存读取失败或内容损坏返回 503。

### 服务接入配置

Servlet 业务服务需要声明 `platform-base`、Web MVC、Spring Security 和 Redis 所需依赖，
版本遵循现有 BOM/parent 管理。platform-base 的 Servlet/Security 依赖为 optional，
业务服务仍需自行声明所需 Starter。

Gateway 与业务服务配置相同的内部签名密钥：

```yaml
security:
  internal:
    secret: ${INTERNAL_AUTH_SECRET}
```

`INTERNAL_AUTH_SECRET` 使用至少 32 字节的随机秘密，与 JWT 密钥分开管理。
业务服务使用标准 `spring.data.redis.*` 配置连接权限缓存所在的 Redis。

业务服务无需重复定义通用 `SecurityConfig`、认证 Filter 或 `@EnableMethodSecurity`。
如需定制，可通过同类型 Bean 覆盖默认的 `AuthorityCacheReader`、`InternalAuthSigner`、
`InternalAuthenticationFilter`、`AuthenticationEntryPoint`、`AccessDeniedHandler` 或
`SecurityFilterChain`。自定义安全链时，需要自行将公共认证 Filter 加入链中。

详细流程见 [权限验证链路说明](docs/dev/system-permission-chain.md)。
