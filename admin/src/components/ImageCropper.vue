<template>
  <teleport to="body">
    <div v-if="visible" class="cropper-overlay" @click.self="onCancel">
      <div class="cropper-dialog">
        <div class="cropper-header">
          <span class="cropper-title">裁剪图片</span>
          <div class="cropper-actions">
            <el-button size="small" @click="onCancel">取消</el-button>
            <el-button size="small" type="primary" @click="onConfirm" :loading="loading">确认</el-button>
          </div>
        </div>
        <div class="cropper-body">
          <div class="cropper-main">
            <div ref="cropperHost" class="cropper-thumbnail" v-if="previewUrl">
              <img :src="previewUrl" ref="previewImg" class="source-image" />
            </div>
          </div>
          <div class="cropper-sidebar">
            <div class="sidebar-section">
              <div class="sidebar-title">裁剪比例</div>
              <div class="ratio-list">
                <div
                  v-for="r in ratioOptions"
                  :key="r.value"
                  class="ratio-item"
                  :class="{ active: currentRatio === r.value }"
                  @click="switchRatio(r.value)"
                >
                  {{ r.label }}
                </div>
              </div>
            </div>
            <div class="sidebar-section">
              <div class="sidebar-title">操作</div>
              <div class="action-btns">
                <el-button size="small" @click="rotate(-90)">↺ 左旋</el-button>
                <el-button size="small" @click="rotate(90)">↻ 右旋</el-button>
                <el-button size="small" @click="reset">重置</el-button>
              </div>
            </div>
            <div class="sidebar-section">
              <div class="sidebar-title">预览</div>
              <div class="preview-box" :style="previewBoxStyle">
                <div ref="previewBox" class="preview-target"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script>
import Cropper from 'cropperjs'

const createRatioMap = () => ({
  free: NaN,
  '1:1': 1,
  '4:3': 4 / 3,
  '3:4': 3 / 4,
  '16:9': 16 / 9,
})

const OUTPUT_SIZE_MAP = {
  'free': { width: 1600, height: 900 },
  '1:1': { width: 1200, height: 1200 },
  '4:3': { width: 1600, height: 1200 },
  '3:4': { width: 1200, height: 1600 },
  '16:9': { width: 1600, height: 900 },
}

const getRatioMapEntries = (ratioMap) => {
  const safeMap = ratioMap && typeof ratioMap === 'object' ? ratioMap : createRatioMap()
  return Object.entries(safeMap)
}

const findRatioValue = (ratio, ratioMap) => {
  if (!Number.isFinite(ratio) || ratio <= 0) {
    return 'free'
  }

  return getRatioMapEntries(ratioMap).find(([, value]) => {
    return Number.isFinite(value) && Math.abs(value - ratio) < 0.001
  })?.[0] || 'free'
}

const buildTemplate = (aspectRatio) => {
  const ratioAttrs = Number.isFinite(aspectRatio) && aspectRatio > 0
    ? ` aspect-ratio="${aspectRatio}" initial-aspect-ratio="${aspectRatio}"`
    : ''

  return `
    <cropper-canvas background>
      <cropper-image initial-center-size="cover" rotatable scalable translatable></cropper-image>
      <cropper-shade hidden></cropper-shade>
      <cropper-handle action="select" plain></cropper-handle>
      <cropper-selection initial-coverage="0.92"${ratioAttrs} movable resizable>
        <cropper-grid role="grid" bordered covered></cropper-grid>
        <cropper-crosshair centered></cropper-crosshair>
        <cropper-handle action="move" theme-color="rgba(255, 255, 255, 0.35)"></cropper-handle>
        <cropper-handle action="n-resize"></cropper-handle>
        <cropper-handle action="e-resize"></cropper-handle>
        <cropper-handle action="s-resize"></cropper-handle>
        <cropper-handle action="w-resize"></cropper-handle>
        <cropper-handle action="ne-resize"></cropper-handle>
        <cropper-handle action="nw-resize"></cropper-handle>
        <cropper-handle action="se-resize"></cropper-handle>
        <cropper-handle action="sw-resize"></cropper-handle>
      </cropper-selection>
    </cropper-canvas>
  `
}

