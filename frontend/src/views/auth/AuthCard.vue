<template>
  <div class="auth-card-wrapper">
    <div class="auth-container" :class="{ active: isSignUp }">
      <!-- 移动端顶部切换栏（保留绿色渐变与眼球，支持上下切换动画） -->

      <div v-if="isMobile" class="mobile-toggle-bar">
        <div class="mobile-toggle-bg">
          <button
            v-if="showGuest"
            type="button"
            class="guest-eye mobile-guest-eye"
            :class="[eyeDirectionClass, { blinking: isEyeBlinking }]"
            aria-label="Continue browsing as guest"
            @click="continueAsGuest"
          >
            <span class="eye-pair" aria-hidden="true">
              <span class="eye"><span class="pupil" /></span>
              <span class="eye"><span class="pupil" /></span>
            </span>
          </button>
          <div v-if="allowRegister" class="switch-tabs">
            <button
              type="button"
              class="switch-tab"
              :class="{ active: !isSignUp }"
              @click="switchToSignIn"
            >Sign In</button>
            <button
              type="button"
              class="switch-tab"
              :class="{ active: isSignUp }"
              @click="switchToSignUp"
            >Sign Up</button>
          </div>
          <p v-else class="mobile-portal-label">{{ loginHeading }}</p>
        </div>
      </div>

      <!-- 登录表单：桌面端始终显示（有动画控制），移动端仅在登录模式下显示 -->
      <div
        v-if="!isMobile || !isSignUp"
        class="form-container sign-in"
        :class="{ 'mobile-only': isMobile }"
      >
        <form @submit.prevent="handleLogin">
          <h1 class="login-title">{{ loginHeading }}</h1>
          <div v-if="authMode === 'customer'" class="form-group specialist-portal-entry">
            <button type="button" class="specialist-portal-btn" @click="goSpecialistPortal">
              I want to login as Specialist
            </button>
          </div>

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

          <div class="form-group">
            <label>Password</label>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="Enter your password"
            />
            <span v-if="loginErrors.password" class="error-text">{{ loginErrors.password }}</span>
          </div>

          <button class="login-btn" :disabled="isLoginLoading" type="submit">
            {{ isLoginLoading ? 'Logging in...' : 'Login' }}
          </button>

          <div class="footer-links">
            <a v-if="allowRegister" href="#" @click.prevent="switchToSignUp">No account? Register now</a>
            <span v-else class="footer-links-spacer" aria-hidden="true" />
            <router-link :to="{ path: '/forgot-password', query: forgotPasswordQuery }">Forgot Password?</router-link>
          </div>

          <div v-if="!allowRegister && authMode === 'specialist'" class="portal-mobile-hint">
            <p class="portal-mobile-hint-title">Need a specialist account?</p>
            <p class="portal-mobile-hint-text">Please contact the ExpertLink administrator.</p>
            <router-link class="portal-back-link" to="/auth">← Back to Customer Login</router-link>
          </div>
          <div v-if="!allowRegister && authMode === 'admin'" class="portal-mobile-hint">
            <p class="portal-mobile-hint-text">Administrator accounts are managed by the system.</p>
            <router-link class="portal-back-link" to="/auth">← Back to Customer Login</router-link>
          </div>
        </form>
      </div>

      <!-- 注册表单：桌面端始终显示（有动画控制），移动端仅在注册模式下显示 -->
      <div
        v-if="allowRegister && (!isMobile || isSignUp)"
        class="form-container sign-up"
        :class="{ 'mobile-only': isMobile }"
      >
        <form @submit.prevent="handleRegister">
          <h1 class="register-title">Create your account</h1>
          <div class="form-group">
            <label>Customer account</label>
            <p class="hint-text">Register a customer account to book expert consultations.</p>
          </div>

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

          <div class="form-group verification-group">
            <label>Verification code</label>
            <div class="verification-row">
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
          </div>
          <span v-if="sendCodeMessage" class="hint-text">{{ sendCodeMessage }}</span>
          <span v-if="registerErrors.verificationCode" class="error-text">{{ registerErrors.verificationCode }}</span>

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

      <!-- 桌面端的左右切换面板（移动端通过 CSS 隐藏） -->
      <div class="toggle-container">
        <div class="toggle">
          <div class="toggle-panel toggle-left">
            <button
              v-if="showGuest"
              type="button"
              class="guest-eye"
              :class="[eyeDirectionClass, { blinking: isEyeBlinking }]"
              aria-label="Continue browsing as guest"
              @click="continueAsGuest"
            >
              <span class="eye-pair" aria-hidden="true">
                <span class="eye"><span class="pupil" /></span>
                <span class="eye"><span class="pupil" /></span>
              </span>
              <span v-if="showGuestHint && isSignUp" class="guest-hint">Click me to browse as guest</span>
            </button>
            <h1>Welcome Back!</h1>
            <p>Enter your personal details to use all of site features</p>
            <button class="hidden" type="button" @click="switchToSignIn">Sign In</button>
          </div>
          <div v-if="allowRegister" class="toggle-panel toggle-right">
            <button
              v-if="showGuest"
              type="button"
              class="guest-eye"
              :class="[eyeDirectionClass, { blinking: isEyeBlinking }]"
              aria-label="Continue browsing as guest"
              @click="continueAsGuest"
            >
              <span class="eye-pair" aria-hidden="true">
                <span class="eye"><span class="pupil" /></span>
                <span class="eye"><span class="pupil" /></span>
              </span>
              <span v-if="showGuestHint && !isSignUp" class="guest-hint">Click me to browse as guest</span>
            </button>
            <h1>Hello, Friend!</h1>
            <p>Register with your personal details to use all of site features</p>
            <button class="hidden" type="button" @click="switchToSignUp">Sign Up</button>
          </div>
          <div v-else-if="authMode === 'specialist'" class="toggle-panel toggle-right toggle-portal-info">
            <button
              v-if="showGuest"
              type="button"
              class="guest-eye"
              :class="[eyeDirectionClass, { blinking: isEyeBlinking }]"
              aria-label="Continue browsing as guest"
              @click="continueAsGuest"
            >
              <span class="eye-pair" aria-hidden="true">
                <span class="eye"><span class="pupil" /></span>
                <span class="eye"><span class="pupil" /></span>
              </span>
              <span v-if="showGuestHint && !isSignUp" class="guest-hint">Click me to browse as guest</span>
            </button>
            <h1>Need a specialist account?</h1>
            <p>Please contact the ExpertLink administrator.</p>
            <router-link class="portal-back-link desktop-back-link" to="/auth">← Back to Customer Login</router-link>
          </div>
          <div v-else class="toggle-panel toggle-right toggle-portal-info">
            <h1>Administrator access</h1>
            <p>Administrator accounts are managed by the system.</p>
            <router-link class="portal-back-link desktop-back-link" to="/auth">← Back to Customer Login</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue';
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { login, register, sendVerificationCode, type LoginPayload, type RegisterPayload } from '@/api/auth';
import { saveAuthData, dispatchSessionActivityEvent } from '@/api/request';
import { useUserStore } from '@/stores/user';
import type { AuthPortalMode } from '@/constants/authPortal';
import { AUTH_PORTAL_PATH } from '@/constants/authPortal';
import {
  clearRememberedCredentials,
  isRememberCredentialsAllowed,
  hasValidRememberedCredentials,
  loadRememberedCredentials,
  saveRememberedCredentials
} from '@/utils/rememberCredentials';

