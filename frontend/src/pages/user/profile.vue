<template>
  <view class="profile-page">
    <view v-if="!isLoggedIn" class="login-panel">
      <image class="login-avatar" src="/static/images/avatar.png" mode="aspectFill"></image>
      <text class="login-title">登录后编辑个人资料</text>
      <text class="login-desc">完善头像、昵称和简介，让收藏家、艺术家与经纪人身份都更清晰。</text>
      <view class="login-btn" @click="goPage('/pages/login/index')">去登录</view>
    </view>

    <template v-else>
      <view class="hero-card">
        <view class="avatar-wrap" @click="changeAvatar">
          <image class="avatar" :src="draft.avatar || '/static/images/avatar.png'" mode="aspectFill"></image>
          <view class="avatar-action">换</view>
        </view>
        <view class="hero-main">
          <input
            class="nickname-input"
            v-model.trim="draft.nickname"
            maxlength="24"
            placeholder="输入昵称"
            placeholder-class="placeholder"
          />
          <text class="uid-line">UID {{ userInfo.uid || userInfo.id || '-' }}</text>
          <view class="identity-tags">
            <text
              class="identity-tag"
              v-for="item in identityTags"
              :key="item.key"
              :class="item.key"
            >
              {{ item.label }}
            </text>
          </view>
        </view>
      </view>

      <view class="completion-card">
        <view class="completion-head">
          <view>
            <text class="section-kicker">资料完整度</text>
            <text class="completion-title">{{ completionText }}</text>
          </view>
          <text class="completion-percent">{{ completionPercent }}%</text>
        </view>
        <view class="progress-track">
          <view class="progress-bar" :style="{ width: completionPercent + '%' }"></view>
        </view>
        <view class="missing-list" v-if="missingFields.length">
          <text class="missing-item" v-for="item in missingFields" :key="item">{{ item }}</text>
        </view>
      </view>

      <view class="form-section">
        <view class="section-head">
          <text class="section-title">基本资料</text>
          <text class="section-note">对外展示</text>
        </view>

        <view class="field-block">
          <text class="field-label">个人简介</text>
          <textarea
            class="textarea"
            v-model.trim="draft.bio"
            maxlength="120"
            placeholder="写一句你和艺术、收藏或创作有关的介绍"
            placeholder-class="placeholder"
          />
          <text class="count">{{ (draft.bio || '').length }}/120</text>
        </view>

        <view class="field-row">
          <text class="field-label">性别</text>
          <picker mode="selector" :range="genderOptions" :value="draft.gender" @change="onGenderChange">
            <view class="picker-value">{{ genderOptions[draft.gender] || '未设置' }}</view>
          </picker>
        </view>

        <view class="field-row">
          <text class="field-label">生日</text>
          <picker mode="date" :value="draft.birthday" @change="onBirthdayChange">
            <view class="picker-value">{{ draft.birthday || '选择日期' }}</view>
          </picker>
        </view>

        <view class="field-row">
          <text class="field-label">所在地区</text>
          <picker
            mode="multiSelector"
            :range="cityPickerRange"
            :value="cityPickerIndex"
            @columnchange="onCityColumnChange"
            @change="onCityChange"
          >
            <view class="picker-value region-picker-value">
              {{ draft.region || '请选择省 / 城市' }}
            </view>
          </picker>
        </view>
      </view>

      <view class="form-section">
        <view class="section-head">
          <text class="section-title">联系信息</text>
          <text class="section-note">仅用于账号与服务</text>
        </view>

        <view class="field-row">
          <text class="field-label">手机号</text>
          <input
            class="contact-input"
            v-model.trim="draft.phone"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            placeholder-class="placeholder"
          />
        </view>

        <view class="field-row">
          <text class="field-label">邮箱</text>
          <input
            class="contact-input"
            v-model.trim="draft.email"
            maxlength="64"
            placeholder="请输入邮箱"
            placeholder-class="placeholder"
          />
        </view>

        <view class="field-row">
          <text class="field-label">微信号</text>
          <input
            class="contact-input"
            v-model.trim="draft.wechat"
            maxlength="32"
            placeholder="请输入微信号"
            placeholder-class="placeholder"
          />
        </view>
      </view>

      <view class="form-section">
        <view class="section-head">
          <text class="section-title">身份资料</text>
          <text class="section-note">按身份开放能力</text>
        </view>

        <view class="identity-row" v-for="item in identityCards" :key="item.key">
          <view class="identity-icon" :class="item.key">{{ item.short }}</view>
          <view class="identity-main">
            <text class="identity-title">{{ item.title }}</text>
            <text class="identity-desc">{{ item.desc }}</text>
          </view>
          <view class="identity-action" v-if="item.path" @click="goPage(item.path)">{{ item.action }}</view>
          <text class="identity-status" v-else>{{ item.status }}</text>
        </view>
      </view>

      <view class="safe-tip">
        <text>敏感资料需要验证后修改；昵称、头像、简介和地区会同步到个人主页。</text>
      </view>

      <view class="save-bar">
        <view class="save-secondary" @click="resetDraft" :class="{ disabled: !isDirty || saving }">还原</view>
        <view class="save-primary" @click="saveProfile" :class="{ disabled: !isDirty || saving }">
          {{ saving ? saveButtonText : '保存修改' }}
        </view>
      </view>
    </template>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'
