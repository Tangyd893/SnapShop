import { createRouter, createWebHistory } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginPage.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      name: 'layout',
      component: () => import('@/views/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardPage.vue'),
          meta: { title: '仪表盘', requiredRole: [] }
        },
        {
          path: 'products',
          name: 'products',
          component: () => import('@/views/ProductList.vue'),
          meta: { title: '商品列表', requiredRole: ['SUPER_ADMIN', 'OPERATOR'] }
        },
        {
          path: 'products/:id',
          name: 'product-edit',
          component: () => import('@/views/ProductEdit.vue'),
          meta: { title: '商品编辑', requiredRole: ['SUPER_ADMIN', 'OPERATOR'] }
        },
        {
          path: 'categories',
          name: 'categories',
          component: () => import('@/views/CategoryList.vue'),
          meta: { title: '分类管理', requiredRole: ['SUPER_ADMIN', 'OPERATOR'] }
        },
        {
          path: 'seckill/activities',
          name: 'seckill-activities',
          component: () => import('@/views/SeckillActivityList.vue'),
          meta: { title: '秒杀活动列表', requiredRole: ['SUPER_ADMIN', 'OPERATOR'] }
        },
        {
          path: 'seckill/activities/:id',
          name: 'seckill-activity-edit',
          component: () => import('@/views/SeckillActivityEdit.vue'),
          meta: { title: '活动配置', requiredRole: ['SUPER_ADMIN', 'OPERATOR'] }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrderList.vue'),
          meta: { title: '订单列表', requiredRole: ['SUPER_ADMIN', 'OPERATOR', 'SUPPORT'] }
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: () => import('@/views/OrderDetail.vue'),
          meta: { title: '订单详情', requiredRole: ['SUPER_ADMIN', 'OPERATOR', 'SUPPORT'] }
        },
        {
          path: 'mq/dead-letters',
          name: 'dead-letters',
          component: () => import('@/views/DeadLetterList.vue'),
          meta: { title: '死信管理', requiredRole: ['SUPER_ADMIN'] }
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UserList.vue'),
          meta: { title: '用户管理', requiredRole: ['SUPER_ADMIN', 'OPERATOR', 'SUPPORT'] }
        },
        {
          path: 'admins',
          name: 'admins',
          component: () => import('@/views/AdminList.vue'),
          meta: { title: '管理员管理', requiredRole: ['SUPER_ADMIN'] }
        }
      ]
    }
  ]
})

// 路由守卫：权限校验
router.beforeEach((to, _from, next) => {
  if (to.meta.public) {
    return next()
  }
  const adminStore = useAdminStore()
  if (!adminStore.isLoggedIn) {
    return next('/login')
  }
  const requiredRoles = to.meta.requiredRole as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0 && !requiredRoles.includes(adminStore.adminRole)) {
    // 无权限，跳转到有权限的页面
    return next('/dashboard')
  }
  next()
})

export default router