defineOptions({ name: 'AuthCard' });

const BOOKING_FORM_DRAFT_STORAGE_KEY = 'customer.booking.form.draft';
const AI_BOOKING_CONTEXT_STORAGE_KEY = 'ai.booking.context';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const isMobile = ref(false);
const isSignUp = ref(false);

const authMode = computed<AuthPortalMode>(() => {
  const raw = route.meta.authPortal;
  return raw === 'specialist' || raw === 'admin' ? raw : 'customer';
});

const allowRegister = computed(() => authMode.value === 'customer');
const showGuest = computed(() => authMode.value !== 'admin');

const portalLoginRole = computed(() => {
  switch (authMode.value) {
    case 'specialist':
      return 'SPECIALIST';
    case 'admin':
      return 'ADMIN';
    default:
      return 'CUSTOMER';
  }
});

const loginHeading = computed(() => {
  if (authMode.value === 'specialist') return 'Specialist Login';
  if (authMode.value === 'admin') return 'Administrator Login';
  return 'ExpertLink Customer Login';
});

const forgotPasswordQuery = computed<LocationQueryRaw>(() =>
  authMode.value === 'customer' ? {} : { portal: authMode.value }
);

watch(allowRegister, (ok) => {
  if (!ok) {
    isSignUp.value = false;
  }
});

