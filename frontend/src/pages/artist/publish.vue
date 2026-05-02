<template>
  <view class="publish-page">
    <!-- 封面图片 -->
    <view class="form-section">
      <view class="section-title">作品封面</view>
      <view class="cover-upload" @click="chooseCover">
        <image v-if="formData.cover" :src="formData.cover" mode="aspectFill" class="cover-preview"></image>
        <view v-else class="upload-placeholder">
          <text class="upload-text">上传封面图</text>
          <text class="upload-tip">建议尺寸 800x800</text>
        </view>
      </view>
    </view>

    <!-- 作品信息 -->
    <view class="form-section">
      <view class="section-title">基本信息</view>
      
      <view class="form-item">
        <text class="form-label">作品名称</text>
        <input 
          class="form-input" 
          v-model="formData.title" 
          placeholder="请输入作品名称"
          maxlength="50"
        />
      </view>

      <!-- 艺术家搜索 -->
      <view class="form-item artist-form-item">
        <view class="artist-label-row">
          <text class="form-label">作者</text>
          <text class="artist-id-display" v-if="formData.authorId">ID: {{ formData.authorId }}</text>
        </view>
        <view class="artist-input-wrapper">
          <input 
            class="form-input artist-input" 
            :class="{ 'artist-new': artistStatus === 'new', 'artist-exists': artistStatus === 'exists' }"
            v-model="artistKeyword" 
            placeholder="输入艺术家名称搜索"
            @input="onArtistInput"
            @focus="showArtistDropdown = true"
            @blur="hideArtistDropdown"
          />
          <view class="artist-status-tag" :class="'status-' + artistStatus" v-if="artistKeyword">
            {{ artistStatusText }}
          </view>
        </view>
        <!-- 艺术家下拉列表 -->
        <view class="artist-dropdown" v-if="showArtistDropdown && artistList.length > 0">
          <view 
            class="artist-option" 
            v-for="artist in artistList" 
            :key="artist.id"
            @click="selectArtist(artist)"
          >
            <image class="artist-avatar" :src="artist.avatar || '/static/avatar/default.png'" mode="aspectFill"></image>
            <view class="artist-info">
              <text class="artist-name">{{ artist.name }}</text>
              <text class="artist-badge" v-if="artist.certified">已认证</text>
            </view>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">创作年代</text>
        <picker mode="selector" :range="yearRange" range-key="label" @change="onYearChange">
          <view class="form-picker">
            <text :class="{ placeholder: !formData.year }">
              {{ formData.year ? formData.year + '年' : '请选择年代' }}
            </text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">作品尺寸</text>
        <view class="size-inputs">
          <input 
            class="form-input size-input" 
            v-model="formData.width" 
            placeholder="宽(cm)"
            type="digit"
          />
          <text class="size-separator">×</text>
          <input 
            class="form-input size-input" 
            v-model="formData.height" 
            placeholder="高(cm)"
            type="digit"
          />
          <text class="size-unit">cm</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">材质</text>
        <picker mode="selector" :range="materialRange" range-key="label" @change="onMaterialChange">
          <view class="form-picker">
            <text :class="{ placeholder: !formData.material }">
              {{ formData.material || '请选择材质' }}
            </text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">艺术门类</text>
        <picker mode="selector" :range="categoryRange" range-key="label" @change="onCategoryChange">
          <view class="form-picker">
            <text :class="{ placeholder: !formData.category }">
              {{ formData.category || '请选择门类' }}
            </text>
          </view>
        </picker>
      </view>
    </view>

    <!-- 价格设置 -->
    <view class="form-section">
      <view class="section-title">价格设置</view>
      
      <view class="form-item">
        <text class="form-label">出售价格</text>
        <view class="price-input">
          <text class="price-unit">¥</text>
          <input 
            class="form-input price-value" 
            v-model="formData.price" 
            placeholder="请输入价格"
            type="digit"
          />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">允许拍卖</text>
        <switch 
          :checked="formData.allowAuction" 
          @change="(e) => formData.allowAuction = e.detail.value"
          color="#667eea"
        />
      </view>

      <view class="form-item">
        <text class="form-label">库存数量</text>
        <input 
          class="form-input" 
          v-model="formData.stock" 
          placeholder="请输入库存数量"
          type="number"
        />
      </view>
    </view>

    <!-- 作品描述 -->
    <view class="form-section">
      <view class="section-title">作品描述</view>
      <textarea 
        class="form-textarea" 
        v-model="formData.description" 
        placeholder="请输入作品描述、创作背景、艺术家简介等信息..."
        maxlength="2000"
      ></textarea>
      <view class="word-count">{{ formData.description.length }}/2000</view>
    </view>

    <!-- 作品图片 -->
    <view class="form-section">
      <view class="section-title">
        作品详情图
        <text class="section-tip">（最多9张）</text>
      </view>
      <view class="images-grid">
        <view 
          class="image-item" 
          v-for="(img, index) in formData.images" 
          :key="index"
        >
          <image :src="img" mode="aspectFill"></image>
          <view class="image-delete" @click="removeImage(index)">
          </view>
        </view>
        <view 
          class="image-add" 
          v-if="formData.images.length < 9" 
          @click="chooseImages"
        >
          <text>添加图片</text>
        </view>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-bar">
      <view class="save-draft" @click="saveDraft">保存草稿</view>
      <view class="submit-btn" @click="submit">发布作品</view>
    </view>
  </view>
