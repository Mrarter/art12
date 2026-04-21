<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">消息模板配置</span>
      <el-button type="primary" @click="showDialog('add')">
        <el-icon><Plus /></el-icon>
        添加模板
      </el-button>
    </div>

    <!-- 模板分类 -->
    <div class="template-tabs">
      <div 
        v-for="tab in categoryTabs" 
        :key="tab.value"
        :class="['tab-item', { active: currentCategory === tab.value }]"
        @click="switchCategory(tab.value)"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
        <span class="tab-count">{{ getCategoryCount(tab.value) }}</span>
      </div>
    </div>

    <!-- 模板列表 -->
    <el-table :data="filteredTemplates" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="模板名称" width="180">
        <template #default="{ row }">
          <div class="template-name">
            <el-icon><Message /></el-icon>
            <span>{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="模板编码" width="150">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.code }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模板内容" min-width="300">
        <template #default="{ row }">
          <div class="template-content">
            <p class="content-text">{{ row.content }}</p>
            <div class="variables" v-if="row.variables?.length">
              <span class="var-label">变量：</span>
              <el-tag 
                v-for="v in row.variables" 
                :key="v"
                size="small"
                class="var-tag"
              >
                {{ `{${v}}` }}
              </el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="通知方式" width="120">
        <template #default="{ row }">
          <div class="notify-types">
            <el-tooltip v-if="row.notifyTypes.includes('sms')" content="短信">
              <el-icon class="notify-icon sms"><Iphone /></el-icon>
            </el-tooltip>
            <el-tooltip v-if="row.notifyTypes.includes('push')" content="推送">
              <el-icon class="notify-icon push"><Bell /></el-icon>
            </el-tooltip>
            <el-tooltip v-if="row.notifyTypes.includes('mail')" content="邮件">
              <el-icon class="notify-icon mail"><Mansion /></el-icon>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
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
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="showDialog('edit', row)">编辑</el-button>
          <el-button type="warning" link @click="handleTest(row)">测试</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码" prop="code">
          <el-input 
            v-model="form.code" 
            placeholder="请输入模板编码，如 order_shipped"
            :disabled="isEdit"
          />
          <div class="form-tip">建议使用下划线命名，如 order_shipped</div>
        </el-form-item>
        <el-form-item label="模板分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="订单通知" value="order" />
            <el-option label="系统通知" value="system" />
            <el-option label="活动通知" value="promotion" />
            <el-option label="安全通知" value="security" />
            <el-option label="拍卖通知" value="auction" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知方式" prop="notifyTypes">
          <el-checkbox-group v-model="form.notifyTypes">
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="push">App推送</el-checkbox>
            <el-checkbox label="mail">邮件</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="模板内容" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="6"
            placeholder="请输入模板内容，使用 {变量名} 引用变量"
          />
          <div class="form-tip">使用 {变量名} 引用变量，如：您的订单 {orderNo} 已发货</div>
        </el-form-item>
        <el-form-item label="变量配置" prop="variables">
          <div class="variables-editor">
            <div 
              v-for="(v, index) in form.variables" 
              :key="index"
              class="variable-item"
            >
              <el-input 
                v-model="form.variables[index]" 
                placeholder="变量名"
                style="width: 200px"
              />
              <el-button 
                type="danger" 
                link 
                @click="removeVariable(index)"
                :disabled="form.variables.length <= 1"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" link @click="addVariable">
              <el-icon><Plus /></el-icon>
              添加变量
            </el-button>
          </div>
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
        <el-button @click="handlePreview">预览</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试弹窗 -->
    <el-dialog v-model="testVisible" title="发送测试消息" width="500px">
      <el-form :model="testForm" label-width="100px">
        <el-form-item label="目标用户">
          <el-input v-model="testForm.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="变量值">
          <div v-for="v in testTemplate?.variables" :key="v" class="test-var">
            <label>{ {{ v }} }</label>
            <el-input v-model="testForm.variables[v]" placeholder="请输入值" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" @click="sendTestMessage" :loading="testing">发送测试</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="消息预览" width="500px">
      <div class="preview-content" v-if="previewData">
        <div class="preview-section">
          <h4>短信预览</h4>
          <div class="preview-box sms">{{ previewData.sms }}</div>
        </div>
        <div class="preview-section">
          <h4>推送预览</h4>
          <div class="preview-box push">
            <div class="push-title">{{ previewData.pushTitle }}</div>
            <div class="push-content">{{ previewData.pushContent }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Delete, Message, Iphone, Bell, Mansion,
  ShoppingCart, Setting, Gift, Lock, Sell
} from '@element-plus/icons-vue'
import request from '@/api/request'

