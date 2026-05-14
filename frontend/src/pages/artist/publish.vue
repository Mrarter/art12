<template>
  <view class="publish-page">
    <view class="art-nav">
      <view class="nav-icon" @click="goBack">
        <image src="/static/art-icons/icon-back.svg" mode="aspectFit"></image>
      </view>
      <view class="nav-title">{{ isEdit ? '编辑作品' : '发布作品' }}</view>
      <view class="nav-icon ghost" @click="saveDraft">
        <image src="/static/art-icons/icon-document.svg" mode="aspectFit"></image>
      </view>
    </view>

    <view class="publish-hero">
      <view class="hero-kicker">Artist Work Entry</view>
      <view class="hero-title">作品发布入口</view>
      <view class="hero-desc">完善作品资料后将同步进入平台作品库、艺术家主页与前端展示链路。</view>
    </view>

    <view class="form-section cover-section">
      <view class="section-head">
        <view>
          <view class="section-title">作品封面</view>
          <view class="section-subtitle">建议上传 1:1 或 4:3 高清作品图</view>
        </view>
        <view class="section-chip">必填</view>
      </view>
      <view class="cover-upload" @click="chooseCover">
        <image v-if="formData.cover" :src="formData.cover" mode="aspectFill" class="cover-preview"></image>
        <view v-else class="upload-placeholder">
          <image class="upload-icon" src="/static/art-icons/icon-preview.svg" mode="aspectFit"></image>
          <text class="upload-text">上传封面图</text>
          <text class="upload-tip">支持相册或拍摄</text>
        </view>
      </view>
    </view>

    <view class="form-section">
      <view class="section-head">
        <view>
          <view class="section-title">基本信息</view>
          <view class="section-subtitle">用于生成作品详情与流通档案</view>
        </view>
      </view>
      
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
          <text class="artist-id-display" v-if="formData.authorUid">UID: {{ formData.authorUid }}</text>
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
            <image class="artist-avatar" :src="artist.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
            <view class="artist-info">
              <text class="artist-name">{{ artist.name }}</text>
              <text class="artist-uid" v-if="artist.uid">{{ artist.uid }}</text>
              <text class="artist-badge" v-if="artist.certified">已认证</text>
            </view>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">创作年代</text>
        <input 
          class="form-input" 
          v-model="formData.year" 
          placeholder="请输入创作年代，如2020"
          type="number"
        />
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
        <text class="form-label">作品分类</text>
        <view class="category-select-wrapper">
          <view class="category-select-trigger" @click="toggleCategoryDropdown">
            <text :class="['category-value', { placeholder: !formData.category }]">{{ formData.category || '请选择作品分类' }}</text>
            <text class="arrow-down">▼</text>
          </view>
          <view class="category-dropdown" v-if="showCategoryDropdown">
            <view 
              class="dropdown-item" 
              v-for="(cat, index) in categoryRange" 
              :key="index"
              :class="{ active: formData.categoryId === cat.id }"
              @click="selectCategory(cat)"
            >
              {{ cat.name }}
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="form-section">
      <view class="section-head">
        <view>
          <view class="section-title">价格设置</view>
          <view class="section-subtitle">配置出售、拍卖、分销与库存策略</view>
        </view>
      </view>
      
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
        <text class="form-label">参与分销</text>
        <switch 
          :checked="formData.allowDistribution" 
          @change="(e) => formData.allowDistribution = e.detail.value"
          color="#F2C14E"
        />
      </view>

      <view class="form-item">
        <text class="form-label">允许拍卖</text>
        <switch 
          :checked="formData.allowAuction" 
          @change="(e) => formData.allowAuction = e.detail.value"
          color="#F2C14E"
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

    <view class="form-section">
      <view class="section-head">
        <view>
          <view class="section-title">作品描述</view>
          <view class="section-subtitle">写清创作背景、主题、材质与收藏亮点</view>
        </view>
      </view>
      <textarea 
        class="form-textarea" 
        v-model="formData.description" 
        placeholder="请输入作品描述、创作背景、艺术家简介等信息..."
        maxlength="2000"
      ></textarea>
      <view class="word-count">{{ formData.description.length }}/2000</view>
    </view>

    <view class="form-section">
      <view class="section-head">
        <view>
          <view class="section-title">
            作品详情图
            <text class="section-tip">最多9张</text>
          </view>
          <view class="section-subtitle">可上传局部、装裱、场景图</view>
        </view>
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
          <image src="/static/art-icons/icon-preview.svg" mode="aspectFit"></image>
          <text>添加图片</text>
        </view>
      </view>
    </view>

    <view class="safe-bottom-space"></view>

    <view class="submit-bar">
      <view class="save-draft" @click="saveDraft">保存草稿</view>
      <view class="submit-btn" @click="submit">发布作品</view>
    </view>

  </view>
