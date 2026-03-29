import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';

/**
 * API 请求类型定义
 */
interface RequestConfig extends InternalAxiosRequestConfig {
  skipErrorMessage?: boolean; // 是否跳过默认错误提示
}

interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

/**
 * 创建 Axios 实例
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
});

/**
 * ==================== 请求拦截器 ====================
 * 功能：
 * 1. 添加 JWT Token 到请求头
 * 2. 处理请求配置
 */
service.interceptors.request.use(
  (config: RequestConfig) => {
    // 从 localStorage 或 sessionStorage 获取 Token
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');

    // 如果存在 Token，添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error: AxiosError) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

/**
 * ==================== 响应拦截器 ====================
 * 功能：
 * 1. 统一处理业务状态码
 * 2. 处理 401/403 异常
 * 3. 处理全局错误提示
 * 4. Token 过期自动退出登录
 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message, data } = response.data;

    // 状态码 200 - 正常响应
    if (code === 200) {
      return data;
    }

    // 状态码 401 - 未认证（Token 无效或过期）
    if (code === 401) {
      ElMessage.error(message || 'Token expired or invalid, please login again');
      
      // 清除本地存储的 Token 和用户信息
      clearAuthData();
      
      // 延迟重定向到登录页，避免多次弹窗
      setTimeout(() => {
        router.push({
          name: 'Login',
          query: { redirect: router.currentRoute.value.fullPath }
        });
      }, 500);
      
      return Promise.reject(new Error(message || 'Unauthorized'));
    }

    // 状态码 403 - 无权限访问
    if (code === 403) {
      ElMessage.error(message || 'You do not have permission to access this resource');
      return Promise.reject(new Error(message || 'Forbidden'));
    }

    // 其他错误状态码
    ElMessage.error(message || 'An error occurred');
    return Promise.reject(new Error(message || 'Request failed'));
  },

  (error: AxiosError<ApiResponse>) => {
    // HTTP 状态码错误处理
    const status = error.response?.status;
    const message = error.response?.data?.message;

    if (status === 401) {
      // 服务器返回 401
      ElMessage.error(message || 'Unauthorized');
      clearAuthData();

      setTimeout(() => {
        router.push({
          name: 'Login',
          query: { redirect: router.currentRoute.value.fullPath }
        });
      }, 500);
    } else if (status === 403) {
      // 服务器返回 403
      ElMessage.error(message || 'Forbidden');
    } else if (status === 500) {
      // 服务器错误
      ElMessage.error('Server error, please try again later');
    } else if (error.code === 'ECONNABORTED') {
      // 请求超时
      ElMessage.error('Request timeout');
    } else if (error.message === 'Network Error') {
      // 网络错误
      ElMessage.error('Network error, please check your connection');
    }

    console.error('Response error:', error);
    return Promise.reject(error);
  }
);

/**
 * 清除认证数据
 * 用于登出或 Token 过期时清除本地存储的信息
 */
function clearAuthData(): void {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
  
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
}

/**
 * 获取当前 Token
 */
export function getToken(): string | null {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
}

/**
 * 保存 Token 到存储
 * @param token JWT Token
 * @param rememberMe 是否记住我（保存到 localStorage）
 */
export function saveToken(token: string, rememberMe: boolean = false): void {
  if (rememberMe) {
    localStorage.setItem('token', token);
    localStorage.setItem('rememberMe', 'true');
  } else {
    sessionStorage.setItem('token', token);
  }
}

/**
 * 保存用户信息
 * @param user 用户对象
 * @param rememberMe 是否记住我
 */
export function saveUser(user: any, rememberMe: boolean = false): void {
  const storage = rememberMe ? localStorage : sessionStorage;
  storage.setItem('user', JSON.stringify(user));
}

/**
 * 获取保存的用户信息
 */
export function getUser(): any {
  const user = localStorage.getItem('user') || sessionStorage.getItem('user');
  return user ? JSON.parse(user) : null;
}

/**
 * 清除所有认证数据
 */
export function logout(): void {
  clearAuthData();
}

export default service;

