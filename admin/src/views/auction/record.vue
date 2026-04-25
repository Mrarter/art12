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
          <p class="phone">{{ row.phone }}</p>
        </template>
      </el-table-column>
      <el-table-column label="出价" width="120">
        <template #default="{ row }">¥{{ row.bidPrice }}</template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.bidType === 'manual' ? 'primary' : 'warning'" size="small">
            {{ row.bidType === 'manual' ? '手动' : '代理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isWin ? 'success' : 'info'" size="small">
            {{ row.isWin ? '中标' : '出局' }}
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
    const data = await request.get('/auction/record/list', { params: { page: pagination.page, size: pagination.size, ...searchForm } })
    tableData.value = data.list
    pagination.total = data.total
  } catch (e) {
    tableData.value = [
      { bidCode: 'BID202604250001A3K7', lotCode: 'LOT202604240001A5K9', lotTitle: '名家山水', artistName: '张大千', userName: '张三', phone: '13800138001', bidPrice: 68000, bidType: 'manual', isWin: true, bidTime: '2024-02-06 14:30:00' },
      { bidCode: 'BID202604250002M8P2', lotCode: 'LOT202604240001A5K9', lotTitle: '名家山水', artistName: '张大千', userName: '李四', phone: '13800138002', bidPrice: 65000, bidType: 'manual', isWin: false, bidTime: '2024-02-06 14:25:00' },
      { bidCode: 'BID202604250003W5T9', lotCode: 'LOT202604240002B2F6', lotTitle: '花鸟画', artistName: '齐白石', userName: '王五', phone: '13800138003', bidPrice: 95000, bidType: 'proxy', isWin: true, bidTime: '2024-02-06 14:20:00' }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

const loadSessions = async () => {
  try {
    sessions.value = await request.get('/auction/sessions')
  } catch (e) {
    sessions.value = [{ sessionId: 'S001', name: '2024春季拍卖会' }]
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
