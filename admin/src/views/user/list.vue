<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">用户列表</span>
      <div class="actions">
        <el-button type="primary" @click="exportData">导出</el-button>
      </div>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="用户身份">
          <el-select v-model="searchForm.role" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="艺术家" value="artist" />
            <el-option label="收藏家" value="collector" />
            <el-option label="艺荐官" value="promoter" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column label="用户信息" min-width="200">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :src="row.avatar" :size="40" />
            <div>
              <p>{{ row.nickname }}</p>
              <p class="phone">{{ row.phone }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="身份" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.isArtist" type="success" size="small">艺术家</el-tag>
          <el-tag v-if="row.isPromoter" type="warning" size="small">艺荐官</el-tag>
          <el-tag v-if="!row.isArtist && !row.isPromoter" size="small">收藏家</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="balance" label="余额" width="120">
        <template #default="{ row }">¥{{ row.balance }}</template>
      </el-table-column>
      <el-table-column prop="orderCount" label="订单数" width="100" />
      <el-table-column prop="createTime" label="注册时间" width="180" />
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
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          <el-button type="primary" link @click="viewOrders(row)">订单</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
    
    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户ID">{{ currentUser.userId }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentUser.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="身份" :span="2">
          <el-tag v-if="currentUser.isArtist" type="success">艺术家</el-tag>
          <el-tag v-if="currentUser.isPromoter" type="warning">艺荐官</el-tag>
          <el-tag v-if="!currentUser.isArtist && !currentUser.isPromoter">收藏家</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="余额">¥{{ currentUser.balance }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ currentUser.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ currentUser.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const detailVisible = ref(false)
const currentUser = ref({})
const tableData = ref([])

const searchForm = reactive({
  userId: '',
  phone: '',
  role: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    }
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const data = await request.get('/user/list', { params })
    tableData.value = data.list
    pagination.total = data.total
  } catch (e) {
    // 模拟数据
    tableData.value = [
      { userId: '10001', nickname: '张三', phone: '13800138001', avatar: '', isArtist: true, isPromoter: false, balance: 5000, orderCount: 12, createTime: '2024-01-15 10:30:00', status: 1 },
      { userId: '10002', nickname: '李四', phone: '13800138002', avatar: '', isArtist: false, isPromoter: true, balance: 12000, orderCount: 28, createTime: '2024-01-16 14:20:00', status: 1 },
      { userId: '10003', nickname: '王五', phone: '13800138003', avatar: '', isArtist: true, isPromoter: true, balance: 25000, orderCount: 45, createTime: '2024-01-17 09:15:00', status: 1 }
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
  Object.assign(searchForm, { userId: '', phone: '', role: '', dateRange: [] })
  handleSearch()
}

const handleStatusChange = async (row) => {
  try {
    await request.post('/user/updateStatus', { userId: row.userId, status: row.status })
    ElMessage.success('状态更新成功')
  } catch (e) {
    row.status = row.status === 1 ? 0 : 1
  }
}

const viewDetail = (row) => {
  currentUser.value = row
  detailVisible.value = true
}

const viewOrders = (row) => {
  // TODO: 跳转订单页面
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
    await request.post('/user/delete', { userId: row.userId })
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {}
}

const exportData = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .phone {
    font-size: 12px;
    color: #999;
  }
}
</style>
