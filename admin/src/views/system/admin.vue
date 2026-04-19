<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">管理员</span>
      <el-button type="primary" @click="showDialog('add')">添加管理员</el-button>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="管理员" min-width="200">
        <template #default="{ row }">
          <div class="admin-info">
            <el-avatar :src="row.avatar" :size="40" icon="UserFilled" />
            <div>
              <p>{{ row.username }}</p>
              <p class="email">{{ row.email }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="150">
        <template #default="{ row }">
          <el-tag :type="row.role === 'super' ? 'danger' : 'primary'" size="small">
            {{ row.role === 'super' ? '超级管理员' : '普通管理员' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '禁用' }}
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
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="超级管理员" value="super" />
            <el-option label="普通管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
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
  username: '',
  password: '',
  email: '',
  phone: '',
  role: 'admin',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑管理员' : '添加管理员')

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await request.get('/system/admin/list')
  } catch (e) {
    // 使用本地模拟数据
    if (!tableData.value.length) {
      tableData.value = [
        { id: 1, username: 'admin', email: 'admin@shiyiju.com', phone: '13800000000', role: 'super', status: 1, lastLoginTime: '2024-01-21 10:00:00', createTime: '2023-01-01 00:00:00' },
        { id: 2, username: 'operator', email: 'op@shiyiju.com', phone: '13800000001', role: 'admin', status: 1, lastLoginTime: '2024-01-20 15:30:00', createTime: '2023-06-01 00:00:00' }
      ]
    }
  } finally {
    loading.value = false
  }
}

const showDialog = (type, row = null) => {
  if (type === 'add') {
    isEdit.value = false
    Object.assign(form, { id: null, username: '', password: '', email: '', phone: '', role: 'admin', status: 1 })
  } else {
    isEdit.value = true
    Object.assign(form, { ...row, password: '' })
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
        tableData.value[index] = { ...tableData.value[index], email: form.email, phone: form.phone, role: form.role, status: form.status }
        ElMessage.success('更新成功')
      }
    } else {
      // 本地添加
      const newId = Math.max(...tableData.value.map(item => item.id), 0) + 1
      tableData.value.unshift({
        id: newId,
        username: form.username,
        email: form.email,
        phone: form.phone,
        role: form.role,
        status: form.status,
        lastLoginTime: '-',
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
  if (row.role === 'super') {
    ElMessage.warning('不能删除超级管理员')
    return
  }
  try {
    await ElMessageBox.confirm('确定要删除该管理员吗？', '提示', { type: 'warning' })
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
.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .email {
    font-size: 12px;
    color: #999;
  }
}
</style>
