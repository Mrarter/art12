<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">订单列表</span>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="买家">
          <el-input v-model="searchForm.buyerName" placeholder="请输入买家" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待付款" value="pending" />
            <el-option label="已付款" value="paid" />
            <el-option label="已发货" value="shipped" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="退款中" value="refunding" />
            <el-option label="已退款" value="refunded" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column label="买家信息" min-width="150">
        <template #default="{ row }">
          <p>{{ row.buyerName }}</p>
          <p class="phone">{{ row.buyerUid || row.buyerPhone || '-' }}</p>
        </template>
      </el-table-column>
      <el-table-column label="作品信息" min-width="260">
        <template #default="{ row }">
          <div class="artwork-info">
            <el-image v-if="row.cover" :src="getFullImageUrl(row.cover)" class="artwork-thumb" fit="cover" />
            <div v-else class="no-cover"><el-icon><Picture /></el-icon></div>
            <span class="artwork-title" :title="row.artworkTitle">{{ row.artworkTitle || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="订单金额" width="120">
        <template #default="{ row }">
          <p class="price">{{ formatAmount(resolveDisplayAmount(row)) }}</p>
          <p class="freight" v-if="row.freight > 0">+运费: {{ formatAmount(row.freight) }}</p>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="860px" destroy-on-close>
      <div v-if="currentOrder.id" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusText(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ currentOrder.buyerNickname || currentOrder.buyerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="买家UID">{{ currentOrder.buyerUid || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ currentOrder.paymentStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ currentOrder.paidAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商品金额">{{ formatAmount(resolveDetailGoodsAmount(currentOrder)) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">{{ formatAmount(resolveDetailPayAmount(currentOrder)) }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ resolveTrackingNo(currentOrder) }}</el-descriptions-item>
          <el-descriptions-item label="运单位置">
            <div class="tracking-location" :class="{ empty: !resolveTrackingLocationMeta(currentOrder).primary }">
              <div class="tracking-location-primary">{{ resolveTrackingLocationMeta(currentOrder).primary || '-' }}</div>
              <div v-if="resolveTrackingLocationMeta(currentOrder).secondary" class="tracking-location-secondary">
                {{ resolveTrackingLocationMeta(currentOrder).secondary }}
              </div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ currentOrder.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="fee-section">
          <div class="detail-section-title">费用明细</div>
          <el-descriptions :column="3" border class="fee-descriptions">
            <el-descriptions-item label="商品金额">{{ formatAmount(resolveFeeAmount('goodsAmount', resolveDetailGoodsAmount(currentOrder))) }}</el-descriptions-item>
            <el-descriptions-item label="运费">{{ formatAmount(resolveFeeAmount('freightAmount', currentOrder.freightAmount || currentOrder.freight)) }}</el-descriptions-item>
            <el-descriptions-item label="优惠">{{ formatAmount(resolveFeeAmount('discountAmount', currentOrder.discountAmount || currentOrder.discount)) }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">{{ formatAmount(resolveFeeAmount('payAmount', resolveDetailPayAmount(currentOrder))) }}</el-descriptions-item>
            <el-descriptions-item label="平台抽佣">{{ formatAmount(resolveFeeAmount('platformCommissionAmount')) }}</el-descriptions-item>
            <el-descriptions-item label="经纪人分佣">{{ formatAmount(resolveFeeAmount('brokerCommissionAmount')) }}</el-descriptions-item>
            <el-descriptions-item label="预计结算金额" :span="3">
              <span class="settlement-amount">{{ formatAmount(resolveFeeAmount('settlementAmount', resolveDetailPayAmount(currentOrder))) }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-table
            v-if="commissionDetails.length"
            :data="commissionDetails"
            border
            size="small"
            class="commission-table"
          >
            <el-table-column label="接收人" min-width="150">
              <template #default="{ row }">
                <p>{{ row.receiverName || row.userId || '-' }}</p>
                <p class="phone">{{ row.receiverUid || '-' }}</p>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">{{ getCommissionTypeText(row.type, row.level) }}</template>
            </el-table-column>
            <el-table-column label="比例" width="90">
              <template #default="{ row }">{{ formatRate(row.rate) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">{{ getCommissionStatusText(row.status) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          </el-table>
        </div>

        <el-table :data="currentOrder.items || []" border class="detail-items">
          <el-table-column label="作品" min-width="320">
            <template #default="{ row }">
              <div class="artwork-info detail-artwork-info">
                <el-image v-if="row.cover || row.cover_url || row.coverImage" :src="getFullImageUrl(row.cover || row.cover_url || row.coverImage)" style="width: 48px; height: 48px" fit="cover" />
                <div v-else class="no-cover"><el-icon><Picture /></el-icon></div>
                <div class="detail-artwork-meta">
                  <p class="detail-artwork-title">{{ row.item_title || row.artwork_title || '-' }}</p>
                  <p class="detail-artwork-artist">{{ row.artist_name || '-' }}</p>
                  <p class="detail-artwork-extra">{{ formatArtworkDetail(row) }}</p>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">{{ formatAmount(resolveItemUnitPrice(row)) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">{{ formatAmount(resolveItemSubtotal(row)) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import request, { getFullImageUrl as getUrl } from '@/api/request'

const getFullImageUrl = getUrl

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const currentOrder = ref({})

const searchForm = reactive({
  orderNo: '',
  buyerName: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { pending: 'warning', paid: 'primary', shipped: 'info', completed: 'success', cancelled: 'info', refunding: 'warning', refunded: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待付款', paid: '已付款', shipped: '已发货', completed: '已完成', cancelled: '已取消', refunding: '退款中', refunded: '已退款' }
  return map[status] || status
}

const formatAmount = (value) => {
  return `¥${Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

const normalizeAmountScale = (rawValue, referenceValue = 0) => {
  const amount = Number(rawValue || 0)
  const reference = Number(referenceValue || 0)
  if (amount <= 0) {
    return reference > 0 ? reference : 0
  }
  return amount
}

const resolveDisplayAmount = (row = {}) => {
  const rawAmount = row.amount || row.payAmount || row.totalAmount || row.goodsAmount || 0
  const reference = row.firstItemSubtotal || row.firstItemPrice || 0
  return normalizeAmountScale(rawAmount, reference)
}

const resolveItemUnitPrice = (row = {}) => {
  const rawPrice = row.unit_price || row.price || 0
  const artworkReference = row.artwork_original_price || row.artwork_price || 0
  return normalizeAmountScale(rawPrice, artworkReference)
}

const resolveItemSubtotal = (row = {}) => {
  const quantity = Number(row.quantity || 1)
  const unitPrice = resolveItemUnitPrice(row)
  const rawSubtotal = row.subtotal_amount || row.subtotal || 0
  return normalizeAmountScale(rawSubtotal, unitPrice * quantity)
}

const resolveDetailGoodsAmount = (order = {}) => {
  const derived = (order.items || []).reduce((sum, item) => sum + resolveItemSubtotal(item), 0)
  return normalizeAmountScale(order.goodsAmount || order.totalAmount || 0, derived)
}

const resolveDetailPayAmount = (order = {}) => {
  const goodsAmount = resolveDetailGoodsAmount(order)
  const freight = Number(order.freightAmount || order.freight || 0)
  const discount = Number(order.discountAmount || order.discount || 0)
  const derived = Math.max(goodsAmount + freight - discount, 0)
  return normalizeAmountScale(order.payAmount || order.amount || 0, derived)
}

const resolveTrackingNo = (order = {}) => {
  return order.logistics?.trackingNo || order.logistics?.expressNo || '-'
}

const formatTrackingNode = (track = {}) => {
  const pieces = [track.time, track.location, track.desc].filter(Boolean)
  return pieces.length ? pieces.join(' / ') : ''
}

const resolveTrackingLocationMeta = (order = {}) => {
  const latestTrack = order.logistics?.traces?.[0] || {}
  const primary = latestTrack.time || order.logistics?.statusText || ''
  const secondary = [latestTrack.location, latestTrack.desc].filter(Boolean).join(' / ')
  return {
    primary,
    secondary
  }
}

const resolveTrackingLocation = (order = {}) => {
  const meta = resolveTrackingLocationMeta(order)
  return meta.secondary ? [meta.primary, meta.secondary].filter(Boolean).join(' / ') : meta.primary || '-'
}

const resolveFinancialSummary = () => {
  return currentOrder.value.financialSummary || currentOrder.value.feeSummary || currentOrder.value.fees || {}
}

const resolveFeeAmount = (key, fallback = 0) => {
  const summary = resolveFinancialSummary()
  return summary[key] ?? fallback ?? 0
}

const commissionDetails = computed(() => {
  const summary = resolveFinancialSummary()
  return summary.commissionDetails || []
})

const formatRate = (value) => {
  if (value === null || value === undefined || value === '') return '-'
  return `${Number(value).toFixed(2)}%`
}

const getCommissionTypeText = (type, level) => {
  const normalized = String(type || '').toLowerCase()
  if (normalized === 'promoter_reward' || normalized === '1') return '经纪人分佣'
  if (normalized === 'team_reward' || normalized === '2') return '团队奖励'
  return level === 2 ? '团队奖励' : '经纪人分佣'
}

const getCommissionStatusText = (status) => {
  const normalized = String(status ?? '').toLowerCase()
  const map = {
    pending: '待结算',
    settled: '已结算',
    freeze: '冻结中',
    cancel: '已取消',
    '0': '待结算',
    '1': '已结算',
    '2': '已失效'
  }
  return map[normalized] || status || '-'
}

const formatArtworkDetail = (row = {}) => {
  const info = []
  if (row.size) info.push(row.size)
  if (row.art_type) info.push(row.art_type)
  if (row.artwork_year) info.push(`${row.artwork_year}年`)
  return info.join(' / ') || '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.orderNo) params.orderNo = searchForm.orderNo
    if (searchForm.buyerName) params.buyerName = searchForm.buyerName
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const data = await request.get('/order/list', { params })
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载订单列表失败', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { orderNo: '', buyerName: '', status: '', dateRange: [] })
  handleSearch()
}

const viewDetail = async (row) => {
  try {
    const data = await request.get(`/order/detail/${row.id}`)
    currentOrder.value = {
      ...row,
      ...data,
      status: data.status || row.status,
      items: data.items || []
    }
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e?.message || '加载订单详情失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.artwork-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.artwork-thumb {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  flex-shrink: 0;
}

.artwork-title {
  display: -webkit-box;
  overflow: hidden;
  color: #303133;
  line-height: 1.4;
  word-break: break-all;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.phone {
  font-size: 12px;
  color: #999;
}

.price {
  font-weight: 500;
  color: #f56c6c;
}

.freight {
  font-size: 12px;
  color: #999;
}

.detail-items {
  margin-top: 16px;
}

.fee-section {
  margin-top: 16px;
}

.detail-section-title {
  margin-bottom: 10px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.fee-descriptions {
  margin-bottom: 12px;
}

.settlement-amount {
  color: #67c23a;
  font-weight: 600;
}

.commission-table {
  margin-top: 12px;
}

.no-cover {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  color: #c0c4cc;
  font-size: 24px;
  flex-shrink: 0;
}

.detail-artwork-info {
  align-items: flex-start;
}

.detail-artwork-meta {
  min-width: 0;
}

.detail-artwork-title {
  color: #303133;
  font-weight: 500;
  line-height: 1.5;
}

.detail-artwork-artist,
.detail-artwork-extra {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.tracking-location {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.5;
}

.tracking-location.empty {
  color: #909399;
}

.tracking-location-primary {
  color: #303133;
  font-weight: 500;
}

.tracking-location-secondary {
  color: #909399;
  font-size: 12px;
}
</style>