</template>

<script>
import { getArtworkDetail, getCategories, publishArtwork, updateArtwork } from '@/api/product.js'
import { searchArtists, findOrCreateArtist } from '@/api/user.js'
import { uploadFile } from '@/api/file.js'

export default {
  data() {
    return {
      isEdit: false,
      artworkId: null,
      formData: {
        cover: '',
        title: '',
        authorId: null,
        authorName: '',
        categoryId: null,
        year: '',
        width: '',
        height: '',
        material: '',
        category: '',
        price: '',
        allowAuction: false,
        stock: 1,
        description: '',
        images: []
      },
      // 艺术家搜索相关
      artistKeyword: '',
      artistList: [],
      artistStatus: '', // '', 'exists', 'new', 'notfound'
      artistId: null,
      showArtistDropdown: false,
      searchTimer: null,
      yearRange: [
        { label: '2020年代', value: 2020 },
        { label: '2010年代', value: 2010 },
        { label: '2000年代', value: 2000 },
        { label: '1990年代', value: 1990 },
        { label: '1980年代', value: 1980 },
        { label: '更早', value: 1900 }
      ],
      materialRange: ['布面油画', '纸本水墨', '宣纸', '绢本', '木板', '铜版', '石版', '青铜', '陶瓷', '综合材料', '其他'],
      categoryRange: []
    }
  },

  computed: {
    artistStatusText() {
      switch (this.artistStatus) {
        case 'exists': return '已存在'
        case 'new': return '新创建'
        case 'notfound': return '不存在将自动创建'
        default: return ''
      }
    }
  },

  onLoad(options) {
    this.loadCategories()
    if (options.id) {
      this.isEdit = true
      this.artworkId = Number(options.id)
      this.loadArtwork(options.id)
    }
  },

  methods: {
    async loadArtwork(id) {
      uni.showLoading({ title: '加载中...' })
      try {
        const res = await getArtworkDetail(id)
        if (res) {
          this.formData = {
            cover: res.cover || '',
            title: res.title || '',
            authorId: res.authorId || null,
            authorName: res.authorName || '',
            categoryId: res.categoryId || null,
            year: res.year || '',
            width: res.width || '',
            height: res.height || '',
            material: res.material || res.medium || '',
            category: res.category || res.categoryName || '',
            price: res.price || '',
            allowAuction: res.allowAuction || false,
            stock: res.stock || 1,
            description: res.description || '',
            images: res.images || []
          }
          this.artistKeyword = res.authorName || ''
          if (res.authorId) {
            this.artistId = res.authorId
            this.artistStatus = 'exists'
          }
        }
      } catch (e) {
        console.error('加载作品失败', e)
      } finally {
        uni.hideLoading()
      }
    },

    async loadCategories() {
      try {
        const list = await getCategories()
        this.categoryRange = (list || [])
          .filter(item => item && item.status !== 0)
          .map(item => ({
            label: item.name,
            value: item.id,
            id: item.id,
            name: item.name
          }))
      } catch (e) {
        console.error('加载作品分类失败', e)
        this.categoryRange = [
          { label: '国画', value: 1, id: 1, name: '国画' },
          { label: '油画', value: 2, id: 2, name: '油画' },
          { label: '书法', value: 3, id: 3, name: '书法' },
          { label: '版画', value: 4, id: 4, name: '版画' },
          { label: '雕塑', value: 5, id: 5, name: '雕塑' }
        ]
      }
    },

    // 艺术家输入事件
    onArtistInput() {
      // 清除之前的定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }

      if (!this.artistKeyword.trim()) {
        this.artistList = []
        this.artistStatus = ''
        this.artistId = null
        this.formData.authorId = null
        this.formData.authorName = ''
        return
      }

      // 防抖搜索
      this.searchTimer = setTimeout(() => {
        this.searchArtist()
      }, 300)
    },

    // 搜索艺术家
    async searchArtist() {
      try {
        const keyword = this.artistKeyword.trim()
        const res = await searchArtists(keyword)
        this.artistList = (res || []).map(this.normalizeArtist)
        
        if (this.artistList.length === 0) {
          this.artistStatus = 'notfound'
          this.formData.authorId = null
          this.formData.authorName = keyword
        } else {
          // 检查是否有完全匹配的
          const exactMatch = this.artistList.find(a => 
            a.name === keyword
          )
          if (exactMatch) {
            this.artistId = exactMatch.id
            this.formData.authorId = exactMatch.id
            this.formData.authorName = exactMatch.name
            this.artistStatus = 'exists'
          } else {
            this.formData.authorId = null
            this.formData.authorName = keyword
            this.artistStatus = 'notfound'
          }
        }
      } catch (e) {
        console.error('搜索艺术家失败', e)
        this.artistList = []
        this.artistStatus = 'notfound'
      }
    },

    normalizeArtist(artist = {}) {
      const name = artist.name || artist.nickname || artist.realName || artist.artistName || artist.username || ''
      return {
        ...artist,
        id: artist.id || artist.userId || artist.artistId,
        name,
        avatar: artist.avatar || artist.avatarUrl || '',
        certified: Boolean(artist.certified || artist.artistCode || artist.badge)
      }
    },

    // 选择艺术家
    selectArtist(artist) {
      this.artistKeyword = artist.name
      this.artistId = artist.id
      this.formData.authorId = artist.id
      this.formData.authorName = artist.name
      this.artistStatus = artist.certified ? 'exists' : 'new'
      this.artistList = []
      this.showArtistDropdown = false
    },

    // 隐藏下拉框
    hideArtistDropdown() {
      // 延迟隐藏，以便点击选项
      setTimeout(() => {
        this.showArtistDropdown = false
      }, 200)
    },

    chooseCover() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.formData.cover = res.tempFilePaths[0]
        }
      })
    },

    chooseImages() {
      const remain = 9 - this.formData.images.length
      uni.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.formData.images = [...this.formData.images, ...res.tempFilePaths]
        }
      })
    },

    removeImage(index) {
      this.formData.images.splice(index, 1)
    },

    onYearChange(e) {
      this.formData.year = this.yearRange[e.detail.value].value
    },

    onMaterialChange(e) {
      this.formData.material = this.materialRange[e.detail.value]
    },

    onCategoryChange(e) {
      const selected = this.categoryRange[e.detail.value]
      this.formData.categoryId = selected.id || selected.value
      this.formData.category = selected.name || selected.label
    },

    validate() {
      if (!this.formData.cover) {
        uni.showToast({ title: '请上传封面图', icon: 'none' })
        return false
      }
      if (!this.formData.title.trim()) {
        uni.showToast({ title: '请输入作品名称', icon: 'none' })
        return false
      }
      if (!this.artistKeyword.trim()) {
        uni.showToast({ title: '请输入艺术家名称', icon: 'none' })
        return false
      }
      if (!this.formData.price) {
        uni.showToast({ title: '请输入价格', icon: 'none' })
        return false
      }
      if (Number(this.formData.price) <= 0) {
        uni.showToast({ title: '价格必须大于0', icon: 'none' })
        return false
      }
      if (!this.formData.categoryId) {
        uni.showToast({ title: '请选择艺术门类', icon: 'none' })
        return false
      }
      return true
    },

    async saveDraft() {
      uni.showToast({ title: '草稿保存成功', icon: 'success' })
    },

    async submit() {
      if (!this.validate()) return

      uni.showLoading({ title: '提交中...' })

      try {
        const coverUrl = await this.ensureUploaded(this.formData.cover)
        const imageUrls = []
        for (const image of this.formData.images) {
          imageUrls.push(await this.ensureUploaded(image))
        }

        // 如果艺术家名称已输入但未选择，需要查找或创建
        if (this.artistKeyword.trim() && !this.formData.authorId) {
          try {
            const artistRes = await findOrCreateArtist(this.artistKeyword.trim())
            if (artistRes) {
              this.formData.authorId = artistRes.id
              this.formData.authorName = artistRes.name
              if (artistRes.pending) {
                uni.showToast({ title: '艺术家不存在，已创建待审核', icon: 'none', duration: 2000 })
              }
            }
          } catch (e) {
            console.error('创建艺术家失败', e)
          }
        }

        const submitData = {
          title: this.formData.title.trim(),
          authorId: this.formData.authorId,
          authorName: this.formData.authorName || this.artistKeyword,
          categoryId: this.formData.categoryId,
          cover: coverUrl,
          images: imageUrls.join(','),
          artType: this.formData.category,
          medium: this.formData.material,
          year: this.formData.year ? Number(this.formData.year) : null,
          size: `${this.formData.width}×${this.formData.height}cm`,
          price: this.formData.price ? Number(this.formData.price) : null,
          stock: this.formData.stock ? Number(this.formData.stock) : 1,
          description: this.formData.description,
          status: 1,
          ownershipType: 1
        }

        if (this.isEdit) {
          await updateArtwork(this.artworkId, submitData)
          uni.showToast({ title: '更新成功', icon: 'success' })
        } else {
          await publishArtwork(submitData)
          uni.showToast({ title: '发布成功', icon: 'success' })
        }

        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1500)
      } catch (e) {
        console.error('发布作品失败', e)
        uni.hideLoading()
        uni.showToast({ title: e.message || '发布失败，请重试', icon: 'none' })
      }
    },

    async ensureUploaded(path) {
      if (!path) return ''
      if (/^https?:\/\//.test(path)) return path
      return uploadFile(path)
    }
  }
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.form-section {
  background: #fff;
  margin-bottom: 20rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
  
  .section-tip {
    font-size: 24rpx;
    color: #999;
    font-weight: normal;
    margin-left: 12rpx;
  }
}

