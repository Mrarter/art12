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
            <div class="cropper-thumbnail" v-if="previewUrl">
              <img :src="previewUrl" ref="previewImg" style="max-width: 100%; max-height: 100%;" />
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
              <div class="preview-box">
                <img :src="previewUrl" ref="previewBox" />
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
import 'cropperjs/dist/cropper.css'

export default {
  name: 'ImageCropper',
  props: {
    visible: Boolean,
    file: File,
    aspectRatio: { type: Number, default: NaN },
  },
  emits: ['close', 'confirm'],
  data() {
    return {
      previewUrl: '',
      cropper: null,
      loading: false,
      currentRatio: 'free',
      ratioMap: {
        'free': NaN,
        '1:1': 1,
        '4:3': 4 / 3,
        '3:4': 3 / 4,
        '16:9': 16 / 9,
      },
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
  },
  methods: {
    initCropper() {
      this.destroyCropper()
      this.previewUrl = URL.createObjectURL(this.file)

      this.$nextTick(() => {
        const img = this.$refs.previewImg
        if (!img) return

        this.cropper = new Cropper(img, {
          aspectRatio: this.ratioMap[this.currentRatio],
          viewMode: 1,
          dragMode: 'move',
          autoCropArea: 0.8,
          restore: false,
          guides: true,
          center: true,
          highlight: false,
          cropBoxMovable: true,
          cropBoxResizable: true,
          toggleDragModeOnDblclick: false,
          ready: () => {
            // 将预览框关联到 cropper
            if (this.$refs.previewBox) {
              this.cropperPreview = new Cropper(this.$refs.previewBox, {
                aspectRatio: this.ratioMap[this.currentRatio],
                viewMode: 1,
                dragMode: 'none',
                autoCropArea: 1,
                restore: false,
                zoomable: false,
                rotatable: false,
                scalable: false,
              })
            }
          },
        })
      })
    },

    destroyCropper() {
      if (this.cropper) {
        this.cropper.destroy()
        this.cropper = null
      }
      if (this.previewUrl) {
        URL.revokeObjectURL(this.previewUrl)
        this.previewUrl = ''
      }
    },

    switchRatio(val) {
      this.currentRatio = val
      if (this.cropper) {
        this.cropper.setAspectRatio(this.ratioMap[val])
      }
    },

    rotate(deg) {
      if (this.cropper) {
        this.cropper.rotate(deg)
      }
    },

    reset() {
      if (this.cropper) {
        this.cropper.reset()
      }
    },

    onCancel() {
      this.destroyCropper()
      this.$emit('close')
    },

    async onConfirm() {
      if (!this.cropper) return
      this.loading = true

      try {
        const canvas = this.cropper.getCroppedCanvas({
          width: 800,
          height: 800,
          fillColor: '#fff',
          imageSmoothingEnabled: true,
          imageSmoothingQuality: 'high',
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
  width: 90vw;
  max-width: 1000px;
  max-height: 90vh;
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
  min-height: 400px;
}

.cropper-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  padding: 20px;
  min-height: 400px;
}

.cropper-thumbnail {
  max-width: 100%;
  max-height: 500px;
}

.cropper-thumbnail img {
  max-width: 100%;
  max-height: 500px;
}

.cropper-sidebar {
  width: 200px;
  padding: 16px;
  border-left: 1px solid #eee;
  display: flex;
  flex-direction: column;
  gap: 16px;
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

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }

  &.active {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
  }
}

.action-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preview-box {
  width: 120px;
  height: 120px;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #f9f9f9;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
</style>
