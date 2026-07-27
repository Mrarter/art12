<template>
  <div class="page">
    <div class="page-header">
      <div><h1>转售记录管理</h1><p>管理艺术品转售记录，审核转售交易，监控二级流通市场。</p></div>
      <div class="actions">
        <button class="ghost" @click="resetFilters">重置筛选</button>
        <button class="primary" @click="fetchList">刷新</button>
      </div>
    </div>
    <div class="stats">
      <div class="stat"><label>全部转售</label><b>{{ stats.total }}</b></div>
      <div class="stat"><label>待售中</label><b>{{ stats.pending }}</b></div>
      <div class="stat"><label>已完成</label><b>{{ stats.completed }}</b></div>
      <div class="stat"><label>已取消</label><b>{{ stats.canceled }}</b></div>
    </div>
    <div class="card">
      <div class="filters">
        <div class="field"><label>状态</label>
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="pending">待售</option>
            <option value="paid">已支付</option>
            <option value="completed">已完成</option>
            <option value="cancel">已取消</option>
          </select>
        </div>
        <div class="field"><label>作品ID</label><input v-model="filters.artworkId" placeholder="作品ID" /></div>
      </div>
      <div class="filter-actions"><button class="primary" @click="fetchList">查询</button></div>
      <el-table :data="list" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="artworkId" label="作品ID" width="80" />
        <el-table-column prop="sellerUserId" label="卖家ID" width="80" />
        <el-table-column prop="buyerUserId" label="买家ID" width="80" />
        <el-table-column prop="resalePrice" label="转售价" width="120">
          <template #default="{ row }">¥{{ row.resalePrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="artistIncome" label="艺术家收益" width="120">
          <template #default="{ row }">¥{{ row.artistIncome?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="platformFee" label="平台服务费" width="120">
          <template #default="{ row }">¥{{ row.platformFee?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="sellerIncome" label="卖家收入" width="120">
          <template #default="{ row }">¥{{ row.sellerIncome?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" type="success" :disabled="row.status !== 'paid'" @click="handleComplete(row)">完成</el-button>
            <el-button size="small" type="danger" :disabled="row.status !== 'pending'" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination" v-if="total > 0">
        <el-pagination background layout="prev,pager,next" :total="total" :page-size="pageSize" v-model:current-page="page" @current-change="fetchList" />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="转售详情" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="作品ID">{{ detail.artworkId }}</el-descriptions-item>
        <el-descriptions-item label="卖家ID">{{ detail.sellerUserId }}</el-descriptions-item>
        <el-descriptions-item label="买家ID">{{ detail.buyerUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="转售价格">¥{{ detail.resalePrice?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="艺术家收益">¥{{ detail.artistIncome?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="平台服务费">¥{{ detail.platformFee?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="卖家收入">¥{{ detail.sellerIncome?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { getResaleList, getResaleDetail, completeResale, cancelResale } from '@/api/resale'

export default {
  name: 'ResaleList',
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      pageSize: 20,
      loading: false,
      filters: { status: '', artworkId: '' },
      stats: { total: 0, pending: 0, completed: 0, canceled: 0 },
      detailVisible: false,
      detail: null
    }
  },
  mounted() { this.fetchList() },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const params = { page: this.page, pageSize: this.pageSize }
        if (this.filters.status) params.status = this.filters.status
        if (this.filters.artworkId) params.artworkId = parseInt(this.filters.artworkId)
        const data = await getResaleList(params)
        this.list = data.records || []
        this.total = data.total || 0
        this.computeStats()
      } catch (e) { /* handled by interceptor */ }
      finally { this.loading = false }
    },
    computeStats() {
      const all = this.list
      this.stats.total = all.length
      this.stats.pending = all.filter(i => i.status === 'pending').length
      this.stats.completed = all.filter(i => i.status === 'completed').length
      this.stats.canceled = all.filter(i => i.status === 'cancel').length
    },
    statusType(s) {
      const map = { pending: 'warning', paid: 'primary', completed: 'success', cancel: 'info' }
      return map[s] || 'info'
    },
    statusLabel(s) {
      const map = { pending: '待售', paid: '已支付', completed: '已完成', cancel: '已取消' }
      return map[s] || s
    },
    resetFilters() { this.filters = { status: '', artworkId: '' }; this.page = 1; this.fetchList() },
    async viewDetail(row) {
      this.detail = row
      try {
        const d = await getResaleDetail(row.id)
        if (d) this.detail = d
      } catch (e) { /* */ }
      this.detailVisible = true
    },
    async handleComplete(row) {
      await this.$confirm('确认完成该转售？资金将分配给艺术家、平台和卖家', '提示', { type: 'warning' })
      await completeResale(row.id)
      this.$message.success('转售已完成')
      this.fetchList()
    },
    async handleCancel(row) {
      await this.$confirm('确认取消该转售？', '提示', { type: 'warning' })
      await cancelResale(row.id)
      this.$message.success('转售已取消')
      this.fetchList()
    }
  }
}
</script>
