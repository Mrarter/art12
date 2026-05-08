<template>
  <view class="mine-page">
    <view class="page-glow"></view>

    <view class="topbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <text class="topbar-title">我的</text>
      <view class="topbar-actions">
        <view class="round-action" @click="goMessage">
          <image class="action-icon-img" src="/static/icons/bell.svg" mode="aspectFit"></image>
          <view class="action-dot" v-if="unreadCount > 0"></view>
        </view>
        <view class="round-action" @click="goSettings">
          <image class="action-icon-img" src="/static/icons/gear.svg" mode="aspectFit"></image>
        </view>
      </view>
    </view>

    <view class="profile-card" @click="handleProfileClick">
      <view class="profile-main" v-if="isLoggedIn">
        <image class="avatar" :src="userInfo.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
        <view class="profile-info">
          <view class="name-row">
            <text class="nickname">{{ userInfo.nickname || '拾艺局用户' }}</text>
            <text class="uid">UID {{ displayUid }}</text>
          </view>
          <view class="identity-tags">
            <text
              class="identity-tag"
              v-for="item in profileIdentityOptions"
              :key="item.value"
              :class="[item.value, { active: activeWorkspace === item.value }]"
              @click.stop="switchWorkspace(item.value)"
            >{{ item.label }}</text>
          </view>
          <view class="identity-active-hint">
            <text>{{ currentWorkspace.title }}</text>
          </view>
        </view>
        <view class="edit-link" @click.stop="goProfile">编辑</view>
      </view>

      <view class="login-main" v-else>
        <image class="avatar" src="/static/images/avatar.png" mode="aspectFill"></image>
        <view class="profile-info">
          <text class="login-title">登录 / 注册</text>
          <text class="login-desc">登录后查看订单、藏品和身份工作台</text>
        </view>
        <view class="login-btn">登录</view>
      </view>

      <view class="quick-stats" v-if="isLoggedIn">
        <view class="quick-stat" v-for="item in profileStats" :key="item.label" @click.stop="goPage(item.path, item.tab)">
          <text class="stat-value">{{ item.value }}</text>
          <text class="stat-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <view class="section identity-entry-section">
      <view class="section-head">
        <text class="section-title">身份入口</text>
      </view>
      <view class="identity-entry-grid">
        <view
          class="identity-entry"
          v-for="item in identityEntryItems"
          :key="item.key"
          @click="handleIdentityEntry(item)"
        >
          <view class="entry-icon" :class="item.tone">
            <image v-if="item.iconPath" class="icon-image" :src="item.iconPath" mode="aspectFit"></image>
            <text v-else>{{ item.icon }}</text>
          </view>
          <view class="entry-copy">
            <view class="entry-title-row">
              <text class="entry-title">{{ item.label }}</text>
              <text class="entry-tag" v-if="item.tag">{{ item.tag }}</text>
            </view>
            <text class="entry-desc">{{ item.desc }}</text>
          </view>
          <text class="chevron">›</text>
        </view>
      </view>
    </view>

    <view class="section" v-if="isLoggedIn">
      <view class="section-head">
        <text class="section-title">我的交易</text>
        <text class="section-link" @click="goOrderList('all')">全部订单</text>
      </view>
      <view class="order-grid">
        <view class="order-item" v-for="item in orderItems" :key="item.type" @click="goOrderList(item.type)">
          <view class="order-icon">
            <image v-if="item.iconPath" class="icon-image" :src="item.iconPath" mode="aspectFit"></image>
            <text v-else>{{ item.icon }}</text>
            <view class="badge" v-if="item.count > 0">{{ item.count }}</view>
          </view>
          <text class="entry-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <view class="section" v-if="isLoggedIn">
      <view class="section-head">
        <text class="section-title">我的艺术资产</text>
      </view>
      <view class="asset-grid">
        <view class="asset-entry" v-for="item in assetItems" :key="item.label" @click="goPage(item.path, item.tab)">
          <view class="entry-icon" :class="item.tone">
            <image v-if="item.iconPath" class="icon-image" :src="item.iconPath" mode="aspectFit"></image>
            <text v-else>{{ item.icon }}</text>
          </view>
          <view class="entry-copy">
            <text class="entry-title">{{ item.label }}</text>
            <text class="entry-desc">{{ item.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section workspace-section" v-if="isLoggedIn">
      <view class="section-head">
        <text class="section-title">身份工作台</text>
      </view>

      <scroll-view class="identity-switch" scroll-x enable-flex>
        <view
          v-for="item in availableWorkspaces"
          :key="item.value"
          class="identity-tab"
          :class="{ active: activeWorkspace === item.value }"
          @click="switchWorkspace(item.value)"
        >
          <text>{{ item.label }}</text>
        </view>
      </scroll-view>

      <view class="workspace-card">
        <view class="workspace-summary">
          <view>
            <text class="workspace-title">{{ currentWorkspace.title }}</text>
            <text class="workspace-desc">{{ currentWorkspace.desc }}</text>
          </view>
          <view class="workspace-status">{{ currentWorkspace.status }}</view>
        </view>

        <view class="workspace-metrics" v-if="currentWorkspace.metrics.length">
          <view class="metric" v-for="item in currentWorkspace.metrics" :key="item.label">
            <text class="metric-value">{{ item.value }}</text>
            <text class="metric-label">{{ item.label }}</text>
          </view>
        </view>

        <view class="workspace-actions">
          <view
            class="workspace-action"
            v-for="item in currentWorkspace.actions"
            :key="item.label"
            @click="goPage(item.path, item.tab)"
          >
            <view class="entry-icon" :class="item.tone">
              <image v-if="item.iconPath" class="icon-image" :src="item.iconPath" mode="aspectFit"></image>
              <text v-else>{{ item.icon }}</text>
            </view>
            <view class="entry-copy">
              <text class="entry-title">{{ item.label }}</text>
              <text class="entry-desc">{{ item.desc }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="section service-section">
      <view class="section-head">
        <text class="section-title">常用服务</text>
      </view>
      <view class="service-list">
        <view class="service-row" v-for="item in serviceItems" :key="item.label" @click="goPage(item.path, item.tab)">
          <view class="service-left">
            <view class="service-icon" :class="item.tone">
              <image v-if="item.iconPath" class="icon-image" :src="item.iconPath" mode="aspectFit"></image>
              <text v-else>{{ item.icon }}</text>
            </view>
            <text class="service-label">{{ item.label }}</text>
          </view>
          <text class="chevron">›</text>
        </view>
      </view>
    </view>

    <view class="safe-area-bottom"></view>
    <CustomTabBar :currentIndex="4" />
  </view>
</template>

<script>
import CustomTabBar from '@/components/custom-tab-bar/index.vue'
import { useUserStore } from '@/store/modules/user.js'
import { getOrderCounts } from '@/api/order.js'

const COMING_SOON = '/pages/common/coming-soon'

export default {
  components: {
    CustomTabBar
  },

  data() {
    return {
      statusBarHeight: 20,
      activeWorkspace: 'collector',
      unreadCount: 0,
      userStats: {
        favorites: 0,
        following: 0,
        history: 0,
        points: 0
      },
      assetStats: {
        purchased: 0,
        coupon: 0,
        wallet: '0.00'
      },
      artistStats: {
        works: 0,
        views: 0,
        favorites: 0,
        sold: 0
      },
      promoterStats: {
        withdrawable: '0.00',
        teamCount: 0,
        orderCount: 0,
        inviteCount: 0
      },
      agentStats: {
        artists: 0,
        works: 0,
        leads: 0
      },
      orderCounts: {
        pending: 0,
        paid: 0,
        shipped: 0,
        received: 0,
        review: 0
      }
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    },
    userInfo() {
      return this.userStore.userInfo || {}
    },
    isLoggedIn() {
      return this.userStore.isAuthenticated || this.userStore.isLogin
    },
    identities() {
      const raw = this.userInfo.identities || this.userInfo.identity_json || this.userInfo.identity || this.userStore.identities
      if (Array.isArray(raw)) return raw
      if (typeof raw === 'string') {
        try {
          const parsed = JSON.parse(raw)
          return Array.isArray(parsed) ? parsed : raw.split(',').filter(Boolean)
        } catch (e) {
          return raw.split(',').filter(Boolean)
        }
      }
      return ['collector']
    },
    isArtist() {
      return this.userInfo.isArtist || this.userStore.isArtist || this.identities.includes('artist')
    },
    isPromoter() {
      return this.userInfo.isPromoter || this.userStore.isPromoter || this.identities.includes('promoter')
    },
    isAgent() {
      return this.userInfo.isAgent || this.userStore.isAgent || this.identities.includes('agent')
    },
    displayUid() {
      return this.userInfo.uid || this.userInfo.userUid || this.userInfo.id || '------'
    },
    identityTags() {
      const tags = [{ value: 'collector', label: '收藏者' }]
      if (this.isArtist) tags.push({ value: 'artist', label: '艺术家' })
      if (this.isPromoter) tags.push({ value: 'promoter', label: '艺荐官' })
      if (this.isAgent) tags.push({ value: 'agent', label: '经纪人' })
      return tags
    },
    profileIdentityOptions() {
      return this.availableWorkspaces.map(item => ({
        ...item,
        label: item.value === 'artistApply' ? '艺术家入驻' : item.label
      }))
    },
    profileStats() {
      return [
        { label: '关注', value: this.formatCount(this.userStats.following), path: '/pages/user/following' },
        { label: '收藏', value: this.formatCount(this.userStats.favorites), path: '/pages/user/favorites' },
        { label: '浏览', value: this.formatCount(this.userStats.history), path: '/pages/user/history' },
        { label: '积分', value: this.formatCount(this.userStats.points), path: '/pages/user/points' }
      ]
    },
    orderItems() {
      return [
        { type: 'pending', label: '待付款', icon: '付', iconPath: '/static/art-icons/icon-payment.svg', count: this.orderCounts.pending || 0 },
        { type: 'paid', label: '待发货', icon: '发', iconPath: '/static/art-icons/icon-box.svg', count: this.orderCounts.paid || 0 },
        { type: 'shipped', label: '待收货', icon: '收', iconPath: '/static/art-icons/icon-location.svg', count: this.orderCounts.shipped || 0 },
        { type: 'review', label: '待评价', icon: '评', iconPath: '/static/art-icons/icon-comment.svg', count: this.orderCounts.review || 0 }
      ]
    },
    assetItems() {
      return [
        { label: '我的收藏', desc: '作品与艺术家', icon: '藏', iconPath: '/static/art-icons/icon-star.svg', tone: 'gold', path: '/pages/user/favorites' },
        { label: '已购作品', desc: `${this.assetStats.purchased} 件藏品`, icon: '购', iconPath: '/static/art-icons/icon-certificate.svg', tone: 'green', path: '/pages/user/purchased' },
        { label: '浏览记录', desc: '最近看过', icon: '览', iconPath: '/static/art-icons/icon-preview.svg', tone: 'blue', path: '/pages/user/history' },
        { label: '购物车', desc: '待收藏作品', icon: '车', iconPath: '/static/art-icons/icon-cart.svg', tone: 'red', path: '/pages/cart/index', tab: true },
        { label: '我的钱包', desc: `余额 ¥${this.assetStats.wallet}`, icon: '钱', iconPath: '/static/art-icons/icon-budget.svg', tone: 'gold', path: '/pages/user/wallet' },
        { label: '优惠券', desc: `${this.assetStats.coupon} 张可用`, icon: '券', iconPath: '/static/art-icons/icon-download.svg', tone: 'purple', path: '/pages/user/coupon' }
      ]
    },
    identityEntryItems() {
      return [
        {
          key: 'collectorLogin',
          label: this.isLoggedIn ? '藏家中心' : '藏家登录',
          desc: this.isLoggedIn ? '管理收藏、订单和个人资料' : '登录后管理收藏、订单和关注',
          icon: '藏',
          iconPath: '/static/art-icons/icon-collector.svg',
          tone: 'gold',
          path: this.isLoggedIn ? '/pages/user/profile' : '/pages/login/index',
          identity: 'collector',
          tag: this.isLoggedIn ? '已登录' : '登录'
        },
        {
          key: 'artistApply',
          label: '艺术家入驻',
          desc: '提交入驻资料，开通作品发布能力',
          icon: '入',
          iconPath: '/static/art-icons/icon-artist.svg',
          tone: 'green',
          path: '/pages/artist/apply',
          identity: 'artist',
          tag: this.isArtist ? '已开通' : '申请'
        },
        {
          key: 'artistCert',
          label: '艺术家认证',
          desc: '完善认证信息，获得平台认证标识',
          icon: '认',
          iconPath: '/static/art-icons/icon-verify.svg',
          tone: 'purple',
          path: '/pages/artist/cert',
          identity: 'artist',
          tag: '认证'
        },
        {
          key: 'promoterApply',
          label: '艺荐官加入',
          desc: '申请推广身份，分享作品获得佣金',
          icon: '荐',
          iconPath: '/static/art-icons/icon-share.svg',
          tone: 'blue',
          path: '/pages/promoter/index',
          identity: 'promoter',
          tag: this.isPromoter ? '已开通' : '加入'
        }
      ]
    },
    availableWorkspaces() {
      const list = [{ value: 'collector', label: '收藏者' }]
      if (this.isArtist) list.push({ value: 'artist', label: '艺术家' })
      if (this.isPromoter) list.push({ value: 'promoter', label: '艺荐官' })
      if (this.isAgent) list.push({ value: 'agent', label: '经纪人' })
      if (!this.isArtist) list.push({ value: 'artistApply', label: '艺术家入驻' })
      if (!this.isPromoter) list.push({ value: 'promoterApply', label: '艺荐官' })
      return list
    },
    currentWorkspace() {
      const configs = {
        collector: {
          title: '收藏者工作台',
          desc: '管理收藏、订单和关注的艺术家',
          status: '默认身份',
          metrics: [
            { label: '收藏', value: this.formatCount(this.userStats.favorites) },
            { label: '已购', value: this.formatCount(this.assetStats.purchased) },
            { label: '关注', value: this.formatCount(this.userStats.following) }
          ],
          actions: [
            { label: '我的收藏', desc: '作品与艺术家', icon: '藏', iconPath: '/static/art-icons/icon-star.svg', tone: 'gold', path: '/pages/user/favorites' },
            { label: '已购作品', desc: '查看藏品', icon: '购', iconPath: '/static/art-icons/icon-certificate.svg', tone: 'green', path: '/pages/user/purchased' },
            { label: '我的关注', desc: '关注的艺术家', icon: '关', iconPath: '/static/art-icons/icon-follow.svg', tone: 'blue', path: '/pages/user/following' },
            { label: '收藏证书', desc: '已购作品凭证', icon: '证', iconPath: '/static/art-icons/icon-verify.svg', tone: 'purple', path: this.comingSoon('收藏证书', '收藏证书列表正在整理中，可先从已购作品进入单件作品证书。') }
          ]
        },
        artist: {
          title: '艺术家工作台',
          desc: '发布作品、管理作品和查看销售状态',
          status: '已开通',
          metrics: [
            { label: '作品', value: this.formatCount(this.artistStats.works) },
            { label: '浏览', value: this.formatCount(this.artistStats.views) },
            { label: '收藏', value: this.formatCount(this.artistStats.favorites) }
          ],
          actions: [
            { label: '艺术家主页', desc: '查看公开主页', icon: '页', iconPath: '/static/art-icons/icon-artist.svg', tone: 'gold', path: `/pages/artist/home?id=${this.userInfo.id || ''}` },
            { label: '作品管理', desc: '上下架与编辑', icon: '管', iconPath: '/static/art-icons/icon-work.svg', tone: 'blue', path: '/pages/artist/manage' },
            { label: '发布作品', desc: '提交新作品', icon: '发', iconPath: '/static/art-icons/icon-gallery.svg', tone: 'green', path: '/pages/artist/publish' },
            { label: '卖出订单', desc: '处理交易履约', icon: '单', iconPath: '/static/art-icons/icon-document.svg', tone: 'red', path: '/pages/order/list?type=sold' },
            { label: '认证信息', desc: '查看认证状态', icon: '认', iconPath: '/static/art-icons/icon-verify.svg', tone: 'purple', path: '/pages/artist/cert' }
          ]
        },
        promoter: {
          title: '艺荐官工作台',
          desc: '查看佣金、团队和邀请转化',
          status: '已开通',
          metrics: [
            { label: '可提现', value: `¥${this.promoterStats.withdrawable}` },
            { label: '团队', value: this.formatCount(this.promoterStats.teamCount) },
            { label: '推广单', value: this.formatCount(this.promoterStats.orderCount) }
          ],
          actions: [
            { label: '推广中心', desc: '佣金总览', icon: '推', iconPath: '/static/art-icons/icon-share.svg', tone: 'gold', path: '/pages/promoter/index' },
            { label: '佣金明细', desc: '订单佣金流水', icon: '佣', iconPath: '/static/art-icons/icon-budget.svg', tone: 'green', path: '/pages/promoter/earnings' },
            { label: '我的团队', desc: '成员与贡献', icon: '队', iconPath: '/static/art-icons/icon-followed.svg', tone: 'blue', path: '/pages/promoter/team' },
            { label: '邀请海报', desc: '生成推广海报', icon: '邀', iconPath: '/static/art-icons/icon-download.svg', tone: 'purple', path: '/pages/promoter/poster' },
            { label: '提现记录', desc: '查看提现进度', icon: '提', iconPath: '/static/art-icons/icon-payment.svg', tone: 'red', path: '/pages/promoter/withdrawLog' }
          ]
        },
        agent: {
          title: '经纪人工作台',
          desc: '管理代理艺术家、代理作品和销售线索',
          status: '预留身份',
          metrics: [
            { label: '艺术家', value: this.formatCount(this.agentStats.artists) },
            { label: '代理作品', value: this.formatCount(this.agentStats.works) },
            { label: '线索', value: this.formatCount(this.agentStats.leads) }
          ],
          actions: [
            { label: '代理艺术家', desc: '维护代理关系', icon: '艺', iconPath: '/static/art-icons/icon-artist.svg', tone: 'gold', path: this.comingSoon('代理艺术家', '经纪人代理艺术家列表正在规划中。') },
            { label: '代理作品', desc: '查看代理作品', icon: '作', iconPath: '/static/art-icons/icon-work.svg', tone: 'blue', path: this.comingSoon('代理作品', '经纪人代理作品管理正在规划中。') },
            { label: '销售线索', desc: '收藏咨询跟进', icon: '索', iconPath: '/static/art-icons/icon-consultant.svg', tone: 'green', path: this.comingSoon('销售线索', '经纪人线索池正在规划中。') }
          ]
        },
        artistApply: {
          title: '成为认证艺术家',
          desc: '通过认证后可发布作品、管理作品和展示艺术家主页',
          status: '未开通',
          metrics: [],
          actions: [
            { label: '提交认证', desc: '填写艺术家资料', icon: '认', iconPath: '/static/art-icons/icon-verify.svg', tone: 'gold', path: '/pages/artist/cert' },
            { label: '入驻申请', desc: '完善入驻信息', icon: '入', iconPath: '/static/art-icons/icon-artist.svg', tone: 'green', path: '/pages/artist/apply' }
          ]
        },
        promoterApply: {
          title: '开通艺荐官',
          desc: '分享作品获得佣金，管理团队与邀请关系',
          status: '未开通',
          metrics: [],
          actions: [
            { label: '了解艺荐官', desc: '查看规则与权益', icon: '规', iconPath: '/static/art-icons/icon-share.svg', tone: 'gold', path: '/pages/promoter/index' },
            { label: '分销规则', desc: '佣金与提现说明', icon: '则', iconPath: '/static/art-icons/icon-document.svg', tone: 'blue', path: '/pages/distribution/rules' }
          ]
        }
      }
      return configs[this.activeWorkspace] || configs.collector
    },
    serviceItems() {
      return [
        { label: '收货地址', icon: '址', iconPath: '/static/art-icons/icon-location.svg', tone: 'gold', path: '/pages/user/address' },
        { label: '消息通知', icon: '息', iconPath: '/static/icons/bell.svg', tone: 'blue', path: '/pages/message/list' },
        { label: '帮助中心', icon: '帮', iconPath: '/static/art-icons/icon-headset.svg', tone: 'green', path: '/pages/help/index' },
        { label: '意见反馈', icon: '馈', iconPath: '/static/art-icons/icon-comment.svg', tone: 'purple', path: '/pages/user/feedback' },
        { label: '通知设置', icon: '通', iconPath: '/static/icons/gear.svg', tone: 'blue', path: '/pages/setting/notification' },
        { label: '关于我们', icon: '关', iconPath: '/static/art-icons/icon-profile.svg', tone: 'gold', path: '/pages/about/index' }
      ]
    }
  },

  onShow() {
    this.initPage()
  },

  methods: {
    async initPage() {
      const systemInfo = uni.getSystemInfoSync()
      this.statusBarHeight = systemInfo.statusBarHeight || 20
      await this.userStore.initUserInfo()
      if (this.isLoggedIn) {
        this.syncDefaultWorkspace()
        this.loadLocalStats()
        this.loadOrderCounts()
      }
    },
    syncDefaultWorkspace() {
      const hasActive = this.availableWorkspaces.some(item => item.value === this.activeWorkspace)
      if (hasActive) return
      if (this.isArtist) this.activeWorkspace = 'artist'
      else if (this.isPromoter) this.activeWorkspace = 'promoter'
      else if (this.isAgent) this.activeWorkspace = 'agent'
      else this.activeWorkspace = 'collector'
    },
    switchWorkspace(value) {
      if (!value || !this.availableWorkspaces.some(item => item.value === value)) return
      this.activeWorkspace = value
    },
    loadLocalStats() {
      const center = this.userStore.centerData || {}
      this.userStats = {
        favorites: center.favoriteCount || center.favorites || 0,
        following: center.followingCount || center.following || 0,
        history: center.historyCount || 0,
        points: center.points || this.userInfo.points || 0
      }
      this.assetStats = {
        purchased: center.purchasedCount || 0,
        coupon: center.couponCount || 0,
        wallet: center.balance || this.userInfo.balance || '0.00'
      }
      this.artistStats = {
        works: center.artworkCount || center.workCount || 0,
        views: center.viewCount || 0,
        favorites: center.artworkFavoriteCount || 0,
        sold: center.soldCount || 0
      }
      this.promoterStats = {
        withdrawable: center.withdrawable || '0.00',
        teamCount: center.teamCount || 0,
        orderCount: center.promoterOrderCount || 0,
        inviteCount: center.inviteCount || 0
      }
      this.unreadCount = center.unreadCount || 0
    },
    async loadOrderCounts() {
      try {
        const res = await getOrderCounts()
        this.orderCounts = { ...this.orderCounts, ...(res || {}) }
      } catch (e) {
        console.log('获取订单数量失败', e)
      }
    },
    handleProfileClick() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/index' })
      }
    },
    goProfile() {
      this.goPage('/pages/user/profile')
    },
    goMessage() {
      this.goPage('/pages/message/list')
    },
    goSettings() {
      this.goPage('/pages/user/settings')
    },
    goOrderList(type = 'all') {
      this.goPage(`/pages/order/list?type=${type}`)
    },
    handleIdentityEntry(item) {
      if (!item) return
      if (!this.isLoggedIn) {
        const redirect = item.path && !item.path.includes('/pages/login') ? `&redirect=${encodeURIComponent(item.path)}` : ''
        uni.navigateTo({ url: `/pages/login/index?identity=${item.identity || 'collector'}${redirect}` })
        return
      }
      this.goPage(item.path)
    },
    goPage(path, isTab = false) {
      if (!path) return
      if (!this.isLoggedIn && !path.includes('/pages/login') && !path.includes('/pages/help') && !path.includes('/pages/about')) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      if (isTab || this.isTabPath(path)) {
        uni.switchTab({ url: path.split('?')[0] })
      } else {
        uni.navigateTo({ url: path })
      }
    },
    isTabPath(path) {
      const purePath = path.split('?')[0]
      return [
        '/pages/index/index',
        '/pages/gallery/index',
        '/pages/auction/index',
        '/pages/cart/index',
        '/pages/user/index'
      ].includes(purePath)
    },
    comingSoon(title, desc) {
      return `${COMING_SOON}?title=${encodeURIComponent(title)}&desc=${encodeURIComponent(desc)}`
    },
    formatCount(count) {
      const value = Number(count) || 0
      if (value >= 10000) return (value / 10000).toFixed(1) + 'w'
      if (value >= 1000) return (value / 1000).toFixed(1) + 'k'
      return String(value)
    }
  }
}
</script>

<style lang="scss" scoped>
$bg: #0b0b0c;
$panel: #171719;
$panel-2: #202024;
$line: rgba(255, 255, 255, 0.08);
$text: #f6f2e8;
$muted: #9b958a;
$dim: #68645c;
$gold: #c9a227;
$green: #58b982;
$blue: #5f8fc7;
$red: #c96262;
$purple: #8c73c9;

.mine-page {
  min-height: 100vh;
  position: relative;
  background: $bg;
  color: $text;
  padding: 0 24rpx;
  box-sizing: border-box;
}

.page-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 460rpx;
  background:
    radial-gradient(circle at 18% 12%, rgba($gold, 0.22), transparent 36%),
    linear-gradient(180deg, #17130d 0%, rgba(11, 11, 12, 0) 100%);
  pointer-events: none;
}

.topbar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 24rpx;
}

