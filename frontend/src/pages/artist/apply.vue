<template>
  <view class="apply-page">
    <!-- 申请须知 -->
    <view class="notice-section card">
      <view class="notice-header">
        
        <text>申请须知</text>
      </view>
      <view class="notice-content">
        <view class="notice-item">1. 申请人须为原创艺术作品的作者</view>
        <view class="notice-item">2. 需提供真实有效的个人身份信息</view>
        <view class="notice-item">3. 需提供20件本人原创代表作品，禁止抄袭或盗用</view>
        <view class="notice-item">4. 认证服务费为 ¥3,600/年，每笔消费收取15%平台服务分成</view>
        <view class="notice-item">5. 当前阶段分享并邀请好友，平台赠送 ¥3,600 年费权益</view>
        <view class="notice-item">6. 审核结果将在3-5个工作日内通知</view>
      </view>
    </view>

    <!-- 申请表单 -->
    <view class="form-section card">
      <view class="form-title">基本信息</view>

      <!-- 头像上传 -->
      <view class="form-item">
        <view class="form-label">头像</view>
        <view class="avatar-upload" @click="chooseAvatar">
          <image v-if="form.avatar" :src="form.avatar" mode="aspectFill"></image>
          <view v-else class="upload-placeholder">
            
            <text>上传头像</text>
          </view>
        </view>
      </view>

      <!-- 姓名 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>姓名</view>
        <input
          class="form-input"
          v-model="form.name"
          placeholder="请输入真实姓名"
          maxlength="20"
        />
      </view>

      <!-- 艺名 -->
      <view class="form-item">
        <view class="form-label">艺名</view>
        <input
          class="form-input"
          v-model="form.artistName"
          placeholder="选填，用于展示在作品上"
          maxlength="20"
        />
      </view>

      <!-- 性别 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>性别</view>
        <view class="radio-group">
          <view
            class="radio-item"
            :class="{ active: form.gender === 1 }"
            @click="form.gender = 1"
          >
            <text>男</text>
          </view>
          <view
            class="radio-item"
            :class="{ active: form.gender === 2 }"
            @click="form.gender = 2"
          >
            <text>女</text>
          </view>
        </view>
      </view>

      <!-- 出生年份 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>出生年份</view>
        <picker mode="date" :value="form.birthYear" fields="year" @change="onBirthYearChange">
          <view class="picker-value">
            {{ form.birthYear || '请选择' }}
            
          </view>
        </picker>
      </view>

      <!-- 所在城市 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>所在城市</view>
        <picker
          mode="multiSelector"
          :range="cityPickerRange"
          :value="cityPickerIndex"
          @columnchange="onCityColumnChange"
          @change="onCityChange"
        >
          <view class="picker-value">
            {{ form.city || '请选择' }}
          </view>
        </picker>
      </view>
    </view>

    <!-- 专业信息 -->
    <view class="form-section card">
      <view class="form-title">专业信息</view>

      <!-- 作品分类 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>作品分类</view>
        <picker :value="form.artCategoryIndex" :range="artCategories" range-key="name" @change="onArtCategoryChange">
          <view class="picker-value">
            {{ form.artCategory || '请选择' }}
            
          </view>
        </picker>
      </view>

      <!-- 创作风格 -->
      <view class="form-item">
        <view class="form-label">创作风格</view>
        <input
          class="form-input"
          v-model="form.style"
          placeholder="如：抽象派、现实主义等"
          maxlength="50"
        />
      </view>

      <!-- 个人简介 -->
      <view class="form-item vertical">
        <view class="form-label"><text class="required">*</text>个人简介</view>
        <textarea
          class="form-textarea"
          v-model="form.resume"
          placeholder="请介绍一下您的艺术经历、代表作品等（50-500字）"
          maxlength="500"
        ></textarea>
        <view class="word-count">{{ form.resume.length }}/500</view>
      </view>

      <!-- 代表作品 -->
      <view class="form-item vertical">
        <view class="form-label"><text class="required">*</text>代表作品</view>
        <view class="works-upload">
          <view class="work-item" v-for="(work, index) in form.works" :key="index">
            <image :src="work" mode="aspectFill"></image>
            <view class="work-delete" @click="deleteWork(index)">
              
            </view>
          </view>
          <view class="work-add" v-if="form.works.length < MAX_WORK_COUNT" @click="chooseWork">
            
            <text>上传作品</text>
          </view>
        </view>
        <view class="upload-tip">请上传20件代表作品，当前已上传 {{ form.works.length }}/{{ MAX_WORK_COUNT }} 件</view>
      </view>
    </view>

    <!-- 联系方式 -->
    <view class="form-section card">
      <view class="form-title">联系方式</view>

      <!-- 手机号 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>手机号</view>
        <input
          class="form-input"
          v-model="form.phone"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>

      <!-- 验证码 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>验证码</view>
        <view class="code-input">
          <input
            class="form-input"
            v-model="form.code"
            type="number"
            placeholder="请输入验证码"
            maxlength="6"
          />
          <view
            class="code-btn"
            :class="{ disabled: countdown > 0 }"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </view>
        </view>
      </view>

      <!-- 邮箱 -->
      <view class="form-item">
        <view class="form-label">邮箱</view>
        <input
          class="form-input"
          v-model="form.email"
          type="text"
          placeholder="选填，用于接收审核通知"
        />
      </view>

      <!-- 社交媒体 -->
      <view class="form-item">
        <view class="form-label">社交媒体</view>
        <input
          class="form-input"
          v-model="form.socialMedia"
          placeholder="选填，如微博/小红书账号"
        />
      </view>
    </view>

    <!-- 身份证明 -->
    <view class="form-section card">
      <view class="form-title">身份证明</view>

      <!-- 身份证号 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>身份证号</view>
        <input
          class="form-input"
          v-model="form.idCard"
          placeholder="请输入18位身份证号"
          maxlength="18"
        />
      </view>

      <!-- 身份证正面 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>身份证正面</view>
        <view class="idcard-upload" @click="chooseIdCard('front')">
          <image v-if="form.idCardFront" :src="form.idCardFront" mode="aspectFit"></image>
          <view v-else class="upload-placeholder">
            
            <text>点击上传</text>
          </view>
        </view>
      </view>

      <!-- 身份证反面 -->
      <view class="form-item">
        <view class="form-label"><text class="required">*</text>身份证反面</view>
        <view class="idcard-upload" @click="chooseIdCard('back')">
          <image v-if="form.idCardBack" :src="form.idCardBack" mode="aspectFit"></image>
          <view v-else class="upload-placeholder">
            
            <text>点击上传</text>
          </view>
        </view>
      </view>

      <view class="idcard-tip">请确保身份证信息清晰可辨，信息仅用于身份验证</view>
    </view>

    <!-- 协议确认 -->
    <view class="agreement-section">
      <view class="agreement-check" :class="{ checked: agreed }" @click="agreeAgreement">
        <view class="check-box">
          <text v-if="agreed">✓</text>
        </view>
        <text>我已阅读并同意</text>
      </view>
      <text class="agreement-link" @click="showAgreement">《艺术家入驻协议》</text>
      <text class="agreement-link" @click="showPrivacy">《隐私政策》</text>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-section">
      <view
        class="submit-btn"
        :class="{ disabled: !canSubmit }"
        @click="submitApplication"
      >
        {{ submitting ? '提交中...' : '提交申请' }}
      </view>
    </view>
  </view>
