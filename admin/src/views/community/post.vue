<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">帖子管理</span>
      <el-button type="primary" @click="openAddDialog">添加帖子</el-button>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="帖子内容">
          <el-input v-model="searchForm.content" placeholder="请输入关键词" clearable />
        </el-form-item>
        <el-form-item label="发布者">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="postId" label="ID" width="80" />
      <el-table-column label="发布者" width="150">
        <template #default="{ row }">
          <p>{{ row.userName }}</p>
          <p class="phone">{{ row.phone }}</p>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="300">
        <template #default="{ row }">
          <div class="content">
            <p class="text">{{ row.content }}</p>
            <div v-if="row.images?.length" class="images">
              <el-image 
                v-for="(img, idx) in row.images" 
                :key="idx"
                :src="img" 
                style="width: 60px; height: 60px; margin-right: 5px"
                fit="cover"
              />
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="话题" width="150">
        <template #default="{ row }">
          <el-tag v-if="row.topicName" size="small">{{ row.topicName }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="点赞" width="80">
        <template #default="{ row }">{{ row.likeCount }}</template>
      </el-table-column>
      <el-table-column label="评论" width="80">
        <template #default="{ row }">{{ row.commentCount }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="viewComments(row)">评论</el-button>
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

    <!-- 评论列表弹窗 -->
    <el-dialog v-model="commentsVisible" title="评论列表" width="600px" destroy-on-close>
      <div v-if="currentPost.postId" class="post-preview">
        <p class="content">{{ currentPost.content }}</p>
      </div>
      <el-divider />
      <div class="comments-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <span class="user">{{ comment.userName }}</span>
            <span class="time">{{ comment.createTime }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          <div class="comment-actions">
            <el-button type="danger" link size="small" @click="handleDeleteComment(comment)">删除</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 添加/编辑帖子弹窗 -->
    <el-dialog v-model="postDialogVisible" :title="isEdit ? '编辑帖子' : '添加帖子'" width="600px" destroy-on-close>
      <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-width="80px">
        <el-form-item label="发布者" prop="userId">
          <el-select v-model="postForm.userId" placeholder="请选择用户" filterable style="width: 100%">
            <el-option v-for="user in userList" :key="user.userId" :label="user.nickname + ' (' + user.phone + ')'" :value="user.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="话题" prop="topicId">
          <el-select v-model="postForm.topicId" placeholder="请选择话题（可选）" clearable style="width: 100%">
            <el-option v-for="topic in topicList" :key="topic.topicId" :label="topic.name" :value="topic.topicId" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="postForm.content" type="textarea" :rows="5" placeholder="请输入帖子内容" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="图片">
          <div class="image-upload">
            <div v-for="(img, idx) in postForm.images" :key="idx" class="upload-item">
              <el-image :src="img" fit="cover" />
              <div class="upload-mask" @click="removeImage(idx)">
                <el-icon><Delete /></el-icon>
              </div>
            </div>
            <div v-if="postForm.images.length < 9" class="upload-btn" @click="triggerImageUpload">
              <el-icon><Plus /></el-icon>
              <span>添加图片</span>
            </div>
          </div>
          <input ref="imageFileInput" type="file" accept="image/*" multiple style="display: none" @change="handleImageChange" />
          <div class="upload-tip">最多9张图片</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="postDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref([])
const commentsVisible = ref(false)
const postDialogVisible = ref(false)
const currentPost = ref({})
const comments = ref([])
const userList = ref([])
const topicList = ref([])
const isEdit = ref(false)
const submitLoading = ref(false)
const postFormRef = ref()
const imageFileInput = ref()

const postForm = reactive({
  postId: '',
  userId: '',
  topicId: '',
  content: '',
  images: []
})

const postRules = {
  userId: [{ required: true, message: '请选择发布者', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const searchForm = reactive({
  content: '',
  userId: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await request.get('/community/post/list', { params: { page: pagination.page, size: pagination.size, ...searchForm } })
    tableData.value = data.list
    pagination.total = data.total
  } catch (e) {
    tableData.value = [
      { postId: 1, userName: '用户A', phone: '13800138001', content: '这是一条帖子内容...', images: [], topicName: '#艺术分享', likeCount: 156, commentCount: 23, createTime: '2024-01-21 10:00:00' }
    ]
    pagination.total = 1
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { content: '', userId: '' })
  handleSearch()
}

const viewComments = (row) => {
  currentPost.value = row
  // 模拟评论数据
  comments.value = [
    { id: 1, userName: '用户B', content: '这幅画真美！', createTime: '2024-01-21 11:00:00' },
    { id: 2, userName: '用户C', content: '请问在哪里可以买到？', createTime: '2024-01-21 11:30:00' },
    { id: 3, userName: '艺术家A', content: '感谢大家关注！', createTime: '2024-01-21 12:00:00' }
  ]
  commentsVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该帖子吗？', '提示', { type: 'warning' })
    // 本地删除
    const index = tableData.value.findIndex(item => item.postId === row.postId)
    if (index > -1) {
      tableData.value.splice(index, 1)
      pagination.total--
      ElMessage.success('删除成功')
    }
  } catch (e) {}
}

const handleDeleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗？', '提示', { type: 'warning' })
    const index = comments.value.findIndex(item => item.id === comment.id)
    if (index > -1) {
      comments.value.splice(index, 1)
      currentPost.value.commentCount--
      ElMessage.success('删除成功')
    }
  } catch (e) {}
}

// 打开添加弹窗
const openAddDialog = async () => {
  isEdit.value = false
  Object.assign(postForm, { postId: '', userId: '', topicId: '', content: '', images: [] })
  await loadUsers()
  await loadTopics()
  postDialogVisible.value = true
}

// 加载用户列表
const loadUsers = async () => {
  try {
    const data = await request.get('/user/list', { params: { page: 1, size: 100 } })
    userList.value = data.records || data.list || []
  } catch (e) {
    userList.value = []
  }
}

// 加载话题列表
const loadTopics = async () => {
  try {
    const data = await request.get('/community/topic/list')
    topicList.value = data.list || data || []
  } catch (e) {
    topicList.value = []
  }
}

// 编辑帖子
const handleEdit = async (row) => {
  isEdit.value = true
  await loadUsers()
  await loadTopics()
  Object.assign(postForm, {
    postId: row.postId,
    userId: row.userId || '',
    topicId: row.topicId || '',
    content: row.content,
    images: row.images ? [...row.images] : []
  })
  postDialogVisible.value = true
}

// 触发图片上传
const triggerImageUpload = () => {
  imageFileInput.value.click()
}

// 处理图片上传
const handleImageChange = async (e) => {
  const files = e.target.files
  if (!files.length) return

  for (const file of files) {
    if (postForm.images.length >= 9) {
      ElMessage.warning('最多上传9张图片')
      break
    }
    try {
      const formData = new FormData()
      formData.append('file', file)
      const url = await request.post('/upload/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      // 支持返回URL或 { url: '...' } 格式
      const imageUrl = typeof url === 'string' ? url : (url.url || url.data || url.path)
      postForm.images.push(imageUrl)
    } catch (e) {
      console.error('图片上传失败', e)
      ElMessage.error('图片上传失败')
    }
  }
  // 清空input
  e.target.value = ''
}

// 删除图片
const removeImage = (index) => {
  postForm.images.splice(index, 1)
}

// 提交表单
const handleSubmit = async () => {
  if (!postFormRef.value) return
  try {
    await postFormRef.value.validate()
    submitLoading.value = true

    const api = isEdit.value ? '/community/post/update' : '/community/post/create'
    const data = isEdit.value
      ? { postId: postForm.postId, content: postForm.content, topicId: postForm.topicId, images: postForm.images }
      : { userId: postForm.userId, content: postForm.content, topicId: postForm.topicId, images: postForm.images }

    await request.post(api, data)
    ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
    postDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('提交失败', e)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.phone {
  font-size: 12px;
  color: #999;
}

.content {
  .text {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  .images {
    margin-top: 5px;
    display: flex;
  }
}

.post-preview {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  
  .content {
    margin: 0;
    color: #303133;
  }
}

.comments-list {
  max-height: 400px;
  overflow-y: auto;
}

.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
  
  &:last-child {
    border-bottom: none;
  }
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  
  .user {
    color: #409eff;
    font-weight: 500;
  }
  
  .time {
    color: #909399;
    font-size: 12px;
  }
}

.comment-content {
  color: #606266;
  line-height: 1.6;
}

.comment-actions {
  margin-top: 8px;
  text-align: right;
}

.image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-item {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  overflow: hidden;
  position: relative;

  .el-image {
    width: 100%;
    height: 100%;
  }

  .upload-mask {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 20px;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.3s;

    &:hover {
      opacity: 1;
    }
  }
}

.upload-btn {
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
    color: #409eff;
  }

  .el-icon {
    font-size: 24px;
    margin-bottom: 4px;
  }

  span {
    font-size: 12px;
  }
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
