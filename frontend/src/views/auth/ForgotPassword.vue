<template>
  <div class="forgot-password-container">
    <div class="card">
      <h1 class="title">Reset Password</h1>

      <!-- 步骤指示器 -->
      <div class="steps">
        <div :class="['step', { active: currentStep >= 1, completed: currentStep > 1 }]">
          <span class="step-number">1</span>
          <span class="step-label">Verify Email</span>
        </div>
        <div :class="['step', { active: currentStep >= 2, completed: currentStep > 2 }]">
          <span class="step-number">2</span>
          <span class="step-label">Enter Code</span>
        </div>
        <div :class="['step', { active: currentStep >= 3 }]">
          <span class="step-number">3</span>
          <span class="step-label">New Password</span>
        </div>
      </div>

      <!-- Step 1: 输入邮箱 -->
      <div v-if="currentStep === 1" class="step-content">
        <div class="form-group">
          <label>Email Address</label>
          <input
            v-model="email"
            type="email"
            placeholder="Enter your registered email"
            :disabled="loading"
          />
          <span v-if="errors.email" class="error">{{ errors.email }}</span>
        </div>
        <button class="btn-primary" :disabled="loading || !email" @click="sendCode">
          {{ loading ? 'Sending...' : 'Send Verification Code' }}
        </button>
        <div class="back-link">
          <router-link to="/auth">Back to Login</router-link>
        </div>
      </div>

      <!-- Step 2: 输入验证码 -->
      <div v-if="currentStep === 2" class="step-content">
        <div class="form-group">
          <label>Verification Code</label>
          <input
            v-model="verificationCode"
            type="text"
            placeholder="6-digit code"
            maxlength="6"
            :disabled="loading"
          />
          <span v-if="errors.code" class="error">{{ errors.code }}</span>
        </div>
        <div class="button-group">
          <button class="btn-secondary" :disabled="loading" @click="currentStep = 1">
            Back
          </button>
          <button class="btn-primary" :disabled="loading || !verificationCode" @click="verifyCode">
            {{ loading ? 'Verifying...' : 'Verify Code' }}
          </button>
        </div>
        <div class="resend-link">
          <a href="javascript:void(0)" @click="resendCode" :class="{ disabled: countdown > 0 }">
            {{ countdown > 0 ? `Resend in ${countdown}s` : 'Resend Code' }}
          </a>
        </div>
      </div>

      <!-- Step 3: 设置新密码 -->
      <div v-if="currentStep === 3" class="step-content">
        <div class="form-group">
          <label>New Password</label>
          <input
            v-model="newPassword"
            type="password"
            placeholder="At least 8 chars, uppercase, lowercase, number"
          />
          <span v-if="errors.newPassword" class="error">{{ errors.newPassword }}</span>
        </div>
        <div class="form-group">
          <label>Confirm Password</label>
          <input
            v-model="confirmPassword"
            type="password"
            placeholder="Re-enter new password"
          />
          <span v-if="errors.confirmPassword" class="error">{{ errors.confirmPassword }}</span>
        </div>
        <div class="button-group">
          <button class="btn-secondary" :disabled="loading" @click="currentStep = 2">
            Back
          </button>
          <button class="btn-primary" :disabled="loading || !newPassword || !confirmPassword" @click="resetPassword">
            {{ loading ? 'Resetting...' : 'Reset Password' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { sendVerificationCode } from '@/api/auth';
import request from '@/api/request';
import { clearRememberedCredentials } from '@/utils/rememberCredentials';

const router = useRouter();
const loading = ref(false);
const currentStep = ref(1);
const email = ref('');
const verificationCode = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const countdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const errors = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
});

function validateEmail() {
  if (!email.value) {
    errors.email = 'Email is required';
    return false;
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email.value)) {
    errors.email = 'Invalid email format';
    return false;
  }
  errors.email = '';
  return true;
}