</template>

<script>
import { getArtworkDetail, getCategories, publishArtwork, updateArtwork } from '@/api/product.js'
import { searchArtists, searchUsers, findOrCreateArtist } from '@/api/user.js'
import { useUserStore } from '@/store/modules/user.js'
import { uploadFile, openCropper } from '@/api/file.js'

export default {
  data() {
    return {
      isEdit: false,
      artworkId: null,
      formData: {
        cover: '',
        title: '',
        authorId: null,
        authorUid: null,
        authorName: '',
        categoryId: null,
        year: '',
        width: '',
        height: '',
        material: '',
        category: '',
        price: '',
        allowDistribution: false,
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
      categoryRange: [],
      showCategoryDropdown: false,
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
    
    // 自动填入当前用户名称作为作者
    if (!options.id) {
      const userStore = useUserStore()
      if (userStore.userInfo) {
        const nickname = userStore.userInfo.nickname || userStore.userInfo.name || ''
        if (nickname) {
          this.artistKeyword = nickname
          this.formData.authorName = nickname
          this.artistStatus = 'exists'
        }
      }
    }
    
    if (options.id) {
      this.isEdit = true
      this.artworkId = Number(options.id)
      this.loadArtwork(options.id)
    }
  },

  mounted() {
    if (typeof document !== 'undefined') {
      document.addEventListener('click', this.handleClickOutside)
    }
  },

  beforeUnmount() {
    if (typeof document !== 'undefined') {
      document.removeEventListener('click', this.handleClickOutside)
    }
  },

  methods: {
    goBack() {
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.reLaunch({ url: '/pages/index/index' })
      }
    },

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
        
        // 材质选项（合并到作品分类）
        const materialOptions = [
          { label: '布面油画', value: '布面油画', name: '布面油画' },
          { label: '纸本水墨', value: '纸本水墨', name: '纸本水墨' },
          { label: '宣纸', value: '宣纸', name: '宣纸' },
          { label: '绢本', value: '绢本', name: '绢本' },
          { label: '木板', value: '木板', name: '木板' },
          { label: '铜版', value: '铜版', name: '铜版' },
          { label: '石版', value: '石版', name: '石版' },
          { label: '青铜', value: '青铜', name: '青铜' },
          { label: '陶瓷', value: '陶瓷', name: '陶瓷' },
          { label: '综合材料', value: '综合材料', name: '综合材料' },
          { label: '其他', value: '其他', name: '其他' }
        ]
        this.categoryRange = [...this.categoryRange, ...materialOptions]
      } catch (e) {
        console.error('加载作品分类失败', e)
        this.categoryRange = [
          { label: '国画', value: 1, id: 1, name: '国画' },
          { label: '油画', value: 2, id: 2, name: '油画' },
          { label: '书法', value: 3, id: 3, name: '书法' },
          { label: '版画', value: 4, id: 4, name: '版画' },
          { label: '雕塑', value: 5, id: 5, name: '雕塑' },
          { label: '布面油画', value: '布面油画', name: '布面油画' },
          { label: '纸本水墨', value: '纸本水墨', name: '纸本水墨' },
          { label: '宣纸', value: '宣纸', name: '宣纸' },
          { label: '绢本', value: '绢本', name: '绢本' },
          { label: '木板', value: '木板', name: '木板' },
          { label: '铜版', value: '铜版', name: '铜版' },
          { label: '石版', value: '石版', name: '石版' },
          { label: '青铜', value: '青铜', name: '青铜' },
          { label: '陶瓷', value: '陶瓷', name: '陶瓷' },
          { label: '综合材料', value: '综合材料', name: '综合材料' },
          { label: '其他', value: '其他', name: '其他' }
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

    // 搜索艺术家（优先搜索艺术家列表，同时搜索全局用户）
    async searchArtist() {
      try {
        const keyword = this.artistKeyword.trim()
        
        // 并行搜索艺术家和全局用户
        const [artistRes, userRes] = await Promise.all([
          searchArtists(keyword).catch(() => []),
          searchUsers(keyword).catch(() => [])
        ])
        
        // 合并结果，艺术家优先
        const artistMap = new Map()
        ;(artistRes || []).forEach(a => {
          const normalized = this.normalizeArtist(a)
          artistMap.set(normalized.id, normalized)
        })
        ;(userRes || []).forEach(u => {
          const normalized = this.normalizeUser(u)
          if (!artistMap.has(normalized.id)) {
            artistMap.set(normalized.id, normalized)
          }
        })
        
        this.artistList = Array.from(artistMap.values())
        
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
            this.formData.authorUid = exactMatch.uid || null
            this.formData.authorName = exactMatch.name
            this.artistStatus = exactMatch.isArtist ? 'exists' : 'new'
          } else {
            this.formData.authorId = null
            this.formData.authorUid = null
            this.formData.authorName = keyword
            this.artistStatus = 'notfound'
          }
        }
      } catch (e) {
        console.error('搜索失败', e)
        this.artistList = []
        this.artistStatus = 'notfound'
      }
    },

    normalizeArtist(artist = {}) {
      const name = artist.name || artist.nickname || artist.realName || artist.artistName || artist.username || ''
      return {
        ...artist,
        id: artist.id || artist.userId || artist.artistId,
        uid: artist.uid || null,
        name,
        avatar: artist.avatar || artist.avatarUrl || '',
        certified: Boolean(artist.certified || artist.artistCode || artist.badge),
        isArtist: true
      }
    },
    
    // 规范化全局用户数据
    normalizeUser(user = {}) {
      const name = user.name || user.nickname || ''
      return {
        ...user,
        id: user.id || user.userId,
        name,
        avatar: user.avatar || '',
        certified: Boolean(user.certified),
        isArtist: Boolean(user.isArtist)
      }
    },

    // 选择艺术家
    selectArtist(artist) {
      this.artistKeyword = artist.name
      this.artistId = artist.id
      this.formData.authorId = artist.id
      this.formData.authorUid = artist.uid || null
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
          const path = res.tempFilePaths[0]
          openCropper(path, { ratio: '4:3', shape: 'square' }).then(cropped => {
            this.formData.cover = cropped
          }).catch(() => {
            this.formData.cover = path
          })
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
          const paths = res.tempFilePaths
          // 对每张图进行裁剪
          const cropPromises = paths.map(p =>
            openCropper(p, { ratio: 'free', shape: 'square' }).catch(() => p)
          )
          Promise.all(cropPromises).then(croppedList => {
            this.formData.images = [...this.formData.images, ...croppedList]
          })
        }
      })
    },

    removeImage(index) {
      this.formData.images.splice(index, 1)
    },

    onYearChange(e) {
      this.formData.year = this.yearRange[e.detail.value].value
    },

    // 分类选择
    toggleCategoryDropdown() {
      this.showCategoryDropdown = !this.showCategoryDropdown
    },

    selectCategory(cat) {
      this.formData.categoryId = cat.id || cat.value
      this.formData.category = cat.name || cat.label
      this.showCategoryDropdown = false
    },

    handleClickOutside(e) {
      const el = this.$el?.querySelector('.category-select-wrapper')
      if (!el) return
      if (!el.contains(e.target)) {
        this.showCategoryDropdown = false
      }
    },

    onCategoryChange(e) {
      const selected = this.categoryRange[e.detail.value]
      if (selected) {
        this.formData.categoryId = selected.id || selected.value
        this.formData.category = selected.name || selected.label
      }
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
      if (!this.formData.category) {
        uni.showToast({ title: '请填写作品分类', icon: 'none' })
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
              this.formData.authorUid = artistRes.uid // 修复：补充 UID 字段
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
          authorUid: this.formData.authorUid || null,
          authorName: this.formData.authorName || this.artistKeyword,
          categoryId: this.formData.categoryId,
          cover: coverUrl,
          images: imageUrls.join(','),
          artType: this.formData.category,
          medium: this.formData.category,
          year: this.formData.year ? Number(this.formData.year) : null,
          size: `${this.formData.width}×${this.formData.height}cm`,
          price: this.formData.price ? Number(this.formData.price) : null,
          allowDistribution: this.formData.allowDistribution || false,
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
  min-height: 200rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: #f9f9f9;
}

.cover-preview {
  width: 100%;
  display: block;
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

.artist-uid {
  font-size: 20rpx;
  color: #999;
  margin-left: 12rpx;
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
  }
  .price-value .uni-input-input,
  .price-value input {
    font-size: 24rpx !important;
    font-weight: 400 !important;
    color: #999 !important;
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

/* 作品分类下拉选择器 */
.category-select-wrapper {
  position: relative;
  flex: 1;
}
.category-select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72rpx;
  padding: 0 20rpx;
  background: #f5f5f5;
  border-radius: 8rpx;
}
.category-value {
  font-size: 28rpx;
  color: #333;
}
.category-value.placeholder {
  color: #999;
}
.arrow-down {
  font-size: 20rpx;
  color: #999;
  margin-left: 10rpx;
}
.category-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
  max-height: 400rpx;
  overflow-y: auto;
  margin-top: 4rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.1);
}
.dropdown-item {
  padding: 24rpx 20rpx;
  font-size: 28rpx;
  color: #333;
  border-bottom: 1rpx solid #f5f5f5;
}
.dropdown-item:last-child {
  border-bottom: none;
}
.dropdown-item.active {
  color: #667eea;
  font-weight: 500;
  background: #f0f5ff;
}

