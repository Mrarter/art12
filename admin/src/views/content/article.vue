<template>
  <div class="article-page">
    <div class="page-header">
      <div>
        <h2>文章发布</h2>
        <p>维护 DAY DAY ART、专题导读和运营内容</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openEditor()">新建文章</el-button>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        class="keyword-input"
        clearable
        placeholder="搜索标题、摘要、标签"
        :prefix-icon="Search"
        @keyup.enter="loadArticles"
        @clear="loadArticles"
      />
      <el-select v-model="query.status" clearable placeholder="状态" @change="loadArticles">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="定时发布" value="SCHEDULED" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已下架" value="OFFLINE" />
      </el-select>
      <el-select v-model="query.category" clearable placeholder="栏目" @change="loadArticles">
        <el-option label="DAY DAY ART" value="APPRECIATION" />
        <el-option label="专题策展" value="CURATION" />
        <el-option label="艺术家故事" value="ARTIST" />
        <el-option label="平台公告" value="NOTICE" />
      </el-select>
      <el-button :icon="Refresh" @click="loadArticles">刷新</el-button>
    </div>

    <el-table :data="articles" v-loading="loading" border stripe>
      <el-table-column label="文章" min-width="340">
        <template #default="{ row }">
          <div class="article-cell">
            <el-image
              v-if="row.coverImage"
              :src="getArticleCoverThumbnailUrl(row.coverImage)"
              class="cover"
              fit="cover"
              lazy
              preview-teleported
              :preview-src-list="[getFullImageUrl(row.coverImage)]"
            >
              <template #error>
                <div class="cover-empty"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="cover-empty"><el-icon><Picture /></el-icon></div>
            <div class="article-meta">
              <button
                type="button"
                class="article-title"
                :title="`预览：${row.title}`"
                @click="openPreview(row)"
              >
                {{ row.title }}
              </button>
              <span>{{ row.subtitle || row.summary || '未填写摘要' }}</span>
              <div class="tag-row">
                <el-tag size="small" effect="plain">{{ categoryLabel(row.category) }}</el-tag>
                <el-tag v-for="tag in splitTags(row.tags)" :key="tag" size="small" type="info" effect="plain">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="author" label="作者" width="120">
        <template #default="{ row }">{{ row.author || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortNo" label="排序" width="90" />
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEditor(row)" :loading="editingId === row.id">编辑</el-button>
          <el-button type="info" link @click="openPublishTimeEditor(row)">改时间</el-button>
          <el-button type="success" link v-if="row.status !== 'PUBLISHED'" @click="changeStatus(row, 'PUBLISHED')">
            发布
          </el-button>
          <el-button type="warning" link v-if="row.status === 'PUBLISHED'" @click="changeStatus(row, 'OFFLINE')">
            下架
          </el-button>
          <el-button type="info" link @click="openPreview(row)">预览</el-button>
          <el-button type="danger" link @click="deleteArticle(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadArticles"
        @current-change="loadArticles"
      />
    </div>

    <el-dialog v-model="editorVisible" :title="editorTitle" width="1120px" destroy-on-close>
      <div class="editor-grid" v-loading="editorLoading">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="请输入文章标题" />
          </el-form-item>
          <el-form-item label="副标题">
            <el-input v-model="form.subtitle" maxlength="255" show-word-limit placeholder="用于文章头部说明" />
          </el-form-item>
          <el-form-item label="作者">
            <el-input v-model="form.author" placeholder="如：李小璐" />
          </el-form-item>
          <el-form-item label="栏目">
            <el-select v-model="form.category">
              <el-option label="DAY DAY ART" value="APPRECIATION" />
              <el-option label="专题策展" value="CURATION" />
              <el-option label="艺术家故事" value="ARTIST" />
              <el-option label="平台公告" value="NOTICE" />
            </el-select>
          </el-form-item>
          <el-form-item label="封面">
            <div class="upload-area" @click="triggerUpload">
              <el-image v-if="form.coverImage" :src="getFullImageUrl(form.coverImage)" fit="cover">
                <template #error>
                  <div class="upload-placeholder image-error">
                    <el-icon><Picture /></el-icon>
                    <span>图片加载失败</span>
                  </div>
                </template>
              </el-image>
              <div v-else class="upload-placeholder">
                <el-icon><Plus /></el-icon>
                <span>上传封面</span>
              </div>
              <div class="upload-mask">
                <el-icon><Upload /></el-icon>
                <span>{{ form.coverImage ? '更换封面并调整取景' : '选择图片并设置封面取景' }}</span>
              </div>
            </div>
            <input ref="fileInput" type="file" accept="image/*" class="file-input" @change="handleFileChange" />
            <div class="upload-help">建议 16:9 或 4:3 图片，上传后可继续选择封面显示区域。</div>
            <el-progress v-if="uploading" :percentage="uploadProgress" class="upload-progress" />
          </el-form-item>
          <el-form-item label="正文展示图">
            <div class="upload-area detail-image-area" @click="triggerBodyUpload">
              <el-image v-if="form.bodyImage" :src="getFullImageUrl(form.bodyImage)" fit="contain">
                <template #error>
                  <div class="upload-placeholder image-error">
                    <el-icon><Picture /></el-icon>
                    <span>图片加载失败</span>
                  </div>
                </template>
              </el-image>
              <div v-else class="upload-placeholder">
                <el-icon><Upload /></el-icon>
                <span>上传正文完整图</span>
              </div>
              <div class="upload-mask">
                <el-icon><Upload /></el-icon>
                <span>{{ form.bodyImage ? '更换正文展示图' : '上传文章详情页完整图片' }}</span>
              </div>
            </div>
            <input ref="bodyFileInput" type="file" accept="image/*" class="file-input" @change="handleBodyFileChange" />
            <div class="upload-help">文章详情页顶部显示完整图片；若不单独上传，默认使用封面上传时的原图。</div>
            <el-progress v-if="bodyUploading" :percentage="bodyUploadProgress" class="upload-progress" />
          </el-form-item>
          <el-form-item label="图片地址">
            <el-input
              v-model="form.coverImage"
              clearable
              placeholder="可填写 https:// 开头的封面图片地址，或点击上方上传"
            />
          </el-form-item>
          <el-form-item label="正文图片地址">
            <el-input
              v-model="form.bodyImage"
              clearable
              placeholder="可填写 https:// 开头的正文完整图片地址，或点击上方上传"
            />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input
              v-model="form.summary"
              type="textarea"
              maxlength="500"
              show-word-limit
              :rows="3"
              placeholder="一句话概括文章重点"
            />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="form.tags" placeholder="用逗号分隔，如：油画,肖像,19世纪" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortNo" :min="0" :max="9999" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio-button label="DRAFT">草稿</el-radio-button>
              <el-radio-button label="SCHEDULED">定时发布</el-radio-button>
              <el-radio-button label="PUBLISHED">发布</el-radio-button>
              <el-radio-button label="OFFLINE">下架</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.status === 'SCHEDULED'" label="发布时间" prop="scheduledDate">
            <div class="schedule-row">
              <el-date-picker
                v-model="form.scheduledDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择发布日期"
                :disabled-date="disablePastDates"
              />
              <el-select v-model="form.scheduledTime" class="schedule-time-select" placeholder="选择时间">
                <el-option
                  v-for="item in scheduledTimeOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
              <span class="schedule-help">发布时刻可直接下拉选择</span>
            </div>
          </el-form-item>
          <el-form-item label="正文" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="12"
              maxlength="12000"
              show-word-limit
              placeholder="输入正文。段落之间用空行分隔，预览会自动分段。"
            />
          </el-form-item>
        </el-form>

        <div class="preview-panel">
          <span class="preview-label">实时预览</span>
          <div v-if="previewHeroImage" class="preview-cover preview-body-image">
            <img :src="previewHeroImage" alt="" />
          </div>
          <h3>{{ form.title || '未命名文章' }}</h3>
          <p class="preview-subtitle">{{ form.subtitle || form.summary || '这里会显示文章副标题或摘要。' }}</p>
          <div class="preview-info">
            <span>{{ categoryLabel(form.category) }}</span>
            <span>{{ form.author || '未署名' }}</span>
          </div>
          <p v-for="(paragraph, index) in previewParagraphs" :key="index">{{ paragraph }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button @click="saveAsDraft">存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="submitArticle">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="文章预览" width="760px">
      <article class="read-preview">
        <img v-if="previewArticleHeroImage" :src="previewArticleHeroImage" alt="" />
        <span>{{ categoryLabel(previewArticle.category) }}</span>
        <h1>{{ previewArticle.title }}</h1>
        <h2 v-if="previewArticle.subtitle">{{ previewArticle.subtitle }}</h2>
        <div class="read-meta">
          {{ previewArticle.author || '未署名' }} · {{ formatTime(previewArticle.publishTime || previewArticle.updateTime) }}
        </div>
        <p v-if="previewArticle.summary" class="summary">{{ previewArticle.summary }}</p>
        <p v-for="(paragraph, index) in articleParagraphs(previewArticle.content)" :key="index">{{ paragraph }}</p>
      </article>
    </el-dialog>

    <el-dialog v-model="publishTimeDialogVisible" title="修改发布时间" width="420px">
      <el-form label-width="88px">
        <el-form-item label="文章标题">
          <div class="publish-time-title">{{ publishTimeArticle.title || '-' }}</div>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="publishTimeForm.publishTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择发布时间"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishTimeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishTimeSubmitting" @click="submitPublishTime">保存</el-button>
      </template>
    </el-dialog>

    <ImageCropper
      :visible="cropperVisible"
      :file="cropperFile"
      :aspect-ratio="16 / 9"
      @close="handleCropperClose"
      @confirm="handleCropperConfirm"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import ImageCropper from '@/components/ImageCropper.vue'
import request from '@/api/request'
import { getFullImageUrl as resolveImageUrl, getImageThumbnailUrl, uploadFile } from '@/api/request'

const getFullImageUrl = resolveImageUrl

const loading = ref(false)
const submitting = ref(false)
const editorLoading = ref(false)
const editingId = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const bodyUploading = ref(false)
const bodyUploadProgress = ref(0)
const editorVisible = ref(false)
const previewVisible = ref(false)
const cropperVisible = ref(false)
const publishTimeDialogVisible = ref(false)
const formRef = ref()
const fileInput = ref()
const bodyFileInput = ref()
const cropperFile = ref(null)
const cropperSourceFile = ref(null)
const articles = ref([])
const total = ref(0)
const previewArticle = ref({})
const publishTimeSubmitting = ref(false)
const publishTimeArticle = ref({})

const publishTimeForm = reactive({
  id: null,
  publishTime: ''
})

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: '',
  category: ''
})

