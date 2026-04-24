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
            <el-avatar :src="getFullImageUrl(row.avatar || row.userAvatar)" :size="50" fit="cover" class="clickable-avatar" @click="openUserProfile(row)" />
            <div class="user-detail">
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
      <el-table-column prop="idCard" label="身份证号" width="180">
        <template #default="{ row }">
          {{ row.idCard || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130">
        <template #default="{ row }">
          {{ row.phone || row.userPhone || '-' }}
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
      <el-table-column prop="resume" label="艺术家简介" min-width="200">
        <template #default="{ row }">
          <span v-if="row.resume || row.bio" class="resume-text" :title="row.resume || row.bio">
            {{ (row.resume || row.bio).substring(0, 30) }}{{ (row.resume || row.bio).length > 30 ? '...' : '' }}
          </span>
          <span v-else>-</span>
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
    
    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="600px" destroy-on-close>
      <div class="user-profile" v-if="currentUser">
        <!-- 用户基本信息 -->
        <div class="profile-header">
          <div class="avatar-wrapper">
            <el-avatar :src="getFullImageUrl(profileForm.avatar)" :size="80" fit="cover" />
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary">更换头像</el-button>
            </el-upload>
          </div>
          <div class="profile-info">
            <h3>{{ profileForm.nickname || currentUser.nickname || currentUser.userNickname || '未知用户' }}
              <el-tag v-if="currentUser.isVip" type="warning" size="small">VIP</el-tag>
            </h3>
            <p class="user-id">ID: {{ currentUser.userId || currentUser.id }}</p>
            <div class="identity-tags">
              <el-tag v-if="currentUser.isArtist" type="success" size="small">艺术家</el-tag>
              <el-tag v-if="currentUser.isPromoter" type="warning" size="small">艺荐官</el-tag>
              <el-tag v-if="!currentUser.isArtist && !currentUser.isPromoter" type="info" size="small">普通用户</el-tag>
            </div>
          </div>
        </div>
        
        <!-- 编辑表单 -->
        <el-form ref="profileFormRef" :model="profileForm" label-width="90px" class="profile-form">
          <el-divider content-position="left">基本信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          
          <el-divider content-position="left">艺术家信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="真实姓名">
                <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="身份证号">
                <el-input v-model="profileForm.idCard" placeholder="请输入身份证号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="艺术家简介">
            <el-input 
              v-model="profileForm.resume" 
              type="textarea" 
              :rows="2" 
              placeholder="请输入艺术家简介"
            />
          </el-form-item>
          
          <el-divider content-position="left">身份配置</el-divider>
          <el-form-item label="身份">
            <el-checkbox-group v-model="profileForm.identities">
              <el-checkbox label="artist">艺术家</el-checkbox>
              <el-checkbox label="promoter">艺荐官</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          
          <el-divider content-position="left">账户信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="info-item">
                <span class="label">账户余额</span>
                <span class="value">¥{{ currentUser.balance || 0 }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="label">累计消费</span>
                <span class="value">¥{{ currentUser.totalConsume || 0 }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="label">订单数量</span>
                <span class="value">{{ currentUser.orderCount || 0 }}</span>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="info-item">
                <span class="label">注册时间</span>
                <span class="value">{{ currentUser.registerTime || currentUser.createTime || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="label">注册来源</span>
                <span class="value">{{ getSourceText(currentUser.source) }}</span>
              </div>
            </el-col>
          </el-row>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveProfile">保存修改</el-button>
      </template>
    </el-dialog>
    
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
    <el-dialog v-model="addVisible" title="添加艺术家" width="600px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
          创建后将同时创建艺术家认证记录
        </el-alert>
        <div class="add-form-header">
          <div class="avatar-section">
            <el-avatar :src="getFullImageUrl(addForm.avatar)" :size="80" fit="cover" />
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleAddAvatarUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary">上传头像</el-button>
            </el-upload>
          </div>
          <div class="add-form-main">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="addForm.phone" placeholder="请输入用户手机号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="昵称">
                  <el-input v-model="addForm.nickname" placeholder="可选，默认为'用户'+手机号后4位" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
        <el-divider content-position="left">艺术家信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="addForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="addForm.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="认证等级" prop="badge">
          <el-select v-model="addForm.badge" placeholder="请选择认证等级">
            <el-option label="普通认证" value="" />
            <el-option label="认证艺术家" value="verified" />
            <el-option label="人气艺术家" value="popular" />
            <el-option label="大师级" value="master" />
          </el-select>
        </el-form-item>
        <el-form-item label="艺术家简介">
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
        <el-button type="primary" :loading="addLoading" @click="confirmAdd">确定添加</el-button>
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
import request, { getFullImageUrl, uploadFile } from '@/api/request'

const loading = ref(false)
const materialsVisible = ref(false)
const rejectVisible = ref(false)
const addVisible = ref(false)
const addLoading = ref(false)
const badgeVisible = ref(false)
const detailVisible = ref(false)
const editLoading = ref(false)
const currentRecord = ref({})
const currentUser = ref({})
const profileFormRef = ref()
const rejectReason = ref('')
const selectedBadge = ref('')
const activeTab = ref('pending')
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)
const tableData = ref([])
const addFormRef = ref()

const profileForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  identities: [],
  realName: '',
  idCard: '',
  resume: ''
})

const addForm = reactive({
  phone: '',
  nickname: '',
  avatar: '',
  realName: '',
  idCard: '',
  badge: '',
  resume: ''
})

const addRules = {
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

const getSourceText = (source) => {
  const map = { wechat: '微信', ios: 'iOS', android: 'Android', web: 'Web', '': '-' }
  return map[source] || source || '-'
}

// 打开用户资料弹窗
const openUserProfile = async (row) => {
  // 从行数据获取用户信息
  const userId = row.userId || row.id
  currentUser.value = { 
    ...row,
    userId,
    isArtist: row.certified || row.status === 1,
    isPromoter: false
  }
  
  // 获取完整用户信息
  try {
    const detail = await request.get(`/user/${userId}`)
    if (detail) {
      currentUser.value = { ...currentUser.value, ...detail }
    }
  } catch (e) {
    // 使用行数据
  }
  
  // 设置表单数据
  Object.assign(profileForm, {
    nickname: row.nickname || row.userNickname || '',
    phone: row.phone || row.userPhone || '',
    email: row.email || '',
    avatar: row.avatar || row.userAvatar || '',
    identities: row.certified ? ['artist'] : [],
    realName: row.realName || '',
    idCard: row.idCard || '',
    resume: row.resume || row.bio || ''
  })
  detailVisible.value = true
}

// 上传头像
const handleAvatarUpload = async (options) => {
  const { file, onSuccess, onError } = options
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    onError(new Error('请选择图片文件'))
    return
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    onError(new Error('图片大小不能超过 10MB'))
    return
  }

  try {
    const result = await uploadFile(file)
    profileForm.avatar = result?.url || result || ''
    ElMessage.success('头像上传成功')
    onSuccess()
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
    onError(e)
  }
}

// 保存用户资料
const saveProfile = async () => {
  try {
    editLoading.value = true
    const userId = currentUser.value.userId || currentUser.value.id
    await request.put(`/user/${userId}`, {
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      phone: profileForm.phone,
      email: profileForm.email,
      identities: profileForm.identities,
      realName: profileForm.realName,
      idCard: profileForm.idCard,
      resume: profileForm.resume
    })
    detailVisible.value = false
    ElMessage.success('保存成功')
    await loadData()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    editLoading.value = false
  }
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
    pendingCount.value = data.pendingCount || 0
    approvedCount.value = data.approvedCount || 0
    rejectedCount.value = data.rejectedCount || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    pendingCount.value = 0
    approvedCount.value = 0
    rejectedCount.value = 0
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
    await loadData()
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
    await loadData()
  } catch (e) {}
}

// 取消认证
const handleRevoke = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该艺术家认证吗？', '提示', { type: 'warning' })
    await request.post('/user/artist/revoke', { id: row.id })
    ElMessage.success('已取消认证')
    await loadData()
  } catch (e) {}
}

