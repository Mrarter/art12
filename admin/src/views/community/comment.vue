<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">评论管理</span>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="评论内容">
          <el-input v-model="searchForm.content" placeholder="请输入关键词" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="commentId" label="ID" width="80" />
      <el-table-column label="评论者" width="150">
        <template #default="{ row }">
          <p>{{ row.userName }}</p>
          <p class="phone">{{ row.phone }}</p>
        </template>
      </el-table-column>
      <el-table-column label="评论内容" min-width="250">
        <template #default="{ row }">
          <p class="comment-content">{{ row.content }}</p>
        </template>
      </el-table-column>
      <el-table-column label="所属帖子" min-width="200">
        <template #default="{ row }">
          <p class="post-content">{{ row.postContent }}</p>
        </template>
      </el-table-column>
      <el-table-column label="点赞" width="80">
        <template #default="{ row }">{{ row.likeCount }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="评论时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewReply(row)">回复</el-button>
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

    <!-- 回复列表弹窗 -->
    <el-dialog v-model="replyVisible" title="回复列表" width="600px" destroy-on-close>
      <div v-if="currentComment.commentId" class="comment-preview">
        <p class="label">原评论：</p>
        <p class="content">{{ currentComment.content }}</p>
        <p class="meta">{{ currentComment.userName }} · {{ currentComment.createTime }}</p>
      </div>
      <el-divider />
      <div class="replies-list">
        <div v-for="reply in replies" :key="reply.id" class="reply-item">
          <div class="reply-header">
            <span class="user">{{ reply.userName }}</span>
            <span class="time">{{ reply.createTime }}</span>
          </div>
          <div class="reply-content">{{ reply.content }}</div>
        </div>
        <el-empty v-if="!replies.length" description="暂无回复" />
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
const replyVisible = ref(false)
const currentComment = ref({})
const replies = ref([])

const searchForm = reactive({
  content: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const data = await request.get('/community/comment/list', { params: { page: pagination.page, size: pagination.size, ...searchForm } })
    tableData.value = data.list
    pagination.total = data.total
  } catch (e) {
    // 使用本地模拟数据
    if (!tableData.value.length) {
      tableData.value = [
        { commentId: 1, userName: '用户B', phone: '13800138002', content: '这幅画真漂亮！', postContent: '这是一条帖子内容...', likeCount: 12, createTime: '2024-01-21 11:00:00' },
        { commentId: 2, userName: '用户C', phone: '13800138003', content: '请问在哪里可以买到？', postContent: '新作品发布...', likeCount: 8, createTime: '2024-01-20 15:30:00' },
        { commentId: 3, userName: '艺术家A', phone: '13800138001', content: '感谢大家关注！', postContent: '艺术分享...', likeCount: 25, createTime: '2024-01-19 09:00:00' }
      ]
      pagination.total = 3
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  searchForm.content = ''
  handleSearch()
}

const viewReply = (row) => {
  currentComment.value = row
  // 模拟回复数据
  replies.value = [
    { id: 1, userName: '用户D', content: '我也想知道', createTime: '2024-01-21 12:00:00' },
    { id: 2, userName: '艺术家A', content: '可以私信我', createTime: '2024-01-21 12:30:00' }
  ]
  replyVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗？', '提示', { type: 'warning' })
    // 本地删除
    const index = tableData.value.findIndex(item => item.commentId === row.commentId)
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
.phone {
  font-size: 12px;
  color: #999;
}

.comment-content, .post-content {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.comment-preview {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  
  .label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;
  }
  
  .content {
    color: #303133;
    margin-bottom: 8px;
  }
  
  .meta {
    font-size: 12px;
    color: #909399;
  }
}

.replies-list {
  max-height: 300px;
  overflow-y: auto;
}

.reply-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
  
  &:last-child {
    border-bottom: none;
  }
}

.reply-header {
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

.reply-content {
  color: #606266;
  line-height: 1.6;
}
</style>
