import request from './request';
import { logout as clearAndRedirect } from './request';

export interface LoginPayload {
  email: string;
  password: string;
  role: 'CUSTOMER' | 'SPECIALIST' | 'ADMIN';
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  userId: number;
  role: string;
  email: string;
  displayName: string;
  expiresIn: number;
}

export interface RegisterPayload {
  email: string;
  verificationCode: string;
  password: string;
  confirmPassword: string;
  role: 'CUSTOMER' | 'SPECIALIST';
}

export interface SendVerificationCodePayload {
  email: string;
  role?: 'CUSTOMER' | 'SPECIALIST';
  type: 'REGISTER' | 'RESET_PASSWORD';
}

/**
 * 用户登录
 * @param payload 登录信息
 * @returns 登录响应
 */
export function login(payload: LoginPayload): Promise<LoginResponse> {
  return request.post<any, LoginResponse>('/auth/login', payload);
}

/**
 * 用户注册
 * @param payload 注册信息
 * @returns 注册响应（返回自动登录的 Token）
 */
export function register(payload: RegisterPayload): Promise<LoginResponse> {
  return request.post<any, LoginResponse>('/auth/register', payload);
}

/**
 * 发送验证码
 * @param payload 发送验证码信息
 * @returns 服务器响应
 */
export function sendVerificationCode(payload: SendVerificationCodePayload): Promise<any> {
  return request.post('/auth/verify-email', payload);
}

/**
 * 用户登出
 * @returns 服务器响应
 */
export function logout(): Promise<any> {
  return request.post('/auth/logout', {}).finally(() => {
    clearAndRedirect();
  });
}

/**
 * 密码重置
 * @param email 用户邮箱
 * @param verificationCode 验证码
 * @param newPassword 新密码
 * @param confirmPassword 确认新密码
 * @returns 服务器响应
 */
export function resetPassword(
  email: string,
  verificationCode: string,
  newPassword: string,
  confirmPassword: string
): Promise<any> {
  return request.post('/auth/reset-password', null, {
    params: {
      email,
      verificationCode,
      newPassword,
      confirmPassword
    }
  });
}
