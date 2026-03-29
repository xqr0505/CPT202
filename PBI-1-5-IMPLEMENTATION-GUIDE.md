# PBI 1-5 实现指导

本文档详细说明如何根据 PBI 需求逐步完成认证模块。

---

## PBI 1: User Registration (用户注册)

### 需求拆分

#### 场景 1-2: 导航和邮箱验证

**需要实现**：
1. 注册页面 - 显示角色选择、邮箱、密码字段
2. 验证码发送功能
3. 错误提示

**技术实现**：

```typescript
// 前端：src/views/auth/Register.vue
- 显示"Sign Up"链接 -> 导航到 Register 页面
- 选择角色 (Customer/Specialist)
- 输入邮箱
- 点击"Get Verification Code"按钮

// 后端任务
- POST /auth/verify-email
  * 检查角色是否选择
  * 检查邮箱格式
  * 检查邮箱是否已注册
  * 检查请求频率（60秒）
  * 生成验证码 + 发送邮件
```

**代码框架**：

```java
// 后端：src/main/java/edu/xjtlu/.../service/EmailService.java

@Service
public class EmailService {
    
    // 生成验证码
    public String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
    
    // 发送验证码邮件
    public void sendVerificationEmail(String email, String code) {
        // TODO: 使用 Spring Mail 或第三方 SMTP 服务
        // 邮件内容示例：
        // 标题：Verification Code for Registration
        // 内容：Your verification code is: 123456
        //      Code will expire in 10 minutes
    }
}
```

```typescript
// 前端：src/views/auth/Register.vue

export function handleGetVerificationCode() {
  // 1. 验证：角色、邮箱、邮箱格式
  // 2. 调用 API
  const response = await sendVerificationCode({
    email: form.email,
    role: form.role,
    type: 'REGISTER'
  });
  // 3. 显示"Verification code sent"
  // 4. 启动 60 秒倒计时
}
```

#### 场景 3-6: 设置密码和验证

**需要实现**：
1. 密码输入字段
2. 密码复杂度检查（≥8 字符，大小写，数字）
3. 验证码过期检查

**代码框架**：

```java
// 后端：RegisterService

@Service
public class RegisterService {
    
    // 验证密码复杂度
    public Boolean validatePasswordComplexity(String password) {
        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";
        return password.matches(regex);
    }
    
    // 注册新用户
    public User registerUser(RegisterRequest request) {
        // 1. 验证验证码
        String code = getVerificationCode(request.getEmail());
        if (!code.equals(request.getVerificationCode())) {
            throw new BusinessException("Verification code incorrect or expired");
        }
        
        // 2. 验证密码复杂度
        if (!validatePasswordComplexity(request.getPassword())) {
            throw new BusinessException("Password not meet complexity");
        }
        
        // 3. 验证两次密码一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("password not match");
        }
        
        // 4. 加密密码
        String passwordHash = passwordEncoder.encode(request.getPassword());
        
        // 5. 保存用户到数据库
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordHash);
        user.setRole(request.getRole());
        user.setDisplayName("New User");
        user.setIsActive(true);
        
        return userRepository.save(user);
    }
}
```

```typescript
// 前端：Register.vue

const validatePassword = (password: string) => {
  const regex = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,}$/;
  return regex.test(password);
};

const handleRegister = async () => {
  // 1. 验证所有字段不为空
  // 2. 验证邮箱格式
  // 3. 验证密码复杂度
  // 4. 验证两次密码相同
  // 5. 调用 register API
  
  const response = await register({
    email: form.email,
    verificationCode: form.verificationCode,
    password: form.password,
    confirmPassword: form.confirmPassword,
    role: form.role
  });
  
  // 6. 自动登录
  // 7. 跳转到首页
};
```

---

## PBI 2: User Login (用户登录)

### 需求拆分

#### 场景 1-4: 导航和表单验证

```typescript
// 需要实现的验证规则

const validators = {
  role: (role) => role ? null : "Please select a role first.",
  email: (email) => {
    if (!email) return "Email is required";
    if (!isValidEmail(email)) return "Please enter a valid email address";
    return null;
  },
  password: (password) => password ? null : "Password is required"
};
```

#### 场景 5-6: 认证处理

