import request, { saveToken, saveUser } from './request';
import { logout as clearAndRedirect } from './request';

export interface LoginPayload {
  email: string;
  password: string;
  role: 'CUSTOMER' | 'SPECIALIST' | 'ADMIN';
}

export interface LoginResponse {
  token: string;
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
 * @param rememberMe 是否记住我
 * @returns 登录响应
 * 
 * 使用示例：
 * const response = await login({
 *   email: 'user@example.com',
 *   password: 'Password123',
 *   role: 'CUSTOMER'
 * }, true);
 * 
 * // 保存 Token 到前端
 * console.log(response.token);
 */
export function login(payload: LoginPayload, rememberMe: boolean = false): Promise<LoginResponse> {
  return request.post<any, LoginResponse>('/auth/login', payload).then(res => {
  
    saveToken(res.token, rememberMe);
    
    saveUser({
      userId: res.userId,
      role: res.role,
      email: res.email,
      displayName: res.displayName
    }, rememberMe);

    return res;
  });
}

/**
 * 用户注册
 * @param payload 注册信息
 * @returns 注册响应（返回自动登录的 Token）
 * 
 * 使用示例：
 * const response = await register({
 *   email: 'newuser@example.com',
 *   verificationCode: '123456',
 *   password: 'Password123',
 *   confirmPassword: 'Password123',
 *   role: 'CUSTOMER'
 * });
 */
export function register(payload: RegisterPayload): Promise<LoginResponse> {
  return request.post<any, LoginResponse>('/auth/register', payload).then(res => {
    // 注册成功后自动登录
    saveToken(res.token, false);
    saveUser({
      userId: res.userId,
      role: res.role,
      email: res.email,
      displayName: res.displayName
    }, false);

    return res;
  });
}

/**
 * 发送验证码
 * @param payload 发送验证码信息
 * @returns 服务器响应
 * 
 * 使用示例：
 * // 注册时发送验证码
 * await sendVerificationCode({
 *   email: 'user@example.com',
 *   role: 'CUSTOMER',
 *   type: 'REGISTER'
 * });
 * 
 * // 密码重置时发送验证码
 * await sendVerificationCode({
 *   email: 'user@example.com',
 *   type: 'RESET_PASSWORD'
 * });
 */
export function sendVerificationCode(payload: SendVerificationCodePayload): Promise<any> {
  return request.post('/auth/verify-email', payload);
}

/**
 * 用户登出
 * @returns 服务器响应
 * 
 * 使用示例：
 * await logout();
 * // 自动跳转到登录页
 */
export function logout(): Promise<any> {
  // 可选：通知后端（如果后端需要）
  return request.post('/auth/logout', {}).finally(() => {
    clearAndRedirect();  // 清除本地数据并跳转登录页
  });
}

/**
 * 密码重置
 * @param email 用户邮箱
 * @param verificationCode 验证码
 * @param newPassword 新密码
 * @param confirmPassword 确认新密码
 * @returns 服务器响应
 * 
 * 使用示例：
 * await resetPassword(
 *   'user@example.com',
 *   '123456',
 *   'NewPassword123',
 *   'NewPassword123'
 * );
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
