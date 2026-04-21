<template>
  <div class="page-container">
    <div class="page-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <span class="title">订单详情</span>
    </div>

    <div class="detail-content" v-if="orderDetail">
      <!-- 订单状态 -->
      <el-card class="status-card">
        <div class="status-header">
          <div class="status-info">
            <el-icon v-if="getStatusIcon(orderDetail.status)" :class="'status-icon-' + orderDetail.status">
              <component :is="getStatusIcon(orderDetail.status)" />
            </el-icon>
            <div class="status-text">
              <span class="status-title">{{ getStatusTitle(orderDetail.status) }}</span>
              <span class="status-desc">{{ getStatusDesc(orderDetail.status) }}</span>
            </div>
          </div>
          <el-button v-if="orderDetail.status === 'paid'" type="primary" @click="handleShip">
            立即发货
          </el-button>
        </div>
      </el-card>

      <!-- 基本信息 -->
      <el-card class="info-card">
        <template #header>
          <span class="card-title">订单信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ orderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ orderDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">
            <el-tag v-if="orderDetail.type === 'auction'" type="warning">拍卖订单</el-tag>
            <el-tag v-else type="primary">普通订单</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            {{ getPayMethod(orderDetail.payMethod) }}
          </el-descriptions-item>
          <el-descriptions-item label="买家留言" :span="2">
            {{ orderDetail.remark || '无' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 买家信息 -->
      <el-card class="info-card">
        <template #header>
          <span class="card-title">买家信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="买家昵称">{{ orderDetail.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ orderDetail.buyerPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ orderDetail.address }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 商品信息 -->
      <el-card class="info-card">
        <template #header>
          <span class="card-title">商品信息</span>
        </template>
        <div class="product-list">
          <div class="product-item" v-for="(item, index) in orderDetail.products" :key="index">
            <el-image :src="item.cover" class="product-cover" fit="cover" />
            <div class="product-info">
              <p class="product-name">{{ item.title }}</p>
              <p class="product-author">{{ item.artistName }}</p>
              <p class="product-spec">{{ item.spec }}</p>
            </div>
            <div class="product-price">
              <span class="price">¥{{ item.price }}</span>
              <span class="count">x{{ item.count }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 金额信息 -->
      <el-card class="info-card">
        <template #header>
          <span class="card-title">金额明细</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品总额">
            <span class="amount">¥{{ orderDetail.goodsAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="运费">
            <span class="amount">¥{{ orderDetail.freight }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="优惠券">
            <span class="amount discount">-¥{{ orderDetail.couponAmount || 0 }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span class="amount total">¥{{ orderDetail.payAmount }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 物流信息 -->
      <el-card class="info-card" v-if="orderDetail.status !== 'pending' && orderDetail.status !== 'cancelled'">
        <template #header>
          <div class="card-header">
            <span class="card-title">物流信息</span>
            <el-button type="primary" link @click="editLogistics">编辑物流</el-button>
          </div>
        </template>
        <div v-if="orderDetail.logistics" class="logistics-info">
          <div class="logistics-main">
            <el-icon><Van /></el-icon>
            <span class="express-name">{{ orderDetail.logistics.expressName }}</span>
            <span class="express-no">{{ orderDetail.logistics.expressNo }}</span>
            <el-button type="primary" link @click="copyExpressNo">复制</el-button>
          </div>
          <div class="logistics-trace">
            <el-timeline>
              <el-timeline-item 
                v-for="(trace, index) in orderDetail.logistics.traces" 
                :key="index"
                :timestamp="trace.time"
                :type="index === 0 ? 'primary' : 'info'"
                :hollow="index !== 0"
              >
                {{ trace.desc }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
        <div v-else class="no-logistics">
          <el-button type="primary" @click="editLogistics">添加物流信息</el-button>
        </div>
      </el-card>
    </div>

    <!-- 编辑物流弹窗 -->
    <el-dialog v-model="logisticsDialogVisible" title="编辑物流信息" width="500px">
      <el-form ref="logisticsFormRef" :model="logisticsForm" :rules="logisticsRules" label-width="100px">
        <el-form-item label="快递公司" prop="expressName">
          <el-select v-model="logisticsForm.expressName" placeholder="请选择快递公司">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="申通快递" value="申通快递" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号" prop="expressNo">
          <el-input v-model="logisticsForm.expressNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="logisticsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLogistics">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Van, CircleCheck, Clock, Package, Warning } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const orderDetail = ref(null)
const logisticsDialogVisible = ref(false)
const logisticsFormRef = ref()

const logisticsForm = reactive({
  expressName: '',
  expressNo: ''
})

const logisticsRules = {
  expressName: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  expressNo: [{ required: true, message: '请输入快递单号', trigger: 'blur' }]
}

const getStatusIcon = (status) => {
  const map = {
    pending: Clock,
    paid: Package,
    shipped: Van,
    completed: CircleCheck,
    cancelled: Warning
  }
  return map[status]
}

const getStatusTitle = (status) => {
  const map = {
    pending: '待付款',
    paid: '待发货',
    shipped: '运输中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

const getStatusDesc = (status) => {
  const map = {
    pending: '买家尚未完成付款，请等待',
    paid: '买家已付款，请尽快发货',
    shipped: '商品正在运输中',
    completed: '订单已完成，感谢您的购买',
    cancelled: '订单已取消'
  }
  return map[status] || ''
}

const getPayMethod = (method) => {
  const map = { wechat: '微信支付', balance: '余额支付', alipay: '支付宝' }
  return map[method] || method
}

const goBack = () => {
  router.back()
}

const handleShip = () => {
  logisticsDialogVisible.value = true
}

const editLogistics = () => {
  if (orderDetail.value?.logistics) {
    logisticsForm.expressName = orderDetail.value.logistics.expressName
    logisticsForm.expressNo = orderDetail.value.logistics.expressNo
  }
  logisticsDialogVisible.value = true
}

const copyExpressNo = () => {
  navigator.clipboard.writeText(orderDetail.value.logistics.expressNo)
  ElMessage.success('单号已复制')
}

const saveLogistics = async () => {
  const valid = await logisticsFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  // 本地模拟
  orderDetail.value.logistics = {
    expressName: logisticsForm.expressName,
    expressNo: logisticsForm.expressNo,
    traces: [
      { time: new Date().toLocaleString('zh-CN'), desc: '包裹已发出，正在运输中' }
    ]
  }
  orderDetail.value.status = 'shipped'
  logisticsDialogVisible.value = false
  ElMessage.success('物流信息已保存')
}

onMounted(() => {
  const orderId = route.query.orderId || 'SYJ20240120001'
  
  // 模拟订单数据
  orderDetail.value = {
    orderNo: orderId,
    createTime: '2024-01-20 14:15:00',
    type: 'normal',
    payMethod: 'wechat',
    remark: '请尽快发货，谢谢',
    buyerName: '李四',
    buyerPhone: '138****8002',
    address: '广东省深圳市南山区科技园xx路xx号xx室',
    status: 'paid',
    goodsAmount: 32000,
    freight: 200,
    couponAmount: 100,
    payAmount: 32100,
    products: [
      {
        cover: 'https://pic.imgdb.cn/item/1.jpg',
        title: '油画风景·夕阳',
        artistName: '王建国',
        spec: '原作 80x60cm',
        price: 32000,
        count: 1
      }
    ],
    logistics: null
  }
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.detail-content {
  .status-card {
    margin-bottom: 20px;
    border-left: 4px solid #667eea;
  }

  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .status-info {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .status-icon-pending,
  .status-icon-paid,
  .status-icon-shipped,
  .status-icon-completed,
  .status-icon-cancelled {
    font-size: 40px;
  }

  .status-icon-pending { color: #e6a23c; }
  .status-icon-paid { color: #409eff; }
  .status-icon-shipped { color: #67c23a; }
  .status-icon-completed { color: #67c23a; }
  .status-icon-cancelled { color: #909399; }

  .status-text {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .status-title {
    font-size: 20px;
    font-weight: 600;
    color: #333;
  }

  .status-desc {
    font-size: 14px;
    color: #999;
  }

  .info-card {
    margin-bottom: 20px;
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .product-list {
    .product-item {
      display: flex;
      align-items: center;
      padding: 20px 0;
      border-bottom: 1px solid #f0f0f0;
    }

    .product-item:last-child {
      border-bottom: none;
    }

    .product-cover {
      width: 100px;
      height: 100px;
      border-radius: 8px;
      margin-right: 16px;
    }

    .product-info {
      flex: 1;

      .product-name {
        font-size: 16px;
        font-weight: 500;
        color: #333;
        margin: 0 0 8px 0;
      }

      .product-author {
        font-size: 14px;
        color: #666;
        margin: 0 0 4px 0;
      }

      .product-spec {
        font-size: 12px;
        color: #999;
        margin: 0;
      }
    }

    .product-price {
      display: flex;
      flex-direction: column;
      align-items: flex-end;

      .price {
        font-size: 18px;
        font-weight: 600;
        color: #f56c6c;
      }

      .count {
        font-size: 14px;
        color: #999;
      }
    }
  }

  .amount {
    font-weight: 600;
    color: #333;
  }

  .amount.discount {
    color: #67c23a;
  }

  .amount.total {
    font-size: 20px;
    color: #f56c6c;
  }

  .logistics-info {
    .logistics-main {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #f5f6f8;
      border-radius: 8px;
      margin-bottom: 20px;

      .express-name {
        font-weight: 600;
        color: #333;
      }

      .express-no {
        color: #666;
      }
    }

    .logistics-trace {
      max-height: 300px;
      overflow-y: auto;
    }
  }

  .no-logistics {
    text-align: center;
    padding: 40px 0;
  }
}
</style>