import { openCropper, uploadFile } from '@/api/file.js'
import { updateUserInfo } from '@/api/user.js'

const EDITABLE_FIELDS = ['avatar', 'nickname', 'bio', 'gender', 'birthday', 'region', 'phone', 'email', 'wechat']
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
const createEmptyDraft = () => ({
  avatar: '',
  nickname: '',
  bio: '',
  gender: 0,
  birthday: '',
  region: '',
  phone: '',
  email: '',
  wechat: ''
})

export default {
  data() {
    return {
      saving: false,
      saveButtonText: '保存中...',
      cityPickerIndex: [0, 0],
      genderOptions: ['未设置', '男', '女'],
      draft: createEmptyDraft(),
      original: createEmptyDraft()
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    },
    isLoggedIn() {
      return this.userStore.isAuthenticated || this.userStore.isLogin
    },
    userInfo() {
      return this.userStore.userInfo || {}
    },
    identities() {
      return this.userStore.identities && this.userStore.identities.length
        ? this.userStore.identities
        : ['collector']
    },
    identityTags() {
      const map = {
        collector: '收藏家',
        artist: '艺术家',
        promoter: '经纪人',
        agent: '代理人'
      }
      return this.identities.map(key => ({ key, label: map[key] || key }))
    },
    identityCards() {
      const cards = [
        {
          key: 'collector',
          short: '藏',
          title: '收藏家资料',
          desc: '用于收藏、订单、转售和证书归档',
          status: '已启用'
        }
      ]

      if (this.identities.includes('artist')) {
        cards.push({
          key: 'artist',
          short: '艺',
          title: '艺术家主页资料',
          desc: '作品发布、艺术家主页和认证信息在此维护',
          action: '管理',
          path: '/pages/artist/home'
        })
        cards.push({
          key: 'artist-signature',
          short: '签',
          title: '艺术家手写签名',
          desc: '用于收藏证书签发区展示',
          action: '创建',
          path: '/pages/artist/signature'
        })
      } else {
        cards.push({
          key: 'artist',
          short: '艺',
          title: '艺术家身份',
          desc: '提交认证后可发布作品与维护主页',
          action: '申请',
          path: '/pages/artist/apply'
        })
      }

      if (this.identities.includes('promoter')) {
        cards.push({
          key: 'promoter',
          short: '荐',
          title: '经纪人资料',
          desc: '推广海报、收益账户和团队关系独立管理',
          action: '进入',
          path: '/pages/promoter/index'
        })
      }

      if (this.identities.includes('agent')) {
        cards.push({
          key: 'agent',
          short: '代',
          title: '代理人资料',
          desc: '代理协议、客户归属和成交协作资料',
          status: '待开放'
        })
      }

      return cards
    },
    completionPercent() {
      const required = ['avatar', 'nickname', 'bio', 'gender', 'birthday', 'region']
      const done = required.filter(key => {
        if (key === 'gender') return Number(this.draft.gender) > 0
        return !!this.draft[key]
      }).length
      return Math.round((done / required.length) * 100)
    },
    completionText() {
      if (this.completionPercent >= 100) return '资料完整，展示状态很好'
      if (this.completionPercent >= 67) return '还差一点就完整'
      return '先补齐基础展示信息'
    },
    cityPickerRange() {
      const province = CITY_OPTIONS[this.cityPickerIndex[0]] || CITY_OPTIONS[0]
      return [CITY_OPTIONS.map(item => item.name), province.children]
    },
    missingFields() {
      const labels = {
        avatar: '头像',
        nickname: '昵称',
        bio: '简介',
        gender: '性别',
        birthday: '生日',
        region: '地区'
      }
      return Object.keys(labels).filter(key => {
        if (key === 'gender') return Number(this.draft.gender) <= 0
        return !this.draft[key]
      }).map(key => labels[key])
    },
    isDirty() {
      return EDITABLE_FIELDS.some(key => String(this.draft[key] ?? '') !== String(this.original[key] ?? ''))
    }
  },

  async onShow() {
    if (!this.isLoggedIn) return
    if (this.isDirty) return
    const info = await this.userStore.fetchUserInfo()
    this.syncDraft(info || this.userInfo)
  },

  methods: {
    buildDraft(info = {}) {
      return {
        avatar: info.avatar || '',
        nickname: info.nickname || '',
        bio: info.bio || '',
        gender: Number(info.gender) || 0,
        birthday: info.birthday || '',
        region: info.region || info.location || '',
        phone: info.phone || '',
        email: info.email || '',
        wechat: info.wechat || ''
      }
    },
    syncDraft(info) {
      const next = this.buildDraft(info)
      this.draft = { ...next }
      this.original = { ...next }
      this.cityPickerIndex = this.resolveCityPickerIndex(next.region)
    },
    resolveCityPickerIndex(region) {
      const value = String(region || '').trim()
      if (!value) return [0, 0]
      for (let provinceIndex = 0; provinceIndex < CITY_OPTIONS.length; provinceIndex += 1) {
        const province = CITY_OPTIONS[provinceIndex]
        const provinceMatched = value.includes(province.name)
        const cityIndex = province.children.findIndex(city => value.includes(city))
        if (provinceMatched || cityIndex >= 0) {
          return [provinceIndex, cityIndex >= 0 ? cityIndex : 0]
        }
      }
      return [0, 0]
    },
    resetDraft() {
      if (!this.isDirty || this.saving) return
      this.draft = { ...this.original }
      this.cityPickerIndex = this.resolveCityPickerIndex(this.original.region)
      uni.showToast({ title: '已还原', icon: 'none' })
    },
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const path = res.tempFilePaths[0]
          openCropper(path, { ratio: '1:1', shape: 'circle', outputSize: 800 }).then(cropped => {
            this.draft.avatar = cropped
          }).catch(() => {
            this.draft.avatar = path
          })
        }
      })
    },
    onGenderChange(e) {
      this.draft.gender = Number(e.detail.value) || 0
    },
    onBirthdayChange(e) {
      this.draft.birthday = e.detail.value
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
      const city = province.children[cityIndex] || province.children[0] || ''
      this.cityPickerIndex = [provinceIndex, cityIndex]
      this.draft.region = province.name === city ? city : `${province.name} ${city}`
    },
    formatPhone(phone) {
      if (!phone) return '未绑定'
      return String(phone).replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },
    validateDraft() {
      if (!this.draft.nickname) {
        uni.showToast({ title: '请填写昵称', icon: 'none' })
        return false
      }
      if (this.draft.phone && !/^1[3-9]\d{9}$/.test(this.draft.phone)) {
        uni.showToast({ title: '请输入正确手机号', icon: 'none' })
        return false
      }
      if (this.draft.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.draft.email)) {
        uni.showToast({ title: '请输入正确邮箱', icon: 'none' })
        return false
      }
      return true
    },
    buildPayload() {
      const payload = {}
      EDITABLE_FIELDS.forEach(key => {
        if (String(this.draft[key] ?? '') !== String(this.original[key] ?? '')) {
          payload[key] = this.draft[key]
        }
      })
      return payload
    },
    async ensureUploaded(path, type = 'avatar') {
      if (!path) return ''
      if (/^https?:\/\//.test(path)) return path
      if (/^data:image\//.test(path) || /^blob:/.test(path)) return uploadFile(path, type)
      if (path.startsWith('/upload/')) return path
      if (path.startsWith('/static/')) return path
      return uploadFile(path, type)
    },
    async saveProfile() {
      if (!this.isDirty || this.saving) return
      if (!this.validateDraft()) return

      this.saving = true
      this.saveButtonText = '保存中...'
      try {
        const payload = this.buildPayload()
        if (payload.avatar) {
          this.saveButtonText = '上传头像中...'
          payload.avatar = await this.ensureUploaded(payload.avatar)
          this.draft.avatar = payload.avatar
        }
        this.saveButtonText = '保存中...'
        await updateUserInfo(payload)
        const merged = {
          ...this.userInfo,
          ...payload,
          location: payload.region !== undefined ? payload.region : this.userInfo.location
        }
        this.userStore.setUserInfo(merged)
        this.original = { ...this.draft }
        uni.showToast({ title: '保存成功', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error.message || '保存失败', icon: 'none' })
      } finally {
        this.saving = false
      }
    },
    goPage(page) {
      uni.navigateTo({ url: page })
    }
  }
}
</script>

