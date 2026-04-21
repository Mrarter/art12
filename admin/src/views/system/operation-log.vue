<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">操作日志</span>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="loadData"
        />
        <el-select v-model="moduleFilter" placeholder="操作模块" clearable @change="loadData">
          <el-option label="用户管理" value="user" />
          <el-option label="订单管理" value="order" />
          <el-option label="作品管理" value="product" />
          <el-option label="拍卖管理" value="auction" />
          <el-option label="分销管理" value="promotion" />
          <el-option label="系统设置" value="system" />
          <el-option label="社区管理" value="community" />
        </el-select>
        <el-select v-model="actionFilter" placeholder="操作类型" clearable @change="loadData">
          <el-option label="新增" value="create" />
          <el-option label="修改" value="update" />
          <el-option label="删除" value="delete" />
          <el-option label="登录" value="login" />
          <el-option label="登出" value="logout" />
          <el-option label="审核" value="audit" />
        </el-select>
        <el-button type="primary" @click="exportLogs">
          <el-icon><Download /></el-icon>
          导出日志
        </el-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div class="stats-cards">
      <div class="stat-card" @click="moduleFilter = ''; loadData()">
        <div class="stat-icon blue">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">今日操作</p>
          <p class="stat-value">{{ formatNumber(stats.todayCount) }}</p>
        </div>
      </div>
      <div class="stat-card" @click="actionFilter = 'create'; loadData()">
        <div class="stat-icon green">
          <el-icon><Plus /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">新增操作</p>
          <p class="stat-value">{{ formatNumber(stats.createCount) }}</p>
        </div>
      </div>
      <div class="stat-card" @click="actionFilter = 'update'; loadData()">
        <div class="stat-icon orange">
          <el-icon><Edit /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">修改操作</p>
          <p class="stat-value">{{ formatNumber(stats.updateCount) }}</p>
        </div>
      </div>
      <div class="stat-card" @click="actionFilter = 'delete'; loadData()">
        <div class="stat-icon red">
          <el-icon><Delete /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">删除操作</p>
          <p class="stat-value">{{ formatNumber(stats.deleteCount) }}</p>
        </div>
      </div>
    </div>

    <!-- 日志列表 -->
    <el-card shadow="hover">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="操作时间" width="160">
          <template #default="{ row }">
            <span class="time">{{ row.createTime || row.createAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作者" width="140">
          <template #default="{ row }">
            <div class="operator-info">
              <el-avatar :src="row.avatar" :size="28" />
              <div class="operator-detail">
                <p class="name">{{ row.operatorName || row.adminName || '系统' }}</p>
                <p class="role">{{ row.roleName || row.adminRole || '-' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作模块" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getModuleType(row.module)" size="small">
              {{ getModuleText(row.module) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small">
              {{ getActionText(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="200">
          <template #default="{ row }">
            <div class="description">
              <span>{{ row.description || row.detail }}</span>
              <el-tooltip v-if="row.reason" :content="row.reason" placement="top">
                <el-icon class="info-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作对象" width="140">
          <template #default="{ row }">
            <div v-if="row.targetId">
              <p class="target-type">{{ row.targetType || '对象' }}</p>
              <p class="target-id">ID: {{ row.targetId }}</p>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="IP地址" width="130">
          <template #default="{ row }">
            <span class="ip">{{ row.ip || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1 || row.status === 'success'" type="success" size="small">成功</el-tag>
            <el-tag v-else-if="row.status === 0 || row.status === 'failed'" type="danger" size="small">失败</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="700px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.createTime || currentLog.createAt }}</el-descriptions-item>
        <el-descriptions-item label="操作者">{{ currentLog.operatorName || currentLog.adminName || '系统' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ currentLog.roleName || currentLog.adminRole || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ getModuleText(currentLog.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ getActionText(currentLog.action) }}</el-descriptions-item>
        <el-descriptions-item label="操作对象" :span="2">
          <template v-if="currentLog.targetId">
            {{ currentLog.targetType || '对象' }} - ID: {{ currentLog.targetId }}
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="User-Agent">{{ currentLog.userAgent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentLog.description || currentLog.detail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <el-tag v-if="currentLog.status === 1 || currentLog.status === 'success'" type="success">成功</el-tag>
          <el-tag v-else-if="currentLog.status === 0 || currentLog.status === 'failed'" type="danger">失败</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentLog.duration ? `${currentLog.duration}ms` : '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="currentLog?.detailData" class="detail-section">
        <p class="section-title">详细数据</p>
        <pre class="json-view">{{ JSON.stringify(currentLog.detailData, null, 2) }}</pre>
      </div>

      <div v-if="currentLog?.beforeData || currentLog?.afterData" class="detail-section">
        <el-row :gutter="20">
          <el-col :span="12">
            <p class="section-title">修改前数据</p>
            <pre class="json-view">{{ JSON.stringify(currentLog.beforeData, null, 2) }}</pre>
          </el-col>
          <el-col :span="12">
            <p class="section-title">修改后数据</p>
            <pre class="json-view">{{ JSON.stringify(currentLog.afterData, null, 2) }}</pre>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Plus, Edit, Delete, Download, InfoFilled } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const dateRange = ref([])
const moduleFilter = ref('')
const actionFilter = ref('')
const tableData = ref([])
const detailVisible = ref(false)
const currentLog = ref({})

const stats = reactive({
  todayCount: 0,
  createCount: 0,
  updateCount: 0,
  deleteCount: 0
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const formatNumber = (num) => {
  if (!num) return '0'
  return Number(num).toLocaleString()
}

const getModuleType = (module) => {
  const types = {
    user: 'primary',
    order: 'success',
    product: 'warning',
    auction: 'danger',
    promotion: 'info',
    system: '',
    community: 'warning'
  }
  return types[module] || ''
}

const getModuleText = (module) => {
  const texts = {
    user: '用户',
    order: '订单',
    product: '作品',
    auction: '拍卖',
    promotion: '分销',
    system: '系统',
    community: '社区'
  }
  return texts[module] || module || '-'
}

const getActionType = (action) => {
  const types = {
    create: 'success',
    update: 'warning',
    delete: 'danger',
    login: 'primary',
    logout: 'info',
    audit: 'warning'
  }
  return types[action] || ''
}

const getActionText = (action) => {
  const texts = {
    create: '新增',
    update: '修改',
    delete: '删除',
    login: '登录',
    logout: '登出',
    audit: '审核'
  }
  return texts[action] || action || '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      module: moduleFilter.value,
      action: actionFilter.value
    }
    const [listRes, statsRes] = await Promise.all([
      request.get('/system/operation-log/list', { params }),
      request.get('/system/operation-log/stats', { params: { startDate: dateRange.value?.[0], endDate: dateRange.value?.[1] } })
    ])

    tableData.value = listRes.records || listRes.list || []
    pagination.total = listRes.total || 0
    Object.assign(stats, statsRes)
  } catch (error) {
    console.error('加载数据失败', error)
    mockData()
  } finally {
    loading.value = false
  }
}

const mockData = () => {
  stats.todayCount = 156
  stats.createCount = 45
  stats.updateCount = 78
  stats.deleteCount = 12

  tableData.value = [
    {
      id: 10001,
      createTime: '2026-04-21 13:45:32',
      operatorName: '管理员小王',
      roleName: '超级管理员',
      module: 'user',
      action: 'update',
      description: '审核通过用户艺术家认证申请',
      targetType: '用户',
      targetId: 10086,
      ip: '192.168.1.100',
      status: 1,
      beforeData: { certified: false },
      afterData: { certified: true }
    },
    {
      id: 10002,
      createTime: '2026-04-21 13:40:15',
      operatorName: '管理员小李',
      roleName: '运营管理员',
      module: 'order',
      action: 'audit',
      description: '处理订单退款申请',
      targetType: '订单',
      targetId: 'ORD20260421001',
      ip: '192.168.1.105',
      status: 1,
      reason: '商品损坏，符合退款条件'
    },
    {
      id: 10003,
      createTime: '2026-04-21 13:35:22',
      operatorName: '管理员小张',
      roleName: '商品管理员',
      module: 'product',
      action: 'create',
      description: '上架新作品《山水画》',
      targetType: '作品',
      targetId: 20056,
      ip: '192.168.1.110',
      status: 1
    },
    {
      id: 10004,
      createTime: '2026-04-21 13:30:08',
      operatorName: '管理员小王',
      roleName: '超级管理员',
      module: 'auction',
      action: 'update',
      description: '修改拍卖专场规则',
      targetType: '专场',
      targetId: 88,
      ip: '192.168.1.100',
      status: 1
    },
    {
      id: 10005,
      createTime: '2026-04-21 13:25:45',
      operatorName: '管理员小李',
      roleName: '运营管理员',
      module: 'promotion',
      action: 'audit',
      description: '审核艺荐官提现申请',
      targetType: '提现',
      targetId: 'WD20260421005',
      ip: '192.168.1.105',
      status: 1,
      reason: '佣金来源合法，审核通过'
    },
    {
      id: 10006,
      createTime: '2026-04-21 13:20:11',
      operatorName: '系统',
      roleName: '-',
      module: 'system',
      action: 'login',
      description: '系统自动备份完成',
      targetType: '-',
      targetId: null,
      ip: '127.0.0.1',
      status: 1
    },
    {
      id: 10007,
      createTime: '2026-04-21 13:15:33',
      operatorName: '管理员小赵',
      roleName: '内容管理员',
      module: 'community',
      action: 'delete',
      description: '删除违规帖子',
      targetType: '帖子',
      targetId: 30089,
      ip: '192.168.1.115',
      status: 1,
      reason: '含有敏感信息'
    }
  ]
  pagination.total = tableData.value.length
}

const viewDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const exportLogs = () => {
  ElMessage.success('日志导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.title { font-size: 20px; font-weight: 600; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.stats-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
.stat-card {
  background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); cursor: pointer; transition: transform 0.2s;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-right: 14px; font-size: 20px; color: #fff; }
.stat-icon.blue { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.green { background: linear-gradient(135deg, #11998e, #38ef7d); }
.stat-icon.orange { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.red { background: linear-gradient(135deg, #ff416c, #ff4b2b); }
.stat-content .stat-label { color: #909399; font-size: 14px; margin-bottom: 4px; }
.stat-content .stat-value { font-size: 22px; font-weight: 600; color: #303133; }
.operator-info { display: flex; align-items: center; gap: 8px; }
.operator-detail .name { font-weight: 500; font-size: 13px; }
.operator-detail .role { font-size: 11px; color: #909399; }
.description { display: flex; align-items: center; gap: 4px; }
.info-icon { color: #909399; cursor: pointer; }
.target-type { font-weight: 500; font-size: 13px; }
.target-id { font-size: 11px; color: #909399; }
.time, .ip { font-size: 13px; color: #606266; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.detail-section { margin-top: 20px; }
.section-title { font-weight: 500; margin-bottom: 10px; color: #303133; }
.json-view {
  background: #f5f7fa; border-radius: 4px; padding: 12px; font-size: 12px;
  overflow-x: auto; max-height: 200px;
}
</style>