const checkIsMobile = () => {
  isMobile.value = window.innerWidth <= 768;
};

onMounted(() => {
  checkIsMobile();
  window.addEventListener('resize', checkIsMobile);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkIsMobile);
});

function switchToSignUp() {
  if (!allowRegister.value) {
    return;
  }
  isSignUp.value = true;
}

function switchToSignIn() {
  isSignUp.value = false;
}

const isEyeBlinking = ref(false);
const showGuestHint = ref(false);

let blinkTimer: number | null = null;
let blinkPhaseTimer: number | null = null;
let doubleBlinkTimer: number | null = null;
let guestHintTimer: number | null = null;

const eyeDirectionClass = computed(() => (isSignUp.value ? 'look-right' : 'look-left'));

const showGuestHintNow = () => {
  showGuestHint.value = true;
  if (guestHintTimer) {
    window.clearTimeout(guestHintTimer);
  }
  guestHintTimer = window.setTimeout(() => {
    showGuestHint.value = false;
    guestHintTimer = null;
  }, 2600);
};

const scheduleNextBlink = () => {
  if (blinkTimer) {
    window.clearTimeout(blinkTimer);
  }
  const delayMs = 2600 + Math.random() * 4200;
  blinkTimer = window.setTimeout(() => {
    isEyeBlinking.value = true;
    if (blinkPhaseTimer) {
      window.clearTimeout(blinkPhaseTimer);
    }
    blinkPhaseTimer = window.setTimeout(() => {
      isEyeBlinking.value = false;
      blinkPhaseTimer = null;
    }, 140);

    if (doubleBlinkTimer) {
      window.clearTimeout(doubleBlinkTimer);
      doubleBlinkTimer = null;
    }
    if (Math.random() < 0.18) {
      doubleBlinkTimer = window.setTimeout(() => {
        isEyeBlinking.value = true;
        window.setTimeout(() => {
          isEyeBlinking.value = false;
        }, 140);
      }, 220);
    }

    scheduleNextBlink();
  }, delayMs);
};

const continueAsGuest = () => {
  router.push('/customer/search').catch(() => null);
};

watch(isSignUp, () => {
  showGuestHintNow();
});

onBeforeUnmount(() => {
  if (blinkTimer) window.clearTimeout(blinkTimer);
  if (blinkPhaseTimer) window.clearTimeout(blinkPhaseTimer);
  if (doubleBlinkTimer) window.clearTimeout(doubleBlinkTimer);
  if (guestHintTimer) window.clearTimeout(guestHintTimer);
});

const isLoginLoading = ref(false);
const hasRememberedCredentials = ref(false);

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

watch(
  portalLoginRole,
  (r) => {
    loginForm.role = r;
  },
  { immediate: true }
);

function goSpecialistPortal() {
  router.push(AUTH_PORTAL_PATH.specialist).catch(() => null);
}

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

