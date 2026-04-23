<template>
  <view class="profile-page">
    <!-- 顶部背景 -->
    <view class="profile-header">
      <view class="header-bg"></view>
      <view class="header-content">
        <!-- 头像 -->
        <view class="avatar-section">
          <view class="avatar-wrapper" @click="changeAvatar">
            <image class="avatar" :src="userInfo.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
            <view class="avatar-edit">
              
            </view>
          </view>
        </view>

        <!-- 用户名 -->
        <view class="name-section">
          <text class="user-name">{{ userInfo.nickname || '未设置昵称' }}</text>
          <view class="identity-tags">
            <text class="identity-tag" v-if="userInfo.isArtist">艺术家</text>
            <text class="identity-tag promoter" v-if="userInfo.isPromoter">艺荐官</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 统计数据 -->
    <view class="stats-section card">
      <view class="stat-item" @click="goPage('followers')">
        <text class="stat-value">{{ userInfo.followersCount || 0 }}</text>
        <text class="stat-label">粉丝</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item" @click="goPage('following')">
        <text class="stat-value">{{ userInfo.followingCount || 0 }}</text>
        <text class="stat-label">关注</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item" @click="goPage('likes')">
        <text class="stat-value">{{ userInfo.likesCount || 0 }}</text>
        <text class="stat-label">获赞</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value">{{ userInfo.worksCount || 0 }}</text>
        <text class="stat-label">作品</text>
      </view>
    </view>

    <!-- 编辑资料按钮 -->
    <view class="edit-section card">
      <view class="edit-btn" @click="goEditProfile">
        
        <text>编辑资料</text>
      </view>
    </view>

    <!-- 信息列表 -->
    <view class="info-section card">
      <view class="info-title">基本信息</view>
      <view class="info-list">
        <view class="info-item" @click="editField('nickname')">
          <view class="info-left">
            
            <text class="info-label">昵称</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.nickname || '未设置' }}</text>
            
          </view>
        </view>

        <view class="info-item" @click="editField('bio')">
          <view class="info-left">
            
            <text class="info-label">简介</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.bio || '未填写' }}</text>
            
          </view>
        </view>

        <view class="info-item" @click="editField('gender')">
          <view class="info-left">
            
            <text class="info-label">性别</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ getGenderText(userInfo.gender) }}</text>
            
          </view>
        </view>

        <view class="info-item" @click="editField('birthday')">
          <view class="info-left">
            
            <text class="info-label">生日</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.birthday || '未设置' }}</text>
            
          </view>
        </view>

        <view class="info-item" @click="editField('location')">
          <view class="info-left">
            
            <text class="info-label">地区</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.location || '未设置' }}</text>
            
          </view>
        </view>
      </view>
    </view>

    <!-- 联系方式 -->
    <view class="info-section card">
      <view class="info-title">联系方式</view>
      <view class="info-list">
        <view class="info-item">
          <view class="info-left">
            
            <text class="info-label">手机号</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ formatPhone(userInfo.phone) }}</text>
          </view>
        </view>

        <view class="info-item" @click="editField('email')">
          <view class="info-left">
            
            <text class="info-label">邮箱</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.email || '未绑定' }}</text>
            
          </view>
        </view>

        <view class="info-item" @click="editField('wechat')">
          <view class="info-left">
            
            <text class="info-label">微信</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.wechat || '未绑定' }}</text>
            
          </view>
        </view>
      </view>
    </view>

    <!-- 艺术家信息 -->
    <view class="info-section card" v-if="userInfo.isArtist">
      <view class="info-title">艺术家认证</view>
      <view class="info-list">
        <view class="info-item">
          <view class="info-left">
            <text>★</text>
            <text class="info-label">艺术门类</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.artCategory || '未设置' }}</text>
          </view>
        </view>

        <view class="info-item">
          <view class="info-left">
            <text>♡</text>
            <text class="info-label">创作风格</text>
          </view>
          <view class="info-right">
            <text class="info-value">{{ userInfo.artStyle || '未设置' }}</text>
          </view>
        </view>

        <view class="info-item">
          <view class="info-left">
            
            <text class="info-label">认证状态</text>
          </view>
          <view class="info-right">
            <text class="info-value status-certified">已认证</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 其他设置 -->
    <view class="info-section card">
      <view class="info-title">其他</view>
      <view class="info-list">
        <view class="info-item" @click="showPrivacy">
          <view class="info-left">
            <text>🔒</text>
            <text class="info-label">隐私设置</text>
          </view>
          <view class="info-right">
            
          </view>
        </view>

        <view class="info-item" @click="goPage('/pages/user/settings')">
          <view class="info-left">
            
            <text class="info-label">账户设置</text>
          </view>
          <view class="info-right">
            
          </view>
        </view>
      </view>
    </view>

    <!-- 底部安全提示 -->
    <view class="security-tip">
      
      <text>为了账户安全，部分信息需验证后修改</text>
    </view>

    <!-- 编辑弹窗 -->
    <!-- 弹窗开始 -->
      <view class="edit-popup">
        <view class="popup-title">编辑{{ editFieldName }}</view>
        <input
          v-if="editType !== 'gender' && editType !== 'birthday'"
          class="popup-input"
          v-model="editValue"
          :placeholder="editPlaceholder"
        />
        <picker
          v-else-if="editType === 'gender'"
          mode="selector"
          :range="genderOptions"
          @change="onGenderChange"
        >
          <view class="popup-picker">{{ editValue || '请选择' }}</view>
        </picker>
        <picker
          v-else-if="editType === 'birthday'"
          mode="date"
          :value="editValue"
          @change="onBirthdayChange"
        >
          <view class="popup-picker">{{ editValue || '请选择日期' }}</view>
        </picker>
        <view class="popup-actions">
          <view class="popup-btn cancel" @click="showEditPopup = false">取消</view>
          <view class="popup-btn confirm" @click="saveEdit">保存</view>
        </view>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'

