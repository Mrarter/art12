<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">Banner管理</span>
      <el-button type="primary" @click="showDialog('add')">添加Banner</el-button>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="图片" width="200">
        <template #default="{ row }">
          <el-image :src="row.image" style="width: 160px; height: 80px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="标题" width="150">
        <template #default="{ row }">{{ row.title }}</template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type === 'home' ? '首页' : row.type === 'artwork' ? '作品详情' : '其他' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跳转链接" min-width="200">
        <template #default="{ row }">
          <p class="link">{{ row.url || '-' }}</p>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog('edit', row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <el-input v-model="form.image" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option label="首页" value="home" />
            <el-option label="作品详情" value="artwork" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转链接" prop="url">
          <el-input v-model="form.url" placeholder="请输入跳转链接" />
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
import request from '@/api/request'

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const tableData = ref([])
const isEdit = ref(false)

const form = reactive({
  id: null,
  title: '',
  image: '',
  type: 'home',
  url: '',
  sort: 0,
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  image: [{ required: true, message: '请输入图片URL', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑Banner' : '添加Banner')

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await request.get('/system/banner/list')
  } catch (e) {
    // 使用本地模拟数据
    if (!tableData.value.length) {
      tableData.value = [
        { id: 1, title: '春季艺术品拍卖会', image: '', type: 'home', url: '/pages/auction/index', sort: 1, status: 1 },
        { id: 2, title: '新用户专享', image: '', type: 'home', url: '/pages/promoter/index', sort: 2, status: 1 },
        { id: 3, title: '艺术家招募', image: '', type: 'artwork', url: '/pages/artist/apply', sort: 3, status: 1 }
      ]
    }
  } finally {
    loading.value = false
  }
}

const showDialog = (type, row = null) => {
  if (type === 'add') {
    isEdit.value = false
    Object.assign(form, { id: null, title: '', image: '', type: 'home', url: '', sort: 0, status: 1 })
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
      tableData.value.unshift({ id: newId, ...form })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该Banner吗？', '提示', { type: 'warning' })
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
.link {
  color: #409eff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
