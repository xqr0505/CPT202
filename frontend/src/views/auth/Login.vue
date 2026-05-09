<template>
  <div class="login-container">
    <div class="login-form-card">
      <h1 class="login-title">Appointment Platform Login</h1>

      <div class="form-group">
        <label>Select Role</label>
        <div class="role-selector">
          <button
            v-for="roleOption in roles"
            :key="roleOption.value"
            :class="['role-btn', { active: form.role === roleOption.value }]"
            @click="form.role = roleOption.value"
          >
            {{ roleOption.label }}
          </button>
        </div>
        <span v-if="errors.role" class="error-text">{{ errors.role }}</span>
      </div>

      <div class="form-group">
        <label>Email Address</label>
        <input
          v-model="form.email"
          type="email"
          placeholder="Enter your email"
          @blur="validateEmail"
        />
        <span v-if="errors.email" class="error-text">{{ errors.email }}</span>
      </div>

      <div class="form-group">
        <label>Password</label>
        <input
          v-model="form.password"
          type="password"
          placeholder="Enter your password"
        />
        <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
      </div>

      <div class="form-group remember-me">
        <label class="checkbox-label">
          <input type="checkbox" v-model="rememberEmail" />
          <span>Remember my email</span>
        </label>
      </div>

      <button
        class="login-btn"
        :disabled="isLoading"
        @click="handleLogin"
      >
        {{ isLoading ? 'Logging in...' : 'Login' }}
      </button>

      <div class="footer-links">
        <router-link to="/register">No account? Register now</router-link>
        <router-link to="/forgot-password">Forgot Password?</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { login, type LoginPayload } from '@/api/auth';
import { saveAuthData, saveRememberedEmail, getRememberedEmail, dispatchSessionActivityEvent } from '@/api/request';
import { useUserStore } from '@/stores/user';

defineOptions({ name: 'AuthLogin' });

const router = useRouter();
const userStore = useUserStore();
const isLoading = ref(false);
const rememberEmail = ref(false);
const rememberSession = ref(false);

onMounted(() => {
  const remembered = getRememberedEmail();
  if (remembered) {
    form.email = remembered;
    rememberEmail.value = true;
  }
  rememberSession.value = localStorage.getItem('rememberMe') === 'true';
});

const roles = [
  { label: 'CUSTOMER', value: 'CUSTOMER' },
  { label: 'SPECIALIST', value: 'SPECIALIST' },
  { label: 'ADMIN', value: 'ADMIN' }
];

const form = reactive({
  email: '',
  password: '',
  role: 'CUSTOMER'
});

const errors = reactive({
  email: '',
  password: '',
  role: ''
});

function validateEmail() {
  if (!form.email) {
    errors.email = 'Email is required';
    return false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(form.email)) {
    errors.email = 'Please enter a valid email address';
    return false;
  }

  errors.email = '';
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

const askRememberMeChoice = async (): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(
      'After successful login, will the password be remembered and the login status be maintained?',
      'Remember Me',
      {
        confirmButtonText: 'Remember',
        cancelButtonText: 'Not Remember',
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
    errors.role = form.role ? '' : 'Please select a role first.';
    const emailValid = validateEmail();
    errors.password = form.password ? '' : 'Password is required';

    if (!form.role || !emailValid || !form.password) {
      return;
    }

    isLoading.value = true;

    const payload: LoginPayload = {
      email: form.email,
      password: form.password,
      role: form.role as 'CUSTOMER' | 'SPECIALIST' | 'ADMIN'
    };

    const response = await login(payload);
    const rememberChoice = await askRememberMeChoice();
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
      saveRememberedEmail(form.email);
    } else {
      saveRememberedEmail('');
    }

    dispatchSessionActivityEvent();
    ElMessage.success('Login successful');

    const targetRoute = form.role === 'CUSTOMER' ? '/customer/search' :
                        form.role === 'SPECIALIST' ? '/specialist/schedule' :
                        '/admin/specialists';
    await router.push(targetRoute);
  } catch (error: any) {
    console.error('Login error:', error);
    ElMessage.error(error?.message || 'Login failed');
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--color-bg-page);
}

.login-form-card {
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  padding: 40px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 40px var(--color-shadow);
  transition: background-color var(--transition-base), box-shadow var(--transition-base);
}

.login-title {
  text-align: center;
  color: var(--color-text-primary);
  margin-bottom: 30px;
  font-size: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text-primary);
  font-weight: 500;
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
  margin-bottom: 10px;
}

.role-btn {
  flex: 1;
  padding: 10px;
  background: var(--color-bg-surface);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-primary);
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

.error-text {
  display: block;
  color: var(--color-danger);
  font-size: 12px;
  margin-top: 5px;
}

.login-btn {
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
  margin-top: 20px;
}

.login-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.login-btn:disabled {
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

.footer-links a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.footer-links a:hover {
  color: var(--color-primary-hover);
}
</style>
