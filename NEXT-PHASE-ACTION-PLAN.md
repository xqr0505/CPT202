# 🎯 下一步行动计划

## Phase 2: 数据库和业务逻辑实现

**预计耗时**: 40-50 小时  
**优先级**: 必需  
**截止时间**: 建议在 1 周内完成

---

## Week 1: 数据库和基础服务

### Day 1-2: 数据库设计和创建

#### Task 1: 创建 User 实体

```java
// backend/src/main/java/edu/xjtlu/.../entity/User.java

@Data
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"email", "role"})
})
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;  // CUSTOMER, SPECIALIST, ADMIN
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // 账户锁定相关
    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    // 审计字段
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

#### Task 2: 创建 UserRole 枚举

```java
// backend/src/main/java/edu/xjtlu/.../enums/UserRole.java

public enum UserRole {
    CUSTOMER("顾客"),
    SPECIALIST("专家"),
    ADMIN("管理员");
    
    private final String label;
    
    UserRole(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
```

#### Task 3: 创建 VerificationCode 实体

```java
// backend/src/main/java/edu/xjtlu/.../entity/VerificationCode.java

@Data
@Entity
@Table(name = "verification_codes")
public class VerificationCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String code;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VerificationType type;  // REGISTER, RESET_PASSWORD
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "is_used")
    private Boolean isUsed = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

#### Task 4: Flyway 数据库迁移

```sql
-- backend/src/main/resources/db/migration/V2__create_user_and_verification_tables.sql

-- 创建用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'SPECIALIST', 'ADMIN') NOT NULL,
    display_name VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email_role (email, role),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建验证码表
CREATE TABLE verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type ENUM('REGISTER', 'RESET_PASSWORD', 'EMAIL_CHANGE') NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_type (email, type),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建审计日志表（可选）
CREATE TABLE login_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_email_created (email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Day 3: 创建 Repository

```java
// backend/src/main/java/edu/xjtlu/.../repository/UserRepository.java

public interface UserRepository extends BaseMapper<User> {
    
    User selectByEmailAndRole(String email, UserRole role);
    
    User selectByEmail(String email);
    
    boolean existsByEmail(String email);
}

// backend/src/main/java/edu/xjtlu/.../repository/VerificationCodeRepository.java

public interface VerificationCodeRepository extends BaseMapper<VerificationCode> {
    
    VerificationCode selectLastValidCode(String email, VerificationType type);
    
