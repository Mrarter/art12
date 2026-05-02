export default [
  {
    path: '/artist-score',
    name: 'ArtistScore',
    meta: { title: '艺术家评分', icon: 'Star' },
    children: [
      {
        path: 'list',
        name: 'ArtistScoreList',
        component: () => import('@/views/artist-score/ArtistScoreList.vue'),
        meta: { title: '评分列表' }
      },
      {
        path: 'identity-audit',
        name: 'ArtistIdentityAudit',
        component: () => import('@/views/artist-score/ArtistIdentityAudit.vue'),
        meta: { title: '资质审核' }
      },
      {
        path: 'detail/:artistId',
        name: 'ArtistScoreDetail',
        component: () => import('@/views/artist-score/ArtistScoreDetail.vue'),
        meta: { title: '评分详情' }
      }
    ]
  },
  {
    path: '/price-control',
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
      }
    ]
  }
]
