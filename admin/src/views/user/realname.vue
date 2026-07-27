<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">实名认证审核</span>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">待审核</div>
          <div class="stat-value stat-orange">{{ pendingCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">已通过</div>
          <div class="stat-value stat-green">{{ approvedCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">已拒绝</div>
          <div class="stat-value stat-red">{{ rejectedCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索筛选 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline @submit.prevent="handleSearch">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option :label="'待审核'" :value="0" />
            <el-option :label="'已通过'" :value="1" />
            <el-option :label="'已拒绝'" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="昵称/姓名">
          <el-input v-model="searchForm.keyword" placeholder="输入昵称或姓名" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column label="ID" width="80">
        <template #default="{ row }">
          <span class="id-display">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户信息" min-width="180">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :src="getFullImageUrl(row.avatar)" :size="40" fit="cover" />
            <div class="user-detail">
              <p class="nickname">{{ row.nickname || '未知' }}</p>
              <p class="phone">{{ row.phone }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="真实姓名" width="120" prop="realName" />
      <el-table-column label="身份证号" width="200" prop="idCard" />
      <el-table-column label="证件照片" width="160">
        <template #default="{ row }">
          <div class="photo-group">
            <el-image
              v-if="row.idFrontUrl"
              :src="getFullImageUrl(row.idFrontUrl)"
              class="photo-thumb"
              fit="cover"
              :preview-src-list="[getFullImageUrl(row.idFrontUrl), getFullImageUrl(row.idBackUrl)].filter(Boolean)"
            />
            <el-image
              v-if="row.idBackUrl"
              :src="getFullImageUrl(row.idBackUrl)"
              class="photo-thumb"
              fit="cover"
            />
            <span v-if="!row.idFrontUrl && !row.idBackUrl" class="no-photo">无</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="人脸核验" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.faceVerified ? 'success' : 'info'" size="small">
            {{ row.faceVerified ? '已通过' : '未核验' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160" prop="createTime" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="showRejectDialog(row)">拒绝</el-button>
          </template>
          <el-tag v-else type="info" size="small">已处理</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝认证" width="400px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请填写拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject" :loading="rejectLoading">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request, { getFullImageUrl } from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const currentRecord = ref(null)
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)

const tableData = ref([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const searchForm = reactive({
  status: '',
  keyword: ''
})

const statusType = (status) => {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

const statusLabel = (status) => {
  return { 0: '待审核', 1: '已通过', 2: '已拒绝' }[status] || '未知'
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await request.post('/user/realname/list', {
      page: pagination.page,
      size: pagination.size,
      status: searchForm.status !== '' ? searchForm.status : null,
      keyword: searchForm.keyword || null
    })
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
    pagination.page = data.page || pagination.page

    // Count stats from all data
    let pending = 0, approved = 0, rejected = 0
    ;(data.records || data.list || []).forEach(r => {
      if (r.status === 0) pending++
      else if (r.status === 1) approved++
      else if (r.status === 2) rejected++
    })
    pendingCount.value = pending
    approvedCount.value = approved
    rejectedCount.value = rejected
  } catch (err) {
    console.error('加载数据失败:', err)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  searchForm.status = ''
  searchForm.keyword = ''
  pagination.page = 1
  loadData()
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定通过 ${row.realName || row.nickname} 的实名认证？`, '提示', { type: 'success' })
    await request.post('/user/realname/approve', { certId: row.id })
    ElMessage.success('已通过认证')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const showRejectDialog = (row) => {
  currentRecord.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const handleReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejectLoading.value = true
  try {
    await request.post('/user/realname/reject', {
      certId: currentRecord.value.id,
      reason: rejectReason.value.trim()
    })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    await loadData()
  } catch (e) {
    console.error(e)
  } finally {
    rejectLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.title {
  font-size: 20px;
  font-weight: 700;
}
.stats-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  font-size: 28px;
  font-weight: 800;
  margin-top: 8px;
}
.stat-green { color: #67c23a; }
.stat-orange { color: #e6a23c; }
.stat-red { color: #f56c6c; }
.search-card {
  margin-bottom: 16px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-detail p {
  margin: 0;
  line-height: 1.5;
}
.nickname {
  font-weight: 600;
}
.phone {
  font-size: 12px;
  color: #909399;
}
.id-display {
  color: #409eff;
  cursor: pointer;
  font-size: 12px;
}
.photo-group {
  display: flex;
  gap: 6px;
}
.photo-thumb {
  width: 48px;
  height: 36px;
  border-radius: 4px;
  cursor: pointer;
}
.no-photo {
  color: #909399;
  font-size: 12px;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
