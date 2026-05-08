<template>
  <view class="detail-page">
    <view class="nav-bar" :class="{ 'is-transparent': pageScrolled }">
      <view class="nav-icon" @click="goBack">‹</view>
      <view class="nav-title">作品详情</view>
      <view class="nav-icon share" @click="onShare">
        <view class="share-mark"></view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="isEmpty">
      <image class="empty-icon" src="/static/images/artwork-fallback.png" mode="aspectFit" />
      <text class="empty-text">作品不存在或已下架</text>
      <button class="empty-btn" @click="goBack">返回</button>
    </view>

    <view class="detail-shell" v-if="!isEmpty">
      <view class="hero-card" :style="heroCardStyle">
        <swiper class="image-swiper" @change="onSwiperChange" v-if="images.length">
          <swiper-item class="hero-slide" v-for="(img, index) in images" :key="index">
            <image class="hero-backdrop" :src="img" mode="aspectFill"></image>
            <image class="hero-image" :src="img" mode="aspectFill" @load="onHeroImageLoad" @error="onArtworkImageError(index)" @click="previewImage(index)"></image>
          </swiper-item>
        </swiper>
        <view class="hero-shadow"></view>
        <view class="new-chip" :class="{ 'chip-hidden': pageScrolled }">☆ NEW</view>
        <view class="hero-copy">
          <text class="hero-serial">作品编号：{{ certificateCode }}</text>
        </view>
        <view class="hero-like" @click="onFavorite">
          <text class="heart">{{ detail.isFavorite ? '♥' : '♡' }}</text>
          <text>{{ displayLikeCount }}</text>
        </view>
        <view class="video-btn" v-if="detail.videoUrl" @click="playVideo">观看视频</view>
      </view>

      <view class="card market-card">
        <view class="market-heading">
          <view>
            <view class="work-title-row">
              <text class="work-title">{{ workName }}</text>
              <view class="tag-row">
                <text class="gold-tag">原创作品</text>
                <text class="gold-tag">唯一原作</text>
                <text class="gold-tag">可流通</text>
                <text class="gold-tag">平台托管</text>
              </view>
            </view>
            <text class="work-artist">{{ detail.authorName || '孟儒' }}</text>
            <text class="work-meta">{{ artworkMetaLine }}</text>
          </view>
        </view>
        <view class="market-content">
          <view class="price-block">
            <view class="label-line">
              <text>当前收藏价</text>
              <text class="question">?</text>
            </view>
            <view class="price">
              <text class="price-symbol">¥</text>
              <text>{{ priceNumber }}</text>
            </view>
            <view class="rise-line">↗ 预计上涨 {{ growthRangeDisplay }}</view>
            <view class="collect-line">♙ 已被 {{ displayLikeCount }} 位藏家收藏</view>
          </view>
          <view class="model-panel">
            <view class="model-title">♙ 涨跌趋势</view>
            <text class="model-sub">预计上涨区间</text>
            <view class="model-body">
              <view class="model-copy">
                <text class="model-price">{{ growthRangeDisplay }}</text>
                <text class="confidence">置信度 78%</text>
                <text class="factor-title">影响因素</text>
                <text class="factor">• 艺术家评级：高</text>
                <text class="factor">• 历史成交：稳定</text>
                <text class="factor">• 当前热度：上升</text>
              </view>
              <view class="confidence-ring">
                <view class="ring-inner">78%</view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="card artist-card" @click="goArtistHome">
        <image class="artist-avatar-lg" :src="authorAvatarSrc" @error="onAuthorAvatarError"></image>
        <view class="artist-info-block">
          <view class="artist-name-row">
            <text class="artist-name">{{ detail.authorName || '孟儒' }}</text>
            <text class="score-badge">B+</text>
            <text class="score-text">艺术家评级</text>
          </view>
          <text class="artist-subtitle">{{ authorSubtitle }}</text>
          <view class="artist-stats">
            <view class="artist-stat" v-for="item in artistStats" :key="item.label">
              <text class="artist-stat-value">{{ item.value }}</text>
              <text class="artist-stat-label">{{ item.label }}</text>
            </view>
          </view>
        </view>
        <view class="artist-link">进入艺术家主页 ›</view>
      </view>

      <view class="card record-card">
        <view class="section-top">
          <view class="section-title">
            <text class="section-icon">▤</text>
            <text>流通记录</text>
          </view>
          <view class="more-link">查看完整记录 ›</view>
        </view>
        <view class="record-body">
          <view class="record-list">
            <view class="record-item" v-for="item in circulationRows" :key="item.date">
              <view class="record-dot"></view>
              <text class="record-date">{{ item.date }}</text>
              <text class="record-event">{{ item.event }}</text>
              <text class="record-price" :class="{ current: item.current }">{{ item.price }}</text>
            </view>
          </view>
          <view class="gain-card">
            <view class="gain-copy">
              <text class="gain-label">累计上涨</text>
              <text class="gain-value">+54%</text>
            </view>
            <view class="sparkline">
              <view class="spark-seg one"></view>
              <view class="spark-seg two"></view>
              <view class="spark-seg three"></view>
              <view class="spark-seg four"></view>
            </view>
          </view>
        </view>
      </view>

      <view class="card cert-card">
        <view class="section-top">
          <view class="section-title">
            <text class="section-icon">▣</text>
            <text>收藏证书</text>
          </view>
          <view class="more-link">查看证书 ›</view>
        </view>
        <view class="cert-body">
          <view class="cert-list">
            <view class="cert-item">
              <view class="cert-icon">▱</view>
              <view>
                <text class="cert-label">唯一编号</text>
                <text class="cert-value">{{ certificateCode }}</text>
              </view>
            </view>
            <view class="cert-item">
              <view class="cert-icon">♕</view>
              <view>
                <text class="cert-label">艺术家认证</text>
                <text class="cert-value">{{ detail.authorName || '孟儒' }} 亲笔签名</text>
              </view>
            </view>
            <view class="cert-item">
              <view class="cert-icon">✓</view>
              <view>
                <text class="cert-label">平台认证</text>
                <text class="cert-value">艺术平台存证</text>
              </view>
            </view>
          </view>
          <view class="cert-preview">
            <image class="cert-image" :src="images[0] || fallbackCover" mode="aspectFill"></image>
          </view>
        </view>
      </view>

      <view class="card description-card">
        <view class="section-title">作品说明</view>
        <view class="story-text" :class="{ expanded: storyExpanded }">
          {{ storyText }}
        </view>
        <view class="expand-link" v-if="storyCanExpand" @click="storyExpanded = !storyExpanded">
          {{ storyExpanded ? '收起' : '展开' }}⌄
        </view>
      </view>

      <view class="commission-tip" v-if="commission > 0" @click="showShareModal">
        <text>分享推广可获得佣金</text>
        <text>{{ formatYuanAmount(commission) }}</text>
      </view>
    </view>

    <view class="bottom-bar safe-area-bottom" v-if="!isEmpty">
      <button class="bar-action" @click="onFavorite">
        <text class="bar-icon">{{ detail.isFavorite ? '♥' : '♡' }}</text>
        <text>{{ detail.isFavorite ? '已收藏' : '收藏' }}</text>
      </button>
      <button class="bar-action consult-action" @click="contactArtist">
        <view class="chat-mark"></view>
        <text>咨询</text>
      </button>
      <button class="collect-btn" @click="onFavorite">立即收藏</button>
    </view>

    <view class="share-modal" v-if="showSharePanel" @click="showSharePanel = false">
      <view class="share-content" @click.stop>
        <view class="share-title">分享到</view>
        <view class="commission-levels" v-if="commissionLevels.length">
          <view class="commission-level-title">艺荐官佣金预估</view>
          <view class="commission-level-row" v-for="level in commissionLevels" :key="level.name">
            <text>{{ level.name }}</text>
            <text>{{ level.amountText }}</text>
          </view>
        </view>
        <view class="share-icons">
          <view class="share-icon-item" @click="shareToFriend">
            <view class="share-icon">微</view>
            <text>微信好友</text>
          </view>
          <view class="share-icon-item" @click="shareToTimeline">
            <view class="share-icon">圈</view>
            <text>朋友圈</text>
          </view>
          <view class="share-icon-item" @click="copyLink">
            <view class="share-icon">链</view>
            <text>复制链接</text>
          </view>
        </view>
        <view class="share-close" @click="showSharePanel = false">取消</view>
      </view>
    </view>

    <view class="contact-modal" v-if="showContactModal" @click="showContactModal = false">
      <view class="contact-content" @click.stop>
        <view class="contact-header">
          <text class="contact-title">联系顾问</text>
          <view class="contact-close" @click="showContactModal = false">×</view>
        </view>
        <view class="contact-artist-info">
          <image class="artist-avatar" :src="authorAvatarSrc" @error="onAuthorAvatarError"></image>
          <text class="artist-name">{{ detail.authorName || '艺术家' }}</text>
        </view>
        <view class="contact-actions">
          <view class="contact-item" @click="sendMessage">
            <text class="contact-icon">讯</text>
            <text>发送消息</text>
          </view>
          <view class="contact-item" @click="makePhoneCall">
            <text class="contact-icon">电</text>
            <text>拨打电话</text>
          </view>
        </view>
        <view class="contact-phone" v-if="detail.authorPhone">
          <text>顾问电话</text>
          <text>{{ detail.authorPhone }}</text>
        </view>
      </view>
    </view>
  </view><!-- /.detail-page -->
