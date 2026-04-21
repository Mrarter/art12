<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">操作日志</span>
      <div class="header-actions">
        <el-select v-model="queryForm.module" placeholder="选择模块" clearable size="default" style="width: 150px" @change="handleQuery">
          <el-option label="用户模块" value="user" />
          <el-option label="订单模块" value="order" />
          <el-option label="作品模块" value="product" />
          <el-option label="拍卖模块" value="auction" />
          <el-option label="分销模块" value="promotion" />
          <el-option label="系统模块" value="system" />
        </el-select>
        <el-select v-model="queryForm.actionType" placeholder="操作类型" clearable size="default" style="width: 150px" @change="handleQuery">
          <el-option label="新增" value="create" />
          <el-option label="修改" value="update" />
          <el-option label="删除" value="delete" />
          <el-option label="登录" value="login" />
          <el-option label="登出" value="logout" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="default"
          style="width: 260px"
          @change="handleDateChange"
        />
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="danger" @click="handleExport" :loading="exporting">导出</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">总操作数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.today }}</span>
          <span class="stat-label">今日操作</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.errors }}</span>
          <span class="stat-label">异常操作</span>
        </div>
      </div>
    </div>

    <el-table 
      :data="tableData" 
      v-loading="loading" 
      border 
      stripe
      :height="'calc(100vh - 380px)'"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="操作时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人" width="120">
        <template #default="{ row }">
          <div class="operator-info">
            <el-avatar :size="24" :src="row.operatorAvatar">
              {{ row.operatorName?.charAt(0) }}
            </el-avatar>
            <span>{{ row.operatorName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="模块" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="getModuleTagType(row.module)">
            {{ getModuleName(row.module) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="getActionTagType(row.actionType)">
            {{ getActionName(row.actionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作内容" min-width="200">
        <template #default="{ row }">
          <div class="content-cell">
            <span class="content-text">{{ row.content }}</span>
            <el-tooltip v-if="row.detail" placement="top">
              <template #content>
                <div class="detail-content">
                  <p><strong>请求参数：</strong></p>
                  <pre>{{ formatJson(row.detail.requestParams) }}</pre>
                  <p><strong>响应结果：</strong></p>
                  <pre>{{ formatJson(row.detail.responseResult) }}</pre>
                </div>
              </template>
              <el-icon class="detail-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="IP地址" width="140">
        <template #default="{ row }">
          <span class="ip-address">{{ row.ip || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success" size="small">成功</el-tag>
          <el-tag v-else type="danger" size="small">失败</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleViewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作详情" width="700px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="操作时间">
          {{ formatDateTime(currentRow.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ currentRow.operatorName }}
        </el-descriptions-item>
        <el-descriptions-item label="模块">
          {{ getModuleName(currentRow.module) }}
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          {{ getActionName(currentRow.actionType) }}
        </el-descriptions-item>
        <el-descriptions-item label="IP地址" :span="2">
          {{ currentRow.ip || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="操作内容" :span="2">
          {{ currentRow.content }}
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="json-display">{{ formatJson(currentRow.detail?.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果" :span="2">
          <pre class="json-display" :class="{ 'error': currentRow.status !== 1 }">
            {{ formatJson(currentRow.detail?.responseResult) }}
          </pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, CircleCheck, Warning, QuestionFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const exporting = ref(false)
const detailVisible = ref(false)
const currentRow = ref(null)
const dateRange = ref([])

const tableData = ref([])
const stats = ref({
  total: 0,
  today: 0,
  errors: 0
})

const queryForm = reactive({
  module: '',
  actionType: '',
  startDate: '',
  endDate: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const getModuleName = (module) => {
  const map = {
    user: '用户模块',
    order: '订单模块',
    product: '作品模块',
    auction: '拍卖模块',
    promotion: '分销模块',
    system: '系统模块'
  }
  return map[module] || module
}

const getModuleTagType = (module) => {
  const map = {
    user: '',
    order: 'success',
    product: 'warning',
    auction: 'danger',
    promotion: 'info',
    system: ''
  }
  return map[module] || ''
}

const getActionName = (type) => {
  const map = {
    create: '新增',
    update: '修改',
    delete: '删除',
    login: '登录',
    logout: '登出',
    query: '查询'
  }
  return map[type] || type
}

const getActionTagType = (type) => {
  const map = {
    create: 'success',
    update: 'warning',
    delete: 'danger',
    login: '',
    logout: 'info',
    query: ''
  }
  return map[type] || ''
}

const formatJson = (data) => {
  if (!data) return '-'
  if (typeof data === 'string') {
    try {
      return JSON.stringify(JSON.parse(data), null, 2)
    } catch {
      return data
    }
  }
  return JSON.stringify(data, null, 2)
}

const handleDateChange = (val) => {
  if (val && val.length === 2) {
    queryForm.startDate = val[0]
    queryForm.endDate = val[1]
  } else {
    queryForm.startDate = ''
    queryForm.endDate = ''
  }
}

const handleQuery = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  queryForm.module = ''
  queryForm.actionType = ''
  queryForm.startDate = ''
  queryForm.endDate = ''
  dateRange.value = []
  handleQuery()
}

const handleExport = async () => {
  exporting.value = true
  try {
    // 模拟导出
    await new Promise(resolve => setTimeout(resolve, 1500))
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const handleViewDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...queryForm
    }
    // 实际项目中调用 API
    // const res = await request.get('/system/operation-log/list', params)
    
    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 500))
    tableData.value = generateMockData()
    pagination.total = 156
    
    // 模拟统计数据
    stats.value = {
      total: 156,
      today: 23,
      errors: 3
    }
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

const generateMockData = () => {
  const modules = ['user', 'order', 'product', 'auction', 'promotion', 'system']
  const actions = ['create', 'update', 'delete', 'login', 'logout']
  const contents = [
    '用户「张三」登录系统',
    '修改用户「李四」的角色为「管理员」',
    '新增作品「山水长卷」审核通过',
    '订单 #20240101001 已发货',
    '艺荐官「王五」申请提现 ¥5000',
    '拍卖专场「春季拍卖会」已创建',
    '用户「赵六」完成艺术家认证'
  ]
  
  return Array.from({ length: 20 }, (_, i) => {
    const module = modules[Math.floor(Math.random() * modules.length)]
    const action = actions[Math.floor(Math.random() * actions.length)]
    return {
      id: 1000 - (pagination.page - 1) * 20 - i,
      createTime: Date.now() - Math.random() * 86400000 * 7,
      operatorName: ['管理员', '张三', '李四', '系统'][Math.floor(Math.random() * 4)],
      operatorAvatar: '',
      module,
      actionType: action,
      content: contents[Math.floor(Math.random() * contents.length)],
      ip: `192.168.1.${Math.floor(Math.random() * 255)}`,
      status: Math.random() > 0.1 ? 1 : 0,
      detail: {
        requestParams: { id: Math.floor(Math.random() * 1000) },
        responseResult: { code: 0, message: '操作成功' }
      }
    }
  })
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
  flex-wrap: wrap;
  gap: 16px;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.stats-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}
.stat-icon.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.stat-icon.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}
.stat-icon.orange {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}
.stat-label {
  font-size: 14px;
  color: #999;
}

.operator-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.content-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.content-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-icon {
  cursor: pointer;
  color: #409eff;
}
.ip-address {
  font-family: monospace;
  font-size: 12px;
}
.detail-content {
  max-width: 500px;
}
.detail-content pre {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
}
.json-display {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.json-display.error {
  background: #fff2f0;
  color: #ff4d4f;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0;
}
</style>
