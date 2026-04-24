<template>
  <div class="auth-card-wrapper">
    <div class="auth-container" :class="{ active: isSignUp }">
      <!-- 登录面板 -->
      <div class="form-container sign-in">
        <!-- 把 Login.vue 的模板内容完整搬过来，注意修改 v-model 绑定变量名 -->
        <form @submit.prevent="handleLogin">
          <h1 class="login-title">Appointment Platform Login</h1>
          <!-- 角色选择 -->
          <div class="form-group">
            <label>Select Role</label>
            <div class="role-selector">
              <button
                v-for="roleOption in roles"
                :key="roleOption.value"
                :class="['role-btn', { active: loginForm.role === roleOption.value }]"
                type="button"
                @click="loginForm.role = roleOption.value"
              >
                {{ roleOption.label }}
              </button>
            </div>
            <span v-if="loginErrors.role" class="error-text">{{ loginErrors.role }}</span>
          </div>

          <!-- Email -->
          <div class="form-group">
            <label>Email Address</label>
            <input
              v-model="loginForm.email"
              type="email"
              placeholder="Enter your email"
              @blur="validateLoginEmail"
            />
            <span v-if="loginErrors.email" class="error-text">{{ loginErrors.email }}</span>
          </div>

          <!-- Password -->
          <div class="form-group">
            <label>Password</label>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="Enter your password"
            />
            <span v-if="loginErrors.password" class="error-text">{{ loginErrors.password }}</span>
          </div>

          <div class="form-group remember-me">
            <label class="checkbox-label">
              <input type="checkbox" v-model="rememberEmail" />
              <span>Remember my email</span>
            </label>
          </div>

          <button class="login-btn" :disabled="isLoginLoading" type="submit">
            {{ isLoginLoading ? 'Logging in...' : 'Login' }}
          </button>

          <div class="footer-links">
            <!-- 注意：这里改为触发切换，不再使用 router-link -->
            <a href="#" @click.prevent="switchToSignUp">No account? Register now</a>
            <router-link to="/forgot-password">Forgot Password?</router-link>
          </div>
        </form>
      </div>

      <!-- 注册面板 -->
      <div class="form-container sign-up">
        <form @submit.prevent="handleRegister">
          <h1 class="register-title">Create your account</h1>
          <div class="form-group">
            <label>Customer account</label>
            <p class="hint-text">Register a customer account to book expert consultations.</p>
          </div>

          <!-- Email -->
          <div class="form-group">
            <label>Email address</label>
            <input
              v-model.trim="registerForm.email"
              type="email"
              placeholder="Enter your email"
              @blur="validateRegisterEmail"
            />
            <span v-if="registerErrors.email" class="error-text">{{ registerErrors.email }}</span>
          </div>

          <!-- Verification code -->
          <div class="form-group verification-group">
            <label>Verification code</label>
            <input
              v-model.trim="registerForm.verificationCode"
              type="text"
              placeholder="Enter 6-digit code"
              maxlength="6"
            />
            <button
              class="code-btn"
              :disabled="isCodeSending || countdown > 0"
              type="button"
              @click="getVerificationCode"
            >
              {{ countdown > 0 ? `${countdown} seconds to resend` : 'Send code' }}
            </button>
          </div>
          <span v-if="sendCodeMessage" class="hint-text">{{ sendCodeMessage }}</span>
          <span v-if="registerErrors.verificationCode" class="error-text">{{ registerErrors.verificationCode }}</span>

          <!-- Password -->
          <div class="form-group">
            <label>Password</label>
            <input
              v-model="registerForm.password"
              type="password"
              placeholder="At least 8 chars, uppercase, lowercase and number"
              @blur="validateRegisterPassword"
            />
            <span v-if="registerErrors.password" class="error-text">{{ registerErrors.password }}</span>
          </div>

          <!-- Confirm Password -->
          <div class="form-group">
            <label>Confirm Password</label>
            <input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="Re-enter password"
            />
            <span v-if="registerErrors.confirmPassword" class="error-text">{{ registerErrors.confirmPassword }}</span>
          </div>

          <button class="register-btn" :disabled="isRegisterLoading" type="submit">
            {{ isRegisterLoading ? 'Registering...' : 'Register & Login' }}
          </button>

          <div class="footer-links">
            <a href="#" @click.prevent="switchToSignIn">Already have an account? Login</a>
          </div>
        </form>
      </div>

      <!-- 右侧切换面板（Toggle） -->
      <div class="toggle-container">
        <div class="toggle">
          <div class="toggle-panel toggle-left">
            <h1>Welcome Back!</h1>
            <p>Enter your personal details to use all of site features</p>
            <button class="hidden" type="button" @click="switchToSignIn">Sign In</button>
          </div>
          <div class="toggle-panel toggle-right">
            <h1>Hello, Friend!</h1>
            <p>Register with your personal details to use all of site features</p>
            <button class="hidden" type="button" @click="switchToSignUp">Sign Up</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { login, register, sendVerificationCode, type LoginPayload, type RegisterPayload } from '@/api/auth';
