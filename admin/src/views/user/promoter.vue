<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">艺荐官管理</span>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
        添加艺荐官
      </el-button>
      <el-button type="success" link @click="handleExport" :loading="exportLoading">
        导出CSV
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
    
    <!-- 搜索筛选 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="昵称">
          <el-input v-model="searchForm.keyword" placeholder="请输入昵称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="ID/UID">
          <el-input v-model="searchForm.userId" placeholder="请输入ID或UID" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="searchForm.level" placeholder="全部" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="普通艺荐官" value="1" />
            <el-option label="高级艺荐官" value="2" />
            <el-option label="金牌艺荐官" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 批量操作栏 -->
    <div v-if="selectedRows.length > 0" class="batch-actions">
      <span class="selected-count">已选择 {{ selectedRows.length }} 个艺荐官</span>
      <el-button type="primary" link @click="clearSelection">取消选择</el-button>
      <el-divider direction="vertical" />
      <el-button type="success" @click="handleBatchApprove" :loading="batchLoading">批量通过</el-button>
      <el-button type="danger" @click="showBatchRejectDialog" :loading="batchLoading">批量拒绝</el-button>
      <el-button type="danger" @click="handleBatchDelete" :loading="batchLoading">批量删除</el-button>
    </div>

    <el-table ref="tblRef" :data="tableData" v-loading="loading" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" width="80">
        <template #default="{ row }">
          <span class="id-display" @click="handleCopyId(row.displayId || row.id)">{{ row.displayId || row.id }}</span>
        </template>
      </el-table-column>
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
      <el-table-column prop="level" label="等级" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.level === 3" type="success" size="small">金牌艺荐官</el-tag>
          <el-tag v-else-if="row.level === 2" type="warning" size="small">高级艺荐官</el-tag>
          <el-tag v-else type="info" size="small">普通艺荐官</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="teamCount" label="团队人数" width="100" />
      <el-table-column prop="directCount" label="直接推荐" width="100" />
      <el-table-column label="累计佣金" width="120">
        <template #default="{ row }">¥{{ row.totalCommission || 0 }}</template>
      </el-table-column>
      <el-table-column label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="becomeTime" label="成为时间" width="180">
        <template #default="{ row }">
          {{ row.becomeTime || row.createTime || row.createdAt || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <!-- 待审核状态 -->
          <template v-if="row.status === 0 || row.status === 'pending'">
            <el-button type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
          <!-- 已认证状态 -->
          <template v-else-if="row.status === 1 || row.status === 'approved'">
            <el-button type="warning" link @click="showLevelDialog(row)">设置等级</el-button>
            <el-button type="danger" link @click="handleRevoke(row)">取消认证</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
          <!-- 已拒绝状态 -->
          <template v-else>
            <el-button type="primary" link @click="handleReapply(row)">重新认证</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 批量拒绝原因弹窗 -->
    <el-dialog v-model="batchRejectDialogVisible" title="批量拒绝" width="500px">
      <el-form label-width="100px">
        <el-form-item label="拒绝原因">
          <el-input v-model="batchRejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="batchLoading" @click="confirmBatchReject">确定拒绝</el-button>
      </template>
    </el-dialog>
    
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
            <p class="user-id">UID: {{ currentUser.uid || currentUser.displayId || '-' }}</p>
            <div class="profile-stats">
              <span>上级：{{ formatParent(currentUser) }}</span>
              <span>团队：{{ currentUser.teamCount || 0 }} 人</span>
            </div>
            <div class="identity-tags">
              <el-tag v-if="currentUser.isArtist" type="success" size="small">艺术家</el-tag>
              <el-tag v-if="currentUser.isPromoter || currentUser.certified" type="warning" size="small">艺荐官</el-tag>
              <el-tag v-if="!currentUser.isArtist && !currentUser.isPromoter && !currentUser.certified" type="info" size="small">普通用户</el-tag>
            </div>
          </div>
        </div>
        
        <!-- 编辑表单 -->
        <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px" class="profile-form">
          <el-divider content-position="left">基本信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">团队关系</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="上级UID">
                <el-input v-model="profileForm.inviterUid" placeholder="请输入上级用户UID" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="团队人数">
                <el-input-number v-model="profileForm.teamCount" :min="0" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="info-item">
                <span class="label">直接下级</span>
                <span class="value">{{ currentUser.directCount || 0 }} 人</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="label">上级信息</span>
                <span class="value">{{ formatParent(currentUser) }}</span>
              </div>
            </el-col>
          </el-row>
          
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

          <!-- 销售记录 -->
          <el-divider content-position="left">销售记录 ({{ userSales.total || 0 }})</el-divider>
          <div v-loading="salesLoading" class="sales-section">
            <div v-if="userSales.list && userSales.list.length > 0" class="sales-list">
              <el-table :data="userSales.list" size="small" border>
                <el-table-column prop="orderNo" label="订单号" width="150" />
                <el-table-column prop="amount" label="订单金额" width="100">
                  <template #default="{ row }">¥{{ row.amount || 0 }}</template>
                </el-table-column>
                <el-table-column prop="commission" label="佣金" width="100">
                  <template #default="{ row }">¥{{ row.commission || 0 }}</template>
                </el-table-column>
                <el-table-column prop="artworkId" label="作品ID" width="80" />
                <el-table-column prop="orderTime" label="下单时间" />
              </el-table>
            </div>
            <el-empty v-else description="暂无销售记录" :image-size="60" />
          </div>
          <div v-if="userSales.total > userSales.list?.length" class="load-more">
            <el-button link type="primary" @click="loadMoreSales">加载更多</el-button>
          </div>

          <!-- 分享记录 -->
          <el-divider content-position="left">分享销售记录 ({{ userSharing.total || 0 }})</el-divider>
          <div v-loading="sharingLoading" class="sharing-section">
            <div v-if="userSharing.list && userSharing.list.length > 0" class="sharing-list">
              <el-table :data="userSharing.list" size="small" border>
                <el-table-column prop="artworkId" label="作品ID" width="80" />
                <el-table-column prop="shareCount" label="分享次数" width="100" />
                <el-table-column prop="shareTime" label="最近分享时间" />
              </el-table>
            </div>
            <el-empty v-else description="暂无分享记录" :image-size="60" />
          </div>
          <div v-if="userSharing.total > userSharing.list?.length" class="load-more">
            <el-button link type="primary" @click="loadMoreSharing">加载更多</el-button>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveProfile">保存修改</el-button>
      </template>
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

    <!-- 添加艺荐官弹窗 -->
    <el-dialog v-model="addVisible" title="添加艺荐官" width="600px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
          创建后将同时创建艺荐官认证记录
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
                <el-form-item label="昵称">
                  <el-input v-model="addForm.nickname" placeholder="可选，默认为'用户'+手机号后4位" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
        <el-divider content-position="left">艺荐官信息</el-divider>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="addForm.realName" placeholder="请输入真实姓名（必填）" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号（用于创建用户）" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="支付宝账户">
              <el-input v-model="addForm.alipayAccount" placeholder="请输入支付宝账户" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="微信账户">
              <el-input v-model="addForm.wechatAccount" placeholder="请输入微信账户" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="银行账户">
          <el-input v-model="addForm.bankCard" placeholder="请输入开户行及银行卡号" />
        </el-form-item>
        <el-form-item label="认证等级" prop="level">
          <el-select v-model="addForm.level" placeholder="请选择认证等级">
            <el-option label="普通艺荐官" :value="1" />
            <el-option label="高级艺荐官" :value="2" />
            <el-option label="金牌艺荐官" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="addForm.remark" 
            type="textarea" 
            :rows="3" 
            placeholder="可选，添加备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="confirmAdd">确定添加</el-button>
      </template>
    </el-dialog>

    <!-- 设置等级弹窗 -->
    <el-dialog v-model="levelVisible" title="设置艺荐官等级" width="400px">
      <el-form label-width="100px">
        <el-form-item label="当前用户">
          {{ currentRecord?.nickname || currentRecord?.userNickname }}
        </el-form-item>
        <el-form-item label="认证等级">
          <el-select v-model="selectedLevel" placeholder="请选择认证等级">
            <el-option label="普通艺荐官" :value="1" />
            <el-option label="高级艺荐官" :value="2" />
            <el-option label="金牌艺荐官" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmLevel">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request, { getFullImageUrl, uploadFile } from '@/api/request'
import { copyId } from '@/utils/id'

const loading = ref(false)
const exportLoading = ref(false)
const rejectVisible = ref(false)
const addVisible = ref(false)
const addLoading = ref(false)
const levelVisible = ref(false)
const detailVisible = ref(false)
const editLoading = ref(false)
const currentRecord = ref({})
const currentUser = ref({})
const profileFormRef = ref()
const rejectReason = ref('')

// 批量操作相关变量
const selectedRows = ref([])
const batchLoading = ref(false)
const batchRejectDialogVisible = ref(false)
const batchRejectReason = ref('')

const tblRef = ref()

const selectedLevel = ref(1)
const activeTab = ref('pending')
const searchForm = reactive({
  keyword: '',
  phone: '',
  userId: '',
  level: '',
  dateRange: []
})
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)
const tableData = ref([])
const addFormRef = ref()
const salesLoading = ref(false)
const userSales = ref({ list: [], total: 0 })
const salesPage = ref(1)
const salesSize = 10
const sharingLoading = ref(false)
const userSharing = ref({ list: [], total: 0 })
const sharingPage = ref(1)
const sharingSize = 10

const profileForm = reactive({
  nickname: '',
  realName: '',
  phone: '',
  email: '',
  avatar: '',
  identities: [],
  inviterUid: '',
  teamCount: 0
})

const addForm = reactive({
  phone: '',
  nickname: '',
  avatar: '',
  level: 1,
  remark: '',
  realName: '',
  alipayAccount: '',
  wechatAccount: '',
  bankCard: ''
})

const addRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号，用于创建用户', trigger: 'blur' }]
}

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  if (status === 1 || status === 'approved') return 'success'
  if (status === 0 || status === 'pending') return 'warning'
  if (status === -1 || status === 2 || status === 'rejected') return 'danger'
  return 'info'
}