</template>

<script>
import { getProductDetail, addFavorite, removeFavorite } from '@/api/product'
import { useUserStore } from '@/store/modules/user'
import { getProductCommission } from '@/api/promoter'
import { triggerCollectIncrease } from '@/api/artworkPrice'

const FALLBACK_COVER = '/static/images/artwork-fallback.png'

export default {
  data() {
    return {
      detail: {
        images: null,
        videoUrl: null,
        cover: null,
        coverImage: null,
        title: '',
        authorName: '',
        price: 0
      },
      images: [],
      currentImageIndex: 0,
      heroHeight: 442,
      pageScrolled: false,
      storyExpanded: false,
      commission: 0,
      commissionLevels: [],
      defaultAvatar: '/static/images/artist-avatar.png',
      showSharePanel: false,
      showContactModal: false,
      isEmpty: false,
      priceGrowth: {
        growthRate: '+0%',
        collectCount: 0,
        nextCondition: '收藏人数增加后可能涨价'
      }
    }
  },

  computed: {
    storyText() {
      return this.detail.description || '这件作品以沉稳的画面关系承载日常物象的温度，厚重笔触与层次色彩形成清晰的视觉节奏，呈现出兼具观赏性与收藏感的当代油画气质。'
    },
    storyCanExpand() {
      return this.storyText && this.storyText.length > 86
    },
    authorAvatarSrc() {
      return this.normalizeResourceUrl(this.detail.authorAvatar) || this.defaultAvatar
    },
    displayLikeCount() {
      return this.detail.displayLikeCount || this.detail.likeCount || this.detail.favoriteCount || 128
    },
    artworkYear() {
      return this.detail.year || this.detail.createYear || '2024'
    },
    artworkMaterial() {
      const raw = this.detail.material || this.detail.medium || this.detail.artType || ''
      return this.extractMaterial(raw.replace(/分类[:：]?\s*/g, '')) || '布面油画'
    },
    artworkSize() {
      return this.detail.size || '40 × 40 cm'
    },
    artworkType() {
      if (this.detail.ownershipTypeText) return this.detail.ownershipTypeText
      if (Number(this.detail.ownershipType) === 2) return '收藏'
      return '原创'
    },
    subjectText() {
      return this.cleanArtworkLabel(this.detail.subject || this.detail.categoryName || this.detail.artType || '').replace(/分类[:：]?\s*/g, '') || '静物'
    },
    authorSubtitle() {
      return this.detail.authorSubtitle || this.detail.authorBadge || '青年油画艺术家 · 杭州'
    },
    fallbackCover() {
      return FALLBACK_COVER
    },
    workName() {
      return this.detail.title || '静物0751'
    },
    heroCardStyle() {
      return `height: ${this.heroHeight}rpx`
    },
    artworkMetaLine() {
      return `${this.artworkMaterial} · ${this.artworkSize} · ${this.artworkYear}`
    },
    priceNumber() {
      const price = Number(this.detail.price || 804000)
      return Math.round(price / 100).toLocaleString()
    },
    growthRangeDisplay() {
      return (this.tomorrowIncreaseRange || '¥1.6 - ¥2.4').replace(/\s*-\s*/g, ' - ')
    },
    certificateCode() {
      const raw = this.detail.uid || this.detail.artworkUid || this.detail.code || this.detail.artworkCode
      if (raw) return raw
      const year = String(this.artworkYear || '2024').replace(/[^\d]/g, '') || '2024'
      return `AW${year}-0751`
    },
    artistStats() {
      return [
        { label: '作品数', value: this.detail.authorWorkCount || 12 },
        { label: '成交数', value: this.detail.authorDealCount || 36 },
        { label: '成交率', value: this.detail.authorDealRate || '85%' },
        { label: '平均涨幅', value: this.detail.authorAverageRise || '+32%' }
      ]
    },
    circulationRows() {
      return [
        { date: '2024.03.12', event: '首次收藏', price: '¥5,200' },
        { date: '2024.06.18', event: '第二次流通', price: '¥6,800' },
        { date: '2024.10.28', event: '第三次流通（当前）', price: this.formatPrice(this.detail.price || 804000), current: true }
      ]
    },
    tomorrowIncreaseRange() {
      const min = Number(this.detail.tomorrowIncreaseMin || 0)
      const max = Number(this.detail.tomorrowIncreaseMax || 0)
      if (min > 0 || max > 0) {
        const low = Math.min(min || max, max || min)
        const high = Math.max(min, max)
        return low === high ? this.formatPrice(low) : `${this.formatPrice(low)} - ${this.formatPrice(high)}`
      }
      const price = Number(this.detail.price || 0)
      const baseRate = Number(this.detail.customBaseDailyRate || this.detail.baseDailyRate || 0)
      const matureRate = Number(this.detail.customMatureDailyRate || this.detail.matureDailyRate || baseRate)
      if (!price || (!baseRate && !matureRate)) return ''
      const low = Math.round(price * Math.min(baseRate || matureRate, matureRate || baseRate))
      const high = Math.round(price * Math.max(baseRate, matureRate))
      return low === high ? this.formatPrice(low) : `${this.formatPrice(low)} - ${this.formatPrice(high)}`
    },
    infoRows() {
      return [
        { label: '创作年份', value: this.artworkYear },
        { label: '作品材质', value: this.artworkMaterial },
        { label: '作品尺寸', value: this.artworkSize },
        { label: '作品类型', value: this.artworkType },
        { label: '题材', value: this.subjectText },
        { label: '作者', value: this.detail.authorName || '孟儒' },
        { label: '签名', value: this.detail.signature || '画背签名' },
        { label: '保存状态', value: this.detail.condition || '完好' }
      ]
    },
    benefits() {
      return [
        { title: '专属收藏证书', desc: '平台联合艺术家出具' },
        { title: '优先展览机会', desc: '受邀参与线下艺术展览' },
        { title: '增值潜力保障', desc: '专业团队长期价值跟踪' },
        { title: '流通服务支持', desc: '平台提供专业流通服务' }
      ]
    },
    circulationSteps() {
      const year = String(this.artworkYear || '2024').replace('年', '')
      return [
        { title: '创作完成', date: `${year}.06` },
        { title: '首次上架', date: `${year}.07` },
        { title: '当前在售', date: `${year}.07` },
        { title: '--', date: '--' }
      ]
    },
    relatedWorks() {
      const cover = this.images[0] || FALLBACK_COVER
      return [
        {
          id: this.detail.id,
          cover,
          title: this.detail.title ? `${this.detail.title}-已修复` : '测试作品-已修复',
          meta: `${this.artworkMaterial} · ${this.artworkSize} · ${this.artworkYear}`,
          price: this.formatPrice(this.detail.price || 1200000)
        },
        {
          id: this.detail.id,
          cover: this.images[1] || cover,
          title: '小女孩',
          meta: `${this.artworkMaterial} · 100 × 80 cm · ${this.artworkYear}`,
          price: '¥12,000'
        }
      ]
    }
  },

  onLoad(options = {}) {
    this.fetchDetail(options)
  },

  onPageScroll(e) {
    this.pageScrolled = Number(e?.scrollTop || 0) > 12
  },

  methods: {
    async fetchDetail(routeOptions = {}) {
      const pages = getCurrentPages()
      const currentPage = pages[pages.length - 1]
      const id = routeOptions.id || currentPage.options?.id || this.readRouteIdFromLocation()

      if (!id) {
        this.isEmpty = true
        return
      }

      try {
        const data = await getProductDetail(id)
        if (data) {
          this.detail = data
          this.initPriceGrowth(data)
          this.currentImageIndex = 0
          this.heroHeight = 442

          this.images = this.buildArtworkImages(data)
          if (!this.images.length) this.images = [FALLBACK_COVER]
          this.loadCommission(id)
          this.saveBrowseHistory(data)
        } else {
          this.isEmpty = true
          this.loadCommission(id)
        }
      } catch (e) {
        console.error('获取详情失败', e)
        this.isEmpty = true
        this.loadCommission(id)
      }
    },

    saveBrowseHistory(item) {
      if (!item || !item.id) return
      const record = {
        id: item.id,
        name: item.title || item.name || '未命名作品',
        author: item.authorName || item.artistName || '未知艺术家',
        price: item.price || 0,
        image: item.coverImage || item.cover || (Array.isArray(item.images) ? item.images[0] : ''),
        time: Date.now()
      }
      const history = uni.getStorageSync('browseHistoryWorks') || []
      const next = [record, ...history.filter(v => v.id !== record.id)].slice(0, 50)
      uni.setStorageSync('browseHistoryWorks', next)

      if (item.authorId || item.authorUid || item.authorName) {
        const artistRecord = {
          id: item.authorId || item.authorUid,
          name: item.authorName || item.artistName || '未知艺术家',
          avatar: item.authorAvatar || this.defaultAvatar,
          tags: [item.artType || item.category || '艺术家'].filter(Boolean),
          intro: item.authorBio || '',
          isFollowing: !!item.isFollowing,
          time: Date.now()
        }
        const artists = uni.getStorageSync('browseHistoryArtists') || []
        uni.setStorageSync('browseHistoryArtists', [artistRecord, ...artists.filter(v => v.id !== artistRecord.id)].slice(0, 50))
      }
    },

    readRouteIdFromLocation() {
      if (typeof window === 'undefined') return ''
      const match = window.location.href.match(/[?&]id=([^&#]+)/)
      return match ? decodeURIComponent(match[1]) : ''
    },

    initPriceGrowth(data) {
      if (!data) return
      const rise = Number(data.priceRise || data.dailyIncreaseRate || 0)
      this.priceGrowth = {
        growthRate: rise > 0 ? `+${(rise * 100).toFixed(1)}%` : '+0%',
        collectCount: data.collectCount || data.favoriteCount || 0,
        nextCondition: '每新增10位藏家收藏，作品价格可能上涨0.5%'
      }
    },

    async loadCommission(productId) {
      try {
        const res = await getProductCommission(productId)
        const rate = res.commissionRate || res.rate || this.detail.commissionRate || 5
        const priceYuan = (this.detail.price || 0) / 100
        this.commission = Math.floor(priceYuan * rate) / 100
        this.commissionLevels = this.buildCommissionLevels(res, rate)
      } catch (e) {
        const rate = this.detail.commissionRate || 5
        const priceYuan = (this.detail.price || 0) / 100
        this.commission = Math.floor(priceYuan * rate) / 100
        this.commissionLevels = this.buildCommissionLevels(null, rate)
      }
    },

    goBack() {
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.reLaunch({ url: '/pages/index/index' })
      }
    },

    onSwiperChange(e) {
      this.currentImageIndex = e.detail.current
    },

    onHeroImageLoad(e) {
      const width = Number(e.detail?.width || 0)
      const height = Number(e.detail?.height || 0)
      if (!width || !height) return
      const containerWidth = 704
      this.heroHeight = Math.round(containerWidth * (height / width))
    },

    previewImage(index) {
      uni.previewImage({
        current: index,
        urls: this.images
      })
    },

    playVideo() {
      if (this.detail.videoUrl) {
        uni.navigateTo({
          url: `/pages/common/video?url=${encodeURIComponent(this.detail.videoUrl)}`
        })
      }
    },

    async onFavorite() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }

      try {
        if (this.detail.isFavorite) {
          await removeFavorite(this.detail.id)
          this.detail.isFavorite = false
          this.bumpLikeCount(-1)
        } else {
          await addFavorite(this.detail.id)
          this.detail.isFavorite = true
          this.bumpLikeCount(1)
          this.triggerPriceOnCollect()
        }
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },

    async triggerPriceOnCollect() {
      try {
        const newPrice = await triggerCollectIncrease(this.detail.id)
        const oldPrice = this.detail.price
        const changeRate = newPrice && oldPrice ? ((newPrice - oldPrice) / oldPrice * 100) : 0.5
        this.detail.price = newPrice || this.detail.price
        this.priceGrowth.collectCount = (this.priceGrowth.collectCount || 0) + 1
        this.priceGrowth.growthRate = `+${changeRate.toFixed(1)}%`
        uni.showToast({ title: '收藏成功，作品热度提升', icon: 'none' })
      } catch (e) {
        console.warn('收藏触发涨价失败', e)
      }
    },

    onShare() {
      this.showSharePanel = true
    },

    shareToFriend() {
      uni.share({
        provider: 'weixin',
        scene: 'WXSceneSession',
        title: this.detail.title,
        imageUrl: this.images[0],
        query: `id=${this.detail.id}&from=share`,
        success: () => {
          uni.showToast({ title: '分享成功', icon: 'success' })
          this.showSharePanel = false
        },
        fail: () => {
          uni.showToast({ title: '分享失败', icon: 'none' })
        }
      })
    },

    shareToTimeline() {
      uni.share({
        provider: 'weixin',
        scene: 'WXSenceTimeline',
        title: this.detail.title,
        imageUrl: this.images[0],
        query: `id=${this.detail.id}&from=share`,
        success: () => {
          uni.showToast({ title: '分享成功', icon: 'success' })
          this.showSharePanel = false
        },
        fail: () => {
          uni.showToast({ title: '分享失败', icon: 'none' })
        }
      })
    },

    copyLink() {
      const app = getApp()
      const link = `${app?.globalData?.domain || ''}/pages/gallery/detail?id=${this.detail.id}&from=share`
      uni.setClipboardData({
        data: link,
        success: () => {
          uni.showToast({ title: '链接已复制', icon: 'success' })
          this.showSharePanel = false
        }
      })
    },

    showShareModal() {
      this.showSharePanel = true
    },

    contactArtist() {
      const userStore = useUserStore()
      if (!userStore.isLogin) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      this.showContactModal = true
    },

    sendMessage() {
      this.showContactModal = false
      uni.navigateTo({
        url: `/pages/message/chat?userId=${this.detail.authorId || ''}`
      })
    },

    makePhoneCall() {
      if (this.detail.authorPhone) {
        uni.makePhoneCall({
          phoneNumber: this.detail.authorPhone
        })
      } else {
        uni.showToast({ title: '暂无电话号码', icon: 'none' })
      }
    },

    goArtistHome() {
      const authorId = this.detail.authorId || this.detail.authorUid || ''
      uni.navigateTo({
        url: `/pages/artist/home?userId=${authorId}`
      })
    },

    goRelatedWork(id) {
      if (!id || id === this.detail.id) return
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },

    buildArtworkImages(data) {
      const candidates = [
        ...this.extractImageList(data.images),
        data.cover,
        data.coverImage
      ]

      const seen = new Set()
      return candidates
        .map(this.normalizeResourceUrl)
        .filter(Boolean)
        .filter(url => {
          if (seen.has(url)) return false
          seen.add(url)
          return true
        })
    },

    extractImageList(value) {
      if (Array.isArray(value)) return value
      if (typeof value !== 'string') return []
      const text = value.trim()
      if (!text || text === '[]' || text === '{}') return []
      try {
        const parsed = JSON.parse(text)
        return Array.isArray(parsed) ? parsed : [text]
      } catch (e) {
        return [text]
      }
    },

    normalizeResourceUrl(url) {
      if (!url || typeof url !== 'string') return ''
      const text = url.trim()
      if (!text || text === '[]' || text === '{}' || text === 'null' || text === 'undefined') return ''
      if (text.startsWith('/')) return text
      if (text.startsWith('http://localhost:8087')) {
        const path = text.slice('http://localhost:8087'.length)
        return path.startsWith('/upload') ? path : `/upload${path}`
      }
      if (text.startsWith('http://127.0.0.1:8087')) {
        const path = text.slice('http://127.0.0.1:8087'.length)
        return path.startsWith('/upload') ? path : `/upload${path}`
      }
      const lanFileMatch = text.match(/^http:\/\/192\.168\.\d+\.\d+:8087(\/.*)$/)
      if (lanFileMatch) {
        return lanFileMatch[1].startsWith('/upload') ? lanFileMatch[1] : `/upload${lanFileMatch[1]}`
      }
      const app = getApp()
      const domain = app?.globalData?.fileDomain || app?.globalData?.domain || ''
      if (!text.startsWith('http')) {
        return domain ? `${domain}${text.startsWith('/') ? '' : '/'}${text}` : text
      }
      return text
    },

    cleanArtworkLabel(value) {
      if (!value || typeof value !== 'string') return ''
      return value.replace(/分类[:：]?\s*/g, '').trim()
    },

    extractMaterial(value) {
      const label = this.cleanArtworkLabel(value)
      const match = label.match(/[（(]([^）)]+)[）)]/)
      return match ? match[1] : label
    },

    onArtworkImageError(index) {
      const next = [...this.images]
      next[index] = FALLBACK_COVER
      this.images = next
    },

    onAuthorAvatarError() {
      this.detail.authorAvatar = this.defaultAvatar
    },

    bumpLikeCount(step) {
      const nextFavorite = Math.max((this.detail.favoriteCount || 0) + step, 0)
      const nextDisplay = Math.max((this.detail.displayLikeCount || this.detail.likeCount || this.detail.favoriteCount || 0) + step, 0)
      this.detail.favoriteCount = nextFavorite
      this.detail.displayLikeCount = nextDisplay
      this.detail.likeCount = nextDisplay
    },

    buildCommissionLevels(res, rate) {
      const priceYuan = (this.detail.price || 0) / 100
      const levels = Array.isArray(res?.levels) && res.levels.length
        ? res.levels
        : [
            { name: '普通艺荐官', rate },
            { name: '高级艺荐官', rate: Number(rate) * 1.2 },
            { name: '合伙人艺荐官', rate: Number(rate) * 1.5 }
          ]

      return levels.map(level => {
        const levelRate = Number(level.rate || level.commissionRate || rate || 0)
        const amount = Number(level.amount || level.commission || (priceYuan * levelRate / 100))
        return {
          name: level.name || level.levelName || '艺荐官',
          amount,
          amountText: this.formatYuanAmount(amount)
        }
      })
    },

    formatPrice(price) {
      if (!price) return '¥0'
      const yuan = Math.round(Number(price) / 100)
      return `¥${yuan.toLocaleString()}`
    },

    formatYuanAmount(amount) {
      const value = Number(amount || 0)
      const yuan = Math.round(value)
      return `¥${yuan.toLocaleString()}`
    }
  }
}
</script>

