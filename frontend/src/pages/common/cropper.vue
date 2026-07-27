<template>
  <view class="cropper-page">
    <!-- 图片容器 -->
    <view class="cropper-view" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
      <image
        class="cropper-image"
        :src="imageSrc"
        :style="imageStyle"
        mode="widthFix"
      />
      <!-- 遮罩层 -->
      <view class="mask-layer">
        <view class="mask-top" :style="{ top: 0, left: 0, right: 0, height: cropTop + 'px' }"></view>
        <view class="mask-left" :style="{ top: cropTop + 'px', left: 0, width: cropLeft + 'px', height: cropH + 'px' }"></view>
        <view class="mask-right" :style="{ top: cropTop + 'px', right: 0, width: cropRightGap + 'px', height: cropH + 'px' }"></view>
        <view class="mask-bottom" :style="{ bottom: 0, left: 0, right: 0, height: cropBottomGap + 'px' }"></view>
      </view>
      <!-- 裁剪框边框装饰 -->
      <view class="crop-border" :class="{ circle: shape === 'circle' }" :style="{
        left: cropLeft + 'px', top: cropTop + 'px',
        width: cropW + 'px', height: cropH + 'px'
      }">
        <view v-if="shape === 'circle'" class="circle-mask-ring"></view>
        <view class="corner tl"></view>
        <view class="corner tr"></view>
        <view class="corner bl"></view>
        <view class="corner br"></view>
        <view class="size-label">{{ cropW }} × {{ cropH }}</view>
      </view>
    </view>

    <!-- 隐藏画布用于最终裁剪 -->
    <canvas v-if="showCanvas" type="2d" id="cropOutputCanvas" class="hidden-canvas"></canvas>
    <canvas canvas-id="cropLegacyCanvas" class="hidden-canvas"></canvas>

    <!-- 底部工具栏 -->
    <view class="cropper-toolbar">
      <view class="tool-btn" @click="onCancel">
        <text class="tool-icon">✕</text>
        <text class="tool-text">取消</text>
      </view>
      <view class="tool-btn" @click="onRotate">
        <text class="tool-icon">↻</text>
        <text class="tool-text">旋转</text>
      </view>
      <view class="tool-btn" @click="onReset">
        <text class="tool-icon">⟲</text>
        <text class="tool-text">重置</text>
      </view>
      <view class="tool-btn primary" @click="onConfirm">
        <text class="tool-icon">✓</text>
        <text class="tool-text">确认</text>
      </view>
    </view>

    <!-- 比例选择器 -->
    <view class="ratio-bar">
      <view
        v-for="r in ratioOptions"
        :key="r.value"
        class="ratio-item"
        :class="{ active: currentRatio === r.value }"
        @click="switchRatio(r.value)"
      >
        <text>{{ r.label }}</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      imageSrc: '',
      imageNaturalW: 0,
      imageNaturalH: 0,
      containerW: 375, // 设备宽度 px
      containerH: 500, // 设备可用高度 px
      // 裁剪框参数
      cropW: 300,
      cropH: 300,
      cropLeft: 0,
      cropTop: 0,
      // 图片变换（相对于容器中心）
      translateX: 0,
      translateY: 0,
      scale: 1,
      rotation: 0, // 0 | 90 | 180 | 270
      // 触摸状态
      touchStart: null,
      touchStartTwo: null,
      lastDist: 0,
      lastTranslateX: 0,
      lastTranslateY: 0,
      lastScale: 1,
      isTouching: false,
      // 配置
      currentRatio: 'auto',
      shape: 'square',
      outputSize: 800,
      showCanvas: false,
      eventKey: '',
    }
  },

  computed: {
    cropRightGap() {
      return this.containerW - this.cropLeft - this.cropW
    },
    cropBottomGap() {
      return this.containerH - this.cropTop - this.cropH
    },

    imageStyle() {
      const isRot90 = this.rotation === 90 || this.rotation === 270
      const displayW = isRot90 ? this.imageNaturalH : this.imageNaturalW
      const displayH = isRot90 ? this.imageNaturalW : this.imageNaturalH
      const fitScale = this.getImageFitScale(displayW, displayH)
      const baseW = displayW * fitScale
      const baseH = displayH * fitScale
      const w = baseW * this.scale
      const h = baseH * this.scale
      return {
        width: w + 'px',
        height: h + 'px',
        transform: `translate(${this.translateX}px, ${this.translateY}px) rotate(${this.rotation}deg)`,
        marginLeft: -(w / 2) + 'px',
        marginTop: -(h / 2) + 'px',
        left: '50%',
        top: '50%',
        position: 'absolute',
      }
    },

    ratioOptions() {
      if (this.shape === 'circle') {
        return [
          { label: '头像', value: '1:1' },
        ]
      }
      return [
        { label: '原图', value: 'auto' },
        { label: '自由', value: 'free' },
        { label: '1:1', value: '1:1' },
        { label: '4:3', value: '4:3' },
        { label: '3:4', value: '3:4' },
        { label: '16:9', value: '16:9' },
      ]
    },

    // 裁剪框的宽高比 (w/h)
    cropRatio() {
      return this.cropW / this.cropH
    },

    shouldFitWholeImage() {
      return this.currentRatio === 'auto' || this.currentRatio === 'free'
    },
  },

  onLoad(options) {
    this.imageSrc = this.decodeRouteValue(options.src || '')
    this.currentRatio = options.ratio || 'auto'
    this.shape = options.shape || 'square'
    this.eventKey = this.decodeRouteValue(options.eventKey || '')
    if (this.shape === 'circle') this.currentRatio = '1:1'
    this.outputSize = Math.max(200, Math.min(1600, Number(options.outputSize) || 800))

    const sys = uni.getSystemInfoSync()
    this.containerW = sys.windowWidth
    this.containerH = sys.windowHeight - 160 * (sys.windowWidth / 750) - 80 * (sys.windowWidth / 750)
    this.showCanvas = true
    this.loadImageInfo()
  },

  onReady() {
    this.initCropWindow()
  },

  methods: {
    decodeRouteValue(value) {
      let decoded = String(value || '')
      for (let i = 0; i < 3; i += 1) {
        let next = decoded
        try {
          next = decodeURIComponent(decoded)
        } catch (e) {
          break
        }
        if (next === decoded) break
        decoded = next
      }
      return decoded
    },

    loadImageInfo() {
      if (process.env.UNI_PLATFORM === 'h5' && typeof Image !== 'undefined') {
        const image = new Image()
        if (/^https?:\/\//.test(this.imageSrc)) image.crossOrigin = 'anonymous'
        image.onload = () => {
          this.applyImageInfo(image.naturalWidth, image.naturalHeight)
        }
        image.onerror = () => {
          uni.showToast({ title: '图片加载失败', icon: 'none' })
        }
        image.src = this.imageSrc
        return
      }

      uni.getImageInfo({
        src: this.imageSrc,
        success: (res) => {
          this.applyImageInfo(res.width, res.height)
        },
        fail: () => {
          uni.showToast({ title: '图片加载失败', icon: 'none' })
        },
      })
    },

    applyImageInfo(width, height) {
      this.imageNaturalW = width
      this.imageNaturalH = height
      if (this.shouldFitWholeImage) {
        this.initCropWindow()
        this.onReset()
      }
    },

    initCropWindow() {
      const margin = this.containerW * 0.06
      const maxW = this.containerW - margin * 2
      const maxH = this.containerH - margin * 2
      this.calcCropSize(maxW, maxH)
    },

    calcCropSize(maxW, maxH) {
      if (this.currentRatio === 'free') {
        const [rw, rh] = this.getRatioParts()
        if (maxW / maxH > rw / rh) {
          this.cropH = maxH
          this.cropW = this.cropH * rw / rh
        } else {
          this.cropW = maxW
          this.cropH = this.cropW * rh / rw
        }
      } else {
        const [rw, rh] = this.getRatioParts()
        if (maxW / maxH > rw / rh) {
          this.cropH = maxH
          this.cropW = this.cropH * rw / rh
        } else {
          this.cropW = maxW
          this.cropH = this.cropW * rh / rw
        }
      }
      this.cropLeft = (this.containerW - this.cropW) / 2
      this.cropTop = (this.containerH - this.cropH) / 2
    },

    switchRatio(val) {
      if (this.shape === 'circle' && val !== '1:1') return
      this.currentRatio = val
      this.initCropWindow()
      this.onReset()
    },

    getRatioParts() {
      if (this.currentRatio === 'auto' || this.currentRatio === 'free') {
        return [this.imageNaturalW || 1, this.imageNaturalH || 1]
      }
      return this.currentRatio.split(':').map(Number)
    },

    getImageFitScale(displayW, displayH) {
      if (!displayW || !displayH) return 1
      const baseScale = Math.max(this.cropW / displayW, this.cropH / displayH)
      return this.shouldFitWholeImage ? baseScale : baseScale * 1.2
    },

    getOutputSize() {
      const ratio = this.cropRatio || 1
      if (ratio >= 1) {
        return {
          width: this.outputSize,
          height: Math.max(1, Math.round(this.outputSize / ratio))
        }
      }
      return {
        width: Math.max(1, Math.round(this.outputSize * ratio)),
        height: this.outputSize
      }
    },

    // ========== 触摸处理 ==========
    onTouchStart(e) {
      const touches = e.touches
      this.touchStart = touches[0]
      this.lastTranslateX = this.translateX
      this.lastTranslateY = this.translateY
      this.lastScale = this.scale

      if (touches.length === 2) {
        this.touchStartTwo = touches[1]
        this.lastDist = this.getTouchDist(touches)
      }
      this.isTouching = true
    },

    onTouchMove(e) {
      if (!this.isTouching) return
      const touches = e.touches

      if (touches.length === 1 && this.touchStart) {
        const dx = touches[0].clientX - this.touchStart.clientX
        const dy = touches[0].clientY - this.touchStart.clientY
        this.translateX = this.lastTranslateX + dx
        this.translateY = this.lastTranslateY + dy
      } else if (touches.length === 2 && this.touchStartTwo) {
        const dist = this.getTouchDist(touches)
        if (this.lastDist > 0) {
          const ratio = dist / this.lastDist
          this.scale = Math.max(0.3, Math.min(5, this.lastScale * ratio))
        }
      }
    },

    onTouchEnd() {
      this.isTouching = false
      this.touchStart = null
      this.touchStartTwo = null
    },

    getTouchDist(touches) {
      const dx = touches[0].clientX - touches[1].clientX
      const dy = touches[0].clientY - touches[1].clientY
      return Math.sqrt(dx * dx + dy * dy)
    },

    // ========== 操作按钮 ==========
    onRotate() {
      this.rotation = (this.rotation + 90) % 360
    },

    onReset() {
      this.translateX = 0
      this.translateY = 0
      this.scale = 1
      this.rotation = 0
    },

    onCancel() {
      uni.navigateBack()
    },

    async onConfirm() {
      uni.showLoading({ title: '裁剪中...' })
      try {
        const result = await this.doCrop()
        const pages = getCurrentPages()
        const currentPage = pages[pages.length - 1]
        if (currentPage && currentPage.$page && currentPage.$page.eventChannel) {
          currentPage.$page.eventChannel.emit('onCrop', result)
        }
        uni.$emit(this.eventKey || 'cropResult', result)
        uni.hideLoading()
        uni.navigateBack()
      } catch (e) {
        uni.hideLoading()
        console.error('裁剪失败', e)
        uni.showToast({ title: '裁剪失败，请重试', icon: 'none' })
      }
    },

    // ========== 核心裁剪 ==========
    doCrop() {
      return new Promise((resolve, reject) => {
        if (process.env.UNI_PLATFORM === 'h5') {
          this.cropOnH5().then(resolve).catch(reject)
        } else {
          this.cropOnMP().then(resolve).catch(reject)
        }
      })
    },

    // 计算原始图片中的裁剪区域
    calcCropRegion(naturalW, naturalH) {
      const isRot90 = this.rotation === 90 || this.rotation === 270
      const refW = isRot90 ? naturalH : naturalW
      const refH = isRot90 ? naturalW : naturalH

      const fitScale = this.getImageFitScale(refW, refH)
      const baseW = refW * fitScale
      const baseH = refH * fitScale
      const scaledW = baseW * this.scale
      const scaledH = baseH * this.scale

      // 图片中心在容器坐标系中
      const imgCX = this.containerW / 2 + this.translateX
      const imgCY = this.containerH / 2 + this.translateY

      // 图片左上角
      const imgLeft = imgCX - scaledW / 2
      const imgTop = imgCY - scaledH / 2

      // 裁剪框相对于图片的偏移
      const relX = this.cropLeft - imgLeft
      const relY = this.cropTop - imgTop

      // 转换为原始图片坐标
      const pixToNat = refW > 0 ? naturalW / refW : 1
      const displayScale = fitScale * this.scale

      let srcX = Math.max(0, relX / displayScale * pixToNat)
      let srcY = Math.max(0, relY / displayScale * pixToNat)
      let srcW = Math.min(naturalW - srcX, this.cropW / displayScale * pixToNat)
      let srcH = Math.min(naturalH - srcY, this.cropH / displayScale * pixToNat)

      // 限制不超出图片边界
      if (srcX + srcW > naturalW) srcW = naturalW - srcX
      if (srcY + srcH > naturalH) srcH = naturalH - srcY

      return { srcX, srcY, srcW, srcH }
    },

    // H5 裁剪
    cropOnH5() {
      return new Promise((resolve, reject) => {
        const img = new Image()
        if (/^https?:\/\//.test(this.imageSrc)) img.crossOrigin = 'anonymous'
        img.onload = () => {
          try {
            const { srcX, srcY, srcW, srcH } = this.calcCropRegion(img.naturalWidth, img.naturalHeight)
            const { width: outW, height: outH } = this.getOutputSize()

            const canvas = document.createElement('canvas')
            canvas.width = outW
            canvas.height = outH
            const ctx = canvas.getContext('2d')

            if (this.rotation % 180 !== 0) {
              ctx.translate(outW / 2, outH / 2)
              ctx.rotate((this.rotation * Math.PI) / 180)
              ctx.drawImage(img, srcX, srcY, srcW, srcH, -outH / 2, -outW / 2, outH, outW)
            } else {
              ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, outW, outH)
            }

            canvas.toBlob((blob) => {
              if (!blob) return reject(new Error('裁剪失败'))
              resolve(URL.createObjectURL(blob))
            }, 'image/jpeg', 0.92)
          } catch (e) {
            reject(e)
          }
        }
        img.onerror = reject
        img.src = this.imageSrc
      })
    },

    // 小程序裁剪
    cropOnMP() {
      return new Promise((resolve, reject) => {
        const query = uni.createSelectorQuery()
        query.select('#cropOutputCanvas').fields({ node: true, size: true }).exec((res) => {
          if (!res || !res[0]) {
            return this.cropOnMPLegacy().then(resolve).catch(reject)
          }
          const canvas = res[0].node
          const ctx = canvas.getContext('2d')
          const { width: outW, height: outH } = this.getOutputSize()
          canvas.width = outW
          canvas.height = outH

          const img = canvas.createImage()
          img.onload = () => {
            try {
              const { srcX, srcY, srcW, srcH } = this.calcCropRegion(this.imageNaturalW, this.imageNaturalH)
              if (this.rotation % 180 !== 0) {
                ctx.translate(outW / 2, outH / 2)
                ctx.rotate((this.rotation * Math.PI) / 180)
                ctx.drawImage(img, srcX, srcY, srcW, srcH, -outH / 2, -outW / 2, outH, outW)
              } else {
                ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, outW, outH)
              }
              uni.canvasToTempFilePath({
                canvas,
                destWidth: outW,
                destHeight: outH,
                success: (r) => resolve(r.tempFilePath),
                fail: reject,
              })
            } catch (e) {
              reject(e)
            }
          }
          img.onerror = reject
          img.src = this.imageSrc
        })
      })
    },

    // 小程序旧版 canvas API 回退
    cropOnMPLegacy() {
      return new Promise((resolve, reject) => {
        const { width: outW, height: outH } = this.getOutputSize()
        const ctx = uni.createCanvasContext('cropLegacyCanvas', this)
        const { srcX, srcY, srcW, srcH } = this.calcCropRegion(this.imageNaturalW, this.imageNaturalH)

        ctx.drawImage(this.imageSrc, srcX, srcY, srcW, srcH, 0, 0, outW, outH)
        ctx.draw(false, () => {
          setTimeout(() => {
            uni.canvasToTempFilePath({
              canvasId: 'cropLegacyCanvas',
              x: 0, y: 0,
              width: outW, height: outH,
              destWidth: outW, destHeight: outH,
              success: (r) => resolve(r.tempFilePath),
              fail: reject,
            }, this)
          }, 100)
        })
      })
    },
  },
}
</script>