// 重新认证
const handleReapply = async (row) => {
  try {
    await ElMessageBox.confirm('确定重新发起认证吗？', '提示', { type: 'info' })
    await request.post('/user/artist/reapply', { id: row.id })
    ElMessage.success('已重新发起认证')
    await loadData()
  } catch (e) {}
}

// 添加艺术家
const showAddDialog = () => {
  Object.assign(addForm, { phone: '', nickname: '', avatar: '', realName: '', idCard: '', badge: '', resume: '' })
  addVisible.value = true
}

// 添加艺术家 - 上传头像
const handleAddAvatarUpload = async (options) => {
  const { file, onSuccess, onError } = options
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    onError(new Error('请选择图片文件'))
    return
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    onError(new Error('图片大小不能超过 10MB'))
    return
  }

  try {
    const result = await uploadFile(file)
    addForm.avatar = result?.url || result || ''
    ElMessage.success('头像上传成功')
    onSuccess()
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
    onError(e)
  }
}

const confirmAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    const result = await request.post('/user/artist/add', addForm)
    const msg = result.message || (result.isNewUser ? `新用户已创建，用户ID：${result.userId}` : `用户ID：${result.userId}`)
    ElMessage.success({ message: '添加成功！' + msg, duration: 5000 })
    addVisible.value = false
    await loadData()
  } catch (e) {}
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
    await loadData()
  } catch (e) {}
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

/* 用户详情弹窗样式 */
.user-profile {
  .profile-header {
    display: flex;
    gap: 20px;
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #eee;
  }
  
  .avatar-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }
  
  .avatar-uploader {
    display: block;
  }
  
  .profile-info {
    flex: 1;
    
    h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .user-id {
      margin: 0 0 8px 0;
      font-size: 12px;
      color: #999;
    }
  }
  
  .profile-form {
    .info-item {
      .label {
        display: block;
        font-size: 12px;
        color: #999;
        margin-bottom: 4px;
      }
      .value {
        font-size: 14px;
        color: #333;
      }
    }
  }
}

.identity-tags {
  display: flex;
  gap: 4px;
}

.add-form-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 20px;
  
  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    
    .avatar-uploader {
      display: block;
    }
  }
  
  .add-form-main {
    flex: 1;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-detail {
  flex: 1;
}

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.clickable-avatar:hover {
  transform: scale(1.05);
}

.resume-text {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