.topbar-title {
  font-size: 34rpx;
  font-weight: 700;
}

.topbar-actions {
  display: flex;
  gap: 16rpx;
}

.round-action {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.action-icon-img {
  width: 34rpx;
  height: 34rpx;
  opacity: 0.88;
}

.action-dot {
  position: absolute;
  top: 10rpx;
  right: 10rpx;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $red;
}

.profile-card,
.section {
  position: relative;
  z-index: 1;
  background: rgba(23, 23, 25, 0.94);
  border: 1rpx solid $line;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}

.profile-card {
  padding: 28rpx;
}

.profile-main,
.login-main {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: $panel-2;
  border: 2rpx solid rgba($gold, 0.35);
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.nickname,
.login-title {
  font-size: 36rpx;
  line-height: 44rpx;
  font-weight: 700;
  color: $text;
}

.uid,
.login-desc {
  font-size: 22rpx;
  color: $muted;
}

.identity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 16rpx;
}

.identity-tag {
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: $gold;
  background: rgba($gold, 0.14);
  border: 1rpx solid transparent;
  transition: all 0.2s ease;

  &.active {
    color: #16130b;
    background: $gold;
    border-color: $gold;
    font-weight: 700;
  }

  &.artist {
    color: $green;
    background: rgba($green, 0.14);
  }

  &.artist.active,
  &.artistApply.active {
    color: #101512;
    background: $green;
    border-color: $green;
  }

  &.promoter {
    color: $blue;
    background: rgba($blue, 0.14);
  }

  &.promoter.active,
  &.promoterApply.active {
    color: #101317;
    background: $blue;
    border-color: $blue;
  }

  &.agent {
    color: $purple;
    background: rgba($purple, 0.14);
  }

  &.agent.active {
    color: #141018;
    background: $purple;
    border-color: $purple;
  }
}

.identity-active-hint {
  margin-top: 10rpx;
  font-size: 21rpx;
  color: $dim;
}

.edit-link,
.login-btn,
.section-link,
.workspace-status {
  padding: 10rpx 18rpx;
  border-radius: 8rpx;
  color: #16130b;
  background: $gold;
  font-size: 24rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.quick-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid $line;
}

.quick-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}

