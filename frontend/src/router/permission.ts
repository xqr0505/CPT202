import type { Router, RouteLocationNormalized } from 'vue-router';
import { getAuthToken } from '@/api/request';  // 我们需要导出 getAuthToken

// 白名单：不需要登录就能访问的页面
const whiteList = ['/auth/login', '/error/403', '/error/404', '/error/500'];

// 设置路由守卫
export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next) => {
    const token = getAuthToken();  // 检查是否存在 token

    if (token) {
      // 已登录
      if (to.path === '/auth/login') {
        // 如果已登录且要去登录页，则重定向到首页（或原来的目标）
        next({ path: '/customer/search' });
      } else {
        // 正常访问，放行
        next();
      }
    } else {
      // 未登录
      // FIXME: Mock 阶段：放行所有路由
      // if (whiteList.includes(to.path)) {
      //   // 在白名单内，放行
      //   next();
      // } else {
      //   // 不在白名单，重定向到登录页，并携带原目标路径
      //   next({ path: '/auth/login', query: { redirect: to.fullPath } });
      // }
      next();
    }
  });
}
