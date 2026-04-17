import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuard } from './permission';

const routes = [
  {
    path: '/auth',
    component: () => import('../layout/AuthLayout.vue'),
    children: [
      { path: 'login', name: 'Login', component: () => import('../views/auth/Login.vue') }
    ]
  },
  {
    path: '/register',
    component: () => import('../views/auth/Register.vue'),
    name: 'Register'
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/auth/ForgotPassword.vue')
  },
  {
  path: '/admin',
  component: () => import('../layout/AdminLayout.vue'),
  meta: { requiresAuth: true, role: 'ADMIN' },
  children: [
    { path: 'categories', component: () => import('../views/admin/AdminCategoryList.vue') },
    { path: 'specialists', component: () => import('../views/admin/AdminSpecialistList.vue') },
    { path: 'specialists/create', component: () => import('../views/admin/AdminSpecialistForm.vue') },
    { path: 'specialists/:id/edit', component: () => import('../views/admin/AdminSpecialistForm.vue') }
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
      {
        name: 'CustomerSpecialists',
        path: 'specialists',
        component: () => import('../views/customer/SpecialistSearch.vue')
      },
      {
        name: 'CustomerSpecialistDetail',
        path: 'specialists/:id',
        component: () => import('../views/customer/SpecialistDetail.vue')
      },
      {
        name: 'CustomerSpecialistBooking',
        path: 'specialists/:id/book',
        component: () => import('../views/customer/SpecialistBooking.vue')
      },
      { path: 'dashboard', component: () => import('../views/customer/Dashboard.vue') },
      { path: 'bookings', component: () => import('../views/customer/Bookings.vue') },
      {
        name: 'CustomerProfile',
        path: 'profile',
        component: () => import('../views/customer/Profile.vue')
      },
      {
        name: 'CustomerProfileEdit',
        path: 'profile/edit',
        component: () => import('../views/customer/ProfileEdit.vue')
      },
      {
        name: 'CustomerChangePassword',
        path: 'profile/password',
        component: () => import('../views/customer/ChangePassword.vue')
      },
      {
        name: 'CustomerStyleSettings',
        path: 'profile/style-settings',
        component: () => import('../views/customer/StyleSettings.vue')
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
    path: '/login',
    redirect: '/auth/login'
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

setupRouterGuard(router);

export default router;