const getStatusText = (status) => {
  if (status === 1 || status === 'approved') return '已认证'
  if (status === 0 || status === 'pending') return '待审核'
  if (status === -1 || status === 2 || status === 'rejected') return '已拒绝'
  return status
}

const getSourceText = (source) => {
  const map = { wechat: '微信', ios: 'iOS', android: 'Android', web: 'Web', '': '-' }
  return map[source] || source || '-'
}

const formatParent = (user) => {
  if (!user) return '无'
  const uid = user.parentUid || user.inviterUid
  const name = user.parentName
  if (!uid && !name) return '无'
  return name ? `${name} (${uid || '-'})` : uid
}

// 复制ID
const handleCopyId = (id) => {
  if (!id) {
    ElMessage.warning('ID为空')
    return
  }
  copyId(id,
    () => ElMessage.success('已复制ID'),
    () => ElMessage.error('复制失败')
  )
}

// 打开用户资料弹窗
const openUserProfile = async (row) => {
  const userId = row.userId || row.id
  currentUser.value = { 
    ...row,
    userId,
    isPromoter: row.certified || row.status === 1,
    isArtist: false
  }
  
  try {
    const detail = await request.get(`/user/${userId}`)
    if (detail) {
      currentUser.value = { ...currentUser.value, ...detail }
    }
  } catch (e) {}
  
  Object.assign(profileForm, {
    nickname: row.nickname || row.userNickname || '',
    realName: row.realName || row.nickname || row.userNickname || '',
    phone: row.phone || row.userPhone || '',
    email: row.email || '',
    avatar: row.avatar || row.userAvatar || '',
    inviterUid: row.inviterUid || row.parentUid || '',
    teamCount: row.teamCount || 0
  })

  // 加载销售记录
  salesPage.value = 1
  userSales.value = { list: [], total: 0 }
  await loadUserSales(userId)

  // 加载分享记录
  sharingPage.value = 1
  userSharing.value = { list: [], total: 0 }
  await loadUserSharing(userId)

  detailVisible.value = true
}

