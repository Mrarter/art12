<template>
  <div class="page">
    <div class="page-header">
      <div><h1>流通数据统计</h1><p>艺术品二级流通市场整体数据概览，包含转售统计、平台收益和艺术家收益。</p></div>
      <div class="actions"><button class="primary" @click="fetchData">刷新数据</button></div>
    </div>

    <div class="card" v-loading="loading">
      <h2>流通概览</h2>
      <div class="stats" v-if="circulation">
        <div class="stat"><label>转售总数</label><b>{{ circulation.totalResales }}</b></div>
        <div class="stat"><label>已完成转售</label><b>{{ circulation.completedResales }}</b></div>
        <div class="stat"><label>待售转售</label><b>{{ circulation.pendingResales }}</b></div>
        <div class="stat"><label>交易链路记录</label><b>{{ circulation.totalTradeRecords }}</b></div>
        <div class="stat"><label>首次出售</label><b>{{ circulation.firstSales }}</b></div>
        <div class="stat"><label>转售次数</label><b>{{ circulation.resaleTrades }}</b></div>
        <div class="stat"><label>流通作品数</label><b>{{ circulation.uniqueArtworks }}</b></div>
      </div>
    </div>

    <div class="card" v-loading="loading">
      <h2>平台收益统计</h2>
      <div class="stats" v-if="feeStats">
        <div class="stat"><label>完成转售数</label><b>{{ feeStats.totalResaleCount }}</b></div>
        <div class="stat"><label>总成交金额</label><b>¥{{ (feeStats.totalTradeAmount || 0).toFixed(2) }}</b></div>
        <div class="stat"><label>累计平台服务费</label><b style="color:#D4AF37;">¥{{ (feeStats.totalPlatformFee || 0).toFixed(2) }}</b></div>
        <div class="stat"><label>累计艺术家收益</label><b style="color:#67C23A;">¥{{ (feeStats.totalArtistIncome || 0).toFixed(2) }}</b></div>
        <div class="stat"><label>累计卖家收入</label><b style="color:#409EFF;">¥{{ (feeStats.totalSellerIncome || 0).toFixed(2) }}</b></div>
      </div>
    </div>

    <div class="card">
      <h2>作品流通查询</h2>
      <div class="filters">
        <div class="field"><label>作品ID</label><input v-model="queryArtworkId" placeholder="输入作品ID" /></div>
      </div>
      <div class="filter-actions"><button class="primary" @click="queryArtwork">查询</button></div>
      <div v-if="artworkData">
        <el-divider />
        <h3>作品 #{{ queryArtworkId }} 流通数据</h3>
        <div class="stats" v-if="artworkStats">
          <div class="stat"><label>转售次数</label><b>{{ artworkStats.resaleCount }}</b></div>
          <div class="stat"><label>流通次数</label><b>{{ artworkStats.totalTrades }}</b></div>
          <div class="stat"><label>首次成交价</label><b>¥{{ (artworkStats.firstPrice || 0).toFixed(2) }}</b></div>
          <div class="stat"><label>最新成交价</label><b>¥{{ (artworkStats.lastPrice || 0).toFixed(2) }}</b></div>
          <div class="stat"><label>最高成交价</label><b>¥{{ (artworkStats.highestPrice || 0).toFixed(2) }}</b></div>
          <div class="stat"><label>总涨幅</label><b :style="{color: growthColor(artworkStats.totalGrowthRate)}">{{ (artworkStats.totalGrowthRate || 0).toFixed(2) }}%</b></div>
        </div>

        <el-divider />
        <h4>交易链路</h4>
        <el-table :data="trades" border size="small" style="width:100%">
          <el-table-column prop="tradeRound" label="轮次" width="60" />
          <el-table-column prop="tradeNo" label="交易编号" width="180" />
          <el-table-column prop="tradeType" label="类型" width="100">
            <template #default="{ row }">{{ row.tradeType === 'first_sale' ? '首次出售' : '转售' }}</template>
          </el-table-column>
          <el-table-column prop="buyerUserId" label="买家ID" width="80" />
          <el-table-column prop="sellerUserId" label="卖家ID" width="80" />
          <el-table-column prop="tradePrice" label="成交价" width="100">
            <template #default="{ row }">¥{{ row.tradePrice?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="createdTime" label="时间" width="160" />
        </el-table>

        <el-divider />
        <h4>价格历史</h4>
        <el-table :data="priceHistory" border size="small" style="width:100%">
          <el-table-column prop="createdTime" label="时间" width="160" />
          <el-table-column prop="beforePrice" label="变动前" width="100">
            <template #default="{ row }">¥{{ (row.beforePrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="afterPrice" label="变动后" width="100">
            <template #default="{ row }">¥{{ row.afterPrice?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="growthRate" label="涨幅" width="80">
            <template #default="{ row }">
              <span :style="{color: growthColor(row.growthRate)}">{{ row.growthRate?.toFixed(2) }}%</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" width="100">
            <template #default="{ row }">{{ reasonLabel(row.reason) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { getPlatformFeeStats, getCirculationStats, getArtworkTrades, getArtworkPriceHistory, getArtworkResaleStats } from '@/api/resale'

export default {
  name: 'ResaleStats',
  data() {
    return {
      loading: false,
      circulation: null,
      feeStats: null,
      queryArtworkId: '',
      artworkData: false,
      artworkStats: null,
      trades: [],
      priceHistory: []
    }
  },
  mounted() { this.fetchData() },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const [fee, circ] = await Promise.all([
          getPlatformFeeStats(),
          getCirculationStats()
        ])
        this.feeStats = fee
        this.circulation = circ
      } catch (e) { /* */ }
      finally { this.loading = false }
    },
    async queryArtwork() {
      if (!this.queryArtworkId) return
      const id = parseInt(this.queryArtworkId)
      if (!id) { this.$message.warning('请输入有效的作品ID'); return }
      try {
        const [tradesData, priceData, statsData] = await Promise.all([
          getArtworkTrades(id),
          getArtworkPriceHistory(id),
          getArtworkResaleStats(id)
        ])
        this.trades = tradesData || []
        this.priceHistory = priceData || []
        this.artworkStats = statsData
        this.artworkData = true
      } catch (e) { /* */ }
    },
    growthColor(rate) {
      if (!rate || rate >= 0) return '#67C23A'
      return '#F56C6C'
    },
    reasonLabel(r) {
      const map = { first_sale: '首次出售', resale: '转售', admin_adjust: '后台调整' }
      return map[r] || r
    }
  }
}
</script>
