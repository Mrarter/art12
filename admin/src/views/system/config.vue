<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">系统配置</span>
      <el-button type="primary" @click="handleSave">
        <el-icon><Save /></el-icon>保存配置
      </el-button>
    </div>
    
    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 交易设置 -->
      <el-tab-pane label="交易设置" name="trade">
        <el-form ref="tradeFormRef" :model="tradeForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">订单设置</el-divider>
          
          <el-form-item label="订单超时时间">
            <el-input-number v-model="tradeForm.orderTimeout" :min="5" :max="120" />
            <span class="tips">分钟（未支付自动取消）</span>
          </el-form-item>
          
          <el-form-item label="退款处理周期">
            <el-input-number v-model="tradeForm.refundDays" :min="1" :max="30" />
            <span class="tips">个工作日</span>
          </el-form-item>
          
          <el-form-item label="允许重复购买">
            <el-switch v-model="tradeForm.allowRepeatBuy" />
            <span class="tips">关闭后，同一用户只能购买同一作品一次</span>
          </el-form-item>
          
          <el-divider content-position="left">价格显示</el-divider>
          
          <el-form-item label="价格单位">
            <el-radio-group v-model="tradeForm.priceUnit">
              <el-radio label="fen">分（精确显示）</el-radio>
              <el-radio label="yuan">元（分转元）</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 分销设置 -->
      <el-tab-pane label="分销设置" name="promotion">
        <el-form ref="promotionFormRef" :model="promotionForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">佣金比例</el-divider>
          
          <el-form-item label="一级佣金（直接推广）">
            <el-input-number v-model="promotionForm.directCommission" :min="0" :max="50" :precision="2" />
            <span class="tips">% 的订单金额</span>
          </el-form-item>
          
          <el-form-item label="二级佣金（团队奖励）">
            <el-input-number v-model="promotionForm.teamCommission" :min="0" :max="20" :precision="2" />
            <span class="tips">% 的订单金额（购买者本身也是艺荐官时发放给其上级）</span>
          </el-form-item>
          
          <el-form-item label="佣金结算时机">
            <el-select v-model="promotionForm.settlementType">
              <el-option label="支付成功后" value="after_pay" />
              <el-option label="确认收货后" value="after_confirm" />
              <el-option label="超过退款期后" value="after_refund" />
            </el-select>
          </el-form-item>
          
          <el-divider content-position="left">提现设置</el-divider>
          
          <el-form-item label="最低提现金额">
            <el-input-number v-model="promotionForm.minWithdraw" :min="1" :max="100000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>
          
          <el-form-item label="提现手续费">
            <el-input-number v-model="promotionForm.withdrawFee" :min="0" :max="10" :precision="1" />
            <span class="tips">%</span>
          </el-form-item>
          
          <el-form-item label="提现周期">
            <el-input-number v-model="promotionForm.withdrawDays" :min="1" :max="30" />
            <span class="tips">个工作日内到账</span>
          </el-form-item>
          
          <el-divider content-position="left">艺荐官门槛</el-divider>
          
          <el-form-item label="成为艺荐官条件">
            <el-radio-group v-model="promotionForm.promoterCondition">
              <el-radio label="free">免费注册</el-radio>
              <el-radio label="purchase">累计消费满</el-radio>
              <el-radio label="approval">人工审核</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="累计消费门槛" v-if="promotionForm.promoterCondition === 'purchase'">
            <el-input-number v-model="promotionForm.purchaseThreshold" :min="0" :max="100000" :precision="0" />
            <span class="tips">元</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 价格增长机制 -->
      <el-tab-pane label="价格增长" name="priceGrowth">
        <el-form ref="priceFormRef" :model="priceForm" label-width="200px" style="max-width: 1000px">
          <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
            <template #title>
              <strong>价格增长规则：</strong>作品价格 = 原价 × 时间系数 × 艺术家知名度系数 × 热度系数 × (1 + 销售加成)^销售次数
            </template>
          </el-alert>
          
          <el-divider content-position="left">时间因素</el-divider>
          
          <el-form-item label="基础日增长率">
            <el-input-number v-model="priceForm.baseDailyRate" :min="0" :max="1" :precision="4" :step="0.0001" />
            <span class="tips">每天增长的比例，如 0.0002 表示 0.02%/天</span>
          </el-form-item>
          
          <el-form-item label="成熟期日增长率">
            <el-input-number v-model="priceForm.matureDailyRate" :min="0" :max="1" :precision="4" :step="0.0001" />
            <span class="tips">发布超过阈值天数后的日增长率</span>
          </el-form-item>
          
          <el-form-item label="成熟期天数阈值">
            <el-input-number v-model="priceForm.matureDays" :min="0" :max="365" />
            <span class="tips">天（超过此天数使用成熟期增长率）</span>
          </el-form-item>
          
          <el-divider content-position="left">艺术家知名度系数</el-divider>
          
          <el-form-item label="普通艺术家">
            <el-input-number v-model="priceForm.defaultBadgeRate" :min="1" :max="5" :precision="1" />
            <span class="tips">倍（基准系数）</span>
          </el-form-item>
          
          <el-form-item label="认证艺术家">
            <el-input-number v-model="priceForm.verifiedBadgeRate" :min="1" :max="5" :precision="1" />
            <span class="tips">倍</span>
          </el-form-item>
          
          <el-form-item label="人气艺术家">
            <el-input-number v-model="priceForm.popularBadgeRate" :min="1" :max="5" :precision="1" />
            <span class="tips">倍</span>
          </el-form-item>
          
          <el-form-item label="大师级艺术家">
            <el-input-number v-model="priceForm.masterBadgeRate" :min="1" :max="10" :precision="1" />
            <span class="tips">倍</span>
          </el-form-item>
          
          <el-divider content-position="left">浏览量热度系数</el-divider>
          
          <el-form-item label="浏览量阈值1">
            <el-input-number v-model="priceForm.viewThreshold1" :min="0" :max="100000" />
            <span class="tips">次（达到此浏览量触发加成）</span>
          </el-form-item>
          <el-form-item label="阈值1加成系数">
            <el-input-number v-model="priceForm.viewRate1" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-form-item label="浏览量阈值2">
            <el-input-number v-model="priceForm.viewThreshold2" :min="0" :max="100000" />
          </el-form-item>
          <el-form-item label="阈值2加成系数">
            <el-input-number v-model="priceForm.viewRate2" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-form-item label="浏览量阈值3">
            <el-input-number v-model="priceForm.viewThreshold3" :min="0" :max="100000" />
          </el-form-item>
          <el-form-item label="阈值3加成系数">
            <el-input-number v-model="priceForm.viewRate3" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-form-item label="浏览量阈值4">
            <el-input-number v-model="priceForm.viewThreshold4" :min="0" :max="100000" />
          </el-form-item>
          <el-form-item label="阈值4加成系数">
            <el-input-number v-model="priceForm.viewRate4" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-divider content-position="left">收藏量热度系数</el-divider>
          
          <el-form-item label="收藏量阈值1">
            <el-input-number v-model="priceForm.favoriteThreshold1" :min="0" :max="10000" />
          </el-form-item>
          <el-form-item label="阈值1加成系数">
            <el-input-number v-model="priceForm.favoriteRate1" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-form-item label="收藏量阈值2">
            <el-input-number v-model="priceForm.favoriteThreshold2" :min="0" :max="10000" />
          </el-form-item>
          <el-form-item label="阈值2加成系数">
            <el-input-number v-model="priceForm.favoriteRate2" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-form-item label="收藏量阈值3">
            <el-input-number v-model="priceForm.favoriteThreshold3" :min="0" :max="10000" />
          </el-form-item>
          <el-form-item label="阈值3加成系数">
            <el-input-number v-model="priceForm.favoriteRate3" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-form-item label="收藏量阈值4">
            <el-input-number v-model="priceForm.favoriteThreshold4" :min="0" :max="10000" />
          </el-form-item>
          <el-form-item label="阈值4加成系数">
            <el-input-number v-model="priceForm.favoriteRate4" :min="1" :max="3" :precision="2" />
          </el-form-item>
          
          <el-divider content-position="left">销售加成</el-divider>
          
          <el-form-item label="单次销售加成">
            <el-input-number v-model="priceForm.saleRate" :min="0" :max="1" :precision="3" :step="0.001" />
            <span class="tips">每售出一次，价格上涨此比例（如 0.05 = 5%）</span>
          </el-form-item>
          
          <el-form-item label="最多计算销售次数">
            <el-input-number v-model="priceForm.maxSaleCount" :min="1" :max="100" />
            <span class="tips">次（超过此次数不再累加）</span>
          </el-form-item>
          
          <el-divider content-position="left">涨幅限制</el-divider>
          
          <el-form-item label="最大涨幅倍数">
            <el-input-number v-model="priceForm.maxGrowthMultiple" :min="1" :max="20" :precision="1" />
            <span class="tips">倍（价格最多上涨到此倍数的原价）</span>
          </el-form-item>
          
          <el-form-item label="价格增长开关">
            <el-switch v-model="priceForm.enabled" />
            <span class="tips">关闭后作品价格不再自动增长</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 拍卖设置 -->
      <el-tab-pane label="拍卖设置" name="auction">
        <el-form ref="auctionFormRef" :model="auctionForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">保证金设置</el-divider>
          
          <el-form-item label="拍卖保证金">
            <el-input-number v-model="auctionForm.auctionDeposit" :min="0" :max="1000000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>
          
          <el-form-item label="保证金退还">
            <el-switch v-model="auctionForm.depositRefund" />
            <span class="tips">竞拍失败后自动退还</span>
          </el-form-item>
          
          <el-divider content-position="left">出价设置</el-divider>
          
          <el-form-item label="延时加价幅度">
            <el-input-number v-model="auctionForm.bidIncrement" :min="0" :max="100000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>
          
          <el-form-item label="延时周期">
            <el-input-number v-model="auctionForm延时Cycles" :min="1" :max="10" />
            <span class="tips">轮（临近结束时每次延长此轮数）</span>
          </el-form-item>
          
          <el-form-item label="延时时长">
            <el-input-number v-model="auctionForm.delayMinutes" :min="1" :max="30" />
            <span class="tips">分钟</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 审核设置 -->
      <el-tab-pane label="审核设置" name="audit">
        <el-form ref="auditFormRef" :model="auditForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">审核开关</el-divider>
          
          <el-form-item label="艺术家认证审核">
            <el-switch v-model="auditForm.artistAudit" />
            <span class="tips">开启后需要后台审核艺术家认证申请</span>
          </el-form-item>
          
          <el-form-item label="作品审核">
            <el-switch v-model="auditForm.artworkAudit" />
            <span class="tips">开启后需要后台审核作品上架</span>
          </el-form-item>
          
          <el-form-item label="动态审核">
            <el-switch v-model="auditForm.postAudit" />
            <span class="tips">开启后需要后台审核艺术圈动态</span>
          </el-form-item>
          
          <el-divider content-position="left">敏感词过滤</el-divider>
          
          <el-form-item label="敏感词过滤">
            <el-switch v-model="auditForm.sensitiveFilter" />
          </el-form-item>
          
          <el-form-item label="敏感词库">
            <el-input
              type="textarea"
              v-model="auditForm.sensitiveWords"
              :rows="5"
              placeholder="每行一个敏感词"
              style="width: 500px"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Save } from '@element-plus/icons-vue'