// 加载用户销售记录
const loadUserSales = async (userId) => {
  salesLoading.value = true
  try {
    const res = await request.get(`/user/${userId}/sales`, {
      params: { page: salesPage.value, size: salesSize }
    })
    if (salesPage.value === 1) {
      userSales.value = { list: res.list || [], total: res.total || 0 }
    } else {
      userSales.value.list = [...userSales.value.list, ...(res.list || [])]
    }
  } catch (e) {
    console.error('加载销售记录失败', e)
  } finally {
    salesLoading.value = false
  }
}

// 加载更多销售记录
const loadMoreSales = () => {
  const userId = currentUser.value.userId
  if (!userId) return
  salesPage.value++
  loadUserSales(userId)
}

// 加载用户分享记录
const loadUserSharing = async (userId) => {
  sharingLoading.value = true
  try {
    const res = await request.get(`/user/${userId}/sharing`, {
      params: { page: sharingPage.value, size: sharingSize }
    })
    if (sharingPage.value === 1) {
      userSharing.value = { list: res.list || [], total: res.total || 0 }
    } else {
      userSharing.value.list = [...userSharing.value.list, ...(res.list || [])]
    }
  } catch (e) {
    console.error('加载分享记录失败', e)
  } finally {
    sharingLoading.value = false
  }
}