.cover-upload {
  width: 300rpx;
  height: 300rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: #f9f9f9;
}

.cover-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #ddd;
  border-radius: 16rpx;

  .upload-text {
    font-size: 28rpx;
    color: #666;
    margin-top: 16rpx;
  }

  .upload-tip {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

.form-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  
  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  font-size: 28rpx;
  color: #333;
  width: 160rpx;
}

.artist-label-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.artist-id-display {
  font-size: 24rpx;
  color: #667eea;
  font-weight: 500;
  padding: 4rpx 12rpx;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 8rpx;
}

.form-input {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: #333;
}

.artist-form-item {
  position: relative;
  flex-direction: column;
  align-items: flex-start;
}

.artist-input-wrapper {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.artist-input {
  flex: 1;
  text-align: left;
  padding: 16rpx 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.artist-input.artist-new {
  border: 2rpx solid #e74c3c;
  background: #fff5f5;
}

.artist-input.artist-exists {
  border: 2rpx solid #50c878;
  background: #f0fff4;
}

.artist-status-tag {
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  white-space: nowrap;
}

.artist-status-tag.status-exists {
  background: #50c878;
  color: #fff;
}

.artist-status-tag.status-new {
  background: #ff9800;
  color: #fff;
}

.artist-status-tag.status-notfound {
  background: #e74c3c;
  color: #fff;
}

.artist-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 12rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.15);
  z-index: 100;
  max-height: 400rpx;
  overflow-y: auto;
  margin-top: 8rpx;
}

