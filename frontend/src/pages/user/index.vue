<template>
  <view class="mine-page">
    <view class="page-glow"></view>

    <view class="topbar" :style="topbarStyle">
      <text class="topbar-title">我的</text>
      <view class="topbar-actions" :style="topbarActionsStyle">
        <view class="round-action" @click="goMessage">
          <image class="action-icon-img" src="/static/icons/bell.svg" mode="aspectFit"></image>
          <view class="action-badge" v-if="totalUnreadCount > 0">{{ totalUnreadCount > 99 ? '99+' : totalUnreadCount }}</view>
        </view>
        <view class="round-action" @click="goSettings">
          <image class="action-icon-img" src="/static/icons/gear.svg" mode="aspectFit"></image>
        </view>
      </view>
    </view>

    <view class="profile-card" @click="handleProfileClick">
      <view class="profile-main" v-if="isLoggedIn">
        <view class="avatar-wrap">
          <image class="avatar" :src="userInfo.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
          <view class="avatar-gold-v" v-if="isArtist">V</view>
        </view>
        <view class="profile-info">
          <view class="name-row">
            <text class="nickname">{{ userInfo.nickname || '艺本艺术用户' }}</text>
            <text class="uid" v-if="showCertifiedUid">UID {{ displayUid }}</text>
          </view>
          <view class="identity-tags" v-if="identityTags.length">
            <text
              class="identity-tag"
              v-for="item in identityTags"
              :key="item.value"
              :class="[item.value, { active: activeWorkspace === item.value, clickable: isIdentityTagClickable(item) }]"
              @tap.stop="handleIdentityTagClick(item)"
              @click.stop="handleIdentityTagClick(item)"
            >{{ item.label }}</text>
          </view>
          <view class="identity-active-hint" v-if="hasCertifiedIdentity">
            <text>{{ currentWorkspace.title }}</text>
          </view>
        </view>
        <view class="profile-actions">
          <view class="home-link" v-if="isArtist" @click.stop="goPersonalHome">个人主页</view>
        </view>
      </view>

      <view class="login-main" v-else>
        <view class="avatar-wrap">
          <image class="avatar" src="/static/images/avatar.png" mode="aspectFill"></image>
        </view>
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

    <view class="section artist-cert-entry" v-if="showArtistCertificationEntry" @click="goArtistCertification">
      <view class="artist-cert-copy">
        <view class="artist-cert-head">
          <text class="artist-cert-title">{{ artistCertificationCard.title }}</text>
          <text
            v-if="artistCertificationCard.badge"
            class="artist-cert-badge"
            :class="artistCertificationCard.badgeTone"
          >{{ artistCertificationCard.badge }}</text>
        </view>
        <text class="artist-cert-desc">{{ artistCertificationCard.desc }}</text>
        <text v-if="artistCertificationCard.tip" class="artist-cert-tip">{{ artistCertificationCard.tip }}</text>
      </view>
      <view v-if="artistCertificationCard.cta" class="artist-cert-cta" :class="artistCertificationCard.ctaTone">
        <text>{{ artistCertificationCard.cta }}</text>
        <text class="artist-cert-arrow">›</text>
      </view>
    </view>

    <view class="section" v-if="isLoggedIn">
      <view class="section-head">
        <text class="section-title">我的交易</text>
        <text class="section-link" @click="goPage('/pages/order/list?type=all')">全部订单</text>
      </view>
      <view class="order-grid">
        <view class="order-item" v-for="item in orderItems" :key="item.type" @click="handleTransactionEntry(item)">
          <view class="order-icon">
            <image v-if="item.iconPath" class="icon-image" :src="item.iconPath" mode="aspectFit"></image>
            <text v-else>{{ item.icon }}</text>
            <view class="badge" v-if="item.count > 0">{{ item.count }}</view>
          </view>
          <text class="entry-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <view class="section workspace-section" v-if="isLoggedIn">
      <view class="section-head">
        <text class="section-title">身份工作台</text>
      </view>

      <scroll-view class="identity-switch" scroll-x enable-flex v-if="availableWorkspaces.length > 1">
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
          <view class="workspace-status" v-if="currentWorkspace.status">{{ currentWorkspace.status }}</view>
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
            @click="handleWorkspaceAction(item)"
          >
            <view class="entry-icon" :class="item.tone">
              <image v-if="item.iconPath" class="icon-image" :src="item.iconPath" mode="aspectFit"></image>
              <text v-else>{{ item.icon }}</text>
            </view>
            <view class="entry-copy">
              <view class="entry-title-row">
                <text class="entry-title">{{ item.label }}</text>
                <view class="workspace-action-badge inline" v-if="item.badgeCount > 0">{{ item.badgeCount > 99 ? '99+' : item.badgeCount }}</view>
              </view>
              <text class="entry-desc">{{ item.desc }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="workspace-assets" v-if="assetItems.length">
        <text class="workspace-subtitle">艺术资产</text>
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
    </view>

    <view class="safe-area-bottom"></view>
    <CustomTabBar :currentIndex="4" />
  </view>
</template>

<script>
import CustomTabBar from '@/components/custom-tab-bar/index.vue'
import { useUserStore } from '@/store/modules/user.js'
import { getOrderCounts, getOrderList } from '@/api/order.js'
import { getArtistCertStatus } from '@/api/user.js'
import { getUnreadCertificateSignNoticeCount } from '@/utils/certificateNotice'
import { AUCTION_ENABLED, isAuctionPath, showAuctionDisabledToast } from '@/utils/platform.js'
import { checkTokenValid, saveRedirectUrl } from '@/utils/auth.js'

const COMING_SOON = '/pages/common/coming-soon'

export default {
  components: {
    CustomTabBar
  },

  data() {
    return {
      statusBarHeight: 20,
      navRightSafeInset: 0,
      activeWorkspace: 'collector',
      workspaceManuallySelected: false,
      unreadCount: 0,
      certificateUnreadCount: 0,
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
      artistCertStatus: null,
      artistCertApprovedDismissed: false,
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
    topbarStyle() {
      return {
        paddingTop: `${this.statusBarHeight}px`
      }
    },
    topbarActionsStyle() {
      if (!this.navRightSafeInset) return {}
      return {
        marginRight: `${this.navRightSafeInset}px`
      }
    },
    userStore() {
      return useUserStore()
    },
    userInfo() {
      return this.userStore.userInfo || {}
    },
    isLoggedIn() {
      return this.hasAccountLogin()
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
    normalizedIdentities() {
      return this.identities.map(item => String(item).trim().toLowerCase()).filter(Boolean)
    },
    artistStatus() {
      const status = this.artistCertStatus?.status ?? this.userInfo.artistStatus ?? this.userInfo.certStatus ?? this.userStore.centerData?.artistStatus
      if (status === null || status === undefined || status === '') return null
      const numeric = Number(status)
      return Number.isNaN(numeric) ? null : numeric
    },
    hasLegacyArtistFlag() {
      return Boolean(this.userInfo.isArtist || this.userStore.isArtist || this.normalizedIdentities.some(item => ['artist', 'certified_artist', 'verified_artist', '艺术家', '认证艺术家'].includes(item)))
    },
    isArtistPending() {
      return this.artistStatus === 0
    },
    isArtistRejected() {
      return this.artistStatus === 2
    },
    isArtist() {
      return this.artistStatus === 1 || (this.artistStatus === null && this.hasLegacyArtistFlag)
    },
    isPromoter() {
      return this.userInfo.isPromoter || this.userStore.isPromoter || this.normalizedIdentities.some(item => ['promoter', 'certified_promoter', 'verified_promoter', '经纪人', '认证经纪人'].includes(item))
    },
    isAgent() {
      return this.userInfo.isAgent || this.userStore.isAgent || this.normalizedIdentities.some(item => ['agent', '经纪人', '认证经纪人'].includes(item))
    },
    showIdentityWorkspace() {
      return this.isArtist || this.isPromoter
    },
    displayUid() {
      return this.userInfo.uid || this.userInfo.userUid || this.userInfo.id || '------'
    },
    hasCertifiedIdentity() {
      return this.isArtist || this.isPromoter || this.isAgent
    },
    showCertifiedUid() {
      return this.hasCertifiedIdentity
    },
    totalUnreadCount() {
      return Number(this.certificateUnreadCount || 0)
    },
    normalizedArtistCertStatus() {
      const status = this.artistCertStatus?.status
      if (status === 0 || status === '0' || status === 'pending') return 'pending'
      if (status === 1 || status === '1' || status === 'approved' || this.artistCertStatus?.isArtist === true || this.isArtist) return 'approved'
      if (status === 2 || status === '2' || status === 'rejected') return 'rejected'
      return 'none'
    },
    showArtistCertificationEntry() {
      if (!this.isLoggedIn) return false
      if (this.normalizedArtistCertStatus !== 'approved') return true
      return !this.artistCertApprovedDismissed
    },
    artistCertificationCard() {
      if (this.normalizedArtistCertStatus === 'approved') {
        return {
          title: '艺术家认证已开通',
          desc: '已获得认证标识、作品发布权限和艺术家主页展示能力。',
          tip: '',
          badge: '已认证',
          badgeTone: 'approved',
          cta: '',
          ctaTone: ''
        }
      }
      if (this.normalizedArtistCertStatus === 'pending') {
        return {
          title: '艺术家身份认证中',
          desc: '认证资料已提交，平台正在审核，通过后将开通艺术家主页与作品发布。',
          tip: '审核期间可随时查看当前进度。',
          badge: '审核中',
          badgeTone: 'pending',
          cta: '查看进度',
          ctaTone: 'pending'
        }
      }
      if (this.normalizedArtistCertStatus === 'rejected') {
        return {
          title: '艺术家认证未通过',
          desc: this.artistCertStatus?.rejectReason || '可根据审核意见补充资料后重新提交认证申请。',
          tip: '认证成功后可以在平台销售作品。',
          badge: '待重提',
          badgeTone: 'rejected',
          cta: '重新认证',
          ctaTone: 'rejected'
        }
      }
      return {
        title: '申请艺术家认证',
        desc: '完成认证后可发布作品、展示个人主页并获得平台认证标识。',
        tip: '提交前请准备真实身份信息和 20 件代表作品。',
        badge: '',
        badgeTone: '',
        cta: '去认证',
        ctaTone: 'default'
      }
    },
    identityTags() {
      const tags = []
      if (this.isArtist) tags.push({ value: 'artist', label: '认证艺术家' })
      else if (this.isArtistPending) tags.push({ value: 'artistPending', label: '艺术家身份认证中' })
      else if (this.isArtistRejected) tags.push({ value: 'artistRejected', label: '艺术家认证未通过' })
      if (this.isPromoter) tags.push({ value: 'promoter', label: '经纪人' })
      if (this.isAgent) tags.push({ value: 'agent', label: '经纪人' })
      return tags
    },
    profileIdentityOptions() {
      return this.identityTags
    },
    personalHomePath() {
      if (this.isArtist) return `/pages/artist/home?userId=${this.userInfo.id || ''}`
      if (this.isPromoter) return '/pages/promoter/index'
      if (this.isAgent) return this.comingSoon('个人主页', '经纪人个人主页正在规划中。')
      return '/pages/user/profile'
    },
    profileStats() {
      return [
        { label: '关注', value: this.formatCount(this.userStats.following), path: '/pages/user/following' },
        { label: '喜欢', value: this.formatCount(this.userStats.favorites), path: '/pages/user/favorites' },
        { label: '浏览', value: this.formatCount(this.userStats.history), path: '/pages/user/history' },
        { label: '积分', value: this.formatCount(this.userStats.points), path: '/pages/user/points' }
      ]
    },
    orderItems() {
      return [
        { type: 'purchased', label: '我买到的', icon: '买', iconPath: '/static/art-icons/icon-certificate.svg', path: '/pages/user/purchased' },
        { type: 'sold', label: '我卖出的', icon: '卖', iconPath: '/static/art-icons/icon-document.svg', path: '/pages/order/list?type=sold' },
        { type: 'refund', label: '退货售后', icon: '退', iconPath: '/static/art-icons/icon-circulation.svg', path: '/pages/order/list?type=refund' },
        { type: 'review', label: '我的评价', icon: '评', iconPath: '/static/art-icons/icon-comment.svg', path: '/pages/user/purchased?type=completed' }
      ]
    },
    assetItems() {
      const items = [
        { label: '我的钱包', desc: `余额 ¥${this.assetStats.wallet}`, icon: '钱', iconPath: '/static/art-icons/icon-budget.svg', tone: 'gold', path: '/pages/user-extra/wallet' },
        { label: '优惠券', desc: `${this.assetStats.coupon} 张可用`, icon: '券', iconPath: '/static/art-icons/icon-download.svg', tone: 'purple', path: '/pages/user/coupon' }
      ]
      if (this.isArtistPending) {
        items.unshift({
          label: '认证进度',
          desc: '艺术家资料审核中',
          icon: '审',
          iconPath: '/static/art-icons/icon-verify.svg',
          tone: 'purple',
          path: '/pages/artist/cert'
        })
      }
      return items
    },
    availableWorkspaces() {
      const list = [{ value: 'collector', label: '收藏家' }]
      if (this.isArtist) list.push({ value: 'artist', label: '艺术家' })
      if (this.isPromoter) list.push({ value: 'promoter', label: '经纪人' })
      return list
    },
    currentWorkspace() {
      const configs = {
        collector: {
          title: '我的工作台',
          desc: '管理收藏、订单和关注的艺术家',
          status: '',
          metrics: [
            { label: '喜欢', value: this.formatCount(this.userStats.favorites) },
            { label: '已购', value: this.formatCount(this.assetStats.purchased) },
            { label: '关注', value: this.formatCount(this.userStats.following) }
          ],
          actions: [
            { label: '转售市场', desc: '二级流通交易', icon: '售', tone: 'orange', path: '/pages/resale/market' },
            { label: '发布转售', desc: '转售已购作品', icon: '发', tone: 'gold', path: '/pages/resale/publish' },
            { label: '我的转售', desc: '管理转售记录', icon: '管', tone: 'green', path: '/pages/resale/my' },
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
            { label: '喜欢', value: this.formatCount(this.artistStats.favorites) }
          ],
          actions: [
            { label: '作品管理', desc: '上下架与编辑', icon: '管', iconPath: '/static/art-icons/icon-work.svg', tone: 'blue', path: '/pages/artist/manage', badgeCount: this.certificateUnreadCount },
            { label: '发布作品', desc: '提交新作品', icon: '发', iconPath: '/static/art-icons/icon-gallery.svg', tone: 'green', path: '/pages/artist/publish' },
            { label: '认证信息', desc: '查看认证状态', icon: '认', iconPath: '/static/art-icons/icon-verify.svg', tone: 'purple', path: '/pages/artist/cert' }
          ]
        },
        promoter: {
          title: '经纪人工作台',
          desc: '查看分成、团队和邀请转化',
          status: '已开通',
          metrics: [
            { label: '可提现', value: `¥${this.promoterStats.withdrawable}` },
            { label: '团队', value: this.formatCount(this.promoterStats.teamCount) },
            { label: '推广单', value: this.formatCount(this.promoterStats.orderCount) }
          ],
          actions: [
            { label: '推广中心', desc: '分成总览', icon: '推', iconPath: '/static/art-icons/icon-share.svg', tone: 'gold', path: '/pages/promoter/index' },
            { label: '分成明细', desc: '订单分成流水', icon: '佣', iconPath: '/static/art-icons/icon-budget.svg', tone: 'green', path: '/pages/promoter/earnings' },
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
          title: '开通经纪人',
          desc: '分享作品获得分成，管理团队与邀请关系',
          status: '未开通',
          metrics: [],
          actions: [
            { label: '了解经纪人', desc: '查看规则与权益', icon: '规', iconPath: '/static/art-icons/icon-share.svg', tone: 'gold', path: '/pages/promoter/index' },
            { label: '分销规则', desc: '分成与提现说明', icon: '则', iconPath: '/static/art-icons/icon-document.svg', tone: 'blue', path: '/pages/distribution/rules' }
          ]
        }
      }
      return configs[this.activeWorkspace] || configs.collector
    }
  },

  onShow() {
    this.initPage()
  },

  methods: {
    hasAccountLogin() {
      const check = checkTokenValid()
      return this.userStore.isLogin && check.valid && !check.isGuest
    },
    redirectToLoginIfNeeded() {
      if (this.hasAccountLogin()) return false
      saveRedirectUrl('/pages/user/index')
      uni.navigateTo({ url: '/pages/login/index' })
      return true
    },
    async initPage() {
      // 1. 先设置状态栏高度（同步，不阻塞）
      const systemInfo = uni.getSystemInfoSync()
      this.statusBarHeight = systemInfo.statusBarHeight || 20
      this.navRightSafeInset = this.getTopbarRightInset(systemInfo)

      if (this.redirectToLoginIfNeeded()) return
      
      // 2. 初始化基础状态（同步）
      this.syncDefaultWorkspace()
      this.loadLocalStats()
      this.refreshUnreadIndicators()
      
      // 3. 后台获取用户信息（不阻塞 UI）
      // 如果 401，会触发登录流程，不影响页面渲染
      this.userStore.initUserInfo().then(async (info) => {
        if (info) {
          this.syncArtistCertApprovedDismissed()
          await this.userStore.fetchCenterData()
          await this.loadArtistCertStatus()
          this.loadLocalStats()
          this.syncDefaultWorkspace()
          this.loadOrderCounts()
          this.refreshPurchasedCount()
          this.refreshUnreadIndicators()
        }
      }).catch(() => {
        // 获取失败不处理，让页面保持游客状态
      })
    },
    getTopbarRightInset(systemInfo = {}) {
      try {
        if (typeof uni.getMenuButtonBoundingClientRect !== 'function') return 0
        const menuButton = uni.getMenuButtonBoundingClientRect()
        const windowWidth = Number(systemInfo.windowWidth || uni.getSystemInfoSync().windowWidth || 375)
        const menuButtonLeft = Number(menuButton?.left || 0)
        if (!menuButtonLeft || !windowWidth) return 0
        return Math.max(windowWidth - menuButtonLeft + 12, 88)
      } catch (e) {
        return 0
      }
    },
    syncDefaultWorkspace() {
      const hasActive = this.availableWorkspaces.some(item => item.value === this.activeWorkspace)
      if (!this.workspaceManuallySelected) {
        if (this.isArtist) this.activeWorkspace = 'artist'
        else if (this.isPromoter) this.activeWorkspace = 'promoter'
        else this.activeWorkspace = 'collector'
        return
      }
      if (!hasActive) this.activeWorkspace = 'collector'
    },
    switchWorkspace(value) {
      if (!value || !this.availableWorkspaces.some(item => item.value === value)) return
      this.workspaceManuallySelected = true
      this.activeWorkspace = value
    },
    handleIdentityTagClick(item) {
      if (!item?.value) return
      if (item.value === 'artist') {
        this.goPage('/pages/artist/identity')
        return
      }
      if (item.value === 'artistPending' || item.value === 'artistRejected') {
        this.goArtistCertification()
        return
      }
      this.switchWorkspace(item.value)
    },
    isIdentityTagClickable(item) {
      return ['artist', 'artistPending', 'artistRejected'].includes(item?.value)
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
    refreshUnreadIndicators() {
      this.certificateUnreadCount = this.isLoggedIn ? getUnreadCertificateSignNoticeCount() : 0
    },
    getArtistCertApprovedDismissedKey() {
      const userId = this.userInfo.id || this.userInfo.userId || this.userStore.userInfo?.id || ''
      return userId ? `artist_cert_approved_dismissed:${userId}` : ''
    },
    syncArtistCertApprovedDismissed() {
      const storageKey = this.getArtistCertApprovedDismissedKey()
      this.artistCertApprovedDismissed = storageKey ? !!uni.getStorageSync(storageKey) : false
    },
    dismissArtistCertApprovedEntry() {
      const storageKey = this.getArtistCertApprovedDismissedKey()
      this.artistCertApprovedDismissed = true
      if (storageKey) {
        uni.setStorageSync(storageKey, true)
      }
    },
    async loadOrderCounts() {
      try {
        const res = await getOrderCounts()
        this.orderCounts = { ...this.orderCounts, ...(res || {}) }
      } catch (e) {
        console.log('获取订单数量失败', e)
      }
    },
    async refreshPurchasedCount() {
      try {
        const data = await getOrderList({ page: 1, pageSize: 200 })
        const rawList = data?.records || data?.list || data || []
        const list = Array.isArray(rawList) ? rawList : []
        const paidStatuses = new Set(['PAID', 'WAIT_DELIVER', 'WAIT_SHIP', 'SHIPPED', 'DELIVERED', 'RECEIVED', 'COMPLETED', 'FINISHED'])
        const purchasedKeys = new Set()
        let fallbackCount = 0

        list.forEach(order => {
          const status = String(order.status || order.orderStatus || '').toUpperCase()
          const paymentStatus = String(order.paymentStatus || order.payStatus || '').toUpperCase()
          const isPaidOrder = paidStatuses.has(status) || paymentStatus === 'PAID'
          if (!isPaidOrder) return

          const items = order.items || order.goodsList || order.goods || order.orderItems || []
          items.forEach(item => {
            const artworkKey = item.artworkId || item.artwork_id || item.productId || item.goodsId || item.id
            if (artworkKey) {
              purchasedKeys.add(String(artworkKey))
            } else {
              fallbackCount += Number(item.quantity || item.count || item.num || 1) || 1
            }
          })
        })

        this.assetStats.purchased = purchasedKeys.size + fallbackCount
      } catch (e) {
        console.log('刷新已购作品数量失败', e)
      }
    },
    async loadArtistCertStatus() {
      try {
        this.artistCertStatus = await getArtistCertStatus()
        if (this.normalizedArtistCertStatus !== 'approved') {
          const storageKey = this.getArtistCertApprovedDismissedKey()
          this.artistCertApprovedDismissed = false
          if (storageKey) {
            uni.removeStorageSync(storageKey)
          }
        } else {
          this.syncArtistCertApprovedDismissed()
        }
      } catch (e) {
        this.artistCertStatus = null
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
    goPersonalHome() {
      this.goPage(this.personalHomePath)
    },
    goMessage() {
      this.goPage('/pages/message/list')
    },
    goSettings() {
      this.goPage('/pages/user/settings')
    },
    goArtistCertification() {
      if (this.normalizedArtistCertStatus === 'approved') {
        this.dismissArtistCertApprovedEntry()
      }
      this.goPage('/pages/artist/cert')
    },
    goOrderList(type = 'all') {
      this.goPage(`/pages/order/list?type=${type}`)
    },
    handleTransactionEntry(item) {
      if (!item?.path) return
      this.goPage(item.path, item.tab)
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
    handleWorkspaceAction(item) {
      if (!item) return
      this.goPage(item.path, item.tab)
    },
    goPage(path, isTab = false) {
      if (!path) return
      if (!AUCTION_ENABLED && isAuctionPath(path)) {
        showAuctionDisabledToast()
        return
      }
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
      const tabPaths = [
        '/pages/index/index',
        '/pages/gallery/index',
        '/pages/cart/index',
        '/pages/user/index'
      ]
      if (AUCTION_ENABLED) {
        tabPaths.push('/pages/auction/index')
      }
      return tabPaths.includes(purePath)
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

.action-badge {
  position: absolute;
  top: -6rpx;
  right: -6rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  box-sizing: border-box;
  border-radius: 999rpx;
  background: #2f7cff;
  border: 2rpx solid $bg;
  color: #fff;
  font-size: 18rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
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

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
  width: 112rpx;
  height: 112rpx;
}

.avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  background: $panel-2;
  border: 2rpx solid rgba($gold, 0.35);
  display: block;
}

.avatar-gold-v {
  position: absolute;
  right: -2rpx;
  bottom: 2rpx;
  width: 34rpx;
  height: 34rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 2rpx solid rgba(23, 23, 25, 0.96);
  background: linear-gradient(135deg, #ffdf6c 0%, $gold 56%, #8f6a12 100%);
  color: #171719;
  font-size: 22rpx;
  line-height: 1;
  font-weight: 900;
  box-shadow: 0 4rpx 12rpx rgba($gold, 0.28);
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

  &.clickable {
    cursor: pointer;
  }

  &.clickable:active {
    transform: scale(0.97);
    opacity: 0.86;
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

  &.artistPending {
    color: #d8b75d;
    background: rgba(#d8b75d, 0.14);
  }

  &.artistRejected {
    color: $red;
    background: rgba($red, 0.14);
  }
}

.identity-active-hint {
  margin-top: 10rpx;
  font-size: 21rpx;
  color: $dim;
}

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  flex-shrink: 0;
}

.edit-link,
.home-link,
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

.home-link {
  color: $gold;
  background: rgba($gold, 0.12);
  border: 1rpx solid rgba($gold, 0.42);
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

.artist-cert-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  background:
    linear-gradient(135deg, rgba(201, 162, 39, 0.16) 0%, rgba(88, 185, 130, 0.08) 100%),
    rgba(23, 23, 25, 0.96);
  overflow: hidden;
}

.artist-cert-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.artist-cert-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.artist-cert-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $text;
}

.artist-cert-badge {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  line-height: 1;
  font-weight: 700;
  border: 1rpx solid transparent;

  &.approved {
    color: #101512;
    background: rgba($green, 0.96);
    border-color: rgba($green, 0.4);
  }

  &.pending {
    color: #1d1707;
    background: rgba(#e2ba58, 0.96);
    border-color: rgba(#e2ba58, 0.42);
  }

  &.rejected {
    color: #fff2f2;
    background: rgba($red, 0.2);
    border-color: rgba($red, 0.36);
  }
}

.artist-cert-desc {
  font-size: 24rpx;
  line-height: 1.6;
  color: $text;
}

.artist-cert-tip {
  font-size: 21rpx;
  line-height: 1.5;
  color: $muted;
}

.artist-cert-cta {
  flex-shrink: 0;
  min-width: 144rpx;
  padding: 18rpx 22rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #16130b;
  background: $gold;

  &.approved {
    color: #101512;
    background: $green;
  }

  &.pending {
    color: #1d1707;
    background: #e2ba58;
  }

  &.rejected {
    color: #fff2f2;
    background: rgba($red, 0.9);
  }
}

.artist-cert-arrow {
  font-size: 24rpx;
  line-height: 1;
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

.payment-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.payment-entry {
  min-height: 156rpx;
  padding: 20rpx 14rpx;
  border-radius: 14rpx;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.025)),
    $panel-2;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  text-align: center;
  box-sizing: border-box;
}

.payment-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 900;

  &.alipay {
    color: #63a7ff;
    background: rgba(22, 119, 255, 0.16);
  }

  &.wechat {
    color: #44d486;
    background: rgba(7, 193, 96, 0.16);
  }

  &.bank {
    color: $gold;
    background: rgba($gold, 0.16);
  }
}

.payment-copy {
  min-width: 0;
}

.payment-title {
  display: block;
  color: $text;
  font-size: 24rpx;
  line-height: 30rpx;
  font-weight: 700;
}

.payment-desc {
  display: block;
  margin-top: 6rpx;
  color: $dim;
  font-size: 19rpx;
  line-height: 27rpx;
}

.identity-entry-scroll {
  width: 100%;
  white-space: nowrap;
}

.identity-entry {
  width: 520rpx;
  min-height: 132rpx;
  margin-right: 16rpx;
  padding: 20rpx;
  border-radius: 12rpx;
  background: $panel-2;
  display: inline-flex;
  align-items: center;
  gap: 16rpx;
  box-sizing: border-box;
  vertical-align: top;

  &:last-child {
    margin-right: 0;
  }
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

.workspace-action-badge {
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 10rpx;
  box-sizing: border-box;
  border-radius: 999rpx;
  background: #2f7cff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 700;
  line-height: 1;
}

.workspace-action-badge.inline {
  position: relative;
  top: -8rpx;
  right: -2rpx;
  flex-shrink: 0;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  font-size: 18rpx;
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

.workspace-assets {
  margin-top: 18rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $line;
}

.workspace-subtitle {
  display: block;
  margin-bottom: 16rpx;
  color: $text;
  font-size: 26rpx;
  font-weight: 600;
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