<style lang="scss" scoped>
$page-bg: #050505;
$panel-bg: #171717;
$panel-bg-soft: #1e1e1e;
$line: rgba(255, 255, 255, 0.09);
$text-main: #f7f7f7;
$text-sub: #a7a7a7;
$text-dim: #707070;
$gold: #d5a91c;
$gold-bright: #f0c83a;

.detail-page {
  min-height: 100vh;
  padding: 118rpx 28rpx 164rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 18% 8%, rgba(213, 169, 28, 0.12), transparent 28%),
    linear-gradient(180deg, #111 0%, $page-bg 30%, $page-bg 100%);
  color: $text-main;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  height: 108rpx;
  padding: 22rpx 28rpx 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(5, 5, 5, 0.9);
  backdrop-filter: blur(18rpx);
}

.nav-title {
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.nav-icon {
  width: 62rpx;
  height: 62rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 54rpx;
  line-height: 1;
}

.nav-icon.share {
  font-size: 40rpx;
}

.hero-card {
  position: relative;
  height: 470rpx;
  overflow: hidden;
  border-radius: 10rpx;
  background: #101010;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24rpx 72rpx rgba(0, 0, 0, 0.45);
}

.image-swiper,
.hero-image {
  width: 100%;
  height: 100%;
}

.image-count,
.video-btn {
  position: absolute;
  right: 18rpx;
  bottom: 18rpx;
  padding: 8rpx 16rpx;
  border-radius: 24rpx;
  background: rgba(0, 0, 0, 0.56);
  color: #fff;
  font-size: 22rpx;
}

.video-btn {
  right: auto;
  left: 18rpx;
}

.summary-section {
  padding: 26rpx 18rpx 18rpx;
}

.work-title {
  margin-bottom: 22rpx;
  font-size: 42rpx;
  line-height: 1.18;
  font-weight: 800;
}

.author-row {
  display: flex;
  align-items: center;
  padding-bottom: 22rpx;
  border-bottom: 1rpx solid $line;
}

.author-avatar {
  width: 66rpx;
  height: 66rpx;
  margin-right: 14rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(213, 169, 28, 0.5);
  background: $panel-bg-soft;
}

.author-info {
  flex: 1;
  min-width: 0;
}

.author-name-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.author-name {
  font-size: 26rpx;
  font-weight: 700;
}

.verify-badge {
  width: 28rpx;
  height: 28rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: $gold;
  color: #111;
  font-size: 18rpx;
  font-weight: 800;
}

.author-subtitle {
  display: block;
  margin-top: 4rpx;
  color: $text-sub;
  font-size: 22rpx;
}

.like-counter {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: $text-sub;
  font-size: 24rpx;
}

.heart {
  font-size: 44rpx;
  color: $text-sub;
}

.meta-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 24rpx 0 20rpx;
}

