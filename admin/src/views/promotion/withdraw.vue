<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">提现管理</span>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="艺荐官">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待处理" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
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
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="艺荐官" min-width="150">
        <template #default="{ row }">
          <p>{{ row.userName }}</p>
          <p class="phone">{{ row.phone }}</p>
        </template>
      </el-table-column>
      <el-table-column label="提现金额" width="120">
        <template #default="{ row }" class="amount">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column label="提现方式" width="120">
        <template #default="{ row }">
          {{ row.type === 'wechat' ? '微信' : row.type === 'alipay' ? '支付宝' : '银行卡' }}
        </template>
      </el-table-column>
      <el-table-column label="收款信息" min-width="200">
        <template #default="{ row }">
          <p>{{ row.account }}</p>
          <p class="name">{{ row.realName }}</p>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'pending'">
            <el-button type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
          </template>
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
    
    <el-dialog v-model="rejectVisible" title="拒绝原因" width="500px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>

    <!-- 提现详情弹窗 -->
    <el-dialog v-model="detailVisible" title="提现详情" width="500px" destroy-on-close>
      <div v-if="currentRecord.id" class="withdraw-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="艺荐官">{{ currentRecord.userName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentRecord.phone }}</el-descriptions-item>
          <el-descriptions-item label="提现金额">
            <span class="amount">¥{{ currentRecord.amount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提现方式">
            {{ currentRecord.type === 'wechat' ? '微信' : currentRecord.type === 'alipay' ? '支付宝' : '银行卡' }}
          </el-descriptions-item>
          <el-descriptions-item label="收款账户">{{ currentRecord.account }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ currentRecord.realName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentRecord.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer v-if="currentRecord.status === 'pending'">
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="danger" @click="handleReject(currentRecord)">拒绝</el-button>
        <el-button type="success" @click="handleApprove(currentRecord)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref([])
const rejectVisible = ref(false)
const detailVisible = ref(false)
const currentRecord = ref({})
const rejectReason = ref('')

const searchForm = reactive({
  userId: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待处理', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.userId) params.userId = searchForm.userId
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const data = await request.get('/promotion/withdraw/list', { params })
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    tableData.value = [
      { id: 1, userName: '艺荐官A', phone: '13900139001', amount: 5000, type: 'wechat', account: 'wx123456', realName: '张三', status: 'pending', createTime: '2024-01-21 10:00:00' },
      { id: 2, userName: '艺荐官B', phone: '13900139002', amount: 8000, type: 'alipay', account: '139****9002', realName: '李四', status: 'pending', createTime: '2024-01-20 14:30:00' },
      { id: 3, userName: '艺荐官C', phone: '13900139003', amount: 3000, type: 'bank', account: '6222****1234', realName: '王五', status: 'approved', createTime: '2024-01-19 09:00:00' }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { userId: '', status: '', dateRange: [] })
  handleSearch()
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该提现申请吗？', '提示', { type: 'success' })
    await request.post('/promotion/withdraw/approve', { id: row.id })
    detailVisible.value = false
    ElMessage.success('已通过')
    loadData()
  } catch (e) {
    console.error('通过失败', e)
  }
}

const handleReject = (row) => {
  currentRecord.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await request.post('/promotion/withdraw/reject', {
      id: currentRecord.value.id,
      reason: rejectReason.value
    })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    detailVisible.value = false
    loadData()
  } catch (e) {
    console.error('拒绝失败', e)
  }
}

const viewDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.phone, .name {
  font-size: 12px;
  color: #999;
}

.amount {
  color: #f56c6c;
  font-weight: 500;
}

.withdraw-detail {
  .amount {
    color: #f56c6c;
    font-weight: bold;
    font-size: 16px;
  }
}
</style>
