// src/router/permission.ts
import type { Router, RouteLocationNormalized } from 'vue-router';
import { getAuthToken, getUser } from '@/api/request';

// WhiteList
const whiteList = ['/auth/login', '/register','/forgot-password', '/error/403', '/error/404', '/error/500'];

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
  router.beforeEach(async (to) => {
    const token = getAuthToken();
    const user = getUser(); // { userId, role, email, displayName }

    if (token && user) {
      // Logged in, check permissions
      if (to.path === '/auth/login' || to.path === '/register') {
        return { path: getDefaultHomePath(user.role) };
      }

      if (to.path === '/') {
        return { path: getDefaultHomePath(user.role) };
      }

      const requiredRole = to.meta?.role as string | undefined;
      if (requiredRole && user.role !== requiredRole) {
        return { path: '/error/403' };
      }

      return true;
    } else {
      // Not logged in, check if the route is in the whitelist
      if (whiteList.includes(to.path)) {
        return true;
      } else {
        return { path: '/auth/login', query: { redirect: to.fullPath } };
      }
    }
  });
}
