<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">售后管理</span>
      <el-radio-group v-model="status" @change="loadData">
        <el-radio-button label="pending">待处理</el-radio-button>
        <el-radio-button label="approved">已通过</el-radio-button>
        <el-radio-button label="rejected">已拒绝</el-radio-button>
      </el-radio-group>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column label="作品信息" min-width="200">
        <template #default="{ row }">
          <div class="artwork-info">
            <el-image :src="getFullImageUrl(row.cover)" style="width: 50px; height: 50px" fit="cover" />
            <span>{{ row.artworkTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="买家" width="120">
        <template #default="{ row }">{{ row.buyerName }}</template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'refund' ? 'warning' : 'primary'">
            {{ row.type === 'refund' ? '退款' : '退货退款' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column label="原因" min-width="150">
        <template #default="{ row }">{{ row.reason }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
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

    <!-- 售后详情弹窗 -->
    <el-dialog v-model="detailVisible" title="售后详情" width="600px" destroy-on-close>
      <div v-if="currentRecord.id" class="aftersale-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ currentRecord.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="作品名称">{{ currentRecord.artworkTitle }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ currentRecord.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="买家UID">{{ currentRecord.buyerUid || '-' }}</el-descriptions-item>
          <el-descriptions-item label="售后类型">
            <el-tag :type="currentRecord.type === 'refund' ? 'warning' : 'primary'" size="small">
              {{ currentRecord.type === 'refund' ? '退款' : '退货退款' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退款金额">
            <span class="amount">¥{{ currentRecord.amount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="申请原因">{{ currentRecord.reason }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRecord.remark" label="处理备注">{{ currentRecord.remark }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentRecord.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer v-if="currentRecord.status === 'pending'">
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject(currentRecord)">拒绝</el-button>
        <el-button type="success" @click="handleApprove(currentRecord)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { getFullImageUrl as getUrl } from '@/api/request'

const getFullImageUrl = getUrl

const loading = ref(false)
const status = ref('pending')
const tableData = ref([])
const detailVisible = ref(false)
const currentRecord = ref({})

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
    const data = await request.get('/order/aftersale/list', { 
      params: { page: pagination.page, size: pagination.size, status: status.value } 
    })
    
    // 处理返回数据，适配前端字段名
    tableData.value = (data.records || []).map(item => ({
      id: item.id,
      orderNo: item.orderNo,
      cover: item.cover || item.coverImage || '',
      artworkTitle: item.artworkTitle || item.goodsTitle || '',
      buyerName: item.buyerName || item.buyerNickname || '',
      buyerUid: item.buyerUid || '',
      type: item.type || 'refund',
      amount: item.amount || item.payAmount || 0,
      reason: item.reason || item.refundReason || '',
      status: item.status || 'pending',
      remark: item.remark || '',
      createTime: item.createTime || item.applyTime || '',
      handleTime: item.handleTime || '',
      completeTime: item.completeTime || ''
    }))
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载售后列表失败', e)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该售后申请吗？', '提示', { type: 'success' })
    // 调用后端API保存
    await request.post('/order/aftersale/handle', {
      id: row.id,
      status: 'approved',
      remark: '管理员通过'
    })
    ElMessage.success('已通过')
    detailVisible.value = false
    // 刷新列表
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm('确定拒绝该售后申请吗？', '提示', { type: 'warning' })
    // 调用后端API保存
    await request.post('/order/aftersale/handle', {
      id: row.id,
      status: 'rejected',
      remark: '管理员拒绝'
    })
    ElMessage.success('已拒绝')
    // 关闭详情弹窗
    detailVisible.value = false
    // 刷新列表
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
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
.artwork-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.aftersale-detail {
  .amount {
    color: #f56c6c;
    font-weight: bold;
    font-size: 16px;
  }
}
</style>
