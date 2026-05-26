import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomePage.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginPage.vue')
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('@/views/ProductList.vue')
    },
    {
      path: '/products/:productId',
      name: 'product-detail',
      component: () => import('@/views/ProductDetail.vue')
    },
    {
      path: '/seckill',
      name: 'seckill',
      component: () => import('@/views/SeckillList.vue')
    },
    {
      path: '/seckill/:activityId',
      name: 'seckill-detail',
      component: () => import('@/views/SeckillDetail.vue')
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/OrderList.vue')
    },
    {
      path: '/orders/:orderId',
      name: 'order-detail',
      component: () => import('@/views/OrderDetail.vue')
    }
  ]
})

export default router
