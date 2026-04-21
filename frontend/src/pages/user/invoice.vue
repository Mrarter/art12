<template>
  <view class="invoice-page">
    <!-- 发票类型 -->
    <view class="section">
      <text class="section-title">发票类型</text>
      <view class="type-options">
        <view class="type-item" :class="{ active: invoiceType === 'normal' }" @click="invoiceType = 'normal'">
          <text class="type-name">普通发票</text>
          <text class="type-desc">可开具个人或企业</text>
        </view>
        <view class="type-item" :class="{ active: invoiceType === 'special' }" @click="invoiceType = 'special'">
          <text class="type-name">增值税专用发票</text>
          <text class="type-desc">仅限企业用户</text>
        </view>
      </view>
    </view>

    <!-- 发票信息 -->
    <view class="section">
      <text class="section-title">发票信息</text>
      <view class="form-list">
        <view class="form-item">
          <text class="form-label">发票抬头</text>
          <input class="form-input" placeholder="请输入发票抬头" v-model="invoiceTitle" />
        </view>
        <view class="form-item" v-if="invoiceType === 'normal'">
          <text class="form-label">税号</text>
          <input class="form-input" placeholder="选填" v-model="taxNumber" />
        </view>
        <view class="form-item">
          <text class="form-label">发票内容</text>
          <picker :value="contentIndex" :range="contentList" @change="onContentChange">
            <view class="picker-value">
              {{ contentList[contentIndex] }}
              <text class="arrow">></text>
            </view>
          </picker>
        </view>
      </view>
    </view>

    <!-- 收票人信息 -->
    <view class="section">
      <text class="section-title">收票人信息</text>
      <view class="form-list">
        <view class="form-item">
          <text class="form-label">收票人</text>
          <input class="form-input" placeholder="请输入收票人姓名" v-model="receiverName" />
        </view>
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input class="form-input" type="number" placeholder="请输入手机号" v-model="receiverPhone" />
        </view>
        <view class="form-item">
          <text class="form-label">所在地区</text>
          <picker mode="region" @change="onRegionChange">
            <view class="picker-value">
              {{ regionText || '请选择所在地区' }}
              <text class="arrow">></text>
            </view>
          </picker>
        </view>
        <view class="form-item">
          <text class="form-label">详细地址</text>
          <input class="form-input" placeholder="请输入详细地址" v-model="address" />
        </view>
      </view>
    </view>

    <!-- 订单选择 -->
    <view class="section">
      <text class="section-title">关联订单</text>
      <view class="order-item" v-for="(order, index) in orders" :key="index" @click="selectOrder(order)">
        <checkbox :checked="selectedOrders.includes(order.id)" />
        <view class="order-info">
          <text class="order-no">订单号：{{ order.no }}</text>
          <text class="order-amount">¥{{ order.amount }}</text>
        </view>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-bar">
      <button class="submit-btn" @click="submitInvoice">提交申请</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      invoiceType: 'normal',
      invoiceTitle: '',
      taxNumber: '',
      contentIndex: 0,
      contentList: ['艺术品', '服务费', '咨询费', '设计费'],
      receiverName: '',
      receiverPhone: '',
      region: [],
      regionText: '',
      address: '',
      orders: [
        { id: 1, no: 'ORD202403150001', amount: '168,000' },
        { id: 2, no: 'ORD202403140002', amount: '98,000' }
      ],
      selectedOrders: []
    }
  },
  methods: {
    onContentChange(e) {
      this.contentIndex = e.detail.value
    },
    onRegionChange(e) {
      this.region = e.detail.value
      this.regionText = this.region.join('')
    },
    selectOrder(order) {
      const index = this.selectedOrders.indexOf(order.id)
      if (index > -1) {
        this.selectedOrders.splice(index, 1)
      } else {
        this.selectedOrders.push(order.id)
      }
    },
    submitInvoice() {
      if (!this.invoiceTitle) {
        return uni.showToast({ title: '请输入发票抬头', icon: 'none' })
      }
      if (!this.receiverName || !this.receiverPhone || !this.address) {
        return uni.showToast({ title: '请完善收票人信息', icon: 'none' })
      }
      if (this.selectedOrders.length === 0) {
        return uni.showToast({ title: '请选择关联订单', icon: 'none' })
      }
      uni.showToast({ title: '提交成功', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1500)
    }
  }
}
</script>

<style lang="scss" scoped>
.invoice-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 150rpx;
}

.section {
  background: #fff;
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.type-options {
  display: flex;
  gap: 20rpx;
}

.type-item {
  flex: 1;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 24rpx;
  border: 4rpx solid transparent;
}

.type-item.active {
  background: #f0f4ff;
  border-color: #667eea;
}

.type-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.type-desc {
  font-size: 22rpx;
  color: #999;
}

.form-list {
  display: flex;
  flex-direction: column;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
}

.form-item:last-child {
  border-bottom: none;
}

.form-label {
  font-size: 28rpx;
  color: #333;
  min-width: 150rpx;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  text-align: right;
}

.picker-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  text-align: right;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.arrow {
  color: #ccc;
  margin-left: 8rpx;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
}

.order-item:last-child {
  margin-bottom: 0;
}

.order-info {
  flex: 1;
}

.order-no {
  font-size: 26rpx;
  color: #666;
  display: block;
  margin-bottom: 8rpx;
}

.order-amount {
  font-size: 28rpx;
  color: #e63946;
  font-weight: 600;
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
  border: none;
}
</style>