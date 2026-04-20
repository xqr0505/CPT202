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

type AuthFailureError = Error & {
  isAuthFailure?: boolean;
  status?: number;
};

const ACTIVITY_EVENT_KEY = 'session-activity-event';
const LOGOUT_EVENT_KEY = 'logout-event';
const AUTH_REFRESH_PATH = '/auth/refresh-token';

export const getAuthToken = (): string | null => {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
};

export const getRefreshToken = (): string | null => {
  return localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken');
};

export const getRememberedEmail = (): string | null => {
  return localStorage.getItem('rememberedEmail');
};

export const isRememberMeSession = (): boolean => {
  return localStorage.getItem('rememberMe') === 'true';
};

const getPreferredStorage = (rememberMe: boolean): Storage => {
  return rememberMe ? localStorage : sessionStorage;
};

let handling401 = false;
let lastErrorMessage = '';
let lastErrorMessageTime = 0;

const shouldSuppressErrorMessage = (config?: any): boolean => {
  return Boolean(config?.suppressErrorMessage);
};

const isFormDataPayload = (value: unknown): value is FormData => {
  return typeof FormData !== 'undefined' && value instanceof FormData;
};

const showErrorOnce = (message: string): void => {
  if (!message) {
    return;
  }

  const now = Date.now();
  if (message !== lastErrorMessage || now - lastErrorMessageTime > 3000) {
    lastErrorMessage = message;
    lastErrorMessageTime = now;
    ElMessage.error(message);
  }
};

export const clearAuthData = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('refreshToken');
  sessionStorage.removeItem('user');
};

export const saveAuthData = (
  token: string,
  refreshToken: string,
  user: any,
  rememberMe: boolean = false
) => {
  saveToken(token, rememberMe);
  saveRefreshToken(refreshToken, rememberMe);
  saveUser(user, rememberMe);
  if (rememberMe) {
    localStorage.setItem('rememberMe', 'true');
  } else {
    localStorage.removeItem('rememberMe');
  }
};

export const saveToken = (token: string, rememberMe: boolean = false) => {
  const storage = getPreferredStorage(rememberMe);
  storage.setItem('token', token);
  if (rememberMe) {
    sessionStorage.removeItem('token');
  } else {
    localStorage.removeItem('token');
  }
};

export const saveRefreshToken = (refreshToken: string, rememberMe: boolean = false) => {
  const storage = getPreferredStorage(rememberMe);
  storage.setItem('refreshToken', refreshToken);
  if (rememberMe) {
    sessionStorage.removeItem('refreshToken');
  } else {
    localStorage.removeItem('refreshToken');
  }
};

export const saveUser = (user: any, rememberMe: boolean = false) => {
  const storage = getPreferredStorage(rememberMe);
  storage.setItem('user', JSON.stringify(user));
  if (rememberMe) {
    sessionStorage.removeItem('user');
  } else {
    localStorage.removeItem('user');
  }
};

export const clearRememberedEmail = () => {
  localStorage.removeItem('rememberedEmail');
};

export const getUser = (): any => {
  const raw = localStorage.getItem('user') || sessionStorage.getItem('user');
  try {
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
};

export const triggerLogoutEvent = () => {
  localStorage.setItem(LOGOUT_EVENT_KEY, Date.now().toString());
  setTimeout(() => localStorage.removeItem(LOGOUT_EVENT_KEY), 100);
};

export const dispatchSessionActivityEvent = () => {
  window.dispatchEvent(new CustomEvent('session-activity'));
  localStorage.setItem(ACTIVITY_EVENT_KEY, Date.now().toString());
  setTimeout(() => localStorage.removeItem(ACTIVITY_EVENT_KEY), 100);
};

export const logout = () => {
  clearAuthData();
  triggerLogoutEvent();
  router.push({ name: 'Login' }).catch(() => null);
};

const createAuthFailureError = (message: string): AuthFailureError => {
  const error = new Error(message) as AuthFailureError;
  error.isAuthFailure = true;
  error.status = 401;
  return error;
};

const isRefreshEndpoint = (url?: string): boolean => {
  if (!url || typeof url !== 'string') {
    return false;
  }
  return url.includes(AUTH_REFRESH_PATH);
};

export const isAuthFailureError = (error: unknown): boolean => {
  if (!error || typeof error !== 'object') {
    return false;
  }

  if ('isAuthFailure' in error && Boolean((error as AuthFailureError).isAuthFailure)) {
    return true;
  }

  if ('status' in error && Number((error as { status?: number }).status) === 401) {
    return true;
  }

  if (
    'response' in error &&
    typeof (error as { response?: { status?: number } }).response === 'object' &&
    (error as { response?: { status?: number } }).response?.status === 401
  ) {
    return true;
  }

  return false;
};

const decodeJwtPayload = (token: string): any | null => {
  if (!token) {
    return null;
  }

  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }

  const payloadPart = parts[1];
  if (!payloadPart) {
    return null;
  }

  try {
    let base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4 !== 0) {
      base64 += '=';
    }
    const jsonPayload = atob(base64);
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
};

