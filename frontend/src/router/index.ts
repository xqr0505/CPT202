import { createRouter, createWebHistory } from 'vue-router'
import {
  applySavedThemePreference,
  isStoredUserAccountDeactivated
} from '@/api/user'

const routes = [
  {
    path: '/auth',
    component: () => import('../layout/AuthLayout.vue'),
    children: [
      { path: 'login', component: () => import('../views/auth/Login.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: 'specialists', component: () => import('../views/admin/AdminSpecialistList.vue') }
    ]
  },
  {
    path: '/customer',
    component: () => import('../layout/DefaultLayout.vue'),
    children: [
      { path: 'search', component: () => import('../views/customer/SpecialistSearch.vue') },
      { path: 'specialists', component: () => import('../views/customer/SpecialistSearch.vue') },
      { path: 'dashboard', component: () => import('../views/customer/Dashboard.vue') },
      { path: 'bookings', component: () => import('../views/customer/Bookings.vue') },
      {
        path: 'profile',
        component: () => import('../views/customer/Profile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile/edit',
        component: () => import('../views/customer/ProfileEdit.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile/password',
        component: () => import('../views/customer/ChangePassword.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile/style-settings',
        component: () => import('../views/customer/StyleSettings.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/error',
    children: [
      { path: '403', component: () => import('../views/error/Forbidden.vue') },
      { path: '404', component: () => import('../views/error/NotFound.vue') },
      { path: '500', component: () => import('../views/error/ServerError.vue') },
      { path: 'global', component: () => import('../views/error/GlobalError.vue') }
    ]
  },
  {
    path: '/dev',
    component: () => import('../views/dev/DevDemo.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/error/404'
  },
  {
    path: '/',
    redirect: '/customer/search'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(to => {
  applySavedThemePreference()

  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const isDeactivated = isStoredUserAccountDeactivated()

  if (requiresAuth && isDeactivated) {
    localStorage.removeItem('token')

    return {
      path: '/auth/login',
      query: {
        reason: 'deactivated'
      }
    }
  }

  if (!requiresAuth) {
    return true
  }

  const token = localStorage.getItem('token')

  if (token) {
    return true
  }

  return {
    path: '/auth/login',
    query: {
      redirect: to.fullPath
    }
  }
})

export default router
