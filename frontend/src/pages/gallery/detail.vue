<template>
  <view class="detail-page">
    <view class="nav-bar">
      <view class="nav-icon" @click="goBack">‹</view>
      <view class="nav-title">作品详情</view>
      <view class="nav-icon share" @click="onShare">↗</view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="isEmpty">
      <image class="empty-icon" src="/static/images/artwork-fallback.png" mode="aspectFit" />
      <text class="empty-text">作品不存在或已下架</text>
      <button class="empty-btn" @click="goBack">返回</button>
    </view>

    <view class="content-area hero-card" v-if="!isEmpty">
      <swiper class="image-swiper" @change="onSwiperChange">
        <swiper-item v-for="(img, index) in images" :key="index">
          <image
            class="hero-image"
            :src="img"
            mode="aspectFill"
            @error="onArtworkImageError(index)"
            @click="previewImage(index)"
          ></image>
        </swiper-item>
      </swiper>
      <view class="image-count">{{ currentImageIndex + 1 }}/{{ images.length || 1 }}</view>
      <view class="video-btn" v-if="detail.videoUrl" @click="playVideo">观看视频</view>
    </view>

    <view class="summary-section">
      <view class="work-title">{{ detail.title || '静物0751' }}</view>

      <view class="author-row">
        <image class="author-avatar" :src="authorAvatarSrc" @click="goArtistHome" @error="onAuthorAvatarError"></image>
        <view class="author-info" @click="goArtistHome">
          <view class="author-name-row">
            <text class="author-name">{{ detail.authorName || '孟儒' }}</text>
            <text class="verify-badge">V</text>
          </view>
          <text class="author-subtitle">{{ authorSubtitle }}</text>
        </view>
        <view class="like-counter" @click="onFavorite">
          <text class="heart">{{ detail.isFavorite ? '♥' : '♡' }}</text>
          <text>{{ displayLikeCount }}</text>
        </view>
      </view>

      <view class="meta-strip">
        <view class="meta-item">
          <text class="meta-label">创作年份</text>
          <text class="meta-value">{{ artworkYear }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">作品材质</text>
          <text class="meta-value">{{ artworkMaterial }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">作品尺寸</text>
          <text class="meta-value">{{ artworkSize }}</text>
        </view>
        <view class="meta-item">
          <text class="meta-label">作品类型</text>
          <text class="meta-value">{{ artworkType }}</text>
        </view>
      </view>

      <view class="price-row">
        <view>
          <view class="price">{{ formatPrice(detail.price) }}</view>
          <view class="forecast">
            <text>预估上涨 {{ tomorrowIncreaseRange || '￥1.6 - ￥2.4' }}</text>
            <text class="info-dot">i</text>
          </view>
        </view>
        <view class="badges">
          <text class="badge">唯一件</text>
          <text class="badge">支持证书</text>
          <text class="badge">支持流通</text>
        </view>
      </view>
    </view>

    <view class="panel story-panel" v-if="storyText">
      <view class="panel-heading">
        <text class="panel-icon">▣</text>
        <text>作品故事</text>
      </view>
      <view class="story-text" :class="{ expanded: storyExpanded }">
        {{ storyText }}
      </view>
      <view class="panel-arrow" v-if="storyCanExpand" @click="storyExpanded = !storyExpanded">›</view>
    </view>

    <view class="split-grid">
      <view class="panel info-panel">
        <view class="panel-heading">
          <text class="panel-icon">▤</text>
          <text>作品信息</text>
        </view>
        <view class="info-row" v-for="row in infoRows" :key="row.label">
          <text class="row-label">{{ row.label }}</text>
          <text class="row-value">{{ row.value }}</text>
        </view>
      </view>

      <view class="panel benefit-panel">
        <view class="panel-heading">
          <text class="panel-icon">◌</text>
          <text>收藏权益</text>
        </view>
        <view class="benefit-item" v-for="item in benefits" :key="item.title">
          <text class="benefit-mark">✓</text>
          <view>
            <view class="benefit-title">{{ item.title }}</view>
            <view class="benefit-desc">{{ item.desc }}</view>
          </view>
        </view>
      </view>
    </view>

    <view class="panel circulation-panel">
      <view class="panel-top">
        <view class="panel-heading">
          <text class="panel-icon">▥</text>
          <text>流通记录摘要</text>
        </view>
        <view class="more-link">查看完整记录 ›</view>
      </view>
      <view class="timeline">
        <view class="timeline-line"></view>
        <view class="timeline-item" v-for="item in circulationSteps" :key="item.title">
          <view class="timeline-dot"></view>
          <view class="timeline-title">{{ item.title }}</view>
          <view class="timeline-date">{{ item.date }}</view>
        </view>
      </view>
    </view>

    <view class="panel related-panel">
      <view class="panel-top">
        <view class="panel-heading">
          <text class="panel-icon">▧</text>
          <text>同艺术家其他作品</text>
        </view>
        <view class="more-link" @click="goArtistHome">更多 ›</view>
      </view>
      <view class="related-grid">
        <view class="related-card" v-for="item in relatedWorks" :key="item.id" @click="goRelatedWork(item.id)">
          <image class="related-image" :src="item.cover" mode="aspectFill"></image>
          <view class="related-like">♡</view>
          <view class="related-body">
            <view class="related-title">{{ item.title }}</view>
            <view class="related-meta">{{ item.meta }}</view>
            <view class="related-price">{{ item.price }}</view>
          </view>
        </view>
      </view>
    </view>

    <view class="commission-tip" v-if="commission > 0" @click="showShareModal">
      <text>分享推广可获得佣金</text>
      <text>{{ formatYuanAmount(commission) }}</text>
    </view>

    <view class="bottom-bar safe-area-bottom">
      <button class="advisor-btn" @click="contactArtist">联系顾问</button>
      <button class="collect-btn" @click="onFavorite">
        <text>{{ detail.isFavorite ? '♥' : '♡' }}</text>
        <text>{{ detail.isFavorite ? '已收藏' : '立即收藏' }}</text>
      </button>
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
    </view><!-- /.content-area -->
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
      storyExpanded: false,
      commission: 0,
      commissionLevels: [],
      defaultAvatar: '/static/images/avatar.png',
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
      return this.extractMaterial(this.detail.material || this.detail.medium || this.detail.artType) || '布面油画'
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
      return this.cleanArtworkLabel(this.detail.subject || this.detail.categoryName || this.detail.artType) || '静物'
    },
    authorSubtitle() {
      return this.detail.authorSubtitle || this.detail.authorBadge || '青年油画艺术家 · 杭州'
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
          price: '¥1.2万'
        }
      ]
    }
  },

  onLoad() {
    this.fetchDetail()
  },

  methods: {
    async fetchDetail() {
      const pages = getCurrentPages()
      const currentPage = pages[pages.length - 1]
      const id = currentPage.options?.id

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

          if (data.images && Array.isArray(data.images) && data.images.length > 0) {
            this.images = data.images.map(this.normalizeResourceUrl).filter(Boolean)
          } else if (data.cover) {
            this.images = [this.normalizeResourceUrl(data.cover)]
          } else if (data.coverImage) {
            this.images = [this.normalizeResourceUrl(data.coverImage)]
          } else {
            this.images = [FALLBACK_COVER]
          }

          this.images = this.images.filter(Boolean)
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

    normalizeResourceUrl(url) {
      if (!url || typeof url !== 'string') return ''
      if (url === '[]' || url === '{}') return ''
      if (url.startsWith('/')) return url
      if (url.startsWith('http://localhost:8087')) {
        return `/upload${url.slice('http://localhost:8087'.length)}`
      }
      if (url.startsWith('http://127.0.0.1:8087')) {
        return `/upload${url.slice('http://127.0.0.1:8087'.length)}`
      }
      const app = getApp()
      const domain = app?.globalData?.fileDomain || app?.globalData?.domain || ''
      if (!url.startsWith('http')) {
        return domain ? `${domain}${url.startsWith('/') ? '' : '/'}${url}` : url
      }
      return url
    },

    cleanArtworkLabel(value) {
      if (!value || typeof value !== 'string') return ''
      return value.replace(/^分类[:：]/, '').trim()
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
      const yuan = Number(price) / 100
      return this.formatYuanAmount(yuan)
    },

    formatYuanAmount(amount) {
      const value = Number(amount || 0)
      if (value >= 10000) {
        const wan = value / 10000
        return `¥${Number.isInteger(wan) ? wan.toFixed(0) : wan.toFixed(1)}万`
      }
      return `¥${Math.round(value).toLocaleString()}`
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
