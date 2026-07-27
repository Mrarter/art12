<template>
  <view class="detail-page" :style="pageStyle">
    <view class="nav-bar" :class="{ 'is-transparent': pageScrolled }" :style="navStyle">
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
            <image class="hero-backdrop" :src="img || fallbackCover" mode="aspectFill" @error="onArtworkImageError(index)"></image>
            <image class="hero-image" :src="img || fallbackCover" mode="aspectFill" @load="onHeroImageLoad" @error="onArtworkImageError(index)" @click="previewImage(index)"></image>
          </swiper-item>
        </swiper>
        <view class="hero-shadow"></view>
        <view class="new-chip" :class="{ 'chip-hidden': pageScrolled, 'is-collected': isSoldArtwork }">
          {{ heroStatusLabel }}
        </view>
        <view class="hero-copy">
          <text class="hero-serial">作品编号：{{ certificateCode }}</text>
        </view>
        <view class="hero-like" @click="onFavorite">
          <text class="heart">{{ detail.isFavorite ? '♥' : '♡' }}</text>
          <text>{{ displayLikeCount }}</text>
        </view>
        <view class="video-btn" v-if="detail.videoUrl" @click="playVideo">观看视频</view>
      </view>

      <view class="card market-card" :class="{ 'is-sold': isSoldArtwork }">
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
            <text class="work-artist">{{ authorName }}</text>
            <text class="work-meta">{{ artworkMetaLine }}</text>
          </view>
        </view>
        <view class="market-content">
          <view class="price-block" :class="{ 'is-sold': isSoldArtwork }">
            <view class="label-line">
              <text>{{ isSoldArtwork ? '作品状态' : '当前收藏价' }}</text>
              <text class="question">?</text>
            </view>
            <view class="price">
              <template v-if="isSoldArtwork">
                <text class="sold-status">已收藏</text>
                <text class="collector-name" v-if="collectorName">藏家：{{ collectorName }}</text>
              </template>
              <template v-else>
                <text class="price-symbol">¥</text>
                <text>{{ priceNumber }}</text>
              </template>
            </view>
            <view class="discount-line" v-if="discountText">{{ discountText }}</view>
            <view class="rise-line" v-if="!isSoldArtwork">↗ 预估未来一年上涨 {{ growthRangeDisplay }}</view>
            <view class="collect-line">♙ 已被 {{ displayLikeCount }} 位藏家喜欢</view>
          </view>
          <view class="model-panel">
            <view class="model-title">♙ 涨跌趋势</view>
            <text class="model-sub">未来一年上涨区间</text>
            <view class="model-body">
              <view class="model-copy">
                <text class="model-price">{{ growthRangeDisplay }}</text>
                <text class="confidence">置信度 78%</text>
                <text class="factor-title">影响因素</text>
                <template v-if="hasValidDisplayPrice">
                  <text class="factor">• 艺术家评级：高</text>
                  <text class="factor">• 历史成交：稳定</text>
                  <text class="factor">• 当前热度：上升</text>
                </template>
                <template v-else>
                  <text class="factor">• 暂无价格基准</text>
                  <text class="factor">• 等待设置收藏价</text>
                  <text class="factor">• 暂不计算上涨</text>
                </template>
              </view>
              <view class="confidence-ring">
                <view class="ring-inner">78%</view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="card artist-card" @click="goProfileHome">
        <view class="artist-avatar-wrap">
          <image class="artist-avatar-lg" :src="authorAvatarSrc" mode="aspectFill" @error="onAuthorAvatarError"></image>
          <view v-if="authorCertified" class="artist-verify-badge">V</view>
          </view>
          <view class="artist-info-block">
            <view class="artist-name-row">
            <text class="artist-name">{{ profileCardName }}</text>
            <template v-if="profileCardIsHolder">
              <text class="score-badge">持有者</text>
              <text class="score-text">藏家资料</text>
            </template>
            <template v-else-if="isArtistProfile">
              <text class="score-badge">{{ artistScoreBadge }}</text>
              <text class="score-text">{{ artistScoreText }}</text>
            </template>
          </view>
          <view class="artist-meta-row">
            <text class="artist-subtitle">{{ profileCardSubtitle }}</text>
            <text v-if="profileCardUidDisplay" class="artist-uid">{{ profileCardUidDisplay }}</text>
          </view>
          <view class="artist-stats">
            <view class="artist-stat" v-for="item in profileCardStats" :key="item.label">
              <text class="artist-stat-value">{{ item.value }}</text>
              <text class="artist-stat-label">{{ item.label }}</text>
            </view>
          </view>
        </view>
        <view class="artist-link">{{ profileCardLinkText }}</view>
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
              <text class="gain-value">{{ totalGainDisplay }}</text>
            </view>
            <view class="sparkline">
              <svg class="sparkline-svg" viewBox="0 0 132 54" preserveAspectRatio="none">
                <defs>
                  <linearGradient id="gainLineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stop-color="#9d6f16" />
                    <stop offset="100%" stop-color="#f0c65d" />
                  </linearGradient>
                  <linearGradient id="gainFillGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stop-color="rgba(240,198,93,0.26)" />
                    <stop offset="100%" stop-color="rgba(240,198,93,0)" />
                  </linearGradient>
                </defs>
                <path class="sparkline-fill" :d="sparklineFillPath" fill="url(#gainFillGradient)"></path>
                <path class="sparkline-line" :d="sparklinePath"></path>
                <circle
                  v-if="sparklineLastPoint"
                  class="sparkline-dot"
                  :cx="sparklineLastPoint.x"
                  :cy="sparklineLastPoint.y"
                  r="3.2"
                ></circle>
              </svg>
            </view>
          </view>
        </view>
      </view>

      <view class="card resale-card" v-if="resaleStats && resaleStats.resaleCount > 0">
        <view class="section-top">
          <view class="section-title">
            <text class="section-icon">⟳</text>
            <text>转售数据</text>
          </view>
          <view class="more-link" @click="goResaleMarket">转售市场 ›</view>
        </view>
        <view class="resale-body">
          <view class="resale-stat-row">
            <view class="resale-stat">
              <text class="resale-stat-value">{{ resaleStats.resaleCount }}</text>
              <text class="resale-stat-label">转售次数</text>
            </view>
            <view class="resale-stat">
              <text class="resale-stat-value">{{ resaleStats.totalTrades }}</text>
              <text class="resale-stat-label">流通次数</text>
            </view>
            <view class="resale-stat">
              <text class="resale-stat-value">¥{{ formatPriceSmall(resaleStats.highestPrice) }}</text>
              <text class="resale-stat-label">最高成交价</text>
            </view>
            <view class="resale-stat">
              <text class="resale-stat-value" :style="{color: (resaleStats.totalGrowthRate || 0) >= 0 ? '#67C23A' : '#F56C6C'}">
                {{ (resaleStats.totalGrowthRate || 0) >= 0 ? '+' : '' }}{{ (resaleStats.totalGrowthRate || 0).toFixed(1) }}%
              </text>
              <text class="resale-stat-label">总涨幅</text>
            </view>
          </view>
        </view>
      </view>

      <view class="card cert-card" @click="goCertificate">
        <view class="section-top">
          <view class="section-title">
            <text class="section-icon">▣</text>
            <text>收藏证书</text>
          </view>
          <view class="more-link">查看证书 ›</view>
        </view>
        <view class="cert-body">
          <view class="cert-list">
            <view class="cert-item cert-item-code">
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
                <text class="cert-value">{{ authorName }} 亲笔签名</text>
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
            <image class="cert-image" :src="certificateThumbnailUrl" mode="aspectFit"></image>
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
        <view class="detail-image-list" v-if="detailImages.length">
          <image
            class="detail-image"
            v-for="(img, index) in detailImages"
            :key="img + index"
            :src="img"
            mode="widthFix"
            @click="previewDetailImage(index)"
            @error="onDetailImageError(index)"
          ></image>
        </view>
      </view>

      <view class="commission-tip" v-if="isCurrentUserPromoter && commission > 0" @click="showShareModal">
        <text>分享推广可获得分成</text>
        <text>{{ formatYuanAmount(commission) }}</text>
      </view>
    </view>

    <view class="certificate-sign-tip" v-if="showCertificateSignTip" @click="goCertificate">
      <view class="certificate-sign-copy">
        <text class="certificate-sign-title">请作者签署收藏证书</text>
        <text class="certificate-sign-desc">作品被收藏或再次收藏后，签署收藏证书即可获得认证费用。</text>
      </view>
      <view class="certificate-sign-action">去签署</view>
    </view>

    <view class="bottom-bar safe-area-bottom" v-if="!isEmpty">
      <button class="bar-action" @click="onFavorite">
        <text class="bar-icon">{{ detail.isFavorite ? '♥' : '♡' }}</text>
        <text>{{ detail.isFavorite ? '已喜欢' : '喜欢' }}</text>
      </button>
      <button class="bar-action consult-action" @click="contactArtist">
        <view class="chat-mark"></view>
        <text>咨询</text>
      </button>
      <button class="collect-btn" :class="{ 'is-loading': buyLoading, 'is-sold-out': !canBuyArtwork }" :disabled="buyLoading || !canBuyArtwork" @click="handleDirectBuy">
        {{ buyButtonText }}
      </button>
    </view>

    <view class="share-modal" v-if="showSharePanel" @click="showSharePanel = false">
      <view class="share-content" @click.stop>
        <view class="share-handle"></view>
        <template v-if="showSharePoster">
          <view class="poster-sheet-head">
            <view>
              <text class="poster-sheet-title">作品分享图</text>
              <text class="poster-sheet-subtitle">{{ sharePosterHint }}</text>
            </view>
            <view class="poster-sheet-back" @click="showSharePoster = false">返回</view>
          </view>
          <view class="share-poster-card">
            <view class="share-poster-art">
              <image :src="posterArtworkImage" mode="aspectFill"></image>
              <view class="poster-art-mask"></view>
              <text class="poster-brand">艺本艺术</text>
              <text class="poster-serial">{{ certificateCode }}</text>
            </view>
            <view class="share-poster-info">
              <view class="poster-title-row">
                <text class="poster-work-title">{{ workName }}</text>
                <text class="poster-tag">{{ artworkType }}</text>
              </view>
              <text class="poster-author">{{ authorName }} · {{ artworkMaterial }} · {{ artworkSize }} · {{ artworkYear }}</text>
              <view class="poster-price-row">
                <view>
                  <text class="poster-price-label">{{ activeResaleListing ? '转售价格' : '当前收藏价' }}</text>
                  <text class="poster-price">¥{{ priceNumber }}</text>
                </view>
                <view class="poster-rise">
                  <text>{{ totalGainDisplay }}</text>
                  <text>累计上涨</text>
                </view>
              </view>
            </view>
            <view class="share-poster-footer">
              <view class="poster-copy">
                <text class="poster-copy-title">{{ sharePosterCodeTitle }}</text>
                <text class="poster-copy-desc">{{ sharePosterCodeDesc }}</text>
              </view>
              <view class="mini-code-box" :class="{ 'is-loading': miniProgramCodeLoading, 'is-error': miniProgramCodeError }">
                <image v-if="miniProgramCodeImage" class="mini-code-image" :src="miniProgramCodeImage" mode="aspectFit"></image>
                <view v-else class="mini-code-mark">
                  <view class="mini-code-ring ring-a"></view>
                  <view class="mini-code-ring ring-b"></view>
                  <view class="mini-code-dot dot-a"></view>
                  <view class="mini-code-dot dot-b"></view>
                  <view class="mini-code-dot dot-c"></view>
                </view>
              </view>
            </view>
          </view>
          <view class="poster-actions">
            <view class="poster-action primary" @click="saveSharePoster">保存分享图</view>
            <view class="poster-action" @click="copyLink">复制链接</view>
          </view>
        </template>
        <template v-else>
          <view class="commission-levels" v-if="showCommissionEstimate">
            <view class="commission-level-title">经纪人分成预估</view>
            <view class="commission-level-row" v-for="level in commissionLevels" :key="level.name">
              <text>{{ level.name }}</text>
              <text>{{ level.amountText }}</text>
            </view>
          </view>
          <view class="share-icons">
            <view class="share-icon-item" @click="shareToFriend">
              <view class="share-icon">
                <image src="/static/share-icons/friend.svg" mode="aspectFit"></image>
              </view>
              <text>分享给好友</text>
            </view>
            <view class="share-icon-item" @click="openMiniProgram">
              <view class="share-icon">
                <image src="/static/share-icons/mini-program.svg" mode="aspectFit"></image>
              </view>
              <text>用小程序打开</text>
            </view>
            <view class="share-icon-item" @click="shareToFriend">
              <view class="share-icon">
                <image src="/static/share-icons/wechat.svg" mode="aspectFit"></image>
              </view>
              <text>分享到微信</text>
            </view>
            <view class="share-icon-item" @click="shareToTimeline">
              <view class="share-icon">
                <image src="/static/share-icons/moments.svg" mode="aspectFit"></image>
              </view>
              <text>分享到朋友圈</text>
            </view>
            <view class="share-icon-item" @click="shareToWeibo">
              <view class="share-icon">
                <image src="/static/share-icons/weibo.svg" mode="aspectFit"></image>
              </view>
              <text>分享到微博</text>
            </view>
            <view class="share-icon-item" @click="copyLink">
              <view class="share-icon">
                <image src="/static/share-icons/link.svg" mode="aspectFit"></image>
              </view>
              <text>复制链接</text>
            </view>
            <view class="share-icon-item" @click="openInBrowser">
              <view class="share-icon">
                <image src="/static/share-icons/browser.svg" mode="aspectFit"></image>
              </view>
              <text>浏览器打开</text>
            </view>
            <view class="share-icon-item" @click="downloadQrCode">
              <view class="share-icon">
                <image src="/static/share-icons/qr.svg" mode="aspectFit"></image>
              </view>
              <text>下载二维码</text>
            </view>
            <view class="share-icon-item" @click="reportArtwork">
              <view class="share-icon">
                <image src="/static/share-icons/report.svg" mode="aspectFit"></image>
              </view>
              <text>举报</text>
            </view>
          </view>
        </template>
        <view class="share-close" @click="closeSharePanel">取消</view>
      </view>
    </view>

    <view class="contact-modal" v-if="showContactModal" @click="showContactModal = false">
      <view class="contact-content" @click.stop>
        <view class="contact-header">
          <text class="contact-title">联系顾问</text>
          <view class="contact-close" @click="showContactModal = false">×</view>
        </view>
        <view class="contact-artist-info">
          <image class="artist-avatar" :src="authorAvatarSrc" mode="aspectFill" @error="onAuthorAvatarError"></image>
          <text class="artist-name">{{ authorName }}</text>
        </view>
        <view class="contact-actions">
          <view class="contact-item" @click="sendMessage">
            <text class="contact-icon">讯</text>
            <text>发送消息</text>
          </view>
        </view>
      </view>
    </view>

    <canvas canvas-id="sharePosterCanvas" class="hidden-share-canvas"></canvas>
  </view><!-- /.detail-page -->
