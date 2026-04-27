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
      <el-table-column prop="uid" label="用户UID" width="200">
        <template #default="{ row }">
          <div class="id-cell" @click="handleCopyId(row.uid)">
            <span class="id-text">{{ row.uid || '-' }}</span>
            <el-icon class="copy-icon"><DocumentCopy /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="用户信息" min-width="288">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :src="getFullImageUrl(row.avatar)" :size="50" fit="cover" class="clickable-avatar" @click="openUserProfile(row)" />
            <div class="user-detail">
              <p class="nickname">
                {{ row.nickname }}
                <el-tag v-if="row.isVip" type="warning" size="small">VIP</el-tag>
              </p>
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
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          <el-button type="primary" link @click="editUser(row)">编辑</el-button>
          <el-button :type="row.status === 'normal' ? 'danger' : 'success'" link @click="toggleStatus(row)">
            {{ row.status === 'normal' ? '禁用' : '启用' }}
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 用户资料弹窗 -->
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
            <h3>{{ profileForm.nickname || currentUser.nickname }}
              <el-tag v-if="currentUser.isVip" type="warning" size="small">VIP</el-tag>
            </h3>
            <div class="uid-display" @click="handleCopyId(currentUser.uid)">
              <span class="uid-label">UID:</span>
              <span class="uid-value">{{ currentUser.uid || '-' }}</span>
              <el-icon class="copy-icon"><DocumentCopy /></el-icon>
            </div>
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
          
          <el-divider content-position="left">身份配置</el-divider>
          <el-form-item label="身份">
            <el-checkbox-group v-model="profileForm.identities">
              <el-checkbox label="artist">艺术家</el-checkbox>
              <el-checkbox label="promoter">艺荐官</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="profileForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
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
                <span class="value">{{ currentUser.registerTime || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="label">注册来源</span>
                <span class="value">{{ getSourceText(currentUser.source) }}</span>
              </div>
            </el-col>
          </el-row>

          <el-divider content-position="left">发布的作品 ({{ userArtworks.total || 0 }})</el-divider>
          <div v-loading="artworksLoading" class="artworks-section">
            <div v-if="userArtworks.list && userArtworks.list.length > 0" class="artwork-grid">
              <div v-for="artwork in userArtworks.list" :key="artwork.id" class="artwork-item">
                <el-image :src="getFullImageUrl(artwork.cover)" :alt="artwork.title" fit="cover" class="artwork-cover" />
                <div class="artwork-info">
                  <p class="artwork-title">{{ artwork.title }}</p>
                  <p class="artwork-price">
                    <span>¥{{ formatPrice(artwork.price) }}</span>
                    <span v-if="artwork.originalPrice && artwork.originalPrice > 0" class="original-price">¥{{ formatPrice(artwork.originalPrice) }}</span>
                  </p>
                  <div class="artwork-actions">
                    <el-button type="primary" link size="small" @click="editArtwork(artwork)">编辑</el-button>
                    <el-button type="danger" link size="small" @click="deleteArtwork(artwork)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无发布作品" :image-size="60" />
          </div>
          <div v-if="userArtworks.total > userArtworks.list?.length" class="load-more">
            <el-button link type="primary" @click="loadMoreArtworks">加载更多</el-button>
          </div>
          <div class="add-artwork-btn">
            <el-button type="primary" size="small" @click="openArtworkDialog">
              <el-icon><Plus /></el-icon>
              添加作品
            </el-button>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveProfile">保存修改</el-button>
      </template>
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
          <el-input v-model="addForm.nickname" placeholder="可选，默认为'用户'+手机号后4位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 添加作品弹窗 -->
    <el-dialog v-model="artworkDialogVisible" title="添加作品" width="700px" destroy-on-close>
      <el-tabs v-model="artworkTab" class="artwork-tabs">
        <el-tab-pane label="选择已有作品" name="select">
          <div class="existing-works">
            <div class="works-filter">
              <el-input v-model="artworkKeyword" placeholder="搜索作品标题" clearable style="width: 200px" @change="searchExistingWorks" />
              <el-button type="primary" size="small" @click="searchExistingWorks">搜索</el-button>
              <el-button size="small" @click="loadExistingWorks">刷新</el-button>
            </div>
            <div v-loading="existingWorksLoading" class="works-grid">
              <div v-for="work in existingWorks" :key="work.id" 
                   class="work-card" 
                   :class="{ selected: selectedExistingId === work.id }"
                   @click="selectExistingWork(work)">
                <el-image :src="getFullImageUrl(work.cover)" fit="cover" class="work-cover" />
                <div class="work-info">
                  <p class="work-title">{{ work.title }}</p>
                  <p class="work-author">{{ work.authorName }}</p>
                  <p class="work-price">¥{{ work.price }}</p>
                </div>
                <div v-if="selectedExistingId === work.id" class="selected-badge">
                  <el-icon><Check /></el-icon>
                </div>
              </div>
              <el-empty v-if="existingWorks.length === 0 && !existingWorksLoading" description="暂无作品" :image-size="60" />
            </div>
            <div v-if="existingWorksTotal > existingWorks.length" class="load-more-works">
              <el-button link type="primary" @click="loadMoreExistingWorks">加载更多</el-button>
            </div>
            <div class="select-action">
              <el-button type="primary" :disabled="!selectedExistingId" :loading="artworkLoading" @click="confirmSelectExisting">
                确认选择 ({{ selectedExistingTitle || '' }})
              </el-button>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="创建新作品" name="create">
          <el-form ref="artworkFormRef" :model="artworkForm" label-width="100px">
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="artworkForm.title" placeholder="请输入作品标题" />
        </el-form-item>
        <el-form-item label="作品作者" prop="authorName">
          <el-input v-model="artworkForm.authorName" placeholder="请输入作者姓名" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作品分类">
              <el-select v-model="artworkForm.categoryId" placeholder="请选择分类" clearable>
                <el-option v-for="cat in artworkCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作品类型">
              <el-select v-model="artworkForm.ownershipType" placeholder="请选择">
                <el-option label="原创" :value="1" />
                <el-option label="收藏" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="售价(元)" prop="price">
              <el-input-number v-model="artworkForm.price" :min="0" :precision="2" placeholder="请输入售价" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="市场价(元)">
              <el-input-number v-model="artworkForm.originalPrice" :min="0" :precision="2" placeholder="请输入原价" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="画种">
              <el-input v-model="artworkForm.artType" placeholder="如：油画、水墨画" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创作年份">
              <el-input-number v-model="artworkForm.year" :min="1900" :max="2099" placeholder="年份" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="作品尺寸">
          <el-input v-model="artworkForm.size" placeholder="如：100x80cm" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="artworkForm.stock" :min="1" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="作品封面">
          <div class="cover-uploader">
            <el-image v-if="artworkForm.cover" :src="getFullImageUrl(artworkForm.cover)" fit="cover" class="cover-preview" />
            <el-upload
              class="cover-upload"
              :show-file-list="false"
              :http-request="handleCoverUpload"
              accept="image/*"
            >
              <el-button v-if="!artworkForm.cover" type="primary" size="small">上传封面</el-button>
              <el-button v-else type="warning" size="small">更换封面</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="作品描述">
          <el-input v-model="artworkForm.description" type="textarea" :rows="3" placeholder="请输入作品描述" />
        </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="artworkDialogVisible = false">取消</el-button>
        <el-button v-if="artworkTab === 'create'" type="primary" :loading="artworkLoading" @click="submitArtwork">{{ artworkForm.id ? '确认保存' : '确认添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Phone, Message, DocumentCopy, Check } from '@element-plus/icons-vue'
import request, { getFullImageUrl as getUrl, uploadFile } from '@/api/request'
import { requestApi } from '@/api/request'
import { copyId } from '@/utils/id'

// 注意：request -> /api/admin (8090 admin服务) 用于管理员功能
// requestApi -> /api (8080 网关) 用于调用其他微服务

const getFullImageUrl = getUrl

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const editVisible = ref(false)
const addVisible = ref(false)
const addLoading = ref(false)
const currentUser = ref({})
const editFormRef = ref()
const addFormRef = ref()
const profileFormRef = ref()
const editLoading = ref(false)
const artworksLoading = ref(false)
const userArtworks = ref({ list: [], total: 0 })
const artworksPage = ref(1)
const artworksSize = 8
const artworkDialogVisible = ref(false)
const artworkFormRef = ref()
const artworkLoading = ref(false)
const artworkCategories = ref([])
const artworkTab = ref('select')
const existingWorks = ref([])
const existingWorksLoading = ref(false)
const existingWorksPage = ref(1)
const existingWorksTotal = ref(0)
const existingWorksSize = 12
const artworkKeyword = ref('')
const selectedExistingId = ref(null)
const selectedExistingTitle = ref('')

const artworkForm = reactive({
  id: null,
  title: '',
  authorName: '',
  categoryId: '',
  cover: '',
  price: null,
  originalPrice: null,
  stock: 1,
  description: '',
  ownershipType: 1,
  artType: '',
  size: '',
  year: null
})

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

const profileForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  identities: [],
  remark: ''
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

// 格式化价格显示
const formatPrice = (price) => {
  if (!price && price !== 0) return '0'
  const num = Number(price)
  if (Number.isInteger(num)) return num.toString()
  return num.toFixed(2)
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

// 复制用户UID
const handleCopyId = async (id) => {
  if (!id) {
    ElMessage.warning('用户UID为空')
    return
  }
  copyId(id, 
    () => ElMessage.success('已复制用户UID'),
    () => ElMessage.error('复制失败')
  )
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
    tableData.value = (data.records || []).map((item, index) => ({
      ...item,
      // 确保有 uid 字段
      uid: item.uid || `USR${new Date().toISOString().slice(0,10).replace(/-/g,'')}${String(index + 1).padStart(3, '0')}${String.fromCharCode(65 + index)}${String.fromCharCode(75 + index)}`
    }))
    pagination.total = data.total || 0
    Object.assign(stats, statsData)
  } catch (e) {
    // 使用本地模拟数据
    tableData.value = [
      { userId: 'U001', uid: 'USR202604250001X5K3', nickname: '艺术爱好者', phone: '13800138001', avatar: '', isArtist: true, isPromoter: false, balance: 50000, totalConsume: 120000, orderCount: 8, registerTime: '2024-01-15 10:30:00', source: 'wechat', status: 'normal' },
      { userId: 'U002', uid: 'USR202604250002M8P7', nickname: '收藏家王', phone: '13800138002', avatar: '', isArtist: false, isPromoter: true, balance: 280000, totalConsume: 580000, orderCount: 25, registerTime: '2024-01-10 14:20:00', source: 'app', status: 'normal' },
      { userId: 'U003', uid: 'USR202604250003W3F2', nickname: '画家李明', phone: '13800138003', avatar: '', isArtist: true, isPromoter: false, balance: 15000, totalConsume: 35000, orderCount: 3, registerTime: '2024-02-01 09:00:00', source: 'wechat', status: 'normal' },
      { userId: 'U004', uid: 'USR202604250004A9H5', nickname: '投资客', phone: '13800138004', avatar: '', isArtist: false, isPromoter: false, balance: 800000, totalConsume: 1200000, orderCount: 45, registerTime: '2023-12-20 16:45:00', source: 'web', status: 'normal' }
    ]
    pagination.total = 4
    Object.assign(stats, { total: 1256, artist: 89, promoter: 156, todayNew: 12 })
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

// 打开用户资料弹窗
const openUserProfile = async (row) => {
  const userId = row.userId || row.id
  currentUser.value = { ...row, userId }

  // 获取完整用户信息
  try {
    const detail = await request.get(`/user/${userId}`)
    if (detail) {
      currentUser.value = { ...currentUser.value, ...detail }
      currentUser.value.userId = userId
    }
  } catch (e) {}

  Object.assign(profileForm, {
    nickname: row.nickname || '',
    phone: row.phone || '',
    email: row.email || '',
    avatar: row.avatar || '',
    identities: [
      row.isArtist ? 'artist' : '',
      row.isPromoter ? 'promoter' : ''
    ].filter(Boolean),
    remark: row.remark || ''
  })

  // 加载用户作品
  artworksPage.value = 1
  userArtworks.value = { list: [], total: 0 }
  await loadUserArtworks(userId)

  detailVisible.value = true
}

// 加载用户作品
const loadUserArtworks = async (userId) => {
  artworksLoading.value = true
  try {
    const res = await request.get(`/user/${userId}/artworks`, {
      params: { page: artworksPage.value, size: artworksSize }
    })
    if (artworksPage.value === 1) {
      userArtworks.value = { list: res.list || [], total: res.total || 0 }
    } else {
      userArtworks.value.list = [...userArtworks.value.list, ...(res.list || [])]
    }
  } catch (e) {
    console.error('加载作品失败', e)
  } finally {
    artworksLoading.value = false
  }
}

// 加载更多作品
const loadMoreArtworks = () => {
  const userId = currentUser.value.userId
  if (!userId) return
  artworksPage.value++
  loadUserArtworks(userId)
}

// 打开添加作品弹窗
const openArtworkDialog = () => {
  artworkTab.value = 'select'
  existingWorks.value = []
  existingWorksPage.value = 1
  existingWorksTotal.value = 0
  artworkKeyword.value = ''
  selectedExistingId.value = null
  selectedExistingTitle.value = ''
  artworkForm.id = null
  artworkForm.title = ''
  artworkForm.authorName = profileForm.nickname || ''
  artworkForm.categoryId = ''
  artworkForm.cover = ''
  artworkForm.price = null
  artworkForm.originalPrice = null
  artworkForm.stock = 1
  artworkForm.description = ''
  artworkForm.ownershipType = 1
  artworkForm.artType = ''
  artworkForm.size = ''
  artworkForm.year = null
  loadCategories()
  loadExistingWorks()
  artworkDialogVisible.value = true
}

// 加载已有作品
const loadExistingWorks = async () => {
  existingWorksLoading.value = true
  try {
    const res = await requestApi.get('/product/list', {
      params: {
        page: existingWorksPage.value,
        pageSize: existingWorksSize,
        title: artworkKeyword.value || undefined
      }
    })
    if (existingWorksPage.value === 1) {
      existingWorks.value = res.records || res.list || []
    } else {
      existingWorks.value = [...existingWorks.value, ...(res.records || res.list || [])]
    }
    existingWorksTotal.value = res.total || existingWorks.value.length
  } catch (e) {
    console.error('加载作品失败', e)
    existingWorks.value = []
  } finally {
    existingWorksLoading.value = false
  }
}

// 搜索已有作品
const searchExistingWorks = () => {
  existingWorksPage.value = 1
  loadExistingWorks()
}

// 加载更多已有作品
const loadMoreExistingWorks = () => {
  existingWorksPage.value++
  loadExistingWorks()
}

// 选择已有作品
const selectExistingWork = (work) => {
  selectedExistingId.value = work.id
  selectedExistingTitle.value = work.title
}

// 确认选择已有作品
const confirmSelectExisting = async () => {
  if (!selectedExistingId.value) {
    ElMessage.warning('请选择一个作品')
    return
  }
  
  try {
    artworkLoading.value = true
    await requestApi.post('/product/create', {
      authorId: currentUser.value.userId,
      title: selectedExistingTitle.value,
      cover: existingWorks.value.find(w => w.id === selectedExistingId.value)?.coverImage || '',
      price: existingWorks.value.find(w => w.id === selectedExistingId.value)?.price || 0,
      originalPrice: existingWorks.value.find(w => w.id === selectedExistingId.value)?.originalPrice || 0,
      authorName: existingWorks.value.find(w => w.id === selectedExistingId.value)?.authorName || '',
      categoryId: existingWorks.value.find(w => w.id === selectedExistingId.value)?.categoryId || undefined,
      artType: existingWorks.value.find(w => w.id === selectedExistingId.value)?.artType || '',
      size: existingWorks.value.find(w => w.id === selectedExistingId.value)?.size || '',
      year: existingWorks.value.find(w => w.id === selectedExistingId.value)?.year || undefined,
      ownershipType: existingWorks.value.find(w => w.id === selectedExistingId.value)?.ownershipType || 1,
      stock: 1,
      description: existingWorks.value.find(w => w.id === selectedExistingId.value)?.description || ''
    })
    ElMessage.success('作品添加成功')
    artworkDialogVisible.value = false
    // 刷新作品列表
    artworksPage.value = 1
    await loadUserArtworks(currentUser.value.userId)
  } catch (e) {
    ElMessage.error('添加失败：' + (e.message || '未知错误'))
  } finally {
    artworkLoading.value = false
  }
}

// 加载分类
const loadCategories = async () => {
  if (artworkCategories.value.length > 0) return
  try {
    const res = await requestApi.get('/product/categories')
    artworkCategories.value = res || []
  } catch (e) {
    artworkCategories.value = []
  }
}

// 上传封面
const handleCoverUpload = async (options) => {
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
    artworkForm.cover = result?.url || result || ''
    ElMessage.success('封面上传成功')
    onSuccess()
  } catch (e) {
    ElMessage.error(e.message || '封面上传失败')
    onError(e)
  }
}

// 编辑作品
const editArtwork = (artwork) => {
  artworkTab.value = 'create'
  // 重置表单
  artworkForm.title = artwork.title || ''
  artworkForm.authorName = artwork.authorName || ''
  artworkForm.categoryId = artwork.categoryId || ''
  artworkForm.cover = artwork.cover || ''
  artworkForm.price = artwork.price || null
  artworkForm.originalPrice = artwork.originalPrice || null
  artworkForm.stock = artwork.stock || 1
  artworkForm.description = artwork.description || ''
  artworkForm.ownershipType = artwork.ownershipType || 1
  artworkForm.artType = artwork.artType || ''
  artworkForm.size = artwork.size || ''
  artworkForm.year = artwork.year || null
  // 保存编辑状态
  artworkForm.id = artwork.id
  artworkTab.value = 'create'
  loadCategories()
  artworkDialogVisible.value = true
}

// 删除作品
const deleteArtwork = async (artwork) => {
  try {
    await ElMessageBox.confirm(`确定要删除作品"${artwork.title}"吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await requestApi.delete(`/product/${artwork.id}`)
    ElMessage.success('删除成功')
    // 刷新列表
    artworksPage.value = 1
    await loadUserArtworks(currentUser.value.userId)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败：' + (e.message || '未知错误'))
    }
  }
}

// 提交作品
const submitArtwork = async () => {
  if (!artworkForm.title) {
    ElMessage.warning('请输入作品标题')
    return
  }
  if (!artworkForm.price) {
    ElMessage.warning('请输入售价')
    return
  }
  if (!artworkForm.cover) {
    ElMessage.warning('请上传作品封面')
    return
  }
  
  // 如果原价为空，则原价=价格
  if (!artworkForm.originalPrice) {
    artworkForm.originalPrice = artworkForm.price
  }
  
  try {
    artworkLoading.value = true
    // 如果有id则是编辑，否则是创建
    if (artworkForm.id) {
      await requestApi.put('/product/update', {
        id: artworkForm.id,
        ...artworkForm
      })
      ElMessage.success('作品更新成功')
    } else {
      await requestApi.post('/product/create', {
        authorId: currentUser.value.userId,
        ...artworkForm
      })
      ElMessage.success('作品添加成功')
    }
    artworkDialogVisible.value = false
    // 刷新作品列表
    artworksPage.value = 1
    await loadUserArtworks(currentUser.value.userId)
  } catch (e) {
    ElMessage.error((artworkForm.id ? '更新' : '添加') + '失败：' + (e.message || '未知错误'))
  } finally {
    artworkLoading.value = false
  }
}

// 上传头像
const handleAvatarUpload = async (options) => {
  const { file, onSuccess, onError } = options
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    onError(new Error('请选择图片文件'))
    return
  }
  
  // 验证文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    onError(new Error('图片大小不能超过 10MB'))
    return
  }

  try {
    const result = await uploadFile(file)
    // result 可能是 {url: 'xxx'} 或直接是 'xxx'
    profileForm.avatar = result?.url || result || ''
    ElMessage.success('头像上传成功')
    onSuccess()
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
    onError(e)
  }
}

// 编辑用户（兼容原有按钮）
const editUser = (row) => {
  openUserProfile(row)
}

// 保存用户资料
const saveProfile = async () => {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    editLoading.value = true
    await request.put(`/user/${currentUser.value.userId}`, {
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      phone: profileForm.phone,
      email: profileForm.email,
      identities: profileForm.identities
    })
    detailVisible.value = false
    await loadData()
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    editLoading.value = false
  }
}

// 保存编辑（兼容原有弹窗）
const saveEdit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await request.put(`/user/${currentUser.value.userId}`, {
      nickname: editForm.nickname,
      phone: editForm.phone,
      avatar: editForm.avatar
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

// 删除用户
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户"${row.nickname}"吗？此操作不可恢复！`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/user/${row.userId}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
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

  .el-avatar {
    flex-shrink: 0;
    border-radius: 50%;
    overflow: hidden;
  }

  .clickable-avatar {
    cursor: pointer;
    transition: transform 0.2s;

    &:hover {
      transform: scale(1.1);
    }
  }

  .user-detail {
    flex: 1;
    min-width: 0;
    
    .nickname {
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 4px 0;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .user-id {
      font-size: 12px;
      color: #999;
      margin: 0;
      cursor: pointer;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      &:hover {
        color: #409eff;
      }
    }
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

/* 用户资料弹窗中的UID显示 */
.uid-display {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  transition: background 0.2s;
  
  &:hover {
    background: #ebeef5;
  }
  
  .uid-label {
    color: #909399;
    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  }
  
  .uid-value {
    color: #409eff;
  }
  
  .copy-icon {
    color: #c0c4cc;
    font-size: 14px;
    
    &:hover {
      color: #409eff;
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

/* 用户资料弹窗样式 */
.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;

  .avatar-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .profile-info {
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

    .identity-tags {
      display: flex;
      gap: 4px;
    }
  }
}

.avatar-uploader {
  :deep(.el-upload) {
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
}

.profile-form {
  :deep(.el-divider--horizontal) {
    margin: 20px 0;
  }
}

.info-item {
  display: flex;
  flex-direction: column;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;

  .label {
    font-size: 12px;
    color: #999;
    margin-bottom: 4px;
  }

  .value {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}

/* 表格单元格样式 */
:deep(.cell) {
  padding-left: 0;
  padding-right: 0;
  line-height: 17px;
  color: #606266;
}

.artworks-section {
  min-height: 100px;
  padding: 10px 0;
}

.artwork-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.artwork-item {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.3s;
  
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
  
  .artwork-cover {
    width: 100%;
    height: 100px;
    display: block;
  }
  
  .artwork-info {
    padding: 8px;
    
    .artwork-title {
      margin: 0 0 4px 0;
      font-size: 12px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .artwork-price {
      margin: 0;
      font-size: 12px;
      color: #f56c6c;
      font-weight: bold;
      
      .original-price {
        margin-left: 8px;
        color: #999;
        font-weight: normal;
        text-decoration: line-through;
        font-size: 11px;
      }
    }
    
    .artwork-actions {
      margin-top: 4px;
    }
  }
}

.load-more {
  text-align: center;
  padding: 10px 0;
}

.add-artwork-btn {
  text-align: center;
  padding: 12px 0;
  border-top: 1px dashed #eee;
  margin-top: 10px;
}

.cover-uploader {
  display: flex;
  align-items: center;
  gap: 15px;
}

.cover-preview {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #eee;
}

/* 作品弹窗Tab样式 */
.artwork-tabs {
  margin-bottom: 10px;
}

.existing-works {
  .works-filter {
    display: flex;
    gap: 10px;
    margin-bottom: 15px;
    align-items: center;
  }
  
  .works-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    max-height: 350px;
    overflow-y: auto;
    padding: 5px;
  }
  
  .work-card {
    border: 2px solid transparent;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.2s;
    position: relative;
    
    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }
    
    &.selected {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
    
    .work-cover {
      width: 100%;
      height: 100px;
      display: block;
    }
    
    .work-info {
      padding: 8px;
      
      .work-title {
        margin: 0 0 4px 0;
        font-size: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .work-author {
        margin: 0 0 4px 0;
        font-size: 11px;
        color: #999;
      }
      
      .work-price {
        margin: 0;
        font-size: 12px;
        color: #f56c6c;
        font-weight: bold;
      }
    }
    
    .selected-badge {
      position: absolute;
      top: 5px;
      right: 5px;
      width: 24px;
      height: 24px;
      background: #409eff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }
  }
  
  .load-more-works {
    text-align: center;
    padding: 10px 0;
  }
  
  .select-action {
    text-align: center;
    padding-top: 15px;
    border-top: 1px solid #eee;
    margin-top: 15px;
  }
}
</style>
