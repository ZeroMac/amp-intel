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