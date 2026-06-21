import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'guides',
        name: 'guides',
        component: () => import('@/views/Guides.vue'),
        meta: { title: '人员管理' },
      },
      {
        path: 'sessions',
        name: 'sessions',
        component: () => import('@/views/Sessions.vue'),
        meta: { title: '场次管理' },
      },
      {
        path: 'pricing',
        name: 'pricing',
        component: () => import('@/views/Pricing.vue'),
        meta: { title: '价格与规则' },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/Orders.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'stats',
        name: 'stats',
        component: () => import('@/views/Stats.vue'),
        meta: { title: '基础统计' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { path: '/' }
  }
})

export default router
