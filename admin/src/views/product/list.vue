<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">作品列表</span>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="作品ID">
          <el-input v-model="searchForm.artworkId" placeholder="请输入作品ID" clearable />
        </el-form-item>
        <el-form-item label="作品名称">
          <el-input v-model="searchForm.title" placeholder="请输入作品名称" clearable />
        </el-form-item>
        <el-form-item label="艺术家">
          <el-input v-model="searchForm.artistName" placeholder="请输入艺术家" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable>
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="上架" value="1" />
            <el-option label="下架" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="artworkId" label="作品ID" width="100" />
      <el-table-column label="作品信息" min-width="280">
        <template #default="{ row }">
          <div class="artwork-info">
            <el-image :src="row.cover" :preview-src-list="row.cover ? [row.cover] : []" style="width: 80px; height: 80px" fit="cover">
              <template #error>
                <div class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="detail">
              <p class="title">{{ row.title }}</p>
              <p class="artist">{{ row.artistName }}</p>
              <p class="category">{{ row.categoryName }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="150">
        <template #default="{ row }">
          <p>¥{{ row.price }}</p>
          <p class="original" v-if="row.originalPrice">原价: ¥{{ row.originalPrice }}</p>
        </template>
      </el-table-column>
      <el-table-column prop="salesCount" label="销量" width="80" />
      <el-table-column prop="favoriteCount" label="收藏数" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分销" width="120">
        <template #default="{ row }">
          <el-tag :type="row.distributionEnabled ? 'success' : 'info'" size="small">
            {{ row.distributionEnabled ? '已开启' : '未开启' }}
          </el-tag>
          <span class="commission-text" v-if="row.distributionEnabled">
            佣金 {{ row.commissionRate || 10 }}%
          </span>
        </template>
      </el-table-column>
      <el-table-column label="分销统计" width="140">
        <template #default="{ row }">
          <div class="dist-stats">
            <span>推广 {{ row.distributionOrders || 0 }} 单</span>
            <span class="money">¥{{ row.distributionEarnings || 0 }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="success" link @click="handleDistribution(row)">分销</el-button>
          <el-button type="warning" link @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
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
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 分销管理弹窗 -->
    <el-dialog v-model="distVisible" title="分销管理" width="600px" destroy-on-close>
      <div class="dist-info" v-if="distForm.artworkId">
        <div class="artwork-preview">
          <el-image :src="distForm.cover" style="width: 80px; height: 80px" fit="cover">
            <template #error>
              <div class="image-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="artwork-detail">
            <p class="name">{{ distForm.title }}</p>
            <p class="price">¥{{ distForm.price }}</p>
          </div>
        </div>
      </div>
      
      <el-form ref="distFormRef" :model="distForm" label-width="100px" class="dist-form">
        <el-form-item label="分销状态">
          <el-switch v-model="distForm.distributionEnabled" active-text="开启分销" inactive-text="关闭分销" />
        </el-form-item>
        
        <el-form-item label="佣金比例" prop="commissionRate">
          <el-input-number 
            v-model="distForm.commissionRate" 
            :min="0" 
            :max="100" 
            :precision="0"
            :disabled="!distForm.distributionEnabled"
          />
          <span class="unit">%</span>
          <span class="tip">（订单金额的百分比作为佣金）</span>
        </el-form-item>
        
        <el-form-item label="佣金计算">
          <div class="calc-preview">
            <p>商品售价：¥{{ distForm.price || 0 }}</p>
            <p>佣金比例：{{ distForm.commissionRate || 0 }}%</p>
            <p class="result">预估佣金：¥{{ ((distForm.price || 0) * (distForm.commissionRate || 0) / 100).toFixed(2) }}</p>
          </div>
        </el-form-item>
        
        <el-divider content-position="left">分销统计</el-divider>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="stat-card">
              <p class="label">推广订单</p>
              <p class="value">{{ distForm.distributionOrders || 0 }}</p>
              <p class="unit">单</p>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <p class="label">分销佣金</p>
              <p class="value">{{ distForm.distributionEarnings || 0 }}</p>
              <p class="unit">元</p>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <p class="label">推广人数</p>
              <p class="value">{{ distForm.distributionUsers || 0 }}</p>
              <p class="unit">人</p>
            </div>
          </el-col>
        </el-row>
        
        <el-form-item label="分销链接">
          <el-input :value="distLink" readonly>
            <template #append>
              <el-button @click="copyLink">复制</el-button>
            </template>
          </el-input>
          <p class="link-tip">推广员可通过此链接分享作品获得佣金</p>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="distVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDistribution" :loading="distSaveLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑作品弹窗 -->
    <el-dialog v-model="editVisible" title="编辑作品" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="作品名称" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入作品名称" />
        </el-form-item>
        <el-form-item label="艺术家" prop="artistName">
          <el-input v-model="editForm.artistName" placeholder="请输入艺术家名称" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作品图片" prop="cover">
          <div class="upload-container">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :http-request="handleImageUpload"
              action="#"
            >
              <img v-if="editForm.cover" :src="editForm.cover" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">支持 JPG/PNG，大小不超过 5MB</div>
          </div>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="editForm.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="editForm.originalPrice" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="editForm.stock" :min="0" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="作品描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入作品描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const saveLoading = ref(false)
const tableData = ref([])
const categories = ref([])
const editVisible = ref(false)
const formRef = ref()
const distFormRef = ref()
const distVisible = ref(false)
const distSaveLoading = ref(false)
const distLink = ref('')

const searchForm = reactive({
  artworkId: '',
  title: '',
  artistName: '',
  categoryId: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const editForm = reactive({
  artworkId: '',
  title: '',
  artistName: '',
  categoryId: '',
  cover: '',
  price: 0,
  originalPrice: 0,
  stock: 0,
  description: '',
  status: 1
})

const distForm = reactive({
  artworkId: '',
  title: '',
  cover: '',
  price: 0,
  distributionEnabled: false,
  commissionRate: 10,
  distributionOrders: 0,
  distributionEarnings: 0,
  distributionUsers: 0
})

const rules = {
  title: [{ required: true, message: '请输入作品名称', trigger: 'blur' }],
  artistName: [{ required: true, message: '请输入艺术家名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.artworkId) params.id = searchForm.artworkId
    if (searchForm.title) params.title = searchForm.title
    if (searchForm.artistName) params.authorName = searchForm.artistName
    if (searchForm.categoryId) params.categoryId = searchForm.categoryId
    if (searchForm.status) params.status = searchForm.status
    const data = await request.get('/product/list', { params })
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    tableData.value = [
      { artworkId: 'A001', title: '山水国画', artistName: '张大千', cover: '', categoryId: 1, categoryName: '国画', price: 58000, originalPrice: 68000, salesCount: 12, favoriteCount: 156, status: 1, createTime: '2024-01-15 10:00:00', stock: 10, description: '经典山水国画作品', distributionEnabled: true, commissionRate: 15, distributionOrders: 28, distributionEarnings: 2436, distributionUsers: 12 },
      { artworkId: 'A002', title: '油画风景', artistName: '李明', cover: '', categoryId: 2, categoryName: '油画', price: 32000, salesCount: 8, favoriteCount: 89, status: 1, createTime: '2024-01-16 14:30:00', stock: 5, description: '现代风格油画', distributionEnabled: false, commissionRate: 10, distributionOrders: 0, distributionEarnings: 0, distributionUsers: 0 },
      { artworkId: 'A003', title: '书法对联', artistName: '王羲之', cover: '', categoryId: 3, categoryName: '书法', price: 15000, salesCount: 25, favoriteCount: 234, status: 1, createTime: '2024-01-17 09:15:00', stock: 20, description: '名家书法作品', distributionEnabled: true, commissionRate: 20, distributionOrders: 45, distributionEarnings: 13500, distributionUsers: 18 }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await request.get('/product/categories')
  } catch (e) {
    categories.value = [
      { id: 1, name: '国画' },
      { id: 2, name: '油画' },
      { id: 3, name: '书法' },
      { id: 4, name: '版画' },
      { id: 5, name: '雕塑' }
    ]
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { artworkId: '', title: '', artistName: '', categoryId: '', status: '' })
  handleSearch()
}

const handleEdit = (row) => {
  Object.assign(editForm, {
    artworkId: row.artworkId,
    title: row.title,
    artistName: row.artistName,
    categoryId: row.categoryId,
    cover: row.cover || '',
    price: row.price,
    originalPrice: row.originalPrice || 0,
    stock: row.stock || 0,
    description: row.description || '',
    status: row.status
  })
  editVisible.value = true
}

const beforeImageUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleImageUpload = async (options) => {
  const { file } = options
  // 模拟上传 - 将文件转为 base64
  const reader = new FileReader()
  reader.onload = (e) => {
    editForm.cover = e.target.result
    ElMessage.success('图片上传成功')
  }
  reader.readAsDataURL(file)
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  saveLoading.value = true
  try {
    // 模拟保存成功
    const index = tableData.value.findIndex(item => item.artworkId === editForm.artworkId)
    if (index > -1) {
      const category = categories.value.find(c => c.id === editForm.categoryId)
      tableData.value[index] = {
        ...tableData.value[index],
        ...editForm,
        categoryName: category?.name || ''
      }
    }
    ElMessage.success('保存成功')
    editVisible.value = false
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saveLoading.value = false
  }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${action}该作品吗？`, '提示', { type: 'warning' })
    row.status = newStatus
    ElMessage.success(`${action}成功`)
  } catch (e) {}
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该作品吗？', '提示', { type: 'warning' })
    await request.post('/product/delete', { artworkId: row.artworkId })
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {}
}

const handleDistribution = (row) => {
  Object.assign(distForm, {
    artworkId: row.artworkId,
    title: row.title,
    cover: row.cover || '',
    price: row.price || 0,
    distributionEnabled: row.distributionEnabled || false,
    commissionRate: row.commissionRate || 10,
    distributionOrders: row.distributionOrders || 0,
    distributionEarnings: row.distributionEarnings || 0,
    distributionUsers: row.distributionUsers || 0
  })
  distLink.value = `https://shiyiju.com/artwork/${row.artworkId}?from=dist`
  distVisible.value = true
}

const handleSaveDistribution = async () => {
  distSaveLoading.value = true
  try {
    // 模拟保存
    const index = tableData.value.findIndex(item => item.artworkId === distForm.artworkId)
    if (index > -1) {
      tableData.value[index] = {
        ...tableData.value[index],
        distributionEnabled: distForm.distributionEnabled,
        commissionRate: distForm.commissionRate,
        distributionOrders: distForm.distributionOrders,
        distributionEarnings: distForm.distributionEarnings,
        distributionUsers: distForm.distributionUsers
      }
    }
    ElMessage.success('分销设置已保存')
    distVisible.value = false
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    distSaveLoading.value = false
  }
}

const copyLink = () => {
  navigator.clipboard.writeText(distLink.value).then(() => {
    ElMessage.success('链接已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped>
.artwork-info {
  display: flex;
  gap: 12px;
  
  .detail {
    .title {
      font-weight: 500;
      margin-bottom: 4px;
    }
    .artist {
      font-size: 13px;
      color: #666;
    }
    .category {
      font-size: 12px;
      color: #999;
    }
  }
}

.original {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

.image-placeholder {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 24px;
}

.upload-container {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.avatar-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color 0.2s;
  }
  
  :deep(.el-upload:hover) {
    border-color: #409eff;
  }
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
}

.avatar {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  padding-top: 8px;
}

/* 分销样式 */
.commission-text {
  display: block;
  font-size: 12px;
  color: #67c23a;
  margin-top: 2px;
}

.dist-stats {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  
  .money {
    color: #f56c6c;
    font-weight: 500;
  }
}

.dist-info {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  
  .artwork-preview {
    display: flex;
    gap: 12px;
    align-items: center;
    
    .artwork-detail {
      .name {
        font-weight: 500;
        margin-bottom: 4px;
      }
      .price {
        color: #f56c6c;
        font-size: 16px;
      }
    }
  }
}

.dist-form {
  .unit {
    margin-left: 8px;
    color: #909399;
  }
  
  .tip {
    margin-left: 12px;
    color: #909399;
    font-size: 12px;
  }
  
  .calc-preview {
    background: #fdf6ec;
    padding: 12px 16px;
    border-radius: 4px;
    font-size: 13px;
    color: #909399;
    
    .result {
      color: #67c23a;
      font-weight: 500;
      margin-top: 8px;
    }
  }
  
  .link-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

.stat-card {
  text-align: center;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: #fff;
  
  .label {
    font-size: 12px;
    opacity: 0.9;
    margin-bottom: 8px;
  }
  
  .value {
    font-size: 24px;
    font-weight: bold;
  }
  
  .unit {
    font-size: 12px;
    opacity: 0.8;
    margin-top: 4px;
  }
}
</style>
