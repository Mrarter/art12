<template>
  <view class="promoter-page">
    <!-- 头部收益信息 -->
    <view class="earnings-header">
      <view class="earnings-bg">
        <view class="earnings-summary">
          <text class="summary-label">累计佣金</text>
          <text class="summary-value">¥{{ promoterInfo.totalCommission }}</text>
        </view>
        <view class="earnings-stats">
          <view class="stat-item">
            <text class="stat-label">可提现</text>
            <text class="stat-value">¥{{ promoterInfo.withdrawable }}</text>
          </view>
          <view class="stat-item">
            <text class="stat-label">已提现</text>
            <text class="stat-value">¥{{ promoterInfo.withdrawn }}</text>
          </view>
          <view class="stat-item">
            <text class="stat-label">团队订单</text>
            <text class="stat-value">{{ promoterInfo.teamOrders }}</text>
          </view>
        </view>
      </view>
      <view class="withdraw-btn" @click="goWithdraw">立即提现</view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-entry">
      <view class="entry-item" v-for="item in entryList" :key="item.id" @click="goEntry(item)">
        <image class="entry-icon" :src="item.icon" mode="aspectFit"></image>
        <text class="entry-text">{{ item.name }}</text>
      </view>
    </view>

    <!-- 数据统计 -->
    <view class="stats-section">
      <view class="section-title">本月数据</view>
      <view class="stats-grid">
        <view class="stat-card">
          <text class="stat-value">{{ monthStats.orders }}</text>
          <text class="stat-label">订单数</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">{{ monthStats.sales }}</text>
          <text class="stat-label">销售额</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">¥{{ monthStats.commission }}</text>
          <text class="stat-label">佣金收益</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">{{ monthStats.newFans }}</text>
          <text class="stat-label">新增粉丝</text>
        </view>
      </view>
    </view>

    <!-- 团队列表 -->
    <view class="team-section">
      <view class="section-header">
        <text class="section-title">我的团队</text>
        <view class="team-count">{{ teamList.length }}人</view>
      </view>
      <scroll-view class="team-list" scroll-x>
        <view class="team-member" v-for="member in teamList" :key="member.id" @click="goMemberHome(member.id)">
          <image class="member-avatar" :src="member.avatar" mode="aspectFill"></image>
          <text class="member-name">{{ member.name }}</text>
          <text class="member-orders">{{ member.orders }}单</text>
        </view>
      </scroll-view>
    </view>

    <!-- 佣金明细 -->
    <view class="commission-section">
      <view class="section-header">
        <text class="section-title">佣金明细</text>
        <view class="more-btn" @click="goCommissionList">查看更多</view>
      </view>
      <view class="commission-list">
        <view class="commission-item" v-for="item in commissionList" :key="item.id">
          <view class="commission-info">
            <text class="commission-product">{{ item.productName }}</text>
            <text class="commission-time">{{ item.createTime }}</text>
          </view>
          <view class="commission-amount">+¥{{ item.amount }}</view>
        </view>
      </view>
    </view>

    <!-- 推广海报 -->
    <view class="poster-section">
      <view class="poster-card" @click="goPoster">
        <image class="poster-icon" src="/static/icons/poster.png" mode="aspectFit"></image>
        <view class="poster-info">
          <text class="poster-title">推广海报</text>
          <text class="poster-desc">生成专属推广海报，分享好友</text>
        </view>
        <u-icon name="arrow-right" size="18" color="#999"></u-icon>
      </view>
    </view>

    <!-- 邀请码 -->
    <view class="invite-section">
      <view class="invite-card">
        <text class="invite-title">我的邀请码</text>
        <view class="invite-code">
          <text class="code">{{ promoterInfo.inviteCode }}</text>
          <view class="copy-btn" @click="copyCode">复制</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getPromoterHome, getCommissionList, getTeamList } from '@/api/promoter.js'

