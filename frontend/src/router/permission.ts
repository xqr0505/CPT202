// src/router/permission.ts
import type { Router, RouteLocationNormalized } from 'vue-router';
import { getAuthToken, getUser } from '@/api/request';

// 白名单：不需要登录就能访问的页面
const whiteList = ['/auth/login', '/register', '/error/403', '/error/404', '/error/500'];

// 根据角色获取默认首页路径
const getDefaultHomePath = (role: string): string => {
  switch (role) {
    case 'ADMIN':
      return '/admin/specialists';
    case 'SPECIALIST':
      return '/specialist/schedule';
    case 'CUSTOMER':
    default:
      return '/customer/search';
  }
};

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const token = getAuthToken();
    const user = getUser(); // { userId, role, email, displayName }

    if (token && user) {
      // ---------- 已登录状态 ----------
      // 1. 如果要去登录页或注册页，重定向到该角色的默认首页
      if (to.path === '/auth/login' || to.path === '/register') {
        next({ path: getDefaultHomePath(user.role) });
        return;
      }

      // 2. 如果访问根路径，也重定向到默认首页
      if (to.path === '/') {
        next({ path: getDefaultHomePath(user.role) });
        return;
      }

      // 3. 权限校验：检查路由要求的角色
      const requiredRole = to.meta?.role as string | undefined;
      if (requiredRole && user.role !== requiredRole) {
        // 角色不匹配，跳转到403页面
        next({ path: '/error/403' });
        return;
      }

      // 其他情况放行
      next();
    } else {
      // ---------- 未登录状态 ----------
      // 白名单内的页面可以访问
      if (whiteList.includes(to.path)) {
        next();
      } else {
        // 不在白名单，重定向到登录页，并携带原目标路径
        next({ path: '/auth/login', query: { redirect: to.fullPath } });
      }
    }
  });
}