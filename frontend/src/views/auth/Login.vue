<template>
  <div class="login-container">
    <div class="login-form-card">
      <h1 class="login-title">预约平台登录</h1>
      
      <!-- 角色选择 -->
      <div class="form-group">
        <label>选择身份</label>
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

      <!-- 邮箱输入 -->
      <div class="form-group">
        <label>邮箱地址</label>
        <input 
          v-model="form.email"
          type="email"
          placeholder="请输入邮箱地址"
          @blur="validateEmail"
        />
        <span v-if="errors.email" class="error-text">{{ errors.email }}</span>
      </div>

      <!-- 密码输入 -->
      <div class="form-group">
        <label>密码</label>
        <input 
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
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
        <router-link to="/register">没有账户？立即注册</router-link>
        <router-link to="/forgot-password">忘记密码？</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login, type LoginPayload } from '@/api/auth';

defineOptions({ name: 'AuthLogin' });

const router = useRouter();
const isLoading = ref(false);

onMounted(() => {
  if (import.meta.env.DEV) {
    router.replace('/specialist/schedule');
  }
});

// 角色选项
const roles = [
  { label: '顾客', value: 'CUSTOMER' },
  { label: '专家', value: 'SPECIALIST' },
  { label: '管理员', value: 'ADMIN' }
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
    const dashboardRoute: Record<string, string> = {
      'CUSTOMER': '/customer/dashboard',
      'SPECIALIST': '/specialist/schedule',
      'ADMIN': '/admin/specialists'
    };
    const targetRoute = dashboardRoute[form.role] || '/customer/dashboard';

    setTimeout(() => {
      router.push(targetRoute);
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