const loading = ref(false)
const dialogVisible = ref(false)
const testVisible = ref(false)
const previewVisible = ref(false)
const formRef = ref()
const isEdit = ref(false)
const testing = ref(false)
const currentCategory = ref('all')
const testTemplate = ref(null)

const categoryTabs = [
  { label: '全部', value: 'all', icon: 'Folder' },
  { label: '订单通知', value: 'order', icon: 'ShoppingCart' },
  { label: '系统通知', value: 'system', icon: 'Setting' },
  { label: '活动通知', value: 'promotion', icon: 'Gift' },
  { label: '安全通知', value: 'security', icon: 'Lock' },
  { label: '拍卖通知', value: 'auction', icon: 'Sell' }
]

const form = reactive({
  id: null,
  name: '',
  code: '',
  category: 'order',
  notifyTypes: ['push'],
  content: '',
  variables: [''],
  status: 1
})

const testForm = reactive({
  userId: '',
  variables: {}
})

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  notifyTypes: [{ required: true, message: '请选择通知方式', trigger: 'change' }],
  content: [{ required: true, message: '请输入模板内容', trigger: 'blur' }]
}

const tableData = ref([])

const filteredTemplates = computed(() => {
  if (currentCategory.value === 'all') {
    return tableData.value
  }
  return tableData.value.filter(t => t.category === currentCategory.value)
})

const dialogTitle = computed(() => isEdit.value ? '编辑模板' : '添加模板')

const previewData = computed(() => {
  if (!form.content) return null
  
  let content = form.content
  let sms = form.content
  let pushContent = form.content
  
  // 替换变量
  form.variables.forEach(v => {
    if (v) {
      const value = `{${v}}`
      const displayValue = `[${v}值]`
      content = content.replace(new RegExp(`\\{${v}\\}`, 'g'), displayValue)
      sms = sms.replace(new RegExp(`\\{${v}\\}`, 'g'), displayValue)
      pushContent = pushContent.replace(new RegExp(`\\{${v}\\}`, 'g'), displayValue)
    }
  })
  
  return {
    sms,
    pushTitle: form.name,
    pushContent
  }
})

const getCategoryCount = (category) => {
  if (category === 'all') return tableData.value.length
  return tableData.value.filter(t => t.category === category).length
}

const switchCategory = (category) => {
  currentCategory.value = category
}

const addVariable = () => {
  form.variables.push('')
}

const removeVariable = (index) => {
  if (form.variables.length > 1) {
    form.variables.splice(index, 1)
  }
}

