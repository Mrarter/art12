<template>
  <view class="user-profile-page">
    <view class="header">
      <view class="page-title">用户画像分析</view>
      <view class="date-selector">
        <picker mode="date" :value="startDate" @change="onStartDateChange">
          <view class="date-item">
            <text>{{ startDate }}</text>
            <u-icon name="arrow-down" size="12"></u-icon>
          </view>
        </picker>
        <text class="date-separator">至</text>
        <picker mode="date" :value="endDate" @change="onEndDateChange">
          <view class="date-item">
            <text>{{ endDate }}</text>
            <u-icon name="arrow-down" size="12"></u-icon>
          </view>
        </picker>
        <view class="search-btn" @click="loadData">查询</view>
      </view>
    </view>

    <!-- 核心指标卡片 -->
    <view class="stats-cards">
      <view class="stats-card">
        <view class="card-value">{{ stats.totalUsers }}</view>
        <view class="card-label">总用户数</view>
        <view class="card-trend" :class="stats.userTrend > 0 ? 'up' : 'down'">
          <u-icon :name="stats.userTrend > 0 ? 'arrow-up' : 'arrow-down'" size="12"></u-icon>
          {{ Math.abs(stats.userTrend) }}%
        </view>
      </view>
      <view class="stats-card">
        <view class="card-value">{{ stats.activeUsers }}</view>
        <view class="card-label">活跃用户</view>
        <view class="card-trend" :class="stats.activeTrend > 0 ? 'up' : 'down'">
          <u-icon :name="stats.activeTrend > 0 ? 'arrow-up' : 'arrow-down'" size="12"></u-icon>
          {{ Math.abs(stats.activeTrend) }}%
        </view>
      </view>
      <view class="stats-card">
        <view class="card-value">{{ stats.newUsers }}</view>
        <view class="card-label">新增用户</view>
        <view class="card-trend" :class="stats.newTrend > 0 ? 'up' : 'down'">
          <u-icon :name="stats.newTrend > 0 ? 'arrow-up' : 'arrow-down'" size="12"></u-icon>
          {{ Math.abs(stats.newTrend) }}%
        </view>
      </view>
    </view>

    <!-- 年龄分布 -->
    <view class="chart-section">
      <view class="section-title">年龄分布</view>
      <view class="chart-container">
        <view class="bar-chart">
          <view class="bar-item" v-for="item in ageDistribution" :key="item.range">
            <view class="bar-wrapper">
              <view class="bar" :style="{ height: item.percent + '%' }"></view>
            </view>
            <view class="bar-label">{{ item.range }}</view>
            <view class="bar-value">{{ item.count }}</view>
          </view>
        </view>
      </view>
    </view>

    <!-- 性别分布 -->
    <view class="chart-section">
      <view class="section-title">性别分布</view>
      <view class="gender-distribution">
        <view class="gender-item">
          <view class="gender-circle male">
            <text>{{ genderDistribution.male }}%</text>
          </view>
          <text class="gender-label">男性</text>
          <text class="gender-count">{{ genderDistribution.maleCount }}人</text>
        </view>
        <view class="gender-item">
          <view class="gender-circle female">
            <text>{{ genderDistribution.female }}%</text>
          </view>
          <text class="gender-label">女性</text>
          <text class="gender-count">{{ genderDistribution.femaleCount }}人</text>
        </view>
        <view class="gender-item">
          <view class="gender-circle unknown">
            <text>{{ genderDistribution.unknown }}%</text>
          </view>
          <text class="gender-label">未知</text>
          <text class="gender-count">{{ genderDistribution.unknownCount }}人</text>
        </view>
      </view>
    </view>

    <!-- 地域分布 -->
    <view class="chart-section">
      <view class="section-title">地域分布 TOP10</view>
      <view class="region-list">
        <view class="region-item" v-for="(item, index) in regionDistribution" :key="item.region">
          <view class="region-rank" :class="{ top3: index < 3 }">{{ index + 1 }}</view>
          <view class="region-name">{{ item.region }}</view>
          <view class="region-bar-wrapper">
            <view class="region-bar" :style="{ width: item.percent + '%' }"></view>
          </view>
          <view class="region-value">{{ item.count }}人</view>
        </view>
      </view>
    </view>

    <!-- 行为分析 -->
    <view class="chart-section">
      <view class="section-title">用户行为分析</view>
      <view class="behavior-grid">
        <view class="behavior-item">
          <view class="behavior-icon">
            <u-icon name="eye" size="24" color="#2979ff"></u-icon>
          </view>
          <view class="behavior-value">{{ behaviorStats.avgBrowseTime }}</view>
          <view class="behavior-label">平均浏览时长</view>
        </view>
        <view class="behavior-item">
          <view class="behavior-icon">
            <u-icon name="shopping-cart" size="24" color="#ff9900"></u-icon>
          </view>
          <view class="behavior-value">{{ behaviorStats.cartRate }}%</view>
          <view class="behavior-label">加购率</view>
        </view>
        <view class="behavior-item">
          <view class="behavior-icon">
            <u-icon name="rmb" size="24" color="#19be6b"></u-icon>
          </view>
          <view class="behavior-value">{{ behaviorStats.payRate }}%</view>
          <view class="behavior-label">支付转化率</view>
        </view>
        <view class="behavior-item">
          <view class="behavior-icon">
            <u-icon name="star" size="24" color="#fa3534"></u-icon>
          </view>
          <view class="behavior-value">{{ behaviorStats.avgOrderAmount }}</view>
          <view class="behavior-label">平均客单价</view>
        </view>
        <view class="behavior-item">
          <view class="behavior-icon">
            <u-icon name="heart" size="24" color="#e65a6b"></u-icon>
          </view>
          <view class="behavior-value">{{ behaviorStats.collectRate }}%</view>
          <view class="behavior-label">收藏率</view>
        </view>
        <view class="behavior-item">
          <view class="behavior-icon">
            <u-icon name="share" size="24" color="#9c27b0"></u-icon>
          </view>
          <view class="behavior-value">{{ behaviorStats.shareRate }}%</view>
          <view class="behavior-label">分享率</view>
        </view>
      </view>
    </view>

    <!-- 用户等级分布 -->
    <view class="chart-section">
      <view class="section-title">用户等级分布</view>
      <view class="level-list">
        <view class="level-item" v-for="item in levelDistribution" :key="item.level">
          <view class="level-info">
            <view class="level-badge" :class="'level-' + item.level">{{ item.levelName }}</view>
            <text class="level-desc">{{ item.desc }}</text>
          </view>
          <view class="level-stats">
            <text class="level-count">{{ item.count }}人</text>
            <text class="level-percent">{{ item.percent }}%</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 流失分析 -->
    <view class="chart-section">
      <view class="section-title">用户留存分析</view>
      <view class="retention-chart">
        <view class="retention-header">
          <text class="header-label"></text>
          <text class="header-label">次日留存</text>
          <text class="header-label">7日留存</text>
          <text class="header-label">30日留存</text>
        </view>
        <view class="retention-row" v-for="item in retentionData" :key="item.cohort">
          <text class="cohort-label">{{ item.cohort }}</text>
          <view class="retention-cell">
            <view class="retention-bar" :style="{ width: item.day1 + '%', background: getRetentionColor(item.day1) }"></view>
            <text>{{ item.day1 }}%</text>
          </view>
          <view class="retention-cell">
            <view class="retention-bar" :style="{ width: item.day7 + '%', background: getRetentionColor(item.day7) }"></view>
            <text>{{ item.day7 }}%</text>
          </view>
          <view class="retention-cell">
            <view class="retention-bar" :style="{ width: item.day30 + '%', background: getRetentionColor(item.day30) }"></view>
            <text>{{ item.day30 }}%</text>
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
      stats: {
        totalUsers: 0,
        userTrend: 0,
        activeUsers: 0,
        activeTrend: 0,
        newUsers: 0,
        newTrend: 0
      },
      ageDistribution: [],
      genderDistribution: {
        male: 0,
        female: 0,
        unknown: 0,
        maleCount: 0,
        femaleCount: 0,
        unknownCount: 0
      },
      regionDistribution: [],
      behaviorStats: {
        avgBrowseTime: '0分钟',
        cartRate: 0,
        payRate: 0,
        avgOrderAmount: '¥0',
        collectRate: 0,
        shareRate: 0
      },
      levelDistribution: [],
      retentionData: []
    }
  },

  onLoad() {
    this.initDates()
    this.loadData()
  },

  methods: {
    initDates() {
      const today = new Date()
      const lastMonth = new Date()
      lastMonth.setMonth(lastMonth.getMonth() - 1)
      
      this.endDate = this.formatDate(today)
      this.startDate = this.formatDate(lastMonth)
    },

    formatDate(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },

    onStartDateChange(e) {
      this.startDate = e.detail.value
    },

    onEndDateChange(e) {
      this.endDate = e.detail.value
    },

    loadData() {
      this.loadStats()
      this.loadAgeDistribution()
      this.loadGenderDistribution()
      this.loadRegionDistribution()
      this.loadBehaviorStats()
      this.loadLevelDistribution()
      this.loadRetentionData()
    },

    loadStats() {
      // 模拟数据
      this.stats = {
        totalUsers: 15678,
        userTrend: 12.5,
        activeUsers: 8923,
        activeTrend: 8.3,
        newUsers: 1256,
        newTrend: 15.2
      }
    },

    loadAgeDistribution() {
      this.ageDistribution = [
        { range: '18-24', count: 3256, percent: 21 },
        { range: '25-34', count: 5823, percent: 37 },
        { range: '35-44', count: 4132, percent: 26 },
        { range: '45-54', count: 1567, percent: 10 },
        { range: '55+', count: 900, percent: 6 }
      ]
    },

    loadGenderDistribution() {
      this.genderDistribution = {
        male: 45,
        female: 48,
        unknown: 7,
        maleCount: 7055,
        femaleCount: 7525,
        unknownCount: 1098
      }
    },

    loadRegionDistribution() {
      this.regionDistribution = [
        { region: '北京', count: 2134, percent: 85 },
        { region: '上海', count: 1892, percent: 75 },
        { region: '广东', count: 1654, percent: 66 },
        { region: '浙江', count: 1234, percent: 49 },
        { region: '江苏', count: 1123, percent: 45 },
        { region: '四川', count: 987, percent: 39 },
        { region: '湖北', count: 876, percent: 35 },
        { region: '山东', count: 765, percent: 30 },
        { region: '河南', count: 654, percent: 26 },
        { region: '福建', count: 543, percent: 22 }
      ]
    },

    loadBehaviorStats() {
      this.behaviorStats = {
        avgBrowseTime: '8.5分钟',
        cartRate: 23.5,
        payRate: 12.8,
        avgOrderAmount: '¥2,356',
        collectRate: 35.2,
        shareRate: 8.6
      }
    },

    loadLevelDistribution() {
      this.levelDistribution = [
        { level: 1, levelName: '新用户', desc: '注册30天内', count: 4523, percent: 28.8 },
        { level: 2, levelName: '活跃用户', desc: '30天内有交易', count: 6234, percent: 39.8 },
        { level: 3, levelName: '资深用户', desc: '累计交易3次以上', count: 3123, percent: 19.9 },
        { level: 4, levelName: 'VIP用户', desc: '累计交易10次以上', count: 1234, percent: 7.9 },
        { level: 5, levelName: '超级VIP', desc: '累计交易50次以上', count: 564, percent: 3.6 }
      ]
    },

    loadRetentionData() {
      this.retentionData = [
        { cohort: '2026-03', day1: 68, day7: 42, day30: 25 },
        { cohort: '2026-02', day1: 65, day7: 38, day30: 22 },
        { cohort: '2026-01', day1: 62, day7: 35, day30: 18 },
        { cohort: '2025-12', day1: 58, day7: 32, day30: 15 },
        { cohort: '2025-11', day1: 55, day7: 28, day30: 12 }
      ]
    },

    getRetentionColor(value) {
      if (value >= 60) return '#19be6b'
      if (value >= 40) return '#ff9900'
      return '#ed4014'
    }
  }
}
</script>