.stat-value {
  font-size: 30rpx;
  font-weight: 700;
  color: $text;
}

.stat-label,
.entry-label,
.metric-label {
  font-size: 22rpx;
  color: $muted;
}

.section {
  padding: 24rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
}

.section-link {
  background: rgba($gold, 0.16);
  color: $gold;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
}

.order-item {
  min-height: 128rpx;
  background: $panel-2;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}

.order-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba($gold, 0.14);
  color: $gold;
  font-size: 24rpx;
  font-weight: 700;
}

.badge {
  position: absolute;
  top: -10rpx;
  right: -12rpx;
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 8rpx;
  border-radius: 15rpx;
  background: $red;
  color: #fff;
  font-size: 18rpx;
  line-height: 30rpx;
  text-align: center;
  box-sizing: border-box;
}

.asset-grid,
.workspace-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
}

.identity-entry-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14rpx;
}

.identity-entry {
  min-height: 108rpx;
  padding: 18rpx;
  border-radius: 12rpx;
  background: $panel-2;
  display: flex;
  align-items: center;
  gap: 16rpx;
  box-sizing: border-box;
}

.asset-entry,
.workspace-action {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 116rpx;
  padding: 18rpx;
  border-radius: 12rpx;
  background: $panel-2;
  box-sizing: border-box;
}

