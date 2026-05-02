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
      <el-table-column label="作品信息" min-width="200">
        <template #default="{ row }">
          <div class="artwork-info">
            <el-image :src="getFullImageUrl(row.cover)" style="width: 50px; height: 50px" fit="cover" />
            <span>{{ row.artworkTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="订单金额" width="120">
        <template #default="{ row }">
          <p class="price">¥{{ row.amount }}</p>
          <p class="freight" v-if="row.freight > 0">+运费: ¥{{ row.freight }}</p>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="720px" destroy-on-close>
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
          <el-descriptions-item label="商品金额">¥{{ currentOrder.goodsAmount || currentOrder.totalAmount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ currentOrder.payAmount || currentOrder.amount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ currentOrder.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="currentOrder.items || []" border class="detail-items">
          <el-table-column label="作品" min-width="220">
            <template #default="{ row }">
              <div class="artwork-info">
                <el-image :src="getFullImageUrl(row.cover_url || row.coverImage)" style="width: 48px; height: 48px" fit="cover" />
                <span>{{ row.item_title || row.artwork_title || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">¥{{ row.unit_price || 0 }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥{{ row.subtotal_amount || 0 }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
</style>