<style lang="scss" scoped>
.user-profile-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 20rpx;
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

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  padding: 30rpx;
  
  .stats-card {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx 20rpx;
    text-align: center;
    
    .card-value {
      font-size: 40rpx;
      font-weight: 600;
      color: #333;
    }
    
    .card-label {
      font-size: 24rpx;
      color: #999;
      margin-top: 10rpx;
    }
    
    .card-trend {
      display: inline-flex;
      align-items: center;
      gap: 4rpx;
      font-size: 22rpx;
      margin-top: 10rpx;
      padding: 4rpx 12rpx;
      border-radius: 20rpx;
      
      &.up {
        color: #19be6b;
        background: rgba(25, 190, 107, 0.1);
      }
      
      &.down {
        color: #ed4014;
        background: rgba(237, 64, 20, 0.1);
      }
    }
  }
}

.chart-section {
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

.bar-chart {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 200rpx;
  
  .bar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .bar-wrapper {
      width: 50rpx;
      height: 150rpx;
      background: #f5f5f5;
      border-radius: 8rpx;
      display: flex;
      align-items: flex-end;
      
      .bar {
        width: 100%;
        background: linear-gradient(180deg, #2979ff, #69b1ff);
        border-radius: 8rpx;
        transition: height 0.3s;
      }
    }
    
    .bar-label {
      font-size: 22rpx;
      color: #666;
      margin-top: 10rpx;
    }
    
    .bar-value {
      font-size: 20rpx;
      color: #999;
    }
  }
}

.gender-distribution {
  display: flex;
  justify-content: space-around;
  
  .gender-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .gender-circle {
      width: 140rpx;
      height: 140rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32rpx;
      font-weight: 600;
      
      &.male {
        background: rgba(41, 121, 255, 0.1);
        color: #2979ff;
      }
      
      &.female {
        background: rgba(230, 90, 107, 0.1);
        color: #e65a6b;
      }
      
      &.unknown {
        background: #f5f5f5;
        color: #999;
      }
    }
    
    .gender-label {
      font-size: 26rpx;
      color: #333;
      margin-top: 15rpx;
    }
    
    .gender-count {
      font-size: 22rpx;
      color: #999;
      margin-top: 5rpx;
    }
  }
}

