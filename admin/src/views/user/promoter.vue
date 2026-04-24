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
      <el-table-column label="上级艺荐官" width="140">
        <template #default="{ row }">
          <span v-if="row.inviterName">{{ row.inviterName }}</span>
          <span v-else class="no-inviter">无（顶级）</span>
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
        <el-table-column label="上级" width="120">
          <template #default="{ row }">
            <span v-if="row.inviterName">{{ row.inviterName }}</span>
            <span v-else>-</span>
          </template>
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
    <el-dialog v-model="addVisible" title="添加艺荐官" width="550px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
          请输入手机号添加艺荐官，如用户不存在将自动创建
        </el-alert>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" @blur="searchUserByPhone">
            <template #append>
              <el-button @click="searchUserByPhone" :loading="searchLoading">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="用户ID" v-if="addForm.userId">
          <el-input :model-value="addForm.userId" disabled>
            <template #suffix>
              <el-tag v-if="userFound" type="success" size="small">已找到</el-tag>
              <el-tag v-else type="warning" size="small">将自动创建</el-tag>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="用户昵称">
          <el-input v-model="addForm.nickname" placeholder="可选，自动使用手机号后4位命名" />
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
const searchLoading = ref(false)
const addFormRef = ref(null)
const currentPromoter = ref({})
const teamMembers = ref([])
const commissionRecords = ref([])
const userFound = ref(false)

// 添加艺荐官表单
const addForm = reactive({
  phone: '',
  userId: '',
  nickname: '',
  level: 1,
  remark: ''
})

const addRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }]
}

// 根据手机号搜索用户
const searchUserByPhone = async () => {
  if (!addForm.phone) return
  searchLoading.value = true
  try {
    const data = await request.get('/user/list', { params: { phone: addForm.phone, page: 1, size: 1 } })
    const records = data.records || data.list || []
    if (records.length > 0) {
      addForm.userId = records[0].id || records[0].userId
      addForm.nickname = records[0].nickname || ''
      userFound.value = true
    } else {
      addForm.userId = ''
      userFound.value = false
    }
  } catch (e) {
    addForm.userId = ''
    userFound.value = false
  } finally {
    searchLoading.value = false
  }
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
    tableData.value = data.list || []
    pagination.total = data.total || 0
    Object.assign(stats, data.stats || {})
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    Object.assign(stats, { total: 0, monthlyNew: 0, pendingCommission: 0 })
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

const viewTeam = async (row) => {
  currentPromoter.value = row
  teamMembers.value = await request.get(`/user/promoter/team/${row.userId}`)
  teamVisible.value = true
}

const viewCommission = async (row) => {
  currentPromoter.value = row
  commissionRecords.value = await request.get(`/user/promoter/commission/${row.userId}`)
  commissionVisible.value = true
}

const handleFreeze = async (row) => {
  const action = row.status === 1 ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(`确定要${action}该艺荐官吗？`, '提示', { type: 'warning' })
    await request.post('/user/promoter/freeze', {
      userId: row.userId,
      status: row.status === 1 ? 0 : 1
    })
    await loadData()
    ElMessage.success(`${action}成功`)
  } catch (e) {}
}

// 打开添加艺荐官弹窗
const openAddDialog = () => {
  Object.assign(addForm, { phone: '', userId: '', nickname: '', level: 1, remark: '' })
  userFound.value = false
  addVisible.value = true
}

// 添加艺荐官
const handleAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    addLoading.value = true
    const result = await request.post('/user/promoter/add', addForm)
    const msg = result.message || '添加成功！用户ID：' + result.userId
    ElMessage.success({ message: msg, duration: 5000 })
    addVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('添加失败：' + (e.message || '未知错误'))
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

.no-inviter {
  color: #909399;
  font-size: 12px;
}
</style>