import { saveAuthData, saveRememberedEmail, getRememberedEmail, dispatchSessionActivityEvent } from '@/api/request';
import { useUserStore } from '@/stores/user';

defineOptions({ name: 'AuthCard' });

const router = useRouter();
const userStore = useUserStore();

// ---------- 切换状态 ----------
const isSignUp = ref(false); // false=显示登录，true=显示注册

function switchToSignUp() {
  isSignUp.value = true;
}

function switchToSignIn() {
  isSignUp.value = false;
}

// ---------- 登录相关 ----------
const isLoginLoading = ref(false);
const rememberEmail = ref(false);
const rememberSession = ref(false);

const roles = [
  { label: 'CUSTOMER', value: 'CUSTOMER' },
  { label: 'SPECIALIST', value: 'SPECIALIST' },
  { label: 'ADMIN', value: 'ADMIN' }
];

const loginForm = reactive({
  email: '',
  password: '',
  role: 'CUSTOMER'
});

const loginErrors = reactive({
  email: '',
  password: '',
  role: ''
});

function validateLoginPassword() { /* 如果需要 */ }
function validateLoginEmail() {
  if (!loginForm.email) {
    loginErrors.email = 'Email is required';
    return false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(loginForm.email)) {
    loginErrors.email = 'Please enter a valid email address';
    return false;
  }

  loginErrors.email = '';
  return true;
}

const mapRoleToUserRole = (role: string) => {
  switch (role?.toUpperCase()) {
    case 'ADMIN':
      return 'admin';
    case 'SPECIALIST':
      return 'specialist';
    case 'CUSTOMER':
    default:
      return 'customer';
  }
};
const askRememberMeChoice = async (defaultChecked: boolean): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(
      'Because Login successful, do you want to remember this account?',
      'Remember Me',
      {
        confirmButtonText: 'Remember me',
        cancelButtonText: 'Not remember',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    );
    return true;
  } catch {
    return false;
  }
};

async function handleLogin() {
  try {
    loginErrors.role = loginForm.role ? '' : 'Please select a role first.';
    const emailValid = validateLoginEmail();
    loginErrors.password = loginForm.password ? '' : 'Password is required';

    if (!loginForm.role || !emailValid || !loginForm.password) {
      return;
    }

    isLoginLoading.value = true;

    const payload: LoginPayload = {
      email: loginForm.email,
      password: loginForm.password,
      role: loginForm.role as 'CUSTOMER' | 'SPECIALIST' | 'ADMIN'
    };

    const response = await login(payload);
    const rememberChoice = await askRememberMeChoice(rememberSession.value);
    const rememberMe = Boolean(rememberChoice);

    saveAuthData(
      response.token,
      response.refreshToken,
      {
        userId: response.userId,
        role: response.role,
        email: response.email,
        displayName: response.displayName
      },
      rememberMe
    );

    rememberSession.value = rememberMe;

    userStore.token = response.token;
    userStore.userInfo = {
      id: response.userId,
      username: response.email,
      nickname: response.displayName,
      email: response.email
    };
    userStore.userRole = mapRoleToUserRole(response.role);

    if (rememberEmail.value) {
      saveRememberedEmail(loginForm.email);
    } else {
      saveRememberedEmail('');
    }

    dispatchSessionActivityEvent();
    ElMessage.success('Login successful');

    const targetRoute = loginForm.role === 'CUSTOMER' ? '/customer/search' :
                        loginForm.role === 'SPECIALIST' ? '/specialist/schedule' :
                        '/admin/specialists';
    await router.push(targetRoute);
  } catch (error: any) {
    console.error('Login error:', error);
    ElMessage.error(error?.message || 'Login failed');
  } finally {
    isLoginLoading.value = false;
  }
}

