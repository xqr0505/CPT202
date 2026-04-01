import { USER_ROLES, type UserRoleType } from '@/constants/roles';

/**
 * 用户配置文件类型
 */
export interface UserProfile {
  id: number;
  username: string;
  nickname?: string;
  fullName?: string;
  email?: string;
  phoneNumber?: string;
  avatar?: string;
  role: UserRoleType;
}

export interface UserProfile {
  id: number;
  username: string;
  nickname?: string;
  fullName?: string;
  email?: string;
  phoneNumber?: string;
  avatar?: string;
  role: typeof USER_ROLES[keyof typeof USER_ROLES];
}

/**
 * 应用已保存主题偏好（可选，防止调用缺失导致路由守卫报错）
 */
export const applySavedThemePreference = (): void => {
  // 这里做一个安全默认，后续可以接 ThemeStore 实现主题同步逻辑
  // 例如：const theme = localStorage.getItem('theme'); if (theme) applyTheme(theme)
};

/**
 * 当前本地存储的用户是否已被管理员停用（默认false）
 */
export const isStoredUserAccountDeactivated = (): boolean => {
  return false;
};

// TODO: Implement user data fetching logic and assign the correct role here.
export const fetchUserProfile = async (): Promise<UserProfile> => {
  // Placeholder implementation
  return Promise.resolve({
    id: 1,
    username: 'test_user',
    nickname: 'Test User',
    role: USER_ROLES.CUSTOMER // USER_ROLES.SPECIALIST / USER_ROLES.ADMIN / USER_ROLES.CUSTOMER
  });
};