.meta-item {
  min-width: 0;
  padding: 0 10rpx;
  text-align: center;
  border-right: 1rpx solid $line;
}

.meta-item:last-child {
  border-right: 0;
}

.meta-label {
  display: block;
  margin-bottom: 10rpx;
  color: $text-dim;
  font-size: 22rpx;
}

.meta-value {
  display: block;
  color: #ddd;
  font-size: 24rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.price-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20rpx;
}

.price {
  color: $gold-bright;
  font-size: 42rpx;
  font-weight: 800;
  line-height: 1;
}

.forecast {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 10rpx;
  color: $gold-bright;
  font-size: 24rpx;
}

.info-dot {
  width: 24rpx;
  height: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 2rpx solid $gold;
  font-size: 18rpx;
  font-weight: 700;
}

.badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12rpx;
  max-width: 340rpx;
}

.badge {
  min-width: 92rpx;
  height: 42rpx;
  padding: 0 14rpx;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx;
  border: 1rpx solid rgba(213, 169, 28, 0.85);
  color: $gold-bright;
  font-size: 22rpx;
}

.panel {
  position: relative;
  margin-top: 16rpx;
  padding: 22rpx;
  border-radius: 10rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(145deg, rgba(31, 31, 31, 0.96), rgba(18, 18, 18, 0.96));
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.04);
}