// 加载更多分享记录
const loadMoreSharing = () => {
  const userId = currentUser.value.userId
  if (!userId) return
  sharingPage.value++
  loadUserSharing(userId)
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
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    editLoading.value = true
    const userId = currentUser.value.userId || currentUser.value.id
    await request.put(`/user/${userId}`, {
      nickname: profileForm.realName,
      avatar: profileForm.avatar,
      phone: profileForm.phone,
      email: profileForm.email
    })
    await request.put(`/user/promoter/${userId}/relation`, {
      realName: profileForm.realName,
      inviterUid: profileForm.inviterUid,
      teamCount: profileForm.teamCount
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

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    phone: '',
    userId: '',
    level: '',
    dateRange: []
  })
  pagination.page = 1
  loadData()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const params = { 
      page: 1, size: 10000,
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      keyword: searchForm.keyword || undefined,
      phone: searchForm.phone || undefined,
      userId: searchForm.userId || undefined,
      level: searchForm.level || undefined,
      startDate: searchForm.dateRange && searchForm.dateRange[0] || undefined,
      endDate: searchForm.dateRange && searchForm.dateRange[1] || undefined
    }
    const data = await request.get('/user/promoter/list', { params })
    const list = data.records || data.list || []
    if (list.length === 0) { ElMessage.warning('没有数据可以导出'); return }
    
    const headers = ['ID', '昵称', '手机号', '等级', '团队人数', '直接推荐', '累计佣金', '认证状态', '成为时间']
    const rows = list.map(item => [
      item.displayId || item.id || '',
      item.nickname || item.userNickname || '',
      item.phone || item.userPhone || '',
      item.level === 3 ? '金牌艺荐官' : item.level === 2 ? '高级艺荐官' : '普通艺荐官',
      item.teamCount || 0,
      item.directCount || 0,
      item.totalCommission || 0,
      item.status === 1 ? '已认证' : item.status === 0 ? '待审核' : item.status === -1 ? '已拒绝' : '未知',
      item.becomeTime || item.createTime || item.createdAt || ''
    ])
    
    let csvContent = headers.join(',') + '\n'
    rows.forEach(row => { csvContent += row.map(cell => `"${cell}"`).join(',') + '\n' })
    
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `艺荐官列表_${new Date().toISOString().split('T')[0]}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success(`成功导出 ${list.length} 条数据`)
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || '未知错误'))
  } finally {
    exportLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
  const params = { 
      page: pagination.page, 
      size: pagination.size,
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      keyword: searchForm.keyword || undefined,
      phone: searchForm.phone || undefined,
      userId: searchForm.userId || undefined,
      level: searchForm.level || undefined,
      startDate: searchForm.dateRange && searchForm.dateRange[0] || undefined,
      endDate: searchForm.dateRange && searchForm.dateRange[1] || undefined
    }
  const data = await request.get('/user/promoter/list', { params })
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

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该艺荐官认证吗？', '提示', { type: 'success' })
    await request.post('/user/promoter/approve', { id: row.id })
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
    await request.post('/user/promoter/reject', { id: currentRecord.value.id, reason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    await loadData()
  }
}

// 批量操作相关方法
const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tblRef.value?.clearSelection()
}

const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(`确定批量通过 ${selectedRows.value.length} 个艺荐官吗？`, '提示', { type: 'success' })
    const ids = selectedRows.value.map(row => row.id)
    await request.post('/user/promoter/batchApprove', { ids })
    ElMessage.success('批量通过成功')
    clearSelection()
    await loadData()
  } catch (e) {}
}

const showBatchRejectDialog = () => {
  batchRejectReason.value = ''
  batchRejectDialogVisible.value = true
}

const confirmBatchReject = async () => {
  if (!batchRejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    const ids = selectedRows.value.map(row => row.id)
    await request.post('/user/promoter/batchReject', { ids, reason: batchRejectReason.value })
    ElMessage.success('批量拒绝成功')
    batchRejectDialogVisible.value = false
    clearSelection()
    await loadData()
  } catch (e) {}
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定批量删除 ${selectedRows.value.length} 个艺荐官吗？`, '删除确认', { type: 'warning' })
    const ids = selectedRows.value.map(row => row.id)
    await request.post('/user/promoter/batchDelete', { ids })
    ElMessage.success('批量删除成功')
    clearSelection()
    await loadData()
  } catch (e) {}
}

