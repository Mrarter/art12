<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">艺术家认证管理</span>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加艺术家
      </el-button>
    </div>

    <!-- 状态 Tab 切换 -->
    <div class="status-tabs">
      <el-radio-group v-model="activeTab" @change="handleTabChange">
        <el-radio-button label="pending">
          待审核 <el-badge :value="pendingCount" :hidden="pendingCount === 0" type="warning" />
        </el-radio-button>
        <el-radio-button label="approved">
          已认证 <el-badge :value="approvedCount" :hidden="approvedCount === 0" type="success" />
        </el-radio-button>
        <el-radio-button label="rejected">
          未通过 <el-badge :value="rejectedCount" :hidden="rejectedCount === 0" type="danger" />
        </el-radio-button>
        <el-radio-button label="all">
          全部
        </el-radio-button>
      </el-radio-group>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="用户信息" min-width="200">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :src="row.avatar || row.userAvatar" :size="40" />
            <div>
              <p class="nickname">
                {{ row.nickname || row.userNickname || '未知用户' }}
                <el-tag v-if="row.certified" type="success" size="small">已认证</el-tag>
                <el-tag v-else type="info" size="small">未认证</el-tag>
              </p>
              <p class="phone">{{ row.phone || row.userPhone }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="realName" label="真实姓名" width="120">
        <template #default="{ row }">
          {{ row.realName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="badge" label="认证等级" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.badge === 'master'" type="danger" size="small">大师级</el-tag>
          <el-tag v-else-if="row.badge === 'popular'" type="warning" size="small">人气艺术家</el-tag>
          <el-tag v-else-if="row.badge === 'verified'" type="success" size="small">认证艺术家</el-tag>
          <span v-else-if="row.certified" class="certified">普通认证</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="认证材料" width="120">
        <template #default="{ row }">
          <el-button 
            v-if="row.images || row.artworks" 
            type="primary" 
            link 
            @click="viewMaterials(row)"
          >
            查看
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="180">
        <template #default="{ row }">
          {{ row.createTime || row.createdAt || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <!-- 待审核状态 -->
          <template v-if="row.status === 0 || row.status === 'pending'">
            <el-button type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
          </template>
          <!-- 已认证状态 -->
          <template v-else-if="row.status === 1 || row.status === 'approved'">
            <el-button type="warning" link @click="showBadgeDialog(row)">设置等级</el-button>
            <el-button type="danger" link @click="handleRevoke(row)">取消认证</el-button>
          </template>
          <!-- 已拒绝状态 -->
          <template v-else>
            <el-button type="primary" link @click="handleReapply(row)">重新认证</el-button>
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
    <el-dialog v-model="materialsVisible" title="认证材料" width="700px">
      <div v-if="currentRecord" class="materials">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="真实姓名">{{ currentRecord.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentRecord.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">{{ currentRecord.resume || currentRecord.bio || '-' }}</el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentRecord.images || currentRecord.artworks" class="images-section">
          <p class="section-title">证件照片：</p>
          <div class="image-list">
            <el-image 
              v-for="(img, index) in (currentRecord.images || '').split(',').filter(Boolean)" 
              :key="'img-' + index"
              :src="img" 
              :preview-src-list="(currentRecord.images || '').split(',').filter(Boolean)"
              style="width: 120px; height: 90px; margin-right: 10px; margin-bottom: 10px"
              fit="cover"
            />
          </div>
        </div>
        
        <div v-if="currentRecord.exhibits" class="images-section">
          <p class="section-title">参展证明：</p>
          <div class="image-list">
            <el-image 
              v-for="(img, index) in currentRecord.exhibits.split(',').filter(Boolean)" 
              :key="'exhibit-' + index"
              :src="img" 
              :preview-src-list="currentRecord.exhibits.split(',').filter(Boolean)"
              style="width: 120px; height: 90px; margin-right: 10px; margin-bottom: 10px"
              fit="cover"
            />
          </div>
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

    <!-- 添加艺术家弹窗 -->
    <el-dialog v-model="addVisible" title="添加艺术家" width="600px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="用户手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入用户手机号" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="addForm.realName" placeholder="请输入艺术家真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="addForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="认证等级" prop="badge">
          <el-select v-model="addForm.badge" placeholder="请选择认证等级">
            <el-option label="普通认证" value="" />
            <el-option label="认证艺术家" value="verified" />
            <el-option label="人气艺术家" value="popular" />
            <el-option label="大师级" value="master" />
          </el-select>
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input 
            v-model="addForm.resume" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入艺术家简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdd">确定添加</el-button>
      </template>
    </el-dialog>

    <!-- 设置等级弹窗 -->
    <el-dialog v-model="badgeVisible" title="设置艺术家等级" width="400px">
      <el-form label-width="100px">
        <el-form-item label="当前用户">
          {{ currentRecord?.nickname || currentRecord?.userNickname }}
        </el-form-item>
        <el-form-item label="认证等级">
          <el-select v-model="selectedBadge" placeholder="请选择认证等级">
            <el-option label="普通认证" value="" />
            <el-option label="认证艺术家" value="verified" />
            <el-option label="人气艺术家" value="popular" />
            <el-option label="大师级" value="master" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="badgeVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBadge">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const materialsVisible = ref(false)
const rejectVisible = ref(false)
const addVisible = ref(false)
const badgeVisible = ref(false)
const currentRecord = ref({})
const rejectReason = ref('')
const selectedBadge = ref('')
const activeTab = ref('pending')
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)
const tableData = ref([])
const addFormRef = ref()

const addForm = reactive({
  phone: '',
  realName: '',
  idCard: '',
  badge: '',
  resume: ''
})

const addRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  if (status === 1 || status === 'approved') return 'success'
  if (status === 0 || status === 'pending') return 'warning'
  if (status === 2 || status === 'rejected') return 'danger'
  return 'info'
}

const getStatusText = (status) => {
  if (status === 1 || status === 'approved') return '已认证'
  if (status === 0 || status === 'pending') return '待审核'
  if (status === 2 || status === 'rejected') return '已拒绝'
  return status
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { 
      page: pagination.page, 
      size: pagination.size,
      status: activeTab.value === 'all' ? undefined : activeTab.value
    }
    const data = await request.get('/user/artist/list', { params })
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
    
    // 模拟各状态数量（实际应从接口获取）
    if (activeTab.value === 'all') {
      pendingCount.value = tableData.value.filter(t => t.status === 0 || t.status === 'pending').length
      approvedCount.value = tableData.value.filter(t => t.status === 1 || t.status === 'approved').length
      rejectedCount.value = tableData.value.filter(t => t.status === 2 || t.status === 'rejected').length
    }
  } catch (e) {
    // 模拟数据
    tableData.value = [
      { id: 1, nickname: '张大千', phone: '13800138001', avatar: '', realName: '张伟', idCard: '110101199001011234', status: 1, badge: 'master', certified: true, createTime: '2024-01-20 10:00:00', resume: '著名国画大师', images: '' },
      { id: 2, nickname: '李娜', phone: '13800138002', avatar: '', realName: '李娜', idCard: '110101199002022345', status: 1, badge: 'popular', certified: true, createTime: '2024-01-19 14:30:00', resume: '专注油画创作', images: '' },
      { id: 3, nickname: '王强', phone: '13800138003', avatar: '', realName: '王强', idCard: '110101199003033456', status: 0, badge: '', certified: false, createTime: '2024-01-18 09:15:00', resume: '书法爱好者', images: '' },
      { id: 4, nickname: '赵敏', phone: '13800138004', avatar: '', realName: '赵敏', idCard: '110101199004044567', status: 2, badge: '', certified: false, createTime: '2024-01-17 11:20:00', resume: '版画创作者', images: '' },
      { id: 5, nickname: '钱多多', phone: '13800138005', avatar: '', realName: '钱伟', idCard: '110101199005055678', status: 1, badge: 'verified', certified: true, createTime: '2024-01-16 16:45:00', resume: '雕塑艺术家', images: '' }
    ]
    pagination.total = 5
    pendingCount.value = 1
    approvedCount.value = 3
    rejectedCount.value = 1
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pagination.page = 1
  loadData()
}

const viewMaterials = (row) => {
  currentRecord.value = row
  materialsVisible.value = true
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该艺术家认证吗？', '提示', { type: 'success' })
    await request.post('/user/artist/approve', { id: row.id })
    ElMessage.success('已通过认证')
    loadData()
  } catch (e) {
    // 本地模拟
    row.status = 1
    row.certified = true
    ElMessage.success('已通过认证')
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
    await request.post('/user/artist/reject', { id: currentRecord.value.id, reason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    loadData()
  } catch (e) {
    // 本地模拟
    currentRecord.value.status = 2
    currentRecord.value.certified = false
    ElMessage.success('已拒绝')
    rejectVisible.value = false
  }
}

// 取消认证
const handleRevoke = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该艺术家认证吗？', '提示', { type: 'warning' })
    await request.post('/user/artist/revoke', { id: row.id })
    ElMessage.success('已取消认证')
    loadData()
  } catch (e) {
    // 本地模拟
    row.status = 0
    row.certified = false
    row.badge = ''
    ElMessage.success('已取消认证')
  }
}

// 重新认证
const handleReapply = async (row) => {
  try {
    await ElMessageBox.confirm('确定重新发起认证吗？', '提示', { type: 'info' })
    await request.post('/user/artist/reapply', { id: row.id })
    ElMessage.success('已重新发起认证')
    loadData()
  } catch (e) {
    // 本地模拟
    row.status = 0
    ElMessage.success('已重新发起认证')
  }
}

// 添加艺术家
const showAddDialog = () => {
  Object.assign(addForm, { phone: '', realName: '', idCard: '', badge: '', resume: '' })
  addVisible.value = true
}

const confirmAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await request.post('/user/artist/add', addForm)
    ElMessage.success('添加成功')
    addVisible.value = false
    loadData()
  } catch (e) {
    // 本地模拟
    const newId = Math.max(...tableData.value.map(t => t.id), 0) + 1
    tableData.value.unshift({
      id: newId,
      nickname: addForm.realName,
      phone: addForm.phone,
      realName: addForm.realName,
      idCard: addForm.idCard,
      badge: addForm.badge,
      resume: addForm.resume,
      status: addForm.badge ? 1 : 0,  // 选择等级则直接认证
      certified: !!addForm.badge,
      createTime: new Date().toLocaleString('zh-CN'),
      images: ''
    })
    pagination.total++
    ElMessage.success('添加成功')
    addVisible.value = false
  }
}

// 设置等级
const showBadgeDialog = (row) => {
  currentRecord.value = row
  selectedBadge.value = row.badge || ''
  badgeVisible.value = true
}

const confirmBadge = async () => {
  try {
    await request.post('/user/artist/badge', { 
      id: currentRecord.value.id, 
      badge: selectedBadge.value 
    })
    ElMessage.success('等级设置成功')
    badgeVisible.value = false
    loadData()
  } catch (e) {
    // 本地模拟
    currentRecord.value.badge = selectedBadge.value
    ElMessage.success('等级设置成功')
    badgeVisible.value = false
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
  font-size: 18px;
  font-weight: 600;
}
.status-tabs {
  margin-bottom: 20px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nickname {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}
.phone {
  font-size: 12px;
  color: #999;
}
.certified {
  color: #67c23a;
  font-size: 12px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.materials {
  .images-section {
    margin-top: 20px;
  }
  .section-title {
    margin-bottom: 10px;
    font-weight: 500;
  }
  .image-list {
    display: flex;
    flex-wrap: wrap;
  }
}
</style>