function validatePassword() {
  const pattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
  if (!newPassword.value) {
    errors.newPassword = 'Password is required';
    return false;
  }
  if (!pattern.test(newPassword.value)) {
    errors.newPassword = 'Password must be at least 8 characters with uppercase, lowercase, and a number';
    return false;
  }
  errors.newPassword = '';
  return true;
}

async function sendCode() {
  if (!validateEmail()) return;
  loading.value = true;
  try {
    await sendVerificationCode({
      email: email.value,
      type: 'RESET_PASSWORD'   // 关键：类型为 RESET_PASSWORD
    });
    ElMessage.success('Verification code sent to your email');
    currentStep.value = 2;
    startCountdown();
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to send code');
  } finally {
    loading.value = false;
  }
}

async function verifyCode() {
  if (!verificationCode.value) {
    errors.code = 'Verification code is required';
    return;
  }
  errors.code = '';
  loading.value = true;
  try {
    await request.post('/auth/reset-password/verify', {
      email: email.value,
      verificationCode: verificationCode.value
    });
    ElMessage.success('Code verified');
    currentStep.value = 3;
  } catch (error: any) {
    ElMessage.error(error.message || 'Invalid verification code');
  } finally {
    loading.value = false;
  }
}

async function resetPassword() {
  if (!validatePassword()) return;
  if (newPassword.value !== confirmPassword.value) {
    errors.confirmPassword = 'Passwords do not match';
    return;
  }
  errors.confirmPassword = '';
  loading.value = true;
  try {
    await request.post('/auth/reset-password/update', {
      email: email.value,
      verificationCode: verificationCode.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value
    });
    clearRememberedCredentials();
    ElMessage.success('Password reset successfully. Please login with your new password.');
    router.push('/auth');
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to reset password');
  } finally {
    loading.value = false;
  }
}

function startCountdown() {
  countdown.value = 60;
  if (timer) clearInterval(timer);
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer!);
      timer = null;
    }
  }, 1000);
}

async function resendCode() {
  if (countdown.value > 0) {
    ElMessage.warning(`Please wait ${countdown.value} seconds`);
    return;
  }
  await sendCode();
}
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--color-bg-page);
}
.card {
  width: 450px;
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: 0 10px 28px var(--color-shadow);
}
.title {
  text-align: center;
  margin-bottom: 24px;
  color: var(--color-text-primary);
}
.steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 32px;
}
.step {
  flex: 1;
  text-align: center;
  position: relative;
}
.step:not(:last-child):after {
  content: '';
  position: absolute;
  top: 15px;
  right: -50%;
  width: 100%;
  height: 2px;
  background: var(--color-border);
  z-index: 1;
}
.step.completed:not(:last-child):after {
  background: var(--color-primary);
}
.step-number {
  display: inline-block;
  width: 30px;
  height: 30px;
  line-height: 30px;
  border-radius: 50%;
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  position: relative;
  z-index: 2;
}
.step.active .step-number {
  background: var(--color-primary);
  color: white;
}
.step.completed .step-number {
  background: var(--color-success);
  color: white;
}
.step-label {
  display: block;
  font-size: 12px;
  margin-top: 8px;
  color: var(--color-text-secondary);
}
.step.active .step-label {
  color: var(--color-primary);
  font-weight: 500;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.form-group input {
  width: 100%;
  padding: 10px 12px;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-primary);
}
.error {
  display: block;
  color: var(--color-danger);
  font-size: 12px;
  margin-top: 4px;
}
.btn-primary, .btn-secondary {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-primary {
  background: var(--color-primary);
  color: white;
}
.btn-primary:disabled {
  background: var(--color-bg-muted);
  cursor: not-allowed;
}
.btn-secondary {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
}
.button-group {
  display: flex;
  gap: 12px;
}
.button-group .btn-primary, .button-group .btn-secondary {
  flex: 1;
}
.back-link, .resend-link {
  margin-top: 16px;
  text-align: center;
}
.back-link a, .resend-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-size: 14px;
}
.resend-link a.disabled {
  color: var(--color-text-tertiary);
  cursor: not-allowed;
  pointer-events: none;
}
</style>
