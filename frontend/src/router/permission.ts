// src/router/permission.ts
import type { Router, RouteLocationNormalized } from 'vue-router';
import { getAuthToken, getUser } from '@/api/request';

// WhiteList
const whiteList = ['/auth/login', '/register', '/error/403', '/error/404', '/error/500'];

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
      // Logged in, check permissions
      if (to.path === '/auth/login' || to.path === '/register') {
        next({ path: getDefaultHomePath(user.role) });
        return;
      }

      if (to.path === '/') {
        next({ path: getDefaultHomePath(user.role) });
        return;
      }

      const requiredRole = to.meta?.role as string | undefined;
      if (requiredRole && user.role !== requiredRole) {
        next({ path: '/error/403' });
        return;
      }

      next();
    } else {
      // Not logged in, check if the route is in the whitelist
      if (whiteList.includes(to.path)) {
        next();
      } else {
        next({ path: '/auth/login', query: { redirect: to.fullPath } });
      }
    }
  });
}