// ---------- 注册相关 ----------
const isRegisterLoading = ref(false);
const isCodeSending = ref(false);
const countdown = ref(0);
const sendCodeMessage = ref('');

const registerForm = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
});

const registerErrors = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
});

function validateRegisterPassword() {
  if (!registerForm.password) {
    registerErrors.password = 'Password is required';
    return false;
  }
  const pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
  if (!pattern.test(registerForm.password)) {
    registerErrors.password = 'Password must be at least 8 characters with uppercase, lowercase, and a number';
    return false;
  }
  registerErrors.password = '';
  return true;
}

function validateRegisterEmail() {
  if (!registerForm.email) {
    registerErrors.email = 'Email is required';
    return false;
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(registerForm.email)) {
    registerErrors.email = 'Invalid email format';
    return false;
  }
  registerErrors.email = '';
  return true;
}

async function getVerificationCode() {
  if (!validateRegisterEmail()) {
    return;
  }

  isCodeSending.value = true;
  sendCodeMessage.value = '';

  try {
    await sendVerificationCode({
      email: registerForm.email,
      role: 'CUSTOMER',
      type: 'REGISTER'
    });

    sendCodeMessage.value = 'Verification code sent';
    countdown.value = 60;

    const timer = setInterval(() => {
      countdown.value -= 1;
      if (countdown.value <= 0) {
        clearInterval(timer);
        countdown.value = 0;
      }
    }, 1000);
  } catch (error: any) {
    sendCodeMessage.value = '';
    ElMessage.error(error.message || 'Failed to send verification code');
  } finally {
    isCodeSending.value = false;
  }
}

async function handleRegister() {
  if (!registerForm.email || !registerForm.verificationCode || !registerForm.password || !registerForm.confirmPassword) {
    registerErrors.email = registerForm.email ? '' : 'Please enter every field';
    registerErrors.verificationCode = registerForm.verificationCode ? '' : 'Please enter every field';
    registerErrors.password = registerForm.password ? '' : 'Please enter every field';
    registerErrors.confirmPassword = registerForm.confirmPassword ? '' : 'Please enter every field';
    ElMessage.error('Please enter every field');
    return;
  }

  if (!validateRegisterEmail() || !validateRegisterPassword()) {
    return;
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    registerErrors.confirmPassword = 'Password not meet complexity or password not match';
    ElMessage.error('Password not meet complexity or password not match');
    return;
  }

  isRegisterLoading.value = true;

  try {
    const payload: RegisterPayload = {
      email: registerForm.email,
      verificationCode: registerForm.verificationCode,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      role: 'CUSTOMER'
    };

    const response = await register(payload);
    saveAuthData(
      response.token,
      response.refreshToken,
      {
        userId: response.userId,
        role: response.role,
        email: response.email,
        displayName: response.displayName
      },
      false
    );
    userStore.token = response.token;
    userStore.userInfo = {
      id: response.userId,
      username: response.email,
      nickname: response.displayName,
      email: response.email
    };
    userStore.userRole = response.role.toLowerCase() as any;
    dispatchSessionActivityEvent();
    ElMessage.success('Registration successful, logging in...');

    const target = response.role === 'SPECIALIST' ? '/specialist/schedule' : '/customer/search';
    setTimeout(() => { router.push(target); }, 500);
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to register');
  } finally {
    isRegisterLoading.value = false;
  }
}
// onMounted 中处理记住邮箱
onMounted(() => {
  const remembered = getRememberedEmail();
  if (remembered) {
    loginForm.email = remembered;
    rememberEmail.value = true;
  }
  rememberSession.value = localStorage.getItem('rememberMe') === 'true';
});
</script>

<style>
/* 注意：这里不使用 scoped，以确保切换动画正确应用 */
.auth-card-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg-page);
}

.auth-container {
  background-color: var(--color-bg-surface);
  border-radius: 30px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
  width: 960px;           /* 稍微加宽，给注册表单更多空间 */
  max-width: 100%;
  min-height: 680px;      /* 增加最小高度，防止内容溢出 */
}

/* 表单容器绝对定位 */
.form-container {
  position: absolute;
  top: 0;
  height: 100%;
  transition: all 0.6s ease-in-out;
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-sizing: border-box;
  overflow-y: auto;       /* 内容过多时滚动，避免溢出 */
}

.form-container form {
  width: 100%;
  max-width: 100%;
}

