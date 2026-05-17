<template>
  <view class="analytics-page">
    <!-- 顶部导航 -->
    <view class="topbar">
      <text class="back" @click="goBack">‹</text>
      <text class="title">数据看板</text>
      <text class="spacer"></text>
    </view>

    <!-- 核心指标卡片 -->
    <view class="section">
      <view class="section-title">核心指标</view>
      <view class="metrics-grid">
        <view class="metric-card" v-for="m in metrics" :key="m.key">
          <text class="metric-label">{{ m.label }}</text>
          <text class="metric-value">{{ m.value }}</text>
          <text class="metric-unit">{{ m.unit }}</text>
        </view>
      </view>
    </view>

    <!-- 趋势图表 = time range selector + CSS bar chart -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">趋势分析</text>
        <view class="range-tabs">
          <text
            v-for="r in ranges"
            :key="r.value"
            class="range-tab"
            :class="{ active: activeRange === r.value }"
            @click="switchRange(r.value)"
          >{{ r.label }}</text>
        </view>
      </view>

      <!-- 趋势切换 Tab（销售 / 粉丝） -->
      <view class="trend-tabs">
        <text
          v-for="t in trendTabs"
          :key="t.value"
          class="trend-tab"
          :class="{ active: activeTrendTab === t.value }"
          @click="activeTrendTab = t.value"
        >{{ t.label }}</text>
      </view>

      <view v-if="trendLoading" class="chart-placeholder">加载趋势中...</view>
      <view v-else-if="trendData.length === 0" class="chart-placeholder">暂无趋势数据</view>
      <view v-else class="chart-wrap">
        <view class="bar-chart">
          <view
            v-for="(item, idx) in trendData"
            :key="idx"
            class="bar-col"
            @click="activeBarIdx = idx"
          >
            <view class="bar-inner" :style="{ height: item.height + '%' }">
              <text v-if="activeBarIdx === idx && item.value > 0" class="bar-tooltip">{{ item.value }}</text>
            </view>
            <text v-if="trendData.length <= 31" class="bar-label">{{ item.label }}</text>
          </view>
        </view>
        <view class="trend-summary">
          <text>总计: <text class="highlight">{{ trendTotal }}</text></text>
          <text>日均: <text class="highlight">{{ trendAvg }}</text></text>
          <text>最高: <text class="highlight">{{ trendMax }}</text></text>
        </view>
      </view>
    </view>

    <!-- 受众画像 -->
    <view class="section">
      <view class="section-title">受众画像</view>

      <view v-if="profileLoading" class="chart-placeholder">加载画像中...</view>
      <template v-else>
        <!-- 地域分布 -->
        <view class="profile-block">
          <text class="profile-head">地域分布</text>
          <view v-if="regionData.length === 0" class="profile-empty">暂无数据</view>
          <view v-else class="horiz-bar-list">
            <view class="horiz-bar-item" v-for="r in regionData" :key="r.name">
              <text class="horiz-bar-label">{{ r.name }}</text>
              <view class="horiz-bar-track">
                <view class="horiz-bar-fill" :style="{ width: r.ratio + '%' }"></view>
              </view>
              <text class="horiz-bar-value">{{ r.ratio }}%</text>
            </view>
          </view>
        </view>

        <!-- 性别分布 -->
        <view class="profile-block">
          <text class="profile-head">性别分布</text>
          <view v-if="genderData.length === 0" class="profile-empty">暂无数据</view>
          <view v-else class="gender-row">
            <view class="gender-item" v-for="g in genderData" :key="g.name">
              <view class="gender-donut" :style="donutStyle(g)">
                <text class="gender-donut-text">{{ g.ratio }}%</text>
              </view>
              <text class="gender-name">{{ g.name }}</text>
              <text class="gender-count">{{ g.count }}人</text>
            </view>
          </view>
        </view>

        <!-- 偏好分析 -->
        <view class="profile-block">
          <text class="profile-head">作品偏好</text>
          <view v-if="prefData.length === 0" class="profile-empty">暂无数据</view>
          <view v-else class="horiz-bar-list">
            <view class="horiz-bar-item" v-for="p in prefData" :key="p.name">
              <text class="horiz-bar-label">{{ p.name }}</text>
              <view class="horiz-bar-track">
                <view class="horiz-bar-fill pref" :style="{ width: p.ratio + '%' }"></view>
              </view>
              <text class="horiz-bar-value">{{ p.count }}</text>
            </view>
          </view>
        </view>
      </template>
    </view>

    <view class="home-indicator"></view>
  </view>
</template>

<script>
import { getAnalyticsOverview, getAnalyticsTrend, getAudienceProfile } from '@/api/user'