export default {
  data() {
    return {
      showEditPopup: false,
      editType: '',
      editValue: '',
      editFieldName: '',
      editPlaceholder: '',
      genderOptions: ['未设置', '男', '女'],
      userInfo: {
        avatar: 'https://picsum.photos/200/200?random=50',
        nickname: '艺术爱好者',
        bio: '热爱艺术，收藏美好',
        gender: 1,
        birthday: '1995-06-15',
        location: '浙江省杭州市',
        phone: '138****8888',
        email: 'user@example.com',
        wechat: 'ArtLover2024',
        followersCount: 256,
        followingCount: 89,
        likesCount: 1234,
        worksCount: 12,
        isArtist: true,
        isPromoter: false,
        artCategory: '油画',
        artStyle: '抽象派'
      }
    }
  },

  methods: {
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.userInfo.avatar = res.tempFilePaths[0]
          uni.showToast({ title: '头像已更新', icon: 'success' })
        }
      })
    },

    goPage(page) {
      if (page === 'followers') {
        uni.navigateTo({ url: '/pages/common/coming-soon?title=我的粉丝&desc=粉丝列表页正在开发中，后续会补齐用户关系数据。' })
      } else if (page === 'following') {
        uni.navigateTo({ url: '/pages/user/following' })
      } else if (page === 'likes') {
        uni.navigateTo({ url: '/pages/common/coming-soon?title=获赞记录&desc=获赞记录页正在开发中，后续会补齐互动统计。' })
      } else {
        uni.navigateTo({ url: page })
      }
    },

    goEditProfile() {
      uni.navigateTo({ url: '/pages/common/coming-soon?title=编辑资料&desc=独立编辑资料页正在开发中，当前页面已支持部分字段修改。' })
    },

    getGenderText(gender) {
      const map = { 0: '未设置', 1: '男', 2: '女' }
      return map[gender] || '未设置'
    },

    formatPhone(phone) {
      if (!phone) return '未绑定'
      return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
    },

    editField(type) {
      const fieldMap = {
        nickname: { name: '昵称', placeholder: '请输入昵称', value: this.userInfo.nickname },
        bio: { name: '简介', placeholder: '请输入个人简介', value: this.userInfo.bio },
        gender: { name: '性别', placeholder: '', value: this.getGenderText(this.userInfo.gender) },
        birthday: { name: '生日', placeholder: '', value: this.userInfo.birthday },
        location: { name: '地区', placeholder: '请输入所在地区', value: this.userInfo.location },
        email: { name: '邮箱', placeholder: '请输入邮箱', value: this.userInfo.email },
        wechat: { name: '微信', placeholder: '请输入微信号', value: this.userInfo.wechat }
      }
      const field = fieldMap[type]
      if (field) {
        this.editType = type
        this.editFieldName = field.name
        this.editPlaceholder = field.placeholder
        this.editValue = field.value
        this.showEditPopup = true
      }
    },

    onGenderChange(e) {
      this.editValue = this.genderOptions[e.detail.value]
    },

    onBirthdayChange(e) {
      this.editValue = e.detail.value
    },

    saveEdit() {
      uni.showToast({ title: '保存成功', icon: 'success' })
      this.showEditPopup = false
    },

    showPrivacy() {
      uni.navigateTo({ url: '/pages/user/agreement?type=privacy' })
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding-bottom: 60rpx;
}

