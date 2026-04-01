<template>
  <div class="login-container">
    <div class="login-form-card">
      <h1 class="login-title">Reservation System Login</h1>
      
      <!-- Role selection -->
      <div class="form-group">
        <label>Select role</label>
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

      <!-- Email input -->
      <div class="form-group">
        <label>Email address</label>
        <input 
          v-model="form.email"
          type="email"
          placeholder="Enter your email"
          @blur="validateEmail"
        />
        <span v-if="errors.email" class="error-text">{{ errors.email }}</span>
      </div>

      <!-- Password input -->
      <div class="form-group">
        <label>Password</label>
        <input 
          v-model="form.password"
          type="password"
          placeholder="Enter your password"
        />
        <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
      </div>

      <!-- 登录按钮 -->
      <button 
        class="login-btn"
        :disabled="isLoading"
        @click="handleLogin"
      >
        {{ isLoading ? '登录中...' : '登录' }}
      </button>

      <!-- 底部链接 -->
      <div class="footer-links">
        <router-link to="/register">No account? Register</router-link>
        <router-link to="/forgot-password">Forgot password?</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login, type LoginPayload } from '@/api/auth';

defineOptions({ name: 'AuthLogin' });

const router = useRouter();
const isLoading = ref(false);

// 角色选项
const roles = [
  { label: 'CUSTOMER', value: 'CUSTOMER' },
  { label: 'SPECIALIST', value: 'SPECIALIST' },
  { label: 'ADMIN', value: 'ADMIN' }
];

// 表单数据
const form = reactive({
  email: '',
  password: '',
  role: 'CUSTOMER'
});

// 表单错误
const errors = reactive({
  email: '',
  password: '',
  role: ''
});

/**
 * 验证邮箱格式
 */
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

/**
 * 处理登录
 */
async function handleLogin() {
  try {
    // 验证所有字段
    errors.role = form.role ? '' : 'Please select a role first.';
    const emailValid = validateEmail();
    errors.password = form.password ? '' : 'Password is required';

    if (!form.role || !emailValid || !form.password) {
      return;
    }

    isLoading.value = true;

    // 调用登录 API
    const payload: LoginPayload = {
      email: form.email,
      password: form.password,
      role: form.role as 'CUSTOMER' | 'SPECIALIST' | 'ADMIN'
    };

    const response = await login(payload, false);

    ElMessage.success('Login successful');

    // 重定向到对应的首页（可根据角色区分）
    let dashboardRoute = '/customer/dashboard';
    if (form.role === 'SPECIALIST') {
      dashboardRoute = '/specialist/dashboard';
    } else if (form.role === 'ADMIN') {
      dashboardRoute = '/admin/dashboard';
    }

    setTimeout(() => {
      router.push(dashboardRoute);
    }, 500);
  } catch (error: any) {
    console.error('Login error:', error);
    ElMessage.error(error.message || 'Login failed');
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-form-card {
  background: white;
  border-radius: 8px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.role-selector {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.role-btn {
  flex: 1;
  padding: 10px;
  border: 2px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.role-btn:hover {
  border-color: #667eea;
}

.role-btn.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.error-text {
  display: block;
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s;
  margin-top: 20px;
}

.login-btn:hover:not(:disabled) {
  background: #5568d3;
}

.login-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.footer-links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  font-size: 14px;
}

.footer-links a {
  color: #667eea;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-links a:hover {
  color: #5568d3;
}
</style>