export default {
  data() {
    return {
      promoterInfo: {
        totalCommission: 0,
        withdrawable: 0,
        withdrawn: 0,
        teamOrders: 0,
        inviteCode: ''
      },
      monthStats: {
        orders: 0,
        sales: 0,
        commission: 0,
        newFans: 0
      },
      entryList: [
        { id: 'commission', name: '佣金明细', icon: '/static/icons/commission.png', path: '/pages/promoter/commission' },
        { id: 'team', name: '团队管理', icon: '/static/icons/team.png', path: '/pages/promoter/team' },
        { id: 'withdraw', name: '提现记录', icon: '/static/icons/withdraw.png', path: '/pages/promoter/withdraw' },
        { id: 'rank', name: '排行榜', icon: '/static/icons/rank.png', path: '/pages/promoter/rank' }
      ],
      teamList: [],
      commissionList: []
    }
  },
  
  onLoad() {
    this.loadPromoterInfo()
  },
  
  onShow() {
    this.loadPromoterInfo()
  },
  
  methods: {
    async loadPromoterInfo() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getPromoterHome()
        this.promoterInfo = res
        this.monthStats = res.monthStats || {}
        this.teamList = res.teamList || []
        this.commissionList = res.commissionList || []
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        this.loadMockData()
      }
    },
    
    loadMockData() {
      this.promoterInfo = {
        totalCommission: 12880,
        withdrawable: 5680,
        withdrawn: 7200,
        teamOrders: 156,
        inviteCode: 'SYJ2024001'
      }
      
      this.monthStats = {
        orders: 23,
        sales: '¥128,000',
        commission: 6400,
        newFans: 56
      }
      
      this.teamList = [
        { id: 1, name: '张三', avatar: '/static/avatar/demo.jpg', orders: 12 },
        { id: 2, name: '李四', avatar: '/static/avatar/demo.jpg', orders: 8 },
        { id: 3, name: '王五', avatar: '/static/avatar/demo.jpg', orders: 5 }
      ]
      
      this.commissionList = [
        { id: 1, productName: '山水长卷', amount: 640, createTime: '2024-01-19' },
        { id: 2, productName: '虾趣图', amount: 440, createTime: '2024-01-18' },
        { id: 3, productName: '奔马图', amount: 1280, createTime: '2024-01-17' }
      ]
    },
    
    goWithdraw() {
      uni.navigateTo({ url: `/pages/promoter/withdraw?amount=${this.promoterInfo.withdrawable}` })
    },
    
    goEntry(item) {
      uni.navigateTo({ url: item.path })
    },
    
    goCommissionList() {
      uni.navigateTo({ url: '/pages/promoter/commission' })
    },
    
    goMemberHome(id) {
      uni.navigateTo({ url: `/pages/artist/home?id=${id}` })
    },
    
    goPoster() {
      uni.navigateTo({ url: '/pages/promoter/poster' })
    },
    
    copyCode() {
      uni.setClipboardData({
        data: this.promoterInfo.inviteCode,
        success: () => {
          uni.showToast({ title: '复制成功', icon: 'success' })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.promoter-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.earnings-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx 30rpx;
  position: relative;
}

.earnings-bg {
  padding-bottom: 60rpx;
}

.earnings-summary {
  text-align: center;
  margin-bottom: 40rpx;
}

.summary-label {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 16rpx;
}

.summary-value {
  font-size: 56rpx;
  color: #fff;
  font-weight: 600;
}

.earnings-stats {
  display: flex;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8rpx;
}

.stat-value {
  font-size: 32rpx;
  color: #fff;
  font-weight: 600;
}

.withdraw-btn {
  position: absolute;
  right: 30rpx;
  bottom: -30rpx;
  width: 160rpx;
  height: 70rpx;
  line-height: 70rpx;
  background: #fff;
  color: #667eea;
  border-radius: 35rpx;
  font-size: 28rpx;
  font-weight: 500;
  text-align: center;
  box-shadow: 0 4rpx 20rpx rgba(102, 126, 234, 0.3);
}

.quick-entry {
  display: flex;
  background: #fff;
  margin: 50rpx 20rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx 0;
}

.entry-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.entry-icon {
  width: 56rpx;
  height: 56rpx;
  margin-bottom: 12rpx;
}

.entry-text {
  font-size: 24rpx;
  color: #666;
}

.stats-section {
  background: #fff;
  margin: 0 20rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 30rpx;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
}

.stat-card {
  text-align: center;
  padding: 20rpx 0;
  background: #f9f9f9;
  border-radius: 12rpx;
}

.stat-card .stat-value {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.stat-card .stat-label {
  font-size: 22rpx;
  color: #999;
}

.team-section {
  background: #fff;
  margin: 0 20rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.team-count {
  font-size: 24rpx;
  color: #999;
}

.team-list {
  display: flex;
  white-space: nowrap;
}

.team-member {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  margin-right: 40rpx;
}

.member-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-bottom: 12rpx;
}

.member-name {
  font-size: 24rpx;
  color: #333;
  margin-bottom: 4rpx;
}

.member-orders {
  font-size: 20rpx;
  color: #999;
}

.commission-section {
  background: #fff;
  margin: 0 20rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.more-btn {
  font-size: 24rpx;
  color: #999;
}

.commission-list {
  margin-top: 20rpx;
}

.commission-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.commission-item:last-child {
  border-bottom: none;
}

.commission-product {
  display: block;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 6rpx;
}

.commission-time {
  font-size: 22rpx;
  color: #999;
}

.commission-amount {
  font-size: 30rpx;
  color: #e74c3c;
  font-weight: 600;
}

.poster-section {
  margin: 0 20rpx 20rpx;
}

.poster-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
}

.poster-icon {
  width: 80rpx;
  height: 80rpx;
  margin-right: 20rpx;
}

.poster-info {
  flex: 1;
}

.poster-title {
  display: block;
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.poster-desc {
  font-size: 24rpx;
  color: #999;
}

.invite-section {
  margin: 0 20rpx;
}

.invite-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
}

.invite-title {
  display: block;
  font-size: 28rpx;
  color: #666;
  margin-bottom: 16rpx;
}

.invite-code {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f9f9f9;
  padding: 24rpx 30rpx;
  border-radius: 12rpx;
}

.code {
  font-size: 36rpx;
  color: #333;
  font-weight: 600;
  letter-spacing: 4rpx;
}

.copy-btn {
  padding: 12rpx 30rpx;
  background: #667eea;
  color: #fff;
  border-radius: 30rpx;
  font-size: 24rpx;
}
</style>
