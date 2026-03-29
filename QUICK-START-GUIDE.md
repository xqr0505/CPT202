# ⚡ 快速入门指南（5 分钟快速了解）

## 你需要知道的核心知识

### 1. 认证流程（一图胜千言）

```
┌─────────────────────────────────────────────────────────────┐
│                  用户登录流程                                │
└─────────────────────────────────────────────────────────────┘

前端登录页面 → 用户填表
       ↓
调用 /auth/login API
       ↓
AuthController.login()
       ↓ 验证邮箱、密码、角色
业务逻辑处理
       ↓ 生成 JWT Token
返回 Token + 用户信息
       ↓
前端保存 Token 到 localStorage
       ↓
所有后续请求都添加:
Authorization: Bearer {token}
       ↓
JwtAuthenticationFilter 验证 Token
       ↓ 解析 → 设置 SecurityContext
请求处理
```

### 2. 四个关键文件

#### 后端

1. **JwtUtils.java** - Token 的生成和解析
   ```java
   // 核心方法
   JwtUtils.generateToken(userId, role)    // 生成 Token
   JwtUtils.parseToken(token)               // 验证和解析 Token
   ```

2. **SecurityConfig.java** - 安全配置
   ```java
   // 核心职责
   - 放行公开接口 (/auth/login, /auth/register)
   - 所有其他请求需要认证
   - 添加 JWT 过滤器
   - 配置异常处理
   ```

3. **JwtAuthenticationFilter.java** - 请求验证
   ```java
   // 核心职责
   - 从请求头获取 Token
   - 解析 Token
   - 传递给 SecurityContext
   ```

4. **AuthController.java** - 认证接口
   ```java
   // 提供的接口
   POST /auth/login           - 用户登录
   POST /auth/register        - 用户注册
   POST /auth/logout          - 用户登出
   POST /auth/verify-email    - 发送验证码
   ```

#### 前端

1. **request.ts** - HTTP 客户端
   ```typescript
   // 请求拦截器
   - 自动添加 Authorization 请求头
   
   // 响应拦截器
   - 处理 401: 清除 Token，重定向登录
   - 处理 403: 显示权限不足
   ```

2. **auth.ts** - 认证 API
   ```typescript
   // 导出的函数
   login()                    - 登录
   register()                 - 注册
   logout()                   - 登出
   sendVerificationCode()     - 发送验证码
   resetPassword()            - 重置密码
   ```

3. **Login.vue** - 登录页面
   ```typescript
   // 用户填表 → 验证表单 → 调用 login() → 跳转首页
   ```

### 3. 秘钥配置（5 秒搞定）

编辑 `backend/src/main/resources/application-dev.yml`：

```yaml
jwt:
  secret: dev-secret-key-with-at-least-32-characters
```

⚠️ **最少 32 字符**，否则报错！

### 4. 环境变量配置

编辑 `frontend/.env.development`：

```
VITE_API_URL=http://localhost:8080
```

### 5. 启动服务

#### 后端
```bash
cd backend
mvn spring-boot:run
# 访问 http://localhost:8080/doc.html 查看 API 文档
```

#### 前端
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173/login
```

---

## 我该按什么顺序学习？

### Week 1: 理解架构（3 小时）

1. **Day 1**: 阅读
   - [AUTH-IMPLEMENTATION-SUMMARY.md](AUTH-IMPLEMENTATION-SUMMARY.md) - 总体概览
   - JWT Token 流程图（上面第 1 部分）

2. **Day 2**: 本地启动
   - 启动后端服务
   - 启动前端开发服务
   - 用 Postman 测试 /auth/login 接口

3. **Day 3**: 阅读代码
   - 从上到下阅读 JwtAuthenticationFilter
   - 理解 Token 验证流程

### Week 2: 实现业务逻辑（40 小时）

按照 [NEXT-PHASE-ACTION-PLAN.md](NEXT-PHASE-ACTION-PLAN.md) 的任务清单：

1. 创建数据库表
2. 创建实体类
3. 实现认证服务
4. 实现邮件服务
5. 前端路由守卫

---

## 常见问题速查

| 问题 | 解决方案 |
|-----|--------|
| "secret key must be at least 32 characters" | 检查 jwt.secret 长度 >= 32 |
| CORS 错误 | 前端 API_URL 是否正确 |
| 登录后仍跳转登录页 | 检查 localStorage 是否保存 token |
| 后端无法绑接收到前端 Token | 检查请求头是否包含 Authorization |
| 邮件无法发送 | 检查 SMTP 配置和凭证 |

---

## 我的第一个任务（从这里开始！）

### 任务 0: 熟悉项目（30 分钟）

```bash
# 1. 启动后端
cd backend
mvn spring-boot:run

# 2. 启动前端（新终端）
cd frontend
npm run dev

# 3. 用浏览器打开
http://localhost:5173/login

# 4. 尝试登录（会失败，因为没有真实的用户数据库）
# 这是正常的！说明整个链路已经通了
```

### 任务 1: 创建 User 实体（2 小时）

```
目标: 定义用户数据库表结构

