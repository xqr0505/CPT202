import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/auth',
    component: () => import('../layout/AuthLayout.vue'),
    children: [
      { path: 'login', name: 'Login', component: () => import('../views/auth/Login.vue') }
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
    path: '/specialist',
    component: () => import('../layout/SpecialistLayout.vue'),
    meta: { requiresAuth: true, role: 'SPECIALIST' },
    children: [
      { path: 'schedule', component: () => import('../views/specialist/ScheduleDashboard.vue') },
      { path: 'rules', component: () => import('../views/specialist/RecurringRules.vue') }
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
      { path: 'profile', component: () => import('../views/customer/Profile.vue') }
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
    path: '/',
    redirect: '/customer/search'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/error/404'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