const showDialog = (type, row = null) => {
  if (type === 'add') {
    isEdit.value = false
    Object.assign(form, {
      id: null,
      name: '',
      code: '',
      category: 'order',
      notifyTypes: ['push'],
      content: '',
      variables: [''],
      status: 1
    })
  } else {
    isEdit.value = true
    Object.assign(form, {
      id: row.id,
      name: row.name,
      code: row.code,
      category: row.category,
      notifyTypes: [...(row.notifyTypes || ['push'])],
      content: row.content,
      variables: row.variables?.length ? [...row.variables] : [''],
      status: row.status
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    // 过滤空变量
    const submitData = {
      ...form,
      variables: form.variables.filter(v => v.trim())
    }

    if (isEdit.value) {
      const index = tableData.value.findIndex(t => t.id === form.id)
      if (index > -1) {
        tableData.value[index] = { ...tableData.value[index], ...submitData }
      }
    } else {
      tableData.value.unshift({
        id: Date.now(),
        ...submitData
      })
    }

    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleTest = (row) => {
  testTemplate.value = row
  testForm.userId = ''
  testForm.variables = {}
  row.variables?.forEach(v => {
    testForm.variables[v] = ''
  })
  testVisible.value = true
}

const sendTestMessage = async () => {
  if (!testForm.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }

  testing.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('测试消息发送成功')
    testVisible.value = false
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    testing.value = false
  }
}

const handlePreview = () => {
  if (!form.content) {
    ElMessage.warning('请输入模板内容')
    return
  }
  previewVisible.value = true
}

const handleStatusChange = async (row) => {
  // 实际项目中调用 API
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该模板吗？', '提示', { type: 'warning' })
    const index = tableData.value.findIndex(t => t.id === row.id)
    if (index > -1) {
      tableData.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  } catch (e) {}
}

const loadData = async () => {
  loading.value = true
  try {
    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 500))
    tableData.value = [
      {
        id: 1,
        name: '订单发货通知',
        code: 'order_shipped',
        category: 'order',
        notifyTypes: ['sms', 'push'],
        content: '您的订单 {orderNo} 已发货，快递单号：{expressNo}，预计 {deliveryDays} 天送达。',
        variables: ['orderNo', 'expressNo', 'deliveryDays'],
        status: 1
      },
      {
        id: 2,
        name: '订单签收通知',
        code: 'order_signed',
        category: 'order',
        notifyTypes: ['push'],
        content: '您的订单 {orderNo} 已签收，感谢您的购买！',
        variables: ['orderNo'],
        status: 1
      },
      {
        id: 3,
        name: '退款成功通知',
        code: 'refund_success',
        category: 'order',
        notifyTypes: ['sms', 'push'],
        content: '您的退款申请已通过，退款金额 ¥{amount} 将于 1-3 个工作日内退回您的账户。',
        variables: ['amount'],
        status: 1
      },
      {
        id: 4,
        name: '拍卖开始提醒',
        code: 'auction_start',
        category: 'auction',
        notifyTypes: ['push'],
        content: '您关注的「{auctionName}」拍卖即将开始，开始时间：{startTime}',
        variables: ['auctionName', 'startTime'],
        status: 1
      },
      {
        id: 5,
        name: '拍卖出价被超越',
        code: 'auction_outbid',
        category: 'auction',
        notifyTypes: ['push'],
        content: '您在「{auctionName}」的出价 ¥{bidAmount} 已被超越，当前最高价 ¥{currentPrice}',
        variables: ['auctionName', 'bidAmount', 'currentPrice'],
        status: 1
      },
      {
        id: 6,
        name: '账户异常登录',
        code: 'security_login_alert',
        category: 'security',
        notifyTypes: ['sms', 'push', 'mail'],
        content: '您的账户在新设备登录，如非本人操作请及时修改密码。登录地点：{location}，登录时间：{time}',
        variables: ['location', 'time'],
        status: 1
      },
      {
        id: 7,
        name: '优惠券到账',
        code: 'coupon_received',
        category: 'promotion',
        notifyTypes: ['push'],
        content: '恭喜您获得一张 {couponName}，有效期至 {expireTime}，快去使用吧！',
        variables: ['couponName', 'expireTime'],
        status: 1
      }
    ]
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
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

.template-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
  color: #666;
}
.tab-item:hover {
  background: #ecf5ff;
  color: #409eff;
}
.tab-item.active {
  background: #409eff;
  color: #fff;
}
.tab-count {
  background: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}
.tab-item.active .tab-count {
  background: rgba(255, 255, 255, 0.3);
}

.template-name {
  display: flex;
  align-items: center;
  gap: 8px;
}
.template-content {
  line-height: 1.6;
}
.content-text {
  margin: 0 0 8px 0;
  color: #666;
}
.variables {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.var-label {
  font-size: 12px;
  color: #999;
}
.var-tag {
  font-family: monospace;
}

.notify-types {
  display: flex;
  gap: 8px;
}
.notify-icon {
  font-size: 18px;
  cursor: pointer;
}
.notify-icon.sms {
  color: #67c23a;
}
.notify-icon.push {
  color: #409eff;
}
.notify-icon.mail {
  color: #e6a23c;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.variables-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.variable-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.test-var {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.test-var label {
  width: 100px;
  font-family: monospace;
  color: #409eff;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.preview-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #666;
}
.preview-box {
  padding: 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
}
.preview-box.sms {
  background: #f5f7fa;
  color: #333;
}
.preview-box.push {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.push-title {
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