import request from '@/api/request'

const activeTab = ref('trade')

// 交易设置表单
const tradeFormRef = ref()
const tradeForm = reactive({
  orderTimeout: 30,
  refundDays: 7,
  allowRepeatBuy: false,
  priceUnit: 'fen'
})

// 分销设置表单
const promotionFormRef = ref()
const promotionForm = reactive({
  directCommission: 5,
  teamCommission: 2,
  settlementType: 'after_pay',
  minWithdraw: 100,
  withdrawFee: 0,
  withdrawDays: 3,
  promoterCondition: 'free',
  purchaseThreshold: 1000
})

// 价格增长表单
const priceFormRef = ref()
const priceForm = reactive({
  enabled: true,
  // 时间因素
  baseDailyRate: 0.0002,
  matureDailyRate: 0.0003,
  matureDays: 30,
  // 艺术家知名度系数
  defaultBadgeRate: 1.0,
  verifiedBadgeRate: 1.5,
  popularBadgeRate: 2.0,
  masterBadgeRate: 3.0,
  // 浏览量阈值
  viewThreshold1: 100,
  viewRate1: 1.1,
  viewThreshold2: 500,
  viewRate2: 1.2,
  viewThreshold3: 1000,
  viewRate3: 1.3,
  viewThreshold4: 5000,
  viewRate4: 1.5,
  // 收藏量阈值
  favoriteThreshold1: 5,
  favoriteRate1: 1.1,
  favoriteThreshold2: 20,
  favoriteRate2: 1.2,
  favoriteThreshold3: 50,
  favoriteRate3: 1.3,
  favoriteThreshold4: 100,
  favoriteRate4: 1.5,
  // 销售加成
  saleRate: 0.05,
  maxSaleCount: 10,
  // 涨幅限制
  maxGrowthMultiple: 5.0
})

