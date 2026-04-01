# 综测系统

本仓库为综测系统项目，包含后端服务和前端应用：

- backend：Spring Boot 后端（认证、权限、业务 API、模板页）
- frontend：Vue 3 + Vite 前端（通过 /api 代理访问后端）

## Docker，Windows/Mac 通用

使用 Docker Compose 交付时，Windows 和 Mac 的启动/停止命令一致。

### 1. 前置条件

- 已安装 Docker Desktop（建议 4.x 及以上）
- Docker Engine 已启动

### 2. 准备环境变量

在项目根目录创建 `.env`（可复制 `.env.example`）：

Mac/Linux：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

- `JWT_SECRET` 必须至少 32 个字符
- 其他参数可按默认值运行（数据库账号、端口、初始密码等）

### 3. 启动（Windows/Mac 相同）

```bash
docker compose up -d --build
```

### 4. 访问地址

- 前端：http://localhost:5173
- 后端：http://localhost:8082
- MySQL：localhost:3306

### 5. 停止与清理

停止容器：

```bash
docker compose down
```

连同数据库卷一起清理（慎用）：

```bash
docker compose down -v
```

## 账号与密码策略

- 系统初始密码配置项：`app.user.initial-password`（默认 `123456`）
- 仅在用户表为空时进行初始化
- 默认只自动创建管理员账号（`admin`）
- `app.bootstrap.create-sample-users=true` 时才会创建示例教师/学生账号

## JWT 配置

- 配置项：`app.jwt.secret`
- 支持环境变量覆盖：`JWT_SECRET`
- 密钥至少 32 个字符，否则服务启动会报错

## CSV 导入与模板

模板下载接口：

- `/api/templates/students-import.csv`
- `/api/templates/pe-scores-import.csv`
- `/api/templates/study-scores-import.csv`

导入字段：

- 学生导入：`studentNo,studentName,className`
- 成绩导入：`studentNo,studentName,className,term,itemCode,score`





## 本地源码运行（可选）

如果不使用 Docker，可按以下方式本地运行。

### 本地运行环境（Mac）

- Java 17
- Maven 3.9+
- Node.js 18+
- MySQL 8+

### 本地启动

1. 启动后端

```bash
./run-backend.sh
```

2. 启动前端

```bash
./run-frontend.sh
```

### 本地数据库默认配置

默认数据库配置在 backend/src/main/resources/application.yml：

- 库名：zongce
- 用户：zongce
- 密码：zongce123!
