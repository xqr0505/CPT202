import { createRouter, createWebHistory } from 'vue-router'

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
    path: '/',
    component: () => import('../layout/DefaultLayout.vue'),
    children: [
      { path: 'search', component: () => import('../views/customer/SpecialistSearch.vue') }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
