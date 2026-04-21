<template>
  <view class="promoter-page">
    <!-- 头部收益信息 -->
    <view class="earnings-header">
      <view class="total-earnings">
        <text class="label">累计佣金 (元)</text>
        <text class="value">{{ stats.totalCommission || 0 }}</text>
      </view>
      <view class="earnings-detail">
        <view class="detail-item">
          <text class="item-label">已提现</text>
          <text class="item-value">{{ stats.withdrawn || 0 }}</text>
        </view>
        <view class="detail-item">
          <text class="item-label">可提现</text>
          <text class="item-value highlight">{{ stats.withdrawable || 0 }}</text>
        </view>
        <view class="detail-item">
          <text class="item-label">预估佣金</text>
          <text class="item-value">{{ stats.estimateCommission || 0 }}</text>
        </view>
      </view>
      <button class="withdraw-btn" @click="goWithdraw">立即提现</button>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-cards">
      <view class="stat-card" @click="goTeamList">
        <text class="card-value">{{ stats.teamCount || 0 }}</text>
        <text class="card-label">团队成员</text>
      </view>
      <view class="stat-card" @click="goEarningsList">
        <text class="card-value">{{ stats.orderCount || 0 }}</text>
        <text class="card-label">推广订单</text>
      </view>
      <view class="stat-card" @click="goEarningsList('invite')">
        <text class="card-value">{{ stats.inviteCount || 0 }}</text>
        <text class="card-label">邀请好友</text>
      </view>
    </view>

    <!-- 收益趋势图 -->
    <view class="earnings-chart card">
      <view class="card-header">
        <text class="card-title">收益趋势</text>
        <view class="period-tabs">
          <text 
            v-for="item in periodOptions" 
            :key="item.value"
            :class="['period-tab', { active: selectedPeriod === item.value }]"
            @click="changePeriod(item.value)"
          >{{ item.label }}</text>
        </view>
      </view>
      <view class="chart-container">
        <!-- 简单柱状图展示 -->
        <view class="simple-chart" v-if="trendData.length > 0">
          <view class="chart-bars">
            <view 
              v-for="(item, index) in trendData" 
              :key="index"
              class="bar-item"
            >
              <view 
                class="bar" 
                :style="{ height: (item.amount / maxTrendValue * 120) + 'rpx' }"
              ></view>
              <text class="bar-label">{{ item.label }}</text>
            </view>
          </view>
        </view>
        <view class="chart-empty" v-else>
          <text>暂无数据</text>
        </view>
      </view>
      <view class="chart-legend">
        <view class="legend-item">
          <view class="legend-color" style="background: #667eea;"></view>
          <text>订单佣金</text>
        </view>
      </view>
    </view>

    <!-- 团队管理 -->
    <view class="team-section card">
      <view class="card-header">
        <text class="card-title">团队管理</text>
        <view class="more-link" @click="goTeamList">
          <text>查看全部</text>
          <u-icon name="arrow-right" size="12" color="#999"></u-icon>
        </view>
      </view>
      <view class="team-list">
        <view class="team-item" v-for="item in teamList" :key="item.userId" @click="goTeamDetail(item.userId)">
          <image class="team-avatar" :src="item.avatar || '/static/avatar/default.jpg'" mode="aspectFill"></image>
          <view class="team-info">
            <text class="team-name">{{ item.nickname }}</text>
            <text class="team-level">{{ item.level === 1 ? '一级成员' : '二级成员' }}</text>
          </view>
          <view class="team-earnings">
            <text class="earnings-text">贡献佣金</text>
            <text class="earnings-value">¥{{ item.commission || 0 }}</text>
          </view>
        </view>
        <view class="team-empty" v-if="teamList.length === 0">
          <text>暂无比员</text>
        </view>
      </view>
    </view>

    <!-- 邀请入口 -->
    <view class="invite-section card">
      <view class="card-header">
        <text class="card-title">邀请注册</text>
      </view>
      <view class="invite-content">
        <view class="invite-code">
          <text class="code-label">我的推荐码</text>
          <view class="code-box">
            <text class="code-value">{{ myCode || '暂无' }}</text>
            <view class="copy-btn" @click="copyCode">
              <text>复制</text>
            </view>
          </view>
        </view>
        <button class="invite-btn" @click="showShareModal">邀请好友</button>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-section card">
      <view class="menu-item" @click="goEarningsList('order')">
        <u-icon name="order" size="22" color="#667eea"></u-icon>
        <text>订单佣金明细</text>
        <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
      </view>
      <view class="menu-item" @click="goWithdrawList">
        <u-icon name="red-packet" size="22" color="#ff6b6b"></u-icon>
        <text>提现记录</text>
        <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
      </view>
      <view class="menu-item" @click="showInviteGuide">
        <u-icon name="question-circle" size="22" color="#50c878"></u-icon>
        <text>如何成为艺荐官</text>
        <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
      </view>
    </view>
  </view>
