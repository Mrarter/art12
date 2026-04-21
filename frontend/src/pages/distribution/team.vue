<template>
  <view class="team-page">
    <!-- 团队概览 -->
    <view class="team-overview">
      <view class="overview-item">
        <text class="number">89</text>
        <text class="label">团队总人数</text>
      </view>
      <view class="divider"></view>
      <view class="overview-item">
        <text class="number">45</text>
        <text class="label">金牌分销商</text>
      </view>
      <view class="divider"></view>
      <view class="overview-item">
        <text class="number">44</text>
        <text class="label">普通分销商</text>
      </view>
    </view>

    <!-- Tab切换 -->
    <view class="tab-bar">
      <view class="tab-item" :class="{ active: currentTab === 'gold' }" @click="switchTab('gold')">
        金牌分销商(45)
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'normal' }" @click="switchTab('normal')">
        普通分销商(44)
      </view>
    </view>

    <!-- 成员列表 -->
    <scroll-view class="member-list" scroll-y>
      <view class="member-card" v-for="(member, index) in memberList" :key="index">
        <image class="avatar" :src="member.avatar" mode="aspectFill" />
        <view class="member-info">
          <view class="name-row">
            <text class="name">{{ member.name }}</text>
            <text class="level" :class="'level-' + member.level">{{ member.levelText }}</text>
          </view>
          <text class="invite-code">邀请码：{{ member.inviteCode }}</text>
          <view class="stats-row">
            <text>客户：{{ member.customerCount }}人</text>
            <text>本月收益：¥{{ member.monthEarnings }}</text>
          </view>
        </view>
        <view class="action-btns">
          <text class="btn-contact">联系</text>
          <text class="btn-detail">详情</text>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-if="memberList.length === 0">
        <text>暂无团队成员</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 'gold',
      memberList: [
        {
          avatar: '/static/avatar-default.png',
          name: '李明',
          level: 1,
          levelText: '金牌',
          inviteCode: 'LM2024001',
          customerCount: 28,
          monthEarnings: '1,850'
        },
        {
          avatar: '/static/avatar-default.png',
          name: '王芳',
          level: 1,
          levelText: '金牌',
          inviteCode: 'WF2024002',
          customerCount: 35,
          monthEarnings: '2,280'
        },
        {
          avatar: '/static/avatar-default.png',
          name: '张伟',
          level: 1,
          levelText: '金牌',
          inviteCode: 'ZW2024003',
          customerCount: 22,
          monthEarnings: '1,420'
        }
      ]
    }
  },
  methods: {
    switchTab(tab) {
      this.currentTab = tab
      if (tab === 'gold') {
        this.memberList = [
          { avatar: '/static/avatar-default.png', name: '李明', level: 1, levelText: '金牌', inviteCode: 'LM2024001', customerCount: 28, monthEarnings: '1,850' },
          { avatar: '/static/avatar-default.png', name: '王芳', level: 1, levelText: '金牌', inviteCode: 'WF2024002', customerCount: 35, monthEarnings: '2,280' },
          { avatar: '/static/avatar-default.png', name: '张伟', level: 1, levelText: '金牌', inviteCode: 'ZW2024003', customerCount: 22, monthEarnings: '1,420' }
        ]
      } else {
        this.memberList = [
          { avatar: '/static/avatar-default.png', name: '赵丽', level: 0, levelText: '普通', inviteCode: 'ZL2024004', customerCount: 15, monthEarnings: '860' },
          { avatar: '/static/avatar-default.png', name: '孙强', level: 0, levelText: '普通', inviteCode: 'SQ2024005', customerCount: 12, monthEarnings: '680' }
        ]
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.team-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.team-overview {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  justify-content: space-around;
  padding: 40rpx 30rpx;
}

.overview-item {
  text-align: center;
}

.overview-item .number {
  font-size: 48rpx;
  font-weight: 700;
  display: block;
}

.overview-item .label {
  font-size: 24rpx;
  opacity: 0.8;
  margin-top: 8rpx;
  display: block;
}

.divider {
  width: 2rpx;
  background: rgba(255, 255, 255, 0.3);
}

.tab-bar {
  background: #fff;
  display: flex;
  border-bottom: 2rpx solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 30rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #667eea;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 6rpx;
  background: #667eea;
  border-radius: 3rpx;
}

.member-list {
  padding: 24rpx;
  height: calc(100vh - 300rpx);
}

.member-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}

.member-info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.level {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
}

.level-1 {
  background: linear-gradient(135deg, #f5af19 0%, #f12711 100%);
  color: #fff;
}

.level-0 {
  background: #e0e0e0;
  color: #666;
}

.invite-code {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}

.stats-row {
  display: flex;
  gap: 24rpx;
  margin-top: 12rpx;
}

.stats-row text {
  font-size: 24rpx;
  color: #666;
}

.action-btns {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.btn-contact,
.btn-detail {
  font-size: 22rpx;
  padding: 10rpx 20rpx;
  border-radius: 8rpx;
  text-align: center;
}

.btn-contact {
  background: #667eea;
  color: #fff;
}

.btn-detail {
  background: #f0f0f0;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}
</style>