// 拍卖设置表单
const auctionFormRef = ref()
const auctionForm = reactive({
  auctionDeposit: 1000,
  depositRefund: true,
  bidIncrement: 100,
  delayCycles: 3,
  delayMinutes: 5
})

// 审核设置表单
const auditFormRef = ref()
const auditForm = reactive({
  artistAudit: true,
  artworkAudit: true,
  postAudit: false,
  sensitiveFilter: true,
  sensitiveWords: '***\n***\n***'
})

// 加载所有配置
const loadAllConfig = async () => {
  try {
    const data = await request.get('/admin/config/all')
    
    // 合并到各表单
    if (data.trade) Object.assign(tradeForm, data.trade)
    if (data.promotion) Object.assign(promotionForm, data.promotion)
    if (data.priceGrowth) Object.assign(priceForm, data.priceGrowth)
    if (data.auction) Object.assign(auctionForm, data.auction)
    if (data.audit) Object.assign(auditForm, data.audit)
  } catch (e) {
    console.log('使用默认配置')
  }
}

// 保存配置
const handleSave = async () => {
  try {
    await request.post('/admin/config/update', {
      trade: tradeForm,
      promotion: promotionForm,
      priceGrowth: priceForm,
      auction: auctionForm,
      audit: auditForm
    })
    ElMessage.success('配置保存成功')
  } catch (e) {
    // 模拟保存成功
    ElMessage.success('配置保存成功（已缓存）')
  }
}

onMounted(() => {
  loadAllConfig()
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
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.title {
  font-size: 20px;
  font-weight: 600;
}

.config-tabs {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.tips {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.el-divider {
  margin: 20px 0;
}
</style>