export default {
  data() {
    return {
      artistId: null,

      // 核心指标
      metrics: [],

      // 时间范围
      activeRange: 30,
      ranges: [
        { label: '7天', value: 7 },
        { label: '30天', value: 30 },
        { label: '90天', value: 90 }
      ],

      // 趋势
      trendLoading: false,
      activeTrendTab: 'sales',
      trendTabs: [
        { label: '销售', value: 'sales' },
        { label: '粉丝累计', value: 'followers' },
        { label: '收入(元)', value: 'revenue' }
      ],
      trendRaw: null,
      activeBarIdx: -1,

      // 受众画像
      profileLoading: false,
      regionData: [],
      genderData: [],
      prefData: []
    }
  },

  computed: {
    trendData() {
      if (!this.trendRaw) return []
      const raw = this.trendRaw
      switch (this.activeTrendTab) {
        case 'sales':
          return this.buildBars(raw.dates, raw.sales)
        case 'followers':
          return this.buildBars(raw.dates, raw.followers)
        case 'revenue':
          return this.buildBars(raw.dates, raw.revenue)
        default:
          return []
      }
    },
    trendTotal() {
      return this.trendData.reduce((s, i) => s + i.value, 0)
    },
    trendAvg() {
      const len = this.trendData.length
      return len ? Math.round(this.trendTotal / len) : 0
    },
    trendMax() {
      return Math.max(...this.trendData.map(i => i.value), 0)
    }
  },

  onLoad(query) {
    this.artistId = query.id
    this.loadAll()
  },

  methods: {
    async loadAll() {
      await Promise.all([
        this.loadOverview(),
        this.loadTrend(),
        this.loadProfile()
      ])
    },

    async loadOverview() {
      try {
        const data = await getAnalyticsOverview(this.artistId)
        this.metrics = [
          { key: 'works', label: '作品总数', value: data.works ?? 0, unit: '件' },
          { key: 'views', label: '总浏览量', value: this.fmt(data.views ?? 0), unit: '' },
          { key: 'favorites', label: '总收藏', value: this.fmt(data.favorites ?? 0), unit: '' },
          { key: 'followers', label: '粉丝', value: this.fmt(data.followers ?? 0), unit: '' },
          { key: 'engagement', label: '互动率', value: data.engagementRate ?? 0, unit: '%' },
          { key: 'sales', label: '已售', value: this.fmt(data.sales ?? 0), unit: '件' }
        ]
      } catch (e) {
        console.warn('加载概览失败', e)
      }
    },

    async loadTrend() {
      this.trendLoading = true
      try {
        this.trendRaw = await getAnalyticsTrend(this.artistId, this.activeRange)
      } catch (e) {
        console.warn('加载趋势失败', e)
      } finally {
        this.trendLoading = false
      }
    },

    async loadProfile() {
      this.profileLoading = true
      try {
        const data = await getAudienceProfile(this.artistId)
        this.regionData = data.regionDistribution || []
        this.genderData = data.genderDistribution || []
        this.prefData = data.preferenceDistribution || []
      } catch (e) {
        console.warn('加载画像失败', e)
      } finally {
        this.profileLoading = false
      }
    },

    switchRange(days) {
      this.activeRange = days
      this.activeBarIdx = -1
      this.loadTrend()
    },

    buildBars(dates, values) {
      if (!dates || !values) return []
      const max = Math.max(...values, 1)
      return dates.map((d, i) => ({
        label: this.shortenDate(d),
        value: values[i] || 0,
        height: max > 0 ? (values[i] / max) * 100 : 0
      }))
    },

    shortenDate(dateStr) {
      if (!dateStr) return ''
      const parts = dateStr.split('-')
      return parts.length === 3 ? (parts[1] + '/' + parts[2]) : dateStr.slice(5)
    },

    donutStyle(item) {
      const r = item.ratio || 0
      // conic-gradient 模拟饼图
      return { background: `conic-gradient(var(--gold) 0% ${r}%, rgba(255,255,255,0.08) ${r}% 100%)` }
    },

    fmt(v) {
      if (v >= 10000) return (v / 10000).toFixed(1) + 'w'
      if (v >= 1000) return (v / 1000).toFixed(1) + 'k'
      return String(v)
    },

    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
$bg: #050505;
$card: #171717;
$gold: #d6a827;
$gold-light: #f1c84b;
$text: #fff;
$text2: #b7b7b7;
$text3: #7d7d7d;
$border: rgba(214, 168, 39, 0.35);
$line: rgba(255, 255, 255, 0.07);
$bar: #d6a827;

.analytics-page {
  min-height: 100vh;
  padding: 0 28rpx 50rpx;
  background: $bg;
  color: $text;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 98rpx;
}
.back, .spacer { width: 72rpx; }
.back { font-size: 44rpx; }
.title { font-size: 32rpx; font-weight: 700; }

/* ===== 分区 ===== */
.section {
  margin-bottom: 32rpx;
}
.section-title {
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
  color: $gold-light;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  .section-title { margin-bottom: 0; }
}

/* ===== 核心指标卡片 ===== */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}
.metric-card {
  padding: 24rpx 16rpx;
  border: 1rpx solid $line;
  border-radius: 20rpx;
  background: $card;
  text-align: center;
}
.metric-label {
  display: block;
  font-size: 22rpx;
  color: $text3;
  margin-bottom: 10rpx;
}
.metric-value {
  display: inline;
  font-size: 38rpx;
  font-weight: 800;
  color: $gold-light;
}
.metric-unit {
  display: inline;
  font-size: 22rpx;
  color: $text3;
  margin-left: 4rpx;
}

/* ===== 时间范围切换 ===== */
.range-tabs {
  display: flex;
  gap: 10rpx;
}
.range-tab {
  flex-shrink: 0;
  white-space: nowrap;
  padding: 8rpx 20rpx;
  border: 1rpx solid rgba(255,255,255,0.09);
  border-radius: 999rpx;
  font-size: 22rpx;
  color: $text3;
}
.range-tab.active {
  border-color: $border;
  color: $gold-light;
  background: rgba(214,168,39,0.12);
}

/* ===== 趋势Tab ===== */
.trend-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.trend-tab {
  flex-shrink: 0;
  white-space: nowrap;
  padding: 10rpx 0;
  font-size: 24rpx;
  color: $text3;
  border-bottom: 2rpx solid transparent;
}
.trend-tab.active {
  color: $gold-light;
  border-bottom-color: $gold;
}

/* ===== CSS柱状图 ===== */
.chart-placeholder {
  padding: 60rpx 0;
  text-align: center;
  color: $text3;
  font-size: 26rpx;
}
.chart-wrap {
  padding: 24rpx 10rpx;
  border: 1rpx solid $line;
  border-radius: 20rpx;
  background: $card;
}
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  height: 280rpx;
  padding: 0 4rpx;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
  position: relative;
}
.bar-inner {
  width: 100%;
  max-width: 32rpx;
  min-height: 2rpx;
  background: linear-gradient(180deg, $gold-light, $gold);
  border-radius: 4rpx 4rpx 0 0;
  position: relative;
  transition: height 0.3s ease;
}
.bar-tooltip {
  position: absolute;
  top: -36rpx;
  left: 50%;
  transform: translateX(-50%);
  background: $gold;
  color: #000;
  font-size: 18rpx;
  font-weight: 700;
  padding: 4rpx 10rpx;
  border-radius: 8rpx;
  white-space: nowrap;
}
.bar-label {
  font-size: 16rpx;
  color: $text3;
  margin-top: 6rpx;
  transform: rotate(-30deg);
  transform-origin: left center;
}
.trend-summary {
  display: flex;
  justify-content: space-around;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $line;
  font-size: 22rpx;
  color: $text2;
}
.highlight {
  color: $gold-light;
  font-weight: 700;
}

