<template>
  <view class="funnel-page">
    <view class="header">
      <view class="page-title">转化漏斗分析</view>
      <view class="date-selector">
        <picker mode="date" :value="startDate" fields="month" @change="onStartDateChange">
          <view class="date-item">
            <text>{{ startDate }}</text>
            <u-icon name="calendar" size="12"></u-icon>
          </view>
        </picker>
        <text class="date-separator">至</text>
        <picker mode="date" :value="endDate" fields="month" @change="onEndDateChange">
          <view class="date-item">
            <text>{{ endDate }}</text>
            <u-icon name="calendar" size="12"></u-icon>
          </view>
        </picker>
        <view class="search-btn" @click="loadData">查询</view>
      </view>
    </view>

    <!-- 整体转化率 -->
    <view class="conversion-overview">
      <view class="overview-card">
        <view class="overview-value">{{ overallConversion }}%</view>
        <view class="overview-label">整体转化率</view>
        <view class="overview-trend" :class="trend > 0 ? 'up' : 'down'">
          <u-icon :name="trend > 0 ? 'arrow-up' : 'arrow-down'" size="12"></u-icon>
          {{ Math.abs(trend) }}%
        </view>
      </view>
      <view class="overview-info">
        <view class="info-item">
          <text class="info-label">UV → 浏览</text>
          <text class="info-value">{{ conversionData.uvToView }}%</text>
        </view>
        <view class="info-item">
          <text class="info-label">浏览 → 加购</text>
          <text class="info-value">{{ conversionData.viewToCart }}%</text>
        </view>
        <view class="info-item">
          <text class="info-label">加购 → 支付</text>
          <text class="info-value">{{ conversionData.cartToPay }}%</text>
        </view>
      </view>
    </view>

    <!-- 漏斗图 -->
    <view class="funnel-section">
      <view class="section-title">用户转化漏斗</view>
      <view class="funnel-chart">
        <view class="funnel-item" v-for="(step, index) in funnelSteps" :key="step.name">
          <view class="funnel-wrapper">
            <view 
              class="funnel-bar" 
              :style="{ 
                width: step.percent + '%',
                background: getStepColor(index)
              }"
            >
              <text class="funnel-name">{{ step.name }}</text>
              <text class="funnel-value">{{ formatNumber(step.value) }}</text>
            </view>
          </view>
          <view class="funnel-meta">
            <text class="step-rate">{{ step.rate }}%</text>
            <text class="drop-rate" v-if="index > 0">
              流失 {{ step.dropRate }}%
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 详细漏斗数据 -->
    <view class="funnel-detail">
      <view class="detail-header">
        <text>环节</text>
        <text>人数</text>
        <text>转化率</text>
        <text>流失率</text>
      </view>
      <view class="detail-row" v-for="(step, index) in funnelSteps" :key="step.name">
        <view class="step-name">
          <view class="step-dot" :style="{ background: getStepColor(index) }"></view>
          <text>{{ step.name }}</text>
        </view>
        <text class="step-value">{{ formatNumber(step.value) }}</text>
        <text class="step-rate">{{ index === 0 ? '100%' : step.rate + '%' }}</text>
        <text class="drop-rate" :class="{ highlight: step.dropRate > 30 }">
          {{ index === 0 ? '-' : step.dropRate + '%' }}
        </text>
      </view>
    </view>

    <!-- 流失分析 -->
    <view class="drop-analysis">
      <view class="section-title">流失用户分析</view>
      <view class="drop-list">
        <view class="drop-item" v-for="item in dropAnalysis" :key="item.from + '-' + item.to">
          <view class="drop-header">
            <text class="from-step">{{ item.from }}</text>
            <u-icon name="arrow-right" size="14" color="#999"></u-icon>
            <text class="to-step">{{ item.to }}</text>
          </view>
          <view class="drop-progress">
            <view class="progress-bar" :style="{ width: item.lostPercent + '%' }"></view>
          </view>
          <view class="drop-stats">
            <text>流失 {{ item.lostCount }}人 ({{ item.lostPercent }}%)</text>
            <text class="main-reason">主要原因: {{ item.mainReason }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 优化建议 -->
    <view class="suggestions">
      <view class="section-title">优化建议</view>
      <view class="suggestion-list">
        <view class="suggestion-item" v-for="item in suggestions" :key="item.id">
          <view class="suggestion-icon" :class="'priority-' + item.priority">
            <u-icon :name="item.priority === 1 ? 'warning' : 'info'" size="16"></u-icon>
          </view>
          <view class="suggestion-content">
            <text class="suggestion-title">{{ item.title }}</text>
            <text class="suggestion-desc">{{ item.description }}</text>
            <view class="suggestion-impact">
              <text class="impact-label">预估提升</text>
              <text class="impact-value">{{ item.expectedImpact }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      startDate: '',
      endDate: '',
      trend: 5.2,
      overallConversion: 12.8,
      conversionData: {
        uvToView: 68.5,
        viewToCart: 28.3,
        cartToPay: 66.2
      },
      funnelSteps: [
        { name: '访问(UV)', value: 10000, percent: 100, rate: 100, dropRate: 0 },
        { name: '浏览商品', value: 6850, percent: 68.5, rate: 68.5, dropRate: 31.5 },
        { name: '加入购物车', value: 1939, percent: 19.4, rate: 28.3, dropRate: 40.2 },
        { name: '提交订单', value: 1284, percent: 12.8, rate: 66.2, dropRate: 33.8 },
        { name: '完成支付', value: 850, percent: 8.5, rate: 66.2, dropRate: 33.8 }
      ],
      dropAnalysis: [
        {
          from: '访问',
          to: '浏览',
          lostCount: 3150,
          lostPercent: 31.5,
          mainReason: '页面加载慢/内容不吸引'
        },
        {
          from: '浏览',
          to: '加购',
          lostCount: 4911,
          lostPercent: 71.7,
          mainReason: '价格因素/犹豫期长'
        },
        {
          from: '加购',
          to: '支付',
          lostCount: 655,
          lostPercent: 33.8,
          mainReason: '支付流程复杂/放弃购买'
        }
      ],
      suggestions: [
        {
          id: 1,
          priority: 1,
          title: '优化商品详情页加载速度',
          description: '当前页面加载时间超过3秒，建议压缩图片、启用CDN加速，预计可提升访问→浏览转化率10%',
          expectedImpact: '+3.2%'
        },
        {
          id: 2,
          priority: 1,
          title: '增加限时优惠弹窗',
          description: '用户在浏览页面时显示限时折扣信息，促进加购决策',
          expectedImpact: '+5.8%'
        },
        {
          id: 3,
          priority: 2,
          title: '简化支付流程',
          description: '减少支付步骤，支持更多快捷支付方式',
          expectedImpact: '+8.5%'
        },
        {
          id: 4,
          priority: 2,
          title: '购物车召回提醒',
          description: '对加购未支付用户发送短信/推送提醒',
          expectedImpact: '+12.3%'
        }
      ]
    }
  },

  onLoad() {
    this.initDates()
    this.loadData()
  },

  methods: {
    initDates() {
      const now = new Date()
      const threeMonthsAgo = new Date()
      threeMonthsAgo.setMonth(now.getMonth() - 3)
      
      this.endDate = this.formatMonth(now)
      this.startDate = this.formatMonth(threeMonthsAgo)
    },

    formatMonth(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      return `${year}-${month}`
    },

    onStartDateChange(e) {
      this.startDate = e.detail.value
    },

    onEndDateChange(e) {
      this.endDate = e.detail.value
    },

    loadData() {
      // 模拟数据加载
      console.log('加载漏斗数据', this.startDate, this.endDate)
    },

    formatNumber(num) {
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'w'
      }
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'k'
      }
      return num.toString()
    },

    getStepColor(index) {
      const colors = ['#1890ff', '#13c2c2', '#52c41a', '#faad14', '#f5222d']
      return colors[index] || '#1890ff'
    }
  }
}
</script>

