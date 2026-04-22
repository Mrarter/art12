<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">Banner管理</span>
      <el-button type="primary" @click="showDialog('add')">添加Banner</el-button>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="图片" width="180">
        <template #default="{ row }">
          <div class="banner-image-wrapper" @click="handleImageClick(row)">
            <el-image 
              v-if="row.imageUrl" 
              :src="getFullImageUrl(row.imageUrl)" 
              style="width: 150px; height: 75px" 
              fit="cover" 
              :preview-src-list="[getFullImageUrl(row.imageUrl)]"
              preview-teleported
              @error="row._imgError = true"
            >
              <template #error>
                <div class="no-image" style="width: 150px; height: 75px">
                  <el-icon><Picture /></el-icon>
                  <span>无图片</span>
                </div>
              </template>
            </el-image>
            <div v-else class="no-image" @click="handleImageClick(row)">
              <el-icon><Plus /></el-icon>
              <span>点击上传</span>
            </div>
            <div class="image-overlay">
              <el-icon><Edit /></el-icon>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="标题" width="150">
        <template #default="{ row }">
          <el-input 
            v-if="editingTitle === row.id" 
            v-model="row.title" 
            size="small" 
            @blur="saveTitle(row)" 
            @keyup.enter="saveTitle(row)"
            placeholder="请输入标题"
          />
          <span v-else class="editable-text" @click="editingTitle = row.id">{{ row.title || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.linkType === 'home' ? '首页' : row.linkType === 'gallery' ? '作品详情' : row.linkType === 'auction' ? '拍卖' : '其他' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跳转链接" min-width="180">
        <template #default="{ row }">
          <p class="link">{{ row.linkValue || '-' }}</p>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch 
            v-model="row.status" 
            :active-value="1" 
            :inactive-value="0" 
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog('edit', row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- Banner 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        
        <!-- 图片上传区域 -->
        <el-form-item label="图片" prop="imageUrl">
          <div class="upload-area" @click="triggerUpload">
            <el-image 
              v-if="form.imageUrl" 
              :src="getFullImageUrl(form.imageUrl)" 
              class="upload-preview"
              fit="cover"
            />
            <div v-else class="upload-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <span>点击上传图片</span>
            </div>
            <div v-if="form.imageUrl" class="upload-mask">
              <el-icon><Edit /></el-icon>
              <span>更换图片</span>
            </div>
          </div>
          <input 
            ref="fileInput" 
            type="file" 
            accept="image/*" 
            style="display: none" 
            @change="handleFileChange"
          />
          <div class="upload-tip">支持 JPG、PNG 格式，文件大小不超过 10MB</div>
          <div v-if="uploading" class="upload-progress">
            <el-progress :percentage="uploadProgress" />
          </div>
        </el-form-item>
        
        <el-form-item label="类型" prop="linkType">
          <el-select v-model="form.linkType" placeholder="请选择类型">
            <el-option label="首页" value="home" />
            <el-option label="作品详情" value="gallery" />
            <el-option label="拍卖专场" value="auction" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转值" prop="linkValue">
          <el-input v-model="form.linkValue" placeholder="请输入跳转链接或ID" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { Plus, Edit, Picture } from '@element-plus/icons-vue'
import request from '@/api/request'
import { uploadFile, getFullImageUrl as getUrl } from '@/api/request'

const getFullImageUrl = getUrl

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const fileInput = ref()
const tableData = ref([])
const isEdit = ref(false)
const editingTitle = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)

const form = reactive({
  id: null,
  title: '',
  imageUrl: '',
  linkType: 'home',
  linkValue: '',
  sort: 0,
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
  linkType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑Banner' : '添加Banner')

const loadData = async () => {
  loading.value = true
  try {
    const data = await request.get('/system/banner/list')
    // 转换后端数据格式到前端
    tableData.value = (data || []).map(item => ({
      id: item.id,
      title: item.title,
      imageUrl: item.imageUrl,
      linkType: item.type === 'BANNER' ? 'home' : item.type === 'ARTIST' ? 'gallery' : item.type === 'AUCTION' ? 'auction' : 'other',
      linkValue: item.target || '',
      sort: item.sortNo || 0,
      status: item.status === 'ENABLED' ? 1 : 0
    }))
  } catch (e) {
    console.error('加载Banner列表失败:', e)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

// 点击图片快速编辑
const handleImageClick = (row) => {
  showDialog('edit', row)
}

// 触发文件上传
const triggerUpload = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  // 验证文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    return
  }

  uploading.value = true
  uploadProgress.value = 0

  try {
    const result = await uploadFile(file, (percent) => {
      uploadProgress.value = percent
    })
    // result 可能是 {url: 'xxx'} 或直接是 'xxx'
    form.imageUrl = result?.url || result || ''
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error(e.message || '图片上传失败')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    // 清空 input 值，允许重复选择同一文件
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

// 保存标题（点击即编辑）
const saveTitle = async (row) => {
  editingTitle.value = null
  if (!row.title?.trim()) {
    ElMessage.warning('标题不能为空')
    row.title = row.title?.trim() || ''
    return
  }
  try {
    await request.put(`/system/banner/${row.id}`, {
      title: row.title,
      imageUrl: row.imageUrl,
      type: row.linkType === 'home' ? 'BANNER' : row.linkType === 'gallery' ? 'ARTIST' : row.linkType === 'auction' ? 'AUCTION' : 'OTHER',
      target: row.linkValue,
      sortNo: row.sort,
      status: row.status === 1 ? 'ENABLED' : 'DISABLED'
    })
    ElMessage.success('标题更新成功')
  } catch (e) {
    if (e.message !== 'backend_offline') {
      ElMessage.error('标题更新失败')
    }
  }
}

// 修改状态
const handleStatusChange = async (row) => {
  try {
    await request.put(`/system/banner/${row.id}/status`, {
      status: row.status === 1 ? 'ENABLED' : 'DISABLED'
    })
    ElMessage.success('状态更新成功')
  } catch (e) {
    if (e.message !== 'backend_offline') {
      ElMessage.error('状态更新失败')
    }
  }
}

const showDialog = (type, row = null) => {
  if (type === 'add') {
    isEdit.value = false
    Object.assign(form, { id: null, title: '', imageUrl: '', linkType: 'home', linkValue: '', sort: 0, status: 1 })
  } else {
    isEdit.value = true
    Object.assign(form, { 
      id: row.id, 
      title: row.title || '', 
      imageUrl: row.imageUrl || '', 
      linkType: row.linkType || 'home', 
      linkValue: row.linkValue || '', 
      sort: row.sort || 0, 
      status: row.status ?? 1 
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  // 转换前端格式到后端格式
  const params = {
    title: form.title,
    imageUrl: form.imageUrl,
    type: form.linkType === 'home' ? 'BANNER' : form.linkType === 'gallery' ? 'ARTIST' : form.linkType === 'auction' ? 'AUCTION' : 'OTHER',
    target: form.linkValue,
    sortNo: form.sort,
    status: form.status === 1 ? 'ENABLED' : 'DISABLED'
  }
  
  try {
    if (isEdit.value) {
      await request.put(`/system/banner/${form.id}`, params)
      ElMessage.success('更新成功')
    } else {
      await request.post('/system/banner', params)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()  // 刷新列表
  } catch (e) {
    if (e.message !== 'backend_offline') {
      ElMessage.error('操作失败: ' + (e.message || ''))
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该Banner吗？', '提示', { type: 'warning' })
    await request.delete(`/system/banner/${row.id}`)
    ElMessage.success('删除成功')
    loadData()  // 刷新列表
  } catch (e) {
    if (e.message !== 'backend_offline') {
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
.link {
  color: #409eff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 图片相关样式 */
.banner-image-wrapper {
  position: relative;
  width: 150px;
  height: 75px;
  cursor: pointer;
  border-radius: 4px;
  overflow: hidden;
}
.banner-image-wrapper :deep(.el-image) {
  width: 100%;
  height: 100%;
}
.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  color: #909399;
  font-size: 12px;
  transition: all 0.3s;
}
.no-image:hover {
  border-color: #409eff;
  color: #409eff;
}
.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  opacity: 0;
  transition: opacity 0.3s;
}
.banner-image-wrapper:hover .image-overlay {
  opacity: 1;
}

/* 可编辑文字 */
.editable-text {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}
.editable-text:hover {
  background: #f5f7fa;
}

/* 上传区域样式 */
.upload-area {
  position: relative;
  width: 240px;
  height: 120px;
  cursor: pointer;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  transition: border-color 0.3s;
}
.upload-area:hover {
  border-color: #409eff;
}
.upload-preview {
  width: 100%;
  height: 100%;
}
.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  color: #909399;
}
.upload-icon {
  font-size: 28px;
  margin-bottom: 8px;
}
.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s;
}
.upload-area:hover .upload-mask {
  opacity: 1;
}
.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.upload-progress {
  margin-top: 10px;
  width: 240px;
}
</style>
