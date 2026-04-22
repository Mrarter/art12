<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">作品列表</span>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>增加作品
      </el-button>
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
      <el-table-column label="作品ID" width="120">
        <template #default="{ row }">
          <span class="artwork-code">{{ row.artworkCode || row.artworkId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="作品信息" min-width="280">
        <template #default="{ row }">
          <div class="artwork-info">
            <div class="cover-wrapper" @click="handleEdit(row)">
              <el-image 
                :src="row.cover" 
                :preview-src-list="row.cover ? [row.cover] : []" 
                style="width: 80px; height: 80px" 
                fit="cover"
                :preview-teleported="true"
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="edit-overlay">
                <el-icon><Edit /></el-icon>
              </div>
            </div>
            <div class="detail">
              <p class="title" @click="handleEdit(row)">{{ row.title }}</p>
              <p class="artist" @click="handleEdit(row)">{{ row.artistName }}</p>
              <p class="category">{{ row.categoryName }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="150">
        <template #default="{ row }">
          <p>¥{{ row.price }}</p>
          <p class="original" v-if="row.originalPrice">原价: ¥{{ row.originalPrice }}</p>
          <p class="price-rise" v-if="row.priceRise > 0" style="color: #ff4d4f; font-size: 12px;">
            涨幅 +{{ (row.priceRise * 100).toFixed(1) }}%
          </p>
        </template>
      </el-table-column>
      <el-table-column label="画种/尺寸/年份" min-width="180">
        <template #default="{ row }">
          <div class="art-info">
            <span v-if="row.artType" class="art-type">{{ row.artType }}</span>
            <span v-if="row.size" class="art-size">{{ row.size }}</span>
            <span v-if="row.year" class="art-year">{{ row.year }}年</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.ownershipType === 1 ? 'success' : 'warning'" size="small">
            {{ row.ownershipType === 1 ? '原创' : '收藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="favoriteCount" label="收藏数" width="80" />
<el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权重" width="100">
        <template #default="{ row }">
          <el-input-number 
            v-model="row.weight" 
            :min="0" 
            :max="9999" 
            size="small" 
            controls 
            @change="handleWeightChange(row)"
          />
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
        <el-form-item label="画种">
          <el-input v-model="editForm.artType" placeholder="如：国画、油画、水彩" />
        </el-form-item>
        <el-form-item label="尺寸">
          <el-input v-model="editForm.size" placeholder="如：100x80cm、四尺整张" />
        </el-form-item>
        <el-form-item label="创作年份">
          <el-input-number v-model="editForm.year" :min="1900" :max="2099" placeholder="如：2024" />
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
        <el-form-item label="作品类型" prop="ownershipType">
          <el-radio-group v-model="editForm.ownershipType">
            <el-radio :value="1">原创</el-radio>
            <el-radio :value="2">收藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作品描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入作品描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
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
import { Plus, Picture, Edit } from '@element-plus/icons-vue'
import request from '@/api/request'
import { requestApi } from '@/api/request'

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
  artType: '',
  size: '',
  year: null,
  cover: '',
  price: 0,
  originalPrice: 0,
  ownershipType: 1,  // 默认原创
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
    const data = await requestApi.get('/product/list', { params })
    // 映射后端数据格式到前端
    tableData.value = (data.records || data.list || []).map(item => ({
      artworkId: item.id,
      title: item.title,
      artistName: item.artistName,  // 后端返回 artistName
      cover: item.cover,             // 后端返回 cover
      categoryId: item.categoryId,
      categoryName: item.category,  // 后端返回 category
      artType: item.artType,        // 画种
      size: item.size,               // 尺寸
      year: item.year,              // 创作年份
      price: item.price ? item.price / 100 : 0,  // 分转元
      originalPrice: item.originalPrice ? item.originalPrice / 100 : 0,
      ownershipType: item.ownershipType || 1,
      artworkCode: item.artworkCode,
      status: item.status,
      weight: item.weight || 0,
      description: item.description,
      salesCount: item.salesCount || 0,
      favoriteCount: item.favoriteCount || 0,
      priceRise: item.priceRise || 0, // 价格增长率
      createTime: item.createTime,
      distributionEnabled: item.distributionEnabled || false,
      commissionRate: item.commissionRate || 10,
      distributionOrders: item.distributionOrders || 0,
      distributionEarnings: item.distributionEarnings || 0,
      distributionUsers: item.distributionUsers || 0
    }))
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载数据失败:', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await requestApi.get('/product/categories')
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
    artType: row.artType || '',
    size: row.size || '',
    year: row.year || null,
    cover: row.cover || '',
    price: row.price,
    originalPrice: row.originalPrice || 0,
    ownershipType: row.ownershipType || 1,
    description: row.description || '',
    status: row.status
  })
  editVisible.value = true
}

const handleAdd = () => {
  // 重置表单
  Object.assign(editForm, {
    artworkId: '',
    title: '',
    artistName: '',
    categoryId: '',
    artType: '',
    size: '',
    year: null,
    cover: '',
    price: 0,
    originalPrice: 0,
    ownershipType: 1,
    description: '',
    status: 1
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
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res = await requestApi.post('/product/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    editForm.cover = res.url
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败：' + (e.message || '未知错误'))
  }
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  saveLoading.value = true
  try {
    const params = {
      title: editForm.title,
      authorName: editForm.artistName,
      categoryId: editForm.categoryId ? Number(editForm.categoryId) : null,
      artType: editForm.artType || null,
      size: editForm.size || null,
      year: editForm.year || null,
      cover: editForm.cover || null,
      price: editForm.price != null ? Number(editForm.price) : null,
      originalPrice: editForm.originalPrice != null ? Number(editForm.originalPrice) : null,
      ownershipType: editForm.ownershipType || 1,
      description: editForm.description,
      status: editForm.status
    }
    console.log('保存参数:', params)
    
    if (editForm.artworkId) {
      // 更新作品
      params.id = Number(editForm.artworkId)
      const res = await requestApi.put('/product/update', params)
      console.log('更新结果:', res)
      ElMessage.success('更新成功')
    } else {
      // 新增作品
      const res = await requestApi.post('/product/create', params)
      console.log('创建结果:', res)
      ElMessage.success('创建成功')
    }
    
    // 成功后刷新列表
    loadData()
    editVisible.value = false
  } catch (e) {
    console.error('保存失败详情:', e)
    const msg = e.response?.data?.message || e.message || '未知错误'
    ElMessage.error('保存失败：' + msg)
  } finally {
    saveLoading.value = false
  }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  console.log('切换状态:', { artworkId: row.artworkId, currentStatus: row.status, newStatus })
  try {
    await ElMessageBox.confirm(`确定要${action}该作品吗？`, '提示', { type: 'warning' })
    console.log('确认后调用API...')
    // 调用后端 API 更新状态
    const res = await requestApi.put('/product/update', {
      id: Number(row.artworkId),
      status: newStatus
    })
    console.log('API响应:', res)
    // 成功后刷新列表获取最新数据
    loadData()
    ElMessage.success(`${action}成功`)
  } catch (e) {
    console.error('操作失败:', e)
    const msg = e.response?.data?.message || e.message || '操作失败'
ElMessage.error(msg)
  }
}

// 权重变更
const handleWeightChange = async (row) => {
  try {
    await requestApi.put('/product/update', {
      id: Number(row.artworkId),
      weight: row.weight || 0
    })
    // 刷新列表
    loadData()
  } catch (e) {
    ElMessage.error('权重保存失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该作品吗？', '提示', { type: 'warning' })
    await requestApi.delete(`/product/${row.artworkId}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '删除失败'
    ElMessage.error(msg)
  }
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
    console.log('分销保存参数:', distForm.artworkId, distForm.distributionEnabled, distForm.commissionRate)
    // 调用后端 API 保存分销设置
    await requestApi.put('/product/update', {
      id: Number(distForm.artworkId),
      distributionEnabled: distForm.distributionEnabled,
      commissionRate: distForm.commissionRate
    })
    // 保存成功后刷新列表
    loadData()
    ElMessage.success('分销设置已保存')
    distVisible.value = false
  } catch (e) {
    console.error('分销保存失败:', e)
    const msg = e.response?.data?.message || e.message || '未知错误'
    ElMessage.error('保存失败：' + msg)
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
  
  .cover-wrapper {
    position: relative;
    cursor: pointer;
    
    &:hover .edit-overlay {
      opacity: 1;
    }
    
    .edit-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 20px;
      opacity: 0;
      transition: opacity 0.2s;
      border-radius: 4px;
    }
  }
  
  .detail {
    .title {
      font-weight: 500;
      margin-bottom: 4px;
      cursor: pointer;
      &:hover {
        color: #409eff;
      }
    }
    .artist {
      font-size: 13px;
      color: #666;
      cursor: pointer;
      &:hover {
        color: #409eff;
      }
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

.art-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  
  .art-type {
    color: #409eff;
    font-weight: 500;
  }
  .art-size {
    color: #666;
  }
  .art-year {
    color: #999;
  }
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

.artwork-code {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #409eff;
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
