<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">拍卖专场</span>
      <el-button type="primary" @click="showDialog('add')">创建专场</el-button>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="专场名称">
          <el-input v-model="searchForm.name" placeholder="请输入专场名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="预展中" value="preview" />
            <el-option label="进行中" value="ongoing" />
            <el-option label="已结束" value="ended" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="sessionId" label="专场ID" width="100" />
      <el-table-column prop="name" label="专场名称" min-width="200" />
      <el-table-column label="封面" width="120">
        <template #default="{ row }">
          <el-image :src="row.cover" style="width: 80px; height: 60px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="时间" width="300">
        <template #default="{ row }">
          <p>预展: {{ row.previewStart }} ~ {{ row.previewEnd }}</p>
          <p>拍卖: {{ row.auctionStart }} ~ {{ row.auctionEnd }}</p>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="成交额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog('edit', row)">编辑</el-button>
          <el-button type="primary" link @click="manageLots(row)">拍品</el-button>
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
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="专场名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入专场名称" />
        </el-form-item>
        <el-form-item label="封面图" prop="cover">
          <el-input v-model="form.cover" placeholder="请输入封面图URL" />
        </el-form-item>
        <el-form-item label="预展时间" required>
          <el-col :span="10">
            <el-form-item prop="previewStart">
              <el-date-picker v-model="form.previewStart" type="datetime" placeholder="开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="2" style="text-align: center">至</el-col>
          <el-col :span="10">
            <el-form-item prop="previewEnd">
              <el-date-picker v-model="form.previewEnd" type="datetime" placeholder="结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-form-item>
        <el-form-item label="拍卖时间" required>
          <el-col :span="10">
            <el-form-item prop="auctionStart">
              <el-date-picker v-model="form.auctionStart" type="datetime" placeholder="开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="2" style="text-align: center">至</el-col>
          <el-col :span="10">
            <el-form-item prop="auctionEnd">
              <el-date-picker v-model="form.auctionEnd" type="datetime" placeholder="结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-form-item>
        <el-form-item label="保证金" prop="deposit">
          <el-input-number v-model="form.deposit" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const tableData = ref([])
const isEdit = ref(false)

const searchForm = reactive({
  name: '',
  status: ''
})

const form = reactive({
  name: '',
  cover: '',
  previewStart: '',
  previewEnd: '',
  auctionStart: '',
  auctionEnd: '',
  deposit: 1000,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入专场名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑专场' : '创建专场')

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { preview: 'primary', ongoing: 'success', ended: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { preview: '预展中', ongoing: '进行中', ended: '已结束' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await request.get('/auction/session/list', { params: { page: pagination.page, size: pagination.size, ...searchForm } })
    tableData.value = data.list
    pagination.total = data.total
  } catch (e) {
    if (!tableData.value.length) {
      tableData.value = [
        { sessionId: 'S001', name: '2024春季艺术品拍卖会', cover: '', previewStart: '2024-02-01 09:00', previewEnd: '2024-02-05 18:00', auctionStart: '2024-02-06 10:00', auctionEnd: '2024-02-06 18:00', status: 'ended', totalAmount: 2580000 },
        { sessionId: 'S002', name: '当代艺术专场', cover: '', previewStart: '2024-03-01 09:00', previewEnd: '2024-03-05 18:00', auctionStart: '2024-03-06 10:00', auctionEnd: '2024-03-06 18:00', status: 'ongoing', totalAmount: 0 },
        { sessionId: 'S003', name: '书画精品专场', cover: '', previewStart: '2024-03-15 09:00', previewEnd: '2024-03-20 18:00', auctionStart: '2024-03-21 10:00', auctionEnd: '2024-03-21 18:00', status: 'preview', totalAmount: 0 }
      ]
      pagination.total = 3
    }
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
  Object.assign(searchForm, { name: '', status: '' })
  handleSearch()
}

const showDialog = (type, row = null) => {
  if (type === 'add') {
    isEdit.value = false
    Object.assign(form, { name: '', cover: '', previewStart: '', previewEnd: '', auctionStart: '', auctionEnd: '', deposit: 1000, description: '' })
  } else {
    isEdit.value = true
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    if (isEdit.value) {
      // 本地更新
      const index = tableData.value.findIndex(item => item.sessionId === form.sessionId)
      if (index > -1) {
        tableData.value[index] = { ...tableData.value[index], ...form }
        ElMessage.success('更新成功')
      }
    } else {
      // 本地添加
      const newId = 'S' + String(Math.max(...tableData.value.map(item => parseInt(item.sessionId.slice(1))), 0) + 1).padStart(3, '0')
      tableData.value.unshift({ sessionId: newId, ...form, status: 'preview', totalAmount: 0 })
      pagination.total++
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const manageLots = (row) => {
  // 跳转到拍品管理页面，并传递专场ID
  router.push({ path: '/auction/lot', query: { sessionId: row.sessionId } })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该专场吗？', '提示', { type: 'warning' })
    // 本地删除
    const index = tableData.value.findIndex(item => item.sessionId === row.sessionId)
    if (index > -1) {
      tableData.value.splice(index, 1)
      pagination.total--
      ElMessage.success('删除成功')
    }
  } catch (e) {}
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