.region-list {
  .region-item {
    display: flex;
    align-items: center;
    padding: 15rpx 0;
    
    .region-rank {
      width: 40rpx;
      height: 40rpx;
      background: #f5f5f5;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22rpx;
      color: #666;
      margin-right: 20rpx;
      
      &.top3 {
        background: #2979ff;
        color: #fff;
      }
    }
    
    .region-name {
      width: 100rpx;
      font-size: 26rpx;
      color: #333;
    }
    
    .region-bar-wrapper {
      flex: 1;
      height: 20rpx;
      background: #f5f5f5;
      border-radius: 10rpx;
      margin: 0 20rpx;
      overflow: hidden;
      
      .region-bar {
        height: 100%;
        background: linear-gradient(90deg, #69b1ff, #2979ff);
        border-radius: 10rpx;
      }
    }
    
    .region-value {
      font-size: 24rpx;
      color: #999;
      width: 100rpx;
      text-align: right;
    }
  }
}

.behavior-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  
  .behavior-item {
    background: #f8f8f8;
    border-radius: 12rpx;
    padding: 25rpx 15rpx;
    text-align: center;
    
    .behavior-icon {
      margin-bottom: 10rpx;
    }
    
    .behavior-value {
      font-size: 28rpx;
      font-weight: 600;
      color: #333;
    }
    
    .behavior-label {
      font-size: 22rpx;
      color: #999;
      margin-top: 5rpx;
    }
  }
}