```java
// 后端：AuthService

@Service
public class AuthService {
    
    public LoginResponse login(LoginRequest request) {
        // 1. 根据 email 和 role 查询用户
        User user = userRepository.findByEmailAndRole(
            request.getEmail(),
            request.getRole()
        );
        
        if (user == null) {
            // 错误：用户不存在或角色不匹配
            throw new BusinessException("Invalid email or password");
        }
        
        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 增加登录失败计数
            incrementLoginAttempts(user);
            throw new BusinessException("Invalid email or password");
        }
        
        // 3. 检查账户是否被锁定
        if (user.isLockedUntil() != null && user.isLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Too many failed attempts. Please try again after 15 minutes.");
        }
        
        // 4. 重置登录失败计数
        user.setLoginAttempts(0);
        userRepository.save(user);
        
        // 5. 生成 JWT Token
        String token = JwtUtils.generateToken(user.getId(), user.getRole());
        
        return new LoginResponse(token, user.getId(), user.getRole(), ...);
    }
    
    // 处理登录失败次数
    private void incrementLoginAttempts(User user) {
        user.setLoginAttempts(user.getLoginAttempts() + 1);
        
        // 5 次失败后锁定 15 分钟
        if (user.getLoginAttempts() >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        }
        
        userRepository.save(user);
    }
}
```

#### 场景 7: 账户锁定

这已在上面的 `incrementLoginAttempts` 中实现。

#### 场景 8-11: 记住我功能

```typescript
// 前端逻辑

const handleLogin = async () => {
  const response = await login(credentials, false);
  
  // 登录成功后，显示"记住我"对话框
  ElMessageBox.confirm(
    'Do you want us to remember your password?',
    'Remember Me',
    {
      confirmButtonText: 'Remember',
      cancelButtonText: "Don't Remember"
    }
  ).then(() => {
    // 用户选择"记住"
    saveCredentials(form.email, form.password, true);
  }).catch(() => {
    // 用户选择"不记住"
    saveCredentials(form.email, form.password, false);
  });
};

// 登录页初始化：自动填充保存的凭证
onMounted(() => {
  const saved = getSavedCredentials();
  if (saved) {
    form.email = saved.email;
    form.password = saved.password;
  }
});

// 修改密码后清除保存的凭证
const handleChangePassword = async () => {
  await changePassword(...);
  clearSavedCredentials();
};
```

---

## PBI 3: User Logout (用户登出)

```typescript
// 前端：AppNavbar.vue

export function handleLogout() {
  ElMessageBox.confirm(
    'Are you sure to logout?',
    'Logout'
  ).then(async () => {
    // 调用登出 API
    await logout();
    
    // 清除本地存储
    clearAuthData();
    
    // 跳转登录页
    router.push('/login');
    
    // 显示提示
    ElMessage.success('Logout successful');
  }).catch(() => {
    // 用户取消
  });
}
```

**多设备/多标签行为**：

- 不同设备登出：互不影响，因为 Token 独立
- 同浏览器多标签：需要通过 localStorage change 事件同步

```typescript
// 前端：监听 localStorage 变化

window.addEventListener('storage', (event) => {
  if (event.key === 'token' && !event.newValue) {
    // Token 被清除，可能在另一个标签页登出了
    // 当前标签页也应该重定向到登录页
    router.push('/login');
  }
});
```

---

## PBI 4: Session Inactivity Timeout (会话超时)

### 实现方案 A：前端倒计时

```typescript
// 前端：composables/useSessionTimeout.ts

export function useSessionTimeout() {
  const inactivityTime = 30 * 60 * 1000; // 30 分钟
  const warningTime = 1 * 60 * 1000;    // 1 分钟
  
  let inactivityTimer: number | null = null;
  let warningTimer: number | null = null;
  
  /**
   * 重置不活跃计时器
   */
  function resetTimer() {
    // 清除现有计时器
    if (inactivityTimer) clearTimeout(inactivityTimer);
    if (warningTimer) clearTimeout(warningTimer);
    
    // 关闭警告弹窗
    warningVisible.value = false;
    
    // 设置 29 分钟后显示警告
    warningTimer = setTimeout(() => {
      showWarning();
    }, inactivityTime - warningTime);
    
    // 设置 30 分钟后自动登出
    inactivityTimer = setTimeout(() => {
      autoLogout();
    }, inactivityTime);
  }
  
  /**
   * 显示警告弹窗
   */
  function showWarning() {
    warningVisible.value = true;
    
    // 1 分钟后自动登出
    warningTimer = setTimeout(() => {
      autoLogout();
    }, warningTime);
  }
  
  /**
   * 自动登出
   */
  async function autoLogout() {
    await logout();
    router.push('/login');
    ElMessage.warning('Your session has expired. Please log in again.');
  }
  
  /**
   * 监听用户活动
   */
  function setupActivityListeners() {
    const events = ['click', 'keydown', 'scroll', 'mousemove', 'touchstart'];
    
    events.forEach(event => {
      window.addEventListener(event, resetTimer);
    });
  }
  
  /**
   * 保持会话活跃
   */
  function stayLoggedIn() {
    resetTimer();
    warningVisible.value = false;
  }
  
  return {
    setupActivityListeners,
    resetTimer,
    stayLoggedIn,
    autoLogout
  };
}
```