.artist-option {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.artist-option:last-child {
  border-bottom: none;
}

.artist-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  margin-right: 16rpx;
  background: #f0f0f0;
}

.artist-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.artist-name {
  font-size: 28rpx;
  color: #333;
}

.artist-badge {
  font-size: 20rpx;
  color: #50c878;
  background: #f0fff4;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
}

.form-picker {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  font-size: 28rpx;
  color: #333;
  
  .placeholder {
    color: #999;
  }
}

.size-inputs {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.size-input {
  width: 140rpx;
  text-align: center;
}

.size-separator {
  color: #999;
}

.size-unit {
  font-size: 28rpx;
  color: #999;
}

.price-input {
  display: flex;
  align-items: center;
  
  .price-unit {
    font-size: 32rpx;
    color: #e74c3c;
    font-weight: 600;
    margin-right: 8rpx;
  }
  
  .price-value {
    text-align: left;
    font-size: 32rpx;
    font-weight: 600;
    color: #e74c3c;
  }
}

.form-textarea {
  width: 100%;
  height: 300rpx;
  padding: 24rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

.word-count {
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 16rpx;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12rpx;
  overflow: hidden;

  image {
    width: 100%;
    height: 100%;
  }

  .image-delete {
    position: absolute;
    top: 8rpx;
    right: 8rpx;
    width: 36rpx;
    height: 36rpx;
    background: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.image-add {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
  border-radius: 12rpx;
  border: 2rpx dashed #ddd;
  
  text {
    font-size: 22rpx;
    color: #999;
    margin-top: 8rpx;
  }
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 24rpx;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.save-draft {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: #f5f5f5;
  color: #666;
  font-size: 30rpx;
  border-radius: 44rpx;
}

.submit-btn {
  flex: 2;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 30rpx;
  border-radius: 44rpx;
}
</style>
