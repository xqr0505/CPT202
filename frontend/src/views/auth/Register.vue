<template>
  <div class="register-container">
    <div class="register-card">
      <h1 class="register-title">Create your account</h1>

      <div class="form-group">
        <label>Customer account</label>
        <p class="hint-text">Register a customer account to book expert consultations.</p>
      </div>

      <div class="form-group">
        <label>Email address</label>
        <input
          v-model.trim="form.email"
          type="email"
          placeholder="Enter your email"
          @blur="validateEmail"
        />
        <span v-if="errors.email" class="error-text">{{ errors.email }}</span>
      </div>

      <div class="form-group verification-group">
        <label>Verification code</label>
        <input
          v-model.trim="form.verificationCode"
          type="text"
          placeholder="Enter 6-digit code"
          maxlength="6"
        />
        <button
          class="code-btn"
          :disabled="isCodeSending || countdown > 0"
          @click="getVerificationCode"
        >
          {{ countdown > 0 ? `${countdown} seconds to resend` : 'Send code' }}
        </button>
      </div>
      <span v-if="sendCodeMessage" class="hint-text">{{ sendCodeMessage }}</span>
      <span v-if="errors.verificationCode" class="error-text">{{ errors.verificationCode }}</span>

      <div class="form-group">
        <label>Password</label>
        <input
          v-model="form.password"
          type="password"
          placeholder="At least 8 chars, uppercase, lowercase and number"
          @blur="validatePassword"
        />
        <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
      </div>

      <div class="form-group">
        <label>Confirm Password</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          placeholder="Re-enter password"
        />
        <span v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</span>
      </div>

      <button class="register-btn" :disabled="isLoading" @click="handleRegister">
        {{ isLoading ? 'Registering...' : 'Register & Login' }}
      </button>

      <div class="footer-links">
        <router-link to="/auth/login">Already have an account? Login</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { register, sendVerificationCode, type RegisterPayload } from '@/api/auth';
import { saveAuthData, dispatchSessionActivityEvent } from '@/api/request';
import { useUserStore } from '@/stores/user';

defineOptions({ name: 'AuthRegister' });

const router = useRouter();
const userStore = useUserStore();
const isLoading = ref(false);
const isCodeSending = ref(false);
const countdown = ref(0);
const sendCodeMessage = ref('');

const form = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
});

const errors = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
});

function validateEmail() {
  if (!form.email) {
    errors.email = 'Email is required';
    return false;
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(form.email)) {
    errors.email = 'Invalid email format';
    return false;
  }
  errors.email = '';
  return true;
}

function validatePassword() {
  if (!form.password) {
    errors.password = 'Password is required';
    return false;
  }
  const pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
  if (!pattern.test(form.password)) {
    errors.password = 'Password must be at least 8 characters with uppercase, lowercase, and a number';
    return false;
  }
  errors.password = '';
  return true;
}

async function getVerificationCode() {
  if (!validateEmail()) {
    return;
  }

  isCodeSending.value = true;
  sendCodeMessage.value = '';

  try {
    await sendVerificationCode({
      email: form.email,
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
  if (!form.email || !form.verificationCode || !form.password || !form.confirmPassword) {
    errors.email = form.email ? '' : 'Please enter every field';
    errors.verificationCode = form.verificationCode ? '' : 'Please enter every field';
    errors.password = form.password ? '' : 'Please enter every field';
    errors.confirmPassword = form.confirmPassword ? '' : 'Please enter every field';
    ElMessage.error('Please enter every field');
    return;
  }

  if (!validateEmail() || !validatePassword()) {
    return;
  }

  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Password not meet complexity or password not match';
    ElMessage.error('Password not meet complexity or password not match');
    return;
  }

  isLoading.value = true;

  try {
    const payload: RegisterPayload = {
      email: form.email,
      verificationCode: form.verificationCode,
      password: form.password,
      confirmPassword: form.confirmPassword
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
    isLoading.value = false;
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--color-bg-page);
}

.register-card {
  width: 420px;
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: 0 10px 28px var(--color-shadow);
  transition: background-color var(--transition-base), box-shadow var(--transition-base);
}

.register-title {
  color: var(--color-text-primary);
  font-weight: 700;
  text-align: center;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  font-weight: 600;
  margin-bottom: 8px;
  display: block;
  color: var(--color-text-primary);
}

.form-group input {
  width: 100%;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-text-primary);
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-rgb), 0.1);
}

.form-group input::placeholder {
  color: var(--color-text-tertiary);
}

.error-text {
  color: var(--color-danger);
  font-size: 12px;
  margin-top: 4px;
  display: block;
}

.hint-text {
  display: block;
  color: var(--color-success);
  font-size: 12px;
  margin-bottom: 8px;
}

.role-selector {
  display: flex;
  gap: 10px;
}

.role-btn {
  flex: 1;
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  background: var(--color-bg-surface);
  border-radius: var(--radius-sm);
  padding: 8px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.role-btn:hover {
  border-color: var(--color-primary);
}

.role-btn.active {
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-color: var(--color-primary);
}

.verification-group {
  display: flex;
  align-items: center;
}

.verification-group input {
  flex: 1;
  margin-right: 10px;
}

.code-btn {
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
  background: var(--color-bg-surface);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.code-btn:hover:not(:disabled) {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.register-btn {
  width: 100%;
  border: none;
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background var(--transition-fast);
}

.register-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.register-btn:disabled {
  background: var(--color-bg-muted);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.footer-links {
  margin-top: 14px;
  text-align: center;
}

.footer-links a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.footer-links a:hover {
  color: var(--color-primary-hover);
}
</style>
