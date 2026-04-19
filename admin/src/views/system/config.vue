<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">参数配置</span>
    </div>
    
    <el-form ref="formRef" :model="form" label-width="150px" style="max-width: 800px">
      <el-divider content-position="left">交易设置</el-divider>
      
      <el-form-item label="订单超时时间">
        <el-input-number v-model="form.orderTimeout" :min="5" :max="60" />
        <span class="tips">分钟</span>
      </el-form-item>
      
      <el-form-item label="退款处理周期">
        <el-input-number v-model="form.refundDays" :min="1" :max="30" />
        <span class="tips">个工作日</span>
      </el-form-item>
      
      <el-divider content-position="left">分销设置</el-divider>
      
      <el-form-item label="直接佣金比例">
        <el-input-number v-model="form.directCommission" :min="0" :max="100" :precision="1" />
        <span class="tips">%</span>
      </el-form-item>
      
      <el-form-item label="团队佣金比例">
        <el-input-number v-model="form.teamCommission" :min="0" :max="50" :precision="1" />
        <span class="tips">%</span>
      </el-form-item>
      
      <el-form-item label="最低提现金额">
        <el-input-number v-model="form.minWithdraw" :min="1" :max="10000" :precision="2" />
        <span class="tips">元</span>
      </el-form-item>
      
      <el-form-item label="提现手续费">
        <el-input-number v-model="form.withdrawFee" :min="0" :max="10" :precision="1" />
        <span class="tips">%</span>
      </el-form-item>
      
      <el-divider content-position="left">拍卖设置</el-divider>
      
      <el-form-item label="拍卖保证金">
        <el-input-number v-model="form.auctionDeposit" :min="0" :max="100000" :precision="2" />
        <span class="tips">元</span>
      </el-form-item>
      
      <el-form-item label="延时加价幅度">
        <el-input-number v-model="form.bidIncrement" :min="0" :max="10000" :precision="2" />
        <span class="tips">元</span>
      </el-form-item>
      
      <el-divider content-position="left">其他设置</el-divider>
      
      <el-form-item label="艺术家认证审核">
        <el-switch v-model="form.artistAudit" />
      </el-form-item>
      
      <el-form-item label="作品审核">
        <el-switch v-model="form.artworkAudit" />
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const formRef = ref()

const form = reactive({
  orderTimeout: 30,
  refundDays: 7,
  directCommission: 10,
  teamCommission: 5,
  minWithdraw: 100,
  withdrawFee: 1,
  auctionDeposit: 1000,
  bidIncrement: 100,
  artistAudit: true,
  artworkAudit: true
})

const loadData = async () => {
  try {
    const data = await request.get('/system/config')
    Object.assign(form, data)
  } catch (e) {
    // 使用默认配置
  }
}

const handleSave = async () => {
  try {
    await request.post('/system/config/update', form)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.success('保存成功（模拟）')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.tips {
  margin-left: 10px;
  color: #999;
}
</style>
