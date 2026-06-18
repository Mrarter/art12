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
            meta: { title: '经纪人管理' }
          },
          {
            path: 'user-profile',
            name: 'UserProfile',
            component: () => import('@/views/user/user-profile.vue'),
            meta: { title: '用户画像' }
          },
          {
            path: 'realname',
            name: 'RealnameAudit',
            component: () => import('@/views/user/realname.vue'),
            meta: { title: '实名认证' }
          },
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
            meta: { title: '作品分类' }
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
          },
          {
            path: 'deal-report',
            name: 'AuctionDealReport',
            component: () => import('@/views/auction/deal-report.vue'),
            meta: { title: '成交统计' }
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
            meta: { title: '经纪人分成记录' }
          },
          {
            path: 'withdraw',
            name: 'Withdraw',
            component: () => import('@/views/promotion/withdraw.vue'),
            meta: { title: '提现管理' }
          },
          {
            path: 'report',
            name: 'PromotionReport',
            component: () => import('@/views/promotion/report.vue'),
            meta: { title: '统计报表' }
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
          },
          {
            path: 'content-review',
            name: 'ContentReview',
            component: () => import('@/views/community/content-review.vue'),
            meta: { title: '内容审核' }
          }
        ]
      },
      {
        path: 'trade',
        name: 'Trade',
        meta: { title: '收藏交易', icon: 'ShoppingCart' },
        children: [
          {
            path: 'intents',
            name: 'IntentList',
            component: () => import('@/views/order/IntentList.vue'),
            meta: { title: '收藏意向单' }
          },
          {
            path: 'orders',
            name: 'TradeOrderList',
            component: () => import('@/views/order/OrderList.vue'),
            meta: { title: '正式订单' }
          },
          {
            path: 'certificates',
            name: 'CertificateList',
            component: () => import('@/views/certificate/CertificateList.vue'),
            meta: { title: '收藏证书' }
          }
        ]
      },
      {
        path: 'price-control',
        name: 'PriceControl',
        meta: { title: '交易调控', icon: 'TrendCharts' },
        children: [
          {
            path: 'artwork',
            name: 'ArtworkPriceControl',
            component: () => import('@/views/artist-score/ArtworkPriceControl.vue'),
            meta: { title: '价格调控' }
          },
          {
            path: 'logs',
            name: 'ArtworkPriceLog',
            component: () => import('@/views/artist-score/ArtworkPriceLog.vue'),
            meta: { title: '价格日志' }
          },
          {
            path: 'price-rule',
            name: 'PriceRuleConfig',
            component: () => import('@/views/system/PriceRuleConfig.vue'),
            meta: { title: '涨价规则' }
          }
        ]
      },
      {
        path: 'resale',
        name: 'Resale',
        meta: { title: '转售管理', icon: 'RefreshRight' },
        children: [
          {
            path: 'list',
            name: 'ResaleList',
            component: () => import('@/views/resale/ResaleList.vue'),
            meta: { title: '转售记录' }
          },
          {
            path: 'stats',
            name: 'ResaleStats',
            component: () => import('@/views/resale/ResaleStats.vue'),
            meta: { title: '流通数据统计' }
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
            path: 'message',
            name: 'Message',
            component: () => import('@/views/message/index.vue'),
            meta: { title: '消息通知' }
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
          },
          {
            path: 'operation-log',
            name: 'OperationLog',
            component: () => import('@/views/system/operation-log.vue'),
            meta: { title: '操作日志' }
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
    document.title = `${to.meta.title} - 艺本艺术后台`
  }
  const token = localStorage.getItem('admin_token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else {
    next()
  }
})

export default router
