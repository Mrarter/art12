<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">艺术家认证</span>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="用户信息" min-width="200">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :src="row.avatar" :size="40" />
            <div>
              <p>{{ row.nickname }}</p>
              <p class="phone">{{ row.phone }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="idCard" label="身份证号" width="180" />
      <el-table-column label="认证材料" width="120">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewMaterials(row)">查看</el-button>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'pending'">
            <el-button type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
          </template>
          <template v-else>
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          </template>
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
    
    <!-- 认证材料弹窗 -->
    <el-dialog v-model="materialsVisible" title="认证材料" width="600px">
      <div v-if="currentRecord" class="materials">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="真实姓名">{{ currentRecord.realName }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentRecord.idCard }}</el-descriptions-item>
          <el-descriptions-item label="个人简介">{{ currentRecord.bio }}</el-descriptions-item>
        </el-descriptions>
        <div class="images">
          <p class="label">证件照片：</p>
          <el-image 
            v-for="(img, index) in currentRecord.images" 
            :key="index"
            :src="img" 
            :preview-src-list="currentRecord.images"
            style="width: 200px; height: 150px; margin-right: 10px"
            fit="cover"
          />
        </div>
      </div>
    </el-dialog>
    
    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝原因" width="500px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input 
            v-model="rejectReason" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const materialsVisible = ref(false)
const rejectVisible = ref(false)
const currentRecord = ref({})
const rejectReason = ref('')
const tableData = ref([])

const searchForm = reactive({
  status: ''
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
  const map = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.status) params.status = searchForm.status
    const data = await request.get('/user/artist/list', { params })
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    tableData.value = [
      { id: 1, nickname: '艺术家A', phone: '13800138001', avatar: '', realName: '张伟', idCard: '110101199001011234', status: 'pending', createTime: '2024-01-20 10:00:00', bio: '毕业于中央美术学院', images: [] },
      { id: 2, nickname: '艺术家B', phone: '13800138002', avatar: '', realName: '李娜', idCard: '110101199002022345', status: 'approved', createTime: '2024-01-19 14:30:00', bio: '专注油画创作10年', images: [] },
      { id: 3, nickname: '艺术家C', phone: '13800138003', avatar: '', realName: '王强', idCard: '110101199003033456', status: 'rejected', createTime: '2024-01-18 09:15:00', bio: '书法爱好者', images: [] }
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
  searchForm.status = ''
  handleSearch()
}

const viewMaterials = (row) => {
  currentRecord.value = row
  materialsVisible.value = true
}

const viewDetail = (row) => {
  currentRecord.value = row
  materialsVisible.value = true
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该艺术家认证吗？', '提示', { type: 'success' })
    await request.post('/user/artist/approve', { id: row.id })
    ElMessage.success('已通过认证')
    loadData()
  } catch (e) {}
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
    await request.post('/user/artist/reject', { id: currentRecord.value.id, reason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    loadData()
  } catch (e) {}
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .phone {
    font-size: 12px;
    color: #999;
  }
}

.materials {
  .images {
    margin-top: 20px;
    
    .label {
      margin-bottom: 10px;
      font-weight: 500;
    }
  }
}
</style>
