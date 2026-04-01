# 综测系统

本仓库为综测系统单仓项目，包含后端服务和前端应用：

- backend：Spring Boot 后端（认证、权限、业务 API、模板页）
- frontend：Vue 3 + Vite 前端（通过 /api 代理访问后端）

## 运行环境

- Java 17
- Maven 3.9+
- Node.js 18+
- MySQL 8+

## 快速启动

1. 启动后端

```bash
./run-backend.sh
```

2. 启动前端

```bash
./run-frontend.sh
```

访问地址：

- 前端：http://localhost:5173
- 后端：http://localhost:8082

## 数据库配置

默认数据库配置在 backend/src/main/resources/application.yml：

- 库名：zongce
- 用户：zongce
- 密码：zongce123!

## 账号与密码策略

- 系统初始密码配置项：app.user.initial-password（默认 123456）
- 仅在用户表为空时进行初始化
- 默认只自动创建管理员账号（admin）
- app.bootstrap.create-sample-users=true 时才会创建示例教师/学生账号

## JWT 配置

- 配置项：app.jwt.secret
- 支持环境变量覆盖：JWT_SECRET
- 密钥至少 32 个字符，否则服务启动会报错

示例：

```bash
export JWT_SECRET='ReplaceWithYourOwnLongRandomSecretAtLeast32Chars'
./run-backend.sh
```

## CSV 导入与模板

模板下载接口：

- /api/templates/students-import.csv
- /api/templates/pe-scores-import.csv
- /api/templates/study-scores-import.csv

导入字段：

- 学生导入：studentNo,studentName,className
- 成绩导入：studentNo,studentName,className,term,itemCode,score

## 常见问题

- 启动前端请使用 ./run-frontend.sh（不是 .run-frontend.sh）
- 若后端端口占用，run-backend 脚本会自动尝试释放 8082 端口