/* ===== 受众画像 ===== */
.profile-block {
  margin-bottom: 28rpx;
}
.profile-head {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: $text;
  margin-bottom: 14rpx;
}
.profile-empty {
  padding: 30rpx 0;
  text-align: center;
  color: $text3;
  font-size: 24rpx;
}

/* 横向条形图 */
.horiz-bar-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}
.horiz-bar-item {
  display: flex;
  align-items: center;
  gap: 14rpx;
}
.horiz-bar-label {
  width: 130rpx;
  font-size: 22rpx;
  color: $text2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.horiz-bar-track {
  flex: 1;
  height: 16rpx;
  background: rgba(255,255,255,0.06);
  border-radius: 8rpx;
  overflow: hidden;
}
.horiz-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, $gold, $gold-light);
  border-radius: 8rpx;
  transition: width 0.4s ease;
}
.horiz-bar-fill.pref {
  background: linear-gradient(90deg, #c0843c, $gold);
}
.horiz-bar-value {
  width: 80rpx;
  text-align: right;
  font-size: 22rpx;
  color: $gold-light;
  font-weight: 600;
}

/* 性别分布（圆环） */
.gender-row {
  display: flex;
  justify-content: space-around;
  gap: 24rpx;
}
.gender-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.gender-donut {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid rgba(255,255,255,0.06);
}
.gender-donut-text {
  font-size: 26rpx;
  font-weight: 800;
  color: $gold-light;
}
.gender-name {
  font-size: 24rpx;
  color: $text2;
}
.gender-count {
  font-size: 20rpx;
  color: $text3;
}

.home-indicator {
  height: 60rpx;
}
</style>