### 实现方案 B：后端 Token 过期

在后端配置 Token 过期时间：

```java
// JwtUtils.java
private static final long EXPIRATION = 1000 * 60 * 30; // 30 分钟
```

前端检查 Token 过期时间：

```typescript
// request.ts - 响应拦截器

if (response.code === 401) {
  // Token 已过期
  clearAuthData();
  router.push('/login');
}
```

---

## PBI 5: Password Reset (密码重置)

```typescript
// 前端：ForgotPassword.vue

const steps = [
  '请求验证码', // Step 1
  '验证邮箱',   // Step 2
  '设置新密码'  // Step 3
];

// Step 1: 请求验证码
const handleSendCode = async () => {
  // 1. 验证邮箱
  if (!isValidEmail(form.email)) {
    error.value = "Invalid email format";
    return;
  }
  
  // 2. 检查邮箱是否注册
  const isBanned = await checkEmailExists(form.email);
  if (!isRegistered) {
    error.value = "Email is not registered.";
    return;
  }
  
  // 3. 发送验证码
  await sendVerificationCode({
    email: form.email,
    type: 'RESET_PASSWORD'
  });
  
  // 4. 显示成功提示
  ElMessage.success('Verification code sent');
  
  // 5. 开始 60 秒倒计时
  startCountdown();
  
  // 6. 步骤切换
  currentStep.value = 1;
};

// Step 2: 验证邮箱
const handleVerifyCode = async () => {
  // 1. 验证码格式检查
  if (!form.verificationCode || form.verificationCode.length !== 6) {
    error.value = "Invalid verification code";
    return;
  }
  
  // 2. 后端验证验证码
  try {
    await verifyCode(form.email, form.verificationCode);
    currentStep.value = 2;
  } catch (err) {
    if (err.message.includes('expired')) {
      error.value = "Verification code expired. Please request a new one.";
    } else {
      error.value = "Invalid verification code";
    }
  }
};

// Step 3: 设置新密码
const handleResetPassword = async () => {
  // 1. 验证密码复杂度
  if (!validatePasswordComplexity(form.newPassword)) {
    error.value = "password not meet complexity";
    return;
  }
  
  // 2. 验证两次密码一致
  if (form.newPassword !== form.confirmPassword) {
    error.value = "Passwords do not match";
    return;
  }
  
  // 3. 调用重置密码 API
  try {
    await resetPassword(
      form.email,
      form.verificationCode,
      form.newPassword,
      form.confirmPassword
    );
    
    // 4. 成功提示并跳转
    ElMessage.success('Your password has been reset. Please log in with your new password.');
    setTimeout(() => {
      router.push('/login');
    }, 1500);
  } catch (err) {
    error.value = err.message;
  }
};
```

---

## 验收标准检查清单

### PBI 1 验收
- [ ] 注册页面显示角色选择
- [ ] 邮箱验证码能正确发送
- [ ] 密码复杂度检查工作
- [ ] 注册成功后自动登录
- [ ] 所有错误提示准确

### PBI 2 验收
- [ ] 登录页面表单验证工作
- [ ] 登录成功返回 Token
- [ ] 角色不匹配返回错误
- [ ] 错误密码 5 次后账户锁定
- [ ] 记住我功能保存凭证

### PBI 3 验收
- [ ] 导航栏显示登出按钮
- [ ] 点击登出清除 Token
- [ ] 登出后无法访问受保护页面
- [ ] 不同设备登出互不影响
- [ ] 同浏览器多标签同步登出

### PBI 4 验收
- [ ] 30 分钟无操作自动登出
- [ ] 29 分钟时显示警告
- [ ] 用户操作时计时器重置
- [ ] 多标签时同步超时

### PBI 5 验收
- [ ] 忘记密码链接跳转到重置页
- [ ] 验证码正确发送
- [ ] 验证码过期检查
- [ ] 密码复杂度检查
- [ ] 密码修改后自动清除保存的凭证

---

## 总结

完成这 5 个 PBI 需要：
1. **后端服务** - AuthService, UserService, EmailService
2. **数据库** - users 表, verification_codes 表
3. **前端组件** - Login, Register, ForgotPassword, Profile
4. **前端逻辑** - 会话管理, Token 管理, 路由守卫

预计工作量：
- 后端：40-50 小时
- 前端：30-40 小时
- 测试：10-15 小时
