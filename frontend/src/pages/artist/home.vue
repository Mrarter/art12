<template>
  <view class="artist-home-page" :class="{ 'style-2': isStyle2 }">
    <view class="top-nav">
      <view class="nav-icon" @click="goBack">
        <image src="/static/art-icons/icon-back.svg" mode="aspectFit"></image>
      </view>
      <view class="nav-title"></view>
      <view class="nav-icon external" @click="shareArtist">
        <image src="/static/art-icons/icon-share.svg" mode="aspectFit"></image>
      </view>
    </view>

    <view class="profile-hero">
      <image v-if="heroCover" class="cover-image" :src="heroCover" mode="aspectFill"></image>
      <view class="cover-shade"></view>

      <view class="profile-core">
        <view class="avatar-wrap">
          <image class="avatar" :src="artist.avatar" mode="aspectFill"></image>
        <view v-if="artist.certified" class="avatar-cert">✓</view>
        </view>
        <view class="identity-block">
          <view class="artist-name-row">
            <view class="artist-name">{{ artist.name }}</view>
            <button
              v-if="showFollowMenu"
              class="follow-more"
              :disabled="followLoading"
              aria-label="关注设置"
              @click.stop="openFollowMenu"
            >
              <view class="follow-more-chevron"></view>
            </button>
            <view
              v-if="showFollowMenu && followMenuVisible"
              class="follow-menu-popover"
              @click.stop="confirmUnfollow"
            >
              取消关注
            </view>
          </view>
          <view v-if="artistSubtitle" class="artist-title">{{ artistSubtitle }}</view>
          <view v-if="isStyle2 && artist.uid" class="style2-artist-uid">UID {{ artist.uid }}</view>
          <view v-if="displayTags.length" class="tag-row">
            <view class="tag" v-for="tag in displayTags" :key="tag">
              <image class="tag-icon-img" src="/static/art-icons/icon-verify.svg" mode="aspectFit"></image>
              <text>{{ tag }}</text>
            </view>
          </view>
          <view v-if="isStyle2" class="style2-hero-stats">
            <view class="style2-hero-pill" v-for="item in stats" :key="item.label">
              <text>{{ item.label }}</text>
              <strong>{{ item.value }}</strong>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!isStyle2 || showHeroActions" class="stats-actions">
      <view v-if="!isStyle2" class="stats">
        <view class="stat-item" v-for="item in stats" :key="item.label">
          <text class="stat-value">{{ item.value }}</text>
          <text class="stat-label">{{ item.label }}</text>
        </view>
      </view>
      <view v-if="showHeroActions" class="hero-actions">
        <button v-if="showFollowButton" class="gold-btn" :disabled="followLoading" @click="followArtist">
          <image v-if="isStyle2" class="follow-btn-icon" src="/static/art-icons/icon-follow.svg" mode="aspectFit"></image>
          <text>{{ followed ? '已关注' : '关注' }}</text>
        </button>
        <button v-if="!isStyle2" class="outline-btn" @click="consult">咨询顾问</button>
      </view>
    </view>

    <template v-if="isStyle2">
      <view class="style2-tabs">
        <view
          v-for="tab in style2Tabs"
          :key="tab.key"
          class="style2-tab"
          :class="{ active: activeStyle2Tab === tab.key }"
          @click="activeStyle2Tab = tab.key"
        >
          {{ tab.label }}
        </view>
      </view>

      <view v-if="activeStyle2Tab === 'gallery'" class="style2-panel style2-gallery">
        <view v-if="featuredWork" class="style2-featured" @click="goWork(featuredWork.id)">
          <image class="style2-featured-image" :src="featuredWork.cover" mode="aspectFill"></image>
          <view class="style2-featured-mask"></view>
          <view class="style2-featured-label">代表作品</view>
          <view class="style2-featured-like">
            <image src="/static/art-icons/icon-heart-outline-white.svg" mode="aspectFit"></image>
          </view>
          <view class="style2-featured-copy">
            <text class="style2-featured-title">{{ featuredWork.title }}</text>
            <text class="style2-featured-meta">{{ featuredWork.year }}</text>
            <text class="style2-featured-meta">{{ featuredWork.material }}</text>
            <text class="style2-featured-meta">{{ featuredWork.size }}</text>
          </view>
          <view class="style2-featured-price-block">
            <view
              class="style2-featured-price"
              :class="{ 'price-error': featuredWork.priceError, collected: featuredWork.collected }"
            >
              {{ featuredWork.collected ? '已收藏' : featuredWork.priceDisplay }}
            </view>
            <view v-if="!featuredWork.collected && featuredWork.forecast30Display" class="style2-featured-growth">预估短期上涨 {{ featuredWork.forecast30Display }}</view>
          </view>
        </view>

        <view v-if="galleryWorks.length" class="style2-grid">
          <view class="style2-work-card" v-for="work in galleryWorks" :key="work.id" @click="goWork(work.id)">
            <view class="style2-work-like">
              <image src="/static/art-icons/icon-heart-outline-white.svg" mode="aspectFit"></image>
            </view>
            <image class="style2-work-image" :src="work.cover" mode="aspectFill"></image>
            <view class="style2-work-info">
              <view class="style2-work-title">{{ work.title }}</view>
              <view class="style2-work-meta">{{ work.material }} / {{ work.size }} / {{ work.year }}</view>
              <template v-if="work.collected">
                <view class="style2-work-collected">已收藏</view>
                <view class="style2-work-growth sold">成交涨幅 {{ work.dealGrowthDisplay }}</view>
              </template>
              <template v-else>
                <view class="style2-work-price">{{ work.priceDisplay }}</view>
                <view class="style2-work-growth">历史涨幅 {{ work.historyGrowth }}</view>
                <view v-if="work.forecast30Display" class="style2-work-forecast">预计涨幅（30天）{{ work.forecast30Display }}</view>
              </template>
            </view>
          </view>
        </view>
        <view v-else class="empty-block">该艺术家暂未上架作品</view>
      </view>

      <view v-else-if="activeStyle2Tab === 'resume'" class="style2-panel">
        <view
          class="style2-resume-card"
          :class="{ 'has-seal': section.emblem || section.seal, 'has-media': section.images?.length }"
          v-for="section in resumeSections"
          :key="section.title"
        >
          <view class="style2-resume-head">
            <view class="style2-resume-icon">
              <image :src="section.icon" mode="aspectFit"></image>
            </view>
            <view class="style2-resume-title">{{ section.title }}</view>
            <button v-if="isOwnArtistPage" class="style2-edit-entry" @click.stop="editResumeSection(section.key)">编辑</button>
          </view>
          <view class="style2-resume-body">
            <view class="style2-resume-timeline">
              <view class="style2-resume-line"></view>
              <view class="style2-resume-entry" v-for="entry in section.entries" :key="entry.year + entry.primary">
                <view class="style2-resume-year">{{ entry.year }}</view>
                <view class="style2-resume-dot"></view>
                <view class="style2-resume-text">
                  <view class="style2-resume-primary">{{ entry.primary }}</view>
                  <view v-if="entry.secondary" class="style2-resume-secondary">{{ entry.secondary }}</view>
                </view>
              </view>
            </view>
            <view v-if="section.images?.length" class="style2-resume-media" :class="{ single: section.images.length === 1 }">
              <image v-for="img in section.images" :key="img" :src="img" mode="aspectFill"></image>
            </view>
            <view
              v-if="section.emblem || section.seal"
              class="style2-resume-seal"
              :class="[section.emblem?.tierClass, { shine: section.emblem?.shine }]"
            >
              <image :src="section.emblem?.src || section.seal" mode="aspectFit"></image>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="style2-panel">
        <view class="style2-circulation-stats">
          <view class="style2-stat" v-for="item in circulationStats" :key="item.label">
            <view class="style2-stat-icon" :style="{ '--stat-icon': `url(${item.icon})` }"></view>
            <view class="style2-stat-label">{{ item.label }}</view>
            <view class="style2-stat-value">
              <text>{{ item.value }}</text>
              <small>{{ item.unit }}</small>
            </view>
          </view>
          <view class="style2-stat-note">数据截至 {{ circulationDate }} | 所有作品均经过平台审核与确权</view>
        </view>
        <view class="style2-record-head">
          <view class="style2-record-title-wrap">
            <text>流通记录</text>
            <image src="/static/art-icons/icon-notice.svg" mode="aspectFit"></image>
          </view>
          <view class="style2-record-sort">按时间最新 <text>⌄</text></view>
        </view>
        <view class="style2-record-timeline">
          <view
            class="style2-record-row"
            v-for="item in circulationRecords"
            :key="item.id"
            @click="goWork(item.id)"
          >
            <view class="style2-record-date">{{ item.date }}</view>
            <view class="style2-record-node"></view>
            <view class="style2-record-card">
              <image class="style2-record-cover" :src="item.cover" mode="aspectFill"></image>
              <view class="style2-record-main">
                <view class="style2-record-name">{{ item.title }}</view>
                <view class="style2-record-cert">证书编号：{{ item.certNo }}</view>
                <view class="style2-record-line">
                  <image src="/static/art-icons/icon-collector.svg" mode="aspectFit"></image>
                  <text>{{ item.collector }}</text>
                </view>
                <view class="style2-record-line">
                  <image src="/static/art-icons/icon-platform-custody.svg" mode="aspectFit"></image>
                  <text>{{ item.custody }}</text>
                </view>
              </view>
              <view class="style2-record-side">
                <view class="style2-record-badge" :class="item.badgeType">{{ item.badge }}</view>
                <view class="style2-record-count">{{ item.action }}</view>
              </view>
            </view>
          </view>
        </view>
        <view class="style2-trust-title">收藏保障</view>
        <view class="trust-section style2-trust">
          <view class="trust-row">
            <view class="trust-item" v-for="item in style2TrustCards" :key="item.title">
              <image class="trust-icon" :src="item.icon" mode="aspectFit"></image>
              <view>
                <view class="trust-title">{{ item.title }}</view>
                <view class="trust-desc">{{ item.desc }}</view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="style2-bottom-cta">
        <button class="style2-reserve-btn" @click="goWorks">
          <image src="/static/art-icons/icon-calendar.svg" mode="aspectFit"></image>
          <text>预约看展</text>
        </button>
        <button class="style2-consult-btn" @click="consult">
          <image src="/static/art-icons/icon-comment.svg" mode="aspectFit"></image>
          <text>收藏咨询</text>
        </button>
      </view>
    </template>

    <template v-else>
      <view class="intro-card">
        <view class="section-title gold-title">艺术家介绍</view>
        <view class="intro-text" :class="{ expanded: introExpanded }">
          {{ artist.intro }}
        </view>
        <view v-if="artist.quote" class="quote">“ {{ artist.quote }} ”</view>
        <view v-if="artist.intro && artist.intro.length > 42" class="expand" @click="introExpanded = !introExpanded">
          {{ introExpanded ? '收起' : '展开' }} <text>{{ introExpanded ? '⌃' : '⌄' }}</text>
        </view>
      </view>

      <view class="section works-section">
        <view class="section-head" v-if="works.length">
          <view class="section-title">代表作品</view>
          <view class="more-link" @click="goWorks">查看全部 ›</view>
        </view>
        <view v-if="works.length" class="work-scroll">
          <view class="work-row">
            <view class="work-card" v-for="work in works" :key="work.id" @click="goWork(work.id)">
              <view class="work-image-wrap">
                <image class="work-image" :src="work.cover" mode="aspectFill"></image>
                <view class="new-badge">新</view>
              </view>
              <view class="work-info">
                <view class="work-title">{{ work.title }}</view>
                <view class="work-meta">{{ work.material }} / {{ work.size }} / {{ work.year }}</view>
                <view class="work-bottom">
                  <text
                    class="price"
                    :class="{ 'price-error': work.priceError, 'collector-label': work.collected }"
                  >
                    {{ work.collected ? work.collectorLabel : work.priceDisplay }}
                  </text>
                  <view class="work-bottom-right">
                    <text v-if="work.collected" class="collect-tag">已收藏</text>
                    <text v-else-if="!work.priceError" class="price-updated">{{ priceUpdatedAt }}</text>
                    <text v-else class="collect-tag">价格待更新</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view v-else class="empty-block">该艺术家暂未上架作品</view>
      </view>

      <view v-if="flowWorks.length" class="section circulation-section">
        <view class="section-title">作品流通入口</view>
        <view class="flow-list">
          <view class="flow-item" v-for="work in flowWorks" :key="work.id" @click="goWork(work.id)">
            <image class="flow-cover" :src="work.cover" mode="aspectFill"></image>
            <view class="flow-main">
              <view class="flow-title">{{ work.title }}</view>
              <view class="flow-price" :class="{ 'price-error': work.priceError }">{{ work.priceDisplay }}</view>
            </view>
            <view class="flow-feature">
              <image src="/static/art-icons/icon-certificate.svg" mode="aspectFit"></image>
              <text>收藏证书</text>
            </view>
            <view class="flow-feature">
              <image src="/static/art-icons/icon-circulation.svg" mode="aspectFit"></image>
              <text>流通记录</text>
            </view>
            <view class="flow-feature">
              <image src="/static/art-icons/icon-verify.svg" mode="aspectFit"></image>
              <text>平台保障</text>
            </view>
            <view class="flow-arrow">›</view>
          </view>
        </view>
      </view>

      <view class="trust-section">
        <view class="section-title">收藏信任</view>
        <view class="trust-row">
          <view class="trust-item" v-for="item in trustItems" :key="item.title">
            <image class="trust-icon" :src="item.icon" mode="aspectFit"></image>
            <view>
              <view class="trust-title">{{ item.title }}</view>
              <view class="trust-desc">{{ item.desc }}</view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-actions">
        <button class="all-works-btn" @click="goWorks">查看全部作品</button>
        <button class="analytics-btn" @click="goAnalytics">
          <view class="analytics-btn__icon">
            <image src="/static/art-icons/icon-chart.svg" mode="aspectFit"></image>
          </view>
          <text>数据看板</text>
        </button>
        <button class="consult-btn" @click="consult">
          <view class="consult-btn__icon">
            <image src="/static/art-icons/icon-consultant.svg" mode="aspectFit"></image>
          </view>
          <text>发起收藏咨询</text>
        </button>
      </view>
    </template>

    <view class="home-indicator"></view>
  </view>