/* ===== v1.2 黑金发布页重构 ===== */
.publish-page {
  min-height: 100vh;
  padding: calc(96rpx + env(safe-area-inset-top)) 24rpx calc(170rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  color: #f5f2ea;
  background:
    radial-gradient(circle at 84% 0%, rgba(242, 193, 78, 0.16), transparent 26%),
    linear-gradient(180deg, #070707 0%, #050505 100%);
}

.art-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 80;
  height: calc(88rpx + env(safe-area-inset-top));
  padding: env(safe-area-inset-top) 24rpx 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(6, 6, 6, 0.92);
  border-bottom: 1rpx solid rgba(242, 193, 78, 0.12);
  backdrop-filter: blur(18rpx);
}

.nav-title {
  color: #f5f2ea;
  font-size: 32rpx;
  font-weight: 900;
}

.nav-icon {
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon image {
  width: 34rpx;
  height: 34rpx;
}

.nav-icon.ghost {
  border-radius: 50%;
  background: rgba(242, 193, 78, 0.08);
}

.publish-hero {
  margin: 8rpx 0 22rpx;
  padding: 28rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(242, 193, 78, 0.18);
  background:
    linear-gradient(135deg, rgba(242, 193, 78, 0.14), rgba(255, 255, 255, 0.03)),
    #111;
}

.hero-kicker {
  color: #d8b24c;
  font-size: 20rpx;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-title {
  margin-top: 10rpx;
  color: #fff;
  font-size: 42rpx;
  font-weight: 900;
}

.hero-desc {
  margin-top: 12rpx;
  color: #a9a39a;
  font-size: 24rpx;
  line-height: 1.55;
}

.form-section {
  margin: 18rpx 0 0;
  padding: 28rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(216, 178, 76, 0.16);
  background: linear-gradient(145deg, #1c1c1c, #101010);
  box-shadow: 0 18rpx 44rpx rgba(0, 0, 0, 0.28);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.section-title {
  margin-bottom: 0;
  color: #f2c14e;
  font-size: 32rpx;
  font-weight: 900;
}

.section-title::after {
  content: '';
  display: block;
  width: 44rpx;
  height: 6rpx;
  margin-top: 12rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #f2c14e, rgba(242, 193, 78, 0));
}

.section-subtitle {
  margin-top: 12rpx;
  color: #716d66;
  font-size: 22rpx;
}

.section-tip,
.section-chip {
  margin-left: 12rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  color: #111;
  background: linear-gradient(180deg, #f6d98a, #d9a935);
  font-size: 20rpx;
  font-weight: 800;
}

.cover-upload {
  width: 100%;
  min-height: 410rpx;
  border-radius: 22rpx;
  overflow: hidden;
  background: #151515;
}

.cover-preview {
  width: 100%;
  height: 410rpx;
  display: block;
}

.upload-placeholder {
  min-height: 410rpx;
  border: 2rpx dashed rgba(242, 193, 78, 0.35);
  border-radius: 22rpx;
  background:
    linear-gradient(135deg, rgba(242, 193, 78, 0.08), rgba(255, 255, 255, 0.02)),
    #151515;
}

.upload-icon {
  width: 68rpx;
  height: 68rpx;
  opacity: 0.88;
}

.upload-placeholder .upload-text {
  margin-top: 18rpx;
  color: #f5f2ea;
  font-size: 30rpx;
  font-weight: 800;
}

.upload-placeholder .upload-tip {
  margin-top: 10rpx;
  color: #716d66;
  font-size: 22rpx;
}

.form-item {
  min-height: 104rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.07);
}

.form-label {
  flex: 0 0 174rpx;
  width: 174rpx;
  color: #f5f2ea;
  font-size: 28rpx;
  font-weight: 700;
}

.form-input {
  min-height: 72rpx;
  color: #f5f2ea;
  font-size: 28rpx;
}

.artist-id-display {
  color: #f2c14e;
  background: rgba(242, 193, 78, 0.1);
}

.artist-input-wrapper {
  gap: 14rpx;
}

.artist-input {
  min-height: 82rpx;
  padding: 0 22rpx;
  color: #f5f2ea;
  background: #141414;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 16rpx;
}

.artist-input.artist-new,
.artist-input.artist-exists {
  background: rgba(242, 193, 78, 0.08);
  border: 1rpx solid rgba(242, 193, 78, 0.55);
}

.artist-status-tag {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  color: #111;
  font-size: 22rpx;
  font-weight: 800;
  background: linear-gradient(180deg, #f6d98a, #d9a935);
}

.artist-status-tag.status-exists,
.artist-status-tag.status-new,
.artist-status-tag.status-notfound {
  color: #111;
  background: linear-gradient(180deg, #f6d98a, #d9a935);
}

.artist-dropdown,
.category-dropdown {
  z-index: 120;
  border: 1rpx solid rgba(216, 178, 76, 0.18);
  border-radius: 18rpx;
  background: #121212;
  box-shadow: 0 22rpx 60rpx rgba(0, 0, 0, 0.45);
}

.artist-option {
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.artist-avatar {
  background: #222;
  border: 1rpx solid rgba(242, 193, 78, 0.24);
}

.artist-name {
  color: #f5f2ea;
}

.artist-uid,
.artist-badge {
  color: #a9a39a;
}

.size-inputs {
  flex: 1;
  justify-content: flex-end;
}

.size-input {
  width: 138rpx;
  min-height: 72rpx;
  border-radius: 14rpx;
  background: #141414;
}

.size-separator,
.size-unit {
  color: #716d66;
}

.category-select-trigger {
  min-height: 82rpx;
  padding: 0 22rpx;
  background: #141414;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 16rpx;
}

.category-value {
  color: #f5f2ea;
}

.category-value.placeholder,
.arrow-down {
  color: #716d66;
}

.dropdown-item {
  color: #f5f2ea;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.dropdown-item.active {
  color: #f2c14e;
  background: rgba(242, 193, 78, 0.1);
}

.price-input {
  flex: 1;
  justify-content: flex-end;
}

.price-input .price-unit {
  color: #f2c14e;
  font-size: 36rpx;
  font-weight: 900;
}

.price-input .price-value {
  max-width: 260rpx;
  color: #f2c14e;
  font-size: 32rpx;
  font-weight: 900;
}

.form-textarea {
  height: 280rpx;
  color: #f5f2ea;
  background: #141414;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 18rpx;
}

.word-count {
  color: #716d66;
}

.images-grid {
  gap: 18rpx;
}

.image-item,
.image-add {
  border-radius: 18rpx;
}

.image-add {
  background: #141414;
  border: 2rpx dashed rgba(242, 193, 78, 0.32);
}

.image-add image {
  width: 52rpx;
  height: 52rpx;
  opacity: 0.82;
}

.image-add text {
  color: #a9a39a;
}

.safe-bottom-space {
  height: calc(150rpx + env(safe-area-inset-bottom));
}

.submit-bar {
  z-index: 90;
  gap: 18rpx;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(10, 10, 10, 0.96);
  border-top: 1rpx solid rgba(216, 178, 76, 0.14);
  box-shadow: 0 -20rpx 50rpx rgba(0, 0, 0, 0.35);
}

.save-draft,
.submit-btn {
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 900;
}

.save-draft {
  color: #d8b24c;
  background: #101010;
  border: 1rpx solid rgba(216, 178, 76, 0.55);
}

.submit-btn {
  color: #111;
  background: linear-gradient(180deg, #f5d36f, #d8b24c);
}
</style>