</template>

<script>
import { openCropper, uploadFile } from '@/api/file.js'
import { getArtistCertStatus, sendSmsCode, submitArtistCert } from '@/api/user.js'

const MAX_WORK_COUNT = 20

const CITY_OPTIONS = [
  { name: '北京市', children: ['北京市'] },
  { name: '天津市', children: ['天津市'] },
  { name: '上海市', children: ['上海市'] },
  { name: '重庆市', children: ['重庆市'] },
  { name: '河北省', children: ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '保定市'] },
  { name: '山西省', children: ['太原市', '大同市', '晋中市', '临汾市', '运城市'] },
  { name: '辽宁省', children: ['沈阳市', '大连市', '鞍山市', '锦州市'] },
  { name: '吉林省', children: ['长春市', '吉林市', '延边州'] },
  { name: '黑龙江省', children: ['哈尔滨市', '齐齐哈尔市', '牡丹江市'] },
  { name: '江苏省', children: ['南京市', '苏州市', '无锡市', '常州市', '南通市'] },
  { name: '浙江省', children: ['杭州市', '宁波市', '温州市', '嘉兴市', '绍兴市'] },
  { name: '安徽省', children: ['合肥市', '芜湖市', '黄山市', '安庆市'] },
  { name: '福建省', children: ['福州市', '厦门市', '泉州市', '漳州市'] },
  { name: '江西省', children: ['南昌市', '景德镇市', '九江市', '赣州市'] },
  { name: '山东省', children: ['济南市', '青岛市', '烟台市', '潍坊市', '临沂市'] },
  { name: '河南省', children: ['郑州市', '洛阳市', '开封市', '南阳市'] },
  { name: '湖北省', children: ['武汉市', '宜昌市', '襄阳市', '黄石市'] },
  { name: '湖南省', children: ['长沙市', '株洲市', '湘潭市', '衡阳市'] },
  { name: '广东省', children: ['广州市', '深圳市', '珠海市', '佛山市', '东莞市'] },
  { name: '广西壮族自治区', children: ['南宁市', '桂林市', '柳州市', '北海市'] },
  { name: '海南省', children: ['海口市', '三亚市', '儋州市'] },
  { name: '四川省', children: ['成都市', '绵阳市', '乐山市', '德阳市'] },
  { name: '贵州省', children: ['贵阳市', '遵义市', '安顺市'] },
  { name: '云南省', children: ['昆明市', '大理市', '丽江市', '曲靖市'] },
  { name: '陕西省', children: ['西安市', '咸阳市', '宝鸡市', '延安市'] },
  { name: '甘肃省', children: ['兰州市', '敦煌市', '天水市'] },
  { name: '青海省', children: ['西宁市', '海东市'] },
  { name: '宁夏回族自治区', children: ['银川市', '吴忠市'] },
  { name: '新疆维吾尔自治区', children: ['乌鲁木齐市', '喀什市', '伊宁市'] },
  { name: '内蒙古自治区', children: ['呼和浩特市', '包头市', '鄂尔多斯市'] },
  { name: '西藏自治区', children: ['拉萨市', '日喀则市'] },
  { name: '香港特别行政区', children: ['香港'] },
  { name: '澳门特别行政区', children: ['澳门'] },
  { name: '台湾省', children: ['台北市', '高雄市', '台中市'] }
]