1. 复制代码: src/main/java/edu/xjtlu/cpt202/backend/entity/User.java
   (参考 NEXT-PHASE-ACTION-PLAN.md 中的代码)

2. 运行项目，Flyway 会自动创建表

3. 验证: 在 MySQL 中查看是否创建了 users 表
   SELECT * FROM users;
```

### 任务 2: 实现 AuthService（4 小时）

```
目标: 实现真实的登录逻辑

1. 创建 UserRepository
2. 创建 AuthService，实现 login() 方法
3. 在 AuthController 中调用 AuthService

关键业务逻辑:
- 查询用户是否存在
- 验证密码是否正确
- 生成并返回 JWT Token
```

### 任务 3: 测试登录流程（1 小时）

```
验证: 使用 Postman 测试登录

1. 使用数据库插入测试数据
   INSERT INTO users (...) VALUES (...)

2. 用 Postman POST 请求
   POST http://localhost:8080/auth/login
   {
     "email": "test@example.com",
     "password": "Password123",
     "role": "CUSTOMER"
   }

3. 预期返回:
   {
     "code": 200,
     "data": {
       "token": "eyJ...",
       "userId": 1,
       ...
     }
   }
```

---

## 推荐阅读顺序

```
1️⃣ 本文件 (5 分钟) ✓ 你在这里
   ↓
2️⃣ AUTH-IMPLEMENTATION-SUMMARY.md (20 分钟)
   了解整体架构和已完成工作
   ↓
3️⃣ AUTHENTICATION-COMPLETE-GUIDE.md (30 分钟)
   理解每个组件的作用
   ↓
4️⃣ PBI-1-5-IMPLEMENTATION-GUIDE.md (30 分钟)
   了解需要实现的功能
   ↓
5️⃣ NEXT-PHASE-ACTION-PLAN.md (1 小时)
   制定你的工作计划
   ↓
6️⃣ 开始实现！
```

---

## 代码模板速查

### 后端：获取当前用户

```java
// 在 Controller 或 Service 中随时访问
@GetMapping("/profile")
public Result<?> getProfile() {
    Long userId = SecurityUtils.getCurrentUserId();
    String role = SecurityUtils.getCurrentUserRole();
    
    User user = userService.getUserById(userId);
    return Result.success(user);
}
```

### 前端：发送认证请求

```typescript
// 自动添加 Token
import request from '@/api/request'

// 所有请求都会自动添加 Authorization 请求头
const response = await request.get('/api/user/profile')

// 如果 Token 过期返回 401
// request.ts 会自动:
// 1. 清除 localStorage
// 2. 跳转到登录页
```

---

## 关键概念解释

### JWT Token 是什么？

一个包含用户信息的加密字符串，形如：

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInJvbGUiOiJDVVNUT01FUiJ9.SIGNATURE
  └─ Header            └─ Payload                          └─ 签名
```

**优点**：无状态，服务器无需存储 Session

### 为什么需要过滤器？

```
每个请求都需要验证 Token:

前端请求 
  ↓
过滤器 1（JwtAuthenticationFilter）验证 Token ← 这是关键
  ↓
过滤器 2, 3, ...
  ↓
Controller 处理业务逻辑
```

### 为什么需要异常处理？

```
场景 1: 没有 Token → 返回 401 → 前端跳转登录页
场景 2: Token 无效 → 返回 401 → 前端重新登录
场景 3: Token 有效但无权限 → 返回 403 → 前端显示"无权限"
```

---

## 下一步该怎么做？

### 立即开始（今天）

- [ ] 启动后端和前端
- [ ] 理解认证流程
- [ ] 使用 Postman 测试 API

### 本周完成（Week 1）

- [ ] 创建 User 实体
- [ ] 创建 UserRepository
- [ ] 实现 AuthService
- [ ] 配置数据库

### 下周完成（Week 2）

- [ ] 实现邮件服务
- [ ] 前端路由守卫
- [ ] 完善页面 UI
- [ ] 全面测试

---

## 需要帮助？

### 三个关键文件

| 问题 | 查看文件 |
|-----|--------|
| "整体流程是什么？" | AUTH-IMPLEMENTATION-SUMMARY.md |
| "PBI 1-5 怎么实现？" | PBI-1-5-IMPLEMENTATION-GUIDE.md |
| "具体怎么写代码？" | NEXT-PHASE-ACTION-PLAN.md |
| "秘钥怎么配置？" | JWT-KEY-MANAGEMENT.md |

### 调试技巧

```bash
# 1. 看后端日志
mvn spring-boot:run
# 查看是否有异常堆栈跟踪

# 2. 用 Postman 测试 API
# 确认请求格式和响应是否正确

# 3. 浏览器开发工具 (F12)
# Application → LocalStorage → token
# 检查 Token 是否被保存

# 4. JWT 调试
# 访问 https://jwt.io/
# 粘贴 Token 查看内容
```

---

**现在就开始吧！** 🚀

有问题？查看文档或问你的技术负责人。

祝你编码愉快！