</template>

<script>
import { getPromoterCenter, getPromoterStats, getEarningsTrend, getTeamList, getMyCode } from '@/api/promoter.js'
import { useUserStore } from '@/store/modules/user.js'

export default {
  data() {
    return {
      stats: {
        totalCommission: 0,
        withdrawn: 0,
        withdrawable: 0,
        estimateCommission: 0,
        teamCount: 0,
        orderCount: 0,
        inviteCount: 0
      },
      trendData: [],
      maxTrendValue: 0,
      teamList: [],
      myCode: '',
      selectedPeriod: 'month',
      periodOptions: [
        { label: '周', value: 'week' },
        { label: '月', value: 'month' },
        { label: '季', value: 'quarter' },
        { label: '年', value: 'year' }
      ]
    }
  },

  computed: {
    userStore() {
      return useUserStore()
    }
  },

  onLoad() {
    this.loadData()
  },

  onShow() {
    this.loadData()
  },

  methods: {
    async loadData() {
      if (!this.userStore.isAuthenticated) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      
      await Promise.all([
        this.loadStats(),
        this.loadTrendData(),
        this.loadTeamList(),
        this.loadMyCode()
      ])
    },

    async loadStats() {
      try {
        const res = await getPromoterStats()
        this.stats = res
      } catch (e) {
        console.error('加载统计数据失败', e)
      }
    },

    async loadTrendData() {
      try {
        const res = await getEarningsTrend(this.selectedPeriod)
        if (res && res.length > 0) {
          this.trendData = res
          this.maxTrendValue = Math.max(...res.map(item => item.amount))
        }
      } catch (e) {
        console.error('加载收益趋势失败', e)
      }
    },

    async loadTeamList() {
      try {
        const res = await getTeamList({ page: 1, pageSize: 5 })
        this.teamList = res.list || []
      } catch (e) {
        console.error('加载团队列表失败', e)
      }
    },

    async loadMyCode() {
      try {
        const res = await getMyCode()
        this.myCode = res.code || ''
      } catch (e) {
        console.error('获取推荐码失败', e)
      }
    },

    changePeriod(period) {
      this.selectedPeriod = period
      this.loadTrendData()
    },

    goWithdraw() {
      uni.navigateTo({ url: '/pages/promoter/withdraw' })
    },

    goEarningsList(type) {
      uni.navigateTo({ url: `/pages/promoter/earnings?type=${type || 'order'}` })
    },

    goTeamList() {
      uni.navigateTo({ url: '/pages/promoter/team' })
    },

    goTeamDetail(userId) {
      uni.navigateTo({ url: `/pages/promoter/team-detail?userId=${userId}` })
    },

    goWithdrawList() {
      uni.navigateTo({ url: '/pages/promoter/withdraw-list' })
    },

    copyCode() {
      if (!this.myCode) return
      uni.setClipboardData({
        data: this.myCode,
        success: () => {
          uni.showToast({ title: '已复制推荐码', icon: 'success' })
        }
      })
    },

    showShareModal() {
      uni.showModal({
        title: '邀请好友',
        content: `我的推荐码：${this.myCode || '暂无'}\n\n将此推荐码分享给好友，好友注册时填写即可成为您的下线成员。`,
        showCancel: false
      })
    },

    showInviteGuide() {
      uni.showModal({
        title: '如何成为艺荐官',
        content: '1. 累计推广订单满10单\n2. 或累计佣金满1000元\n满足任一条件即可申请成为艺荐官，享受更多佣金福利！',
        showCancel: false
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
  padding: 40rpx 30rpx 60rpx;
  color: #fff;
}

.total-earnings {
  text-align: center;
  margin-bottom: 30rpx;
  
  .label {
    display: block;
    font-size: 26rpx;
    opacity: 0.8;
    margin-bottom: 10rpx;
  }
  
  .value {
    font-size: 60rpx;
    font-weight: 600;
  }
}

.earnings-detail {
  display: flex;
  justify-content: center;
  gap: 60rpx;
  margin-bottom: 30rpx;
  
  .detail-item {
    text-align: center;
    
    .item-label {
      display: block;
      font-size: 22rpx;
      opacity: 0.7;
      margin-bottom: 6rpx;
    }
    
    .item-value {
      font-size: 32rpx;
      font-weight: 500;
    }
    
    .item-value.highlight {
      color: #ffd700;
    }
  }
}

.withdraw-btn {
  display: block;
  width: 200rpx;
  height: 70rpx;
  line-height: 70rpx;
  margin: 0 auto;
  background: #fff;
  color: #667eea;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 35rpx;
  text-align: center;
}

.stats-cards {
  display: flex;
  padding: 0 20rpx;
  margin-top: -40rpx;
  gap: 20rpx;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 20rpx;
  text-align: center;
  
  .card-value {
    display: block;
    font-size: 36rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 10rpx;
  }
  
  .card-label {
    font-size: 22rpx;
    color: #999;
  }
}

.card {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.more-link {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #999;
  
  text {
    margin-right: 6rpx;
  }
}

.period-tabs {
  display: flex;
  gap: 20rpx;
}

.period-tab {
  font-size: 24rpx;
  color: #999;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  
  &.active {
    background: #667eea;
    color: #fff;
  }
}

.chart-container {
  height: 200rpx;
}

.chart-empty {
  height: 200rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.simple-chart {
  height: 100%;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 160rpx;
  padding-top: 40rpx;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .bar {
    width: 40rpx;
    background: linear-gradient(180deg, #667eea 0%, #a78bfa 100%);
    border-radius: 8rpx 8rpx 0 0;
    margin-bottom: 10rpx;
  }
  
  .bar-label {
    font-size: 20rpx;
    color: #999;
  }
}

.chart-legend {
  display: flex;
  justify-content: center;
  margin-top: 20rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  color: #666;
  
  .legend-color {
    width: 20rpx;
    height: 20rpx;
    border-radius: 4rpx;
    margin-right: 8rpx;
  }
}

.team-list {
  .team-item {
    display: flex;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;
    
    &:last-child {
      border-bottom: none;
    }
  }
  
  .team-avatar {
    width: 60rpx;
    height: 60rpx;
    border-radius: 50%;
    margin-right: 16rpx;
  }
  
  .team-info {
    flex: 1;
    
    .team-name {
      display: block;
      font-size: 28rpx;
      color: #333;
      margin-bottom: 4rpx;
    }
    
    .team-level {
      font-size: 22rpx;
      color: #999;
    }
  }
  
  .team-earnings {
    text-align: right;
    
    .earnings-text {
      display: block;
      font-size: 20rpx;
      color: #999;
      margin-bottom: 4rpx;
    }
    
    .earnings-value {
      font-size: 26rpx;
      color: #e74c3c;
      font-weight: 600;
    }
  }
  
  .team-empty {
    text-align: center;
    padding: 40rpx;
    color: #ccc;
    font-size: 26rpx;
  }
}

.invite-content {
  .invite-code {
    margin-bottom: 24rpx;
    
    .code-label {
      font-size: 24rpx;
      color: #999;
      margin-bottom: 12rpx;
    }
    
    .code-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: #f5f5f5;
      padding: 20rpx 24rpx;
      border-radius: 12rpx;
      
      .code-value {
        font-size: 32rpx;
        font-weight: 600;
        color: #667eea;
        letter-spacing: 4rpx;
      }
      
      .copy-btn {
        padding: 10rpx 24rpx;
        background: #667eea;
        color: #fff;
        font-size: 24rpx;
        border-radius: 20rpx;
      }
    }
  }
  
  .invite-btn {
    width: 100%;
    height: 80rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 30rpx;
    font-weight: 600;
    border-radius: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.menu-section {
  padding: 0;
  
  .menu-item {
    display: flex;
    align-items: center;
    padding: 30rpx 24rpx;
    border-bottom: 1rpx solid #f5f5f5;
    
    &:last-child {
      border-bottom: none;
    }
    
    text {
      flex: 1;
      font-size: 28rpx;
      color: #333;
      margin-left: 20rpx;
    }
  }
}
</style>