</template>

<script>
import * as userApi from '@/api/user'
import { getProductList } from '@/api/product'
import { resolveSchoolEmblemFromEntries } from '@/utils/schoolEmblems'
import { getCurrentUserIdentity } from '@/utils/auth'
import { buildDefaultResumeEntries, parseArtistResume } from '@/utils/artistResume'

export default {
  data() {
    return {
      followed: false,
      followLoading: false,
      followMenuVisible: false,
      introExpanded: false,
      loading: true,
      requestedStyle: '',
      requestedArtistId: '',
      activeStyle2Tab: 'gallery',
      defaultStyle2Tabs: [
        { key: 'gallery', label: '个人美术馆' },
        { key: 'resume', label: '艺术履历' },
        { key: 'circulation', label: '流通记录' }
      ],
      artist: {
        id: 0,
        name: '',
        title: '',
        uid: '',
        region: '',
        avatar: '',
        cover: '',
        intro: '',
        resume: '',
        quote: '',
        tags: [],
        certified: false,
        layoutStyle: '',
        homepageConfig: {},
        isOwner: false
      },
      works: [],
      trustItems: [
        { icon: '/static/art-icons/icon-verify.svg', title: '平台认证', desc: '严格审核' },
        { icon: '/static/art-icons/icon-certificate.svg', title: '收藏证书', desc: '权威出具' },
        { icon: '/static/art-icons/icon-circulation.svg', title: '流通记录', desc: '全程可查' },
        { icon: '/static/art-icons/icon-platform-custody.svg', title: '保管支持', desc: '专业保管' }
      ],
      stats: [
        { label: '作品', value: '0' },
        { label: '喜欢', value: '0' },
        { label: '粉丝', value: '0' }
      ],
      priceUpdatedAt: '',
      priceLoading: false,
      priceError: false,
      pageReady: false
    }
  },

  async onLoad(options = {}) {
    const routeOptions = this.readRouteOptions(options)
    const artistId = routeOptions.id || routeOptions.userId || routeOptions.artistId
    this.requestedStyle = String(routeOptions.style || routeOptions.layoutStyle || routeOptions.homepageStyle || '')
    if (artistId) {
      this.requestedArtistId = artistId
      this.artist.id = artistId
      await this.loadArtistData(artistId)
    }
    this.loading = false
    this.pageReady = true
    // H5 右上角分享走浏览器能力，小程序走原生分享菜单
    // #ifdef MP-WEIXIN
    uni.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
    // #endif
  },

  async onShow() {
    const artistId = this.requestedArtistId || this.artist.id
    if (this.pageReady && artistId) {
      await this.loadArtistData(artistId)
    }
  },

  computed: {
    style2Tabs() {
      const configuredTabs = this.normalizeStyle2Tabs(this.artist.homepageConfig)
      return configuredTabs.length ? configuredTabs : this.defaultStyle2Tabs
    },
    isStyle2() {
      if (this.requestedStyle) {
        return String(this.requestedStyle) === '2'
      }
      return true
    },
    currentUserIdentity() {
      return getCurrentUserIdentity()
    },
    isOwnArtistPage() {
      const current = this.currentUserIdentity
      const artistId = this.normalizeIdentityValue(this.requestedArtistId || this.artist.id)
      const currentId = this.normalizeIdentityValue(current.id)
      const artistUid = this.normalizeIdentityValue(this.artist.uid)
      const currentUid = this.normalizeIdentityValue(current.uid)
      return !!(this.artist.isOwner || (artistId && currentId && artistId === currentId) || (artistUid && currentUid && artistUid === currentUid))
    },
    showFollowButton() {
      return !this.isOwnArtistPage && (!this.isStyle2 || !this.followed)
    },
    showFollowMenu() {
      return this.isStyle2 && !this.isOwnArtistPage && this.followed
    },
    showHeroActions() {
      return !this.isOwnArtistPage && (!this.isStyle2 || !this.followed)
    },
    flowWorks() {
      return this.works.slice(0, 4)
    },
    featuredWork() {
      return this.works[0] || null
    },
    heroCover() {
      if (this.isStyle2 && this.featuredWork?.cover) return this.featuredWork.cover
      return this.artist.cover
    },
    artistSubtitle() {
      if (!this.isStyle2) return this.artist.title
      const identity = this.artist.identityLabel || this.artist.title || '认证艺术家'
      const region = this.artist.region || this.artist.location || ''
      return [identity, region].filter(Boolean).join(' / ')
    },
    galleryWorks() {
      return this.works.slice(1)
    },
    resumeSections() {
      const name = this.artist.name || '艺术家'
      const defaults = buildDefaultResumeEntries(name)
      const saved = parseArtistResume(this.artist.resume)
      const entries = key => saved?.sections?.[key] || defaults[key]
      const educationEntries = entries('education')
      return [
        {
          key: 'education',
          title: '教育经历',
          icon: '/static/art-icons/icon-certificate.svg',
          entries: educationEntries,
          emblem: resolveSchoolEmblemFromEntries(educationEntries)
        },
        {
          key: 'exhibitions',
          title: '个展 / 联展',
          icon: '/static/art-icons/icon-gallery.svg',
          entries: entries('exhibitions'),
          images: ['/static/images/museum-v12-hero-bg.png', this.heroCover || '/pages/artist/static/images/museum-v12-work-boat.png']
        },
        {
          key: 'awards',
          title: '获奖经历',
          icon: '/static/art-icons/icon-star.svg',
          entries: entries('awards'),
          seal: '/pages/artist/static/images/profile-v12-work-girl.png'
        },
        {
          key: 'collections',
          title: '机构收藏',
          icon: '/static/art-icons/icon-trust.svg',
          entries: entries('collections'),
          seal: '/pages/artist/static/artist-ui/collection-trust.png'
        },
        {
          key: 'media',
          title: '媒体报道',
          icon: '/static/art-icons/icon-document.svg',
          entries: entries('media'),
          seal: '/pages/artist/static/images/museum-v12-work-still.png'
        }
      ]
    },
    circulationStats() {
      const collectedCount = this.works.filter(item => item.collected).length
      const workCount = Number(this.stats.find(item => item.label === '作品')?.value || this.works.length || 0)
      const collectedTotal = Math.max(workCount * 20 + 8, collectedCount)
      const generatedCount = Math.max(Math.round(workCount * 16), collectedCount)
      const recirculatedCount = Math.max(Math.round(workCount * 3), 0)
      const custodyCount = Math.max(workCount * 6 + 3, 0)
      return [
        { icon: '/static/art-icons/icon-like.svg', label: '累计收藏', value: String(collectedTotal), unit: '件' },
        { icon: '/static/art-icons/icon-certificate.svg', label: '已生成证书', value: String(generatedCount), unit: '份' },
        { icon: '/static/art-icons/icon-circulation.svg', label: '再次流通', value: String(recirculatedCount), unit: '件' },
        { icon: '/static/art-icons/icon-platform-custody.svg', label: '平台保管', value: String(custodyCount), unit: '件' }
      ]
    },
    circulationDate() {
      return '2024.06.06'
    },
    circulationRecords() {
      const dates = ['2024.06.01', '2024.03.18', '2023.11.27', '2023.07.09']
      const collectors = ['公开藏家  张先生（杭州）', '匿名藏家', '公开藏家  李女士（上海）', '匿名藏家']
      return this.flowWorks.map((work, index) => ({
        ...work,
        date: dates[index] || '2023.06.18',
        certNo: `CERT-${(dates[index] || '2023.06.18').replace(/\./g, '')}-${String(work.id || index + 1).padStart(4, '0')}`,
        collector: collectors[index] || '认证藏家',
        custody: index % 2 === 0 ? '平台保管' : '艺术家保管',
        badge: index % 2 === 0 ? '证书已生成' : '再次流通',
        badgeType: index % 2 === 0 ? 'green' : 'blue',
        action: index === 0 ? '首次收藏' : `流通第 ${index + 1} 次`
      }))
    },
    style2TrustCards() {
      return [
        { icon: '/static/art-icons/icon-verify.svg', title: '平台认证', desc: '作品与艺术家身份双重审核，确保真实可靠' },
        { icon: '/static/art-icons/icon-certificate.svg', title: '收藏证书', desc: '区块链确权存证，独立证书，永久有效' },
        { icon: '/static/art-icons/icon-circulation.svg', title: '流通可追溯', desc: '完整记录每一次流通，来源清晰，去向可查' }
      ]
    },
    sharePath() {
      return `/pages/artist/home?userId=${this.artist.id}`
    },
    shareTitle() {
      return `${this.artist.name || '艺术家'}的艺术主页`
    },
    displayTags() {
      const tags = Array.isArray(this.artist.tags) ? this.artist.tags.filter(Boolean) : []
      if (tags.length) return tags
      const fallback = []
      if (this.artist.certified) fallback.push('平台认证')
      if (this.artist.title) fallback.push(this.artist.title)
      const firstWork = this.works[0]
      if (firstWork?.material) fallback.push(firstWork.material)
      return fallback.slice(0, 4)
    }
  },

  methods: {
    readRouteOptions(options = {}) {
      if (typeof window === 'undefined') return options
      const hashQuery = window.location.hash.split('?')[1] || ''
      const searchQuery = window.location.search.replace(/^\?/, '')
      return {
        ...Object.fromEntries(new URLSearchParams(searchQuery)),
        ...Object.fromEntries(new URLSearchParams(hashQuery)),
        ...options
      }
    },
    normalizeIdentityValue(value) {
      return String(value ?? '').trim()
    },
    async loadArtistData(artistId) {
      try {
        const data = await userApi.getArtistInfo(artistId)
        this.applyArtistData(data, artistId)

        // 获取艺术家名称后，调用商品服务接口拉取实时价格
        const artistName = data?.nickname || data?.name || data?.realName
        if (artistId || artistName) {
          await this.fetchRealTimePrices(artistId, artistName)
        }
      } catch (e) {
        console.error('加载艺术家数据失败', e)
        this.applyArtistData({}, artistId)
      }
    },

    applyArtistData(data = {}, artistId) {
      const works = (data.works || data.artworks || []).map(w => {
        const resolvedPrice = this.resolveCurrentPrice(w)
        const priceText = resolvedPrice ? '¥' + this.formatPrice(resolvedPrice) : (w.priceText || '')
        return {
          id: w.id,
          title: w.title || w.name || '',
          material: w.material || w.artType || w.medium || '',
          size: w.size || '',
          year: w.year || w.createYear || '',
          priceText,
          historyGrowth: w.historyGrowth || w.growthRate || w.increaseRate || '+0.0%',
          cover: w.cover || w.coverImage || w.coverUrl || '/pages/artist/static/images/museum-v12-work-boat.png',
          priceDisplay: priceText,
          collected: !!(w.collected || w.isCollected || w.sold || Number(w.status) === 2),
          collectorRegion: w.collectorRegion || '',
          collectorLabel: w.collectorLabel || this.buildCollectorLabel(w.collectorRegion),
          originalPrice: this.resolveOriginalPrice(w),
          dealPrice: this.resolveDealPrice(w),
          priceRise: Number(w.priceRise || w.price_rise || w.dailyIncreaseRate || 0),
          tomorrowIncreaseMin: Number(w.tomorrowIncreaseMin || 0),
          tomorrowIncreaseMax: Number(w.tomorrowIncreaseMax || 0),
          forecast30Display: w.collected || w.isCollected || w.sold || Number(w.status) === 2 ? '' : this.buildForecast30Display({
            currentPrice: resolvedPrice,
            priceRise: w.priceRise || w.price_rise || w.dailyIncreaseRate,
            tomorrowIncreaseMin: w.tomorrowIncreaseMin,
            tomorrowIncreaseMax: w.tomorrowIncreaseMax
          }),
          dealGrowthDisplay: this.buildDealGrowthDisplay({
            ...w,
            originalPrice: this.resolveOriginalPrice(w),
            dealPrice: this.resolveDealPrice(w)
          }),
          priceError: false
        }
      })
      const artistTags = this.normalizeTags(data.artistTags || data.tags || data.badges)
      const resume = data.resume || ''
      const introCandidate = data.intro || data.bio || ''
      const intro = parseArtistResume(introCandidate) ? '暂未补充艺术家介绍' : (introCandidate || '暂未补充艺术家介绍')
      const cover = data.homepageCover || data.cover || data.coverUrl || works[0]?.cover || data.avatar || '/static/images/museum-v12-hero-bg.png'
      const homepageConfig = this.normalizeHomepageConfig(
        data.homepageConfig || data.profileConfig || data.layoutConfig || data.styleConfig || data.artistHomepageConfig
      )

      this.artist = {
        id: data.id || data.userId || artistId,
        uid: data.uid || data.userUid || data.artistUid || data.artistCode || '',
        name: data.nickname || data.name || data.realName || '艺术家',
        title: data.artistTitle || data.title || data.identityTypeLabel || '',
        identityLabel: data.identityLabel || data.identityTypeLabel || data.artistTitle || data.title || '',
        region: data.region || data.location || data.city || data.province || '',
        avatar: data.avatar || data.avatarUrl || '/static/images/artist-avatar.png',
        cover,
        intro,
        resume,
        quote: data.quote || '',
        tags: artistTags,
        certified: !!(data.certified || data.certStatus === 1 || data.isArtist || artistTags.includes('平台认证')),
        layoutStyle: data.homepageStyle || data.layoutStyle || data.profileStyle || data.templateStyle || '',
        homepageConfig,
        isOwner: !!data.isOwner
      }
      this.works = works
      this.followed = !!data.followed
      this.stats = [
        { label: '作品', value: String(data.workCount || data.artworkCount || works.length || 0) },
        { label: '喜欢', value: String(data.collectCount || data.favoriteCount || 0) },
        { label: '粉丝', value: String(data.fansCount || data.followerCount || 0) }
      ]
      if (this.isStyle2 && !this.style2Tabs.some(tab => tab.key === this.activeStyle2Tab)) {
        this.activeStyle2Tab = this.style2Tabs[0]?.key || 'gallery'
      }
    },

    /** 从商品服务接口拉取实时价格并覆盖 works 中的价格 */
    async fetchRealTimePrices(artistId, artistName) {
      this.priceLoading = true
      this.priceError = false
      try {
        const query = artistId ? { authorId: artistId, pageSize: 100 } : { authorName: artistName, pageSize: 100 }
        const res = await getProductList(query)
        const records = res?.records || []
        if (records.length === 0) {
          this.priceError = true
          return
        }

        // 构建 works 中已有的价格显示（防止实时价格请求失败时闪白）
        const initialPrices = {}
        this.works.forEach(w => { initialPrices[w.id] = w.priceDisplay })

        // 创建 id -> 实时作品信息映射
        const productMap = {}
        records.forEach(r => {
          const cp = this.resolveCurrentPrice(r)
          productMap[r.id] = {
            _pricesNormalized: true,
            price: cp,
            currentPrice: cp,
            publishPrice: this.resolvePublishPrice(r),
            originalPrice: this.resolveOriginalPrice(r),
            dealPrice: this.resolveDealPrice(r),
            priceRise: this.normalizeGrowthRate(r.priceRise || r.price_rise || r.dailyIncreaseRate || 0),
            tomorrowIncreaseMin: Number(r.tomorrowIncreaseMin || 0),
            tomorrowIncreaseMax: Number(r.tomorrowIncreaseMax || 0),
            cover: r.cover || r.coverImage || r.coverUrl || '',
            collected: !!(r.collected || r.isCollected || r.sold || Number(r.status) === 2 || r.statusText === '已售罄')
          }
        })

        // 用实时价格覆盖 works
        this.works = this.works.map(w => {
          const real = productMap[w.id]
          const cover = real?.cover || w.cover
          const metrics = real
            ? {
                priceRise: real.priceRise,
                tomorrowIncreaseMin: real.tomorrowIncreaseMin,
                tomorrowIncreaseMax: real.tomorrowIncreaseMax,
                forecast30Display: this.buildForecast30Display(real),
                originalPrice: real.originalPrice,
                dealPrice: real.dealPrice,
                dealGrowthDisplay: this.buildDealGrowthDisplay(real)
              }
            : {}
          const isCollected = !!(w.collected || real?.collected)
          if (isCollected) {
            return { ...w, ...metrics, cover, collected: true, forecast30Display: '', priceError: false }
          }
          if (real?.currentPrice > 0) {
            const formatted = '¥' + this.formatPrice(real.currentPrice)
            return { ...w, ...metrics, cover, priceDisplay: formatted, priceError: false }
          }
          // 作品在 artistInfo 中但不在 productList 中时通常是下架状态，仍按主页价格口径展示。
          return { ...w, cover, priceError: false }
        })

        // 记录价格更新时间
        const now = new Date()
        const h = String(now.getHours()).padStart(2, '0')
        const m = String(now.getMinutes()).padStart(2, '0')
        this.priceUpdatedAt = h + ':' + m + ' 更新'
      } catch (e) {
        console.warn('[artist/home] 获取实时价格失败:', e)
        this.priceError = true
        // 保持原有价格显示，但标记为过期
        this.works = this.works.map(w => ({ ...w, priceError: true }))
      } finally {
        this.priceLoading = false
      }
    },

    normalizeTags(rawValue) {
      if (Array.isArray(rawValue)) {
        return rawValue.map(item => String(item || '').trim()).filter(Boolean)
      }
      if (typeof rawValue === 'string' && rawValue.trim()) {
        return rawValue.split(/[,\n，|]/).map(item => item.trim()).filter(Boolean)
      }
      return []
    },
    normalizeHomepageConfig(rawValue) {
      if (!rawValue) return {}
      if (typeof rawValue === 'object') return rawValue
      if (typeof rawValue !== 'string') return {}
      const value = rawValue.trim()
      if (!value) return {}
      try {
        return JSON.parse(value)
      } catch (e) {
        console.warn('[artist/home] 首页配置解析失败:', e)
        return {}
      }
    },
    normalizeStyle2Tabs(config = {}) {
      const defaultByKey = this.defaultStyle2Tabs.reduce((map, tab) => {
        map[tab.key] = tab
        return map
      }, {})
      const rawTabs = config.style2Tabs || config.tabs || config.sections || config.modules
      if (!Array.isArray(rawTabs)) return []
      const tabs = rawTabs
        .map(item => {
          if (typeof item === 'string') return defaultByKey[item] ? { ...defaultByKey[item] } : null
          const key = item?.key || item?.module || item?.name
          if (!key || !defaultByKey[key] || item.visible === false || item.enabled === false) return null
          return {
            ...defaultByKey[key],
            label: item.label || item.title || defaultByKey[key].label
          }
        })
        .filter(Boolean)
      const seen = new Set()
      return tabs.filter(tab => {
        if (seen.has(tab.key)) return false
        seen.add(tab.key)
        return true
      })
    },

    formatPrice(v) {
      const amount = Number(v || 0)
      return amount > 0 ? amount.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }) : '0.00'
    },
    normalizeProductPrice(value) {
      const amount = Number(value || 0)
      if (!amount) return 0
      return amount >= 100 ? amount / 100 : amount
    },
    normalizeGrowthRate(value) {
      const rate = Number(value || 0)
      if (!rate) return 0
      return Math.abs(rate) > 10 ? rate / 100 : rate
    },
    resolvePublishPrice(item = {}) {
      if (item._pricesNormalized) {
        return Number(item.publishPrice || item.originalPrice || item.original_price || item.price || 0)
      }
      return this.normalizeProductPrice(
        item.publishPrice ||
        item.publish_price ||
        item.originalPrice ||
        item.original_price ||
        item.basePrice ||
        item.price ||
        0
      )
    },
    resolveCurrentPrice(item = {}) {
      if (item._pricesNormalized) {
        return Number(item.currentPrice || item.price || item.publishPrice || item.originalPrice || 0)
      }
      const currentPrice = this.normalizeProductPrice(item.currentPrice || item.current_price || item.displayPrice || 0)
      const publishPrice = this.resolvePublishPrice(item)
      const priceRise = this.normalizeGrowthRate(item.priceRise || item.price_rise || item.dailyIncreaseRate || 0)
      if (publishPrice > 0 && priceRise > 0) {
        const computedPrice = publishPrice * (1 + priceRise)
        if (!currentPrice || currentPrice <= publishPrice * 1.02) {
          return computedPrice
        }
      }
      return currentPrice || publishPrice || this.normalizeProductPrice(item.price || 0)
    },
    resolveOriginalPrice(item = {}) {
      return this.resolvePublishPrice(item)
    },
    resolveDealPrice(item = {}) {
      if (item._pricesNormalized) {
        return Number(item.dealPrice || item.tradePrice || item.soldPrice || item.transactionPrice || item.currentPrice || item.price || 0)
      }
      return this.normalizeProductPrice(item.dealPrice || item.tradePrice || item.soldPrice || item.transactionPrice || item.currentPrice || item.price || 0)
    },
    buildCollectorLabel(region) {
      const value = String(region || '').trim()
      if (!value) return '藏家收藏'
      if (value.endsWith('地区') || value.endsWith('藏家')) return `${value}收藏`
      return `${value}地区藏家收藏`
    },
    buildForecast30Display(item = {}) {
      const tomorrowMin = Number(item.tomorrowIncreaseMin || 0)
      const tomorrowMax = Number(item.tomorrowIncreaseMax || 0)
      if (tomorrowMin > 0 || tomorrowMax > 0) {
        const low = Math.min(tomorrowMin || tomorrowMax, tomorrowMax || tomorrowMin) * 30
        const high = Math.max(tomorrowMin, tomorrowMax) * 30
        return low === high ? this.formatYuanDelta(low) : `${this.formatYuanDelta(low)}–${this.formatYuanDelta(high)}`
      }

      const price = Number(item.currentPrice || item.price || 0)
      const priceRise = this.normalizeGrowthRate(item.priceRise || item.price_rise || item.dailyIncreaseRate || 0)
      if (!price || !priceRise) return ''
      const delta = price * priceRise * 30
      if (delta <= 0) return ''
      return this.formatYuanDelta(delta)
    },
    buildDealGrowthDisplay(item = {}) {
      const original = this.resolveOriginalPrice(item)
      const deal = this.resolveDealPrice(item)
      if (!original || !deal || deal <= original) return '+0.0%'
      return `+${(((deal - original) / original) * 100).toFixed(1)}%`
    },
    formatYuanDelta(value) {
      const amount = Number(value || 0)
      if (amount <= 0) return ''
      if (amount < 1) return `¥${amount.toFixed(2)}`
      if (amount < 100) return `¥${amount.toFixed(1).replace(/\.0$/, '')}`
      return `¥${this.formatPrice(amount)}`
    },
    goBack() {
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.reLaunch({ url: '/pages/index/index' })
      }
    },
    async followArtist() {
      if (this.followLoading) return
      this.followLoading = true
      try {
        if (this.followed) {
          await userApi.unfollowArtist(this.artist.id)
          this.followed = false
          this.adjustFansCount(-1)
          uni.showToast({ title: '已取消关注', icon: 'success' })
        } else {
          await userApi.followArtist(this.artist.id)
          this.followed = true
          this.adjustFansCount(1)
          uni.showToast({ title: '关注成功', icon: 'success' })
        }
      } catch (e) {
        console.error('关注操作失败', e)
        uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
      } finally {
        this.followLoading = false
      }
    },
    openFollowMenu() {
      if (this.followLoading || !this.followed) return
      this.followMenuVisible = !this.followMenuVisible
    },
    confirmUnfollow() {
      if (this.followLoading || !this.followed) return
      this.followMenuVisible = false
      uni.showModal({
        title: '取消关注',
        content: `确认不再关注${this.artist.name || '该艺术家'}？`,
        confirmText: '取消关注',
        confirmColor: '#c59b30',
        success: ({ confirm }) => {
          if (confirm) this.followArtist()
        }
      })
    },
    adjustFansCount(step) {
      this.stats = this.stats.map(item => {
        if (item.label !== '粉丝') return item
        const current = Number(String(item.value || '0').replace(/,/g, '')) || 0
        return { ...item, value: String(Math.max(current + step, 0)) }
      })
    },
    async shareArtist() {
      const shareUrl = typeof window !== 'undefined'
        ? `${window.location.origin}/#${this.sharePath}`
        : this.sharePath

      // #ifdef H5
      if (navigator?.share) {
        try {
          await navigator.share({
            title: this.shareTitle,
            text: `${this.artist.title || '艺术家主页'}，来看看TA的代表作品`,
            url: shareUrl
          })
          return
        } catch (error) {
          if (error?.name === 'AbortError') {
            return
          }
        }
      }
      if (typeof window !== 'undefined') {
        uni.showActionSheet({
          itemList: ['复制链接', '新窗口打开'],
          success: ({ tapIndex }) => {
            if (tapIndex === 0) {
              uni.setClipboardData({
                data: shareUrl,
                success: () => {
                  uni.showToast({ title: '链接已复制，可分享到微信或小红书', icon: 'none' })
                },
                fail: () => {
                  uni.showToast({ title: '当前环境暂不支持复制', icon: 'none' })
                }
              })
              return
            }
            window.open(shareUrl, '_blank', 'noopener,noreferrer')
          },
          fail: () => {
            uni.showToast({ title: '当前环境暂不支持系统分享', icon: 'none' })
          }
        })
        return
      }
      // #endif

      uni.setClipboardData({
        data: shareUrl,
        success: () => {
          uni.showToast({ title: '分享链接已复制', icon: 'none' })
        },
        fail: () => {
          uni.showToast({ title: '当前环境暂不支持分享', icon: 'none' })
        }
      })
    },
    consult() {
      const query = [`userId=${this.artist.id}`]
      if (this.artist.name) query.push(`name=${encodeURIComponent(this.artist.name)}`)
      uni.navigateTo({ url: `/pages/message/chat?${query.join('&')}` })
    },
    goGallery() { uni.navigateTo({ url: `/pages/artist/gallery/index?id=${this.artist.id}` }) },
    goWorks() { uni.navigateTo({ url: `/pages/artist/works/index?id=${this.artist.id}` }) },
    goAnalytics() { uni.navigateTo({ url: `/pages/artist/analytics?id=${this.artist.id}` }) },
    goWork(id) { uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` }) },
    editHomepage(section = '') {
      const query = section ? `?section=${encodeURIComponent(section)}` : ''
      uni.navigateTo({ url: `/pages/user/profile${query}` })
    },
    editResumeSection(sectionKey) {
      if (!this.isOwnArtistPage) return
      const artistId = this.requestedArtistId || this.artist.id
      uni.navigateTo({
        url: `/pages/artist/resume-edit?userId=${encodeURIComponent(artistId)}&section=${encodeURIComponent(sectionKey)}`
      })
    },
    goIndex() { uni.reLaunch({ url: '/pages/index/index' }) },
    goPublish() { uni.navigateTo({ url: '/pages/artist/publish' }) },
    goCart() { uni.navigateTo({ url: '/pages/cart/index' }) },
    goMine() { uni.navigateTo({ url: '/pages/user/index' }) }
  },
  onShareAppMessage() {
    return {
      title: this.shareTitle,
      path: this.sharePath,
      imageUrl: this.artist.cover || this.artist.avatar || ''
    }
  },
  onShareTimeline() {
    return {
      title: this.shareTitle,
      query: `userId=${this.artist.id}`,
      imageUrl: this.artist.cover || this.artist.avatar || ''
    }
  }
}
</script>

<style lang="scss" scoped>
$bg: #050505;
$surface: #151515;
$surface-2: #1e1e1e;
$gold: #d7a51d;
$gold-light: #f3c43d;
$text: #f7f7f7;
$muted: #a7a7a7;
$dim: #6f6f6f;
$line: rgba(255, 255, 255, 0.1);
$gold-line: rgba(215, 165, 29, 0.65);

.artist-home-page {
  min-height: 100vh;
  padding: calc(88rpx + env(safe-area-inset-top)) 24rpx calc(178rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  color: $text;
  background:
    radial-gradient(circle at 70% 5%, rgba(215, 165, 29, 0.12), transparent 26%),
    linear-gradient(180deg, #080808 0%, #040404 100%);
}

.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 40;
  height: calc(88rpx + env(safe-area-inset-top));
  padding: env(safe-area-inset-top) 24rpx 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: transparent;
}

.nav-title {
  flex: 1;
}

.nav-icon {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 58rpx;
  line-height: 1;
}

.nav-icon.external {
  font-size: 40rpx;
}

.profile-hero {
  position: relative;
  height: 288rpx;
  overflow: visible;
  border-radius: 10rpx;
  background:
    radial-gradient(circle at 72% 20%, rgba(215, 165, 29, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(8, 36, 68, 0.96), rgba(7, 20, 38, 0.98));
}

.cover-image,
.cover-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border-radius: 10rpx;
}

.cover-shade {
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.02), rgba(0, 0, 0, 0.2) 48%, rgba(0, 0, 0, 0.86) 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.45), transparent 42%, rgba(0, 0, 0, 0.12));
}

.profile-core {
  position: absolute;
  left: 42rpx;
  right: 28rpx;
  bottom: -72rpx;
  display: flex;
  align-items: flex-end;
  gap: 26rpx;
}

.avatar-wrap {
  position: relative;
  flex: 0 0 auto;
}

.avatar {
  width: 166rpx;
  height: 166rpx;
  border-radius: 50%;
  border: 4rpx solid $gold-light;
  background: #222;
}

.avatar-cert {
  position: absolute;
  right: 4rpx;
  bottom: 8rpx;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffdc62, $gold);
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
}

.identity-block {
  flex: 1;
  min-width: 0;
  padding-bottom: 18rpx;
}

.artist-name-row {
  display: flex;
  align-items: center;
  min-width: 0;
}

.artist-name {
  min-width: 0;
  font-size: 42rpx;
  line-height: 1.08;
  font-weight: 900;
}

.artist-title {
  margin-top: 8rpx;
  color: #d4d4d4;
  font-size: 19rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 14rpx;
  margin-top: 14rpx;
}

.tag {
  flex-shrink: 0;
  white-space: nowrap;
  height: 42rpx;
  padding: 0 16rpx;
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  border: 1rpx solid $gold-line;
  border-radius: 7rpx;
  color: $gold-light;
  background: rgba(0, 0, 0, 0.34);
  font-size: 22rpx;
  font-weight: 700;
}

.tag-icon {
  font-size: 22rpx;
}

.stats-actions {
  display: grid;
  grid-template-columns: 1fr 360rpx;
  gap: 24rpx;
  align-items: center;
  margin-top: 90rpx;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}

.stat-item {
  text-align: center;
  border-right: 1rpx solid rgba(255, 255, 255, 0.22);
}

.stat-item:last-child {
  border-right: 0;
}

.stat-value {
  display: block;
  font-size: 32rpx;
  line-height: 1;
  font-weight: 900;
}

.stat-label {
  display: block;
  margin-top: 10rpx;
  color: $muted;
  font-size: 22rpx;
}

.hero-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.gold-btn,
.outline-btn,
.gallery-btn,
.all-works-btn,
.consult-btn {
  height: 62rpx;
  border-radius: 8rpx;
  font-size: 25rpx;
  font-weight: 800;
}

.gold-btn,
.gallery-btn,
.consult-btn {
  color: #201600;
  background: linear-gradient(135deg, #f3c43d 0%, #d6a51d 100%);
}

.outline-btn,
.all-works-btn {
  color: $gold-light;
  border: 1rpx solid $gold-line;
  background: rgba(5, 5, 5, 0.5);
}

.intro-card {
  position: relative;
  margin-top: 28rpx;
  padding: 24rpx 28rpx 30rpx;
  border-radius: 12rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(135deg, #202020, #151515);
}

.section-title {
  color: $text;
  font-size: 32rpx;
  line-height: 1;
  font-weight: 900;
}

.gold-title {
  color: $gold-light;
}

.intro-text {
  margin-top: 22rpx;
  color: #b9b9b9;
  font-size: 25rpx;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.intro-text.expanded {
  display: block;
}

.quote {
  margin-top: 14rpx;
  padding-right: 96rpx;
  color: $gold-light;
  font-size: 24rpx;
  line-height: 1.5;
  font-weight: 700;
}

.expand {
  position: absolute;
  right: 26rpx;
  bottom: 28rpx;
  color: $dim;
  font-size: 24rpx;
}

.section {
  margin-top: 28rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.more-link {
  color: $muted;
  font-size: 24rpx;
}

.empty-block {
  padding: 36rpx 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.03);
  color: $muted;
  font-size: 24rpx;
  text-align: center;
}

.work-scroll {
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
}

.work-row {
  display: inline-flex;
  gap: 16rpx;
}

.work-card {
  width: 292rpx;
  overflow: hidden;
  border-radius: 8rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background: #1a1a1a;
}

.work-image-wrap {
  position: relative;
  height: 180rpx;
}

.work-image {
  width: 100%;
  height: 100%;
}

.new-badge {
  position: absolute;
  top: 14rpx;
  right: 14rpx;
  width: 40rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7rpx;
  color: #151000;
  background: $gold-light;
  font-size: 22rpx;
  font-weight: 900;
}

.work-info {
  padding: 16rpx;
}

.work-title {
  color: #fff;
  font-size: 25rpx;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.work-meta {
  margin-top: 8rpx;
  color: $muted;
  font-size: 20rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.work-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 12rpx;
}

.price {
  color: $gold-light;
  font-size: 27rpx;
  font-weight: 900;
}

.collector-label {
  max-width: 132rpx;
  font-size: 21rpx;
  line-height: 1.2;
  white-space: normal;
  word-break: break-all;
}

.collect-tag {
  padding: 3rpx 10rpx;
  border-radius: 6rpx;
  border: 1rpx solid $gold-line;
  color: $gold-light;
  font-size: 19rpx;
}

.gallery-banner {
  position: relative;
  min-height: 122rpx;
  margin-top: 26rpx;
  padding: 18rpx 22rpx;
  display: grid;
  grid-template-columns: 92rpx 1fr 190rpx;
  gap: 20rpx;
  align-items: center;
  overflow: hidden;
  border-radius: 10rpx;
  border: 1rpx solid $gold-line;
  background: #0c0c0c;
}

.gallery-bg,
.gallery-mask {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.gallery-bg {
  opacity: 0.46;
}

.gallery-mask {
  background: linear-gradient(90deg, rgba(5, 5, 5, 0.9), rgba(26, 17, 4, 0.72), rgba(5, 5, 5, 0.82));
}

.gallery-icon {
  position: relative;
  z-index: 1;
  color: $gold-light;
  font-size: 66rpx;
  line-height: 1;
  text-align: center;
}

.gallery-title {
  position: relative;
  z-index: 1;
  color: $gold-light;
  font-size: 34rpx;
  font-weight: 900;
}

.gallery-desc {
  position: relative;
  z-index: 1;
  margin-top: 8rpx;
  color: $muted;
  font-size: 22rpx;
}

.gallery-btn {
  position: relative;
  z-index: 1;
  height: 58rpx;
  font-size: 23rpx;
}

.flow-list {
  margin-top: 18rpx;
}

.flow-item {
  display: grid;
  grid-template-columns: 126rpx 1fr 118rpx 118rpx 118rpx 28rpx;
  gap: 12rpx;
  align-items: center;
  margin-top: 10rpx;
  padding: 10rpx;
  border-radius: 8rpx;
  background: linear-gradient(135deg, #222, #151515);
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.flow-cover {
  width: 126rpx;
  height: 72rpx;
  border-radius: 6rpx;
}

.flow-title {
  color: #e9e9e9;
  font-size: 24rpx;
  font-weight: 800;
}

.flow-price {
  margin-top: 6rpx;
  color: $gold-light;
  font-size: 24rpx;
  font-weight: 900;
}

.flow-feature {
  color: $gold-light;
  font-size: 21rpx;
  text-align: center;
}

.flow-arrow {
  color: $muted;
  font-size: 38rpx;
}

.trust-section {
  margin-top: 28rpx;
}

.trust-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10rpx;
  margin-top: 22rpx;
}

.trust-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding-right: 10rpx;
  border-right: 1rpx solid rgba(255, 255, 255, 0.16);
}

.trust-item:last-child {
  border-right: 0;
}

.trust-icon {
  flex: 0 0 auto;
  color: $gold-light;
  font-size: 42rpx;
}

.trust-title {
  color: #cfcfcf;
  font-size: 20rpx;
  line-height: 1.2;
}

.trust-desc {
  margin-top: 4rpx;
  color: $muted;
  font-size: 18rpx;
  line-height: 1.2;
}

.bottom-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 30rpx;
}

.all-works-btn,
.consult-btn {
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  border-radius: 8rpx;
  font-size: 27rpx;
}

.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  height: 126rpx;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  padding-top: 14rpx;
  box-sizing: border-box;
  background: rgba(24, 24, 24, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18rpx);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  color: #8c8c8c;
  font-size: 22rpx;
}

.tab-item.active {
  color: $gold-light;
}

.tab-icon {
  font-size: 38rpx;
  line-height: 1;
}

button::after {
  border: 0;
}

/* ===== art_profile_page_with_modern_design 1:1 rebuild ===== */
.artist-home-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + 12rpx) 20rpx calc(170rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  color: #f7f7f7;
  background:
    radial-gradient(circle at 80% 5%, rgba(214, 166, 32, 0.08), transparent 24%),
    linear-gradient(180deg, #000 0%, #050505 100%);
}

.top-nav {
  position: absolute;
  top: env(safe-area-inset-top);
  left: 20rpx;
  right: 20rpx;
  z-index: 2;
  height: 76rpx;
  padding: 0 0;
  background: transparent;
  backdrop-filter: none;
}

.nav-title {
  color: #fff;
  font-size: 30rpx;
  line-height: 1;
  font-weight: 800;
}

.nav-icon {
  width: 58rpx;
  height: 58rpx;
  font-size: 0;
}

.nav-icon image {
  width: 40rpx;
  height: 40rpx;
}

.nav-icon.external image {
  width: 38rpx;
  height: 38rpx;
}

.profile-hero {
  height: 356rpx;
  margin-top: 0;
  border-radius: 14rpx;
}

.cover-image,
.cover-shade {
  border-radius: 14rpx;
}

.cover-image {
  height: 230rpx;
}

.cover-shade {
  height: 356rpx;
  background:
    linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.06) 34%, rgba(0, 0, 0, 0.9) 70%, #000 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.18), transparent 45%, rgba(0, 0, 0, 0.08));
}

.profile-core {
  left: 36rpx;
  right: 22rpx;
  bottom: 12rpx;
  gap: 20rpx;
  align-items: flex-end;
}

.avatar {
  width: 166rpx;
  height: 166rpx;
  border: 4rpx solid #f2c14e;
  box-shadow: 0 0 0 1rpx rgba(0, 0, 0, 0.45), 0 16rpx 38rpx rgba(0, 0, 0, 0.45);
}

.avatar-cert {
  right: -6rpx;
  bottom: 14rpx;
  width: 46rpx;
  height: 46rpx;
  color: #fff;
  font-size: 30rpx;
  background: linear-gradient(180deg, #f7d46c, #d9a935);
}

.identity-block {
  padding-bottom: 8rpx;
}

.artist-name {
  color: #fff;
  font-size: 38rpx;
  line-height: 1;
  font-weight: 900;
}

.artist-title {
  margin-top: 14rpx;
  color: #d0d0d0;
  font-size: 23rpx;
}

.tag-row {
  gap: 12rpx;
  margin-top: 22rpx;
}

.tag {
  flex-shrink: 0;
  white-space: nowrap;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 7rpx;
  border-color: rgba(242, 193, 78, 0.9);
  color: #f2c14e;
  background: rgba(0, 0, 0, 0.42);
  font-size: 20rpx;
  font-weight: 700;
}

.tag-icon-img {
  width: 22rpx;
  height: 22rpx;
}

.stats-actions {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: 395rpx 1fr;
  gap: 24rpx;
}

.stats {
  grid-template-columns: repeat(3, 1fr);
}

.stat-value {
  color: #fff;
  font-size: 34rpx;
}

.stat-label {
  margin-top: 10rpx;
  color: #a9a9a9;
  font-size: 22rpx;
}

.stat-item {
  border-right-color: rgba(255, 255, 255, 0.24);
}

.hero-actions {
  gap: 14rpx;
}

.gold-btn,
.outline-btn {
  height: 58rpx;
  border-radius: 7rpx;
  font-size: 22rpx;
  font-weight: 900;
}

.gold-btn {
  background: linear-gradient(180deg, #f4c653, #d8a81e);
}

.outline-btn {
  color: #f2c14e;
  border-color: rgba(242, 193, 78, 0.75);
  background: rgba(0, 0, 0, 0.28);
}

.intro-card {
  margin-top: 30rpx;
  padding: 28rpx 28rpx 26rpx;
  border-radius: 11rpx;
  border-color: rgba(255, 255, 255, 0.09);
  background: linear-gradient(135deg, #202020, #141414);
}

.section-title {
  color: #fff;
  font-size: 32rpx;
  line-height: 1;
}

.gold-title {
  color: #f2c14e;
  font-size: 27rpx;
}

.intro-text {
  margin-top: 22rpx;
  color: #c1c1c1;
  font-size: 22rpx;
  line-height: 1.72;
  -webkit-line-clamp: 2;
}

.quote {
  margin-top: 16rpx;
  padding-right: 110rpx;
  color: #f2c14e;
  font-size: 21rpx;
  line-height: 1.5;
}

.expand {
  right: 28rpx;
  bottom: 27rpx;
  color: #8c8c8c;
  font-size: 21rpx;
}

.section {
  margin-top: 30rpx;
}

.section-head {
  margin-bottom: 20rpx;
}

.more-link {
  color: #aaa;
  font-size: 22rpx;
}

.work-row {
  gap: 12rpx;
}

.work-card {
  width: 274rpx;
  border-radius: 8rpx;
  background: #171717;
}

.work-image-wrap {
  height: 190rpx;
}

.new-badge {
  top: 12rpx;
  right: 12rpx;
  width: 38rpx;
  height: 42rpx;
  border-radius: 8rpx;
  font-size: 21rpx;
  background: linear-gradient(180deg, #f6d98a, #d9a935);
}

.work-info {
  padding: 16rpx;
}

.work-title {
  font-size: 24rpx;
}

.work-meta {
  font-size: 18rpx;
}

.price {
  font-size: 27rpx;
  color: #f2c14e;
}

.collect-tag {
  padding: 4rpx 11rpx;
  border-radius: 6rpx;
  font-size: 18rpx;
}

/* 价格更新时间戳与错误状态 */
.work-bottom-right {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.price-updated {
  color: #7d7d7d;
  font-size: 18rpx;
}

.price-error {
  color: #e74c3c !important;
  text-decoration: line-through;
  opacity: 0.7;
}

.circulation-section {
  margin-top: 30rpx;
}

.flow-list {
  margin-top: 18rpx;
}

.flow-item {
  min-height: 76rpx;
  grid-template-columns: 138rpx 150rpx 104rpx 104rpx 104rpx 18rpx;
  gap: 8rpx;
  margin-top: 7rpx;
  padding: 0 12rpx 0 0;
  border-radius: 7rpx;
  background: linear-gradient(135deg, #202020, #141414);
}

.flow-cover {
  width: 138rpx;
  height: 76rpx;
  border-radius: 7rpx 0 0 7rpx;
}

.flow-title {
  font-size: 21rpx;
  line-height: 1.15;
}

.flow-price {
  font-size: 22rpx;
}

.flow-feature {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5rpx;
  color: #f2c14e;
  font-size: 17rpx;
  white-space: nowrap;
}

.flow-feature image {
  width: 24rpx;
  height: 24rpx;
}

.flow-arrow {
  color: #aaa;
  font-size: 34rpx;
}

.trust-section {
  margin-top: 30rpx;
}

.trust-row {
  margin-top: 22rpx;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10rpx;
}

.trust-item {
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 4rpx;
  min-height: 94rpx;
  padding: 12rpx 10rpx;
  border-right: 0;
  border-radius: 10rpx;
  background: linear-gradient(135deg, rgba(28, 28, 28, 0.94), rgba(17, 17, 17, 0.96));
}

.trust-item:last-child {
  border-right: 0;
}

.trust-icon {
  width: 32rpx;
  height: 32rpx;
}

.trust-title {
  color: #e2e2e2;
  font-size: 17rpx;
  line-height: 1.25;
  font-weight: 700;
}

.trust-desc {
  color: #999;
  font-size: 16rpx;
  line-height: 1.25;
}

.bottom-actions {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: calc(22rpx + env(safe-area-inset-bottom));
  z-index: 30;
  grid-template-columns: minmax(0, 0.88fr) minmax(0, 1.12fr);
  gap: 16rpx;
  margin-top: 34rpx;
  padding: 16rpx;
  border-radius: 18rpx;
  background: rgba(6, 6, 6, 0.92);
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(16rpx);
  box-shadow: 0 -10rpx 30rpx rgba(0, 0, 0, 0.22);
  align-items: stretch;
}

.all-works-btn,
.consult-btn {
  height: 80rpx;
  border-radius: 10rpx;
  font-size: 24rpx;
  font-weight: 800;
}

.analytics-btn {
  height: 80rpx;
  border-radius: 10rpx;
  font-size: 24rpx;
  font-weight: 800;
  border: 1rpx solid rgba(214, 168, 39, 0.35);
  background: transparent;
  color: #d6a827;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 0 24rpx;
  min-width: 140rpx;
}

.analytics-btn__icon {
  width: 32rpx;
  height: 32rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.analytics-btn__icon image {
  width: 24rpx;
  height: 24rpx;
}

.all-works-btn {
  color: #fff;
  border: 1rpx solid rgba(242, 193, 78, 0.8);
  background: linear-gradient(135deg, #202020, #111);
}

.consult-btn {
  color: #111;
  background: linear-gradient(180deg, #f6d269, #d9a935);
  box-shadow: 0 12rpx 28rpx rgba(217, 169, 53, 0.18);
}

.consult-btn__icon {
  width: 38rpx;
  height: 38rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(17, 17, 17, 0.16);
}

.consult-btn__icon image {
  width: 22rpx;
  height: 22rpx;
  filter: brightness(0) saturate(100%);
}

.artist-home-page.style-2 {
  padding: 0 24rpx calc(154rpx + env(safe-area-inset-bottom));
  background: #090909;
}

.style-2 .top-nav {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  height: calc(88rpx + env(safe-area-inset-top));
  padding: env(safe-area-inset-top) 28rpx 0;
  background: transparent;
  backdrop-filter: none;
}

.style-2 .nav-title {
  min-width: 0;
  padding: 0 20rpx;
  color: #fff;
  font-size: 31rpx;
  line-height: 1;
  font-weight: 900;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style-2 .profile-hero {
  height: 390rpx;
  border-radius: 14rpx;
  margin-top: 0;
  overflow: hidden;
  box-shadow: 0 20rpx 46rpx rgba(0, 0, 0, 0.52);
}

.style-2 .cover-image {
  height: 100%;
  opacity: 0.92;
}

.style-2 .cover-shade {
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.24) 42%, rgba(0, 0, 0, 0.86) 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.64), rgba(0, 0, 0, 0.1) 58%, rgba(0, 0, 0, 0.26));
}

.style-2 .profile-core {
  left: 38rpx;
  right: 34rpx;
  bottom: 40rpx;
  align-items: center;
  gap: 28rpx;
}

.style-2 .avatar {
  width: 150rpx;
  height: 150rpx;
  border: 4rpx solid #f2c14e;
  box-shadow: 0 16rpx 34rpx rgba(0, 0, 0, 0.42);
}

.style-2 .avatar-cert {
  right: -8rpx;
  bottom: 5rpx;
  width: 44rpx;
  height: 44rpx;
  background: linear-gradient(180deg, #f8d970, #d7a51d);
}

.style-2 .identity-block {
  padding-bottom: 0;
  min-width: 0;
}

.style-2 .artist-name {
  max-width: calc(100% - 40rpx);
  font-size: 26rpx;
  line-height: 1;
  font-weight: 950;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style-2 .artist-name-row {
  position: relative;
  gap: 6rpx;
}

.style-2 .follow-more {
  width: 34rpx;
  height: 34rpx;
  min-width: 34rpx;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  line-height: 1;
  box-sizing: border-box;
}

.style-2 .follow-more::after {
  border: 0;
}

.style-2 .follow-more[disabled] {
  opacity: 0.45;
}

.style-2 .follow-more-chevron {
  width: 9rpx;
  height: 9rpx;
  margin-top: -4rpx;
  border-right: 2rpx solid rgba(242, 193, 78, 0.92);
  border-bottom: 2rpx solid rgba(242, 193, 78, 0.92);
  transform: rotate(45deg);
  box-sizing: border-box;
}

.style-2 .follow-menu-popover {
  position: absolute;
  z-index: 8;
  top: 42rpx;
  right: -12rpx;
  min-width: 112rpx;
  padding: 12rpx 16rpx;
  border: 1rpx solid rgba(242, 193, 78, 0.46);
  border-radius: 6rpx;
  background: rgba(16, 15, 12, 0.96);
  color: #d8b14d;
  font-size: 20rpx;
  line-height: 1;
  font-weight: 400;
  text-align: center;
  white-space: nowrap;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.36);
}

.style-2 .artist-title {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.74);
  font-size: 23rpx;
  line-height: 1.08;
  font-weight: 400;
  letter-spacing: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-artist-uid {
  margin-top: 9rpx;
  color: rgba(255, 255, 255, 0.48);
  font-size: 19rpx;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style-2 .tag-row {
  display: none;
}

.style2-hero-stats {
  display: flex;
  gap: 16rpx;
  margin-top: 18rpx;
  min-width: 0;
}

.style2-hero-pill {
  height: 52rpx;
  min-width: 122rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  background: rgba(12, 12, 12, 0.62);
  box-sizing: border-box;
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.05);
}

.style2-hero-pill text {
  color: rgba(255, 255, 255, 0.72);
  font-size: 23rpx;
  line-height: 1;
  font-weight: 400;
}

.style2-hero-pill strong {
  color: #f2c14e;
  font-size: 27rpx;
  line-height: 1;
  font-weight: 800;
}

.style-2 .stats-actions {
  display: block;
  margin-top: 32rpx;
}

.style-2 .hero-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  justify-items: center;
}

.style-2 .gold-btn,
.style-2 .outline-btn {
  width: 100%;
  max-width: 360rpx;
  height: 76rpx;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  border-radius: 12rpx;
  font-size: 29rpx;
  font-weight: 900;
  box-sizing: border-box;
}

.style-2 .follow-btn-icon {
  width: 28rpx;
  height: 28rpx;
  flex: 0 0 28rpx;
  display: block;
  filter: sepia(1) saturate(2) hue-rotate(353deg) brightness(1.08);
}

.style-2 .gold-btn {
  color: #f2c14e;
  border: 1rpx solid rgba(242, 193, 78, 0.72);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.01));
}

.style-2 .outline-btn {
  color: #171100;
  border: 0;
  background: linear-gradient(180deg, #f6d56e, #d9a935);
  font-weight: 400;
}

.style2-tabs {
  height: 104rpx;
  margin: 36rpx 0 12rpx;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;
}

.style2-tab {
  position: relative;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: rgba(247, 247, 247, 0.24);
  font-size: 18px;
  font-weight: 400;
  white-space: nowrap;
}

.style2-tab.active {
  color: #d9d9d9;
  font-size: 19px;
  background: transparent;
}

.style2-tab.active::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 14rpx;
  width: 58rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: #f2c14e;
}

.style2-panel {
  margin-top: 0;
}

.style2-featured {
  position: relative;
  height: 336rpx;
  overflow: hidden;
  display: block;
  border-radius: 10rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.11);
  background: linear-gradient(135deg, #23211d 0%, #151412 100%);
  box-shadow:
    inset 0 1rpx 0 rgba(255, 255, 255, 0.05),
    0 14rpx 30rpx rgba(0, 0, 0, 0.28);
}

.style2-featured-image {
  position: absolute;
  z-index: 0;
  left: 16rpx;
  top: 16rpx;
  width: calc(56% - 24rpx);
  height: calc(100% - 32rpx);
  border-radius: 10rpx;
  box-shadow: 0 10rpx 22rpx rgba(0, 0, 0, 0.22);
}

.style2-featured::after {
  content: "";
  position: absolute;
  z-index: 2;
  top: 16rpx;
  right: 16rpx;
  bottom: 16rpx;
  width: calc(44% - 8rpx);
  border-radius: 0 10rpx 10rpx 0;
  background:
    linear-gradient(90deg, rgba(22, 20, 17, 0.74) 0%, rgba(28, 25, 20, 0.96) 28%, rgba(18, 17, 15, 0.99) 100%),
    radial-gradient(circle at 100% 0%, rgba(228, 185, 72, 0.14), transparent 40%);
}

.style2-featured-mask {
  position: absolute;
  z-index: 1;
  left: 16rpx;
  top: 16rpx;
  bottom: 16rpx;
  width: calc(56% - 24rpx);
  border-radius: 10rpx;
  background:
    linear-gradient(90deg, rgba(0, 0, 0, 0) 58%, rgba(0, 0, 0, 0.32) 100%),
    linear-gradient(180deg, rgba(0, 0, 0, 0.02) 38%, rgba(0, 0, 0, 0.52) 100%);
}

.style2-featured-label {
  position: absolute;
  z-index: 3;
  left: 34rpx;
  bottom: 34rpx;
  height: 42rpx;
  padding: 0 17rpx;
  display: flex;
  align-items: center;
  border-radius: 999rpx;
  border: 1rpx solid rgba(230, 185, 61, 0.76);
  background: rgba(11, 11, 11, 0.62);
  color: #e6b93d;
  font-size: 20rpx;
  line-height: 1;
  font-weight: 300;
  box-sizing: border-box;
  backdrop-filter: blur(8rpx);
}

.style2-featured-like {
  position: absolute;
  z-index: 4;
  right: 6rpx;
  top: 8rpx;
  width: 46rpx;
  height: 46rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 0;
  background: transparent;
}

.style2-featured-like image {
  width: 32rpx;
  height: 32rpx;
}

.style2-featured-copy {
  position: absolute;
  z-index: 3;
  left: calc(56% + 28rpx);
  right: 64rpx;
  top: 46rpx;
  bottom: auto;
}

.style2-featured-title,
.style2-featured-meta,
.style2-featured-growth {
  display: block;
}

.style2-featured-title {
  max-width: 100%;
  color: #f5efef;
  font-size: 30rpx;
  line-height: 1.24;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-featured-meta {
  margin-top: 13rpx;
  color: rgba(247, 247, 247, 0.7);
  font-size: 20rpx;
  line-height: 1.16;
  font-weight: 300;
  white-space: nowrap;
}

.style2-featured-growth {
  margin-top: 16rpx;
  color: #c59b30;
  font-size: 17rpx;
  line-height: 1.2;
  font-weight: 200;
  white-space: nowrap;
}

.style2-featured-price-block {
  position: absolute;
  z-index: 3;
  left: calc(56% + 28rpx);
  right: 34rpx;
  bottom: 30rpx;
  text-align: left;
}

.style2-featured-price {
  color: #ecbd51;
  font-size: 34rpx;
  line-height: 1;
  font-weight: 700;
}

.style2-featured-price.collected {
  color: #ecbd51;
  font-size: 28rpx;
  line-height: 1.15;
  font-weight: 600;
}

.style2-featured-history {
  margin-top: 10rpx;
  color: rgba(245, 241, 232, 0.56);
  font-size: 19rpx;
  line-height: 1;
  font-weight: 600;
  white-space: nowrap;
}

.style2-grid {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22rpx 18rpx;
}

.style2-work-card,
.style2-resume-card,
.style2-circulation-stats,
.style2-record {
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background:
    radial-gradient(circle at 86% 0%, rgba(242, 193, 78, 0.08), transparent 34%),
    linear-gradient(145deg, rgba(31, 31, 31, 0.96), rgba(14, 14, 14, 0.98));
}

.style2-work-card {
  position: relative;
  overflow: hidden;
  border-radius: 10rpx;
}

.style2-work-badge {
  position: absolute;
  top: 18rpx;
  right: 18rpx;
  z-index: 2;
  min-width: 54rpx;
  height: 42rpx;
  padding: 0 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx;
  background: linear-gradient(180deg, #f5d875, #c8a144);
  color: #17120a;
  font-size: 21rpx;
  font-weight: 900;
  box-sizing: border-box;
}

.style2-work-like {
  position: absolute;
  top: 18rpx;
  right: 18rpx;
  z-index: 3;
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(10, 10, 10, 0.46);
  backdrop-filter: blur(8rpx);
}

.style2-work-like image {
  width: 32rpx;
  height: 32rpx;
}

.style2-work-image {
  width: 100%;
  height: 260rpx;
  display: block;
}

.style2-work-info {
  min-height: 190rpx;
  padding: 22rpx 20rpx 20rpx;
}

.style2-work-title {
  color: #fff;
  font-size: 26rpx;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-work-meta {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.58);
  font-size: 20rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-work-price {
  margin-top: 12rpx;
  color: #f2c14e;
  font-size: 25rpx;
  font-weight: 900;
}

.style2-work-collected {
  margin-top: 12rpx;
  color: #f2c14e;
  font-size: 22rpx;
  line-height: 1.2;
  font-weight: 600;
}

.style2-work-growth,
.style2-work-forecast {
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-work-growth {
  color: rgba(245, 241, 232, 0.58);
  font-size: 18rpx;
  font-weight: 600;
}

.style2-work-forecast {
  color: rgba(242, 193, 78, 0.82);
  font-size: 15rpx;
  font-weight: 300;
}

.style2-resume-card {
  position: relative;
  min-height: 244rpx;
  margin-top: 18rpx;
  padding: 32rpx 34rpx 30rpx;
  border-radius: 13rpx;
  overflow: hidden;
}

.style2-resume-card:first-child {
  margin-top: 0;
}

.style2-resume-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-width: 0;
}

.style2-edit-entry {
  margin: 0 0 0 auto;
  padding: 0 18rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(215, 171, 69, 0.55);
  border-radius: 999rpx;
  background: rgba(215, 171, 69, 0.08);
  color: #d7ab45;
  font-size: 18rpx;
  line-height: 1;
  font-weight: 400;
}

.style2-edit-entry::after {
  border: 0;
}

.style2-resume-icon {
  width: 46rpx;
  height: 46rpx;
  flex: 0 0 46rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: rgba(207, 160, 46, 0.12);
}

.style2-resume-icon image {
  width: 30rpx;
  height: 30rpx;
  filter: sepia(1) saturate(2.1) hue-rotate(354deg) brightness(1.08);
}

.style2-resume-title {
  margin-left: 4rpx;
  color: #d7ab45;
  font-size: 25rpx;
  line-height: 1.12;
  font-weight: 400;
  letter-spacing: 0;
}

.style2-resume-body {
  position: relative;
  margin-top: 28rpx;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 178rpx;
  gap: 24rpx;
  align-items: end;
}

.style2-resume-timeline {
  position: relative;
  display: grid;
  gap: 12rpx;
  min-width: 0;
}

.style2-resume-line {
  position: absolute;
  left: 82rpx;
  top: 13rpx;
  bottom: 13rpx;
  width: 2rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, rgba(215, 171, 69, 0.96), rgba(215, 171, 69, 0.42));
  box-shadow: 0 0 10rpx rgba(215, 171, 69, 0.18);
}

.style2-resume-entry {
  position: relative;
  min-height: 40rpx;
  display: grid;
  grid-template-columns: 62rpx 26rpx minmax(0, 1fr);
  gap: 8rpx;
  align-items: start;
}

.style2-resume-year {
  color: #d7ab45;
  font-size: 22rpx;
  line-height: 1.22;
  font-weight: 300;
  letter-spacing: 0;
}

.style2-resume-dot {
  position: relative;
  z-index: 1;
  width: 18rpx;
  height: 18rpx;
  margin-top: 7rpx;
  border-radius: 50%;
  border: 3rpx solid #d7ab45;
  background: #17130a;
  box-shadow: 0 0 0 5rpx rgba(215, 171, 69, 0.11);
  box-sizing: border-box;
}

.style2-resume-text {
  min-width: 0;
  max-width: 100%;
}

.style2-resume-primary {
  color: rgba(245, 241, 232, 0.82);
  font-size: 17rpx;
  line-height: 1.18;
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-resume-secondary {
  margin-top: 4rpx;
  color: rgba(245, 241, 232, 0.5);
  font-size: 14rpx;
  line-height: 1.15;
  font-weight: 300;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-resume-media {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.style2-resume-media.single {
  display: flex;
  align-items: center;
  justify-content: center;
}

.style2-resume-media image {
  width: 100%;
  height: 92rpx;
  border-radius: 8rpx;
  opacity: 0.92;
}

.style2-resume-seal {
  position: absolute;
  right: 34rpx;
  top: 72rpx;
  width: 188rpx;
  height: 188rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.42;
  pointer-events: none;
}

.style2-resume-seal image {
  position: relative;
  z-index: 1;
  max-width: 100%;
  max-height: 100%;
  border-radius: 8rpx;
}

.style2-resume-seal.elite {
  opacity: 0.82;
  filter: drop-shadow(0 0 18rpx rgba(235, 190, 86, 0.28)) drop-shadow(0 0 36rpx rgba(215, 171, 69, 0.12));
}

.style2-resume-seal.elite::before {
  content: '';
  position: absolute;
  inset: 10rpx;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(242, 210, 118, 0.3), rgba(242, 210, 118, 0.02) 58%, transparent 70%);
  box-shadow: 0 0 34rpx rgba(242, 193, 78, 0.2);
}

.style2-resume-seal.shine::after {
  content: '';
  position: absolute;
  top: 18rpx;
  left: 16rpx;
  width: 78rpx;
  height: 150rpx;
  border-radius: 50%;
  background: linear-gradient(115deg, transparent 0%, rgba(255, 245, 181, 0.08) 42%, rgba(255, 245, 181, 0.38) 50%, rgba(255, 245, 181, 0.05) 58%, transparent 100%);
  transform: rotate(18deg);
  mix-blend-mode: screen;
}

.style2-resume-seal.strong {
  opacity: 0.64;
  filter: drop-shadow(0 0 14rpx rgba(189, 163, 110, 0.14));
}

.style2-resume-seal.ranked {
  opacity: 0.52;
  filter: grayscale(0.18) drop-shadow(0 0 10rpx rgba(159, 152, 126, 0.12));
}

.style2-resume-seal.normal {
  opacity: 0.42;
  filter: grayscale(0.36);
}

.style2-resume-card.has-seal .style2-resume-body {
  padding-right: 178rpx;
  display: block;
}

.style2-resume-card.has-media .style2-resume-body {
  grid-template-columns: minmax(0, 1fr);
}

.style2-resume-card.has-media .style2-resume-media {
  margin-left: 118rpx;
  margin-top: 22rpx;
}

.style2-circulation-stats {
  position: relative;
  padding: 32rpx 22rpx 28rpx;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  overflow: hidden;
  border-radius: 12rpx;
}

.style2-stat {
  position: relative;
  min-width: 0;
  min-height: 146rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.style2-stat:not(:last-of-type)::after {
  content: '';
  position: absolute;
  top: 20rpx;
  right: 0;
  width: 1rpx;
  height: 82rpx;
  background: rgba(255, 255, 255, 0.055);
}

.style2-stat-icon {
  width: 44rpx;
  height: 44rpx;
  margin-bottom: 20rpx;
  background: #e9bb56;
  -webkit-mask: var(--stat-icon) center / contain no-repeat;
  mask: var(--stat-icon) center / contain no-repeat;
}

.style2-stat-label {
  color: rgba(245, 241, 232, 0.75);
  font-size: 22rpx;
  line-height: 1;
  font-weight: 600;
}

.style2-stat-value {
  margin-top: 15rpx;
  color: #e9bb56;
  font-weight: 400;
  line-height: 1;
}

.style2-stat-value text {
  font-size: 34rpx;
  letter-spacing: 0;
}

.style2-stat-value small {
  margin-left: 6rpx;
  color: rgba(245, 241, 232, 0.72);
  font-size: 22rpx;
  font-weight: 700;
}

.style2-stat-note {
  grid-column: 1 / -1;
  margin-top: 6rpx;
  color: rgba(245, 241, 232, 0.45);
  font-size: 17rpx;
  line-height: 1.3;
  text-align: center;
}

.style2-record-head {
  margin: 30rpx 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}

.style2-record-title-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 30rpx;
  line-height: 1;
  font-weight: 500;
}

.style2-record-title-wrap image {
  width: 28rpx;
  height: 28rpx;
  opacity: 0.62;
  filter: brightness(1.4);
}

.style2-record-sort {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: rgba(245, 241, 232, 0.55);
  font-size: 19rpx;
  font-weight: 400;
}

.style2-record-sort text {
  color: rgba(245, 241, 232, 0.7);
  font-size: 24rpx;
}

.style2-record-timeline {
  position: relative;
  display: grid;
  gap: 16rpx;
}

.style2-record-timeline::before {
  content: '';
  position: absolute;
  top: 44rpx;
  bottom: 44rpx;
  left: 98rpx;
  width: 2rpx;
  background: linear-gradient(180deg, rgba(214, 171, 55, 0.15), rgba(214, 171, 55, 0.72), rgba(214, 171, 55, 0.16));
}

.style2-record-row {
  position: relative;
  display: grid;
  grid-template-columns: 78rpx 24rpx minmax(0, 1fr);
  gap: 8rpx;
  align-items: center;
}

.style2-record-date {
  color: rgba(245, 241, 232, 0.64);
  font-size: 15rpx;
  line-height: 1;
  font-weight: 400;
  white-space: nowrap;
}

.style2-record-node {
  position: relative;
  z-index: 1;
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  border: 4rpx solid #d7a51d;
  background: #15120a;
  box-shadow: 0 0 0 5rpx rgba(215, 165, 29, 0.18);
}

.style2-record-card {
  min-width: 0;
  min-height: 142rpx;
  padding: 14rpx 16rpx 14rpx 14rpx;
  display: grid;
  grid-template-columns: 128rpx minmax(0, 1fr) 116rpx;
  gap: 16rpx;
  align-items: center;
  border-radius: 12rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.09);
  background:
    radial-gradient(circle at 88% 16%, rgba(215, 171, 69, 0.1), transparent 32%),
    linear-gradient(135deg, rgba(35, 35, 35, 0.97), rgba(13, 13, 13, 0.99));
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.04);
}

.style2-record-cover {
  width: 128rpx;
  height: 104rpx;
  border-radius: 8rpx;
}

.style2-record-main {
  min-width: 0;
}

.style2-record-name {
  color: #fff;
  font-size: 22rpx;
  line-height: 1.16;
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-record-cert {
  margin-top: 10rpx;
  color: rgba(245, 241, 232, 0.42);
  font-size: 16rpx;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style2-record-line {
  margin-top: 9rpx;
  display: flex;
  align-items: center;
  gap: 6rpx;
  color: rgba(245, 241, 232, 0.58);
  font-size: 18rpx;
  line-height: 1;
  font-weight: 300;
}

.style2-record-line image {
  width: 18rpx;
  height: 18rpx;
  filter: sepia(1) saturate(2.2) hue-rotate(354deg) brightness(1.05);
}

.style2-record-side {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
}

.style2-record-badge {
  max-width: 112rpx;
  padding: 7rpx 10rpx;
  border-radius: 6rpx;
  font-size: 16rpx;
  line-height: 1;
  font-weight: 400;
  white-space: nowrap;
}

.style2-record-badge.green {
  color: #83c77a;
  background: rgba(46, 124, 49, 0.26);
}

.style2-record-badge.blue {
  color: #60b9ff;
  background: rgba(31, 102, 170, 0.28);
}

.style2-record-count {
  color: #d7ab45;
  font-size: 19rpx;
  line-height: 1;
  font-weight: 400;
  white-space: nowrap;
}

.style2-trust-title {
  margin: 34rpx 0 22rpx;
  color: #fff;
  font-size: 32rpx;
  line-height: 1;
  font-weight: 700;
}

.style2-trust {
  margin-top: 0;
  padding: 0;
  border: 0;
  background: transparent !important;
}

.style2-trust .trust-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
}

.style2-trust .trust-item {
  min-height: 152rpx;
  padding: 22rpx 18rpx;
  display: grid;
  grid-template-columns: 50rpx minmax(0, 1fr);
  gap: 14rpx;
  align-items: flex-start;
  border-radius: 10rpx;
  border: 1rpx solid rgba(215, 165, 29, 0.28);
  background:
    radial-gradient(circle at 88% 8%, rgba(229, 181, 62, 0.13), transparent 38%),
    linear-gradient(145deg, rgba(37, 34, 27, 0.96), rgba(18, 17, 15, 0.99));
  box-sizing: border-box;
  box-shadow:
    inset 0 1rpx 0 rgba(255, 255, 255, 0.035),
    0 10rpx 24rpx rgba(0, 0, 0, 0.22);
}

.style2-trust .trust-icon {
  width: 48rpx;
  height: 48rpx;
  margin-top: 2rpx;
  filter: sepia(1) saturate(2.3) hue-rotate(354deg) brightness(1.08);
}

.style2-trust .trust-item > view:last-child {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.style2-trust .trust-title {
  width: 116rpx;
  height: 30rpx;
  display: flex;
  align-items: center;
  color: #bfbfbf;
  font-size: 19rpx;
  line-height: 30rpx;
  font-weight: 400;
  white-space: nowrap;
}

.style2-trust .trust-desc {
  margin-top: 8rpx;
  color: rgba(245, 241, 232, 0.58);
  font-size: 9px;
  line-height: 1.32;
  font-weight: 300;
}

.style-2 .trust-section {
  box-shadow: none;
}

.style2-bottom-cta {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 45;
  min-height: calc(112rpx + env(safe-area-inset-bottom));
  padding: 16rpx 30rpx calc(16rpx + env(safe-area-inset-bottom));
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  background: rgba(10, 10, 10, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18rpx);
  box-sizing: border-box;
}

.style2-reserve-btn,
.style2-consult-btn {
  width: 100%;
  height: 82rpx;
  margin: 0;
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: 10rpx;
  font-size: 34rpx;
  font-weight: 400;
  box-sizing: border-box;
}

.style2-reserve-btn {
  color: #f2c14e;
  border: 1rpx solid rgba(242, 193, 78, 0.64);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.015));
}

.style2-consult-btn {
  color: #171100;
  border: 0;
  background: linear-gradient(180deg, #f6d56e, #d9a935);
}

.style2-reserve-btn image,
.style2-consult-btn image {
  width: 34rpx;
  height: 34rpx;
  flex: 0 0 auto;
}

.style2-consult-btn image {
  filter: brightness(0) saturate(100%);
}

.style-2 .home-indicator {
  display: none;
}

/* Style 2 final visual lock: keep the whole template in the black/gold art-home direction. */
.artist-home-page.style-2 {
  color: #f6f1e6;
  background:
    radial-gradient(circle at 50% 10%, rgba(213, 164, 48, 0.055), transparent 28%),
    linear-gradient(180deg, #090909 0%, #050505 100%) !important;
}

.style-2 .style2-bottom-cta {
  background: rgba(8, 8, 8, 0.98) !important;
}

.style-2 .top-nav {
  background: transparent !important;
  box-shadow: none !important;
  border: 0 !important;
}

.style-2 .profile-hero,
.style2-featured,
.style2-work-card,
.style2-resume-card,
.style2-circulation-stats,
.style2-record,
.style2-reserve-btn {
  background-color: #111 !important;
}

.style-2 .profile-hero {
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  background: #0d0d0d !important;
}

.style-2 .cover-image {
  inset: 0 !important;
  width: 100% !important;
  height: 100% !important;
  filter: saturate(0.92) contrast(1.02) brightness(0.72);
}

.style-2 .cover-shade {
  inset: 0 !important;
  width: 100% !important;
  height: 100% !important;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.08) 0%, rgba(0, 0, 0, 0.28) 42%, rgba(0, 0, 0, 0.72) 74%, rgba(0, 0, 0, 0.96) 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.5) 0%, rgba(0, 0, 0, 0.05) 58%, rgba(0, 0, 0, 0.3) 100%) !important;
}

.style-2 .nav-icon image {
  filter: none;
}

.style-2 .hero-actions,
.style2-tabs,
.style2-panel,
.style2-gallery {
  background: transparent !important;
}

.style2-featured,
.style2-work-card,
.style2-resume-card,
.style2-circulation-stats,
.style2-record {
  border-color: rgba(255, 255, 255, 0.1) !important;
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.04);
}

.style2-featured,
.style2-work-card {
  background:
    linear-gradient(145deg, rgba(31, 31, 31, 0.96), rgba(12, 12, 12, 0.98)) !important;
}

.style2-featured {
  height: 336rpx !important;
  display: block !important;
  border-radius: 10rpx !important;
  background: linear-gradient(135deg, #23211d 0%, #151412 100%) !important;
}

.style2-featured::after {
  background:
    linear-gradient(90deg, rgba(22, 20, 17, 0.74) 0%, rgba(28, 25, 20, 0.96) 28%, rgba(18, 17, 15, 0.99) 100%),
    radial-gradient(circle at 100% 0%, rgba(228, 185, 72, 0.14), transparent 40%) !important;
}

.style2-resume-card,
.style2-circulation-stats,
.style2-record {
  background:
    radial-gradient(circle at 82% 18%, rgba(215, 171, 69, 0.07), transparent 34%),
    linear-gradient(145deg, rgba(27, 27, 27, 0.97), rgba(8, 8, 8, 0.99)) !important;
}

.style2-resume-card {
  border-color: rgba(255, 255, 255, 0.075) !important;
  background:
    radial-gradient(circle at 86% 24%, rgba(215, 171, 69, 0.09), transparent 30%),
    linear-gradient(145deg, rgba(28, 28, 28, 0.98), rgba(9, 9, 9, 0.99)) !important;
  box-shadow:
    inset 0 1rpx 0 rgba(255, 255, 255, 0.035),
    0 14rpx 28rpx rgba(0, 0, 0, 0.24) !important;
}

.style2-work-image {
  filter: saturate(1.02) contrast(1.02);
}

.style2-featured-price,
.style2-work-price,
.style2-resume-title,
.style2-stat text,
.style2-record-price,
.style2-record-meta,
.style2-hero-pill strong {
  color: #f2c14e !important;
}

.style2-tab.active::after {
  background: #f2c14e !important;
}

@media (max-width: 420px) {
  .bottom-actions {
    grid-template-columns: 1fr;
  }
}

.tabbar {
  display: none;
}

.home-indicator {
  width: 326rpx;
  height: 9rpx;
  margin: 36rpx auto 0;
  border-radius: 999rpx;
  background: #fff;
}
</style>