<style lang="scss" scoped>
$bg: #0b0b0c;
$panel: #171719;
$panel2: #202024;
$line: rgba(255, 255, 255, 0.08);
$text: #f6f2e8;
$muted: #9b958a;
$dim: #68645c;
$gold: #c9a227;
$green: #58b982;
$blue: #5f8fc7;
$purple: #8c73c9;
$danger: #d86b6b;

.profile-page {
  min-height: 100vh;
  background: $bg;
  color: $text;
  padding: 24rpx 24rpx 156rpx;
  box-sizing: border-box;
}

.login-panel,
.hero-card,
.completion-card,
.form-section {
  background: $panel;
  border: 1rpx solid $line;
  border-radius: 16rpx;
}

.login-panel {
  min-height: calc(100vh - 48rpx);
  padding: 80rpx 48rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.login-avatar {
  width: 132rpx;
  height: 132rpx;
  border-radius: 50%;
  margin-bottom: 28rpx;
  border: 2rpx solid rgba($gold, 0.38);
}

.login-title {
  font-size: 36rpx;
  font-weight: 800;
}

.login-desc {
  margin-top: 14rpx;
  color: $muted;
  font-size: 25rpx;
  line-height: 38rpx;
}

.login-btn {
  margin-top: 34rpx;
  width: 260rpx;
  height: 82rpx;
  border-radius: 12rpx;
  background: $gold;
  color: #17130a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 800;
}

.hero-card {
  padding: 30rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  background:
    radial-gradient(circle at 12% 0%, rgba($gold, 0.24), transparent 40%),
    $panel;
}

.avatar-wrap {
  position: relative;
  width: 128rpx;
  height: 128rpx;
  flex-shrink: 0;
}

.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 2rpx solid rgba($gold, 0.42);
  background: $panel2;
}