.level-list {
  .level-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;
    
    &:last-child {
      border-bottom: none;
    }
    
    .level-info {
      display: flex;
      align-items: center;
      
      .level-badge {
        padding: 6rpx 16rpx;
        border-radius: 20rpx;
        font-size: 22rpx;
        margin-right: 15rpx;
        
        &.level-1 { background: #e8e8e8; color: #666; }
        &.level-2 { background: #d4e9ff; color: #2979ff; }
        &.level-3 { background: #ffe0b2; color: #ff9800; }
        &.level-4 { background: #f8bbd9; color: #e91e63; }
        &.level-5 { background: linear-gradient(135deg, #ffd700, #ff6b35); color: #fff; }
      }
      
      .level-desc {
        font-size: 24rpx;
        color: #999;
      }
    }
    
    .level-stats {
      text-align: right;
      
      .level-count {
        font-size: 26rpx;
        color: #333;
        display: block;
      }
      
      .level-percent {
        font-size: 22rpx;
        color: #999;
      }
    }
  }
}

.retention-chart {
  .retention-header {
    display: grid;
    grid-template-columns: 120rpx repeat(3, 1fr);
    gap: 10rpx;
    padding-bottom: 15rpx;
    border-bottom: 1rpx solid #f5f5f5;
    margin-bottom: 15rpx;
    
    .header-label {
      font-size: 24rpx;
      color: #999;
      text-align: center;
    }
  }
  
  .retention-row {
    display: grid;
    grid-template-columns: 120rpx repeat(3, 1fr);
    gap: 10rpx;
    padding: 12rpx 0;
    
    .cohort-label {
      font-size: 24rpx;
      color: #333;
    }
    
    .retention-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .retention-bar {
        height: 16rpx;
        border-radius: 8rpx;
        margin-bottom: 5rpx;
        max-width: 100%;
      }
      
      text {
        font-size: 22rpx;
        color: #666;
      }
    }
  }
}
</style>
