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
      <el-table-column prop="sessionCode" label="专场编号" width="200">
        <template #default="{ row }">
          <div class="id-cell" @click="handleCopyId(row.sessionCode)">
            <span class="id-text">{{ row.sessionCode || '-' }}</span>
            <el-icon class="copy-icon"><DocumentCopy /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="专场名称" min-width="200" />
      <el-table-column label="封面" width="120">
        <template #default="{ row }">
          <el-image :src="row.cover" style="width: 80px; height: 60px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="时间" width="180">
        <template #default="{ row }">
          <p>{{ row.auctionStart || row.startTime }}</p>
          <p>至 {{ row.auctionEnd || row.endTime }}</p>
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
        
        <!-- 封面图上传 -->
        <el-form-item label="封面图" prop="cover">
          <div class="upload-area" @click="triggerCoverUpload">
            <el-image 
              v-if="form.cover" 
              :src="getFullImageUrl(form.cover)" 
              class="upload-preview"
              fit="cover"
            />
            <div v-else class="upload-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <span>点击上传封面图</span>
            </div>
            <div v-if="form.cover" class="upload-mask">
              <el-icon><Edit /></el-icon>
              <span>更换图片</span>
            </div>
          </div>
          <input 
            ref="coverFileInput" 
            type="file" 
            accept="image/*" 
            style="display: none" 
            @change="handleCoverChange"
          />
          <div class="upload-tip">支持 JPG、PNG 格式，文件大小不超过 10MB</div>
          <div v-if="uploading" class="upload-progress">
            <el-progress :percentage="uploadProgress" />
          </div>
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
          <div class="description-editor">
            <div class="description-preview" v-if="form.description" v-html="form.description"></div>
            <el-input 
              v-else 
              placeholder="点击添加专场描述，支持富文本格式"
              disabled
              style="cursor: pointer"
            />
            <el-button type="primary" link @click="showDescriptionDialog" style="margin-top: 8px">
              {{ form.description ? '编辑描述' : '添加描述' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 描述编辑对话框 -->
    <el-dialog v-model="descDialogVisible" title="编辑专场描述" width="800px" destroy-on-close>
      <div class="desc-editor-container">
        <div class="editor-toolbar">
          <el-button-group>
            <el-button size="small" @click="insertTag('b')"><b>B</b></el-button>
            <el-button size="small" @click="insertTag('i')"><i>I</i></el-button>
            <el-button size="small" @click="insertTag('u')"><u>U</u></el-button>
            <el-button size="small" @click="insertTag('br')">换行</el-button>
          </el-button-group>
          <el-button size="small" @click="insertTag('p')">段落</el-button>
        </div>
        <el-input
          ref="descTextarea"
          v-model="descContent"
          type="textarea"
          :rows="12"
          placeholder="请输入专场描述..."
          @keydown.ctrl.enter="saveDescription"
        />
        <div class="editor-tip">支持 HTML 标签：&lt;b&gt;加粗&lt;/b&gt;、&lt;i&gt;斜体&lt;/i&gt;、&lt;u&gt;下划线&lt;/u&gt;、&lt;br&gt;换行</div>
      </div>
      <template #footer>
        <el-button @click="descDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDescription">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, DocumentCopy } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { uploadFile, getFullImageUrl as getUrl } from '@/api/request'
import { copyId } from '@/utils/id'

const getFullImageUrl = getUrl
const router = useRouter()

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const coverFileInput = ref()
const tableData = ref([])
const isEdit = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)

// 描述编辑相关
const descDialogVisible = ref(false)
const descContent = ref('')
const descTextarea = ref()

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
  // 支持数值和字符串状态
  if (typeof status === 'number') {
    const map = { 1: 'primary', 2: 'success', 3: 'info' }
    return map[status] || 'info'
  }
  const map = { preview: 'primary', ongoing: 'success', ended: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  // 支持数值和字符串状态
  if (typeof status === 'number') {
    const map = { 1: '预展中', 2: '拍卖中', 3: '已结束' }
    return map[status] || status
  }
  const map = { preview: '预展中', ongoing: '进行中', ended: '已结束' }
  return map[status] || status
}