const clearBookingSessionState = (userId?: number | string | null) => {
  if (typeof window === 'undefined') {
    return;
  }

  const scopedUserId = userId === null || userId === undefined ? '' : String(userId);
  [BOOKING_FORM_DRAFT_STORAGE_KEY, AI_BOOKING_CONTEXT_STORAGE_KEY].forEach((key) => {
    window.sessionStorage.removeItem(key);
    if (scopedUserId) {
      window.sessionStorage.removeItem(`${key}:${scopedUserId}`);
    }
  });
};

const askRememberCredentialsChoice = async (): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(
      'Remember account? (Valid for 7 days)',
      'Remember Account',
      {
        confirmButtonText: 'Remember Account',
        cancelButtonText: 'Don\'t Remember',
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
    loginErrors.role = '';
    const emailValid = validateLoginEmail();
    loginErrors.password = loginForm.password ? '' : 'Password is required';

    const roleForPortal = portalLoginRole.value as LoginPayload['role'];
    if (!emailValid || !loginForm.password) {
      return;
    }

    isLoginLoading.value = true;

    const payload: LoginPayload = {
      email: loginForm.email,
      password: loginForm.password,
      role: roleForPortal
    };

    const response = await login(payload);

    const responseRole = String(response.role || '').toUpperCase();
    if (responseRole !== roleForPortal) {
      ElMessage.error('This account does not have access to this portal.');
      return;
    }

    const allowRememberCredentials = isRememberCredentialsAllowed();
    let rememberMe = false;
    let shouldPersistRememberedCredentials = false;

    if (allowRememberCredentials) {
      const alreadyRemembered =
        hasRememberedCredentials.value ||
        (await hasValidRememberedCredentials().catch(() => false));

      if (alreadyRemembered) {
        rememberMe = true;
        hasRememberedCredentials.value = true;
      } else {
        rememberMe = await askRememberCredentialsChoice();
        shouldPersistRememberedCredentials = rememberMe;
      }
    } else {
      rememberMe = false;
    }

    if (rememberMe && allowRememberCredentials && shouldPersistRememberedCredentials) {
      await saveRememberedCredentials(loginForm.email);
      hasRememberedCredentials.value = true;
    } else if (!rememberMe) {
      clearRememberedCredentials();
      hasRememberedCredentials.value = false;
    }

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

    userStore.token = response.token;
    userStore.userInfo = {
      id: response.userId,
      username: response.email,
      nickname: response.displayName,
      email: response.email
    };
    userStore.userRole = mapRoleToUserRole(response.role);

    dispatchSessionActivityEvent();
    clearBookingSessionState(response.userId);
    ElMessage.success('Login successful');

    const targetRoute =
      responseRole === 'CUSTOMER'
        ? '/customer/search'
        : responseRole === 'SPECIALIST'
          ? '/specialist/schedule'
          : '/admin/specialists';
    await router.push(targetRoute);
  } catch (error: unknown) {
    console.error('Login error:', error);
    const raw = String((error as { message?: string })?.message || '');
    if (
      raw.includes('This account does not have access to this portal') ||
      raw.toLowerCase().includes('role not match')
    ) {
      ElMessage.error('This account does not have access to this portal.');
    } else {
      ElMessage.error(raw || 'Login failed');
    }
  } finally {
    isLoginLoading.value = false;
  }
}

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
    clearBookingSessionState(response.userId);
    ElMessage.success('Registration successful, logging in...');

    const target = response.role === 'SPECIALIST' ? '/specialist/schedule' : '/customer/search';
    setTimeout(() => { router.push(target); }, 500);
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to register');
  } finally {
    isRegisterLoading.value = false;
  }
}

