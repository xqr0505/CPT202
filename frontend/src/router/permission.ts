// src/router/permission.ts
import type { Router } from 'vue-router';
import { getAuthToken, getRefreshToken, getUser, isTokenExpired, refreshAuthToken, clearAuthData } from '@/api/request';

// WhiteList
const whiteList = ['/auth/login', '/register', '/forgot-password', '/error/403', '/error/404', '/error/500'];

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
    const refreshToken = getRefreshToken();
    const user = getUser();

    const shouldAttemptRefresh = Boolean(refreshToken && (!token || isTokenExpired(token)));

    if (shouldAttemptRefresh) {
      try {
        await refreshAuthToken();
      } catch {
        clearAuthData();
        next({ path: '/auth/login', query: { redirect: to.fullPath } });
        return;
      }
    }

    const currentToken = getAuthToken();
    const currentUser = getUser();
    const isAuthenticated = Boolean(currentToken && currentUser);

    if (isAuthenticated) {
      if (to.path === '/auth/login' || to.path === '/register') {
        next({ path: getDefaultHomePath(currentUser.role) });
        return;
      }

      if (to.path === '/') {
        next({ path: getDefaultHomePath(currentUser.role) });
        return;
      }

      const requiredRole = to.meta?.role as string | undefined;
      if (requiredRole && currentUser.role !== requiredRole) {
        next({ path: '/error/403' });
        return;
      }

      next();
    } else {
      if (whiteList.includes(to.path)) {
        next();
      } else {
        next({ path: '/auth/login', query: { redirect: to.fullPath } });
      }
    }
  });
}