<style lang="scss" scoped>
.funnel-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 30rpx;
}

.header {
  background: #fff;
  padding: 30rpx;
  
  .page-title {
    font-size: 32rpx;
    font-weight: 600;
    margin-bottom: 20rpx;
  }
  
  .date-selector {
    display: flex;
    align-items: center;
    gap: 20rpx;
    
    .date-item {
      display: flex;
      align-items: center;
      padding: 12rpx 20rpx;
      background: #f5f5f5;
      border-radius: 8rpx;
      font-size: 26rpx;
    }
    
    .date-separator {
      color: #999;
    }
    
    .search-btn {
      padding: 12rpx 30rpx;
      background: #2979ff;
      color: #fff;
      border-radius: 8rpx;
      font-size: 26rpx;
    }
  }
}

.conversion-overview {
  display: flex;
  margin: 30rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  
  .overview-card {
    width: 200rpx;
    text-align: center;
    padding-right: 30rpx;
    border-right: 1rpx solid #f5f5f5;
    
    .overview-value {
      font-size: 48rpx;
      font-weight: 700;
      color: #1890ff;
    }
    
    .overview-label {
      font-size: 24rpx;
      color: #999;
      margin-top: 8rpx;
    }
    
    .overview-trend {
      display: inline-flex;
      align-items: center;
      gap: 4rpx;
      font-size: 22rpx;
      margin-top: 10rpx;
      padding: 4rpx 12rpx;
      border-radius: 20rpx;
      
      &.up {
        color: #52c41a;
        background: rgba(82, 196, 26, 0.1);
      }
      
      &.down {
        color: #f5222d;
        background: rgba(245, 34, 45, 0.1);
      }
    }
  }
  
  .overview-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding-left: 30rpx;
    
    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8rpx 0;
      
      .info-label {
        font-size: 26rpx;
        color: #666;
      }
      
      .info-value {
        font-size: 28rpx;
        font-weight: 600;
        color: #1890ff;
      }
    }
  }
}

