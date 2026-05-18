<template>
  <view class="signature-page">
    <view class="custom-header">
      <view class="header-action" @click="goBack">‹</view>
      <text class="header-title">艺术家签名</text>
      <view class="header-action"></view>
    </view>

    <view class="content">
      <view class="meta-card">
        <text class="meta-label">作者编号</text>
        <text class="meta-value">{{ artistCode }}</text>
      </view>

      <view class="canvas-card">
        <canvas
          id="signatureCanvas"
          class="signature-canvas"
          @touchstart="onTouchStart"
          @touchmove="onTouchMove"
          @touchend="onTouchEnd"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseup="onMouseUp"
          @mouseleave="onMouseUp"
        ></canvas>
        <view class="canvas-hint" v-if="!hasStroke && !savedSignature">请在此处手写签名</view>
        <image v-if="!hasStroke && savedSignature" class="saved-preview" :src="savedSignature" mode="aspectFit" />
      </view>

      <view class="actions">
        <button class="secondary-btn" @click="clearCanvas">清空</button>
        <button class="primary-btn" @click="saveSignature">保存签名</button>
      </view>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user'

export default {
  data() {
    return {
      ctx: null,
      canvas: null,
      drawing: false,
      hasStroke: false,
      savedSignature: ''
    }
  },

  computed: {
    artistCode() {
      const userStore = useUserStore()
      const user = userStore.userInfo || {}
      return user.uid || user.artistUid || this.formatIdentity('USR', user.id || user.userId)
    }
  },

  onReady() {
    this.initCanvas()
    this.savedSignature = uni.getStorageSync(`artistSignature:${this.artistCode}`) || ''
  },

  methods: {
    initCanvas() {
      if (typeof document === 'undefined') return
      const canvas = document.getElementById('signatureCanvas')
      if (!canvas) return
      const rect = canvas.getBoundingClientRect()
      const ratio = window.devicePixelRatio || 1
      canvas.width = Math.max(rect.width, 1) * ratio
      canvas.height = Math.max(rect.height, 1) * ratio
      const ctx = canvas.getContext('2d')
      ctx.scale(ratio, ratio)
      ctx.lineWidth = 3
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.strokeStyle = '#1f1308'
      this.canvas = canvas
      this.ctx = ctx
    },
    getPoint(event) {
      const touch = event.touches?.[0] || event.changedTouches?.[0]
      const rect = this.canvas.getBoundingClientRect()
      return { x: touch.clientX - rect.left, y: touch.clientY - rect.top }
    },
    onTouchStart(event) {
      if (!this.ctx || !this.canvas) return
      const point = this.getPoint(event)
      this.drawing = true
      this.hasStroke = true
      this.ctx.beginPath()
      this.ctx.moveTo(point.x, point.y)
    },
    onTouchMove(event) {
      if (!this.drawing || !this.ctx) return
      const point = this.getPoint(event)
      this.ctx.lineTo(point.x, point.y)
      this.ctx.stroke()
    },
    onTouchEnd() {
      this.drawing = false
    },
    onMouseDown(event) {
      if (!this.ctx || !this.canvas) return
      const rect = this.canvas.getBoundingClientRect()
      this.drawing = true
      this.hasStroke = true
      this.ctx.beginPath()
      this.ctx.moveTo(event.clientX - rect.left, event.clientY - rect.top)
    },
    onMouseMove(event) {
      if (!this.drawing || !this.ctx || !this.canvas) return
      const rect = this.canvas.getBoundingClientRect()
      this.ctx.lineTo(event.clientX - rect.left, event.clientY - rect.top)
      this.ctx.stroke()
    },
    onMouseUp() {
      this.drawing = false
    },
    clearCanvas() {
      if (!this.ctx || !this.canvas) return
      const rect = this.canvas.getBoundingClientRect()
      this.ctx.clearRect(0, 0, rect.width, rect.height)
      this.hasStroke = false
      this.savedSignature = ''
      uni.removeStorageSync(`artistSignature:${this.artistCode}`)
    },
    saveSignature() {
      if (!this.canvas || !this.hasStroke) {
        uni.showToast({ title: '请先手写签名', icon: 'none' })
        return
      }
      const dataUrl = this.canvas.toDataURL('image/png')
      uni.setStorageSync(`artistSignature:${this.artistCode}`, dataUrl)
      this.savedSignature = dataUrl
      uni.showToast({ title: '签名已保存', icon: 'none' })
    },
    goBack() {
      uni.navigateBack()
    },
    formatIdentity(prefix, value) {
      if (!value) return `${prefix}000000000000`
      const digits = String(value).replace(/\D/g, '')
      return `${prefix}${digits.padStart(12, '0')}`
    }
  }
}
</script>

<style scoped>
.signature-page { min-height: 100vh; background: #0b0b0b; color: #f6f1e8; }
.custom-header { height: 96rpx; padding: calc(var(--status-bar-height) + 16rpx) 28rpx 12rpx; display: flex; align-items: center; justify-content: space-between; }
.header-action { width: 64rpx; font-size: 52rpx; }
.header-title { font-size: 32rpx; font-weight: 600; }
.content { padding: 20rpx 24rpx; }
.meta-card, .canvas-card { border: 1rpx solid rgba(255,255,255,.08); border-radius: 18rpx; background: #151515; }
.meta-card { padding: 24rpx; }
.meta-label, .meta-value { display: block; }
.meta-label { color: rgba(246,241,232,.58); font-size: 22rpx; }
.meta-value { margin-top: 8rpx; font-size: 28rpx; font-weight: 600; }
.canvas-card { position: relative; margin-top: 20rpx; height: 420rpx; padding: 18rpx; background: #fbf6ea; }
.signature-canvas { width: 100%; height: 100%; display: block; }
.canvas-hint { position: absolute; left: 0; right: 0; top: 50%; color: rgba(31,19,8,.35); text-align: center; transform: translateY(-50%); }
.saved-preview { position: absolute; inset: 18rpx; width: calc(100% - 36rpx); height: calc(100% - 36rpx); }
.actions { margin-top: 22rpx; display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; }
.secondary-btn, .primary-btn { height: 78rpx; line-height: 78rpx; border: 0; border-radius: 12rpx; font-size: 26rpx; }
.secondary-btn { background: rgba(255,255,255,.08); color: #f6f1e8; }
.primary-btn { background: #d4af37; color: #17120a; font-weight: 600; }
</style>