onMounted(() => {
  if (isRememberCredentialsAllowed()) {
    loadRememberedCredentials()
      .then(payload => {
        if (payload?.email) {
          loginForm.email = payload.email;
          hasRememberedCredentials.value = true;
        } else {
          hasRememberedCredentials.value = false;
        }
      })
      .catch(() => {
        clearRememberedCredentials();
        hasRememberedCredentials.value = false;
      });
  } else {
    clearRememberedCredentials();
    hasRememberedCredentials.value = false;
  }

  showGuestHintNow();
  scheduleNextBlink();
});
</script>

<style>
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
  width: 960px;           
  max-width: 100%;
  min-height: 680px;      
}

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
  overflow-y: auto;       
}

.form-container form {
  width: 100%;
  max-width: 100%;
}

.sign-in {
  left: 0;
  width: 50%;
  z-index: 2;
  transform: translateX(0);
  opacity: 1;
  transition: all 0.6s ease-in-out;
}

.auth-container.active .sign-in {
  transform: translateX(100%);
  opacity: 0;
  pointer-events: none;
}

.sign-up {
  left: 0;
  width: 50%;
  opacity: 0;
  z-index: 1;
  transform: translateX(0);   
  pointer-events: none;
  transition: all 0.6s ease-in-out;
}

.auth-container.active .sign-up {
  transform: translateX(100%);       
  opacity: 1;
  z-index: 5;
  pointer-events: auto;
}

@keyframes move {
  0%, 49.99% { opacity: 0; z-index: 1; }
  50%, 100% { opacity: 1; z-index: 5; }
}

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
  justify-content: center; 
}

.footer-links a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.footer-links a:hover {
  color: var(--color-primary-hover);
}

.specialist-portal-entry {
  margin-bottom: 4px;
}

.specialist-portal-btn {
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  color: var(--color-primary);
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.specialist-portal-btn:hover {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.footer-links-spacer {
  min-width: 1px;
}

.mobile-portal-label {
  margin: 0;
  color: white;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
}

.portal-mobile-hint {
  margin-top: 20px;
  text-align: center;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-muted);
}

.portal-mobile-hint-title {
  margin: 0 0 6px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-size: 15px;
}

.portal-mobile-hint-text {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.portal-back-link {
  color: var(--color-primary);
  font-weight: 600;
  text-decoration: none;
  font-size: 14px;
}

.portal-back-link:hover {
  text-decoration: underline;
}

.toggle-portal-info h1 {
  font-size: 22px;
}

.toggle-portal-info p {
  font-size: 14px;
  line-height: 1.5;
  max-width: 280px;
}

.desktop-back-link {
  margin-top: 14px;
  display: inline-block;
  color: white !important;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.55);
}

.desktop-back-link:hover {
  opacity: 0.92;
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

.guest-eye {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px 14px;
  margin: 0 auto 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.12);
  color: white;
  cursor: pointer;
  transition: transform 180ms ease, background 180ms ease, border-color 180ms ease;
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.guest-eye:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.55);
}

.guest-eye:active {
  transform: translateY(0px) scale(0.99);
}

.eye-pair {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.eye {
  width: 22px;
  height: 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  position: relative;
  overflow: hidden;
  transform-origin: 50% 50%;
  transition: transform 140ms ease;
  box-shadow: inset 0 -2px 0 rgba(0, 0, 0, 0.08);
}

.guest-eye.blinking .eye {
  transform: scaleY(0.08);
}

.pupil {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #111827;
  transform: translate(-50%, -50%);
  transition: transform 220ms ease;
}

.pupil::after {
  content: '';
  position: absolute;
  left: 1px;
  top: 1px;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
}

.guest-eye.look-left .pupil {
  transform: translate(-50%, -50%) translateX(-4px);
}

.guest-eye.look-right .pupil {
  transform: translate(-50%, -50%) translateX(4px);
}

.guest-hint {
  position: absolute;
  left: calc(100% + 10px);
  top: 50%;
  transform: translateY(-50%);
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.18);
  pointer-events: none;
  animation: guestHintPop 220ms ease-out;
}

