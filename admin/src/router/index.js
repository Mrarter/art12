import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/views/Layout.vue'
import Login from '@/views/Login.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '控制台', icon: 'Odometer' }
      },
      {
        path: 'user',
        name: 'User',
        meta: { title: '用户管理', icon: 'User' },
        children: [
          {
            path: 'list',
            name: 'UserList',
            component: () => import('@/views/user/list.vue'),
            meta: { title: '用户列表' }
          },
          {
            path: 'artist',
            name: 'ArtistAudit',
            component: () => import('@/views/user/artist.vue'),
            meta: { title: '艺术家认证' }
          },
          {
            path: 'promoter',
            name: 'PromoterList',
            component: () => import('@/views/user/promoter.vue'),
            meta: { title: '艺荐官管理' }
          }
        ]
      },
      {
        path: 'product',
        name: 'Product',
        meta: { title: '作品管理', icon: 'Goods' },
        children: [
          {
            path: 'list',
            name: 'ProductList',
            component: () => import('@/views/product/list.vue'),
            meta: { title: '作品列表' }
          },
          {
            path: 'category',
            name: 'Category',
            component: () => import('@/views/product/category.vue'),
            meta: { title: '分类管理' }
          },
          {
            path: 'audit',
            name: 'ProductAudit',
            component: () => import('@/views/product/audit.vue'),
            meta: { title: '审核管理' }
          }
        ]
      },
      {
        path: 'order',
        name: 'Order',
        meta: { title: '订单管理', icon: 'ShoppingCart' },
        children: [
          {
            path: 'list',
            name: 'OrderList',
            component: () => import('@/views/order/list.vue'),
            meta: { title: '订单列表' }
          },
          {
            path: 'aftersale',
            name: 'AfterSale',
            component: () => import('@/views/order/aftersale.vue'),
            meta: { title: '售后管理' }
          }
        ]
      },
      {
        path: 'auction',
        name: 'Auction',
        meta: { title: '拍卖管理', icon: 'Hammer' },
        children: [
          {
            path: 'session',
            name: 'AuctionSession',
            component: () => import('@/views/auction/session.vue'),
            meta: { title: '专场管理' }
          },
          {
            path: 'lot',
            name: 'AuctionLot',
            component: () => import('@/views/auction/lot.vue'),
            meta: { title: '拍品管理' }
          },
          {
            path: 'record',
            name: 'AuctionRecord',
            component: () => import('@/views/auction/record.vue'),
            meta: { title: '竞拍记录' }
          }
        ]
      },
      {
        path: 'promotion',
        name: 'Promotion',
        meta: { title: '分销管理', icon: 'Share' },
        children: [
          {
            path: 'commission',
            name: 'Commission',
            component: () => import('@/views/promotion/commission.vue'),
            meta: { title: '佣金记录' }
          },
          {
            path: 'withdraw',
            name: 'Withdraw',
            component: () => import('@/views/promotion/withdraw.vue'),
            meta: { title: '提现管理' }
          }
        ]
      },
      {
        path: 'community',
        name: 'Community',
        meta: { title: '社区管理', icon: 'ChatDotRound' },
        children: [
          {
            path: 'post',
            name: 'Post',
            component: () => import('@/views/community/post.vue'),
            meta: { title: '帖子管理' }
          },
          {
            path: 'comment',
            name: 'Comment',
            component: () => import('@/views/community/comment.vue'),
            meta: { title: '评论管理' }
          },
          {
            path: 'topic',
            name: 'Topic',
            component: () => import('@/views/community/topic.vue'),
            meta: { title: '话题管理' }
          }
        ]
      },
      {
        path: 'system',
        name: 'System',
        meta: { title: '系统设置', icon: 'Setting' },
        children: [
          {
            path: 'banner',
            name: 'Banner',
            component: () => import('@/views/system/banner.vue'),
            meta: { title: 'Banner管理' }
          },
          {
            path: 'config',
            name: 'Config',
            component: () => import('@/views/system/config.vue'),
            meta: { title: '参数配置' }
          },
          {
            path: 'admin',
            name: 'Admin',
            component: () => import('@/views/system/admin.vue'),
            meta: { title: '管理员' }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 拾艺局后台`
  }
  const token = localStorage.getItem('admin_token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else {
    next()
  }
})

export default router