.panel-heading,
.panel-top {
  display: flex;
  align-items: center;
}

.panel-top {
  justify-content: space-between;
}

.panel-heading {
  gap: 10rpx;
  color: $text-main;
  font-size: 27rpx;
  font-weight: 800;
}

.panel-icon {
  color: $gold-bright;
  font-size: 28rpx;
}

.story-text {
  margin-top: 16rpx;
  padding-right: 32rpx;
  color: #bdbdbd;
  font-size: 25rpx;
  line-height: 1.58;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.story-text.expanded {
  display: block;
}

.panel-arrow {
  position: absolute;
  right: 22rpx;
  top: 72rpx;
  color: $text-sub;
  font-size: 54rpx;
}

.split-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
}

.info-panel,
.benefit-panel {
  min-height: 292rpx;
}

.info-row {
  display: grid;
  grid-template-columns: 96rpx 1fr;
  gap: 12rpx;
  margin-top: 12rpx;
  font-size: 23rpx;
  line-height: 1.2;
}

.row-label {
  color: $text-sub;
}

.row-value {
  color: #d2d2d2;
  overflow-wrap: anywhere;
}

.benefit-item {
  display: grid;
  grid-template-columns: 36rpx 1fr;
  gap: 10rpx;
  margin-top: 16rpx;
}

.benefit-mark {
  width: 32rpx;
  height: 32rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 2rpx solid $text-sub;
  color: $text-sub;
  font-size: 18rpx;
}

.benefit-title {
  color: #d4d4d4;
  font-size: 23rpx;
}

.benefit-desc {
  margin-top: 4rpx;
  color: $text-dim;
  font-size: 20rpx;
  line-height: 1.25;
}

.more-link {
  color: $text-sub;
  font-size: 24rpx;
}

.timeline {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 24rpx;
  padding: 0 8rpx;
}

.timeline-line {
  position: absolute;
  left: 58rpx;
  right: 58rpx;
  top: 11rpx;
  height: 2rpx;
  background: rgba(213, 169, 28, 0.4);
}

.timeline-item {
  position: relative;
  z-index: 1;
  color: $text-sub;
  font-size: 22rpx;
  text-align: center;
}

.timeline-dot {
  width: 16rpx;
  height: 16rpx;
  margin: 4rpx auto 16rpx;
  border-radius: 50%;
  background: $gold;
  box-shadow: 0 0 0 6rpx rgba(213, 169, 28, 0.12);
}

.timeline-title {
  color: #c8c8c8;
}

.timeline-date {
  margin-top: 4rpx;
  color: $text-sub;
}

.related-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 18rpx;
}

.related-card {
  position: relative;
  overflow: hidden;
  border-radius: 8rpx;
  background: #202020;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
}

.related-image {
  width: 100%;
  height: 150rpx;
  display: block;
}

.related-like {
  position: absolute;
  top: 14rpx;
  right: 14rpx;
  width: 42rpx;
  height: 42rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.48);
  color: #fff;
  font-size: 30rpx;
}

.related-body {
  padding: 12rpx;
}

.related-title {
  color: #fff;
  font-size: 23rpx;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-meta {
  margin-top: 6rpx;
  color: $text-sub;
  font-size: 19rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-price {
  margin-top: 6rpx;
  color: $gold-bright;
  font-size: 25rpx;
  font-weight: 800;
}

.commission-tip {
  margin-top: 16rpx;
  padding: 18rpx 22rpx;
  display: flex;
  justify-content: space-between;
  border-radius: 10rpx;
  border: 1rpx solid rgba(213, 169, 28, 0.35);
  background: rgba(213, 169, 28, 0.1);
  color: $gold-bright;
  font-size: 24rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 60;
  display: grid;
  grid-template-columns: 1fr 1.08fr;
  gap: 16rpx;
  padding: 18rpx 28rpx 24rpx;
  background: rgba(18, 18, 18, 0.95);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18rpx);
}

.advisor-btn,
.collect-btn {
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  border-radius: 8rpx;
  font-size: 26rpx;
  line-height: 1;
}

.advisor-btn {
  color: #e6e6e6;
  background: #191919;
  border: 1rpx solid rgba(255, 255, 255, 0.22);
}

.collect-btn {
  color: #211800;
  font-weight: 800;
  border: 0;
  background: linear-gradient(135deg, #f1ce4a 0%, #d5a91c 100%);
}

.share-modal,
.contact-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.62);
}

