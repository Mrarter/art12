<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">分类管理</span>
      <el-button type="primary" @click="showDialog('add')">添加分类</el-button>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名称" width="150" />
      <el-table-column prop="icon" label="图标" width="100">
        <template #default="{ row }">
          <el-icon :size="24"><component :is="row.icon" /></el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="weight" label="权重" width="100">
        <template #default="{ row }">
          <el-tag type="info">{{ row.weight || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="artworkCount" label="作品数" width="100" />
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
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类图标" prop="icon">
          <el-input v-model="form.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-input-number v-model="form.weight" :min="0" :max="999" />
          <span class="tip">数值越大排序越靠前</span>
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
  id: null,
  name: '',
  icon: '',
  weight: 0,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑分类' : '添加分类')

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await request.get('/product/categories')
  } catch (e) {
    // 使用本地模拟数据
    if (!tableData.value.length) {
      tableData.value = [
        { id: 1, name: '国画', icon: 'Picture', weight: 100, artworkCount: 256, status: 1, createTime: '2023-01-01 00:00:00' },
        { id: 2, name: '油画', icon: 'Picture', weight: 90, artworkCount: 189, status: 1, createTime: '2023-01-01 00:00:00' },
        { id: 3, name: '书法', icon: 'EditPen', weight: 80, artworkCount: 145, status: 1, createTime: '2023-01-01 00:00:00' },
        { id: 4, name: '版画', icon: 'Picture', weight: 70, artworkCount: 78, status: 1, createTime: '2023-01-01 00:00:00' },
        { id: 5, name: '雕塑', icon: 'Box', weight: 60, artworkCount: 56, status: 1, createTime: '2023-01-01 00:00:00' }
      ]
    }
  } finally {
    loading.value = false
  }
}

const showDialog = (type, row = null) => {
  if (type === 'add') {
    isEdit.value = false
    Object.assign(form, { id: null, name: '', icon: '', weight: 0, status: 1 })
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
      const index = tableData.value.findIndex(item => item.id === form.id)
      if (index > -1) {
        tableData.value[index] = { ...tableData.value[index], ...form }
        ElMessage.success('更新成功')
      }
    } else {
      // 本地添加
      const newId = Math.max(...tableData.value.map(item => item.id), 0) + 1
      tableData.value.unshift({
        id: newId,
        ...form,
        artworkCount: 0,
        createTime: new Date().toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' }).replace(/\//g, '-')
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
    await ElMessageBox.confirm('确定要删除该分类吗？', '提示', { type: 'warning' })
    // 本地删除
    const index = tableData.value.findIndex(item => item.id === row.id)
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
.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
