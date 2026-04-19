<template>
  <view class="profile-page">
    <!-- 用户信息区 -->
    <view class="profile-header">
      <view class="user-info" @click="goLogin">
        <image class="avatar" :src="userInfo?.avatar || '/static/default-avatar.png'" />
        <view class="info-text">
          <text class="nickname">{{ userInfo?.nickname || '点击登录' }}</text>
          <view class="identity-tags" v-if="userInfo?.identities">
            <text 
              v-for="id in identityList" 
              :key="id"
              class="identity-tag"
            >
              {{ getIdentityText(id) }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 身份入口 -->
    <view class="identity-cards" v-if="isLoggedIn">
      <view class="identity-card" @click="goArtistCenter" v-if="userInfo?.isArtist">
        <text class="card-icon">🎨</text>
        <text class="card-name">艺术家中心</text>
      </view>
      <view class="identity-card" @click="goPromoterCenter" v-if="userInfo?.isPromoter">
        <text class="card-icon">💰</text>
        <text class="card-name">艺荐官中心</text>
      </view>
      <view class="identity-card" @click="goAgentCenter" v-if="userInfo?.isAgent">
        <text class="card-icon">📋</text>
        <text class="card-name">经纪人中心</text>
      </view>
      <view class="identity-card" @click="goCertArtist" v-if="!userInfo?.isArtist">
        <text class="card-icon">✨</text>
        <text class="card-name">申请艺术家认证</text>
      </view>
    </view>

    <!-- 订单入口 -->
    <view class="order-section">
      <view class="section-header">
        <text class="section-title">我的订单</text>
        <text class="section-more" @click="goOrderList">全部订单 ›</text>
      </view>
      <view class="order-tabs">
        <view class="order-tab" @click="goOrderList('pending')">
          <text class="tab-icon">⏳</text>
          <text class="tab-text">待付款</text>
        </view>
        <view class="order-tab" @click="goOrderList('paid')">
          <text class="tab-icon">📦</text>
          <text class="tab-text">已付款</text>
        </view>
        <view class="order-tab" @click="goOrderList('shipped')">
          <text class="tab-icon">🚚</text>
          <text class="tab-text">已发货</text>
        </view>
        <view class="order-tab" @click="goOrderList('completed')">
          <text class="tab-icon">⭐</text>
          <text class="tab-text">已完成</text>
        </view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="menu-section">
      <view class="menu-item" @click="goFavorites">
        <text class="menu-icon">❤️</text>
        <text class="menu-text">我的收藏</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goBrowseHistory">
        <text class="menu-icon">🕐</text>
        <text class="menu-text">浏览足迹</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goAddress">
        <text class="menu-icon">📍</text>
        <text class="menu-text">收货地址</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goSettings">
        <text class="menu-icon">⚙️</text>
        <text class="menu-text">设置</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goHelp">
        <text class="menu-icon">❓</text>
        <text class="menu-text">帮助与反馈</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section" v-if="isLoggedIn">
      <button class="btn-logout" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const identityList = computed(() => userInfo.value?.identities?.split(',') || [])

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo()
  }
})

function getIdentityText(id) {
  const map = {
    artist: '艺术家',
    agent: '经纪人',
    collector: '收藏家',
    promoter: '艺荐官'
  }
  return map[id] || id
}

function goLogin() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/index/login' })
  }
}

function goArtistCenter() {
  uni.navigateTo({ url: '/pages/profile/artist' })
}

function goPromoterCenter() {
  uni.switchTab({ url: '/pages/promoter/index' })
}

function goAgentCenter() {
  uni.navigateTo({ url: '/pages/profile/agent' })
}

function goCertArtist() {
  uni.navigateTo({ url: '/pages/profile/cert-artist' })
}

function goOrderList(status) {
  const url = status ? `/pages/order/list?status=${status}` : '/pages/order/list'
  uni.navigateTo({ url })
}

function goFavorites() {
  uni.navigateTo({ url: '/pages/profile/favorites' })
}

function goBrowseHistory() {
  uni.navigateTo({ url: '/pages/profile/history' })
}

function goAddress() {
  uni.navigateTo({ url: '/pages/profile/address' })
}

function goSettings() {
  uni.navigateTo({ url: '/pages/profile/settings' })
}

function goHelp() {
  uni.navigateTo({ url: '/pages/profile/help' })
}

function logout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: env(safe-area-inset-bottom);
}

.profile-header {
  background: #fff;
  padding: 48rpx 32rpx;
  
  .user-info {
    display: flex;
    align-items: center;
    
    .avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
    }
    
    .info-text {
      margin-left: 24rpx;
      
      .nickname {
        display: block;
        font-size: 36rpx;
        font-weight: 600;
        color: #333;
        margin-bottom: 12rpx;
      }
      
      .identity-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8rpx;
        
        .identity-tag {
          font-size: 20rpx;
          padding: 4rpx 12rpx;
          background: rgba(201, 169, 98, 0.1);
          color: #c9a962;
          border-radius: 4rpx;
        }
      }
    }
  }
}

.identity-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  padding: 16rpx;
  background: #f5f5f5;
  
  .identity-card {
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 24rpx;
    background: #fff;
    border-radius: 12rpx;
    
    .card-icon {
      font-size: 40rpx;
    }
    
    .card-name {
      font-size: 26rpx;
      color: #333;
    }
  }
}

.order-section {
  background: #fff;
  margin-top: 16rpx;
  padding: 24rpx 32rpx;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    
    .section-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
    }
    
    .section-more {
      font-size: 26rpx;
      color: #999;
    }
  }
  
  .order-tabs {
    display: flex;
    justify-content: space-around;
    
    .order-tab {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8rpx;
      
      .tab-icon {
        font-size: 48rpx;
      }
      
      .tab-text {
        font-size: 24rpx;
        color: #666;
      }
    }
  }
}

.menu-section {
  background: #fff;
  margin-top: 16rpx;
  
  .menu-item {
    display: flex;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
    }
    
    .menu-icon {
      font-size: 36rpx;
      margin-right: 16rpx;
    }
    
    .menu-text {
      flex: 1;
      font-size: 28rpx;
      color: #333;
    }
    
    .menu-arrow {
      font-size: 32rpx;
      color: #ccc;
    }
  }
}

.logout-section {
  padding: 48rpx 32rpx;
  
  .btn-logout {
    width: 100%;
    height: 88rpx;
    background: #fff;
    color: #e53935;
    border-radius: 44rpx;
    font-size: 30rpx;
  }
}
</style>
