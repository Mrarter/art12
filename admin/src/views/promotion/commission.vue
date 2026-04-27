<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">佣金记录</span>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="艺荐官">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable>
            <el-option label="直接佣金" value="direct" />
            <el-option label="团队佣金" value="team" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
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
      <el-table-column prop="commissionCode" label="佣金编号" width="200">
        <template #default="{ row }">
          <IdCell :value="row.commissionCode" success-message="已复制佣金编号" />
        </template>
      </el-table-column>
      <el-table-column label="艺荐官" min-width="150">
        <template #default="{ row }">
          <p>{{ row.promoterName }}</p>
          <p class="phone">{{ row.promoterPhone }}</p>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'direct' ? 'primary' : 'warning'" size="small">
            {{ row.type === 'direct' ? '直接佣金' : '团队佣金' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订单信息" min-width="200">
        <template #default="{ row }">
          <p class="clickable" @click="showOrderDetail(row.orderNo)">订单号: <span class="order-link">{{ row.orderNo }}</span></p>
          <p>买家: {{ row.buyerName }}</p>
        </template>
      </el-table-column>
      <el-table-column label="订单金额" width="120">
        <template #default="{ row }">¥{{ row.orderAmount }}</template>
      </el-table-column>
      <el-table-column label="佣金比例" width="100">
        <template #default="{ row }">{{ (row.rate * 100).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column label="佣金金额" width="120">
        <template #default="{ row }" class="amount">¥{{ row.commission }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'settled' ? 'success' : 'warning'" size="small">
            {{ row.status === 'settled' ? '已结算' : '待结算' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="180" />
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

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="orderDialogVisible" title="订单详情" width="700px">
      <div v-if="orderDetail" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ orderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusType(orderDetail.orderStatus)" size="small">
              {{ orderDetail.statusText }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="购买用户">
            <span v-if="orderDetail.buyerNickname">{{ orderDetail.buyerNickname }}</span>
            <span v-else-if="orderDetail.buyerUid">{{ orderDetail.buyerUid }}</span>
            <span v-else>用户ID: {{ orderDetail.userId }}</span>
            <span v-if="orderDetail.buyerPhone" class="phone">{{ orderDetail.buyerPhone }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="商品金额">¥{{ orderDetail.goodsAmount }}</el-descriptions-item>
          <el-descriptions-item label="运费">¥{{ orderDetail.freightAmount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">-¥{{ orderDetail.discountAmount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="实付金额" class="highlight">
            <strong>¥{{ orderDetail.payAmount }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="orderDetail.paymentStatus === 'PAID' ? 'success' : 'warning'" size="small">
              {{ orderDetail.paymentStatus === 'PAID' ? '已支付' : '未支付' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ orderDetail.createTime }}</el-descriptions-item>
        </el-descriptions>

        <!-- 商品信息 -->
        <h4 class="section-title">商品信息</h4>
        <el-table :data="orderDetail.items || []" border size="small">
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div class="artwork-info">
                <el-image v-if="row.cover_image" :src="row.cover_image" fit="cover" class="artwork-thumb" />
                <span>{{ row.artwork_title || '商品' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="小计" width="120" align="right">
            <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="orderDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import IdCell from '@/components/IdCell.vue'

const loading = ref(false)
const tableData = ref([])
const orderDialogVisible = ref(false)
const orderDetail = ref(null)

const searchForm = reactive({
  userId: '',
  type: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size, ...searchForm }
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const data = await request.get('/promotion/commission/list', { params })
    tableData.value = data.list
    pagination.total = data.total
  } catch (e) {
    tableData.value = [
      { id: 1, commissionCode: 'CMS202604250001A7K3', promoterName: '艺荐官A', promoterPhone: '13900139001', type: 'direct', orderNo: 'SYJ20240120001', buyerName: '张三', orderAmount: 58000, rate: 0.1, commission: 5800, status: 'settled', createTime: '2024-01-20 10:30:00' },
      { id: 2, commissionCode: 'CMS202604250002D4P8', promoterName: '艺荐官A', promoterPhone: '13900139001', type: 'team', orderNo: 'SYJ20240120002', buyerName: '李四', orderAmount: 42000, rate: 0.05, commission: 2100, status: 'settled', createTime: '2024-01-19 14:20:00' },
      { id: 3, commissionCode: 'CMS202604250003H2N5', promoterName: '艺荐官B', promoterPhone: '13900139002', type: 'direct', orderNo: 'SYJ20240120003', buyerName: '王五', orderAmount: 65000, rate: 0.1, commission: 6500, status: 'pending', createTime: '2024-01-18 09:15:00' }
    ]
    pagination.total = 1
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { userId: '', type: '', dateRange: [] })
  handleSearch()
}

const showOrderDetail = async (orderNo) => {
  try {
    const res = await request.get(`/order/detail/by-no/${orderNo}`)
    if (res.code === 200 && res.data) {
      orderDetail.value = res.data
      orderDialogVisible.value = true
    } else {
      ElMessage.warning('订单详情加载失败')
    }
  } catch (e) {
    // 模拟数据
    orderDetail.value = {
      orderNo: orderNo,
      orderStatus: 'COMPLETED',
      statusText: '已完成',
      buyerNickname: '张三',
      buyerUid: 'USR202604250001VKO5',
      buyerPhone: '139****1234',
      userId: 1,
      goodsAmount: 58000,
      freightAmount: 0,
      discountAmount: 0,
      payAmount: 58000,
      paymentStatus: 'PAID',
      createTime: '2024-01-20 10:30:00',
      items: [
        { artwork_title: '山水国画', cover_image: 'https://picsum.photos/100/100', quantity: 1, price: 58000 }
      ]
    }
    orderDialogVisible.value = true
  }
}

const getOrderStatusType = (status) => {
  const map = {
    'PENDING_PAYMENT': 'info',
    'PAID': 'warning',
    'SHIPPED': 'primary',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.phone {
  font-size: 12px;
  color: #999;
}

.amount {
  color: #67c23a;
  font-weight: 500;
}

.clickable {
  cursor: pointer;
}

.order-link {
  color: #409eff;
  text-decoration: underline;
}

.order-link:hover {
  color: #66b1ff;
}

.order-detail {
  padding: 10px 0;
}

.highlight {
  color: #f56c6c;
  font-weight: bold;
}

.section-title {
  margin: 16px 0 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  color: #333;
}

.artwork-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.artwork-thumb {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
}

.phone {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}
</style>
