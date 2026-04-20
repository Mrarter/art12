<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">艺荐官管理</span>
      <div class="actions">
        <el-statistic title="艺荐官总数" :value="stats.total" />
        <el-statistic title="本月新增" :value="stats.monthlyNew" />
        <el-statistic title="待结算佣金" :value="'¥' + stats.pendingCommission" />
        <el-button type="primary" @click="openAddDialog">添加艺荐官</el-button>
      </div>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="searchForm.level" placeholder="全部" clearable>
            <el-option label="普通艺荐官" value="1" />
            <el-option label="高级艺荐官" value="2" />
            <el-option label="金牌艺荐官" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column label="用户信息" min-width="180">
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
      <el-table-column label="等级" width="120">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.level)">{{ getLevelText(row.level) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="teamCount" label="团队人数" width="100" />
      <el-table-column prop="directCount" label="直接推荐" width="100" />
      <el-table-column label="累计佣金" width="120">
        <template #default="{ row }">¥{{ row.totalCommission }}</template>
      </el-table-column>
      <el-table-column label="可提现" width="120">
        <template #default="{ row }">¥{{ row.withdrawable }}</template>
      </el-table-column>
      <el-table-column prop="becomeTime" label="成为时间" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '正常' : '冻结' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewTeam(row)">团队</el-button>
          <el-button type="primary" link @click="viewCommission(row)">佣金</el-button>
          <el-button type="danger" link @click="handleFreeze(row)">
            {{ row.status === 1 ? '冻结' : '解冻' }}
          </el-button>
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

    <!-- 团队成员弹窗 -->
    <el-dialog v-model="teamVisible" title="团队成员" width="700px" destroy-on-close>
      <div v-if="currentPromoter.userId" class="promoter-info">
        <span>艺荐官：{{ currentPromoter.nickname }}</span>
        <span>团队人数：{{ currentPromoter.teamCount }} 人</span>
      </div>
      <el-table :data="teamMembers" border stripe size="small">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="等级" width="100">
          <template #default="{ row }">{{ row.level === 1 ? '普通' : row.level === 2 ? '高级' : '金牌' }}</template>
        </el-table-column>
        <el-table-column prop="joinTime" label="加入时间" width="120" />
        <el-table-column prop="teamCount" label="下级人数" width="100" />
      </el-table>
    </el-dialog>

    <!-- 佣金记录弹窗 -->
    <el-dialog v-model="commissionVisible" title="佣金记录" width="700px" destroy-on-close>
      <div v-if="currentPromoter.userId" class="promoter-info">
        <span>艺荐官：{{ currentPromoter.nickname }}</span>
        <span>累计佣金：¥{{ currentPromoter.totalCommission }}</span>
        <span>可提现：¥{{ currentPromoter.withdrawable }}</span>
      </div>
      <el-table :data="commissionRecords" border stripe size="small">
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column label="订单金额" width="100">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="佣金比例" width="100">
          <template #default="{ row }">{{ (row.rate * 100).toFixed(0) }}%</template>
        </el-table-column>
        <el-table-column label="佣金" width="100">
          <template #default="{ row }"><span class="commission">¥{{ row.commission }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'settled' ? 'success' : 'warning'" size="small">
              {{ row.status === 'settled' ? '已结算' : '待结算' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" />
      </el-table>
    </el-dialog>

    <!-- 添加艺荐官弹窗 -->
    <el-dialog v-model="addVisible" title="添加艺荐官" width="500px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="addForm.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="艺荐官等级" prop="level">
          <el-select v-model="addForm.level" placeholder="请选择等级" style="width: 100%">
            <el-option label="普通艺荐官" :value="1" />
            <el-option label="高级艺荐官" :value="2" />
            <el-option label="金牌艺荐官" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="可选，添加备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确认添加</el-button>
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
const stats = reactive({
  total: 0,
  monthlyNew: 0,
  pendingCommission: 0
})
const teamVisible = ref(false)
const commissionVisible = ref(false)
const addVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref(null)
const currentPromoter = ref({})
const teamMembers = ref([])
const commissionRecords = ref([])

// 添加艺荐官表单
const addForm = reactive({
  userId: '',
  level: 1,
  remark: ''
})

const addRules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }]
}

