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
            <el-option label="待上拍" :value="0" />
            <el-option label="竞拍中" :value="1" />
            <el-option label="已成交" :value="2" />
            <el-option label="流拍" :value="3" />
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
            <el-image :src="getFullImageUrl(row.cover)" style="width: 60px; height: 60px" fit="cover" />
            <div>
              <p class="title">{{ row.title }}</p>
              <p class="artist">{{ row.artistName }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="专场" width="180">
        <template #default="{ row }">{{ getSessionName(row.sessionId) }}</template>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑拍品' : '添加拍品'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入作品标题" />
        </el-form-item>
        <el-form-item label="拍品编号" prop="lotNo">
          <el-input v-model="form.lotNo" placeholder="例如 QA-001" />
        </el-form-item>
        <el-form-item label="艺术家">
          <el-input v-model="form.artistName" placeholder="请输入艺术家名称" />
        </el-form-item>
        <el-form-item label="所属专场" prop="sessionId">
          <el-select v-model="form.sessionId" placeholder="请选择专场" style="width: 100%">
            <el-option v-for="s in sessions" :key="s.sessionId" :label="s.name" :value="s.sessionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图片">
          <el-input v-model="form.cover" placeholder="图片 URL 或上传图片">
            <template #append><el-button @click="triggerCoverUpload">上传</el-button></template>
          </el-input>
          <input ref="coverFileInput" type="file" accept="image/*" style="display:none" @change="handleCoverChange" />
        </el-form-item>
        <el-form-item label="起拍价" prop="startPrice">
          <el-input-number v-model="form.startPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="保留价">
          <el-input-number v-model="form.reservePrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="加价幅度" prop="increment">
          <el-input-number v-model="form.increment" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="保证金" prop="depositAmount">
          <el-input-number v-model="form.depositAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="竞拍时间" required>
          <el-col :span="11"><el-form-item prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" placeholder="开始时间" style="width:100%" /></el-form-item></el-col>
          <el-col :span="2" style="text-align:center">至</el-col>
          <el-col :span="11"><el-form-item prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" placeholder="结束时间" style="width:100%" /></el-form-item></el-col>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="待上拍" :value="0" />
            <el-option label="竞拍中" :value="1" />
            <el-option label="已成交" :value="2" />
            <el-option label="流拍" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 出价记录弹窗 -->
    <el-dialog v-model="recordVisible" title="出价记录" width="600px">
      <div v-if="currentLot.id" class="lot-info">
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
import request, { uploadFile, getFullImageUrl } from '@/api/request'
import { copyId } from '@/utils/id'

const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const sessions = ref([])
const dialogVisible = ref(false)
const recordVisible = ref(false)
const formRef = ref()
const coverFileInput = ref()
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
  increment: 50,
  depositAmount: 0,
  lotNo: '',
  startTime: '',
  endTime: '',
  status: 0,
  sessionId: ''
})

const rules = {
  title: [{ required: true, message: '请输入作品标题', trigger: 'blur' }],
  lotNo: [{ required: true, message: '请输入拍品编号', trigger: 'blur' }],
  sessionId: [{ required: true, message: '请选择专场', trigger: 'change' }],
  startPrice: [{ required: true, message: '请输入起拍价', trigger: 'change' }],
  increment: [{ required: true, message: '请输入加价幅度', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const currentLot = ref({})
const bidRecords = ref([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '待上拍', 1: '竞拍中', 2: '已成交', 3: '流拍' }
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
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.sessionId !== '') params.sessionId = searchForm.sessionId
    if (searchForm.status !== '') params.status = searchForm.status
    const data = await request.get('/auction/lots', { params })
    tableData.value = (data.records || data.list || []).map(item => ({
      lotId: 'L' + String(item.id),
      id: item.id,
      title: item.title,
      cover: item.coverImage || item.cover || '',
      artistName: item.artistName,
      lotNo: item.lotNo,
      startPrice: item.startPrice,
      currentPrice: item.currentPrice,
      reservePrice: item.reservePrice,
      increment: item.increment,
      depositAmount: item.depositAmount,
      bidCount: item.bidCount,
      status: item.status,
      startTime: item.startTime,
      endTime: item.endTime,
      sessionId: item.sessionId
    }))
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载拍品列表失败:', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const loadSessions = async () => {
  try {
    const data = await request.get('/auction/sessions', { params: { page: 1, size: 100 } })
    sessions.value = (data.records || data.list || data || []).map(s => ({
      sessionId: s.id,
      id: s.id,
      name: s.title || s.name
    }))
  } catch (e) {
    console.error('加载专场列表失败:', e)
    sessions.value = []
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
  Object.assign(form, { id: null, title: '', artistName: '', cover: '', startPrice: 0, reservePrice: 0, increment: 50, depositAmount: 0, lotNo: '', startTime: '', endTime: '', status: 0, sessionId: route.query.sessionId ? Number(route.query.sessionId) : '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (new Date(form.endTime).getTime() <= new Date(form.startTime).getTime()) return ElMessage.warning('结束时间必须晚于开始时间')
  try {
    const payload = {
      title: form.title,
      coverImage: form.cover || '',
      artistName: form.artistName || '',
      sessionId: form.sessionId,
      lotNo: form.lotNo || 0,
      startPrice: form.startPrice,
      reservePrice: form.reservePrice || 0,
      increment: form.increment,
      depositAmount: form.depositAmount || 0,
      status: form.status,
      startTime: form.startTime,
      endTime: form.endTime
    }
    if (isEdit.value) {
      await request.put(`/auction/lots/${form.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await request.post('/auction/lots', payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  }
}

const viewRecord = async (row) => {
  currentLot.value = row
  recordVisible.value = true
  try {
    const data = await request.get('/auction/bids', { params: { lotId: row.id, page: 1, size: 100 } })
    bidRecords.value = (data.records || []).map(item => ({ id: item.id, bidder: `用户 ${item.userId}`, price: item.bidPrice, time: formatDateTime(item.bidTime) }))
  } catch (e) {
    bidRecords.value = []
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该拍品吗？', '提示', { type: 'warning' })
    await request.delete(`/auction/lots/${row.id}`)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败: ' + (e.message || '未知错误'))
  }
}

const getSessionName = (id) => sessions.value.find(item => Number(item.sessionId) === Number(id))?.name || `专场 #${id}`

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-')
}

const triggerCoverUpload = () => coverFileInput.value?.click()
const handleCoverChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/') || file.size > 10 * 1024 * 1024) {
    ElMessage.warning('请选择不超过 10MB 的图片')
    return
  }
  try {
    const result = await uploadFile(file)
    form.cover = result?.url || result || ''
    ElMessage.success('封面上传成功')
  } catch (e) {
    ElMessage.error('封面上传失败: ' + (e.message || '未知错误'))
  } finally {
    event.target.value = ''
  }
}

onMounted(() => {
  // 从路由参数获取专场ID
  if (route.query.sessionId) {
    searchForm.sessionId = Number(route.query.sessionId)
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

  .price {
    color: #67c23a;
    font-weight: bold;
    font-size: 16px;
  }
}

.lot-info p {
  margin: 4px 0;
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