.avatar-action {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $gold;
  color: #17130a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 800;
}

.hero-main {
  flex: 1;
  min-width: 0;
}

.nickname-input {
  height: 56rpx;
  color: $text;
  font-size: 36rpx;
  font-weight: 800;
}

.uid-line {
  display: block;
  margin-top: 8rpx;
  color: $dim;
  font-size: 22rpx;
}

.identity-tags,
.missing-list {
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.identity-tags {
  margin-top: 16rpx;
}

.identity-tag,
.missing-item {
  padding: 6rpx 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  color: $gold;
  background: rgba($gold, 0.15);
}

.identity-tag.artist,
.identity-icon.artist {
  color: $green;
  background: rgba($green, 0.15);
}

.identity-tag.promoter,
.identity-icon.promoter {
  color: $blue;
  background: rgba($blue, 0.15);
}

.identity-tag.agent,
.identity-icon.agent {
  color: $purple;
  background: rgba($purple, 0.15);
}

.completion-card,
.form-section {
  margin-top: 20rpx;
  padding: 26rpx;
}

.completion-head,
.section-head,
.field-row,
.identity-row,
.save-bar {
  display: flex;
  align-items: center;
}

.completion-head,
.section-head,
.field-row,
.identity-row {
  justify-content: space-between;
}

.section-kicker,
.section-note,
.count {
  color: $dim;
  font-size: 22rpx;
}

.completion-title,
.section-title {
  display: block;
  margin-top: 6rpx;
  font-size: 30rpx;
  font-weight: 800;
}

.completion-percent {
  color: $gold;
  font-size: 42rpx;
  font-weight: 900;
}

.progress-track {
  height: 12rpx;
  margin-top: 24rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, $gold, $green);
}

