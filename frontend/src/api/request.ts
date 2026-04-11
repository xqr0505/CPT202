import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';

/**
 * Default API response structure
 * @template T - data type of the response payload
 */
interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

type RequestWithToastControl = {
  suppressErrorMessage?: boolean;
};

export const getAuthToken = (): string | null => {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
};

const clearAuthData = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
};

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
    localStorage.removeItem('rememberMe');  }
};

export const saveToken = (token: string, rememberMe: boolean = false) => {
  if (rememberMe) {
    localStorage.setItem('token', token);
    sessionStorage.removeItem('token');
  } else {
    sessionStorage.setItem('token', token);
    localStorage.removeItem('token');
  }
};

export const saveUser = (user: any, rememberMe: boolean = false) => {
  if (rememberMe) {
    localStorage.setItem('user', JSON.stringify(user));
    sessionStorage.removeItem('user');
  } else {
    sessionStorage.setItem('user', JSON.stringify(user));
    localStorage.removeItem('user');
  }
};

export const getUser = (): any => {
  const raw = localStorage.getItem('user') || sessionStorage.getItem('user');
  try {
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
};

export const logout = () => {
  clearAuthData();
  router.push({ name: 'Login' }).catch(() => null);
};

// TTL for suppressing duplicate messages (ms)
const ERROR_CACHE_TTL = 5000
const errorCache = new Map<string, number>()
let handling401 = false

function showErrorOnce(msg: string) {
  if (!msg) return
  const now = Date.now()
  const last = errorCache.get(msg)
  if (!last || now - last > ERROR_CACHE_TTL) {
    ElMessage.error(msg)
    errorCache.set(msg, now)
  }
}

function shouldSuppressErrorMessage(config?: unknown): boolean {
  return Boolean(
    config &&
      typeof config === 'object' &&
      'suppressErrorMessage' in config &&
      (config as RequestWithToastControl).suppressErrorMessage
  )
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || 'http://localhost:8081',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
});

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
    const suppressErrorMessage = shouldSuppressErrorMessage(response.config);

    if (res.code === 200) {
      return res.data;
    }

    if (res.code === 401) {
      if (!handling401) {
        handling401 = true
        clearAuthData();
        if (!suppressErrorMessage) {
          showErrorOnce(res.message || 'Unauthorized, please login again');
        }
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }).catch(() => null);
        // allow future 401 handling after short delay
        setTimeout(() => { handling401 = false }, 3000)
      }
      return Promise.reject(new Error(res.message || 'Unauthorized'));
    }

    if (res.code === 403) {
      if (!suppressErrorMessage) {
        showErrorOnce(res.message || 'Forbidden');
      }
      return Promise.reject(new Error(res.message || 'Forbidden'));
    }

    if (!suppressErrorMessage) {
      showErrorOnce(res.message || 'Error');
    }
    return Promise.reject(new Error(res.message || 'Error'));
  },
  error => {
    const status = error.response?.status;
    const suppressErrorMessage = shouldSuppressErrorMessage(error.config);

    if (status === 401) {
      if (!handling401) {
        handling401 = true
        clearAuthData();
        if (!suppressErrorMessage) {
          showErrorOnce('Unauthorized, please login again');
        }
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }).catch(() => null);
        setTimeout(() => { handling401 = false }, 3000)
      }
    } else if (status === 403) {
      if (!suppressErrorMessage) {
        showErrorOnce('Forbidden');
      }
    } else if (status === 500) {
      if (!suppressErrorMessage) {
        showErrorOnce('Server error, please try again later');
      }
    } else if (error.code === 'ECONNABORTED') {
      if (!suppressErrorMessage) {
        showErrorOnce('Request timeout');
      }
    } else if (error.message?.includes('Network Error')) {
      if (!suppressErrorMessage) {
        showErrorOnce('Network error, please check your connection');
      }
    }

    return Promise.reject(error);
  }
);

export default service;

// ========== 新增：记住邮箱功能 ==========
export const saveRememberedEmail = (email: string) => {
  if (email) {
    localStorage.setItem('rememberedEmail', email);
  } else {
    localStorage.removeItem('rememberedEmail');
  }
};

export const getRememberedEmail = (): string | null => {
  return localStorage.getItem('rememberedEmail');
};