// 复制专场编号
const handleCopyId = async (id) => {
  if (!id) {
    ElMessage.warning('专场编号为空')
    return
  }
  copyId(id,
    () => ElMessage.success('已复制专场编号'),
    () => ElMessage.error('复制失败')
  )
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.status) params.status = searchForm.status
    const data = await request.get('/auction/sessions', { params })
    
    // 映射API字段到前端期望的字段
    tableData.value = (data.records || data.list || []).map(item => ({
      sessionId: item.id,
      sessionCode: item.sessionCode || `SES${String(item.id).padStart(10, '0')}`,
      name: item.title || item.name,
      cover: item.cover || item.coverImage,
      previewStart: item.previewStart || item.startTime,
      previewEnd: item.previewEnd || item.endTime,
      auctionStart: item.auctionStart || item.startTime,
      auctionEnd: item.auctionEnd || item.endTime,
      status: item.statusText?.toLowerCase() || (item.status === 1 ? 'preview' : item.status === 2 ? 'ongoing' : 'ended'),
      totalAmount: item.totalAmount || 0,
      totalLots: item.totalLots || item.lotCount || 0
    }))
    pagination.total = data.total || 0
  } catch (e) {
    tableData.value = [
      { sessionId: 'S001', sessionCode: 'SES202604240001M5K8', name: '2024春季艺术品拍卖会', cover: '', previewStart: '2024-02-01 09:00', previewEnd: '2024-02-05 18:00', auctionStart: '2024-02-06 10:00', auctionEnd: '2024-02-06 18:00', status: 'ended', totalAmount: 2580000 },
      { sessionId: 'S002', sessionCode: 'SES202604250001A3F2', name: '当代艺术专场', cover: '', previewStart: '2024-03-01 09:00', previewEnd: '2024-03-05 18:00', auctionStart: '2024-03-06 10:00', auctionEnd: '2024-03-06 18:00', status: 'ongoing', totalAmount: 0 },
      { sessionId: 'S003', sessionCode: 'SES202604250002W7N9', name: '书画精品专场', cover: '', previewStart: '2024-03-15 09:00', previewEnd: '2024-03-20 18:00', auctionStart: '2024-03-21 10:00', auctionEnd: '2024-03-21 18:00', status: 'preview', totalAmount: 0 }
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

// 封面图上传相关
const triggerCoverUpload = () => {
  coverFileInput.value?.click()
}

const handleCoverChange = async (e) => {
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
    form.cover = result?.url || result || ''
    ElMessage.success('封面上传成功')
  } catch (e) {
    ElMessage.error(e.message || '封面上传失败')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    if (coverFileInput.value) {
      coverFileInput.value.value = ''
    }
  }
}

// 描述编辑相关
const showDescriptionDialog = () => {
  descContent.value = form.description || ''
  descDialogVisible.value = true
}

const insertTag = (tag) => {
  const textarea = descTextarea.value?.$el?.querySelector('textarea')
  if (!textarea) return
  
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = descContent.value.substring(start, end)
  
  let insertText = ''
  if (tag === 'br') {
    insertText = '<br>'
  } else if (tag === 'p') {
    insertText = '\n<p></p>\n'
  } else {
    insertText = `<${tag}>${selectedText}</${tag}>`
  }
  
  descContent.value = descContent.value.substring(0, start) + insertText + descContent.value.substring(end)
}

const saveDescription = () => {
  form.description = descContent.value
  descDialogVisible.value = false
  if (descContent.value) {
    ElMessage.success('描述已保存')
  }
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

/* 封面上传样式 */
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

/* 描述编辑器样式 */
.description-editor {
  .description-preview {
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    min-height: 60px;
    max-height: 150px;
    overflow: auto;
    margin-bottom: 8px;
  }
}

.desc-editor-container {
  .editor-toolbar {
    margin-bottom: 12px;
    padding: 8px;
    background: #f5f7fa;
    border-radius: 4px;
  }
  .editor-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
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
</style>