<style lang="scss" scoped>
.cropper-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #000;
  display: flex;
  flex-direction: column;
  z-index: 9999;
}

.cropper-view {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.cropper-image {
  position: absolute;
  will-change: transform;
}

/* 遮罩层 */
.mask-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;

  view {
    position: absolute;
    background: rgba(0, 0, 0, 0.55);
  }
}

/* 裁剪框边框装饰 */
.crop-border {
  position: absolute;
  pointer-events: none;
}

.crop-border.circle {
  border: 4rpx solid rgba(255, 255, 255, 0.92);
  border-radius: 50%;
  box-shadow:
    0 0 0 2rpx rgba(255, 255, 255, 0.16),
    inset 0 0 0 2rpx rgba(255, 255, 255, 0.18),
    0 0 32rpx rgba(0, 0, 0, 0.28);

  .corner {
    display: none;
  }

  .size-label {
    bottom: 16rpx;
    padding: 2rpx 12rpx;
    border-radius: 999rpx;
    background: rgba(0, 0, 0, 0.32);
  }
}

.circle-mask-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.55);
}

.corner {
  position: absolute;
  width: 40rpx;
  height: 40rpx;
  border-color: #fff;
  border-style: solid;

  &.tl { top: -4rpx; left: -4rpx; border-width: 6rpx 0 0 6rpx; }
  &.tr { top: -4rpx; right: -4rpx; border-width: 6rpx 6rpx 0 0; }
  &.bl { bottom: -4rpx; left: -4rpx; border-width: 0 0 6rpx 6rpx; }
  &.br { bottom: -4rpx; right: -4rpx; border-width: 0 6rpx 6rpx 0; }
}