.profile-header {
  position: relative;

  .header-bg {
    height: 300rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  .header-content {
    position: absolute;
    top: 120rpx;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    flex-direction: column;
    align-items: center;

    .avatar-section {
      .avatar-wrapper {
        position: relative;
        width: 160rpx;
        height: 160rpx;
        border-radius: 50%;
        border: 6rpx solid #fff;
        overflow: hidden;

        .avatar {
          width: 100%;
          height: 100%;
        }

        .avatar-edit {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 48rpx;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
    }

    .name-section {
      margin-top: 24rpx;
      text-align: center;

      .user-name {
        font-size: 36rpx;
        font-weight: 600;
        color: #fff;
      }

      .identity-tags {
        display: flex;
        justify-content: center;
        gap: 12rpx;
        margin-top: 12rpx;

        .identity-tag {
          padding: 6rpx 16rpx;
          font-size: 22rpx;
          color: #fff;
          background: rgba(255, 255, 255, 0.3);
          border-radius: 20rpx;

          &.promoter {
            background: rgba(255, 193, 7, 0.8);
          }
        }
      }
    }
  }
}

.stats-section {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 30rpx 0;
  margin: -60rpx 20rpx 0;

  .stat-item {
    flex: 1;
    text-align: center;

    .stat-value {
      display: block;
      font-size: 40rpx;
      font-weight: 600;
      color: #333;
    }

    .stat-label {
      display: block;
      font-size: 24rpx;
      color: #999;
      margin-top: 8rpx;
    }
  }

  .stat-divider {
    width: 1rpx;
    height: 60rpx;
    background: #eee;
  }
}

.edit-section {
  margin: 20rpx;
  padding: 0;

  .edit-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24rpx;

    text {
      margin-left: 12rpx;
      font-size: 28rpx;
      color: #667eea;
    }
  }
}

.info-section {
  margin: 20rpx;
  padding: 0;

  .info-title {
    padding: 24rpx 30rpx;
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
    border-bottom: 1rpx solid #f5f5f5;
  }

  .info-list {
    padding: 0 30rpx;

    .info-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 28rpx 0;
      border-bottom: 1rpx solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .info-left {
        display: flex;
        align-items: center;

        .info-label {
          margin-left: 16rpx;
          font-size: 28rpx;
          color: #333;
        }
      }

      .info-right {
        display: flex;
        align-items: center;

        .info-value {
          font-size: 28rpx;
          color: #999;
          max-width: 400rpx;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;

          &.status-certified {
            color: #52c41a;
          }
        }
      }
    }
  }
}

.security-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  gap: 8rpx;

  text {
    font-size: 24rpx;
    color: #999;
  }
}

.edit-popup {
  width: 600rpx;
  padding: 40rpx;

  .popup-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .popup-input {
    width: 100%;
    height: 88rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    background: #f5f5f5;
    border-radius: 12rpx;
  }

  .popup-picker {
    height: 88rpx;
    padding: 0 24rpx;
    font-size: 28rpx;
    background: #f5f5f5;
    border-radius: 12rpx;
    display: flex;
    align-items: center;
  }

  .popup-actions {
    display: flex;
    gap: 24rpx;
    margin-top: 40rpx;

    .popup-btn {
      flex: 1;
      height: 80rpx;
      line-height: 80rpx;
      text-align: center;
      font-size: 28rpx;
      border-radius: 40rpx;

      &.cancel {
        color: #666;
        background: #f5f5f5;
      }

      &.confirm {
        color: #fff;
        background: #667eea;
      }
    }
  }
}
</style>
