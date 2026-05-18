<template>
  <view class="page certificate-page">
    <view class="custom-header">
      <view class="header-action" @click="goBack">‹</view>
      <text class="header-title">收藏证书</text>
      <view class="header-action"></view>
    </view>

    <scroll-view class="scroll-content" scroll-y>
      <view v-if="loading" class="state-card">证书生成中...</view>
      <view v-else-if="!detail.id" class="state-card">未找到证书信息</view>

      <template v-else>
        <view class="preview-card">
          <image class="certificate-preview" :src="certificateImageUrl" mode="widthFix" />
          <button class="download-btn" @click="downloadCertificate">下载证书图片</button>
        </view>

        <view class="info-card">
          <view class="info-row">
            <text class="info-label">作品编号</text>
            <text class="info-value">{{ artworkCode }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">作者编号</text>
            <text class="info-value">{{ authorCode }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">持有人编号</text>
            <text class="info-value">{{ holderCode }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">收藏日期</text>
            <text class="info-value">{{ collectDate }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">证书更新依据</text>
            <text class="info-value multiline">作品与当前持有人绑定，换手后自动生成新的证书信息</text>
          </view>
        </view>

        <view class="statement-card">
          <text class="statement-title">平台声明</text>
          <text class="statement-copy">
            本证书用于记录作品当前收藏归属。证书编号由作品编号与当前持有人编号共同生成，
            用于区分同一作品在不同持有人名下的收藏状态。
          </text>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script>
import { getProductDetail } from '@/api/product'
import { useUserStore } from '@/store/modules/user'

const FALLBACK_COVER = '/static/images/artwork-fallback.png'

export default {
  data() {
    return {
      detail: {},
      coverDataUrl: '',
      signatureDataUrl: '',
      loading: true
    }
  },

  computed: {
    artworkCode() {
      return this.detail.artworkUid || this.detail.artworkCode || this.detail.code || `ART-${this.detail.id || ''}`
    },
    authorCode() {
      return this.detail.authorUid || this.detail.displayAuthorId || this.formatIdentity('USR', this.detail.authorId)
    },
    holderIdentity() {
      return this.detail.holderUid || this.detail.holderUserUid || this.detail.holderId || this.currentUserId || 'UNBOUND'
    },
    holderCode() {
      return typeof this.holderIdentity === 'string' && String(this.holderIdentity).startsWith('USR')
        ? this.holderIdentity
        : this.formatIdentity('USR', this.holderIdentity)
    },
    holderName() {
      const userStore = useUserStore()
      const currentUserId = this.currentUserId
      if (this.detail.holderName) return this.detail.holderName
      if (String(this.detail.holderId || '') === String(currentUserId || '')) {
        return userStore.userInfo?.nickname || userStore.userInfo?.realName || '当前用户'
      }
      if (!this.detail.holderId && currentUserId) {
        return userStore.userInfo?.nickname || userStore.userInfo?.realName || '当前用户'
      }
      return this.detail.ownerName || this.detail.publisherName || (this.detail.holderId ? `用户 ${this.detail.holderId}` : '待归属')
    },
    currentUserId() {
      const userStore = useUserStore()
      return userStore.userInfo?.id || userStore.userInfo?.userId || userStore.tokenData?.userId || ''
    },
    certificateNo() {
      return `CERT-${String(this.artworkCode).toUpperCase()}-${String(this.holderCode).toUpperCase()}`
    },
    certificateImageUrl() {
      return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(this.certificateSvg)}`
    },
    certificateSvg() {
      const title = this.escapeXml(this.detail.title || '未命名作品')
      const author = this.escapeXml(this.artistRealName)
      const holder = this.escapeXml(this.holderName)
      const material = this.escapeXml(this.detail.artType || this.detail.categoryName || this.detail.material || '-')
      const size = this.escapeXml(this.detail.size || '-')
      const year = this.escapeXml(this.detail.year || '-')
      const artworkCode = this.escapeXml(this.artworkCode)
      const authorCode = this.escapeXml(this.authorCode)
      const holderCode = this.escapeXml(this.holderCode)
      const collectDate = this.escapeXml(this.collectDate)
      const certificateNo = this.escapeXml(this.certificateNo)
      const cover = this.escapeXml(this.certificateCover)

      const signatureName = this.escapeXml(this.artistRealName)
      const signatureMarkup = this.signatureDataUrl
        ? `<image href="${this.escapeXml(this.signatureDataUrl)}" x="1310" y="882" width="160" height="64" preserveAspectRatio="xMidYMid meet"/>`
        : `<text x="1230" y="930" font-family="STKaiti, KaiTi, serif" font-size="22" fill="#4f3217">艺术家签名：${signatureName}</text>`

      return `
        <svg xmlns="http://www.w3.org/2000/svg" width="1600" height="1120" viewBox="0 0 1600 1120">
          <defs>
            <linearGradient id="paper" x1="0" x2="1" y1="0" y2="1">
              <stop offset="0%" stop-color="#fcf8ef"/>
              <stop offset="100%" stop-color="#f6ecd8"/>
            </linearGradient>
            <linearGradient id="gold" x1="0" x2="1">
              <stop offset="0%" stop-color="#b99356"/>
              <stop offset="50%" stop-color="#d8bf8b"/>
              <stop offset="100%" stop-color="#a17a3f"/>
            </linearGradient>
            <pattern id="waves" width="34" height="18" patternUnits="userSpaceOnUse">
              <path d="M0 9 Q8 3 17 9 T34 9" fill="none" stroke="#ddcfb3" stroke-width="1" opacity="0.35"/>
            </pattern>
            <g id="corner">
              <path d="M0,118 C18,54 48,22 116,0 C72,4 40,12 18,34 C10,54 4,82 0,118Z" fill="none" stroke="#c8a56b" stroke-width="5"/>
              <path d="M18,96 C28,58 54,30 92,18" fill="none" stroke="#c8a56b" stroke-width="3"/>
              <path d="M30,78 C48,78 54,58 72,52 C72,70 58,82 40,90" fill="none" stroke="#c8a56b" stroke-width="3"/>
              <path d="M64,34 C74,42 88,42 98,28" fill="none" stroke="#c8a56b" stroke-width="3"/>
            </g>
          </defs>
          <rect width="1600" height="1120" fill="url(#paper)"/>
          <rect width="1600" height="1120" fill="url(#waves)"/>
          <rect x="28" y="28" width="1544" height="1064" fill="none" stroke="url(#gold)" stroke-width="6"/>
          <rect x="42" y="42" width="1516" height="1036" fill="none" stroke="#ceb078" stroke-width="2"/>
          <use href="#corner" x="42" y="42"/>
          <use href="#corner" transform="translate(1558 42) scale(-1 1)"/>
          <use href="#corner" transform="translate(42 1078) scale(1 -1)"/>
          <use href="#corner" transform="translate(1558 1078) scale(-1 -1)"/>

          <path d="M540 150 H718" stroke="#d7bd86" stroke-width="2"/>
          <path d="M882 150 H1060" stroke="#d7bd86" stroke-width="2"/>
          <path d="M724 150 C754 112 846 112 876 150 C846 188 754 188 724 150Z" fill="none" stroke="#d7bd86" stroke-width="3"/>
          <path d="M754 150 C774 138 826 138 846 150 C826 162 774 162 754 150Z" fill="#d7bd86" opacity="0.62"/>
          <text x="800" y="246" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="82" fill="#4d2e16">美术作品收藏证书</text>
          <text x="800" y="308" text-anchor="middle" font-family="Georgia, serif" font-size="26" letter-spacing="10" fill="#624421">ARTWORK COLLECTION CERTIFICATE</text>

          <circle cx="1310" cy="210" r="78" fill="none" stroke="#d8c8aa" stroke-width="5" opacity="0.52"/>
          <circle cx="1310" cy="210" r="58" fill="none" stroke="#d8c8aa" stroke-width="2" opacity="0.52"/>
          <text x="1310" y="226" text-anchor="middle" font-family="Georgia, serif" font-size="58" fill="#d8c8aa" opacity="0.6">A</text>

          <rect x="168" y="360" width="486" height="352" rx="6" fill="#fffaf0" stroke="#c9a86b" stroke-width="3"/>
          <rect x="154" y="346" width="514" height="380" rx="8" fill="none" stroke="#d8bf8b" stroke-width="2"/>
          <path d="M154 382 H668 M154 690 H668" stroke="#d8bf8b" stroke-width="2" opacity="0.7"/>
          <image href="${cover}" x="186" y="380" width="450" height="314" preserveAspectRatio="xMidYMid slice"/>
          <text x="411" y="768" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="30" fill="#4f3217">《${title}》</text>
          <text x="411" y="808" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="22" fill="#5a4630">${author}</text>
          <text x="411" y="844" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="20" fill="#5a4630">${year}年　|　${material}　|　${size}</text>

          <text x="790" y="388" font-family="STKaiti, KaiTi, serif" font-size="26" fill="#3f2b18">兹证明您收藏的美术作品信息如下：</text>
          ${this.svgLine('收藏者', holder, 446)}
          ${this.svgLine('作品名称', title, 500)}
          ${this.svgLine('艺术家', author, 554)}
          ${this.svgLine('创作年代', `${year}年`, 608)}
          ${this.svgLine('作品材质', material, 662)}
          ${this.svgLine('作品尺寸', size, 716)}
          ${this.svgLine('作品编号', artworkCode, 770)}
          ${this.svgLine('作者编号', authorCode, 824)}
          ${this.svgLine('持有人编号', holderCode, 878)}

          <text x="170" y="948" font-family="STKaiti, KaiTi, serif" font-size="22" fill="#5f4a32">此证书仅证明作品当前收藏归属，不作为作品真伪与价值鉴定依据。</text>
          <text x="170" y="984" font-family="Georgia, serif" font-size="16" fill="#6b573d">This certificate records the current collection ownership of the artwork.</text>

          <g transform="translate(770 940)">
            <path d="M0 68 L0 24 L28 0 L56 24 L56 68" fill="none" stroke="#b99356" stroke-width="5"/>
            <path d="M8 68 H48 M14 24 H42 M14 36 H42 M14 48 H42" stroke="#b99356" stroke-width="4"/>
            <text x="28" y="108" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="22" fill="#b99356">艺术 · 传承 · 价值</text>
          </g>

          <circle cx="1080" cy="952" r="96" fill="none" stroke="#a43d28" stroke-width="5"/>
          <circle cx="1080" cy="952" r="78" fill="none" stroke="#a43d28" stroke-width="2"/>
          <text x="1080" y="940" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#a43d28">艺术鉴定中心</text>
          <text x="1080" y="978" text-anchor="middle" font-family="Georgia, serif" font-size="44" fill="#a43d28">SYJ</text>

          ${signatureMarkup}
          <text x="1230" y="1010" font-family="STKaiti, KaiTi, serif" font-size="22" fill="#4f3217">签发编号：${certificateNo}</text>
          <text x="1230" y="1048" font-family="STKaiti, KaiTi, serif" font-size="22" fill="#4f3217">签发日期：${collectDate}</text>
        </svg>
      `.trim()
    },
    cover() {
      return this.detail.coverImage || this.detail.cover || FALLBACK_COVER
    },
    artistRealName() {
      return this.detail.authorRealName || this.detail.artistRealName || this.detail.realName || this.detail.authorName || '未知艺术家'
    },
    certificateCover() {
      return this.coverDataUrl || this.cover
    },
    artworkMeta() {
      return [this.detail.artType || this.detail.categoryName || this.detail.material, this.detail.size, this.detail.year]
        .filter(Boolean)
        .join(' / ')
    },
    statusText() {
      return Number(this.detail.status) === 2 ? '已收藏' : '有效'
    },
    collectDate() {
      return this.formatDate(this.detail.holderSince || this.detail.updateTime || this.detail.createTime)
    }
  },

  onLoad(options) {
    this.fetchCertificate(options?.id)
  },

  methods: {
    async fetchCertificate(id) {
      if (!id) {
        this.loading = false
        return
      }
      try {
        this.detail = await getProductDetail(id) || {}
        await this.loadCoverDataUrl()
        this.signatureDataUrl = uni.getStorageSync(`artistSignature:${this.authorCode}`) || ''
      } catch (error) {
        console.error('加载收藏证书失败', error)
      } finally {
        this.loading = false
      }
    },
    goBack() {
      uni.navigateBack()
    },
    async loadCoverDataUrl() {
      if (typeof fetch === 'undefined') return
      try {
        const response = await fetch(this.cover)
        const blob = await response.blob()
        this.coverDataUrl = await new Promise((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = () => resolve(reader.result)
          reader.onerror = reject
          reader.readAsDataURL(blob)
        })
      } catch (error) {
        console.warn('封面转码失败，使用原始地址', error)
      }
    },
    formatIdentity(prefix, value) {
      if (!value) return `${prefix}000000000000`
      const digits = String(value).replace(/\D/g, '')
      return `${prefix}${digits.padStart(12, '0')}`
    },
    formatDate(value) {
      const date = value ? new Date(value) : new Date()
      if (Number.isNaN(date.getTime())) return ''
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
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
    svgLine(label, value, y) {
      return `
        <text x="790" y="${y}" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#3f2b18">${this.escapeXml(label)}：</text>
        <text x="930" y="${y}" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#4f3217">${this.escapeXml(value)}</text>
        <line x1="930" y1="${y + 12}" x2="1435" y2="${y + 12}" stroke="#d8c39c" stroke-width="1"/>
      `
    },
    async downloadCertificate() {
      if (typeof document === 'undefined') {
        uni.showToast({ title: '当前环境暂不支持下载', icon: 'none' })
        return
      }

      try {
        const image = new Image()
        image.src = this.certificateImageUrl
        await new Promise((resolve, reject) => {
          image.onload = resolve
          image.onerror = reject
        })

        const canvas = document.createElement('canvas')
        canvas.width = 1600
        canvas.height = 1120
        const ctx = canvas.getContext('2d')
        ctx.drawImage(image, 0, 0, canvas.width, canvas.height)

        canvas.toBlob((blob) => {
          if (!blob) {
            uni.showToast({ title: '证书生成失败', icon: 'none' })
            return
          }
          const url = URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = `${this.certificateNo}.png`
          link.click()
          URL.revokeObjectURL(url)
        }, 'image/png')
      } catch (error) {
        console.error('下载证书失败', error)
        uni.showToast({ title: '下载失败，请稍后重试', icon: 'none' })
      }
    }
  }
}
</script>

<style scoped lang="scss">
.certificate-page {
  min-height: 100vh;
  background: #0b0b0b;
  color: #f6f1e8;
}

.custom-header {
  height: 96rpx;
  padding: calc(var(--status-bar-height) + 16rpx) 28rpx 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-action {
  width: 64rpx;
  font-size: 52rpx;
  line-height: 1;
}

.header-title {
  font-size: 32rpx;
  font-weight: 600;
}

.scroll-content {
  height: calc(100vh - 120rpx);
  box-sizing: border-box;
  padding: 16rpx 24rpx 40rpx;
}

.state-card,
.preview-card,
.info-card,
.statement-card {
  margin-top: 20rpx;
  padding: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 18rpx;
  background: #151515;
}

.preview-card {
  padding: 20rpx;
}

.certificate-preview {
  width: 100%;
  display: block;
  border-radius: 12rpx;
}

.download-btn {
  margin-top: 18rpx;
  height: 78rpx;
  line-height: 78rpx;
  border-radius: 12rpx;
  border: 0;
  background: #d4af37;
  color: #17120a;
  font-size: 26rpx;
  font-weight: 600;
}

.info-row {
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.info-row:last-child {
  border-bottom: 0;
}

.info-label {
  flex: 0 0 150rpx;
  color: rgba(246, 241, 232, 0.58);
  font-size: 22rpx;
}

.info-value {
  flex: 1;
  text-align: right;
  color: #f6f1e8;
  font-size: 24rpx;
  word-break: break-word;
}

.info-value.multiline {
  line-height: 1.55;
}

.statement-title,
.statement-copy {
  display: block;
}

.statement-title {
  font-size: 26rpx;
  font-weight: 600;
}

.statement-copy {
  margin-top: 14rpx;
  color: rgba(246, 241, 232, 0.72);
  font-size: 22rpx;
  line-height: 1.7;
}
</style>