    void updateAsUsed(Long id);
}
```

### Day 4: 创建 Service 层

#### UserService

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 根据邮箱和角色查询用户
     */
    public User getUserByEmailAndRole(String email, UserRole role) {
        return userRepository.selectByEmailAndRole(email, role);
    }
    
    /**
     * 检查邮箱是否被注册
     */
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * 创建新用户
     */
    public User createUser(String email, String password, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setDisplayName("New User");
        user.setIsActive(true);
        user.setLoginAttempts(0);
        
        return userRepository.insert(user);
    }
    
    /**
     * 验证密码
     */
    public boolean validatePassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
    
    /**
     * 更新密码
     */
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.selectById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.updateById(user);
    }
    
    /**
     * 增加登录失败次数
     */
    public void incrementLoginAttempts(User user) {
        user.setLoginAttempts(user.getLoginAttempts() + 1);
        
        if (user.getLoginAttempts() >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        }
        
        userRepository.updateById(user);
    }
    
    /**
     * 重置登录失败计数
     */
    public void resetLoginAttempts(Long userId) {
        User user = userRepository.selectById(userId);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.updateById(user);
    }
    
    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked(User user) {
        return user.getLockedUntil() != null 
            && user.getLockedUntil().isAfter(LocalDateTime.now());
    }
}
```

#### AuthService

```java
@Service
public class AuthService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private VerificationCodeService verificationCodeService;
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) throws BusinessException {
        // 1. 查询用户
        User user = userService.getUserByEmailAndRole(
            request.getEmail(),
            UserRole.valueOf(request.getRole())
        );
        
        if (user == null) {
            throw new BusinessException("Invalid email or password");
        }
        
        // 2. 检查账户是否被锁定
        if (userService.isAccountLocked(user)) {
            throw new BusinessException("Too many failed attempts. Please try again after 15 minutes.");
        }
        
        // 3. 验证密码
        if (!userService.validatePassword(request.getPassword(), user.getPasswordHash())) {
            userService.incrementLoginAttempts(user);
            throw new BusinessException("Invalid email or password");
        }
        
        // 4. 重置登录失败计数
        userService.resetLoginAttempts(user.getId());
        
        // 5. 生成 Token
        String token = JwtUtils.generateToken(user.getId(), user.getRole().name());
        
        // 6. 记录审计日志（可选）
        auditLoginSuccess(user);
        
        return new LoginResponse(
            token,
            user.getId(),
            user.getRole().name(),
            user.getEmail(),
            user.getDisplayName(),
            30 * 60L
        );
    }
    
    /**
     * 用户注册
     */
    public LoginResponse register(RegisterRequest request) throws BusinessException {
        // 1. 验证两个密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        
        // 2. 验证密码复杂度
        if (!validatePasswordComplexity(request.getPassword())) {
            throw new BusinessException("Password not meet complexity");
        }
        
        // 3. 检查邮箱是否已注册
        if (userService.isEmailRegistered(request.getEmail())) {
            throw new BusinessException("This email is already registered");
        }
        
        // 4. 验证验证码
        if (!verificationCodeService.verifyCode(
            request.getEmail(),
            request.getVerificationCode(),
            VerificationType.REGISTER
        )) {
            throw new BusinessException("Verification code incorrect or expired");
        }
        
        // 5. 创建用户
        User user = userService.createUser(
            request.getEmail(),
            request.getPassword(),
            UserRole.valueOf(request.getRole())
        );
        
        // 6. 生成 Token 并自动登录
        String token = JwtUtils.generateToken(user.getId(), user.getRole().name());
        
        return new LoginResponse(
            token,
            user.getId(),
            user.getRole().name(),
            user.getEmail(),
            user.getDisplayName(),
            30 * 60L
        );
    }
    
    /**
     * 验证密码复杂度
     * 要求：至少 8 字符，包含大小写字母和数字
     */
    private boolean validatePasswordComplexity(String password) {
        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";
        return password.matches(regex);
    }
    
    private void auditLoginSuccess(User user) {
        // TODO: 记录登录日志
    }
}
```

#### VerificationCodeService

```java
@Service
public class VerificationCodeService {
    
    @Autowired
    private VerificationCodeRepository verificationCodeRepository;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * 发送验证码
     */
    public void sendVerificationCode(String email, VerificationType type) throws BusinessException {
        // 1. 检查上次请求间隔（防止频繁发送）
        VerificationCode lastCode = verificationCodeRepository.selectLastValidCode(email, type);
        
        if (lastCode != null) {
            Duration duration = Duration.between(lastCode.getCreatedAt(), LocalDateTime.now());
            if (duration.getSeconds() < 60) {
                throw new BusinessException("Please wait 60 seconds before requesting a new code");
            }
        }
        
        // 2. 生成 6 位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        // 3. 保存到数据库
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setType(type);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationCode.setIsUsed(false);
        
        verificationCodeRepository.insert(verificationCode);
        
        // 4. 发送邮件
        emailService.sendVerificationEmail(email, code);
    }
    
    /**
     * 验证验证码
     */
    public boolean verifyCode(String email, String code, VerificationType type) {
        VerificationCode verificationCode = verificationCodeRepository.selectLastValidCode(email, type);
        
        if (verificationCode == null) {
            return false;
        }
        
        // 检查验证码是否已使用
        if (verificationCode.getIsUsed()) {
            return false;
        }
        
        // 检查验证码是否过期
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        // 检查验证码是否正确
        if (!verificationCode.getCode().equals(code)) {
            return false;
        }
        
        // 标记为已使用
        verificationCode.setIsUsed(true);
        verificationCodeRepository.updateById(verificationCode);
        
        return true;
    }
}
```

### Day 5: 配置密码加密

```java
// backend/src/main/java/edu/xjtlu/.../config/EncryptionConfig.java

@Configuration
public class EncryptionConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Week 2: 邮件服务和完善

### Day 1-2: 邮件服务实现

#### EmailService

```java
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${mail.from.address}")
    private String fromAddress;
    
    /**
     * 发送验证码邮箱
     */
    public void sendVerificationEmail(String recipientEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipientEmail);
            message.setSubject("[CPT202 预约平台] 注册验证码");
            message.setText("你的验证码是: " + code + "\n\n" +
                           "验证码有效期 10 分钟\n" +
                           "如非本人操作，请忽略此邮件");
            