.entry-icon,
.service-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 700;
  color: $gold;
  background: rgba($gold, 0.15);
  flex-shrink: 0;

  &.green {
    color: $green;
    background: rgba($green, 0.15);
  }

  &.blue {
    color: $blue;
    background: rgba($blue, 0.15);
  }

  &.red {
    color: $red;
    background: rgba($red, 0.15);
  }

  &.purple {
    color: $purple;
    background: rgba($purple, 0.15);
  }
}

.icon-image {
  width: 30rpx;
  height: 30rpx;
  opacity: 0.92;
}

.entry-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.entry-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.entry-title {
  font-size: 26rpx;
  color: $text;
  font-weight: 600;
}

.entry-tag {
  padding: 4rpx 10rpx;
  border-radius: 8rpx;
  font-size: 19rpx;
  color: $gold;
  background: rgba($gold, 0.14);
  flex-shrink: 0;
}

.entry-desc {
  font-size: 21rpx;
  color: $dim;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.identity-switch {
  white-space: nowrap;
  margin-bottom: 18rpx;
}

.identity-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 132rpx;
  height: 58rpx;
  margin-right: 12rpx;
  padding: 0 20rpx;
  border-radius: 10rpx;
  background: $panel-2;
  color: $muted;
  font-size: 24rpx;
  box-sizing: border-box;

  &.active {
    color: #16130b;
    background: $gold;
    font-weight: 700;
  }
}

.workspace-card {
  background: rgba(255, 255, 255, 0.035);
  border-radius: 14rpx;
  padding: 20rpx;
}

.workspace-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.workspace-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: $text;
  margin-bottom: 8rpx;
}

.workspace-desc {
  font-size: 23rpx;
  color: $muted;
  line-height: 34rpx;
}

.workspace-status {
  background: rgba($gold, 0.16);
  color: $gold;
}

.workspace-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.metric {
  min-height: 92rpx;
  border-radius: 12rpx;
  background: $panel-2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.metric-value {
  font-size: 28rpx;
  color: $text;
  font-weight: 700;
}

.service-list {
  background: $panel-2;
  border-radius: 12rpx;
  overflow: hidden;
}

.service-row {
  min-height: 92rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid $line;

  &:last-child {
    border-bottom: none;
  }
}

.service-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.service-icon {
  width: 44rpx;
  height: 44rpx;
  border-radius: 10rpx;
  font-size: 20rpx;
}

.service-label {
  font-size: 27rpx;
  color: $text;
}

.chevron {
  color: $dim;
  font-size: 34rpx;
}

.safe-area-bottom {
  height: calc(130rpx + env(safe-area-inset-bottom));
}
</style>