</template>

<script>
import { getProductDetail, addFavorite, removeFavorite } from '@/api/product'
import * as userApi from '@/api/user'
import QRCode from 'qrcode'
import { getArtistScore } from '@/api/artistScore'
import { useUserStore } from '@/store/modules/user'
import { getProductCommission } from '@/api/promoter'
import { triggerCollectIncrease } from '@/api/artworkPrice'
import { getArtworkTrades, getArtworkResaleStats } from '@/api/resale'
import { getAccessToken, isGuestUser, saveRedirectUrl, getCurrentPagePath } from '@/utils/auth'
import { upsertCertificateSignNotice, removeCertificateSignNoticesByArtwork } from '@/utils/certificateNotice'
import { formatArtworkPrice, formatArtworkPriceNumber, formatYuanAmount as formatYuanAmountShared, formatYuanNumber } from '@/utils/price'
import { IS_MP_WEIXIN } from '@/utils/platform'
import { buildH5ShareUrl, setH5ShareMeta } from '@/utils/share'

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
      artistProfile: null,
      artistScore: null,
      images: [],
      detailImages: [],
      currentImageIndex: 0,
      heroHeight: 442,
      pageScrolled: false,
      storyExpanded: false,
      commission: 0,
      commissionLevels: [],
      defaultAvatar: '/static/images/artist-avatar.png',
      showSharePanel: false,
      showSharePoster: false,
      sharePosterChannel: 'wechat',
      miniProgramCodeImage: '',
      miniProgramCodeLoading: false,
      miniProgramCodeError: '',
      sharePosterSaving: false,
      showContactModal: false,
      isEmpty: false,
      priceGrowth: {
        growthRate: '+0%',
        collectCount: 0,
        nextCondition: '喜欢人数增加后可能涨价'
      },
      // 转售/流通数据
      resaleTrades: [],
      resaleStats: null,
      loadingResale: false,
      buyLoading: false,
      buyErrorMessage: '',
      favoriteCountOverride: null,
      authorCertificateSigned: false,
      navMetrics: {
        statusBarHeight: 24,
        navBarHeight: 48,
        menuButtonLeft: 0,
        menuButtonHeight: 32,
        menuButtonTop: 28,
        windowWidth: 375
      }
    }
  },

  computed: {
    pageStyle() {
      return this.navStyle
    },

    navStyle() {
      const m = this.navMetrics
      const navTop = Number(m.statusBarHeight || 24)
      const navHeight = Number(m.navBarHeight || 48)
      const menuButtonLeft = Number(m.menuButtonLeft || 0)
      const rightReserve = menuButtonLeft > 0
        ? Math.max(Number(m.windowWidth || 375) - menuButtonLeft + 8, 72)
        : 16
      return {
        '--status-bar-height': `${navTop}px`,
        '--nav-content-height': `${navHeight}px`,
        '--nav-total-height': `${navTop + navHeight}px`,
        '--nav-right-reserve': `${rightReserve}px`,
        '--menu-button-top': `${Number(m.menuButtonTop || navTop + 4)}px`,
        '--menu-button-height': `${Number(m.menuButtonHeight || 32)}px`
      }
    },

    storyText() {
      return this.detail.description || '这件作品以沉稳的画面关系承载日常物象的温度，厚重笔触与层次色彩形成清晰的视觉节奏，呈现出兼具观赏性与收藏感的当代油画气质。'
    },
    storyCanExpand() {
      return this.storyText && this.storyText.length > 86
    },
    authorAvatarSrc() {
      const profileAvatar = this.profileMatchesDetailAuthor ? this.artistProfile?.avatar : ''
      return this.normalizeAvatarUrl(profileAvatar) || this.normalizeAvatarUrl(this.detail.authorAvatar) || this.defaultAvatar
    },
    authorName() {
      if (this.profileMatchesDetailAuthor) {
        return this.decodeDisplayText(this.artistProfile?.nickname || this.artistProfile?.name || this.artistProfile?.realName || this.detail.authorName || '艺术家')
      }
      return this.decodeDisplayText(this.detail.authorName || this.artistProfile?.nickname || this.artistProfile?.name || this.artistProfile?.realName || '艺术家')
    },
    authorCertified() {
      return !!(this.artistProfile?.certified || this.artistProfile?.certStatus === 1 || this.detail.authorIdentity === 'artist')
    },
    isArtistProfile() {
      if (!this.artistProfile) return true // 未加载时默认展示
      return this.artistProfile.isArtist || this.artistProfile.identities?.includes('artist')
    },
    authorUidDisplay() {
      // 优先使用 artistProfile 的 uid（用户 UID），匹配失败时才回退到 detail.authorUid
      const profileUid = this.profileMatchesDetailAuthor ? this.artistProfile?.uid : ''
      const uid = profileUid || this.detail.authorUid || this.detail.displayAuthorId
      if (!uid) return ''
      return `UID ${uid}`
    },
    profileMatchesDetailAuthor() {
      if (!this.artistProfile || !this.detail) return false
      // 1. 优先比较数字 ID（最可靠，不受 UID/artistCode 格式影响）
      const detailAuthorId = String(this.detail.authorId || '').trim()
      const profileUserId = String(this.artistProfile.userId || this.artistProfile.id || '').trim()
      if (detailAuthorId && profileUserId && detailAuthorId === profileUserId) {
        return true
      }
      // 2. 比较 UID（detail.authorUid 可能是 artistCode 或 userUid）
      const detailUid = String(this.detail.authorUid || '').trim()
      const profileUid = String(this.artistProfile.uid || '').trim()
      if (detailUid && profileUid) {
        // 可能是 artistCode(ART...) vs userUid(USR...)，或者旧格式，都做统一比较
        return detailUid === profileUid
      }
      // 3. 名称回退（最后手段，名称可能重复）
      const detailName = this.decodeDisplayText(this.detail.authorName || '').trim()
      const profileName = this.decodeDisplayText(this.artistProfile.nickname || this.artistProfile.name || this.artistProfile.realName || '').trim()
      return !!(detailName && profileName && detailName === profileName)
    },
    displayLikeCount() {
      if (this.favoriteCountOverride !== null) return this.favoriteCountOverride
      // 用户侧展示使用后台配置后的收藏数，真实收藏数仅作兜底。
      return this.detail.displayLikeCount ?? this.detail.likeCount ?? this.detail.realFavoriteCount ?? this.detail.favoriteCount ?? 0
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
      const parts = [
        (this.profileMatchesDetailAuthor ? this.artistProfile?.artistTitle : '') || this.detail.authorSubtitle || this.detail.authorBadge || (this.authorCertified ? '认证艺术家' : ''),
        (this.profileMatchesDetailAuthor ? this.artistProfile?.region : '') || ''
      ].filter(Boolean)
      return parts.join(' · ') || '艺术家'
    },
    fallbackCover() {
      return FALLBACK_COVER
    },
    workName() {
      return this.decodeDisplayText(this.detail.title || '静物0751')
    },
    posterArtworkImage() {
      return this.images[0] || this.fallbackCover
    },
    sharePosterHint() {
      const map = {
        friend: IS_MP_WEIXIN ? '生成带小程序码的好友分享图' : '生成带H5二维码的好友分享图',
        wechat: IS_MP_WEIXIN ? '生成带小程序码的微信分享图' : '生成带H5二维码的微信分享图',
        timeline: '生成适合朋友圈发布的分享图',
        mini: IS_MP_WEIXIN ? '生成小程序分享图' : '生成H5入口分享图',
        qrcode: IS_MP_WEIXIN ? '生成可保存的小程序码分享图' : '生成可保存的H5二维码分享图'
      }
      return map[this.sharePosterChannel] || (IS_MP_WEIXIN ? '生成带小程序码的分享图' : '生成带H5二维码的分享图')
    },
    sharePosterCodeTitle() {
      return IS_MP_WEIXIN ? '长按识别小程序码' : '长按识别H5二维码'
    },
    sharePosterCodeDesc() {
      return IS_MP_WEIXIN
        ? '打开艺本艺术小程序查看作品详情与流通记录'
        : '打开艺本艺术 H5 查看作品详情与流通记录'
    },
    heroCardStyle() {
      return `height: ${this.heroHeight}rpx`
    },
    artworkMetaLine() {
      return `${this.artworkMaterial} · ${this.artworkSize} · ${this.artworkYear}`
    },
    displayPrice() {
      if (this.activeResaleListing?.resalePrice) {
        return Number(this.activeResaleListing.resalePrice)
      }
      return this.resolveCurrentPrice(this.detail)
    },
    priceNumber() {
      return formatArtworkPriceNumber(this.displayPrice || 0)
    },
    publishPrice() {
      return Number(
        this.detail.publishPrice ||
        this.detail.publish_price ||
        this.detail.originalPrice ||
        this.detail.original_price ||
        this.detail.price ||
        0
      )
    },
    discountText() {
      const currentPrice = Number(this.displayPrice || 0)
      const publishPrice = Number(this.publishPrice || 0)
      if (this.isSoldArtwork || currentPrice <= 0 || publishPrice <= 0 || currentPrice >= publishPrice) return ''
      return `限时优惠${this.formatDiscountAmount(publishPrice - currentPrice)}元`
    },
    hasValidDisplayPrice() {
      return Number(this.displayPrice || 0) > 0
    },
    isSoldArtwork() {
      return Number(this.detail.status) === 2 && !this.activeResaleListing
    },
    activeResaleListing() {
      const listing = this.detail.resaleListing || this.detail.activeResaleListing || null
      if (!listing || String(listing.status || '') !== 'pending') return null
      return listing
    },
    currentUserId() {
      const userStore = useUserStore()
      const info = userStore.userInfo || {}
      return Number(info.id || info.userId || userStore.tokenData?.userId || 0)
    },
    isCurrentAuthor() {
      const authorId = Number(this.detail.authorId || 0)
      return !!authorId && authorId === this.currentUserId
    },
    isCurrentUserPromoter() {
      const userStore = useUserStore()
      const info = userStore.userInfo || {}
      const rawIdentities = info.identities || info.identity_json || info.identity || userStore.identities || []
      const identities = this.normalizeIdentityList(rawIdentities)
      return Boolean(
        info.isPromoter ||
        userStore.isPromoter ||
        identities.some(item => ['promoter', 'certified_promoter', 'verified_promoter', '经纪人', '认证经纪人'].includes(item))
      )
    },
    showCommissionEstimate() {
      return this.isCurrentUserPromoter && this.commissionLevels.length > 0
    },
    isCurrentHolder() {
      const holderId = Number(this.detail.holderId || 0)
      return !!holderId && holderId === this.currentUserId
    },
    canPublishResale() {
      return this.isSoldArtwork && this.isCurrentHolder && !this.activeResaleListing
    },
    showCertificateSignTip() {
      return this.isSoldArtwork && this.isCurrentAuthor && !this.authorCertificateSigned
    },
    canBuyArtwork() {
      if (this.activeResaleListing || this.canPublishResale) return true
      return Number(this.detail.status) === 1
    },
    buyButtonText() {
      if (this.buyLoading) return '购买中...'
      if (this.activeResaleListing) return '立即购买'
      if (this.canPublishResale) return '发起转售'
      if (this.isSoldArtwork) return '已收藏'
      if (Number(this.detail.status) === 0) return '已下架'
      return '立即购买'
    },
    heroStatusLabel() {
      return this.isSoldArtwork ? '已收藏' : '☆ NEW'
    },
    collectorName() {
      const name = this.detail.holderName || this.detail.collectorName || this.detail.buyerName || this.detail.ownerName
      if (name) return this.decodeDisplayText(name)
      if (this.isSoldArtwork && this.detail.authorIdentity === 'collector') {
        return this.decodeDisplayText(this.authorName)
      }
      return ''
    },
    growthRangeDisplay() {
      if (!this.hasValidDisplayPrice) return '¥0.00 - ¥0.00'
      return (this.tomorrowIncreaseRange || '¥0.00 - ¥0.00').replace(/\s*-\s*/g, ' - ')
    },
    certificateCode() {
      const raw = this.detail.uid || this.detail.artworkUid || this.detail.code || this.detail.artworkCode
      if (raw) return raw
      const year = String(this.artworkYear || '2024').replace(/[^\d]/g, '') || '2024'
      return `AW${year}-0751`
    },
    certificateThumbnailUrl() {
      return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(this.certificateThumbnailSvg)}`
    },
    certificateThumbnailSvg() {
      const title = this.escapeXml(this.workName || '未命名作品')
      const author = this.escapeXml(this.decodeDisplayText(this.detail.authorRealName || this.detail.artistRealName || this.detail.realName || this.authorName || '未知艺术家'))
      const artworkCode = this.escapeXml(this.certificateCode)
      const cover = this.escapeXml(this.images[0] || this.fallbackCover)
      return `
        <svg xmlns="http://www.w3.org/2000/svg" width="1600" height="1120" viewBox="0 0 1600 1120">
          <defs>
            <linearGradient id="paper" x1="0" x2="1" y1="0" y2="1">
              <stop offset="0%" stop-color="#fcf8ef"/>
              <stop offset="100%" stop-color="#f6ecd8"/>
            </linearGradient>
            <pattern id="waves" width="34" height="18" patternUnits="userSpaceOnUse">
              <path d="M0 9 Q8 3 17 9 T34 9" fill="none" stroke="#ddcfb3" stroke-width="1" opacity="0.35"/>
            </pattern>
          </defs>
          <rect width="1600" height="1120" fill="url(#paper)"/>
          <rect width="1600" height="1120" fill="url(#waves)"/>
          <rect x="28" y="28" width="1544" height="1064" fill="none" stroke="#c7a56d" stroke-width="6"/>
          <rect x="42" y="42" width="1516" height="1036" fill="none" stroke="#ceb078" stroke-width="2"/>
          <text x="800" y="246" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="82" fill="#4d2e16">美术作品收藏证书</text>
          <text x="800" y="308" text-anchor="middle" font-family="Georgia, serif" font-size="26" letter-spacing="10" fill="#624421">ARTWORK COLLECTION CERTIFICATE</text>
          <rect x="168" y="360" width="486" height="352" rx="6" fill="#fffaf0" stroke="#c9a86b" stroke-width="3"/>
          <rect x="154" y="346" width="514" height="380" rx="8" fill="none" stroke="#d8bf8b" stroke-width="2"/>
          <image href="${cover}" x="186" y="380" width="450" height="314" preserveAspectRatio="xMidYMid slice"/>
          <text x="411" y="768" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="30" fill="#4f3217">《${title}》</text>
          <text x="411" y="808" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="22" fill="#5a4630">${author}</text>
          <text x="790" y="388" font-family="STKaiti, KaiTi, serif" font-size="26" fill="#3f2b18">兹证明您收藏的美术作品信息如下：</text>
          <text x="790" y="446" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#3f2b18">作品名称：</text>
          <text x="930" y="446" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#4f3217">${title}</text>
          <line x1="930" y1="458" x2="1435" y2="458" stroke="#d8c39c" stroke-width="1"/>
          <text x="790" y="500" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#3f2b18">艺术家：</text>
          <text x="930" y="500" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#4f3217">${author}</text>
          <line x1="930" y1="512" x2="1435" y2="512" stroke="#d8c39c" stroke-width="1"/>
          <text x="790" y="554" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#3f2b18">作品编号：</text>
          <text x="930" y="554" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#4f3217">${artworkCode}</text>
          <line x1="930" y1="566" x2="1435" y2="566" stroke="#d8c39c" stroke-width="1"/>
          <circle cx="1080" cy="952" r="96" fill="none" stroke="#a43d28" stroke-width="5"/>
          <circle cx="1080" cy="952" r="78" fill="none" stroke="#a43d28" stroke-width="2"/>
          <text x="1080" y="940" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#a43d28">艺本艺术</text>
          <text x="1080" y="976" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#a43d28">鉴定中心</text>
        </svg>
      `.trim()
    },
    artistStats() {
      const workCount = Number(this.artistProfile?.artworkCount || this.artistProfile?.workCount || this.detail.authorWorkCount || 0)
      const dealCount = Number(this.artistProfile?.dealCount || this.artistProfile?.soldCount || this.detail.authorDealCount || this.detail.saleCount || 0)
      const dealRate = workCount > 0 ? `${Math.round((dealCount / workCount) * 100)}%` : '0%'
      const averageRiseValue = Number(this.detail.priceRise || 0) * 100
      return [
        { label: '作品数', value: String(workCount) },
        { label: '成交数', value: String(dealCount) },
        { label: '成交率', value: this.artistProfile?.dealRate || this.detail.authorDealRate || dealRate },
        { label: '平均涨幅', value: this.detail.authorAverageRise || `${averageRiseValue >= 0 ? '+' : ''}${averageRiseValue.toFixed(1)}%` }
      ]
    },
    profileCardIsHolder() {
      return !!(this.activeResaleListing && this.detail.holderId)
    },
    profileCardName() {
      return this.profileCardIsHolder ? (this.collectorName || this.authorName) : this.authorName
    },
    profileCardSubtitle() {
      if (!this.profileCardIsHolder) return this.authorSubtitle
      return this.decodeDisplayText(this.detail.holderTitle || this.detail.authorBadge || '资深藏家')
    },
    profileCardUidDisplay() {
      return this.authorUidDisplay
    },
    profileCardStats() {
      if (!this.profileCardIsHolder) return this.artistStats
      const averageRiseValue = Number(this.detail.priceRise || 0) * 100
      return [
        { label: '持有作品', value: String(this.detail.holderWorkCount || this.artistStats[0]?.value || 0) },
        { label: '成交数', value: String(this.detail.holderDealCount || this.artistStats[1]?.value || 0) },
        { label: '成交率', value: this.detail.holderDealRate || this.artistStats[2]?.value || '0%' },
        { label: '平均涨幅', value: this.detail.holderAverageRise || `${averageRiseValue >= 0 ? '+' : ''}${averageRiseValue.toFixed(1)}%` }
      ]
    },
    profileCardLinkText() {
      return this.profileCardIsHolder ? '进入持有者主页 ›' : '进入艺术家主页 ›'
    },
    artistScoreBadge() {
      if (this.detail.authorScoreLevel) return this.detail.authorScoreLevel
      const total = Number(this.artistScore?.totalScore || 0)
      return total > 0 ? `${total}分` : '待评'
    },
    artistScoreText() {
      return this.detail.authorScoreLevel ? '艺术家评级' : '艺术家评分'
    },
    circulationRows() {
      if (this.resaleTrades.length) {
        return this.resaleTrades.map((trade, index) => ({
          date: this.formatRecordDate(trade.createdTime),
          event: trade.tradeType === 'first_sale'
            ? '首次成交'
            : `第${trade.tradeRound || index + 1}次流通`,
          price: this.formatCirculationPrice(Number(trade.tradePrice || 0)),
          current: index === this.resaleTrades.length - 1
        }))
      }

      const currentPrice = Number(this.displayPrice || 0)
      const basePrice = this.startingPrice
      const startDate = this.formatRecordDate(this.detail.createTime, 0)
      const currentDate = this.formatRecordDate(new Date(), 0)
      const rows = [{ date: startDate, event: '首次上架', price: this.formatCirculationPrice(basePrice) }]

      if (currentDate !== startDate || currentPrice !== basePrice) {
        rows.push({ date: currentDate, event: '当前收藏价', price: this.formatCirculationPrice(currentPrice), current: true })
      } else {
        rows[0].current = true
      }

      return rows
    },
    startingPrice() {
      const currentPrice = Number(this.displayPrice || 0)
      const originalPrice = Number(this.detail.originalPrice || 0)
      if (originalPrice > 0) return originalPrice
      const fallback = currentPrice - Math.max(Number(this.detail.tomorrowIncreaseMax || 0) * 36, Math.round(currentPrice * 0.14))
      return Math.max(fallback, Math.round(currentPrice * 0.72))
    },
    middlePrice() {
      const currentPrice = Number(this.displayPrice || 0)
      const basePrice = this.startingPrice
      if (!currentPrice || !basePrice) return currentPrice || basePrice
      return Math.round((basePrice * 0.44 + currentPrice * 0.56) / 100) * 100
    },
    totalGainDisplay() {
      const currentPrice = Number(this.displayPrice || 0)
      const basePrice = this.startingPrice
      if (!currentPrice || !basePrice || currentPrice <= basePrice) return '+0%'
      const ratio = ((currentPrice - basePrice) / basePrice) * 100
      return `+${Math.round(ratio)}%`
    },
    sparklineSeries() {
      const currentPrice = Number(this.displayPrice || 0)
      const basePrice = this.startingPrice
      const middlePrice = this.middlePrice
      if (!currentPrice || !basePrice) return [0, 0, 0, 0, 0]
      const latePrice = Math.round((middlePrice * 0.48 + currentPrice * 0.52) / 100) * 100
      return [
        basePrice,
        Math.round((basePrice * 0.88 + middlePrice * 0.12) / 100) * 100,
        middlePrice,
        latePrice,
        currentPrice
      ]
    },
    sparklinePoints() {
      const values = this.sparklineSeries
      const width = 132
      const height = 54
      const left = 2
      const top = 5
      const usableWidth = 124
      const usableHeight = 38
      const min = Math.min(...values)
      const max = Math.max(...values)
      const span = Math.max(max - min, 1)
      return values.map((value, index) => ({
        x: left + (usableWidth / Math.max(values.length - 1, 1)) * index,
        y: top + usableHeight - ((value - min) / span) * usableHeight
      }))
    },
    sparklinePath() {
      const points = this.sparklinePoints
      if (!points.length) return ''
      if (points.length === 1) return `M ${points[0].x} ${points[0].y}`
      let path = `M ${points[0].x} ${points[0].y}`
      for (let i = 0; i < points.length - 1; i += 1) {
        const current = points[i]
        const next = points[i + 1]
        const controlX = (current.x + next.x) / 2
        path += ` Q ${controlX} ${current.y} ${next.x} ${next.y}`
      }
      return path
    },
    sparklineFillPath() {
      const points = this.sparklinePoints
      if (!points.length) return ''
      const baseY = 48
      return `${this.sparklinePath} L ${points[points.length - 1].x} ${baseY} L ${points[0].x} ${baseY} Z`
    },
    sparklineLastPoint() {
      const points = this.sparklinePoints
      return points.length ? points[points.length - 1] : null
    },
    tomorrowIncreaseRange() {
      const price = Number(this.displayPrice || 0)
      if (price <= 0) return '¥0.00 - ¥0.00'

      const min = Number(this.detail.tomorrowIncreaseMin || 0)
      const max = Number(this.detail.tomorrowIncreaseMax || 0)
      if (min > 0 || max > 0) {
        const low = Math.min(min || max, max || min) * 365
        const high = Math.max(min, max) * 365
        return low === high ? this.formatPriceDelta(low) : `${this.formatPriceDelta(low)} - ${this.formatPriceDelta(high)}`
      }
      const baseRate = Number(this.detail.customBaseDailyRate || this.detail.baseDailyRate || 0)
      const matureRate = Number(this.detail.customMatureDailyRate || this.detail.matureDailyRate || baseRate)
      if (!baseRate && !matureRate) return ''
      const low = Math.round(price * Math.min(baseRate || matureRate, matureRate || baseRate) * 365)
      const high = Math.round(price * Math.max(baseRate, matureRate) * 365)
      return low === high ? this.formatPriceDelta(low) : `${this.formatPriceDelta(low)} - ${this.formatPriceDelta(high)}`
    },
    infoRows() {
      return [
        { label: '创作年份', value: this.artworkYear },
        { label: '作品材质', value: this.artworkMaterial },
        { label: '作品尺寸', value: this.artworkSize },
        { label: '作品类型', value: this.artworkType },
        { label: '题材', value: this.subjectText },
        { label: '作者', value: this.authorName },
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
          title: this.detail.title ? `${this.workName}-已修复` : '测试作品-已修复',
          meta: `${this.artworkMaterial} · ${this.artworkSize} · ${this.artworkYear}`,
          price: this.formatPrice(this.displayPrice || 1200000)
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
    this.initNavMetrics()
    this.fetchDetail(options)
  },

  onShow() {
    this.refreshAuthorCertificateState()
  },

  onPageScroll(e) {
    this.pageScrolled = Number(e?.scrollTop || 0) > 12
  },

  onShareAppMessage() {
    return this.buildMiniProgramSharePayload()
  },

  onShareTimeline() {
    const payload = this.buildMiniProgramSharePayload()
    const id = encodeURIComponent(this.detail?.id || '')
    return {
      title: payload.title,
      query: id ? `id=${id}&from=timeline` : '',
      imageUrl: payload.imageUrl
    }
  },

  methods: {
    initNavMetrics() {
      try {
        const systemInfo = uni.getSystemInfoSync ? uni.getSystemInfoSync() : {}
        let menuButton = null
        // #ifdef MP-WEIXIN
        if (uni.getMenuButtonBoundingClientRect) {
          menuButton = uni.getMenuButtonBoundingClientRect()
        }
        // #endif

        const statusBarHeight = Number(systemInfo.statusBarHeight || 24)
        const windowWidth = Number(systemInfo.windowWidth || 375)
        const menuButtonTop = Number(menuButton?.top || statusBarHeight + 4)
        const menuButtonHeight = Number(menuButton?.height || 32)
        const navBarHeight = menuButton
          ? Math.max((menuButtonTop - statusBarHeight) * 2 + menuButtonHeight, 44)
          : 48

        this.navMetrics = {
          statusBarHeight,
          navBarHeight,
          menuButtonLeft: Number(menuButton?.left || 0),
          menuButtonHeight,
          menuButtonTop,
          windowWidth
        }
      } catch (error) {
        this.navMetrics = {
          statusBarHeight: 24,
          navBarHeight: 48,
          menuButtonLeft: 0,
          menuButtonHeight: 32,
          menuButtonTop: 28,
          windowWidth: 375
        }
      }
    },

    async fetchDetail(routeOptions = {}) {
      const pages = getCurrentPages()
      const currentPage = pages[pages.length - 1]
      const id = this.resolveRouteArtworkId(routeOptions, currentPage?.options)

      if (!id) {
        this.isEmpty = true
        return
      }
      if (String(this.detail.id || '') !== String(id)) {
        this.miniProgramCodeImage = ''
        this.miniProgramCodeError = ''
      }

      try {
        const data = await getProductDetail(id)
        if (data) {
          this.detail = data
          this.favoriteCountOverride = null
          this.initPriceGrowth(data)
          this.currentImageIndex = 0
          this.heroHeight = 442

          this.images = this.buildArtworkImages(data)
          if (!this.images.length) this.images = [FALLBACK_COVER]
          this.detailImages = this.buildDetailImages(data)
          await Promise.allSettled([
            this.loadArtistProfile(data.authorId || data.authorUid),
            this.loadArtistScore(data.authorId || data.authorUid),
            this.loadCommission(id),
            this.loadResaleData(id)
          ])
          this.refreshAuthorCertificateState()
          this.syncAuthorCertificateNotice()
          this.saveBrowseHistory(data)
          this.updateShareMeta()
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

    async loadArtistProfile(authorId) {
      if (!authorId) return
      try {
        const profile = await userApi.getArtistInfo(authorId)
        this.artistProfile = profile || null
      } catch (error) {
        console.warn('加载艺术家资料失败', error)
      }
    },

    async loadArtistScore(authorId) {
      if (!authorId || Number.isNaN(Number(authorId))) return
      try {
        const score = await getArtistScore(Number(authorId))
        this.artistScore = score || null
      } catch (error) {
        console.warn('加载艺术家评分失败', error)
      }
    },

    async loadResaleData(artworkId) {
      if (!artworkId) return
      this.loadingResale = true
      try {
        const [trades, stats] = await Promise.allSettled([
          getArtworkTrades(artworkId),
          getArtworkResaleStats(artworkId)
        ])
        if (trades.status === 'fulfilled' && trades.value) {
          this.resaleTrades = trades.value
        }
        if (stats.status === 'fulfilled' && stats.value) {
          this.resaleStats = stats.value
        }
      } catch (e) {
        console.warn('加载转售数据失败', e)
      }
      finally { this.loadingResale = false }
    },

    goCertificate() {
      if (!this.detail?.id) return
      uni.navigateTo({ url: `/pages/gallery/certificate?id=${this.detail.id}` })
    },

    getAuthorCertificateStorageKey() {
      const authorCode = this.detail.authorUid || this.detail.displayAuthorId || this.formatIdentity('USR', this.detail.authorId)
      return `artistSignature:${authorCode}`
    },

    refreshAuthorCertificateState() {
      if (!this.detail?.authorId) {
        this.authorCertificateSigned = false
        return
      }
      const savedSignature = uni.getStorageSync(this.getAuthorCertificateStorageKey()) || ''
      this.authorCertificateSigned = !!savedSignature
      if (this.authorCertificateSigned && this.detail?.id) {
        removeCertificateSignNoticesByArtwork(this.detail.id, this.detail.authorId)
      }
    },

    syncAuthorCertificateNotice() {
      if (!this.isSoldArtwork || !this.detail?.authorId || !this.detail?.id) return
      if (this.authorCertificateSigned) {
        removeCertificateSignNoticesByArtwork(this.detail.id, this.detail.authorId)
        return
      }
      const tradeStage = Math.max(Number(this.resaleStats?.totalTrades || this.resaleStats?.resaleCount || 1), 1)
      upsertCertificateSignNotice({
        userId: this.detail.authorId,
        artworkId: this.detail.id,
        artworkTitle: this.workName,
        certificateCode: this.certificateCode,
        collectorName: this.collectorName,
        holderId: this.detail.holderId || '',
        tradeStage
      })
    },

    formatIdentity(prefix, value) {
      if (!value) return `${prefix}000000000000`
      const digits = String(value).replace(/\D/g, '')
      return `${prefix}${digits.padStart(12, '0')}`
    },

    saveBrowseHistory(item) {
      if (!item || !item.id) return
      const artworkImage = this.extractImageList(item.images)[0] || ''
      const record = {
        id: item.id,
        name: this.decodeDisplayText(item.title || item.name || '未命名作品'),
        author: this.decodeDisplayText(item.authorName || item.artistName || '未知艺术家'),
        price: this.resolveCurrentPrice(item),
        image: artworkImage || item.coverImage || item.cover || '',
        time: Date.now()
      }
      const history = uni.getStorageSync('browseHistoryWorks') || []
      const next = [record, ...history.filter(v => v.id !== record.id)].slice(0, 50)
      uni.setStorageSync('browseHistoryWorks', next)

      if (item.authorId || item.authorUid || item.authorName) {
        const artistRecord = {
          id: item.authorId || item.authorUid,
          name: this.decodeDisplayText(item.authorName || item.artistName || '未知艺术家'),
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

    resolveRouteArtworkId(...optionSources) {
      for (const options of optionSources) {
        const directId = options?.id || options?.artworkId
        if (directId) return directId
        const sceneId = this.readArtworkIdFromScene(options?.scene)
        if (sceneId) return sceneId
      }
      return this.readRouteIdFromLocation()
    },

    readArtworkIdFromScene(scene) {
      if (!scene) return ''
      const text = decodeURIComponent(String(scene))
      if (/^\d+$/.test(text)) return text
      const params = new URLSearchParams(text)
      return params.get('id') || params.get('artworkId') || ''
    },

    formatRecordDate(baseDate, offsetDays = 0) {
      const source = baseDate ? new Date(baseDate) : new Date()
      if (Number.isNaN(source.getTime())) return '2026.05.11'
      source.setDate(source.getDate() + offsetDays)
      const year = source.getFullYear()
      const month = String(source.getMonth() + 1).padStart(2, '0')
      const day = String(source.getDate()).padStart(2, '0')
      return `${year}.${month}.${day}`
    },
    escapeXml(value) {
      return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&apos;')
    },
    normalizeIdentityList(value) {
      if (Array.isArray(value)) {
        return value.map(item => String(item).trim().toLowerCase()).filter(Boolean)
      }
      if (typeof value === 'string') {
        try {
          return this.normalizeIdentityList(JSON.parse(value))
        } catch (e) {
          return value.split(',').map(item => item.trim().toLowerCase()).filter(Boolean)
        }
      }
      return []
    },

    initPriceGrowth(data) {
      if (!data) return
      const rise = Number(data.priceRise || data.dailyIncreaseRate || 0)
      const price = Number(this.resolveCurrentPrice(data) || 0)
      this.priceGrowth = {
        growthRate: price > 0 && rise > 0 ? `+${(rise * 100).toFixed(1)}%` : '+0%',
        collectCount: data.collectCount || data.favoriteCount || 0,
        nextCondition: '每新增10人喜欢，作品价格可能上涨0.5%'
      }
    },

    async loadCommission(productId) {
      if (!this.isCurrentUserPromoter) {
        this.commission = 0
        this.commissionLevels = []
        return
      }
      try {
        const res = await getProductCommission(productId)
        const rate = res.commissionRate || res.rate || this.detail.commissionRate || 5
        const priceYuan = Number(this.displayPrice || 0)
        this.commission = Math.floor(priceYuan * rate) / 100
        this.commissionLevels = this.buildCommissionLevels(res, rate)
      } catch (e) {
        const rate = this.detail.commissionRate || 5
        const priceYuan = Number(this.displayPrice || 0)
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

    previewDetailImage(index) {
      uni.previewImage({
        current: index,
        urls: this.detailImages
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

    /**
     * 带自动重试机制的异步执行函数
     * @param {Function} fn - 要执行的异步函数（接收 attempt 序号）
     * @param {Object} [options]
     * @param {number} [options.maxRetries=3] - 最大尝试次数（含首次）
     * @param {number} [options.delay=1000] - 重试间隔(ms)
     * @param {string} [options.label='操作'] - 操作名称（日志用）
     * @returns {Promise<any>}
     */
    async withRetry(fn, { maxRetries = 3, delay = 1000, label = '操作' } = {}) {
      let lastError = null
      for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
          return await fn(attempt)
        } catch (error) {
          lastError = error
          const isServerBusy = error.message && (
            error.message.includes('系统繁忙') ||
            error.message.includes('服务器内部错误') ||
            error.message.includes('服务暂不可用') ||
            error.message.includes('503') ||
            error.message.includes('502') ||
            error.message.includes('timeout') ||
            error.message.includes('超时')
          )
          // 非服务器繁忙类错误（如参数错误、权限不足）直接抛出，不重试
          if (!isServerBusy && attempt < maxRetries) {
            console.warn(`[${label}] 不可重试错误，放弃剩余重试:`, error.message)
            throw error
          }
          if (attempt < maxRetries) {
            console.warn(`[${label}] 第${attempt}次尝试失败，${delay}ms后重试:`, error.message)
            await new Promise(r => setTimeout(r, delay))
          }
        }
      }
      throw lastError
    },

    async handleDirectBuy() {
      const userStore = useUserStore()
      // 双重检查：Pinia store 可能在 handleAuthFailure 后仍存旧值
      if (!userStore.isLogin || userStore.userInfo?.isGuest || !getAccessToken()) {
        saveRedirectUrl(getCurrentPagePath())
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      const id = this.detail?.id
      if (!id) {
        uni.showToast({ title: '作品信息不完整', icon: 'none' })
        return
      }

      // 防止重复点击
      if (this.buyLoading) {
        uni.showToast({ title: '正在创建订单，请勿重复操作', icon: 'none' })
        return
      }

      if (this.activeResaleListing?.id) {
        uni.navigateTo({ url: this.buildResaleConfirmUrl(this.activeResaleListing) })
        return
      }

      if (this.canPublishResale) {
        const suggestedPrice = Math.round(Number(this.displayPrice || this.detail.price || 0))
        uni.navigateTo({ url: `/pages/resale/publish?artworkId=${id}&price=${suggestedPrice}` })
        return
      }

      uni.navigateTo({ url: `/pages/order/confirm?artworkId=${id}&quantity=1` })
    },

    async triggerPriceOnCollect() {
      try {
        const newPrice = await triggerCollectIncrease(this.detail.id)
        const oldPrice = this.displayPrice
        const changeRate = newPrice && oldPrice ? ((newPrice - oldPrice) / oldPrice * 100) : 0.5
        if (newPrice) this.detail.currentPrice = newPrice
        this.priceGrowth.collectCount = (this.priceGrowth.collectCount || 0) + 1
        this.priceGrowth.growthRate = `+${changeRate.toFixed(1)}%`
        uni.showToast({ title: '喜欢成功，作品热度提升', icon: 'none' })
      } catch (e) {
        console.warn('收藏触发涨价失败', e)
      }
    },

    onShare() {
      this.showSharePanel = true
      this.showSharePoster = false
    },

    buildMiniProgramSharePayload() {
      const artworkId = this.detail?.id || ''
      const titleParts = [this.workName, this.authorName].filter(Boolean)
      return {
        title: titleParts.length ? `${titleParts.join(' - ')}｜艺本艺术` : '艺本艺术｜作品详情',
        path: artworkId ? `/pages/gallery/detail?id=${encodeURIComponent(artworkId)}&from=share` : '/pages/gallery/index',
        imageUrl: this.images[0] || this.detail?.coverImage || this.detail?.cover || ''
      }
    },

    shareToFriend() {
      this.openSharePoster('wechat')
    },

    shareToTimeline() {
      this.openSharePoster('timeline')
    },

    copyLink() {
      const app = getApp()
      const link = this.buildShareLink()
      uni.setClipboardData({
        data: link,
        success: () => {
          uni.showToast({ title: '链接已复制', icon: 'success' })
          this.showSharePanel = false
        }
      })
    },

    openSharePoster(channel = 'wechat') {
      this.sharePosterChannel = channel
      this.showSharePoster = true
      this.loadMiniProgramCode()
    },

    closeSharePanel() {
      this.showSharePoster = false
      this.showSharePanel = false
    },

    buildShareLink() {
      if (!this.detail.id) return buildH5ShareUrl('/pages/gallery/index?from=share')
      return buildH5ShareUrl(`/pages/gallery/detail?id=${encodeURIComponent(this.detail.id)}&from=share`)
    },

    updateShareMeta() {
      if (!this.detail.id) return
      setH5ShareMeta({
        title: this.buildMiniProgramSharePayload().title,
        description: `${this.authorName}的作品《${this.workName}》，查看作品详情与流通记录`,
        imageUrl: this.images[0] || this.detail.coverImage || this.detail.cover,
        url: this.buildShareLink()
      })
    },

    async loadMiniProgramCode() {
      if (!this.detail.id || this.miniProgramCodeImage || this.miniProgramCodeLoading) return
      this.miniProgramCodeLoading = true
      this.miniProgramCodeError = ''
      try {
        if (IS_MP_WEIXIN) {
          const miniCode = await userApi.getMiniProgramCode({
            page: 'pages/gallery/detail',
            scene: `id=${this.detail.id}`,
            width: 280
          })
          this.miniProgramCodeImage = this.resolveMiniProgramCodeUrl(miniCode)
        } else {
          this.miniProgramCodeImage = await QRCode.toDataURL(this.buildShareLink(), {
            errorCorrectionLevel: 'M',
            margin: 1,
            width: 280,
            color: {
              dark: '#0f395d',
              light: '#ffffff'
            }
          })
        }
        if (!this.miniProgramCodeImage) {
          throw new Error(IS_MP_WEIXIN ? '小程序码为空' : 'H5二维码为空')
        }
      } catch (e) {
        console.warn(IS_MP_WEIXIN ? '生成小程序码失败' : '生成H5二维码失败', e)
        this.miniProgramCodeError = e?.message || '生成失败'
      } finally {
        this.miniProgramCodeLoading = false
      }
    },

    resolveMiniProgramCodeUrl(payload) {
      const candidates = [
        payload?.miniCodeUrl,
        payload?.minicodeUrl,
        payload?.qrCodeUrl,
        payload?.codeUrl,
        payload?.url,
        payload?.imageUrl,
        payload?.data?.miniCodeUrl,
        payload?.data?.minicodeUrl,
        payload?.data?.qrCodeUrl,
        payload?.data?.codeUrl,
        payload?.data?.url,
        payload?.data?.imageUrl,
        payload?.base64,
        payload?.data?.base64
      ].filter(Boolean)

      const raw = candidates[0] || ''
      if (!raw) return ''
      if (String(raw).startsWith('data:image')) return raw
      if (/^[A-Za-z0-9+/=]+$/.test(String(raw)) && String(raw).length > 120) {
        return `data:image/png;base64,${raw}`
      }
      return String(raw)
    },

    openMiniProgram() {
      this.openSharePoster('mini')
    },

    shareToWeibo() {
      this.openSharePoster('timeline')
    },

    openInBrowser() {
      const link = this.buildShareLink()
      if (typeof window !== 'undefined') {
        window.open(link, '_blank', 'noopener,noreferrer')
        this.showSharePanel = false
        return
      }
      this.copyLink()
    },

    downloadQrCode() {
      this.openSharePoster('qrcode')
    },

    async saveSharePoster() {
      // #ifdef MP-WEIXIN
      await this.saveSharePosterToAlbum()
      return
      // #endif
      uni.showToast({ title: '当前环境请长按图片保存', icon: 'none' })
    },

    async saveSharePosterToAlbum() {
      if (this.sharePosterSaving) return
      if (!this.posterArtworkImage) {
        uni.showToast({ title: '图片未加载完成', icon: 'none' })
        return
      }

      this.sharePosterSaving = true
      uni.showLoading({ title: '保存中...' })

      try {
        if (!this.miniProgramCodeImage) {
          await this.loadMiniProgramCode()
        }

        const [artImage, qrImage] = await Promise.all([
          this.getPosterImageInfo(this.posterArtworkImage),
          this.getPosterImageInfo(this.miniProgramCodeImage)
        ])

        await this.drawSharePosterCanvas(artImage.path || artImage.tempFilePath, qrImage.path || qrImage.tempFilePath)
        const tempFilePath = await this.exportSharePosterCanvas()
        await this.saveImageWithPermission(tempFilePath)
        uni.showToast({ title: '分享图已保存', icon: 'success' })
      } catch (error) {
        console.warn('保存分享图失败', error)
        uni.showToast({ title: error?.message || '保存失败', icon: 'none' })
      } finally {
        this.sharePosterSaving = false
        uni.hideLoading()
      }
    },

    getPosterImageInfo(src) {
      return new Promise((resolve, reject) => {
        if (!src) {
          reject(new Error('分享图片未准备好'))
          return
        }
        uni.getImageInfo({
          src,
          success: resolve,
          fail: (err) => reject(err || new Error('图片读取失败'))
        })
      })
    },

    drawSharePosterCanvas(artworkPath, qrPath) {
      return new Promise((resolve) => {
        const ctx = uni.createCanvasContext('sharePosterCanvas', this)
        const width = 750
        const height = 1334

        ctx.setFillStyle('#0c0c0f')
        ctx.fillRect(0, 0, width, height)

        ctx.setFillStyle('#17171d')
        ctx.fillRect(36, 48, 678, 1238)

        ctx.drawImage(artworkPath, 66, 96, 618, 640)

        ctx.setFillStyle('rgba(0,0,0,0.22)')
        ctx.fillRect(66, 626, 618, 110)

        ctx.setFillStyle('#D8B253')
        ctx.setFontSize(46)
        ctx.fillText('艺本艺术', 90, 156)

        ctx.setFillStyle('#ffffff')
        ctx.setFontSize(28)
        ctx.fillText(this.certificateCode || '', 90, 690)

        ctx.setFillStyle('#111111')
        ctx.fillRect(66, 736, 618, 430)

        ctx.setFillStyle('#ffffff')
        ctx.setFontSize(54)
        ctx.fillText(this.clipCanvasText(this.workName, 12), 90, 828)

        ctx.setFillStyle('#8f8f96')
        ctx.setFontSize(28)
        ctx.fillText(this.clipCanvasText(`${this.authorName} · ${this.artworkMaterial} · ${this.artworkSize} · ${this.artworkYear}`, 34), 90, 884)

        ctx.setFillStyle('#888888')
        ctx.setFontSize(24)
        ctx.fillText(this.activeResaleListing ? '转售价格' : '当前收藏价', 90, 964)

        ctx.setFillStyle('#F4C74F')
        ctx.setFontSize(66)
        ctx.fillText(`¥${this.priceNumber}`, 90, 1036)

        ctx.setFillStyle('#3b321a')
        this.fillRoundRect(ctx, 518, 930, 140, 116, 16)
        ctx.setFillStyle('#F4C74F')
        ctx.setFontSize(44)
        ctx.fillText(this.totalGainDisplay || '+0%', 548, 992)
        ctx.setFontSize(22)
        ctx.fillText('累计上涨', 564, 1032)

        ctx.setFillStyle('#17171d')
        ctx.fillRect(66, 1066, 618, 188)

        ctx.setFillStyle('#ffffff')
        ctx.setFontSize(38)
        ctx.fillText(this.sharePosterCodeTitle, 90, 1142)

        ctx.setFillStyle('#8f8f96')
        ctx.setFontSize(24)
        ctx.fillText(this.clipCanvasText(this.sharePosterCodeDesc, 26), 90, 1188)

        ctx.setFillStyle('#ffffff')
        ctx.fillRect(530, 1096, 116, 116)
        ctx.drawImage(qrPath, 538, 1104, 100, 100)

        ctx.draw(false, () => {
          setTimeout(resolve, 160)
        })
      })
    },

    exportSharePosterCanvas() {
      return new Promise((resolve, reject) => {
        uni.canvasToTempFilePath({
          canvasId: 'sharePosterCanvas',
          quality: 1,
          success: (res) => resolve(res.tempFilePath),
          fail: (err) => reject(err || new Error('分享图导出失败'))
        }, this)
      })
    },

    saveImageWithPermission(filePath) {
      return new Promise((resolve, reject) => {
        uni.saveImageToPhotosAlbum({
          filePath,
          success: resolve,
          fail: (err) => {
            const errMsg = String(err?.errMsg || '')
            if (errMsg.includes('auth deny') || errMsg.includes('authorize no response')) {
              uni.showModal({
                title: '需要相册权限',
                content: '请允许保存图片到相册，以便保存分享图。',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    uni.openSetting({})
                  }
                }
              })
              reject(new Error('请先开启相册权限'))
              return
            }
            reject(err || new Error('保存到相册失败'))
          }
        })
      })
    },

    fillRoundRect(ctx, x, y, width, height, radius) {
      const r = Math.max(0, Math.min(radius, width / 2, height / 2))
      ctx.beginPath()
      ctx.moveTo(x + r, y)
      ctx.lineTo(x + width - r, y)
      ctx.arcTo(x + width, y, x + width, y + r, r)
      ctx.lineTo(x + width, y + height - r)
      ctx.arcTo(x + width, y + height, x + width - r, y + height, r)
      ctx.lineTo(x + r, y + height)
      ctx.arcTo(x, y + height, x, y + height - r, r)
      ctx.lineTo(x, y + r)
      ctx.arcTo(x, y, x + r, y, r)
      ctx.closePath()
      ctx.fill()
    },

    clipCanvasText(text, maxLength = 24) {
      const raw = String(text || '')
      return raw.length > maxLength ? `${raw.slice(0, maxLength)}...` : raw
    },

    reportArtwork() {
      this.showSharePanel = false
      uni.showModal({
        title: '举报作品',
        content: '是否提交该作品的举报反馈？',
        confirmText: '提交',
        success: (res) => {
          if (res.confirm) uni.showToast({ title: '已收到反馈', icon: 'none' })
        }
      })
    },

    showShareModal() {
      this.showSharePanel = true
      this.showSharePoster = false
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
      const query = [
        `userId=${encodeURIComponent(this.detail.authorId || '')}`,
        `workId=${encodeURIComponent(this.detail.id || this.detail.artworkId || '')}`,
        `workTitle=${encodeURIComponent(this.workName || '')}`,
        `workCover=${encodeURIComponent(this.detail.coverImage || this.detail.cover || '')}`,
        `workPrice=${encodeURIComponent(this.priceNumber || '')}`
      ].filter(item => !item.endsWith('='))
      uni.navigateTo({
        url: `/pages/message/chat?${query.join('&')}`
      })
    },

    goArtistHome() {
      const authorId = this.detail.authorId || this.detail.authorUid || ''
      uni.navigateTo({
        url: `/pages/artist/home?userId=${authorId}`
      })
    },

    goProfileHome() {
      if (this.profileCardIsHolder) {
        const holderId = this.detail.holderId || this.activeResaleListing?.sellerUserId || ''
        uni.navigateTo({ url: `/pages/artist/home?userId=${holderId}` })
        return
      }
      this.goArtistHome()
    },

    goRelatedWork(id) {
      if (!id || id === this.detail.id) return
      uni.navigateTo({ url: `/pages/gallery/detail?id=${id}` })
    },

    buildArtworkImages(data) {
      const candidates = [
        data.cover,
        data.coverImage,
        ...this.extractImageList(data.images)
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

    buildDetailImages(data) {
      const coverUrls = new Set([data.cover, data.coverImage].map(this.normalizeResourceUrl).filter(Boolean))
      const seen = new Set()
      return this.extractImageList(data.images)
        .map(this.normalizeResourceUrl)
        .filter(Boolean)
        .filter(url => !coverUrls.has(url))
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
        return text.split(',').map(item => item.trim()).filter(Boolean)
      }
    },

    normalizeResourceUrl(url) {
      if (!url || typeof url !== 'string') return ''
      let text = url.trim()
      if (!text || text === '[]' || text === '{}' || text === 'null' || text === 'undefined') return ''
      const duplicated = text.match(/^(?:https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?)?\/uploads\/upload\/(.+)$/)
      if (duplicated) text = `/upload/${duplicated[1]}`
      const localUpload = text.match(/^https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?(\/upload\/.+)$/)
      if (localUpload) text = localUpload[1]
      // 已经是完整 URL（http/https），直接返回
      if (text.startsWith('http://') || text.startsWith('https://')) return text
      // 本地静态资源（/static/），直接返回
      if (text.startsWith('/static/')) return text
      // /upload/ 开头的相对路径，拼接文件服务器地址
      if (text.startsWith('/upload/')) {
        const app = getApp()
        const domain = app?.globalData?.fileDomain || app?.globalData?.domain || ''
        return domain ? `${domain}${text}` : text
      }
      // 其他相对路径
      if (text.startsWith('/')) {
        const app = getApp()
        const domain = app?.globalData?.fileDomain || app?.globalData?.domain || ''
        return domain ? `${domain}${text}` : text
      }
      return text
    },

    normalizeAvatarUrl(url) {
      if (!url || typeof url !== 'string') return ''
      const text = url.trim()
      if (!text || text === '[]' || text === '{}' || text === 'null' || text === 'undefined') return ''
      const lower = text.toLowerCase()
      if (
        lower.includes('default-avatar') ||
        lower.includes('/static/avatar/demo') ||
        lower.endsWith('/images/avatar.png') ||
        lower.endsWith('/static/images/avatar.png')
      ) {
        return ''
      }
      return this.normalizeResourceUrl(text)
    },

    cleanArtworkLabel(value) {
      if (!value || typeof value !== 'string') return ''
      return value.replace(/分类[:：]?\s*/g, '').trim()
    },

    decodeDisplayText(value) {
      if (!value) return ''
      const text = String(value)
      if (!/%[0-9A-Fa-f]{2}/.test(text)) return text
      try {
        return decodeURIComponent(text)
      } catch (e) {
        return text
      }
    },

    extractMaterial(value) {
      const label = this.cleanArtworkLabel(value)
      const match = label.match(/[（(]([^）)]+)[）)]/)
      return match ? match[1] : label
    },

    onArtworkImageError(index) {
      // 防止重复替换（已经替换过的图片不再处理）
      if (this.images[index] === FALLBACK_COVER) return
      const failedUrl = this.images[index]
      const next = [...this.images]
      next[index] = FALLBACK_COVER
      this.images = next
      console.warn('[Detail] 图片加载失败，已切换占位图:', {
        index,
        failedUrl,
        fallback: FALLBACK_COVER
      })
    },

    onDetailImageError(index) {
      const next = [...this.detailImages]
      next.splice(index, 1)
      this.detailImages = next
    },

    onAuthorAvatarError() {
      this.detail.authorAvatar = this.defaultAvatar
      if (this.profileMatchesDetailAuthor && this.artistProfile) {
        this.artistProfile.avatar = this.defaultAvatar
      }
    },

    bumpLikeCount(step) {
      const baseReal = this.detail.realFavoriteCount ?? this.detail.favoriteCount ?? 0
      const baseDisplay = this.detail.displayLikeCount ?? this.detail.likeCount ?? baseReal
      const nextFavorite = Math.max(baseReal + step, 0)
      const nextDisplay = Math.max(baseDisplay + step, 0)
      this.favoriteCountOverride = Math.max(Number(this.displayLikeCount || 0) + step, 0)
      this.detail.realFavoriteCount = nextFavorite
      this.detail.favoriteCount = nextFavorite
      this.detail.displayLikeCount = nextDisplay
      this.detail.likeCount = nextDisplay
    },

    buildCommissionLevels(res, rate) {
      const priceYuan = Number(this.displayPrice || 0)
      const levels = Array.isArray(res?.levels) && res.levels.length
        ? res.levels
        : [
            { name: '普通经纪人', rate },
            { name: '高级经纪人', rate: Number(rate) * 1.2 },
            { name: '合伙人经纪人', rate: Number(rate) * 1.5 }
          ]

      return levels.map(level => {
        const levelRate = Number(level.rate || level.commissionRate || rate || 0)
        const amount = Number(level.amount || level.commission || (priceYuan * levelRate / 100))
        return {
          name: level.name || level.levelName || '经纪人',
          amount,
          amountText: this.formatYuanAmount(amount)
        }
      })
    },

    formatPrice(price) {
      if (!price) return '¥0'
      return formatArtworkPrice(price)
    },

    formatCirculationPrice(price) {
      if (Number(price || 0) <= 0) return '¥0'
      return formatArtworkPrice(price)
    },

    formatPriceSmall(price) {
      if (!price) return '0.00'
      return formatYuanNumber(Number(price))
    },

    formatTime(time) {
      if (!time) return ''
      return String(time).substring(0, 10)
    },

    goResaleMarket() {
      uni.navigateTo({ url: '/pages/resale/market' })
    },

    goResaleDetail() {
      if (!this.activeResaleListing?.id) return
      uni.navigateTo({ url: this.buildResaleConfirmUrl(this.activeResaleListing) })
    },

    buildResaleConfirmUrl(listing = {}) {
      const artworkId = listing.artworkId || this.detail.id || ''
      const resalePrice = listing.resalePrice || 0
      const sellerUid = listing.sellerUid || listing.sellerUserUid || this.detail.holderUid || this.detail.holderUserUid || ''
      const query = [
        `resaleId=${encodeURIComponent(listing.id || '')}`,
        `artworkId=${encodeURIComponent(artworkId)}`,
        `resalePrice=${encodeURIComponent(resalePrice)}`,
        `artworkUid=${encodeURIComponent(this.certificateCode || '')}`,
        `sellerUid=${encodeURIComponent(sellerUid)}`
      ].join('&')
      return `/pages/order/confirm?${query}`
    },

    formatPriceDelta(price) {
      const value = Number(price || 0)
      if (value <= 0) return '¥0'
      return formatArtworkPrice(value)
    },

    formatDiscountAmount(amount) {
      const value = Number(amount || 0)
      if (value <= 0) return '0'
      return Number.isInteger(value)
        ? value.toLocaleString('zh-CN')
        : value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },

    resolveCurrentPrice(item = {}) {
      const currentPrice = Number(item.currentPrice || item.current_price || item.displayPrice || 0)
      if (currentPrice > 0) return currentPrice
      return Number(item.price || 0)
    },

    formatYuanAmount(amount) {
      return formatYuanAmountShared(amount)
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
  padding: calc(var(--nav-total-height, 72px) + 14rpx) 28rpx calc(164rpx + env(safe-area-inset-bottom));
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
  height: var(--nav-total-height, 72px);
  padding: var(--status-bar-height, 24px) var(--nav-right-reserve, 28rpx) 0 28rpx;
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

.sold-status {
  color: $gold-bright;
  font-size: 34rpx;
  font-weight: 700;
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
  padding: 18rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(18, 18, 18, 0.95);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18rpx);
}

.certificate-sign-tip {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(128rpx + env(safe-area-inset-bottom));
  z-index: 61;
  padding: 20rpx 22rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border: 1rpx solid rgba(213, 169, 28, 0.28);
  border-radius: 18rpx;
  background: linear-gradient(135deg, rgba(48, 34, 9, 0.96), rgba(28, 22, 12, 0.96));
  box-shadow: 0 16rpx 34rpx rgba(0, 0, 0, 0.24);
}

.certificate-sign-copy {
  flex: 1;
  min-width: 0;
}

.certificate-sign-title {
  display: block;
  font-size: 25rpx;
  font-weight: 700;
  color: #f4d27a;
}

.certificate-sign-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: rgba(255, 244, 218, 0.82);
}

.certificate-sign-action {
  flex-shrink: 0;
  min-width: 120rpx;
  height: 62rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
  color: #2b1d03;
  background: linear-gradient(135deg, #f1ce4a 0%, #d5a91c 100%);
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
}

.contact-modal {
  background: rgba(0, 0, 0, 0.62);
}

.share-modal {
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.08) 0%, rgba(0, 0, 0, 0.22) 46%, rgba(0, 0, 0, 0.76) 100%);
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

.share-content {
  padding: 18rpx 0 0;
  border-radius: 0;
  background: rgba(18, 18, 19, 0.98);
  box-shadow: 0 -24rpx 70rpx rgba(0, 0, 0, 0.45);
  overflow: hidden;
}

.share-handle {
  width: 72rpx;
  height: 6rpx;
  margin: 0 auto 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.18);
}

.poster-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 0 28rpx 20rpx;
}

.poster-sheet-title,
.poster-sheet-subtitle {
  display: block;
}

.poster-sheet-title {
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
}

.poster-sheet-subtitle {
  margin-top: 6rpx;
  color: rgba(255, 255, 255, 0.48);
  font-size: 21rpx;
}

.poster-sheet-back {
  height: 48rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(255, 255, 255, 0.12);
  border-radius: 999rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 22rpx;
}

.share-poster-card {
  width: min(596rpx, calc(100vw - 72rpx));
  margin: 0 auto 24rpx;
  overflow: hidden;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 20% 0%, rgba(242, 200, 91, 0.22), transparent 38%),
    linear-gradient(180deg, #171717 0%, #0a0a0a 100%);
  border: 1rpx solid rgba(242, 200, 91, 0.2);
  box-shadow: 0 28rpx 90rpx rgba(0, 0, 0, 0.42);
}

.share-poster-art {
  position: relative;
  height: 560rpx;
  background: #222;
}

.share-poster-art image {
  width: 100%;
  height: 100%;
  display: block;
}

.poster-art-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.38), transparent 30%, rgba(0, 0, 0, 0.7) 100%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.36), transparent 44%);
}

.poster-brand {
  position: absolute;
  left: 28rpx;
  top: 26rpx;
  color: #f5d56c;
  font-size: 28rpx;
  font-weight: 900;
  letter-spacing: 6rpx;
}

.poster-serial {
  position: absolute;
  left: 28rpx;
  right: 28rpx;
  bottom: 24rpx;
  color: rgba(255, 255, 255, 0.68);
  font-size: 21rpx;
  overflow-wrap: anywhere;
}

.share-poster-info {
  padding: 28rpx 30rpx 24rpx;
}

.poster-title-row,
.poster-price-row,
.share-poster-footer,
.poster-actions {
  display: flex;
  align-items: center;
}

.poster-title-row,
.poster-price-row,
.share-poster-footer {
  justify-content: space-between;
  gap: 20rpx;
}

.poster-work-title {
  min-width: 0;
  flex: 1;
  color: #fff;
  font-size: 36rpx;
  font-weight: 900;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poster-tag {
  flex: 0 0 auto;
  padding: 6rpx 14rpx;
  border: 1rpx solid rgba(242, 200, 91, 0.5);
  border-radius: 999rpx;
  color: #f2c85b;
  font-size: 19rpx;
  font-weight: 800;
}

.poster-author {
  display: block;
  margin-top: 14rpx;
  color: rgba(255, 255, 255, 0.58);
  font-size: 22rpx;
  line-height: 1.35;
}

.poster-price-row {
  margin-top: 28rpx;
}

.poster-price-label,
.poster-price,
.poster-rise text,
.poster-copy-title,
.poster-copy-desc {
  display: block;
}

.poster-price-label {
  color: rgba(255, 255, 255, 0.44);
  font-size: 20rpx;
}

.poster-price {
  margin-top: 6rpx;
  color: #f5d56c;
  font-size: 44rpx;
  font-weight: 900;
  line-height: 1;
}

.poster-rise {
  min-width: 116rpx;
  padding: 12rpx 16rpx;
  border-radius: 16rpx;
  background: rgba(242, 200, 91, 0.1);
  text-align: center;
}

.poster-rise text:first-child {
  color: #f5d56c;
  font-size: 28rpx;
  font-weight: 900;
}

.poster-rise text:last-child {
  margin-top: 3rpx;
  color: rgba(255, 255, 255, 0.44);
  font-size: 18rpx;
}

.share-poster-footer {
  padding: 24rpx 30rpx 28rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.03);
}

.poster-copy {
  min-width: 0;
  flex: 1;
}

.poster-copy-title {
  color: #fff;
  font-size: 25rpx;
  font-weight: 900;
}

.poster-copy-desc {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.45);
  font-size: 20rpx;
  line-height: 1.35;
}

.mini-code-box {
  flex: 0 0 auto;
  width: 144rpx;
  height: 144rpx;
  padding: 8rpx;
  border-radius: 18rpx;
  background: #fff;
  text-align: center;
  box-sizing: border-box;
}

.mini-code-box.is-loading,
.mini-code-box.is-error {
  background: #eef5ff;
}

.mini-code-box.is-error {
  border: 2rpx solid rgba(214, 63, 63, 0.28);
}

.mini-code-image {
  display: block;
  width: 128rpx;
  height: 128rpx;
  margin: 0 auto;
}

.mini-code-mark {
  position: relative;
  width: 128rpx;
  height: 128rpx;
  margin: 0 auto;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, #111 0 8rpx, transparent 9rpx),
    conic-gradient(from 22deg, #111 0 18deg, transparent 18deg 38deg, #111 38deg 61deg, transparent 61deg 90deg, #111 90deg 118deg, transparent 118deg 148deg, #111 148deg 176deg, transparent 176deg 216deg, #111 216deg 250deg, transparent 250deg 282deg, #111 282deg 312deg, transparent 312deg 360deg);
}

.mini-code-ring,
.mini-code-dot {
  position: absolute;
  border-radius: 50%;
  background: #111;
}

.mini-code-ring {
  background: transparent;
  border: 6rpx solid #111;
}

.ring-a {
  left: 14rpx;
  top: 16rpx;
  width: 28rpx;
  height: 28rpx;
}

.ring-b {
  right: 16rpx;
  bottom: 18rpx;
  width: 24rpx;
  height: 24rpx;
}

.mini-code-dot {
  width: 10rpx;
  height: 10rpx;
}

.dot-a { right: 18rpx; top: 24rpx; }
.dot-b { left: 24rpx; bottom: 22rpx; }
.dot-c { left: 48rpx; top: 48rpx; }

.poster-actions {
  gap: 16rpx;
  padding: 0 28rpx 28rpx;
}

.poster-action {
  flex: 1;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(255, 255, 255, 0.12);
  border-radius: 999rpx;
  color: rgba(255, 255, 255, 0.82);
  font-size: 25rpx;
  font-weight: 800;
}

.poster-action.primary {
  border-color: rgba(242, 200, 91, 0.48);
  background: linear-gradient(135deg, #f2cf5b 0%, #d6a92b 100%);
  color: #1c1607;
}

.share-title,
.contact-title {
  font-size: 30rpx;
  font-weight: 800;
  text-align: center;
}

.commission-levels {
  margin: 0 28rpx 22rpx;
  padding: 18rpx 22rpx;
  border: 1rpx solid rgba(213, 169, 28, 0.18);
  border-radius: 14rpx;
  background: rgba(213, 169, 28, 0.08);
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
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 34rpx 0;
  padding: 26rpx 22rpx 36rpx;
}

.share-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
  color: rgba(255, 255, 255, 0.46);
  font-size: 21rpx;
  line-height: 1.2;
  text-align: center;
}

.share-icon-item text {
  width: 100%;
  white-space: nowrap;
  transform: scale(0.92);
  transform-origin: center top;
}

.share-icon {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #020202;
  color: #fff;
  font-size: 34rpx;
  font-weight: 800;
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.04);
}

.share-icon image {
  width: 50rpx;
  height: 50rpx;
  display: block;
}

.share-close {
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
  background: #050505;
  color: #f1f1f1;
  font-size: 30rpx;
  font-weight: 800;
}

.hidden-share-canvas {
  position: fixed;
  left: -9999px;
  top: -9999px;
  width: 750px;
  height: 1334px;
  opacity: 0;
  pointer-events: none;
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
  grid-template-columns: 1fr;
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

.detail-page {
  min-height: 100vh;
  padding: calc(var(--nav-total-height, 72px) + 12rpx) 0 calc(168rpx + env(safe-area-inset-bottom));
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
  height: var(--nav-total-height, 72px);
  padding: var(--status-bar-height, 24px) var(--nav-right-reserve, 34rpx) 0 34rpx;
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

.new-chip.is-collected {
  opacity: 0.92;
  color: #f5d56c;
  border-color: rgba(245, 213, 108, 0.78);
  background: rgba(36, 29, 13, 0.72);
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

.market-card.is-sold .market-content {
  margin-top: 22rpx;
  align-items: stretch;
}

.price-block {
  min-height: 216rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.price-block.is-sold {
  min-height: 202rpx;
  justify-content: flex-start;
  padding: 22rpx 24rpx;
  border: 1rpx solid rgba(240, 198, 93, 0.12);
  border-radius: 14rpx;
  background: rgba(240, 198, 93, 0.045);
  box-sizing: border-box;
}

.price-block.is-sold .price {
  margin-top: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8rpx;
  line-height: 1.2;
}

.price-block.is-sold .collect-line {
  margin-top: 18rpx;
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

.discount-line {
  align-self: flex-start;
  margin-top: 14rpx;
  padding: 8rpx 14rpx;
  border-radius: 6rpx;
  background: rgba(231, 76, 60, 0.14);
  color: #ff7a68;
  font-size: 22rpx;
  line-height: 1.25;
  font-weight: 700;
}

.collector-name {
  color: rgba(255, 255, 255, 0.66);
  font-size: 22rpx;
  line-height: 1.35;
  font-weight: 500;
  text-shadow: none;
  word-break: break-all;
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
  grid-template-columns: minmax(0, 1fr) 94rpx;
  gap: 12rpx;
  margin-top: 8rpx;
  align-items: center;
}

.model-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.model-price {
  color: #f0c65d;
  font-size: 26rpx;
  line-height: 1.2;
  font-weight: 800;
  white-space: nowrap;
  letter-spacing: -0.5rpx;
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
  justify-self: end;
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

.artist-avatar-wrap {
  position: relative;
  width: 92rpx;
  height: 92rpx;
}

.artist-avatar-lg {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(212, 160, 52, 0.56);
  background: #2a2a2a;
}

.artist-verify-badge {
  position: absolute;
  right: -2rpx;
  bottom: 4rpx;
  width: 28rpx;
  height: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(180deg, #f3ce67, #d8aa45);
  color: #181818;
  font-size: 16rpx;
  font-weight: 900;
  box-shadow: 0 0 0 2rpx rgba(17, 17, 17, 0.9);
}

.artist-info-block {
  min-width: 0;
  padding-right: 0;
}

.artist-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10rpx;
}

.artist-name {
  max-width: 100%;
  min-width: 0;
  color: #f5f5f5;
  font-size: 27rpx;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.artist-meta-row {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.artist-subtitle {
  display: block;
  min-width: 0;
  flex: 1;
}

.artist-uid {
  min-width: 0;
  flex: 0 1 auto;
  color: rgba(255, 255, 255, 0.34);
  font-size: 16rpx;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  grid-column: 2;
  justify-self: flex-start;
  position: static;
  height: 42rpx;
  margin-top: 14rpx;
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
  grid-template-columns: minmax(0, 1fr);
  gap: 16rpx;
  margin-top: 22rpx;
  align-items: stretch;
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
  grid-template-columns: 132rpx minmax(0, 1fr) auto;
  gap: 12rpx;
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
  min-width: 0;
  white-space: nowrap;
}

.record-price {
  text-align: right;
  color: rgba(255, 255, 255, 0.64);
  white-space: nowrap;
  margin-right: 0;
}

.record-price.current {
  color: #f0c65d;
  font-weight: 800;
}

.gain-card {
  min-height: 102rpx;
  padding: 16rpx 18rpx 14rpx;
  position: relative;
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

.sparkline-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.sparkline-fill {
  opacity: 0.95;
}

.sparkline-line {
  fill: none;
  stroke: url(#gainLineGradient);
  stroke-width: 4.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  filter: drop-shadow(0 0 8rpx rgba(240, 198, 93, 0.22));
}

.sparkline-dot {
  fill: #f0c65d;
  filter: drop-shadow(0 0 8rpx rgba(240, 198, 93, 0.46));
}

.cert-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 168rpx;
  gap: 14rpx;
  align-items: center;
  margin-top: 22rpx;
}

.cert-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx 16rpx;
}

.cert-item {
  display: grid;
  grid-template-columns: 34rpx 1fr;
  gap: 8rpx;
  min-width: 0;
  align-items: center;
}

.cert-item-code {
  grid-column: 1 / -1;
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
  line-height: 1.35;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-all;
}

.cert-preview {
  width: 188rpx;
  height: 132rpx;
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

.detail-image-list {
  margin-top: 22rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.detail-image {
  width: 100%;
  border-radius: 14rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
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
  bottom: 0;
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
  transition: opacity 0.25s ease;
}

.collect-btn.is-loading {
  opacity: 0.7;
  pointer-events: none;
}

.collect-btn.is-sold-out {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.55);
  font-weight: 600;
  box-shadow: none;
  pointer-events: none;
}

.collect-btn[disabled] {
  opacity: 0.55;
  pointer-events: none;
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

  .market-card.is-sold .market-content {
    margin-top: 18rpx;
    align-items: stretch;
  }

  .price-block {
    min-height: 196rpx;
  }

  .price-block.is-sold {
    min-height: 190rpx;
    padding: 18rpx;
  }

  .model-panel {
    padding: 16rpx;
  }

  .model-body {
    grid-template-columns: minmax(0, 1fr) 82rpx;
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
    grid-template-columns: minmax(0, 1fr);
    gap: 16rpx;
  }

  .gain-card {
    width: 100%;
    min-height: 102rpx;
    justify-self: stretch;
  }

  .record-item {
    grid-template-columns: 122rpx minmax(0, 1fr) auto;
    gap: 8rpx;
    font-size: 20rpx;
  }

  .cert-body {
    grid-template-columns: minmax(0, 1fr) 150rpx;
    gap: 12rpx;
  }

  .cert-preview {
    width: 176rpx;
    height: 124rpx;
    justify-self: end;
  }

  .cert-list {
    gap: 10rpx 12rpx;
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

/* 转售数据卡片 */
.resale-card { margin-top: 16rpx; }
.resale-body { padding: 8rpx 0; }
.resale-stat-row { display: flex; flex-wrap: wrap; gap: 8rpx; }
.resale-stat { flex: 1; min-width: 100rpx; text-align: center; padding: 12rpx 0; }
.resale-stat-value { display: block; font-size: 30rpx; font-weight: 600; color: $gold; }
.resale-stat-label { display: block; font-size: 20rpx; color: $text-dim; margin-top: 4rpx; }

</style>
