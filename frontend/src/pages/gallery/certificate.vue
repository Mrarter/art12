<template>
  <view class="page certificate-page">
    <view class="custom-header">
      <view class="header-action back-button" @click="goBack">
        <view class="back-icon"></view>
      </view>
      <text class="header-title">收藏证书</text>
      <view class="header-action"></view>
    </view>

    <scroll-view class="scroll-content" scroll-y>
      <view v-if="loading" class="state-card">证书生成中...</view>
      <view v-else-if="!detail.id" class="state-card">未找到证书信息</view>

      <template v-else>
        <view class="signature-card" v-if="isCurrentAuthor">
          <view class="signature-head">
            <view>
              <text class="signature-title">艺术家手写签名</text>
              <text class="signature-desc">上传手写签名图片后，证书右下角的艺术家签名区域会实时更新。</text>
            </view>
            <view class="signature-state">{{ signatureDataUrl ? '已签署' : '待上传' }}</view>
          </view>
          <view class="signature-body">
            <view class="signature-preview-box">
              <image v-if="signatureDataUrl" class="signature-preview" :src="signatureDataUrl" mode="aspectFit" />
              <text v-else class="signature-placeholder">上传后的签名会显示在这里</text>
            </view>
            <view class="signature-actions">
              <button class="signature-btn primary" @click="chooseSignatureImage">{{ signatureDataUrl ? '更换签名' : '上传签名' }}</button>
              <button class="signature-btn" v-if="signatureDataUrl" @click="clearSignatureImage">清除</button>
            </view>
          </view>
        </view>

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
import { removeCertificateSignNoticesByArtwork } from '@/utils/certificateNotice'
import QRCode from 'qrcode'

const FALLBACK_COVER = '/static/images/artwork-fallback.png'

