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
  router.beforeEach(async (to) => {
    const token = getAuthToken();
    const refreshToken = getRefreshToken();
    const user = getUser();

    const shouldAttemptRefresh = Boolean(refreshToken && (!token || isTokenExpired(token)));

    if (shouldAttemptRefresh) {
      try {
        await refreshAuthToken();
      } catch {
        clearAuthData();
        return { path: '/auth/login', query: { redirect: to.fullPath } };
      }
    }

    const currentToken = getAuthToken();
    const currentUser = getUser();
    const isAuthenticated = Boolean(currentToken && currentUser);

    if (isAuthenticated) {
      if (to.path === '/auth/login' || to.path === '/register') {
        return { path: getDefaultHomePath(currentUser.role) };
      }

      if (to.path === '/') {
        return { path: getDefaultHomePath(currentUser.role) };
      }

      const requiredRole = to.meta?.role as string | undefined;
      if (requiredRole && currentUser.role !== requiredRole) {
        return { path: '/error/403' };
      }

      return true;
    } else {
      if (whiteList.includes(to.path)) {
        return true;
      } else {
        return { path: '/auth/login', query: { redirect: to.fullPath } };
      }
    }
  });
}
