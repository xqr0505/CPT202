// src/router/permission.ts
import type { Router } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { getAuthToken, getRefreshToken, getUser, isTokenExpired, refreshAuthToken, clearAuthData } from '@/api/request';

const publicRoutes = [
  '/',
  '/customer/search',
  '/customer/specialists',
  '/error/403',
  '/error/404',
  '/error/500',
  '/error/global'
];
const authRoutes = ['/auth', '/login', '/register', '/forgot-password'];

const isPublicPath = (path: string): boolean => publicRoutes.includes(path);

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

    const shouldAttemptRefresh = Boolean(refreshToken && (!token || isTokenExpired(token)));
    if (shouldAttemptRefresh) {
      try {
        await refreshAuthToken();
      } catch {
        clearAuthData();
      }
    }

    const currentToken = getAuthToken();
    const currentUser = getUser();
    const userRole = typeof currentUser?.role === 'string' ? currentUser.role : null;
    if (currentToken && currentUser && userRole) {
      if (to.path === '/auth' || to.path === '/login' || to.path === '/register') {
        return { path: getDefaultHomePath(userRole) };
      }

      if (to.path === '/') {
        return { path: getDefaultHomePath(userRole) };
      }

      const requiredRole = to.meta?.role as string | undefined;
      if (requiredRole && userRole !== requiredRole) {
        return { path: '/error/403' };
      }

      return true;
    } else {
      if (to.path === '/') {
        return { path: '/customer/search' };
      }

      if (isPublicPath(to.path) || authRoutes.includes(to.path)) {
        return true;
      }

      try {
        await ElMessageBox.confirm('This page requires login to access', 'Permission Required', {
          confirmButtonText: 'Login',
          cancelButtonText: 'Continue Browsing',
          closeOnClickModal: false,
          closeOnPressEscape: false,
          showCancelButton: true,
          distinguishCancelAndClose: true,
          type: 'warning'
        });

        return { path: '/auth', query: { redirect: to.fullPath } };
      } catch {
        return { path: '/customer/search' };
      }
    }
  });
}
