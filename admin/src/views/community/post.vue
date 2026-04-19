<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">帖子管理</span>
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
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref([])
const commentsVisible = ref(false)
const currentPost = ref({})
const comments = ref([])

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
</style>