const searchForm = reactive({
  userId: '',
  level: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getLevelType = (level) => {
  const map = { 1: '', 2: 'warning', 3: 'success' }
  return map[level] || ''
}

const getLevelText = (level) => {
  const map = { 1: '普通', 2: '高级', 3: '金牌' }
  return map[level] || '普通'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size, ...searchForm }
    const data = await request.get('/user/promoter/list', { params })
    tableData.value = data.list
    pagination.total = data.total
    Object.assign(stats, data.stats)
  } catch (e) {
    tableData.value = [
      { userId: '20001', nickname: '艺荐官A', phone: '13900139001', avatar: '', level: 3, teamCount: 156, directCount: 28, totalCommission: 125000, withdrawable: 8500, becomeTime: '2023-06-01 10:00:00', status: 1 },
      { userId: '20002', nickname: '艺荐官B', phone: '13900139002', avatar: '', level: 2, teamCount: 89, directCount: 15, totalCommission: 68000, withdrawable: 4200, becomeTime: '2023-08-15 14:30:00', status: 1 },
      { userId: '20003', nickname: '艺荐官C', phone: '13900139003', avatar: '', level: 1, teamCount: 23, directCount: 8, totalCommission: 15000, withdrawable: 1200, becomeTime: '2023-11-20 09:00:00', status: 1 }
    ]
    pagination.total = 3
    stats.total = 1256
    stats.monthlyNew = 89
    stats.pendingCommission = 258000
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, { userId: '', level: '' })
  handleSearch()
}

const viewTeam = (row) => {
  currentPromoter.value = row
  // 模拟团队成员数据
  teamMembers.value = [
    { userId: '30001', nickname: '团队成员A', phone: '13800138001', level: 1, joinTime: '2023-07-01', teamCount: 5 },
    { userId: '30002', nickname: '团队成员B', phone: '13800138002', level: 1, joinTime: '2023-08-15', teamCount: 3 },
    { userId: '30003', nickname: '团队成员C', phone: '13800138003', level: 1, joinTime: '2023-09-20', teamCount: 0 }
  ]
  teamVisible.value = true
}

const viewCommission = (row) => {
  currentPromoter.value = row
  // 模拟佣金记录
  commissionRecords.value = [
    { id: 1, orderNo: 'SYJ20240120001', amount: 58000, rate: 0.15, commission: 8700, status: 'settled', time: '2024-01-20 10:30:00' },
    { id: 2, orderNo: 'SYJ20240118002', amount: 32000, rate: 0.1, commission: 3200, status: 'pending', time: '2024-01-18 15:20:00' },
    { id: 3, orderNo: 'SYJ20240115003', amount: 8800, rate: 0.1, commission: 880, status: 'settled', time: '2024-01-15 09:45:00' }
  ]
  commissionVisible.value = true
}

const handleFreeze = async (row) => {
  const action = row.status === 1 ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(`确定要${action}该艺荐官吗？`, '提示', { type: 'warning' })
    // 本地更新状态
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success(`${action}成功`)
  } catch (e) {}
}

// 打开添加艺荐官弹窗
const openAddDialog = () => {
  Object.assign(addForm, { userId: '', level: 1, remark: '' })
  addVisible.value = true
}

// 添加艺荐官
const handleAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    addLoading.value = true
    // 调用后端接口
    await request.post('/user/promoter/add', addForm)
    ElMessage.success('添加成功')
    addVisible.value = false
    loadData()
  } catch (e) {
    // 模拟成功（接口未实现时）
    const newPromoter = {
      userId: addForm.userId,
      nickname: '用户' + addForm.userId,
      phone: '未设置',
      avatar: '',
      level: addForm.level,
      teamCount: 0,
      directCount: 0,
      totalCommission: 0,
      withdrawable: 0,
      becomeTime: new Date().toLocaleString(),
      status: 1
    }
    tableData.value.unshift(newPromoter)
    stats.total++
    ElMessage.success('添加成功（模拟）')
    addVisible.value = false
  } finally {
    addLoading.value = false
  }
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
  
  .actions {
    display: flex;
    gap: 30px;
    align-items: center;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .phone {
    font-size: 12px;
    color: #999;
  }
}

.promoter-info {
  display: flex;
  gap: 24px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 16px;
  font-size: 14px;
}

.commission {
  color: #67c23a;
  font-weight: 500;
}
</style>
