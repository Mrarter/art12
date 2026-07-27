<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">竞拍记录</span>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="专场">
          <el-select v-model="searchForm.sessionId" placeholder="全部" clearable>
            <el-option v-for="s in sessions" :key="s.sessionId" :label="s.name" :value="s.sessionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="bidCode" label="竞拍编号" width="200">
        <template #default="{ row }">
          <div class="id-cell" @click="handleCopyId(row.bidCode)">
            <span class="id-text">{{ row.bidCode || '-' }}</span>
            <el-icon class="copy-icon"><DocumentCopy /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="lotCode" label="拍品编号" width="200">
        <template #default="{ row }">
          <div class="id-cell" @click="handleCopyId(row.lotCode)">
            <span class="id-text">{{ row.lotCode || '-' }}</span>
            <el-icon class="copy-icon"><DocumentCopy /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="拍品" min-width="200">
        <template #default="{ row }">
          <p>{{ row.lotTitle }}</p>
          <p class="artist">{{ row.artistName }}</p>
        </template>
      </el-table-column>
      <el-table-column label="用户" min-width="150">
        <template #default="{ row }">
          <p>{{ row.userName }}</p>
          <p class="phone">用户 ID：{{ row.userId }}</p>
        </template>
      </el-table-column>
      <el-table-column label="出价" width="120">
        <template #default="{ row }">¥{{ row.bidPrice }}</template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '当前领先' : '已被超越' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="bidTime" label="出价时间" width="180" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import request from '@/api/request'
import { copyId } from '@/utils/id'

const loading = ref(false)
const tableData = ref([])
const sessions = ref([])

const searchForm = reactive({
  sessionId: '',
  userId: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 复制编号
const handleCopyId = async (id) => {
  if (!id) {
    ElMessage.warning('编号为空')
    return
  }
  copyId(id,
    () => ElMessage.success('已复制编号'),
    () => ElMessage.error('复制失败')
  )
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.sessionId !== '') params.sessionId = searchForm.sessionId
    if (searchForm.userId) params.userId = searchForm.userId
    const data = await request.get('/auction/bids', { params })
    tableData.value = (data.records || []).map(item => ({
      ...item,
      bidCode: `BID${String(item.id).padStart(12, '0')}`,
      lotCode: item.lotNo || `LOT${String(item.lotId).padStart(12, '0')}`,
      userName: `用户 ${item.userId}`,
      bidTime: formatDateTime(item.bidTime)
    }))
    pagination.total = data.total || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const loadSessions = async () => {
  try {
    const data = await request.get('/auction/sessions', { params: { page: 1, size: 100 } })
    sessions.value = (data.records || []).map(item => ({ sessionId: item.id, name: item.title || item.name }))
  } catch (e) {
    sessions.value = []
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { sessionId: '', userId: '' })
  handleSearch()
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-')
}

onMounted(() => {
  loadData()
  loadSessions()
})
</script>

<style scoped>
.artist, .phone {
  font-size: 12px;
  color: #999;
}

/* UID单元格样式 */
.id-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: #409eff;

  .id-text {
    letter-spacing: 0.5px;
  }

  .copy-icon {
    opacity: 0;
    transition: opacity 0.2s;
    font-size: 12px;
  }

  &:hover .copy-icon {
    opacity: 1;
  }
}
</style>