/* 登录面板（默认显示） */
.sign-in {
  left: 0;
  width: 50%;
  z-index: 2;
  transform: translateX(0);
  opacity: 1;
  transition: all 0.6s ease-in-out;
}

/* 切换到注册时 */
.auth-container.active .sign-in {
  transform: translateX(100%);
  opacity: 0;
  pointer-events: none;
}

/* 注册面板（默认在左侧外面） */
.sign-up {
  left: 0;
  width: 50%;
  opacity: 0;
  z-index: 1;
  transform: translateX(0);   /* ✅ 关键：一开始就在外面 */
  pointer-events: none;
  transition: all 0.6s ease-in-out;
}

/* 激活注册 */
.auth-container.active .sign-up {
  transform: translateX(100%);       /* ✅ 滑入 */
  opacity: 1;
  z-index: 5;
  pointer-events: auto;
}

@keyframes move {
  0%, 49.99% { opacity: 0; z-index: 1; }
  50%, 100% { opacity: 1; z-index: 5; }
}

/* 切换面板 */
.toggle-container {
  position: absolute;
  top: 0;
  left: 50%;
  width: 50%;
  height: 100%;
  overflow: hidden;
  transition: all 0.6s ease-in-out;
  border-radius: 150px 0 0 100px;
  z-index: 1000;
}

.auth-container.active .toggle-container {
  transform: translateX(-100%);
  border-radius: 0 150px 100px 0;
}

.toggle {
  background: linear-gradient(to right, var(--color-primary-light, #5c6bc0), var(--color-primary, #512da8));
  color: white;
  position: relative;
  left: -100%;
  height: 100%;
  width: 200%;
  transform: translateX(0);
  transition: all 0.6s ease-in-out;
}

.auth-container.active .toggle {
  transform: translateX(50%);
}

.toggle-panel {
  position: absolute;
  width: 50%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 0 30px;
  text-align: center;
  top: 0;
  transform: translateX(0);
  transition: all 0.6s ease-in-out;
}

.toggle-left {
  transform: translateX(-200%);
}

.auth-container.active .toggle-left {
  transform: translateX(0);
}

.toggle-right {
  right: 0;
  transform: translateX(0);
}

.auth-container.active .toggle-right {
  transform: translateX(200%);
}

/* ---------- 通用表单样式（合并去重） ---------- */
.login-title, .register-title {
  text-align: center;
  color: var(--color-text-primary);
  margin-bottom: 24px;
  font-size: 24px;
  font-weight: 700;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text-primary);
  font-weight: 500;
  font-size: 14px;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--color-text-primary);
  transition: border-color var(--transition-fast), background var(--transition-fast);
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-rgb), 0.1);
}

.form-group input::placeholder {
  color: var(--color-text-tertiary);
}

.role-selector {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.role-btn {
  flex: 1;
  padding: 10px 8px;
  background: var(--color-bg-surface);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 14px;
  text-align: center;
}

.role-btn:hover {
  border-color: var(--color-primary);
}

.role-btn.active {
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-color: var(--color-primary);
}

.error-text {
  display: block;
  color: var(--color-danger);
  font-size: 12px;
  margin-top: 4px;
}

.hint-text {
  display: block;
  color: var(--color-success);
  font-size: 12px;
  margin-bottom: 8px;
}

.verification-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.verification-group input {
  flex: 1;
  margin-right: 0;
}

.code-btn {
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
  background: var(--color-bg-surface);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  font-size: 13px;
}

.code-btn:hover:not(:disabled) {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-btn, .register-btn {
  width: 100%;
  padding: 12px;
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background var(--transition-fast);
  margin-top: 16px;
}

.login-btn:hover:not(:disabled), .register-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.login-btn:disabled, .register-btn:disabled {
  background: var(--color-bg-muted);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.footer-links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  font-size: 14px;
}

.sign-up .footer-links {
  justify-content: center; /* 注册面板只有一个链接，居中 */
}

.footer-links a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.footer-links a:hover {
  color: var(--color-primary-hover);
}

.remember-me {
  display: flex;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input {
  width: auto;
  margin: 0;
}

/* 隐藏切换面板按钮默认样式 */
.toggle-panel button.hidden {
  background: transparent;
  border: 1px solid white;
  color: white;
  padding: 10px 30px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 20px;
  transition: all 0.3s;
}

.toggle-panel button.hidden:hover {
  background: white;
  color: var(--color-primary);
}
</style>