export const isTokenExpired = (token: string | null): boolean => {
  if (!token) {
    return true;
  }

  const payload = decodeJwtPayload(token);
  const exp = payload?.exp;
  if (typeof exp !== 'number') {
    return true;
  }

  return Date.now() / 1000 >= exp;
};

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || 'http://localhost:8081',
  timeout: 10000
});

service.interceptors.request.use(
  config => {
    config.headers = config.headers || {};
    const headers = config.headers as Record<string, any> & {
      setContentType?: (value?: string | false) => void;
    };

    if (isFormDataPayload(config.data)) {
      if (typeof headers.setContentType === 'function') {
        headers.setContentType(undefined);
      } else {
        delete headers['Content-Type'];
        delete headers['content-type'];
      }
    } else if (config.data !== undefined && config.data !== null) {
      if (typeof headers.setContentType === 'function') {
        headers.setContentType('application/json;charset=UTF-8');
      } else if (!headers['Content-Type'] && !headers['content-type']) {
        headers['Content-Type'] = 'application/json;charset=UTF-8';
      }
    }

    const token = getAuthToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

let refreshingPromise: Promise<string> | null = null;

export const refreshAuthToken = async (): Promise<string> => {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw createAuthFailureError('Refresh token missing');
  }

  if (refreshingPromise) {
    return refreshingPromise;
  }

  const rememberMe = isRememberMeSession();
  refreshingPromise = service
    .post<any, any>(
      '/auth/refresh-token',
      { refreshToken },
      { suppressErrorMessage: true } as any
    )
    .then(result => {
      if (!result || typeof result.token !== 'string') {
        throw new Error('Failed to refresh access token');
      }

      saveToken(result.token, rememberMe);

      if (typeof result.refreshToken === 'string' && result.refreshToken) {
        saveRefreshToken(result.refreshToken, rememberMe);
      }

      dispatchSessionActivityEvent();
      return result.token;
    })
    .catch(error => {
      logout();
      throw error;
    })
    .finally(() => {
      refreshingPromise = null;
    });

  return refreshingPromise;
};

service.interceptors.response.use(
  response => {
    const res = response.data as ApiResponse;
    const suppressErrorMessage = shouldSuppressErrorMessage(response.config);

    if (res.code === 200) {
      return res.data;
    }

    if (res.code === 401) {
      const originalRequest = response.config as any;
      if (
        !originalRequest?._retry &&
        !isRefreshEndpoint(originalRequest?.url) &&
        getRefreshToken()
      ) {
        originalRequest._retry = true;
        return refreshAuthToken()
          .then(() => service(originalRequest))
          .catch(error => {
            if (!suppressErrorMessage) {
              showErrorOnce(error.message || 'Unauthorized, please login again');
            }
            logout();
            return Promise.reject(createAuthFailureError(error.message || 'Unauthorized'));
          });
      }

      if (!handling401) {
        handling401 = true;
        logout();
        if (!suppressErrorMessage) {
          showErrorOnce(res.message || 'Unauthorized, please login again');
        }
        setTimeout(() => {
          handling401 = false;
        }, 3000);
      }
      return Promise.reject(createAuthFailureError(res.message || 'Unauthorized'));
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
  async error => {
    const status = error.response?.status;
    const suppressErrorMessage = shouldSuppressErrorMessage(error.config);
    const originalRequest = error.config as any;

    if (
      status === 401 &&
      !originalRequest?._retry &&
      !isRefreshEndpoint(originalRequest?.url) &&
      getRefreshToken()
    ) {
      originalRequest._retry = true;
      try {
        await refreshAuthToken();
        return service(originalRequest);
      } catch (refreshError) {
        if (!suppressErrorMessage) {
          showErrorOnce('Unauthorized, please login again');
        }
        logout();
        const authFailureError = error as AuthFailureError;
        authFailureError.isAuthFailure = true;
        authFailureError.status = 401;
        return Promise.reject(authFailureError);
      }
    }

    if (status === 401) {
      if (!handling401) {
        handling401 = true;
        logout();
        if (!suppressErrorMessage) {
          showErrorOnce('Unauthorized, please login again');
        }
        setTimeout(() => {
          handling401 = false;
        }, 3000);
      }
      const authFailureError = error as AuthFailureError;
      authFailureError.isAuthFailure = true;
      authFailureError.status = 401;
      return Promise.reject(authFailureError);
    }

    if (status === 403) {
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