const emptyForm = () => ({
  id: null,
  title: '',
  subtitle: '',
  author: '李小璐',
  coverImage: '',
  coverOriginalImage: '',
  bodyImage: '',
  category: 'APPRECIATION',
  summary: '',
  content: '',
  tags: '',
  sortNo: 0,
  status: 'DRAFT',
  scheduledDate: '',
  scheduledTime: '06:00'
})

const form = reactive(emptyForm())

const rules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文章正文', trigger: 'blur' }],
  scheduledDate: [{
    validator: (_rule, value, callback) => {
      if (form.status !== 'SCHEDULED') {
        callback()
        return
      }
      if (!String(value || '').trim()) {
        callback(new Error('请选择定时发布日期'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

const editorTitle = computed(() => (form.id ? '编辑文章' : '新建文章'))

const previewParagraphs = computed(() => {
  const paragraphs = articleParagraphs(form.content)
  return paragraphs.length ? paragraphs : ['正文预览会在这里自动分段显示。']
})

const previewHeroImage = computed(() => getFullImageUrl(form.bodyImage || form.coverOriginalImage || form.coverImage))

const previewArticleHeroImage = computed(() => getFullImageUrl(
  previewArticle.value?.bodyImage || previewArticle.value?.coverOriginalImage || previewArticle.value?.coverImage
))

const scheduledTimeOptions = Array.from({ length: 48 }, (_value, index) => {
  const hour = String(Math.floor(index / 2)).padStart(2, '0')
  const minute = index % 2 === 0 ? '00' : '30'
  return `${hour}:${minute}`
})

const getArticleCoverThumbnailUrl = (coverImage) => {
  const source = getFullImageUrl(coverImage)
  if (!source) return ''
  return getImageThumbnailUrl(source, 360)
}

const disablePastDates = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

const formatDateOnly = (value) => {
  if (!value) return ''
  return String(value).slice(0, 10)
}

const formatTimeOnly = (value) => {
  if (!value) return '06:00'
  const text = String(value)
  const match = text.match(/(\d{2}):(\d{2})/)
  return match ? `${match[1]}:${match[2]}` : '06:00'
}

const buildScheduledPublishTime = (scheduledDate, scheduledTime) => {
  const dateText = String(scheduledDate || '').trim()
  const timeText = formatTimeOnly(scheduledTime)
  return dateText ? `${dateText}T${timeText}:00` : null
}

const normalizeArticleTitle = (title) => String(title || '').replace(/每日鉴赏/g, 'DAY DAY ART')

const normalizeEditorForm = (row = {}) => {
  const normalized = { ...emptyForm(), ...row }
  normalized.title = normalizeArticleTitle(normalized.title)
  normalized.scheduledDate = normalized.status === 'SCHEDULED'
    ? formatDateOnly(normalized.publishTime)
    : ''
  normalized.scheduledTime = normalized.status === 'SCHEDULED'
    ? formatTimeOnly(normalized.publishTime)
    : '06:00'
  return normalized
}

const loadArticles = async () => {
  loading.value = true
  try {
    const data = await request.get('/content/article/list', { params: { ...query } })
    articles.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (e) {
    if (e.message !== 'backend_offline') {
      ElMessage.error('文章列表加载失败')
    }
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const openEditor = async (row = null) => {
  Object.assign(form, normalizeEditorForm(row || {}))
  editorVisible.value = true
  if (!row?.id) return

  editorLoading.value = true
  editingId.value = row.id
  try {
    const detail = await request.get(`/content/article/${row.id}`)
    Object.assign(form, normalizeEditorForm(detail || row))
  } catch (e) {
    if (e.message !== 'backend_offline') {
      ElMessage.error('文章详情加载失败，已使用列表数据')
    }
  } finally {
    editorLoading.value = false
    editingId.value = null
  }
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const triggerBodyUpload = () => {
  bodyFileInput.value?.click()
}

const handleFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    event.target.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    event.target.value = ''
    return
  }

  cropperFile.value = file
  cropperSourceFile.value = file
  cropperVisible.value = true
  event.target.value = ''
}

const handleCropperClose = () => {
  cropperVisible.value = false
  cropperFile.value = null
  cropperSourceFile.value = null
}

const handleCropperConfirm = async (file) => {
  cropperVisible.value = false
  uploading.value = true
  uploadProgress.value = 0
  try {
    const originalResult = await uploadFile(cropperSourceFile.value || file, (percent) => {
      uploadProgress.value = Math.round(percent * 0.5)
    })
    const result = await uploadFile(file, (percent) => {
      uploadProgress.value = 50 + Math.round(percent * 0.5)
    })
    form.coverOriginalImage = originalResult?.url || originalResult || ''
    form.coverImage = result?.url || result || ''
    if (!String(form.bodyImage || '').trim()) {
      form.bodyImage = form.coverOriginalImage
    }
    ElMessage.success('封面上传成功')
  } catch (e) {
    ElMessage.error(e.message || '封面上传失败')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    cropperFile.value = null
    cropperSourceFile.value = null
  }
}

const handleBodyFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    event.target.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    event.target.value = ''
    return
  }

  bodyUploading.value = true
  bodyUploadProgress.value = 0
  try {
    const result = await uploadFile(file, (percent) => {
      bodyUploadProgress.value = Math.round(percent)
    })
    form.bodyImage = result?.url || result || ''
    ElMessage.success('正文展示图上传成功')
  } catch (e) {
    ElMessage.error(e.message || '正文展示图上传失败')
  } finally {
    bodyUploading.value = false
    bodyUploadProgress.value = 0
    event.target.value = ''
  }
}

const saveAsDraft = async () => {
  form.status = 'DRAFT'
  form.scheduledDate = ''
  form.scheduledTime = '06:00'
  await submitArticle()
}

const submitArticle = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  const payload = {
    ...form,
    title: normalizeArticleTitle(form.title),
    tags: normalizeTags(form.tags),
    publishTime: form.status === 'SCHEDULED'
      ? buildScheduledPublishTime(form.scheduledDate, form.scheduledTime)
      : form.status === 'PUBLISHED'
        ? (form.publishTime || null)
        : null
  }
  try {
    if (form.id) {
      await request.put(`/content/article/${form.id}`, payload)
      ElMessage.success('文章已更新')
    } else {
      await request.post('/content/article', payload)
      ElMessage.success(
        form.status === 'PUBLISHED'
          ? '文章已发布'
          : form.status === 'SCHEDULED'
            ? '文章已加入定时发布'
            : '文章已保存'
      )
    }
    editorVisible.value = false
    loadArticles()
  } catch (e) {
    if (e.message !== 'backend_offline') {
      ElMessage.error('保存失败')
    }
  } finally {
    submitting.value = false
  }
}

const changeStatus = async (row, status) => {
  const text = status === 'PUBLISHED' ? '发布' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${text}《${row.title}》吗？`, '提示', { type: 'warning' })
    await request.put(`/content/article/${row.id}/status`, { status })
    ElMessage.success(`${text}成功`)
    loadArticles()
  } catch (e) {
    if (e !== 'cancel' && e.message !== 'backend_offline') {
      ElMessage.error(`${text}失败`)
    }
  }
}

const deleteArticle = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除《${row.title}》吗？删除后不可恢复。`, '提示', { type: 'warning' })
    await request.delete(`/content/article/${row.id}`)
    ElMessage.success('删除成功')
    loadArticles()
  } catch (e) {
    if (e !== 'cancel' && e.message !== 'backend_offline') {
      ElMessage.error('删除失败')
    }
  }
}

const openPreview = async (row) => {
  try {
    previewArticle.value = await request.get(`/content/article/${row.id}`)
  } catch (e) {
    previewArticle.value = row
  }
  previewVisible.value = true
}

const openPublishTimeEditor = (row) => {
  publishTimeArticle.value = row || {}
  publishTimeForm.id = row?.id || null
  publishTimeForm.publishTime = normalizeDateTimeValue(row?.publishTime)
  publishTimeDialogVisible.value = true
}

const submitPublishTime = async () => {
  if (!publishTimeForm.id) return
  if (!String(publishTimeForm.publishTime || '').trim()) {
    ElMessage.warning('请选择发布时间')
    return
  }

  publishTimeSubmitting.value = true
  try {
    await request.put(`/content/article/${publishTimeForm.id}/publish-time`, {
      publishTime: String(publishTimeForm.publishTime).replace(' ', 'T')
    })
    ElMessage.success('发布时间已更新')
    publishTimeDialogVisible.value = false
    loadArticles()
  } catch (e) {
    if (e.message !== 'backend_offline') {
      ElMessage.error('发布时间更新失败')
    }
  } finally {
    publishTimeSubmitting.value = false
  }
}

const splitTags = (tags) => {
  return String(tags || '')
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter(Boolean)
    .slice(0, 3)
}

const normalizeTags = (tags) => {
  return String(tags || '')
    .split(/[,，]/)
    .map((tag) => tag.trim())
    .filter(Boolean)
    .join(',')
}

const articleParagraphs = (content) => {
  return String(content || '')
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const categoryLabel = (category) => {
  const map = {
    APPRECIATION: 'DAY DAY ART',
    CURATION: '专题策展',
    ARTIST: '艺术家故事',
    NOTICE: '平台公告'
  }
  return map[category] || 'DAY DAY ART'
}

const statusLabel = (status) => {
  const map = {
    DRAFT: '草稿',
    SCHEDULED: '定时发布',
    PUBLISHED: '已发布',
    OFFLINE: '已下架'
  }
  return map[status] || '草稿'
}

const statusType = (status) => {
  const map = {
    DRAFT: 'info',
    SCHEDULED: 'warning',
    PUBLISHED: 'success',
    OFFLINE: 'warning'
  }
  return map[status] || 'info'
}

const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const normalizeDateTimeValue = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(loadArticles)
</script>

<style scoped lang="scss">
.article-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.page-header h2 {
  margin: 0;
  color: #1f2d3d;
  font-size: 20px;
  font-weight: 700;
}

.page-header p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.publish-time-title {
  color: #303133;
  line-height: 1.5;
}

.toolbar {
  display: flex;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.toolbar .keyword-input {
  width: 300px;
}

.article-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.cover,
.cover-empty {
  flex: 0 0 96px;
  width: 96px;
  height: 64px;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f7fa;
}

.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  border: 1px dashed #dcdfe6;
}

.article-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.article-title {
  display: block;
  width: fit-content;
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: #303133;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  text-align: left;
  cursor: pointer;
}

.article-title:hover {
  color: #409eff;
  text-decoration: underline;
}

.article-title:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 2px;
  border-radius: 2px;
}

.article-meta strong {
  color: #303133;
  font-size: 14px;
}

.article-meta span {
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
}

.upload-area {
  position: relative;
  width: 260px;
  height: 146px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.upload-area .el-image {
  width: 100%;
  height: 100%;
}

.detail-image-area {
  background: #fafafa;
}

.detail-image-area .el-image {
  background: #fafafa;
}

.upload-placeholder,
.upload-mask {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.upload-placeholder {
  color: #909399;
  background: #fafafa;
}

.upload-mask {
  position: absolute;
  inset: 0;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.2s;
}

.upload-area:hover .upload-mask {
  opacity: 1;
}

.file-input {
  display: none;
}

.upload-help {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.upload-progress {
  width: 260px;
  margin-top: 8px;
}

.schedule-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.schedule-help {
  color: #909399;
  font-size: 12px;
}

.schedule-time-select {
  width: 128px;
}

.preview-panel {
  align-self: start;
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fbfcfe;
  max-height: 680px;
  overflow: auto;
}

.preview-panel .preview-label {
  display: block;
  margin-bottom: 12px;
  color: #909399;
  font-size: 12px;
}

.preview-panel h3 {
  margin: 14px 0 8px;
  color: #1f2d3d;
  font-size: 22px;
  line-height: 1.3;
}

.preview-panel p {
  color: #303133;
  font-size: 14px;
  line-height: 1.8;
}

.preview-cover {
  border-radius: 6px;
  overflow: hidden;
}

.preview-cover img {
  width: 100%;
  display: block;
}

.preview-body-image {
  background: #f5f7fa;
}

.preview-subtitle {
  color: #606266 !important;
}

.preview-info {
  display: flex;
  gap: 12px;
  color: #909399;
  font-size: 12px;
}

.read-preview img {
  width: 100%;
  display: block;
  border-radius: 6px;
  margin-bottom: 18px;
}

.read-preview span {
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
}

.read-preview h1 {
  margin: 8px 0;
  color: #1f2d3d;
  font-size: 28px;
  line-height: 1.3;
}

.read-preview h2 {
  margin: 0 0 12px;
  color: #606266;
  font-size: 18px;
  font-weight: 500;
}

.read-preview p {
  color: #303133;
  font-size: 15px;
  line-height: 1.9;
}

.read-preview .summary {
  padding: 12px 14px;
  color: #606266;
  background: #f5f7fa;
  border-radius: 6px;
}

.read-meta {
  margin-bottom: 16px;
  color: #909399;
  font-size: 13px;
}
</style>
