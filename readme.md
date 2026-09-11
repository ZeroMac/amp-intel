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