.funnel-section {
  background: #fff;
  margin: 0 30rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
  
  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    margin-bottom: 30rpx;
  }
}

.funnel-chart {
  .funnel-item {
    margin-bottom: 20rpx;
    
    .funnel-wrapper {
      height: 60rpx;
      background: #f5f5f5;
      border-radius: 8rpx;
      overflow: hidden;
      
      .funnel-bar {
        height: 100%;
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 20rpx;
        border-radius: 8rpx;
        transition: width 0.5s ease;
        
        .funnel-name {
          font-size: 26rpx;
          color: #fff;
        }
        
        .funnel-value {
          font-size: 24rpx;
          color: #fff;
          font-weight: 600;
        }
      }
    }
    
    .funnel-meta {
      display: flex;
      justify-content: space-between;
      padding: 8rpx 10rpx;
      
      .step-rate {
        font-size: 22rpx;
        color: #666;
      }
      
      .drop-rate {
        font-size: 22rpx;
        color: #f5222d;
      }
    }
  }
}

.funnel-detail {
  background: #fff;
  margin: 0 30rpx 20rpx;
  border-radius: 16rpx;
  padding: 0 30rpx;
  
  .detail-header {
    display: grid;
    grid-template-columns: 2fr 1fr 1fr 1fr;
    gap: 10rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;
    font-size: 24rpx;
    color: #999;
    text-align: center;
  }
  
  .detail-row {
    display: grid;
    grid-template-columns: 2fr 1fr 1fr 1fr;
    gap: 10rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;
    font-size: 26rpx;
    text-align: center;
    
    &:last-child {
      border-bottom: none;
    }
    
    .step-name {
      display: flex;
      align-items: center;
      justify-content: center;
      
      .step-dot {
        width: 16rpx;
        height: 16rpx;
        border-radius: 50%;
        margin-right: 12rpx;
      }
      
      text {
        color: #333;
      }
    }
    
    .step-value {
      color: #1890ff;
      font-weight: 600;
    }
    
    .step-rate {
      color: #52c41a;
    }
    
    .drop-rate {
      color: #999;
      
      &.highlight {
        color: #f5222d;
        font-weight: 600;
      }
    }
  }
}

.drop-analysis {
  background: #fff;
  margin: 0 30rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
  
  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    margin-bottom: 20rpx;
  }
  
  .drop-list {
    .drop-item {
      padding: 20rpx;
      background: #fafafa;
      border-radius: 12rpx;
      margin-bottom: 15rpx;
      
      .drop-header {
        display: flex;
        align-items: center;
        gap: 15rpx;
        margin-bottom: 12rpx;
        
        .from-step, .to-step {
          font-size: 26rpx;
          color: #333;
        }
      }
      
      .drop-progress {
        height: 8rpx;
        background: #f0f0f0;
        border-radius: 4rpx;
        margin-bottom: 10rpx;
        
        .progress-bar {
          height: 100%;
          background: linear-gradient(90deg, #ff7875, #ff4d4f);
          border-radius: 4rpx;
        }
      }
      
      .drop-stats {
        display: flex;
        justify-content: space-between;
        font-size: 22rpx;
        color: #999;
        
        .main-reason {
          color: #ff4d4f;
        }
      }
    }
  }
}

.suggestions {
  background: #fff;
  margin: 0 30rpx 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
  
  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    margin-bottom: 20rpx;
  }
  
  .suggestion-list {
    .suggestion-item {
      display: flex;
      padding: 20rpx;
      border: 1rpx solid #f5f5f5;
      border-radius: 12rpx;
      margin-bottom: 15rpx;
      
      .suggestion-icon {
        width: 50rpx;
        height: 50rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 15rpx;
        
        &.priority-1 {
          background: rgba(245, 34, 45, 0.1);
          color: #f5222d;
        }
        
        &.priority-2 {
          background: rgba(24, 144, 255, 0.1);
          color: #1890ff;
        }
      }
      
      .suggestion-content {
        flex: 1;
        
        .suggestion-title {
          font-size: 28rpx;
          font-weight: 600;
          color: #333;
          display: block;
        }
        
        .suggestion-desc {
          font-size: 24rpx;
          color: #666;
          margin-top: 8rpx;
          display: block;
        }
        
        .suggestion-impact {
          display: inline-flex;
          align-items: center;
          gap: 10rpx;
          margin-top: 12rpx;
          padding: 6rpx 16rpx;
          background: rgba(82, 196, 26, 0.1);
          border-radius: 20rpx;
          
          .impact-label {
            font-size: 22rpx;
            color: #52c41a;
          }
          
          .impact-value {
            font-size: 24rpx;
            font-weight: 600;
            color: #52c41a;
          }
        }
      }
    }
  }
}
</style>