export default {
  data() {
    return {
      form: {
        avatar: '',
        name: '',
        artistName: '',
        gender: 1,
        birthYear: '',
        city: '',
        cityArr: ['', '', ''],
        artCategory: '',
        artCategoryIndex: 0,
        style: '',
        resume: '',  // 统一字段名
        works: [],
        phone: '',
        code: '',
        email: '',
        socialMedia: '',
        idCard: '',
        idCardFront: '',
        idCardBack: ''
      },
      cityPickerIndex: [0, 0],
      artCategories: [],
      countdown: 0,
      agreed: false,
      loadingCategories: true,
      submitting: false,
      certStatus: null,
      MAX_WORK_COUNT
    }
  },

  computed: {
    cityPickerRange() {
      const province = CITY_OPTIONS[this.cityPickerIndex[0]] || CITY_OPTIONS[0]
      return [CITY_OPTIONS.map(item => item.name), province.children]
    },
    canSubmit() {
      return (
        this.agreed &&
        this.form.name &&
        this.form.gender &&
        this.form.birthYear &&
        this.form.city &&
        this.form.artCategory &&
        this.form.resume &&
        this.form.works.length >= MAX_WORK_COUNT &&
        this.form.phone &&
        this.form.code &&
        this.form.idCard &&
        this.form.idCardFront &&
        this.form.idCardBack &&
        !this.submitting
      )
    }
  },

  onLoad() {
    this.loadCategories()
    this.loadCertStatus()
  },

  methods: {
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const path = res.tempFilePaths[0]
          openCropper(path, { ratio: '1:1', shape: 'circle', outputSize: 800 }).then(cropped => {
            this.form.avatar = cropped
          }).catch(() => {
            this.form.avatar = path
          })
        }
      })
    },

    onBirthYearChange(e) {
      this.form.birthYear = e.detail.value
    },

    onCityColumnChange(e) {
      const { column, value } = e.detail
      const next = [...this.cityPickerIndex]
      next[column] = value
      if (column === 0) next[1] = 0
      this.cityPickerIndex = next
    },

    onCityChange(e) {
      const [provinceIndex = 0, cityIndex = 0] = e.detail.value || []
      const province = CITY_OPTIONS[provinceIndex] || CITY_OPTIONS[0]
      const city = province.children[cityIndex] || province.children[0]
      this.cityPickerIndex = [provinceIndex, cityIndex]
      this.form.cityArr = [province.name, city]
      this.form.city = province.name === city ? city : `${province.name} ${city}`
    },

    onArtCategoryChange(e) {
      this.form.artCategoryIndex = e.detail.value
      const category = this.artCategories[e.detail.value]
      this.form.artCategory = category ? category.name : ''
    },

    chooseWork() {
      const remain = Math.min(MAX_WORK_COUNT - this.form.works.length, 9)
      uni.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const paths = res.tempFilePaths
          Promise.all(paths.map(p =>
            openCropper(p, { ratio: 'free', shape: 'square' }).catch(() => p)
          )).then(croppedList => {
            this.form.works = [...this.form.works, ...croppedList].slice(0, MAX_WORK_COUNT)
          })
        }
      })
    },

    deleteWork(index) {
      this.form.works.splice(index, 1)
    },

    chooseIdCard(type) {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          if (type === 'front') {
            this.form.idCardFront = res.tempFilePaths[0]
          } else {
            this.form.idCardBack = res.tempFilePaths[0]
          }
        }
      })
    },

    async sendCode() {
      if (this.countdown > 0) return
      if (!/^1[3-9]\d{9}$/.test(this.form.phone)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      let result
      try {
        result = await sendSmsCode(this.form.phone, 'artist_cert')
      } catch (e) {
        uni.showToast({ title: e.message || '验证码发送失败', icon: 'none' })
        return
      }
      this.countdown = 60
      const timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          clearInterval(timer)
        }
      }, 1000)
      if (result?.mock) {
        uni.showToast({ title: `测试验证码 ${result.code}`, icon: 'none', duration: 2500 })
      } else {
        uni.showToast({ title: '验证码已发送', icon: 'success' })
      }
    },

    agreeAgreement() {
      this.agreed = !this.agreed
    },

    showAgreement() {
      uni.navigateTo({ url: '/pages/user/agreement?type=artist' })
    },

    showPrivacy() {
      uni.navigateTo({ url: '/pages/user/agreement?type=privacy' })
    },

    async loadCategories() {
      try {
        const { getCategories } = await import('@/api/product.js')
        const list = await getCategories()
        if (list && list.length > 0) {
          this.artCategories = list.map(item => ({ id: item.id, name: item.name }))
        }
      } catch (e) {
        this.artCategories = [
          { id: 1, name: '国画' }, { id: 2, name: '油画' },
          { id: 3, name: '版画' }, { id: 4, name: '雕塑' },
          { id: 5, name: '书法' }, { id: 6, name: '摄影' },
          { id: 7, name: '水彩' }, { id: 8, name: '插画' },
          { id: 9, name: '数字艺术' }, { id: 10, name: '其他' }
        ]
      } finally {
        this.loadingCategories = false
      }
    },

    async loadCertStatus() {
      try {
        this.certStatus = await getArtistCertStatus()
      } catch (e) {
        this.certStatus = null
      }
    },

    validateForm() {
      if (!this.agreed) return '请先阅读并同意入驻协议'
      if (!this.form.name) return '请输入真实姓名'
      if (!this.form.birthYear) return '请选择出生年份'
      if (!this.form.city) return '请选择所在城市'
      if (!this.form.artCategory) return '请选择作品分类'
      if (!this.form.resume || this.form.resume.trim().length < 50) return '个人简介至少50字'
      if (this.form.works.length < MAX_WORK_COUNT) return `请上传${MAX_WORK_COUNT}件代表作品`
      if (!/^1[3-9]\d{9}$/.test(this.form.phone)) return '请输入正确的手机号'
      if (!/^\d{4,6}$/.test(this.form.code)) return '请输入正确验证码'
      if (!/^\d{17}[\dXx]$/.test(this.form.idCard)) return '请输入正确身份证号'
      if (!this.form.idCardFront || !this.form.idCardBack) return '请上传身份证正反面'
      return ''
    },

    async uploadImages(paths, type) {
      return Promise.all(paths.map(path => {
        if (!path || /^https?:\/\//.test(path) || path.startsWith('/upload/')) return path
        return uploadFile(path, type)
      }))
    },

    submitApplication() {
      const error = this.validateForm()
      if (error) {
        uni.showToast({ title: error, icon: 'none' })
        return
      }

      uni.showModal({
        title: '确认提交',
        content: '确认提交艺术家认证申请？',
        success: async (res) => {
          if (res.confirm) {
            this.submitting = true
            uni.showLoading({ title: '提交中...' })
            try {
              const artworks = await this.uploadImages(this.form.works, 'artist-artwork')
              const exhibits = await this.uploadImages([this.form.idCardFront, this.form.idCardBack].filter(Boolean), 'artist-cert')
              await submitArtistCert({
                realName: this.form.name.trim(),
                idCard: this.form.idCard.trim(),
                artField: this.form.artCategory,
                resume: this.form.resume.trim(),
                artworks,
                exhibits,
                phone: this.form.phone,
                verifyCode: this.form.code,
                city: this.form.city,
                gender: this.form.gender,
                birthYear: this.form.birthYear,
                artistName: this.form.artistName,
                style: this.form.style,
                email: this.form.email,
                socialMedia: this.form.socialMedia,
                avatar: this.form.avatar
              })
              uni.hideLoading()
              uni.showToast({ title: '提交成功，请等待审核', icon: 'success' })
              setTimeout(() => {
                uni.navigateBack()
              }, 1000)
            } catch (e) {
              uni.hideLoading()
              uni.showToast({ title: e.message || '提交失败，请重试', icon: 'none' })
            } finally {
              this.submitting = false
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.apply-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 20rpx;
  padding-bottom: 140rpx;
}

.notice-section {
  padding: 30rpx;

  .notice-header {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    text {
      margin-left: 12rpx;
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
  }

  .notice-content {
    .notice-item {
      font-size: 26rpx;
      color: #666;
      line-height: 1.8;
      margin-bottom: 12rpx;
    }
  }
}

.form-section {
  padding: 30rpx;
  margin-bottom: 20rpx;

  .form-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 30rpx;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .form-item {
    display: flex;
    align-items: center;
    margin-bottom: 30rpx;

    &.vertical {
      flex-direction: column;
      align-items: flex-start;
    }

    .form-label {
      width: 160rpx;
      font-size: 28rpx;
      color: #333;
      flex-shrink: 0;

      .required {
        color: #ff4d4f;
        margin-right: 4rpx;
      }
    }

    .form-input {
      flex: 1;
      height: 80rpx;
      padding: 0 20rpx;
      font-size: 28rpx;
      color: #333;
      background: #f5f5f5;
      border-radius: 8rpx;
    }

    .radio-group {
      display: flex;
      gap: 40rpx;

      .radio-item {
        padding: 16rpx 40rpx;
        font-size: 28rpx;
        color: #666;
        background: #f5f5f5;
        border-radius: 8rpx;
        border: 2rpx solid transparent;

        &.active {
          color: #667eea;
          background: rgba(102, 126, 234, 0.1);
          border-color: #667eea;
        }
      }
    }

    .picker-value {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 80rpx;
      padding: 0 20rpx;
      font-size: 28rpx;
      color: #333;
      background: #f5f5f5;
      border-radius: 8rpx;
    }

    .form-textarea {
      width: 100%;
      height: 200rpx;
      padding: 20rpx;
      font-size: 28rpx;
      color: #333;
      background: #f5f5f5;
      border-radius: 8rpx;
      box-sizing: border-box;
    }

    .word-count {
      align-self: flex-end;
      font-size: 24rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }

  .avatar-upload {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    overflow: hidden;
    background: #f5f5f5;

    .upload-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      text {
        font-size: 22rpx;
        color: #999;
        margin-top: 8rpx;
      }
    }
  }

  .works-upload {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;

    .work-item {
      width: 200rpx;
      height: 200rpx;
      border-radius: 12rpx;
      overflow: hidden;
      position: relative;

      .work-delete {
        position: absolute;
        top: 8rpx;
        right: 8rpx;
        width: 40rpx;
        height: 40rpx;
        background: rgba(0, 0, 0, 0.5);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .work-add {
      width: 200rpx;
      height: 200rpx;
      background: #f5f5f5;
      border-radius: 12rpx;
      border: 2rpx dashed #ddd;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      text {
        font-size: 24rpx;
        color: #999;
        margin-top: 8rpx;
      }
    }
  }

  .upload-tip {
    font-size: 24rpx;
    color: #999;
    margin-top: 12rpx;
  }

  .idcard-upload {
    width: 100%;
    height: 260rpx;
    border-radius: 12rpx;
    overflow: hidden;
    background: #f5f5f5;
    border: 2rpx dashed #ddd;

    image {
      width: 100%;
      height: 100%;
      display: block;
      background: #111;
    }

    .upload-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      text {
        font-size: 24rpx;
        color: #999;
        margin-top: 8rpx;
      }
    }
  }

  .idcard-tip {
    font-size: 24rpx;
    color: #999;
    margin-top: 16rpx;
  }

  .code-input {
    flex: 1;
    display: flex;
    gap: 16rpx;

    .form-input {
      flex: 1;
    }

    .code-btn {
      padding: 0 24rpx;
      font-size: 26rpx;
      color: #667eea;
      background: rgba(102, 126, 234, 0.1);
      border-radius: 8rpx;
      display: flex;
      align-items: center;

      &.disabled {
        color: #999;
        background: #f5f5f5;
      }
    }
  }
}

.agreement-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
  flex-wrap: wrap;

  .agreement-check {
    display: flex;
    align-items: center;
    min-height: 48rpx;

    .check-box {
      width: 32rpx;
      height: 32rpx;
      border-radius: 50%;
      border: 3rpx solid #b8b8b8;
      display: flex;
      align-items: center;
      justify-content: center;
      box-sizing: border-box;

      text {
        margin-left: 0;
        font-size: 22rpx;
        line-height: 1;
        color: #16130b;
        font-weight: 800;
      }
    }

    &.checked .check-box {
      border-color: #667eea;
      background: #667eea;
    }

    text {
      margin-left: 8rpx;
      font-size: 26rpx;
      color: #666;
    }
  }

  .agreement-link {
    font-size: 26rpx;
    color: #667eea;
    margin-left: 8rpx;
  }
}

.submit-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;

  .submit-btn {
    height: 88rpx;
    line-height: 88rpx;
    text-align: center;
    font-size: 32rpx;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 44rpx;

    &.disabled {
      background: #ccc;
    }
  }
}

/* 身份入口二级页：暗色重构覆盖层 */
.apply-page {
  background: #0b0b0c;
  color: #f6f2e8;
  padding: 24rpx;
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.card,
.notice-section,
.form-section {
  background: #171719;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 16rpx;
}

.notice-section {
  background:
    linear-gradient(135deg, rgba(201, 162, 39, 0.2), rgba(23, 23, 25, 0.96)),
    #171719;

  .notice-header text {
    color: #f6f2e8;
    margin-left: 0;
    font-size: 34rpx;
    line-height: 42rpx;
    font-weight: 800;
  }

  .notice-content .notice-item {
    color: #9b958a;
  }
}

.form-section {
  .form-title {
    color: #f6f2e8;
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .form-item {
    .form-label {
      color: #f6f2e8;
    }

    .form-input,
    .picker-value,
    .form-textarea {
      background: #202024;
      color: #f6f2e8;
    }

    .radio-group .radio-item {
      background: #202024;
      color: #9b958a;

      &.active {
        color: #16130b;
        background: #c9a227;
        border-color: #c9a227;
      }
    }

    .word-count {
      color: #68645c;
    }
  }

  .avatar-upload,
  .works-upload .work-add,
  .idcard-upload {
    background: #202024;
    border-color: rgba(255, 255, 255, 0.12);
  }

  .avatar-upload .upload-placeholder text,
  .works-upload .work-add text,
  .idcard-upload .upload-placeholder text,
  .upload-tip,
  .idcard-tip {
    color: #9b958a;
  }

  .code-input .code-btn {
    color: #c9a227;
    background: rgba(201, 162, 39, 0.14);

    &.disabled {
      color: #68645c;
      background: #202024;
    }
  }
}

.agreement-section {
  .agreement-check {
    .check-box {
      border-color: #68645c;
      background: rgba(255, 255, 255, 0.04);
    }

    &.checked .check-box {
      border-color: #c9a227;
      background: #c9a227;
      box-shadow: 0 0 0 6rpx rgba(201, 162, 39, 0.12);
    }
  }

  .agreement-check text {
    color: #9b958a;
  }

  .agreement-link {
    color: #c9a227;
  }
}

.submit-section {
  background: rgba(11, 11, 12, 0.96);
  border-top: 1rpx solid rgba(255, 255, 255, 0.08);

  .submit-btn {
    background: #c9a227;
    color: #16130b;

    &.disabled {
      background: #343436;
      color: #68645c;
    }
  }
}
</style>