.share-content,
.contact-content {
  width: 100%;
  padding: 30rpx 30rpx 44rpx;
  box-sizing: border-box;
  border-radius: 24rpx 24rpx 0 0;
  background: #191919;
  color: $text-main;
}

.share-title,
.contact-title {
  font-size: 30rpx;
  font-weight: 800;
  text-align: center;
}

.commission-levels {
  margin: 24rpx 0;
  padding: 20rpx;
  border-radius: 12rpx;
  background: rgba(213, 169, 28, 0.1);
}

.commission-level-title {
  margin-bottom: 12rpx;
  color: $gold-bright;
  font-size: 24rpx;
}

.commission-level-row {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  color: #d6d6d6;
  font-size: 24rpx;
}

.share-icons {
  display: flex;
  justify-content: space-around;
  padding: 24rpx 0;
}

.share-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  color: $text-sub;
  font-size: 22rpx;
}

.share-icon {
  width: 78rpx;
  height: 78rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(213, 169, 28, 0.14);
  color: $gold-bright;
  font-size: 28rpx;
}

.share-close {
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10rpx;
  background: #252525;
  color: #d6d6d6;
  font-size: 28rpx;
}

.contact-header {
  position: relative;
}

.contact-close {
  position: absolute;
  right: 0;
  top: -8rpx;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-sub;
  font-size: 42rpx;
}

.contact-artist-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  margin: 26rpx 0;
}

.artist-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: #2a2a2a;
}

.artist-name {
  font-size: 26rpx;
  font-weight: 700;
}

.contact-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.contact-item {
  height: 94rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  border-radius: 10rpx;
  background: #242424;
  color: #e6e6e6;
  font-size: 25rpx;
}

.contact-icon {
  width: 36rpx;
  height: 36rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(213, 169, 28, 0.18);
  color: $gold-bright;
  font-size: 20rpx;
}

.contact-phone {
  margin-top: 18rpx;
  display: flex;
  justify-content: space-between;
  color: $text-sub;
  font-size: 24rpx;
}

.detail-page {
  min-height: 100vh;
  padding: 104rpx 0 168rpx;
  background:
    radial-gradient(circle at 50% -14%, rgba(224, 181, 67, 0.1), transparent 28%),
    linear-gradient(180deg, #090909 0%, #111 42%, #0a0a0a 100%);
  color: #f2f2f2;
}

.detail-shell {
  width: 100%;
  max-width: 860rpx;
  margin: 0 auto;
  padding: 0 26rpx;
  box-sizing: border-box;
}

.nav-bar {
  height: 104rpx;
  padding: 20rpx 34rpx 0;
  background: transparent;
  backdrop-filter: none;
  border-bottom: 0;
  transition: background 0.2s ease, backdrop-filter 0.2s ease;
}

.nav-bar.is-transparent {
  background: transparent;
  backdrop-filter: none;
}

.nav-bar.is-transparent .nav-title,
.nav-bar.is-transparent .nav-icon {
  opacity: 0;
}

.nav-title {
  font-size: 34rpx;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.88);
  transition: opacity 0.25s ease;
}

.nav-icon {
  width: 64rpx;
  height: 64rpx;
  font-size: 58rpx;
  color: rgba(255, 255, 255, 0.92);
  transition: opacity 0.25s ease;
}

.nav-icon.share {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #dfbd65;
}

.share-mark {
  position: relative;
  width: 38rpx;
  height: 40rpx;
  background: linear-gradient(#dfbd65, #dfbd65) center 4rpx / 3rpx 24rpx no-repeat;
}

.share-mark::before {
  content: '';
  position: absolute;
  left: 6rpx;
  right: 6rpx;
  bottom: 2rpx;
  height: 18rpx;
  border: 3rpx solid #dfbd65;
  border-top: 0;
  border-radius: 5rpx;
}

.share-mark::after {
  content: '';
  position: absolute;
  left: 11rpx;
  top: 2rpx;
  width: 15rpx;
  height: 15rpx;
  border-top: 3rpx solid #dfbd65;
  border-left: 3rpx solid #dfbd65;
  transform: rotate(45deg);
}

.hero-card {
  height: 442rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.12);
  background: #070707;
  box-shadow: none;
}

.image-swiper,
.hero-image {
  width: 100%;
  height: 100%;
}

.hero-slide {
  position: relative;
  overflow: hidden;
}

.hero-backdrop {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  filter: blur(18rpx);
  opacity: 0.36;
  transform: scale(1.08);
}

.hero-image {
  position: relative;
  z-index: 1;
}

.hero-shadow {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.04) 38%, rgba(0, 0, 0, 0.72) 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.28), transparent 50%, rgba(0, 0, 0, 0.2));
  pointer-events: none;
}

.new-chip {
  position: absolute;
  top: 22rpx;
  right: 18rpx;
  height: 48rpx;
  padding: 0 18rpx;
  display: flex;
  align-items: center;
  border: 1rpx solid rgba(221, 174, 64, 0.65);
  border-radius: 999rpx;
  background: rgba(18, 18, 18, 0.66);
  color: #f0c76d;
  font-size: 24rpx;
  font-weight: 400;
  opacity: 0.55;
  transition: opacity 0.25s ease;
}

.new-chip.chip-hidden {
  opacity: 0;
}

.hero-copy {
  position: absolute;
  left: 24rpx;
  bottom: 8rpx;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8rpx;
}

.hero-serial {
  font-size: 22rpx;
  line-height: 1;
  font-weight: 400;
  color: #999;
  letter-spacing: 1rpx;
  font-family: 'FangSong', '仿宋', 'FZFS', serif;
  white-space: nowrap;
  display: inline;
}

.hero-like {
  position: absolute;
  right: 28rpx;
  bottom: 32rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: rgba(255, 255, 255, 0.88);
  font-size: 26rpx;
  font-weight: 600;
}

.heart {
  color: rgba(255, 255, 255, 0.92);
  font-size: 42rpx;
  line-height: 1;
}

.card {
  margin-top: 18rpx;
  padding: 22rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background:
    radial-gradient(circle at 0% 0%, rgba(255, 255, 255, 0.035), transparent 34%),
    linear-gradient(135deg, rgba(33, 33, 33, 0.96), rgba(21, 21, 21, 0.98));
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.035);
}

.market-card {
  padding: 22rpx 22rpx 20rpx;
}

.market-heading {
  display: block;
}

.work-title,
.work-artist,
.work-meta {
  display: block;
}

.work-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.work-title {
  color: #f5f5f5;
  font-size: 30rpx;
  line-height: 1.18;
  font-weight: 800;
  flex-shrink: 0;
}

.work-artist {
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.58);
  font-size: 23rpx;
}

.work-meta {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.5);
  font-size: 22rpx;
  line-height: 1.3;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6rpx;
  min-width: 0;
}

.gold-tag {
  height: 28rpx;
  padding: 0 9rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(211, 159, 45, 0.7);
  width: 105rpx;
  border-radius: 999rpx;
  background: rgba(211, 159, 45, 0.06);
  color: #d8aa45;
  font-size: 16rpx;
  line-height: 1;
  font-weight: 500;
}