export default {
  data() {
    return {
      detail: {},
      coverDataUrl: '',
      signatureDataUrl: '',
      qrCodeDataUrl: '',
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
    isCurrentAuthor() {
      return String(this.detail.authorId || '') === String(this.currentUserId || '')
    },
    signatureStorageKey() {
      return `artistSignature:${this.authorCode}`
    },
    certificateNo() {
      return `CERT-${String(this.artworkCode).toUpperCase()}-${String(this.holderCode).toUpperCase()}`
    },
    certificatePageUrl() {
      const route = `/#/pages/gallery/certificate?id=${this.detail.id || ''}`
      if (typeof window === 'undefined' || !window.location?.origin) {
        return route
      }
      return `${window.location.origin}${route}`
    },
    detailPageUrl() {
      return `/pages/gallery/detail?id=${this.detail.id || ''}`
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
      const qrCodeMarkup = this.qrCodeDataUrl
        ? `<rect x="1232" y="132" width="156" height="156" rx="12" fill="#fffaf0" stroke="#d8c8aa" stroke-width="4"/>
           <image href="${this.escapeXml(this.qrCodeDataUrl)}" x="1242" y="142" width="136" height="136" preserveAspectRatio="xMidYMid meet"/>`
        : `<circle cx="1310" cy="210" r="78" fill="none" stroke="#d8c8aa" stroke-width="5" opacity="0.52"/>
           <circle cx="1310" cy="210" r="58" fill="none" stroke="#d8c8aa" stroke-width="2" opacity="0.52"/>
           <text x="1310" y="226" text-anchor="middle" font-family="Georgia, serif" font-size="58" fill="#d8c8aa" opacity="0.6">A</text>`

      const signatureName = this.escapeXml(this.artistRealName)
      const signatureMarkup = this.signatureDataUrl
        ? `<image href="${this.escapeXml(this.signatureDataUrl)}" x="1208" y="836" width="320" height="136" preserveAspectRatio="xMidYMid meet"/>`
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
          <text x="800" y="246" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="82" fill="#4d2e16">收藏证书</text>
          <text x="800" y="308" text-anchor="middle" font-family="Georgia, serif" font-size="26" letter-spacing="10" fill="#624421">ARTWORK COLLECTION CERTIFICATE</text>

          ${qrCodeMarkup}

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

          <g transform="translate(740 928)">
            <circle cx="60" cy="28" r="28" fill="none" stroke="#b99356" stroke-width="4"/>
            <path d="M44 38 C50 20 60 12 78 8 C72 18 68 28 66 44" fill="none" stroke="#b99356" stroke-width="4.5" stroke-linecap="round"/>
            <path d="M42 18 C52 24 60 34 62 48" fill="none" stroke="#d8bf8b" stroke-width="3.2" stroke-linecap="round"/>
            <circle cx="82" cy="10" r="4" fill="#d8bf8b"/>
            <text x="60" y="86" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="24" fill="#b99356">艺本艺术</text>
            <text x="60" y="114" text-anchor="middle" font-family="STKaiti, KaiTi, serif" font-size="16" fill="#c2a06a" letter-spacing="3">YIBEN ART</text>
          </g>

          <defs>
            <path id="seal-top-arc" d="M 1014 956 A 66 66 0 0 1 1146 956" />
            <path id="seal-bottom-arc" d="M 1032 986 A 52 52 0 0 0 1128 986" />
          </defs>
          <circle cx="1080" cy="952" r="96" fill="none" stroke="#c63f32" stroke-width="7"/>
          <circle cx="1080" cy="952" r="86" fill="none" stroke="#dd6a5a" stroke-width="1.8" opacity="0.72"/>
          <text font-family="FangSong, STFangsong, serif" font-size="22" fill="#c63f32" letter-spacing="2.2">
            <textPath href="#seal-top-arc" startOffset="50%" text-anchor="middle">艺本艺术鉴定中心</textPath>
          </text>
          <polygon points="1080,922 1090,946 1116,946 1095,961 1103,986 1080,971 1057,986 1065,961 1044,946 1070,946" fill="#d54436"/>
          <text font-family="FangSong, STFangsong, serif" font-size="14" fill="#c85a4d" letter-spacing="0.4">
            <textPath href="#seal-bottom-arc" startOffset="50%" text-anchor="middle">${artworkCode}</textPath>
          </text>

          ${signatureMarkup}
          <text x="1484" y="1002" text-anchor="end" font-family="STKaiti, KaiTi, serif" font-size="20" fill="#4f3217">签发编号：${certificateNo}</text>
          <text x="1484" y="1038" text-anchor="end" font-family="STKaiti, KaiTi, serif" font-size="20" fill="#4f3217">签发日期：${collectDate}</text>
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
        await this.loadSignatureDataUrl()
        await this.generateCertificateQrCode()
      } catch (error) {
        console.error('加载收藏证书失败', error)
      } finally {
        this.loading = false
      }
    },
    async generateCertificateQrCode() {
      if (!this.detail.id) {
        this.qrCodeDataUrl = ''
        return
      }
      try {
        this.qrCodeDataUrl = await QRCode.toDataURL(this.certificatePageUrl, {
          errorCorrectionLevel: 'M',
          margin: 1,
          width: 136,
          color: {
            dark: '#3f2b18',
            light: '#fffaf0'
          }
        })
      } catch (error) {
        console.warn('证书二维码生成失败', error)
        this.qrCodeDataUrl = ''
      }
    },
    goBack() {
      const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
      const previousPage = pages.length > 1 ? pages[pages.length - 2] : null
      const previousRoute = previousPage?.route || ''

      if (previousRoute && previousRoute !== 'pages/gallery/certificate') {
        uni.navigateBack()
        return
      }

      if (this.detail.id) {
        uni.redirectTo({ url: this.detailPageUrl })
        return
      }

      uni.switchTab({ url: '/pages/index/index' })
    },
    async loadCoverDataUrl() {
      if (typeof fetch === 'undefined') return
      try {
        this.coverDataUrl = await this.loadRemoteAsDataUrl(this.cover)
      } catch (error) {
        console.warn('封面转码失败，使用原始地址', error)
      }
    },
    async loadRemoteAsDataUrl(url) {
      if (typeof fetch === 'undefined' || !url) return ''
      const response = await fetch(url)
      const blob = await response.blob()
      return await new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = reject
        reader.readAsDataURL(blob)
      })
    },
    async loadSignatureDataUrl() {
      const savedUrl = uni.getStorageSync(this.signatureStorageKey) || ''
      if (!savedUrl) {
        this.signatureDataUrl = ''
        return
      }
      if (this.isDataUrl(savedUrl)) {
        this.signatureDataUrl = savedUrl
        return
      }
      try {
        this.signatureDataUrl = await this.loadRemoteAsDataUrl(savedUrl)
      } catch (error) {
        console.warn('签名转码失败，使用原始地址', error)
        this.signatureDataUrl = savedUrl
      }
    },
    isDataUrl(value) {
      return typeof value === 'string' && value.startsWith('data:image/')
    },
    async processSignatureImage(filePath) {
      if (typeof document === 'undefined') return filePath
      const image = new Image()
      image.crossOrigin = 'anonymous'
      image.src = filePath
      await new Promise((resolve, reject) => {
        image.onload = resolve
        image.onerror = reject
      })

      const canvas = document.createElement('canvas')
      canvas.width = image.naturalWidth || image.width
      canvas.height = image.naturalHeight || image.height
      const ctx = canvas.getContext('2d', { willReadFrequently: true })
      ctx.drawImage(image, 0, 0, canvas.width, canvas.height)

      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
      const { data, width, height } = imageData
      let minX = width
      let minY = height
      let maxX = -1
      let maxY = -1

      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const index = (y * width + x) * 4
          const r = data[index]
          const g = data[index + 1]
          const b = data[index + 2]
          const alpha = data[index + 3]
          const max = Math.max(r, g, b)
          const min = Math.min(r, g, b)
          const saturation = max === 0 ? 0 : (max - min) / max
          const luminance = 0.299 * r + 0.587 * g + 0.114 * b
          const isNearWhite = luminance > 238
          const isLightGray = luminance > 220 && saturation < 0.12
          const isPaperTint = luminance > 198 && saturation < 0.18

          if (alpha === 0 || isNearWhite || isLightGray || isPaperTint) {
            data[index + 3] = 0
            continue
          }

          if (x < minX) minX = x
          if (y < minY) minY = y
          if (x > maxX) maxX = x
          if (y > maxY) maxY = y
        }
      }

      ctx.putImageData(imageData, 0, 0)

      if (maxX < minX || maxY < minY) {
        return canvas.toDataURL('image/png')
      }

      const padding = 12
      const cropX = Math.max(minX - padding, 0)
      const cropY = Math.max(minY - padding, 0)
      const cropWidth = Math.min(maxX - minX + padding * 2 + 1, width - cropX)
      const cropHeight = Math.min(maxY - minY + padding * 2 + 1, height - cropY)

      const cropCanvas = document.createElement('canvas')
      cropCanvas.width = cropWidth
      cropCanvas.height = cropHeight
      const cropCtx = cropCanvas.getContext('2d')
      cropCtx.drawImage(canvas, cropX, cropY, cropWidth, cropHeight, 0, 0, cropWidth, cropHeight)

      const maxWidth = 560
      const maxHeight = 180
      const scale = Math.min(maxWidth / cropWidth, maxHeight / cropHeight, 1)
      const outputWidth = Math.max(Math.round(cropWidth * scale), 1)
      const outputHeight = Math.max(Math.round(cropHeight * scale), 1)

      const outputCanvas = document.createElement('canvas')
      outputCanvas.width = outputWidth
      outputCanvas.height = outputHeight
      const outputCtx = outputCanvas.getContext('2d')
      outputCtx.drawImage(cropCanvas, 0, 0, cropWidth, cropHeight, 0, 0, outputWidth, outputHeight)
      return outputCanvas.toDataURL('image/png')
    },
    persistSignature(dataUrl) {
      if (!dataUrl) return
      uni.setStorageSync(this.signatureStorageKey, dataUrl)
      if (this.detail?.id && this.detail?.authorId) {
        removeCertificateSignNoticesByArtwork(this.detail.id, this.detail.authorId)
      }
    },
    async chooseSignatureImage() {
      if (!this.isCurrentAuthor) return
      try {
        const chooseRes = await new Promise((resolve, reject) => {
          uni.chooseImage({
            count: 1,
            sizeType: ['compressed'],
            sourceType: ['album', 'camera'],
            success: resolve,
            fail: reject
          })
        })
        const filePath = chooseRes?.tempFilePaths?.[0]
        if (!filePath) return
        uni.showLoading({ title: '处理中...' })
        const processedSignature = await this.processSignatureImage(filePath)
        this.persistSignature(processedSignature)
        this.signatureDataUrl = processedSignature
        uni.showToast({ title: '签名已更新', icon: 'success' })
      } catch (error) {
        console.error('上传签名失败', error)
        const message = String(error?.message || '')
        const isQuotaError = message.includes('quota') || message.includes('QuotaExceededError')
        uni.showToast({ title: isQuotaError ? '签名过大，请换更小图片' : (error?.message || '签名上传失败'), icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    clearSignatureImage() {
      uni.removeStorageSync(this.signatureStorageKey)
      this.signatureDataUrl = ''
      uni.showToast({ title: '已清除签名', icon: 'none' })
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
    async renderCertificatePngBlob() {
      const image = new Image()
      image.decoding = 'sync'
      image.src = this.certificateImageUrl

      await new Promise((resolve, reject) => {
        image.onload = resolve
        image.onerror = () => reject(new Error('证书图片加载失败'))
      })

      if (typeof image.decode === 'function') {
        await image.decode().catch(() => {})
      }

      const canvas = document.createElement('canvas')
      canvas.width = 1600
      canvas.height = 1120

      const ctx = canvas.getContext('2d')
      if (!ctx) throw new Error('无法创建画布')

      ctx.fillStyle = '#fcf8ef'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      ctx.drawImage(image, 0, 0, canvas.width, canvas.height)

      return await new Promise((resolve, reject) => {
        if (typeof canvas.toBlob === 'function') {
          canvas.toBlob((blob) => {
            if (blob) resolve(blob)
            else reject(new Error('证书生成失败'))
          }, 'image/png')
          return
        }

        try {
          const dataUrl = canvas.toDataURL('image/png')
          const base64 = dataUrl.split(',')[1]
          const byteString = atob(base64)
          const uint8Array = new Uint8Array(byteString.length)
          for (let i = 0; i < byteString.length; i++) {
            uint8Array[i] = byteString.charCodeAt(i)
          }
          resolve(new Blob([uint8Array], { type: 'image/png' }))
        } catch (error) {
          reject(error)
        }
      })
    },
    async blobToDataUrl(blob) {
      return await new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = reject
        reader.readAsDataURL(blob)
      })
    },
    async saveBlobToFile(blob, fileName) {
      const link = document.createElement('a')
      const supportsDownload = typeof link.download === 'string'
      if (supportsDownload) {
        const objectUrl = URL.createObjectURL(blob)
        try {
          link.href = objectUrl
          link.download = fileName
          link.rel = 'noopener'
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          return {
            mode: 'download',
            message: `已开始下载 ${fileName}`,
            objectUrl
          }
        } catch (error) {
          URL.revokeObjectURL(objectUrl)
          throw error
        }
      }

      const dataUrl = await this.blobToDataUrl(blob)
      if (typeof location !== 'undefined') {
        location.href = dataUrl
      }
      return {
        mode: 'preview',
        message: '当前环境不支持直接下载，已打开 PNG 预览'
      }
    },
    async downloadCertificate() {
      if (typeof document === 'undefined') {
        uni.showToast({ title: '当前环境暂不支持下载', icon: 'none' })
        return
      }

      try {
        const pngBlob = await this.renderCertificatePngBlob()
        const fileName = `${this.certificateNo}.png`
        const result = await this.saveBlobToFile(pngBlob, fileName)
        uni.showToast({ title: result.message, icon: 'none' })
        if (result.objectUrl) {
          window.setTimeout(() => URL.revokeObjectURL(result.objectUrl), 1000)
        }
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
  height: 64rpx;
}

.back-button {
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.04);
  border: 1rpx solid rgba(212, 175, 55, 0.18);
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.03);
}

.back-icon {
  width: 18rpx;
  height: 18rpx;
  border-left: 4rpx solid #f6f1e8;
  border-bottom: 4rpx solid #f6f1e8;
  transform: translateX(4rpx) rotate(45deg);
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
.signature-card,
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

.signature-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.signature-title,
.signature-desc {
  display: block;
}

.signature-title {
  font-size: 28rpx;
  font-weight: 600;
}

.signature-desc {
  margin-top: 8rpx;
  color: rgba(246, 241, 232, 0.68);
  font-size: 22rpx;
  line-height: 1.6;
}

.signature-state {
  flex-shrink: 0;
  min-width: 108rpx;
  height: 52rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(212, 175, 55, 0.14);
  color: #d4af37;
  font-size: 22rpx;
  font-weight: 600;
}

.signature-body {
  margin-top: 22rpx;
}

.signature-preview-box {
  min-height: 160rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  border: 1rpx dashed rgba(212, 175, 55, 0.28);
  background: rgba(255, 255, 255, 0.03);
}

.signature-preview {
  width: 100%;
  max-width: 420rpx;
  height: 120rpx;
}

.signature-placeholder {
  color: rgba(246, 241, 232, 0.42);
  font-size: 22rpx;
}

.signature-actions {
  margin-top: 18rpx;
  display: flex;
  gap: 14rpx;
}

.signature-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 12rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  color: #f6f1e8;
  font-size: 24rpx;
}

.signature-btn.primary {
  border: 0;
  background: #d4af37;
  color: #17120a;
  font-weight: 600;
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