.size-label {
  position: absolute;
  bottom: -48rpx;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.6);
  font-size: 22rpx;
  white-space: nowrap;
}

.hidden-canvas {
  position: fixed;
  left: -9999px;
  top: -9999px;
  width: 800px;
  height: 800px;
}

/* 底部工具栏 */
.cropper-toolbar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 20rpx 40rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #1a1a1a;
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);
}

.tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 24rpx;
  border-radius: 16rpx;

  &.primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 12rpx 40rpx;
  }
}

.tool-icon {
  font-size: 40rpx;
  color: #fff;
  line-height: 1;
}

.tool-text {
  font-size: 22rpx;
  color: #ccc;
  margin-top: 6rpx;
}

.tool-btn.primary .tool-text {
  color: #fff;
}

/* 比例选择器 */
.ratio-bar {
  display: flex;
  justify-content: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  padding-bottom: 8rpx;
  background: #1a1a1a;
}

.ratio-item {
  padding: 8rpx 24rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.08);
  border: 1rpx solid rgba(255, 255, 255, 0.12);

  text {
    font-size: 24rpx;
    color: #999;
  }

  &.active {
    background: rgba(102, 126, 234, 0.2);
    border-color: #667eea;
    text { color: #667eea; font-weight: 600; }
  }
}
</style>