.market-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 352rpx;
  gap: 14rpx;
  margin-top: -78rpx;
}

.price-block {
  min-height: 216rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.label-line {
  display: flex;
  align-items: center;
  gap: 10rpx;
  color: rgba(255, 255, 255, 0.68);
  font-size: 20rpx;
  line-height: 1.45;
  font-weight: 700;
}

.question {
  width: 24rpx;
  height: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1rpx solid rgba(220, 177, 78, 0.8);
  color: #d8aa45;
  font-size: 18rpx;
}

.price {
  margin-top: 14rpx;
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  color: #f0c65d;
  font-size: 54rpx;
  line-height: 1;
  font-weight: 800;
  text-shadow: 0 0 34rpx rgba(224, 181, 67, 0.18);
}

.price-symbol {
  font-size: 34rpx;
}

.rise-line {
  margin-top: 20rpx;
  color: #d7ad4d;
  font-size: 21rpx;
  font-weight: 700;
}

.collect-line {
  margin-top: 18rpx;
  color: rgba(255, 255, 255, 0.6);
  font-size: 19rpx;
  line-height: 1.55;
}

.model-panel {
  padding: 18rpx 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.07);
  border-radius: 14rpx;
  background: rgba(13, 13, 13, 0.34);
  align-self: start;
}

.model-title {
  color: #efefef;
  font-size: 24rpx;
  font-weight: 800;
}

.model-sub {
  display: block;
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.36);
  font-size: 17rpx;
  line-height: 1.45;
}

.model-body {
  display: grid;
  grid-template-columns: 1fr 94rpx;
  gap: 12rpx;
  margin-top: 8rpx;
  align-items: center;
}

.model-copy {
  display: flex;
  flex-direction: column;
}

.model-price {
  color: #f0c65d;
  font-size: 28rpx;
  line-height: 1.2;
  font-weight: 800;
}

.confidence,
.factor-title,
.factor {
  display: block;
  font-size: 17rpx;
  line-height: 1.55;
}

.confidence {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.56);
}

.factor-title {
  margin-top: 14rpx;
  color: rgba(255, 255, 255, 0.42);
}

.factor {
  margin-top: 4rpx;
  color: rgba(255, 255, 255, 0.58);
}

.confidence-ring {
  width: 88rpx;
  height: 88rpx;
  padding: 10rpx;
  border-radius: 50%;
  background: conic-gradient(#f0c65d 0 78%, rgba(255, 255, 255, 0.09) 78% 100%);
  box-sizing: border-box;
  margin-top: -125rpx;
}

.ring-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #171717;
  color: #fff;
  font-size: 22rpx;
  font-weight: 800;
}

.artist-card {
  position: relative;
  display: grid;
  grid-template-columns: 92rpx minmax(0, 1fr);
  gap: 18rpx;
  align-items: center;
  padding-right: 28rpx;
}

.artist-avatar-lg {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(212, 160, 52, 0.56);
  background: #2a2a2a;
}

.artist-info-block {
  min-width: 0;
  padding-right: 0;
}

.artist-name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.artist-name {
  color: #f5f5f5;
  font-size: 27rpx;
  font-weight: 800;
}

.score-badge {
  height: 30rpx;
  padding: 0 10rpx;
  display: inline-flex;
  align-items: center;
  border: 1rpx solid rgba(220, 177, 78, 0.9);
  border-radius: 999rpx;
  color: #d8aa45;
  font-size: 19rpx;
  font-weight: 800;
}

.score-text,
.artist-subtitle {
  color: rgba(255, 255, 255, 0.58);
  font-size: 20rpx;
}

.score-text {
  color: #d8aa45;
}

.artist-subtitle {
  display: block;
  margin-top: 10rpx;
}

.artist-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 16rpx;
}

.artist-stat {
  min-width: 0;
  padding: 0 12rpx;
  border-left: 1rpx solid rgba(255, 255, 255, 0.07);
}

.artist-stat:first-child {
  padding-left: 0;
  border-left: 0;
}

.artist-stat-value,
.artist-stat-label {
  display: block;
}

.artist-stat-value {
  color: #fff;
  font-size: 25rpx;
  font-weight: 700;
}

.artist-stat-label {
  margin-top: 4rpx;
  color: rgba(255, 255, 255, 0.48);
  font-size: 18rpx;
  white-space: nowrap;
}

.artist-link,
.more-link {
  color: rgba(255, 255, 255, 0.78);
  font-size: 21rpx;
  white-space: nowrap;
}

.artist-link {
  position: absolute;
  right: 24rpx;
  top: 24rpx;
  height: 42rpx;
  padding: 0 16rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(216, 170, 69, 0.38);
  border-radius: 999rpx;
  background: rgba(216, 170, 69, 0.08);
  color: #d8aa45;
  font-size: 20rpx;
  font-weight: 700;
}

.section-top,
.section-title {
  display: flex;
  align-items: center;
}

.section-top {
  justify-content: space-between;
}

.section-title {
  gap: 12rpx;
  color: #f2f2f2;
  font-size: 26rpx;
  font-weight: 800;
}

.section-icon {
  color: #d8aa45;
  font-size: 26rpx;
}

.record-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 222rpx;
  gap: 18rpx;
  margin-top: 22rpx;
  align-items: end;
}

.record-list {
  position: relative;
}

.record-list::before {
  content: '';
  position: absolute;
  left: 6rpx;
  top: 14rpx;
  bottom: 14rpx;
  width: 2rpx;
  background: rgba(240, 198, 93, 0.45);
}

.record-item {
  position: relative;
  display: grid;
  grid-template-columns: 124rpx minmax(0, 1fr) 86rpx;
  gap: 10rpx;
  align-items: center;
  padding: 8rpx 0 8rpx 24rpx;
  color: rgba(255, 255, 255, 0.66);
  font-size: 21rpx;
}

.record-dot {
  position: absolute;
  left: 0;
  top: 18rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #f0c65d;
}

.record-event {
  color: rgba(255, 255, 255, 0.76);
}

.record-price {
  text-align: right;
  color: rgba(255, 255, 255, 0.64);
  white-space: nowrap;
  margin-right: 60rpx;
}

.record-price.current {
  color: #f0c65d;
  font-weight: 800;
}

.gain-card {
  height: 120rpx;
  padding: 16rpx 18rpx 14rpx;
  position: relative;
  margin-top: -35rpx;
  overflow: hidden;
  border-radius: 12rpx;
  border: 1rpx solid rgba(216, 170, 69, 0.18);
  background:
    radial-gradient(circle at 84% 28%, rgba(240, 198, 93, 0.12), transparent 34%),
    linear-gradient(135deg, rgba(216, 170, 69, 0.08), rgba(12, 12, 12, 0.34)),
    rgba(9, 9, 9, 0.48);
  box-sizing: border-box;
}

.gain-card::before {
  content: '';
  position: absolute;
  inset: auto 12rpx 16rpx 12rpx;
  height: 1rpx;
  background: linear-gradient(90deg, transparent, rgba(216, 170, 69, 0.24), transparent);
}

.gain-label,
.gain-value {
  display: block;
}

