<template>
  <div class="register-container">
    <div class="register-card">
      <h1 class="register-title">Create your account</h1>

      <div class="form-group">
        <label>Select role</label>
        <div class="role-selector">
          <button
            v-for="option in roles"
            :key="option.value"
            :class="['role-btn', { active: form.role === option.value }]"
            @click="form.role = option.value"
          >
            {{ option.label }}
          </button>
        </div>
        <span v-if="errors.role" class="error-text">{{ errors.role }}</span>
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

defineOptions({ name: 'AuthRegister' });

const router = useRouter();
const isLoading = ref(false);
const isCodeSending = ref(false);
const countdown = ref(0);
const sendCodeMessage = ref('');

const roles = [
  { label: 'Customer', value: 'CUSTOMER' },
  { label: 'Specialist', value: 'SPECIALIST' }
];

const form = reactive({
  role: '',
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
});

const errors = reactive({
  role: '',
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
  errors.role = form.role ? '' : 'Please select a role first.';
  if (!form.role) {
    return;
  }

  if (!validateEmail()) {
    return;
  }

  isCodeSending.value = true;
  sendCodeMessage.value = '';

  try {
    await sendVerificationCode({
      email: form.email,
      role: form.role as 'CUSTOMER' | 'SPECIALIST',
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
  if (!form.role || !form.email || !form.verificationCode || !form.password || !form.confirmPassword) {
    errors.role = form.role ? '' : 'Please select a role first.';
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
      role: form.role as 'CUSTOMER' | 'SPECIALIST',
      verificationCode: form.verificationCode,
      password: form.password,
      confirmPassword: form.confirmPassword
    };

    const response = await register(payload);
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 420px;
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.1);
}
.register-title {
  color: #333;
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
}
.form-group input {
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 14px;
}
.error-text {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
  display: block;
}
.hint-text {
  display: block;
  color: #4a90e2;
  font-size: 12px;
  margin-bottom: 8px;
}
.role-selector {
  display: flex;
  gap: 10px;
}
.role-btn {
  flex: 1;
  border: 1px solid #ddd;
  color: #333;
  background: #fff;
  border-radius: 4px;
  padding: 8px;
  cursor: pointer;
}
.role-btn.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
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
  border: 1px solid #667eea;
  color: #667eea;
  background: #fff;
  border-radius: 4px;
  padding: 8px 12px;
  cursor: pointer;
}
.register-btn {
  width: 100%;
  border: none;
  border-radius: 6px;
  padding: 12px;
  background: #667eea;
  color: white;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}
.footer-links {
  margin-top: 14px;
  text-align: center;
}
.footer-links a {
  color: #667eea;
}
</style>
