<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">拍品管理</span>
      <el-button type="primary" @click="handleAdd">添加拍品</el-button>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="专场">
          <el-select v-model="searchForm.sessionId" placeholder="全部" clearable>
            <el-option v-for="s in sessions" :key="s.sessionId" :label="s.name" :value="s.sessionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待上拍" value="pending" />
            <el-option label="已成交" value="sold" />
            <el-option label="未成交" value="unsold" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="lotCode" label="拍品编号" width="200">
        <template #default="{ row }">
          <div class="id-cell" @click="handleCopyId(row.lotCode)">
            <span class="id-text">{{ row.lotCode || '-' }}</span>
            <el-icon class="copy-icon"><DocumentCopy /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="作品信息" min-width="250" class-name="artwork-header">
        <template #default="{ row }">
          <div class="artwork-info">
            <el-image :src="row.cover" style="width: 60px; height: 60px" fit="cover" />
            <div>
              <p class="title">{{ row.title }}</p>
              <p class="artist">{{ row.artistName }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="专场" width="180">
        <template #default="{ row }">{{ row.sessionName }}</template>
      </el-table-column>
      <el-table-column label="起拍价" width="120">
        <template #default="{ row }">¥{{ row.startPrice }}</template>
      </el-table-column>
      <el-table-column label="成交价" width="120">
        <template #default="{ row }">
          <span v-if="row.finalPrice">¥{{ row.finalPrice }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="出价次数" width="100">
        <template #default="{ row }">{{ row.bidCount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="viewRecord(row)">记录</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 添加/编辑拍品弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑拍品' : '添加拍品'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="作品标题">
          <el-input v-model="form.title" placeholder="请输入作品标题" />
        </el-form-item>
        <el-form-item label="艺术家">
          <el-input v-model="form.artistName" placeholder="请输入艺术家名称" />
        </el-form-item>
        <el-form-item label="所属专场">
          <el-select v-model="form.sessionId" placeholder="请选择专场" style="width: 100%">
            <el-option v-for="s in sessions" :key="s.sessionId" :label="s.name" :value="s.sessionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="起拍价">
          <el-input-number v-model="form.startPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="保留价">
          <el-input-number v-model="form.reservePrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 出价记录弹窗 -->
    <el-dialog v-model="recordVisible" title="出价记录" width="600px">
      <div v-if="currentLot.lotId" class="lot-info">
        <p><strong>拍品：</strong>{{ currentLot.title }}</p>
        <p><strong>艺术家：</strong>{{ currentLot.artistName }}</p>
        <p><strong>起拍价：</strong>¥{{ currentLot.startPrice }}</p>
        <p><strong>成交价：</strong><span class="price">¥{{ currentLot.finalPrice || '未成交' }}</span></p>
      </div>
      <el-table :data="bidRecords" border stripe size="small">
        <el-table-column prop="bidder" label="出价人" />
        <el-table-column prop="price" label="出价">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="time" label="时间" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import request from '@/api/request'
import { copyId } from '@/utils/id'

const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const sessions = ref([])
const dialogVisible = ref(false)
const recordVisible = ref(false)
const formRef = ref()
const isEdit = ref(false)

const searchForm = reactive({
  sessionId: '',
  status: ''
})

const form = reactive({
  lotId: '',
  title: '',
  artistName: '',
  cover: '',
  startPrice: 0,
  reservePrice: 0,
  sessionId: ''
})

const currentLot = ref({})
const bidRecords = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { pending: 'warning', sold: 'success', unsold: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待上拍', sold: '已成交', unsold: '未成交' }
  return map[status] || status
}

// 复制拍品编号
const handleCopyId = async (id) => {
  if (!id) {
    ElMessage.warning('拍品编号为空')
    return
  }
  copyId(id,
    () => ElMessage.success('已复制拍品编号'),
    () => ElMessage.error('复制失败')
  )
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await request.get('/auction/lot/list', { params: { page: pagination.page, size: pagination.size, ...searchForm } })
    tableData.value = data.list
    pagination.total = data.total
  } catch (e) {
    // 使用本地模拟数据
    const sessionId = route.query.sessionId || ''
    if (!tableData.value.length) {
      tableData.value = sessionId === 'S002' ? [
        { lotId: 'L101', lotCode: 'LOT202604250001X7K2', title: '现代油画', artistName: '李明', cover: '', sessionId: 'S002', sessionName: '当代艺术专场', startPrice: 30000, finalPrice: 0, bidCount: 5, status: 'pending' },
        { lotId: 'L102', lotCode: 'LOT202604250002M9N5', title: '抽象艺术', artistName: '王芳', cover: '', sessionId: 'S002', sessionName: '当代艺术专场', startPrice: 50000, finalPrice: 0, bidCount: 8, status: 'pending' },
        { lotId: 'L103', lotCode: 'LOT202604250003W3T8', title: '风景写生', artistName: '赵丽', cover: '', sessionId: 'S002', sessionName: '当代艺术专场', startPrice: 20000, finalPrice: 0, bidCount: 3, status: 'pending' }
      ] : [
        { lotId: 'L001', lotCode: 'LOT202604240001A5K9', title: '名家山水', artistName: '张大千', cover: '', sessionId: 'S001', sessionName: '2024春季拍卖会', startPrice: 50000, finalPrice: 68000, bidCount: 12, status: 'sold' },
        { lotId: 'L002', lotCode: 'LOT202604240002B2F6', title: '花鸟画', artistName: '齐白石', cover: '', sessionId: 'S001', sessionName: '2024春季拍卖会', startPrice: 80000, finalPrice: 95000, bidCount: 15, status: 'sold' },
        { lotId: 'L003', lotCode: 'LOT202604240003C8H1', title: '书法作品', artistName: '启功', cover: '', sessionId: 'S001', sessionName: '2024春季拍卖会', startPrice: 100000, finalPrice: 0, bidCount: 2, status: 'unsold' }
      ]
    }
    pagination.total = tableData.value.length
  } finally {
    loading.value = false
  }
}

const loadSessions = async () => {
  try {
    sessions.value = await request.get('/auction/sessions')
  } catch (e) {
    sessions.value = [
      { sessionId: 'S001', name: '2024春季拍卖会' },
      { sessionId: 'S002', name: '当代艺术专场' },
      { sessionId: 'S003', name: '书画精品专场' }
    ]
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { sessionId: '', status: '' })
  handleSearch()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { lotId: '', title: '', artistName: '', cover: '', startPrice: 0, reservePrice: 0, sessionId: route.query.sessionId || '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.title || !form.startPrice) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (isEdit.value) {
      const index = tableData.value.findIndex(item => item.lotId === form.lotId)
      if (index > -1) {
        const session = sessions.value.find(s => s.sessionId === form.sessionId)
        tableData.value[index] = { ...tableData.value[index], ...form, sessionName: session?.name || '' }
        ElMessage.success('更新成功')
      }
    } else {
      const newId = 'L' + String(Math.max(...tableData.value.map(item => parseInt(item.lotId.slice(1))), 0) + 1).padStart(3, '0')
      const session = sessions.value.find(s => s.sessionId === form.sessionId)
      tableData.value.unshift({ lotId: newId, ...form, sessionName: session?.name || '', finalPrice: 0, bidCount: 0, status: 'pending' })
      pagination.total++
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const viewRecord = (row) => {
  currentLot.value = row
  // 模拟出价记录
  bidRecords.value = [
    { id: 1, bidder: '用户A', price: 55000, time: '2024-03-06 10:30:00' },
    { id: 2, bidder: '用户B', price: 58000, time: '2024-03-06 10:35:00' },
    { id: 3, bidder: '用户C', price: 62000, time: '2024-03-06 10:40:00' },
    { id: 4, bidder: '用户D', price: 65000, time: '2024-03-06 10:45:00' },
    { id: 5, bidder: '用户E', price: 68000, time: '2024-03-06 10:50:00' }
  ]
  recordVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该拍品吗？', '提示', { type: 'warning' })
    const index = tableData.value.findIndex(item => item.lotId === row.lotId)
    if (index > -1) {
      tableData.value.splice(index, 1)
      pagination.total--
      ElMessage.success('删除成功')
    }
  } catch (e) {}
}

onMounted(() => {
  // 从路由参数获取专场ID
  if (route.query.sessionId) {
    searchForm.sessionId = route.query.sessionId
  }
  loadData()
  loadSessions()
})
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 4px;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.page-header .title {
  font-size: 18px;
  font-weight: 600;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.artwork-info {
  display: flex;
  gap: 10px;
  
  .title {
    font-weight: 500;
  }
  .artist {
    font-size: 13px;
    color: #999;
  }
}

.lot-info {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  
  p {
    margin: 4px 0;
  }
  
  .price {
    color: #67c23a;
    font-weight: bold;
    font-size: 16px;
  }
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

/* 作品信息列头样式 */
:deep(.artwork-header) {
  .cell {
    width: 116px;
    height: 23px;
    padding-left: 12px;
    padding-right: 12px;
    line-height: 23px;
  }
}
</style>