.gain-label {
  color: rgba(255, 255, 255, 0.62);
  font-size: 18rpx;
}

.gain-value {
  margin-top: 2rpx;
  color: #f0c65d;
  font-size: 34rpx;
  line-height: 1;
  font-weight: 800;
}

.sparkline {
  position: absolute;
  left: 78rpx;
  right: 14rpx;
  bottom: 14rpx;
  height: 54rpx;
}

.sparkline::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 12rpx;
  height: 1rpx;
  background: linear-gradient(90deg, transparent, rgba(216, 170, 69, 0.22), transparent);
}

.sparkline::after {
  content: '';
  position: absolute;
  right: 4rpx;
  top: 7rpx;
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #f0c65d;
  box-shadow: 0 0 12rpx rgba(240, 198, 93, 0.45);
}

.spark-seg {
  position: absolute;
  height: 5rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #a77a19, #f0c65d);
  transform-origin: left center;
  box-shadow: 0 0 14rpx rgba(240, 198, 93, 0.22);
}

.spark-seg.one { left: 2rpx; bottom: 14rpx; width: 34rpx; transform: rotate(-18deg); }
.spark-seg.two { left: 31rpx; bottom: 22rpx; width: 32rpx; transform: rotate(13deg); }
.spark-seg.three { left: 59rpx; bottom: 21rpx; width: 34rpx; transform: rotate(-24deg); }
.spark-seg.four { left: 88rpx; bottom: 31rpx; width: 42rpx; transform: rotate(-39deg); }

.cert-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 168rpx;
  gap: 14rpx;
  align-items: center;
  margin-top: 22rpx;
}

.cert-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10rpx;
}

.cert-item {
  display: grid;
  grid-template-columns: 34rpx 1fr;
  gap: 8rpx;
  min-width: 0;
  align-items: center;
}

.cert-icon {
  width: 30rpx;
  height: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 1rpx solid rgba(216, 170, 69, 0.35);
  color: #d8aa45;
  background: rgba(216, 170, 69, 0.08);
}

.cert-label,
.cert-value {
  display: block;
}

.cert-label {
  color: rgba(255, 255, 255, 0.78);
  font-size: 18rpx;
  font-weight: 600;
}

.cert-value {
  margin-top: 4rpx;
  color: rgba(255, 255, 255, 0.52);
  font-size: 16rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cert-preview {
  width: 168rpx;
  height: 94rpx;
  padding: 8rpx;
  border-radius: 12rpx;
  border: 1rpx solid rgba(216, 170, 69, 0.18);
  background:
    linear-gradient(135deg, rgba(216, 170, 69, 0.08), transparent),
    rgba(10, 10, 10, 0.4);
  box-sizing: border-box;
}

.cert-image {
  width: 100%;
  height: 100%;
  border-radius: 8rpx;
  opacity: 0.62;
  border: 1rpx solid rgba(216, 170, 69, 0.2);
}

.description-card {
  padding-bottom: 30rpx;
}

.story-text {
  margin-top: 14rpx;
  padding-right: 58rpx;
  color: rgba(255, 255, 255, 0.62);
  font-size: 21rpx;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.story-text.expanded {
  display: block;
}

.expand-link {
  position: absolute;
  right: 28rpx;
  bottom: 30rpx;
  color: #d8aa45;
  font-size: 24rpx;
}

.commission-tip {
  margin-top: 18rpx;
  padding: 20rpx 26rpx;
  border-radius: 14rpx;
  border: 1rpx solid rgba(216, 170, 69, 0.35);
  background: rgba(216, 170, 69, 0.08);
  color: #f0c65d;
}

.bottom-bar {
  grid-template-columns: 104rpx 104rpx minmax(0, 1fr);
  gap: 18rpx;
  padding: 16rpx 30rpx calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(24, 24, 24, 0.94);
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.bar-action {
  height: 82rpx;
  width: 100%;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.86);
  font-size: 20rpx;
  line-height: 1;
}

.chat-mark {
  position: relative;
  width: 36rpx;
  height: 30rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  box-sizing: border-box;
}

.chat-mark::after {
  content: '';
  position: absolute;
  right: 1rpx;
  bottom: -6rpx;
  width: 10rpx;
  height: 10rpx;
  border-right: 3rpx solid rgba(255, 255, 255, 0.9);
  border-bottom: 3rpx solid rgba(255, 255, 255, 0.9);
  transform: rotate(18deg);
}

.bar-icon {
  font-size: 38rpx;
  line-height: 1;
}

.collect-btn {
  height: 80rpx;
  width: 100%;
  margin: 0;
  padding: 0 36rpx;
  border: 0;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #d6aa34 0%, #c19724 52%, #aa7712 100%);
  color: #fff;
  font-size: 27rpx;
  font-weight: 800;
  box-shadow: 0 12rpx 34rpx rgba(194, 145, 33, 0.28);
}

@media (max-width: 520px) {
  .detail-shell {
    max-width: none;
    padding: 0 22rpx;
  }

  .tag-row {
    gap: 6rpx;
  }

  .gold-tag {
    height: 26rpx;
    padding: 0 8rpx;
    font-size: 15rpx;
  }

  .market-content {
    grid-template-columns: minmax(0, 1fr) 312rpx;
    align-items: end;
  }

  .price-block {
    min-height: 196rpx;
  }

  .model-panel {
    padding: 16rpx;
  }

  .model-body {
    grid-template-columns: 1fr 82rpx;
  }

  .confidence-ring {
    width: 78rpx;
    height: 78rpx;
  }

  .artist-card {
    grid-template-columns: 86rpx minmax(0, 1fr);
  }

  .artist-avatar-lg {
    width: 86rpx;
    height: 86rpx;
  }

  .artist-stats {
    column-gap: 0;
  }

  .artist-stat {
    padding: 0 8rpx;
  }

  .artist-stat-value {
    font-size: 24rpx;
  }

  .artist-stat-label {
    font-size: 17rpx;
    line-height: 1.18;
  }

  .record-body {
    grid-template-columns: minmax(0, 1fr) 174rpx;
    gap: 14rpx;
  }

  .gain-card {
    width: 174rpx;
    height: 112rpx;
    justify-self: end;
  }

  .record-item {
    grid-template-columns: 118rpx minmax(0, 1fr) 78rpx;
    gap: 8rpx;
    font-size: 20rpx;
  }

  .cert-body {
    grid-template-columns: minmax(0, 1fr) 150rpx;
    gap: 12rpx;
  }

  .cert-preview {
    width: 150rpx;
    height: 86rpx;
    justify-self: end;
  }

  .cert-list {
    gap: 8rpx;
  }

  .cert-item {
    grid-template-columns: 30rpx 1fr;
    gap: 6rpx;
  }

  .cert-icon {
    width: 28rpx;
    height: 28rpx;
    font-size: 16rpx;
  }
}

button::after {
  border: 0;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 40rpx;
}
.empty-icon {
  width: 240rpx;
  height: 240rpx;
  opacity: 0.4;
  margin-bottom: 32rpx;
}
.empty-text {
  font-size: 28rpx;
  color: $text-dim;
  margin-bottom: 40rpx;
}
.empty-btn {
  width: 240rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 40rpx;
  background: $gold;
  color: #fff;
  font-size: 28rpx;
}
</style>