// 取消认证
const handleRevoke = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该艺荐官认证吗？', '提示', { type: 'warning' })
    await request.post('/user/promoter/revoke', { id: row.id })
    ElMessage.success('已取消认证')
    await loadData()
  } catch (e) {}
}

// 重新认证
const handleReapply = async (row) => {
  try {
    await ElMessageBox.confirm('确定重新发起认证吗？', '提示', { type: 'info' })
    await request.post('/user/promoter/reapply', { id: row.id })
    ElMessage.success('已重新发起认证')
    await loadData()
  } catch (e) {}
}

// 删除艺荐官认证
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除艺荐官"${row.nickname || row.userNickname}"的认证记录吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/user/promoter/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 添加艺荐官
const showAddDialog = () => {
  Object.assign(addForm, { 
    phone: '', 
    nickname: '', 
    avatar: '', 
    level: 1, 
    remark: '',
    realName: '',
    alipayAccount: '',
    wechatAccount: '',
    bankCard: ''
  })
  addVisible.value = true
}

// 添加艺荐官 - 上传头像
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
    const result = await request.post('/user/promoter/add', addForm)
    const msg = result.message || (result.isNewUser ? `新用户已创建，用户ID：${result.userId}` : `用户ID：${result.userId}`)
    ElMessage.success({ message: '添加成功！' + msg, duration: 5000 })
    addVisible.value = false
    await loadData()
  } catch (e) {}
}

// 设置等级
const showLevelDialog = (row) => {
  currentRecord.value = row
  selectedLevel.value = row.level || 1
  levelVisible.value = true
}

const confirmLevel = async () => {
  try {
    await request.post('/user/promoter/level', { 
      id: currentRecord.value.id, 
      level: selectedLevel.value 
    })
    ElMessage.success('等级设置成功')
    levelVisible.value = false
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
  
  .el-avatar {
    flex-shrink: 0;
    border-radius: 50%;
    overflow: hidden;
  }
}
.user-detail {
  flex: 1;
  min-width: 0;
  
  .nickname {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .phone {
    font-size: 12px;
    color: #999;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
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

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.clickable-avatar:hover {
  transform: scale(1.05);
}

/* 4位数ID显示样式 */
.id-display {
  font-family: 'Consolas', 'Monaco', monospace;
  font-weight: 600;
  color: #409eff;
  letter-spacing: 1px;
  font-size: 11px;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.sales-section, .sharing-section {
  min-height: 80px;
  padding: 10px 0;
}

.sales-list, .sharing-list {
  margin-bottom: 10px;
}

.load-more {
  text-align: center;
  padding: 10px 0;
}
</style>