.missing-list {
  margin-top: 18rpx;
}

.missing-item {
  color: $muted;
  background: rgba(255, 255, 255, 0.06);
}

.section-head {
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid $line;
}

.field-block {
  padding: 24rpx 0;
  border-bottom: 1rpx solid $line;
}

.field-label {
  color: $text;
  font-size: 27rpx;
  font-weight: 700;
}

.textarea {
  width: 100%;
  min-height: 148rpx;
  margin-top: 18rpx;
  padding: 22rpx;
  border-radius: 12rpx;
  background: $panel2;
  color: $text;
  font-size: 27rpx;
  line-height: 40rpx;
  box-sizing: border-box;
}

.count {
  display: block;
  text-align: right;
  margin-top: 10rpx;
}

.field-row {
  min-height: 100rpx;
  border-bottom: 1rpx solid $line;

  &:last-child {
    border-bottom: none;
  }
}

.picker-value,
.readonly-value,
.contact-input {
  color: $text;
  font-size: 27rpx;
  text-align: right;
}

.picker-value,
.readonly-value,
.contact-input {
  max-width: 430rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.readonly-value {
  color: $muted;
}

.region-picker-value {
  min-width: 320rpx;
  margin-left: 28rpx;
}

.contact-input {
  flex: 1;
  min-width: 0;
  height: 88rpx;
  margin-left: 28rpx;
}

.readonly {
  opacity: 0.78;
}

.identity-row {
  gap: 18rpx;
  min-height: 116rpx;
  border-bottom: 1rpx solid $line;

  &:last-child {
    border-bottom: none;
  }
}

.identity-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 14rpx;
  flex-shrink: 0;
  color: $gold;
  background: rgba($gold, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 800;
}

.identity-main {
  flex: 1;
  min-width: 0;
}

.identity-title,
.identity-desc {
  display: block;
}

.identity-title {
  font-size: 27rpx;
  font-weight: 800;
}

.identity-desc {
  margin-top: 6rpx;
  color: $muted;
  font-size: 22rpx;
  line-height: 32rpx;
}

.identity-action,
.identity-status {
  flex-shrink: 0;
  font-size: 24rpx;
}

.identity-action {
  color: $gold;
  font-weight: 800;
}

.identity-status {
  color: $dim;
}

.safe-tip {
  padding: 20rpx 8rpx 0;
  color: $muted;
  font-size: 23rpx;
  line-height: 36rpx;
}

.save-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  gap: 16rpx;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(11, 11, 12, 0.92);
  border-top: 1rpx solid $line;
  box-sizing: border-box;
}

.save-secondary,
.save-primary {
  height: 86rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 800;
}

.save-secondary {
  width: 190rpx;
  color: $gold;
  background: rgba($gold, 0.12);
}

.save-primary {
  flex: 1;
  color: #17130a;
  background: $gold;
}

.disabled {
  opacity: 0.45;
}

.placeholder {
  color: $dim;
}
</style>
