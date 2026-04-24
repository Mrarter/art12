<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">用户管理</span>
      <div class="header-actions">
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          添加用户
        </el-button>
        <el-button type="primary" @click="exportData">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
    </div>

    <!-- 筛选表单 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="searchForm.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="searchForm.identity" placeholder="全部" clearable>
            <el-option label="普通用户" value="user" />
            <el-option label="艺术家" value="artist" />
            <el-option label="艺荐官" value="promoter" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 用户统计 -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-label">总用户数</span>
        <span class="stat-value">{{ stats.total }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">艺术家</span>
        <span class="stat-value">{{ stats.artist }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">艺荐官</span>
        <span class="stat-value">{{ stats.promoter }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">今日新增</span>
        <span class="stat-value">{{ stats.todayNew }}</span>
      </div>
    </div>

    <!-- 用户列表 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column label="用户信息" min-width="220">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :src="row.avatar" :size="50" class="clickable-avatar" @click="copyId(row)" />
            <div class="user-detail">
              <p class="nickname">
                {{ row.nickname }}
                <el-tag v-if="row.isVip" type="warning" size="small">VIP</el-tag>
              </p>
              <p class="user-id" :title="row.openId || row.userId">ID: {{ row.openId ? (row.openId.slice(0, 16) + '...') : row.userId }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="联系方式" width="160">
        <template #default="{ row }">
          <p class="contact-item">
            <el-icon><Phone /></el-icon>
            <span @click="copyText(row.phone, '手机号')" class="clickable-text">{{ row.phone || '-' }}</span>
          </p>
          <p class="contact-item" v-if="row.email">
            <el-icon><Message /></el-icon>
            <span @click="copyText(row.email, '邮箱')" class="clickable-text">{{ row.email }}</span>
          </p>
        </template>
      </el-table-column>
      <el-table-column label="身份" width="140">
        <template #default="{ row }">
          <div class="identity-tags">
            <el-tag v-if="row.isArtist" type="success" size="small">艺术家</el-tag>
            <el-tag v-if="row.isPromoter" type="warning" size="small">艺荐官</el-tag>
            <el-tag v-if="!row.isArtist && !row.isPromoter" type="info" size="small">普通用户</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="资产" width="140">
        <template #default="{ row }">
          <p class="balance">¥{{ row.balance || 0 }}</p>
          <p class="coupon" v-if="row.couponCount">优惠券 {{ row.couponCount }} 张</p>
        </template>
      </el-table-column>
      <el-table-column label="消费" width="120">
        <template #default="{ row }">
          <p class="consume">¥{{ row.totalConsume || 0 }}</p>
          <p class="order-count">{{ row.orderCount || 0 }} 笔订单</p>
        </template>
      </el-table-column>
      <el-table-column label="注册信息" width="160">
        <template #default="{ row }">
          <p>{{ row.registerTime }}</p>
          <p class="source">{{ getSourceText(row.source) }}</p>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'normal' ? 'success' : 'danger'" size="small">
            {{ row.status === 'normal' ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          <el-button type="primary" link @click="editUser(row)">编辑</el-button>
          <el-button :type="row.status === 'normal' ? 'danger' : 'success'" link @click="toggleStatus(row)">
            {{ row.status === 'normal' ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="800px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.userId }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ currentUser.gender === 'male' ? '男' : currentUser.gender === 'female' ? '女' : '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="生日">{{ currentUser.birthday || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ currentUser.registerTime }}</el-descriptions-item>
        <el-descriptions-item label="注册来源">{{ getSourceText(currentUser.source) }}</el-descriptions-item>
        <el-descriptions-item label="账户余额">¥{{ currentUser.balance || 0 }}</el-descriptions-item>
        <el-descriptions-item label="累计消费">¥{{ currentUser.totalConsume || 0 }}</el-descriptions-item>
        <el-descriptions-item label="订单数量">{{ currentUser.orderCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="收藏数量">{{ currentUser.favoriteCount || 0 }}</el-descriptions-item>
      </el-descriptions>
      
      <div class="user-tags" v-if="currentUser">
        <p class="tags-title">身份标签</p>
        <el-tag v-if="currentUser.isArtist" type="success" size="large">艺术家</el-tag>
        <el-tag v-if="currentUser.isPromoter" type="warning" size="large">艺荐官</el-tag>
        <el-tag v-if="currentUser.isVip" type="danger" size="large">VIP会员</el-tag>
      </div>
    </el-dialog>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份">
          <el-checkbox-group v-model="editForm.identities">
            <el-checkbox label="artist">艺术家</el-checkbox>
            <el-checkbox label="promoter">艺荐官</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加用户弹窗 -->
    <el-dialog v-model="addVisible" title="添加用户" width="450px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
          创建后默认身份为收藏者，可通过编辑功能添加其他身份
        </el-alert>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="addForm.nickname" placeholder="可选，默认为\"用户\"+手机号后4位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Phone, Message } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const editVisible = ref(false)
const addVisible = ref(false)
const addLoading = ref(false)
const currentUser = ref({})
const editFormRef = ref()
const addFormRef = ref()

const stats = reactive({
  total: 0,
  artist: 0,
  promoter: 0,
  todayNew: 0
})

const searchForm = reactive({
  userId: '',
  nickname: '',
  phone: '',
  identity: '',
  dateRange: []
})

const editForm = reactive({
  nickname: '',
  phone: '',
  identities: [],
  remark: ''
})

const addForm = reactive({
  phone: '',
  nickname: ''
})

const addRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const editRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getSourceText = (source) => {
  const map = { wechat: '微信', app: 'APP', web: '网页', other: '其他' }
  return map[source] || source
}

// 复制用户ID
const copyId = async (row) => {
  const id = row.openId || row.userId
  try {
    await navigator.clipboard.writeText(id)
    ElMessage.success('已复制用户ID')
  } catch {
    ElMessage.info('用户ID: ' + id)
  }
}

// 复制文本
const copyText = async (text, label) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制${label}`)
  } catch {
    ElMessage.info(`${label}: ${text}`)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      userId: searchForm.userId || undefined,
      nickname: searchForm.nickname || undefined,
      phone: searchForm.phone || undefined,
      identity: searchForm.identity || undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined
    }
    const [data, statsData] = await Promise.all([
      request.get('/user/list', { params }),
      request.get('/user/stats')
    ])
    tableData.value = data.records || []
    pagination.total = data.total || 0
    Object.assign(stats, statsData)
  } catch (e) {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { userId: '', nickname: '', phone: '', identity: '', dateRange: [] })
  handleSearch()
}

const exportData = () => {
  ElMessage.info('正在导出数据...')
  // TODO: 实现导出功能
}

// 打开添加用户弹窗
const openAddDialog = () => {
  Object.assign(addForm, { phone: '', nickname: '' })
  addVisible.value = true
}

// 添加用户
const handleAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    addLoading.value = true
    // 调用创建用户接口
    const result = await request.post('/user/create', addForm)
    // 如果返回了 userId，说明创建成功
    if (result.userId) {
      ElMessage.success({ message: '用户创建成功！用户ID：' + result.userId, duration: 5000 })
    } else {
      ElMessage.info(result.message || '用户已存在')
    }
    addVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('创建失败：' + (e.message || '未知错误'))
  } finally {
    addLoading.value = false
  }
}

const viewDetail = (row) => {
  currentUser.value = { ...row }
  detailVisible.value = true
}

const editUser = (row) => {
  currentUser.value = row
  Object.assign(editForm, {
    nickname: row.nickname,
    phone: row.phone,
    identities: [
      row.isArtist ? 'artist' : '',
      row.isPromoter ? 'promoter' : ''
    ].filter(Boolean),
    remark: row.remark || ''
  })
  editVisible.value = true
}

const saveEdit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await request.put(`/user/${currentUser.value.userId}`, {
      nickname: editForm.nickname,
      phone: editForm.phone,
      identities: editForm.identities,
      remark: editForm.remark
    })
    editVisible.value = false
    await loadData()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const toggleStatus = async (row) => {
  const action = row.status === 'normal' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', { type: 'warning' })
    await request.post('/user/updateStatus', {
      userId: row.userId,
      status: row.status === 'normal' ? 0 : 1
    })
    await loadData()
    ElMessage.success(`${action}成功`)
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

.search-form {
  margin-bottom: 20px;
}

.stats-bar {
  display: flex;
  gap: 40px;
  padding: 20px 30px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .stat-label {
      font-size: 14px;
      color: #999;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;

  .clickable-avatar {
    cursor: pointer;
    transition: transform 0.2s;

    &:hover {
      transform: scale(1.1);
    }
  }

  .user-detail {
    .nickname {
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 4px 0;
    }

    .user-id {
      font-size: 12px;
      color: #999;
      margin: 0;
      cursor: pointer;

      &:hover {
        color: #409eff;
      }
    }
  }
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 2px 0;

  .el-icon {
    color: #909399;
  }

  .clickable-text {
    cursor: pointer;
    transition: color 0.2s;

    &:hover {
      color: #409eff;
    }
  }
}

.identity-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.balance, .consume {
  font-weight: 600;
  color: #f56c6c;
  margin: 0 0 4px 0;
}

.coupon, .order-count, .email, .source {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.user-tags {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;

  .tags-title {
    margin-bottom: 12px;
    font-weight: 500;
  }
}
</style>
