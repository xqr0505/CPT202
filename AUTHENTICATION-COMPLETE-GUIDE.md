# 完整的认证体系实现指南

## 📚 目录
1. [系统架构](#系统架构)
2. [技术栈](#技术栈)
3. [后端实现](#后端实现)
4. [前端实现](#前端实现)
5. [集成步骤](#集成步骤)
6. [测试流程](#测试流程)
7. [常见问题](#常见问题)
8. [下一步任务](#下一步任务)

---

## 系统架构

### 流程图

```
┌─────────────────────────────────────────────────────────────┐
│                     用户登录流程                              │
└─────────────────────────────────────────────────────────────┘

1. 前端：用户填表 (邮箱、密码、角色)
   ↓
2. 前端：调用 login() API
   ↓
3. 后端：AuthController 验证凭证
   ├─ 检查邮箱是否存在
   ├─ 验证密码是否正确
   ├─ 检查账户是否被锁定
   └─ 生成 JWT Token
   ↓
4. 前端：Request 拦截器保存 Token
   ├─ localStorage.setItem('token', token)
   ├─ 保存用户信息到 Store
   └─ 跳转到首页
   ↓
5. 后续请求：自动添加 Token
   ├─ Header: Authorization: Bearer {token}
   └─ JwtAuthenticationFilter 验证 Token
   ↓
6. Token 失效：自动重定向登录页
```

---

## 技术栈

### 后端
- **Spring Boot 3.3.5**
- **Spring Security** - 安全认证框架
- **JJWT** - JWT 库
- **MyBatis Plus** - ORM 框架

### 前端
- **Vue 3** - 框架
- **TypeScript** - 类型系统
- **Axios** - HTTP 客户端
- **Pinia** - 状态管理

---

## 后端实现

### 已完成的文件

```
common/
├── utils/
│   ├── JwtUtils.java           ✅ JWT Token 生成和解析
│   └── SecurityUtils.java       ✅ 用户上下文获取工具
├── security/
│   ├── JwtAuthenticationFilter  ✅ 验证 Token 的过滤器
│   ├── RestAuthenticationEntryPoint  ✅ 处理 401 异常
│   └── RestAccessDeniedHandler      ✅ 处理 403 异常
├── config/
│   ├── SecurityConfig.java      ✅ Security 配置
│   └── JwtConfig.java           ✅ JWT 秘钥初始化
└── context/
    └── UserContextHolder.java   ✅ ThreadLocal 用户上下文

controller/
└── AuthController.java          ✅ 示例认证接口

dto/
├── LoginRequest.java            ✅ 登录请求
├── LoginResponse.java           ✅ 登录响应
├── RegisterRequest.java         ✅ 注册请求
└── SendVerificationCodeRequest  ✅ 验证码请求
```

### 核心类解析

#### 1. JwtUtils - Token 生成器

```java
// 生成 Token
String token = JwtUtils.generateToken(userId, role);

// 验证 Token
Claims claims = JwtUtils.parseToken(token);
Long userId = claims.get("userId", Long.class);
String role = claims.get("role", String.class);

// 检查 Token 有效性
if (JwtUtils.validateToken(token)) {
    // Token 有效
}
```

#### 2. SecurityUtils - 用户上下文获取

```java
// 在任何位置获取当前用户信息
Long userId = SecurityUtils.getCurrentUserId();
String role = SecurityUtils.getCurrentUserRole();
boolean isAuth = SecurityUtils.isAuthenticated();
```

#### 3. JwtAuthenticationFilter - 请求验证

工作流程：
1. 从请求头获取 `Authorization: Bearer {token}`
2. 提取 Token 字符串
3. 解析 Token，获取 `userId` 和 `role`
4. 放入 SecurityContext 供后续处理
5. 放行请求（或拒绝）

---

## 前端实现

### 已完成的文件

```
src/
├── api/
│   ├── request.ts           ✅ Axios 配置 + 拦截器
│   └── auth.ts              ✅ 认证 API 函数
├── views/auth/
│   └── Login.vue            ✅ 登录页面
└── stores/
    └── user.ts              ⚠️ 需要更新
```

### 核心模块解析

#### 1. request.ts - HTTP 客户端

```typescript
// 请求拦截器：自动添加 Token
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：处理 401/403
axios.interceptors.response.use(
  (response) => {
    if (response.code === 401) {
      // 清除 Token，重定向登录
      clearAuthData();
      router.push('/login');
    }
  }
);
```

#### 2. auth.ts - API 函数

```typescript
// 登录
const response = await login({
  email: 'user@example.com',
  password: 'Password123',
  role: 'CUSTOMER'
}, rememberMe);

// 注册
await register({
  email: 'newuser@example.com',
  verificationCode: '123456',
  password: 'Password123',
  confirmPassword: 'Password123',
  role: 'CUSTOMER'
});

// 发送验证码
await sendVerificationCode({
  email: 'user@example.com',
  type: 'REGISTER'
});

// 登出
await logout();
```

#### 3. Login.vue - 登录页面

- ✅ 角色选择
- ✅ 邮箱验证
- ✅ 密码输入
- ✅ 错误提示
- ✅ 加载状态

---

## 集成步骤

### Step 1: 配置秘钥

编辑 `backend/src/main/resources/application-dev.yml`：

```yaml
jwt:
  secret: your-secret-key-with-at-least-32-characters
```

> ⚠️ **重要**：秘钥长度必须 >= 32 字符

### Step 2: 创建用户实体 (TODO)

你需要创建用户 Entity：

```java
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    private String passwordHash;  // 使用 BCryptPasswordEncoder 加密
    private String role;  // CUSTOMER, SPECIALIST, ADMIN
    private String displayName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Step 3: 实现真实的 AuthService (TODO)

在 AuthController 中调用真实的业务逻辑：

```java
@Service
public class AuthService {
    
    // 验证用户凭证
    public User authenticate(String email, String password, String role) {
        // 1. 查询用户
        // 2. 检查角色是否匹配
        // 3. 验证密码 (使用 BCryptPasswordEncoder)
        // 4. 检查账户是否被锁定
        // 5. 返回用户
    }
    
    // 发送验证码
    public void sendVerificationCode(String email, String type) {
        // 1. 生成 6 位随机验证码
        // 2. 保存到 Redis (TTL: 10 分钟)
        // 3. 发送邮件
    }
    
    // 验证验证码
    public boolean verifyCode(String email, String code) {
        // 1. 从 Redis 获取验证码
        // 2. 比较
        // 3. 删除（防止重复使用）
    }
}
```

### Step 4: 建立数据库表 (TODO)

运行 Flyway 迁移创建用户表：

```sql
-- db/migration/V2__create_user_table.sql

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'SPECIALIST', 'ADMIN') NOT NULL,
    display_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email_role (email, role)
);

-- 用于存储验证码（可选，实际使用 Redis）
CREATE TABLE verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type ENUM('REGISTER', 'RESET_PASSWORD', 'EMAIL_CHANGE') NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Step 5: 设置前端路由保护 (TODO)

在 `router/index.ts` 中添加路由守卫：

```typescript
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  const token = localStorage.getItem('token');

  // 需要认证的页面
  if (to.meta.requiresAuth && !token) {
    next('/login');
  } else if (to.path === '/login' && token) {
    // 已登录不能访问登录页
    next('/dashboard');
  } else {
    next();
  }
});
```

### Step 6: 更新环境变量

编辑 `frontend/.env.development`：

```
VITE_API_URL=http://localhost:8080
```

---

## 测试流程

### 后端测试

#### 1. 启动服务

```bash
cd backend
mvn spring-boot:run
```

服务运行在 `http://localhost:8080`

#### 2. 使用 Postman 测试登录

```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123",
  "role": "CUSTOMER"
}
```

响应应该包含 Token 和用户信息。

#### 3. 测试受保护的接口

```
GET http://localhost:8080/api/user/profile
Authorization: Bearer {token_from_login_response}
```

### 前端测试

#### 1. 启动前端开发服务

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

#### 2. 测试登录流程

1. 访问 `http://localhost:5173/login`
2. 填写邮箱和密码
3. 点击登录
4. 应该看到成功提示并跳转到首页

#### 3. 测试 Token 持久化

1. 登录成功
2. 打开浏览器开发工具 (F12) -> Application -> LocalStorage
3. 验证 `token` 已保存

#### 4. 测试 Token 失效处理

1. 手动清除 localStorage 中的 token
2. 试图访问受保护页面
3. 应该自动跳转到登录页

---

## 常见问题

### Q1: Secret key must be at least 32 characters long

**错误**：`java.lang.IllegalArgumentException: Secret key must be at least 32 characters long`

**解决**：
- 检查 `application-dev.yml` 中的 `jwt.secret` 长度
- 秘钥必须 >= 32 字符

```yaml
jwt:
  secret: dev-secret-key-with-at-least-32-characters
```

### Q2: Token 过期时间设置

默认 30 分钟过期。如需修改：

编辑 `JwtUtils.java`：
```java
private static final long EXPIRATION = 1000 * 60 * 60; // 1 小时
```

### Q3: 跨域问题 (CORS)

如果前端请求报 CORS 错误，在 SecurityConfig 中添加：

```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:5173")
                    .allowedMethods("*")
                    .allowCredentials(true);
        }
    };
}
```

### Q4: 前端无法获取用户信息

检查 `request.ts` 中的响应处理：

```typescript
// 确保返回的是 data，而不是整个 response
return response.data;
```

---

## 下一步任务

### 高优先级 (必须完成)

- [ ] 1. 创建 User 实体和数据库表
- [ ] 2. 实现 UserService 和 UserRepository
- [ ] 3. 实现真实的认证逻辑（查询数据库、验证密码）
- [ ] 4. 实现密码加密（BCryptPasswordEncoder）
- [ ] 5. 实现验证码发送功能（集成邮件服务）
- [ ] 6. 处理账户锁定逻辑（登错密码5次15分钟锁定）

### 中优先级 (需要完成)

- [ ] 7. 实现"记住我"功能（30 天过期）
- [ ] 8. 实现会话超时提醒（29 分钟警告，30 分钟自动登出）
- [ ] 9. 添加登出时的 Token 黑名单
- [ ] 10. 添加路由守卫和权限检查
- [ ] 11. 实现前端用户 Profile 页面
- [ ] 12. 实现密码修改功能

### 低优先级 (优化相关)

- [ ] 13. 添加审计日志（记录登录、登出、访问）
- [ ] 14. 集成 OAuth2 (Google/微信登录)
- [ ] 15. 添加双因素认证 (2FA)
- [ ] 16. 实现单设备登录（踢掉旧的 Token）

---

## 参考资源

- [Spring Security 官方文档](https://spring.io/projects/spring-security)
- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [JWT.io - JWT 调试工具](https://jwt.io/)
- [Axios 文档](https://axios-http.com/)
- [Vue 3 官方文档](https://vuejs.org/)

---

## 总结

✅ **已完成**：
- [x] 后端 JWT 工具类
- [x] 后端 Security 配置
- [x] 后端异常处理
- [x] 前端 Axios 拦截器
- [x] 前端 API 函数
- [x] 前端登录页面示例

⚠️ **需要继续**：
- 数据库和用户实体
- 真实的认证逻辑
- 邮件验证码服务
- 路由守卫
- 更多页面实现

💡 **架构优势**：
- 无状态 JWT 认证
- 前后端分离
- 易于扩展
- 安全性强
- 易于测试