.guest-hint::before {
  content: '';
  position: absolute;
  right: 100%;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-top: 7px solid transparent;
  border-bottom: 7px solid transparent;
  border-right: 8px solid rgba(255, 255, 255, 0.96);
}

@keyframes guestHintPop {
  from {
    transform: translateY(-50%) scale(0.96);
    opacity: 0;
  }
  to {
    transform: translateY(-50%) scale(1);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .auth-card-wrapper {
    background: var(--color-bg-page); 
  }

  .auth-container {
    width: 100%;
    min-height: 100vh;
    border-radius: 0;
    box-shadow: none;
    display: flex;
    flex-direction: column;
  }

  .form-container {
    position: relative !important;
    width: 100% !important;
    left: auto !important;
    transform: none !important;
    opacity: 1 !important;
    pointer-events: auto !important;
    z-index: 2 !important;
    padding: 20px 24px;
    height: auto;
    overflow-y: visible;
    transition: none;
  }

  .sign-in,
  .sign-up {
    position: relative !important;
    width: 100% !important;
    left: auto !important;
    transform: none !important;
    opacity: 1 !important;
    pointer-events: auto !important;
    z-index: 2 !important;
    transition: none;
  }

  .toggle-container {
    display: none;
  }

  .mobile-toggle-bar {
    width: 100%;
    background: linear-gradient(135deg, var(--color-primary-light, #5c6bc0), var(--color-primary, #512da8));
    padding: 24px 24px 20px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  }

  .mobile-toggle-bg {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .mobile-guest-eye {
    display: flex;
    margin: 0;
    border: 1px solid rgba(255,255,255,0.35);
    background: rgba(255,255,255,0.12);
    color: white;
  }

  .switch-tabs {
    display: flex;
    background: rgba(255,255,255,0.15);
    border-radius: 30px;
    padding: 4px;
    width: 100%;
    max-width: 280px;
  }

  .switch-tab {
    flex: 1;
    padding: 10px 0;
    border-radius: 30px;
    border: none;
    background: transparent;
    color: rgba(255,255,255,0.8);
    font-weight: 600;
    font-size: 15px;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    z-index: 1;
  }

  .switch-tab.active {
    background: white;
    color: var(--color-primary, #512da8);
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }

  .form-group {
    margin-bottom: 20px;
  }

  .form-group input {
    padding: 14px 14px;
    font-size: 16px;            
  }

  .role-selector {
    gap: 8px;
  }
  .role-btn {
    min-width: 70px;
    padding: 12px 8px;
    font-size: 14px;
  }

  .verification-group .verification-row {
    display: flex;
    gap: 8px;
    margin-top: 6px;
  }
  .verification-group .verification-row input {
    flex: 1;
  }
  .code-btn {
    white-space: nowrap;
    padding: 12px 10px;
    font-size: 13px;
  }

  .login-btn,
  .register-btn {
    padding: 16px 24px;
    min-height: 50px;
    font-size: 18px;
    margin-top: 12px;
  }

  .footer-links {
    flex-direction: column;
    align-items: center;
    gap: 14px;
    margin-top: 28px;
  }
  .footer-links a {
    padding: 8px 0;
    font-size: 14px;
  }

  .guest-hint {
    display: none;
  }
  
  .sign-in .footer-links,
  .sign-up .footer-links {
    flex-direction: column;
    align-items: center;
    gap: 16px;
    margin-top: 24px;
    padding-bottom: 20px;
  }

  .sign-in .footer-links a,
  .sign-up .footer-links a {
    display: inline-block;
    padding: 12px 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-primary);
    text-decoration: none;
    border-bottom: 2px solid transparent;
    transition: border-color 0.2s ease;
  }

  .sign-in .footer-links a:hover,
  .sign-up .footer-links a:hover {
    border-bottom-color: var(--color-primary);
  }
}
</style>