            mailSender.send(message);
            
        } catch (MailException e) {
            throw new BusinessException("Failed to send verification email: " + e.getMessage());
        }
    }
    
    /**
     * 发送密码重置邮件
     */
    public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipientEmail);
            message.setSubject("[CPT202 预约平台] 密码重置");
            message.setText("请点击以下链接重置密码:\n" + resetLink + "\n\n" +
                           "链接有效期 1 小时\n" +
                           "如非本人操作，请忽略此邮件");
            
            mailSender.send(message);
            
        } catch (MailException e) {
            throw new BusinessException("Failed to send password reset email");
        }
    }
}
```

#### 配置 application-dev.yml

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password  # 使用应用专用密码
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

mail:
  from:
    address: noreply@cpt202.com
```

### Day 3-4: 前端路由守卫

```typescript
// frontend/src/router/index.ts

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/api/request'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/customer/dashboard',
      name: 'CustomerDashboard',
      component: () => import('@/views/customer/Dashboard.vue'),
      meta: { requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: '/specialist/dashboard',
      name: 'SpecialistDashboard',
      component: () => import('@/views/specialist/Dashboard.vue'),
      meta: { requiresAuth: true, roles: ['SPECIALIST'] }
    },
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/Dashboard.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = getToken()

  // 需要认证的页面
  if (to.meta.requiresAuth) {
    if (!token) {
      // 未登录，重定向到登录页
      next({
        name: 'Login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 检查角色权限
    if (to.meta.roles) {
      if (!to.meta.roles.includes(userStore.userRole)) {
        ElMessage.error('Access denied')
        next(from)
        return
      }
    }
  }

  // 已登录不能访问登录页
  if (to.name === 'Login' && token) {
    next('/customer/dashboard')
    return
  }

  next()
})

export default router
```

### Day 5: 测试和调试

**测试检查清单**：

- [ ] 注册新用户
- [ ] 验证邮箱是否收到验证码
- [ ] 验证码过期时间
- [ ] 密码复杂度检查
- [ ] 登录失败 5 次后锁定
- [ ] 登出后清除 Token
- [ ] 访问受保护页面时重定向

---

## 关键文件作业分配

### 推荐分工（8 人团队）

| 人员 | 任务 | 预计时间 |
|-----|------|---------|
| 人 1 | 数据库设计 + SQL | 8 小时 |
| 人 2 | 实体类 + Repository | 8 小时 |
| 人 3 | AuthService + UserService | 12 小时 |
| 人 4 | VerificationCodeService + EmailService | 10 小时 |
| 人 5 | 前端路由守卫 + Store 完善 | 10 小时 |
| 人 6 | 注册页面实现 | 8 小时 |
| 人 7 | 密码重置页面 | 8 小时 |
| 人 8 | 集成测试 + 文档 | 12 小时 |

**总计**: 76 小时 ≈ 2 周（4 人同时工作）

---

## 验收标准

### 单元测试

```java
// 认证服务测试
@Test
public void testLoginSuccess() { }

@Test
public void testLoginInvalidPassword() { }

@Test
public void testAccountLocking() { }

@Test
public void testRegistration() { }

@Test
public void testVerificationCodeExpiry() { }
```

### 集成测试

```typescript
// e2e 测试
describe('Authentication Flow', () => {
  it('should register and login', async () => { })
  
  it('should handle login failures', async () => { })
  
  it('should logout properly', async () => { })
})
```

---

## 成功指标

- ✅ 所有 API 接口在 Postman 中可用
- ✅ 邮件验证码能正常接收
- ✅ 密码加密和验证工作
- ✅ 登录失败锁定功能生效
- ✅ 前端路由守卫保护
- ✅ 单元测试覆盖率 > 80%
- ✅ 所有 PBI 需求满足

---

## 常见陷阱（避免）

- ❌ 将秘钥硬编码
- ❌ 明文存储密码
- ❌ 忘记验证邮箱格式
- ❌ 验证码没有过期时间
- ❌ 没有限制登录失败次数
- ❌ 前端不验证，只注册后端
- ❌ 邮件服务没有异常处理

---

**下一步**：完成 Phase 2 后，可以开始 PBI 6（预约管理）

Good luck! 🚀
