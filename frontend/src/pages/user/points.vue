<template>
  <view class="points-page">
    <!-- 积分概览 -->
    <view class="points-overview">
      <view class="points-main">
        <text class="points-label">我的积分</text>
        <text class="points-value">{{ currentPoints }}</text>
      </view>
      <view class="points-hint">
        <text class="hint-text">当前积分</text>
      </view>
    </view>

    <!-- 积分规则 -->
    <view class="rules-card" @click="viewRules">
      <view class="rules-info">
        <text class="rules-title">积分规则</text>
        <text class="rules-desc">了解如何获取和使用积分</text>
      </view>
      <text class="arrow">></text>
    </view>

    <!-- 积分任务 -->
    <view class="task-section">
      <text class="section-title">赚积分</text>
      <view class="task-list">
        <view class="task-item" v-for="(task, index) in tasks" :key="index">
          <view class="task-icon">
            <image :src="task.icon" mode="aspectFit" />
          </view>
          <view class="task-info">
            <text class="task-name">{{ task.name }}</text>
            <text class="task-reward">+{{ task.reward }}积分</text>
          </view>
          <view class="task-action" v-if="!task.completed" @click="doTask(task)">
            <text class="action-btn">{{ task.actionText }}</text>
          </view>
          <view class="task-done" v-else>
            <text class="done-text">已完成</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 积分商城入口 -->
    <view class="mall-banner" @click="goToMall">
      <view class="banner-content">
        <text class="banner-title">积分商城</text>
        <text class="banner-desc">更多好物，积分兑换</text>
      </view>
      <view class="banner-btn">
        <text>立即前往</text>
      </view>
    </view>

    <!-- 积分明细 -->
    <view class="detail-section">
      <view class="detail-header">
        <text class="section-title">积分明细</text>
        <text class="more-btn">查看全部</text>
      </view>
      <view class="detail-list">
        <view class="detail-item" v-for="(item, index) in detailList" :key="index">
          <view class="detail-info">
            <text class="detail-title">{{ item.title }}</text>
            <text class="detail-time">{{ item.time }}</text>
          </view>
          <text class="detail-points" :class="{ positive: item.points > 0 }">
            {{ item.points > 0 ? '+' : '' }}{{ item.points }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentPoints: 12580,
      tasks: [
        { id: 1, name: '每日签到', reward: 10, icon: '/static/icon-sign.png', actionText: '去签到', completed: false },
        { id: 2, name: '分享作品', reward: 5, icon: '/static/icon-share.png', actionText: '去分享', completed: true },
        { id: 3, name: '完成订单', reward: 100, icon: '/static/icon-order.png', actionText: '去购物', completed: false },
        { id: 4, name: '评价作品', reward: 20, icon: '/static/icon-review.png', actionText: '去评价', completed: false },
        { id: 5, name: '完善资料', reward: 50, icon: '/static/icon-profile.png', actionText: '去完善', completed: true }
      ],
      detailList: [
        { title: '每日签到', time: '2024-03-15 09:00', points: 10 },
        { title: '分享作品奖励', time: '2024-03-14 15:30', points: 5 },
        { title: '购买艺术品', time: '2024-03-12 10:20', points: -1000 },
        { title: '订单评价奖励', time: '2024-03-10 14:00', points: 20 },
        { title: '新人注册奖励', time: '2024-03-01 10:00', points: 500 }
      ]
    }
  },
  methods: {
    viewRules() {
      uni.showModal({
        title: '积分规则',
        content: '1. 签到：每日签到可获得10积分\n2. 分享：分享作品每次获得5积分\n3. 购物：每消费1元获得1积分\n4. 评价：评价作品获得20积分\n5. 积分可抵扣现金使用',
        showCancel: false
      })
    },
    doTask(task) {
      if (task.id === 1) {
        uni.showToast({ title: '签到成功 +10积分', icon: 'success' })
        task.completed = true
        this.currentPoints += 10
      } else {
        uni.showToast({ title: task.actionText, icon: 'none' })
      }
    },
    goToMall() {
      uni.showToast({ title: '积分商城开发中', icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.points-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.points-overview {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 60rpx 40rpx;
  text-align: center;
}

.points-main {
  margin-bottom: 16rpx;
}

.points-label {
  font-size: 28rpx;
  opacity: 0.8;
  display: block;
  margin-bottom: 12rpx;
}

.points-value {
  font-size: 72rpx;
  font-weight: 700;
  display: block;
}

.points-hint {
  font-size: 24rpx;
  opacity: 0.7;
}

.rules-card {
  background: #fff;
  margin: -30rpx 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.rules-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.rules-desc {
  font-size: 24rpx;
  color: #999;
}

.arrow {
  color: #ccc;
  font-size: 28rpx;
}

.task-section {
  background: #fff;
  margin: 0 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 24rpx;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.task-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
}

.task-icon {
  width: 80rpx;
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 16rpx;
  margin-right: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-icon image {
  width: 48rpx;
  height: 48rpx;
}

.task-info {
  flex: 1;
}

.task-name {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 6rpx;
}

.task-reward {
  font-size: 24rpx;
  color: #667eea;
}

.action-btn {
  font-size: 24rpx;
  color: #667eea;
  background: #f0f4ff;
  padding: 10rpx 20rpx;
  border-radius: 20rpx;
}

.done-text {
  font-size: 24rpx;
  color: #52c41a;
  background: #f6ffed;
  padding: 10rpx 20rpx;
  border-radius: 20rpx;
}

.mall-banner {
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b35 100%);
  margin: 0 24rpx 24rpx;
  border-radius: 16rpx;
  padding: 30rpx;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.banner-title {
  font-size: 32rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 8rpx;
}

.banner-desc {
  font-size: 24rpx;
  opacity: 0.9;
}

.banner-btn {
  background: rgba(255, 255, 255, 0.2);
  padding: 16rpx 24rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
}

.detail-section {
  background: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.more-btn {
  font-size: 24rpx;
  color: #667eea;
}

.detail-list {
  display: flex;
  flex-direction: column;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-title {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 6rpx;
}

.detail-time {
  font-size: 24rpx;
  color: #999;
}

.detail-points {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.detail-points.positive {
  color: #e63946;
}
</style>