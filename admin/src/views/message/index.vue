<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">消息通知管理</span>
      <div class="header-actions">
        <el-select v-model="typeFilter" placeholder="消息类型" clearable @change="loadData">
          <el-option label="系统通知" value="system" />
          <el-option label="订单通知" value="order" />
          <el-option label="拍卖通知" value="auction" />
          <el-option label="活动通知" value="activity" />
          <el-option label="审核通知" value="audit" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="发送状态" clearable @change="loadData">
          <el-option label="待发送" value="pending" />
          <el-option label="已发送" value="sent" />
          <el-option label="发送失败" value="failed" />
        </el-select>
        <el-button type="primary" @click="showSendDialog">
          <el-icon><Plus /></el-icon>
          发送消息
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card" @click="statusFilter = ''; typeFilter = ''; loadData()">
        <div class="stat-icon blue">
          <el-icon><Message /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">消息总数</p>
          <p class="stat-value">{{ formatNumber(stats.total) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><SuccessFilled /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">今日发送</p>
          <p class="stat-value">{{ formatNumber(stats.todaySent) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">待发送</p>
          <p class="stat-value">{{ formatNumber(stats.pending) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <div class="stat-content">
          <p class="stat-label">发送失败</p>
          <p class="stat-value">{{ formatNumber(stats.failed) }}</p>
        </div>
      </div>
    </div>

    <!-- 消息模板列表 -->
    <el-card shadow="hover" class="template-card">
      <template #header>
        <div class="card-header">
          <span>消息模板</span>
          <el-button type="success" @click="showTemplateDialog">
            <el-icon><Plus /></el-icon>
            添加模板
          </el-button>
        </div>
      </template>
      <el-table :data="templateList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeType(row.type)" size="small">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题模板" min-width="200" />
        <el-table-column prop="content" label="内容模板" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="toggleTemplate(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="editTemplate(row)">编辑</el-button>
            <el-button type="success" link @click="useTemplate(row)">使用</el-button>
            <el-button type="danger" link @click="deleteTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 消息发送记录 -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>发送记录</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索接收人/内容"
            style="width: 200px"
            clearable
          />
        </div>
      </template>
      <el-table :data="filteredMessageList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="发送时间" width="160">
          <template #default="{ row }">
            {{ row.createTime || row.sentAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeType(row.type)" size="small">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="消息标题" min-width="180" />
        <el-table-column prop="content" label="消息内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="接收人" width="140">
          <template #default="{ row }">
            <div v-if="row.targetType === 'all'" class="target-tag">
              <el-tag type="success" size="small">全部用户</el-tag>
            </div>
            <div v-else-if="row.targetType === 'group'" class="target-tag">
              <el-tag type="warning" size="small">用户群</el-tag>
              <span class="target-name">{{ row.targetName || row.groupName }}</span>
            </div>
            <div v-else class="user-info">
              <el-avatar :src="row.userAvatar" :size="24" />
              <span>{{ row.userName || row.nickname || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发送方式" width="100" align="center">
          <template #default="{ row }">
            <div class="send-methods">
              <el-tag v-if="row.push" type="primary" size="small">推送</el-tag>
              <el-tag v-if="row.sms" type="success" size="small">短信</el-tag>
              <el-tag v-if="row.email" type="warning" size="small">邮件</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'sent' || row.status === 1" type="success" size="small">已发送</el-tag>
            <el-tag v-else-if="row.status === 'failed' || row.status === -1" type="danger" size="small">失败</el-tag>
            <el-tag v-else type="info" size="small">待发送</el-tag>
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

    <!-- 发送消息弹窗 -->
    <el-dialog v-model="sendDialogVisible" title="发送消息" width="600px">
      <el-form ref="sendFormRef" :model="sendForm" :rules="sendRules" label-width="100px">
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="sendForm.type" placeholder="请选择消息类型">
            <el-option label="系统通知" value="system" />
            <el-option label="订单通知" value="order" />
            <el-option label="拍卖通知" value="auction" />
            <el-option label="活动通知" value="activity" />
            <el-option label="审核通知" value="audit" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="sendForm.title" placeholder="请输入消息标题" />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="sendForm.content" type="textarea" :rows="4" placeholder="请输入消息内容" />
        </el-form-item>
        <el-form-item label="接收人" prop="targetType">
          <el-radio-group v-model="sendForm.targetType">
            <el-radio label="all">全部用户</el-radio>
            <el-radio label="group">指定用户群</el-radio>
            <el-radio label="single">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="sendForm.targetType === 'group'" label="用户群">
          <el-select v-model="sendForm.groupId" placeholder="请选择用户群">
            <el-option label="新用户（注册7天内）" value="new_user" />
            <el-option label="活跃用户" value="active_user" />
            <el-option label="VIP用户" value="vip_user" />
            <el-option label="艺术家" value="artist" />
            <el-option label="艺荐官" value="promoter" />
            <el-option label="未消费用户" value="no_purchase" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="sendForm.targetType === 'single'" label="用户手机号">
          <el-input v-model="sendForm.phone" placeholder="请输入用户手机号" />
        </el-form-item>
        <el-form-item label="发送方式">
          <el-checkbox-group v-model="sendForm.methods">
            <el-checkbox label="push">站内推送</el-checkbox>
            <el-checkbox label="sms">短信通知</el-checkbox>
            <el-checkbox label="email">邮件通知</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="定时发送">
          <el-switch v-model="sendForm.timing" />
        </el-form-item>
        <el-form-item v-if="sendForm.timing" label="发送时间">
          <el-date-picker
            v-model="sendForm.sendTime"
            type="datetime"
            placeholder="选择发送时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSend">确定发送</el-button>
      </template>
    </el-dialog>

    <!-- 模板编辑弹窗 -->
    <el-dialog v-model="templateDialogVisible" :title="isEditTemplate ? '编辑模板' : '添加模板'" width="600px">
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="templateForm.type" placeholder="请选择消息类型">
            <el-option label="系统通知" value="system" />
            <el-option label="订单通知" value="order" />
            <el-option label="拍卖通知" value="auction" />
            <el-option label="活动通知" value="activity" />
            <el-option label="审核通知" value="audit" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题模板" prop="title">
          <el-input v-model="templateForm.title" placeholder="支持变量：{username}、{orderNo}" />
        </el-form-item>
        <el-form-item label="内容模板" prop="content">
          <el-input v-model="templateForm.content" type="textarea" :rows="5" placeholder="支持变量：{username}、{orderNo}、{amount}" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTemplate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 消息详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="消息详情" width="600px">
      <el-descriptions :column="2" border v-if="currentMessage">
        <el-descriptions-item label="消息ID">{{ currentMessage.id }}</el-descriptions-item>
        <el-descriptions-item label="消息类型">{{ getTypeText(currentMessage.type) }}</el-descriptions-item>
        <el-descriptions-item label="消息标题" :span="2">{{ currentMessage.title }}</el-descriptions-item>
        <el-descriptions-item label="消息内容" :span="2">{{ currentMessage.content }}</el-descriptions-item>
        <el-descriptions-item label="接收人类型">
          {{ currentMessage.targetType === 'all' ? '全部用户' : currentMessage.targetType === 'group' ? '用户群' : '指定用户' }}
        </el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ currentMessage.sentAt || currentMessage.createTime }}</el-descriptions-item>
        <el-descriptions-item label="发送方式" :span="2">
          <span v-if="currentMessage.push">站内推送 </span>
          <span v-if="currentMessage.sms">短信 </span>
          <span v-if="currentMessage.email">邮件 </span>
        </el-descriptions-item>
        <el-descriptions-item label="发送状态">
          <el-tag v-if="currentMessage.status === 'sent' || currentMessage.status === 1" type="success">已发送</el-tag>
          <el-tag v-else-if="currentMessage.status === 'failed' || currentMessage.status === -1" type="danger">失败</el-tag>
          <el-tag v-else type="info">待发送</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发送成功数">{{ currentMessage.successCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="发送失败数">{{ currentMessage.failCount || 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Message, SuccessFilled, Clock, CircleCloseFilled, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const typeFilter = ref('')
const statusFilter = ref('')
const searchKeyword = ref('')
const sendDialogVisible = ref(false)
const templateDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const sendFormRef = ref()
const templateFormRef = ref()
const isEditTemplate = ref(false)

const stats = reactive({
  total: 0,
  todaySent: 0,
  pending: 0,
  failed: 0
})

const templateList = ref([])
const messageList = ref([])
const currentMessage = ref({})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const sendForm = reactive({
  type: 'system',
  title: '',
  content: '',
  targetType: 'all',
  groupId: '',
  phone: '',
  methods: ['push'],
  timing: false,
  sendTime: ''
})

const sendRules = {
  type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }]
}

const templateForm = reactive({
  id: null,
  name: '',
  type: 'system',
  title: '',
  content: ''
})

const templateRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容模板', trigger: 'blur' }]
}

const filteredMessageList = computed(() => {
  if (!searchKeyword.value) return messageList.value
  return messageList.value.filter(item =>
    item.title?.includes(searchKeyword.value) ||
    item.content?.includes(searchKeyword.value) ||
    item.userName?.includes(searchKeyword.value) ||
    item.nickname?.includes(searchKeyword.value)
  )
})

const formatNumber = (num) => {
  if (!num) return '0'
  return Number(num).toLocaleString()
}

const getTypeType = (type) => {
  const types = { system: '', order: 'success', auction: 'warning', activity: 'danger', audit: 'info' }
  return types[type] || ''
}

const getTypeText = (type) => {
  const texts = { system: '系统', order: '订单', auction: '拍卖', activity: '活动', audit: '审核' }
  return texts[type] || type || '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      type: typeFilter.value,
      status: statusFilter.value
    }
    const [messageRes, templateRes, statsRes] = await Promise.all([
      request.get('/message/list', { params }),
      request.get('/message/template/list'),
      request.get('/message/stats')
    ])
    messageList.value = messageRes.records || messageRes.list || []
    pagination.total = messageRes.total || 0
    templateList.value = templateRes.list || []
    Object.assign(stats, statsRes)
  } catch (error) {
    console.error('加载数据失败', error)
    mockData()
  } finally {
    loading.value = false
  }
}

const mockData = () => {
  stats.total = 15680
  stats.todaySent = 328
  stats.pending = 12
  stats.failed = 45

  templateList.value = [
    { id: 1, name: '订单支付成功', type: 'order', title: '订单支付成功通知', content: '尊敬的{username}，您的订单{orderNo}已支付成功，金额{amount}元', status: 1 },
    { id: 2, name: '订单发货通知', type: 'order', title: '订单已发货', content: '尊敬的{username}，您的订单{orderNo}已发货，快递单号：{expressNo}', status: 1 },
    { id: 3, name: '拍卖开始提醒', type: 'auction', title: '拍卖即将开始', content: '{username}，您关注的【{lotTitle}】拍卖即将开始，快来参与吧！', status: 1 },
    { id: 4, name: '竞拍成功通知', type: 'auction', title: '恭喜竞拍成功', content: '恭喜{username}，您成功竞得【{lotTitle}】，成交价{price}元', status: 1 },
    { id: 5, name: '艺术家认证结果', type: 'audit', title: '认证审核结果', content: '{username}，您的艺术家认证申请已{result}，{reason}', status: 1 },
    { id: 6, name: '系统公告', type: 'system', title: '系统维护通知', content: '【系统维护】{content}，给您带来不便敬请谅解。', status: 1 }
  ]

  messageList.value = [
    { id: 1001, type: 'system', title: '系统升级通知', content: '平台将于本周日凌晨2:00-6:00进行系统升级，届时部分功能将暂停使用。', targetType: 'all', sentAt: '2026-04-21 10:00:00', status: 'sent', push: true, sms: false, email: false, successCount: 15680, failCount: 0 },
    { id: 1002, type: 'order', title: '订单发货通知', content: '您的订单ORD20260420001已发货，快递单号SF1234567890', targetType: 'single', nickname: '收藏家A', userAvatar: '', sentAt: '2026-04-21 09:30:00', status: 'sent', push: true, sms: true, email: false, successCount: 1, failCount: 0 },
    { id: 1003, type: 'auction', title: '拍卖即将开始', content: '【当代艺术精品专场】将于今晚8点开始，快来参与吧！', targetType: 'group', groupName: '活跃用户', sentAt: '2026-04-21 08:00:00', status: 'sent', push: true, sms: false, email: false, successCount: 856, failCount: 12 },
    { id: 1004, type: 'audit', title: '艺术家认证通过', content: '恭喜您通过艺术家认证，现在可以发布作品了！', targetType: 'single', nickname: '画家张三', userAvatar: '', sentAt: '2026-04-20 16:30:00', status: 'sent', push: true, sms: false, email: false, successCount: 1, failCount: 0 },
    { id: 1005, type: 'activity', title: '五一活动预告', content: '五一期间全场作品享8折优惠，详情请点击查看。', targetType: 'all', createTime: '2026-04-21 14:00:00', status: 'pending', push: true, sms: false, email: false, successCount: 0, failCount: 0 }
  ]
  pagination.total = messageList.value.length
}

const showSendDialog = () => {
  sendForm.type = 'system'
  sendForm.title = ''
  sendForm.content = ''
  sendForm.targetType = 'all'
  sendForm.methods = ['push']
  sendForm.timing = false
  sendDialogVisible.value = true
}

const confirmSend = async () => {
  await sendFormRef.value.validate()
  try {
    await request.post('/message/send', sendForm)
    ElMessage.success('消息发送成功')
    sendDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('发送失败，请重试')
  }
}

const showTemplateDialog = () => {
  isEditTemplate.value = false
  templateForm.id = null
  templateForm.name = ''
  templateForm.type = 'system'
  templateForm.title = ''
  templateForm.content = ''
  templateDialogVisible.value = true
}

const editTemplate = (row) => {
  isEditTemplate.value = true
  Object.assign(templateForm, row)
  templateDialogVisible.value = true
}

const useTemplate = (row) => {
  sendForm.type = row.type
  sendForm.title = row.title
  sendForm.content = row.content
  showSendDialog()
}

const confirmTemplate = async () => {
  await templateFormRef.value.validate()
  try {
    if (isEditTemplate.value) {
      await request.put(`/message/template/${templateForm.id}`, templateForm)
      ElMessage.success('模板更新成功')
    } else {
      await request.post('/message/template', templateForm)
      ElMessage.success('模板创建成功')
    }
    templateDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  }
}

const deleteTemplate = async (row) => {
  await ElMessageBox.confirm('确定删除该模板吗？', '提示', { type: 'warning' })
  try {
    await request.delete(`/message/template/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const toggleTemplate = async (row) => {
  try {
    await request.put(`/message/template/${row.id}/status`, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('更新失败')
  }
}

const viewDetail = (row) => {
  currentMessage.value = row
  detailDialogVisible.value = true
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
.template-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.user-info { display: flex; align-items: center; gap: 6px; }
.target-tag { display: flex; align-items: center; gap: 6px; }
.target-name { font-size: 12px; color: #606266; }
.send-methods { display: flex; gap: 4px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
