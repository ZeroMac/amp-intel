# API 路由与命名约定

## 1. URL 风格

采用 REST 风格，不使用 `.do`、`.action` 等动态请求后缀，不使用 PascalCase 动作名。

```text
推荐：
GET /sys/functions/{parentId}/children

避免：
GET /sys/func/GetByPId.do
```

URL 使用小写，资源名优先使用复数名词；HTTP Method 表达操作语义。

## 2. 外部 API 与服务内部路由

所有对外请求统一使用 `/api/**` 作为入口前缀：

```text
/api/sys/**
/api/trade/**
```

`/api` 只属于 Nginx / Gateway 层，不写入微服务 Controller。

示例：

```text
外部：
GET /api/sys/functions/100/children

Gateway 转发后：
GET /sys/functions/100/children
```

Nginx 可通过 `/api/**` 明确区分动态请求与前端静态资源。

## 3. 服务间调用

微服务之间通过 Nacos 服务发现直接调用目标服务，不经过外部 `/api` 前缀。

```text
http://system-service/sys/functions/100/children
```

推荐使用 OpenFeign 等方式按服务名调用。

## 4. 内部专用接口

仅允许服务间调用、禁止外部暴露的接口统一使用：

```text
/internal/**
```

例如：

```text
/internal/auth/check-permission
/internal/users/{userId}/permissions
```

Gateway 不为 `/internal/**` 配置外部转发规则。

## 5. 路由层次

```text
/api/**          外部入口，仅存在于 Nginx / Gateway

/sys/**
/trade/**
/xxx/**          微服务业务接口

/internal/**     服务间专用接口
```

## 6. Java 命名

Java 使用标准 lowerCamelCase：

```text
类：
FunctionController
FunctionService
FunctionServiceImpl

方法：
getChildren
listChildrenByParentId

字段：
funcId
parentId
```

避免：

```text
GetByPId
ParentId
getByParentid
```

集合查询方法优先使用 `listXxx`，单对象查询使用 `getXxx`。

## 7. Controller 返回

Controller 不直接暴露数据库 Entity，优先返回 VO / DTO。

```text
Entity -> Service -> VO -> Controller -> JSON
```

数据库字段命名不应直接决定外部 API 字段命名。