export default {
  name: 'ImageCropper',
  props: {
    visible: Boolean,
    file: File,
    aspectRatio: { type: Number, default: NaN },
  },
  emits: ['close', 'confirm'],
  data() {
    const ratioMap = createRatioMap()
    const defaultRatio = findRatioValue(this.aspectRatio, ratioMap)
    return {
      previewUrl: '',
      cropper: null,
      loading: false,
      currentRatio: defaultRatio,
      selectionEl: null,
      previewRenderToken: 0,
      selectionListener: null,
      ratioMap,
      ratioOptions: [
        { label: '自由', value: 'free' },
        { label: '1:1', value: '1:1' },
        { label: '4:3', value: '4:3' },
        { label: '3:4', value: '3:4' },
        { label: '16:9', value: '16:9' },
      ],
    }
  },
  watch: {
    visible(val) {
      if (val && this.file) {
        this.$nextTick(() => this.initCropper())
      } else if (!val) {
        this.destroyCropper()
      }
    },
    aspectRatio: {
      immediate: true,
      handler(val) {
        this.currentRatio = findRatioValue(val, this.ratioMap)
      }
    }
  },
  computed: {
    previewBoxStyle() {
      const ratio = this.ratioMap?.[this.currentRatio]
      const width = 150
      if (!Number.isFinite(ratio) || ratio <= 0) {
        return {
          width: `${width}px`,
          height: `${width}px`,
        }
      }

      return {
        width: `${width}px`,
        aspectRatio: String(ratio),
      }
    },
  },
  methods: {
    initCropper() {
      this.destroyCropper()
      this.currentRatio = findRatioValue(this.aspectRatio, this.ratioMap)
      this.previewUrl = URL.createObjectURL(this.file)

      this.$nextTick(() => {
        const img = this.$refs.previewImg
        if (!img) {
          return
        }

        const createCropper = () => {
          this.cropper = new Cropper(img, {
            container: this.$refs.cropperHost,
            template: buildTemplate(this.ratioMap[this.currentRatio]),
          })

          this.$nextTick(() => {
            this.selectionEl = this.cropper?.getCropperSelection?.() || null
            if (this.selectionEl) {
              this.selectionListener = () => {
                this.renderPreview()
              }
              this.selectionEl.addEventListener('change', this.selectionListener)
            }
            this.initializeCropperView()
          })
        }

        if (img.complete && img.naturalWidth > 0) {
          createCropper()
          return
        }

        img.onload = () => {
          img.onload = null
          createCropper()
        }
      })
    },

    destroyCropper() {
      if (this.selectionEl && this.selectionListener) {
        this.selectionEl.removeEventListener('change', this.selectionListener)
      }
      this.selectionEl = null
      this.selectionListener = null
      if (this.cropper) {
        this.cropper.destroy()
        this.cropper = null
      }
      if (this.$refs.previewBox) {
        this.$refs.previewBox.innerHTML = ''
      }
      if (this.previewUrl) {
        URL.revokeObjectURL(this.previewUrl)
        this.previewUrl = ''
      }
    },

    switchRatio(val) {
      this.currentRatio = val
      if (this.selectionEl) {
        const ratio = this.ratioMap[val]
        this.selectionEl.aspectRatio = ratio
        this.selectionEl.initialAspectRatio = ratio
        this.$nextTick(() => this.applyInitialCropBox())
      }
    },

    rotate(deg) {
      const image = this.cropper?.getCropperImage?.()
      if (image?.$rotate) {
        image.$rotate(`${deg}deg`)
        this.$nextTick(() => this.applyInitialCropBox())
      }
    },

    reset() {
      const image = this.cropper?.getCropperImage?.()
      if (image?.$resetTransform) {
        image.$resetTransform()
      }
      if (this.selectionEl?.$reset) {
        this.selectionEl.aspectRatio = this.ratioMap[this.currentRatio]
        this.selectionEl.initialAspectRatio = this.ratioMap[this.currentRatio]
        this.$nextTick(() => this.initializeCropperView())
      }
    },

    initializeCropperView() {
      const image = this.cropper?.getCropperImage?.()
      if (image?.$center) {
        image.$center('contain')
      }
      this.$nextTick(() => this.applyInitialCropBox())
    },

    applyInitialCropBox() {
      if (!this.cropper || !this.selectionEl?.$change) return
      const ratio = this.ratioMap[this.currentRatio]
      const canvas = this.cropper.getCropperCanvas?.()
      const image = this.cropper.getCropperImage?.()
      if (!canvas || !image) {
        return
      }

      const canvasRect = canvas.getBoundingClientRect()
      const imageRect = image.getBoundingClientRect()
      if (!canvasRect.width || !canvasRect.height || !imageRect.width || !imageRect.height) {
        return
      }

      const imageLeft = Math.max(imageRect.left - canvasRect.left, 0)
      const imageTop = Math.max(imageRect.top - canvasRect.top, 0)
      const imageWidth = Math.min(imageRect.width, canvasRect.width)
      const imageHeight = Math.min(imageRect.height, canvasRect.height)

      let cropWidth = imageWidth * 0.94
      let cropHeight = imageHeight * 0.94

      if (Number.isFinite(ratio) && ratio > 0) {
        cropWidth = Math.min(imageWidth * 0.94, imageHeight * ratio * 0.94)
        cropHeight = cropWidth / ratio

        if (cropHeight > imageHeight * 0.94) {
          cropHeight = imageHeight * 0.94
          cropWidth = cropHeight * ratio
        }
      }

      const left = imageLeft + Math.max((imageWidth - cropWidth) / 2, 0)
      const top = imageTop + Math.max((imageHeight - cropHeight) / 2, 0)

      this.selectionEl.aspectRatio = ratio
      this.selectionEl.initialAspectRatio = ratio
      this.selectionEl.$change(left, top, cropWidth, cropHeight, ratio, true)
      this.renderPreview()
    },

    async renderPreview() {
      if (!this.selectionEl?.$toCanvas || !this.$refs.previewBox) {
        return
      }

      const previewBox = this.$refs.previewBox
      const ratio = this.ratioMap?.[this.currentRatio]
      const width = Math.max(previewBox.clientWidth || 150, 1)
      const height = Number.isFinite(ratio) && ratio > 0
        ? Math.max(Math.round(width / ratio), 1)
        : Math.max(previewBox.clientHeight || 150, 1)
      const token = ++this.previewRenderToken

      try {
        const canvas = await this.selectionEl.$toCanvas({
          width,
          height,
        })

        if (token !== this.previewRenderToken || !this.$refs.previewBox) {
          return
        }

        canvas.style.width = '100%'
        canvas.style.height = '100%'
        canvas.style.display = 'block'
        previewBox.innerHTML = ''
        previewBox.appendChild(canvas)
      } catch (e) {
        console.error('预览渲染失败', e)
      }
    },

    onCancel() {
      this.destroyCropper()
      this.$emit('close')
    },

    async onConfirm() {
      if (!this.selectionEl?.$toCanvas) return
      this.loading = true

      try {
        const outputSize = OUTPUT_SIZE_MAP[this.currentRatio] || OUTPUT_SIZE_MAP['16:9']
        const canvas = await this.selectionEl.$toCanvas({
          width: outputSize.width,
          height: outputSize.height,
        })

        const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.92))
        if (!blob) throw new Error('裁剪失败')

        const croppedFile = new File([blob], this.file.name.replace(/\.[^.]+$/, '.jpg'), {
          type: 'image/jpeg',
          lastModified: Date.now(),
        })

        this.$emit('confirm', croppedFile)
        this.destroyCropper()
      } catch (e) {
        console.error('裁剪失败', e)
        ElMessage.error('裁剪失败，请重试')
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.cropper-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cropper-dialog {
  background: #fff;
  border-radius: 12px;
  width: 96vw;
  max-width: 1320px;
  height: min(88vh, 920px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.cropper-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.cropper-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.cropper-actions {
  display: flex;
  gap: 8px;
}

.cropper-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.cropper-main {
  flex: 1;
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  background: #f0f0f0;
  padding: 12px;
  min-width: 0;
  min-height: 0;
}

.cropper-thumbnail {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 560px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.source-image {
  display: block;
  width: auto;
  height: auto;
  max-width: none;
  max-height: none;
}

.cropper-sidebar {
  width: 180px;
  padding: 16px 14px;
  border-left: 1px solid #eee;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fff;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-title {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.ratio-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ratio-item {
  padding: 4px 12px;
  border-radius: 4px;
  border: 1px solid #ddd;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;

}

.ratio-item:hover {
  border-color: #409eff;
  color: #409eff;
}

.ratio-item.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.action-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preview-box {
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #f9f9f9;
}

.preview-target {
  width: 100%;
  height: 100%;
}

.preview-box :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cropper-main :deep(.cropper-container) {
  width: 100% !important;
  height: 100% !important;
}

.cropper-main :deep(cropper-canvas) {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
