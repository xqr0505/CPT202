import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';

/**
 * 定义后端返回数据的通用结构
 * @template T - 返回数据 data 的类型
 */
interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

/**
 * 获取当前的认证令牌 (Token)
 * 优先检查持久化 LocalStorage，其次检查会话级SessionStorage
 */
const getAuthToken = (): string | null => {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
};

/**
 * 清除所有认证相关的数据
 */
const clearAuthData = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
};

/**
 * 保存认证数据
 * 根据用户是否选择“记住我”，决定存储位置
 */
export const saveAuthData = (token: string, user: any, rememberMe: boolean = false) => {
  if (rememberMe) {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    localStorage.setItem('rememberMe', 'true');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
  } else {
    sessionStorage.setItem('token', token);
    sessionStorage.setItem('user', JSON.stringify(user));
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('rememberMe');
  }
};

/**
 * 获取当前登录的用户信息
 */
export const getUser = (): any => {
  const raw = localStorage.getItem('user') || sessionStorage.getItem('user');
  try {
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
};

/**
 * 用户登出操作
 * 清除本地数据并强制跳转到登录页
 */
export const logout = () => {
  clearAuthData();
  router.push({ name: 'Login' }).catch(() => null);
};

const service = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
});

/**
 * 请求拦截器
 * 在请求发送之前执行：主要用于给请求头添加 Token
 */
service.interceptors.request.use(
  config => {
    const token = getAuthToken();
    if (token) {
      config.headers = config.headers || {};
      (config.headers as Record<string, any>)['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  response => {
    const res = response.data as ApiResponse;

    if (res.code === 200) {
      return res.data;
    }

    if (res.code === 401) {
      clearAuthData();
      ElMessage.error(res.message || 'Unauthorized, please login again');
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }).catch(() => null);
      return Promise.reject(new Error(res.message || 'Unauthorized'));
    }

    if (res.code === 403) {
      ElMessage.error(res.message || 'Forbidden');
      return Promise.reject(new Error(res.message || 'Forbidden'));
    }

    ElMessage.error(res.message || 'Error');
    return Promise.reject(new Error(res.message || 'Error'));
  },
  error => {
    const status = error.response?.status;

    if (status === 401) {
      clearAuthData();
      ElMessage.error('Unauthorized, please login again');
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }).catch(() => null);
    } else if (status === 403) {
      ElMessage.error('Forbidden');
    } else if (status === 500) {
      ElMessage.error('Server error, please try again later');
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('Request timeout');
    } else if (error.message?.includes('Network Error')) {
      ElMessage.error('Network error, please check your connection');
    }

    return Promise.reject(error);
  }
);

export default service;


