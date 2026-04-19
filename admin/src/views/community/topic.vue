<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">话题管理</span>
      <el-button type="primary" @click="showDialog('add')">添加话题</el-button>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="topicId" label="ID" width="80" />
      <el-table-column label="话题" min-width="200">
        <template #default="{ row }">
          <el-tag size="large">{{ row.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column label="帖子数" width="100">
        <template #default="{ row }">{{ row.postCount }}</template>
      </el-table-column>
      <el-table-column label="浏览量" width="100">
        <template #default="{ row }">{{ row.viewCount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog('edit', row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="话题名称" prop="name">
          <el-input v-model="form.name" placeholder="如: #艺术分享#" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入话题描述" />
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
import request from '@/api/request'

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const tableData = ref([])
const isEdit = ref(false)

const form = reactive({
  topicId: null,
  name: '',
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入话题名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑话题' : '添加话题')

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await request.get('/community/topic/list')
  } catch (e) {
    // 使用本地模拟数据
    if (!tableData.value.length) {
      tableData.value = [
        { topicId: 1, name: '#艺术分享', description: '分享你的艺术收藏', postCount: 256, viewCount: 12580, status: 1, createTime: '2024-01-01 00:00:00' },
        { topicId: 2, name: '#每日一画', description: '每天欣赏一幅画', postCount: 189, viewCount: 8920, status: 1, createTime: '2024-01-01 00:00:00' },
        { topicId: 3, name: '#艺术品鉴赏', description: '一起鉴赏艺术品的魅力', postCount: 145, viewCount: 6780, status: 1, createTime: '2024-01-01 00:00:00' }
      ]
    }
  } finally {
    loading.value = false
  }
}

const showDialog = (type, row = null) => {
  if (type === 'add') {
    isEdit.value = false
    Object.assign(form, { topicId: null, name: '', description: '', status: 1 })
  } else {
    isEdit.value = true
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    if (isEdit.value) {
      // 本地更新
      const index = tableData.value.findIndex(item => item.topicId === form.topicId)
      if (index > -1) {
        tableData.value[index] = { ...tableData.value[index], ...form }
        ElMessage.success('更新成功')
      }
    } else {
      // 本地添加
      const newId = Math.max(...tableData.value.map(item => item.topicId), 0) + 1
      tableData.value.unshift({
        topicId: newId,
        ...form,
        postCount: 0,
        viewCount: 0,
        createTime: new Date().toLocaleString('zh-CN').replace(/\//g, '-')
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该话题吗？', '提示', { type: 'warning' })
    // 本地删除
    const index = tableData.value.findIndex(item => item.topicId === row.topicId)
    if (index > -1) {
      tableData.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  } catch (e) {}
}

onMounted(() => {
  loadData()
})
</script>
