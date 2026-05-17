<template>
  <view class="page">
    <!-- 空状态 -->
    <view v-if="!loading && list.length === 0" class="empty-state">
      <text class="empty-icon">+</text>
      <text class="empty-title">暂无收款账户</text>
      <text class="empty-desc">添加收款账户后即可提现佣金和收益</text>
    </view>

    <!-- 账户列表 -->
    <view v-else class="account-list">
      <view
        v-for="item in list"
        :key="item.id"
        class="account-card"
        :class="{ default: item.isDefault }"
        @click="onSelect(item)"
      >
        <view class="card-left">
          <view class="icon-circle" :class="item.icon">
            <text class="icon-text">{{ typeLabel(item.accountType) }}</text>
          </view>
        </view>
        <view class="card-body">
          <view class="card-top">
            <text class="type-label">{{ item.accountTypeText }}</text>
            <view v-if="item.isDefault" class="default-badge">默认</view>
          </view>
          <text class="account-info">{{ displayInfo(item) }}</text>
          <text class="real-name">{{ item.realName }}</text>
        </view>
        <view class="card-actions">
          <text class="del-btn" @click.stop="onDelete(item)">删除</text>
        </view>
      </view>
    </view>

    <!-- 加载 -->
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 底部添加按钮 -->
    <view class="bottom-bar">
      <button class="add-btn" @click="goAdd">添加收款账户</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPayAccountList, deletePayAccount, setDefaultPayAccount } from '@/api/pay'

const loading = ref(false)
const list = ref([])

const typeLabel = (t) => ({ 1: '微', 2: '支', 3: '银' })[t] || '?'

const displayInfo = (item) => {
  if (item.accountType === 1) return '微信收款'
  if (item.accountType === 2) return item.alipayAccount || '支付宝收款'
  if (item.accountType === 3) return item.bankCard || item.bankName || '银行卡'
  return ''
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await getPayAccountList()
    list.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const onSelect = async (item) => {
  if (item.isDefault) return
  try {
    await setDefaultPayAccount(item.id)
    uni.showToast({ title: '已设为默认', icon: 'success' })
    await loadList()
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

const onDelete = (item) => {
  uni.showModal({
    title: '提示',
    content: '确定删除该收款账户？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deletePayAccount(item.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        await loadList()
      } catch (e) {
        uni.showToast({ title: e.message || '删除失败', icon: 'none' })
      }
    }
  })
}

const goAdd = () => {
  uni.navigateTo({ url: '/pages/user/pay-account/add' })
}

onMounted(() => { loadList() })
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #0d0d0d;
  padding: 24rpx 24rpx 160rpx;
  box-sizing: border-box;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}

.empty-title {
  color: #f5f5f5;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.empty-desc {
  color: #888;
  font-size: 26rpx;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.account-card {
  background: #1a1a1a;
  border-radius: 20rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  position: relative;
}

.account-card.default {
  border-color: rgba(212, 175, 55, 0.3);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.06), #1a1a1a);
}

.icon-circle {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-circle.wechat { background: rgba(7, 193, 96, 0.15); }
.icon-circle.alipay { background: rgba(22, 119, 255, 0.15); }
.icon-circle.bank { background: rgba(212, 175, 55, 0.15); }

.icon-text {
  font-size: 28rpx;
  font-weight: 800;
}

.icon-circle.wechat .icon-text { color: #07c160; }
.icon-circle.alipay .icon-text { color: #1677ff; }
.icon-circle.bank .icon-text { color: #d4af37; }

.card-body {
  flex: 1;
  min-width: 0;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.type-label {
  color: #f5f5f5;
  font-size: 28rpx;
  font-weight: 700;
}

.default-badge {
  background: rgba(212, 175, 55, 0.2);
  color: #d4af37;
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 999rpx;
}

.account-info {
  color: #b3b3b3;
  font-size: 26rpx;
  display: block;
  margin-bottom: 4rpx;
}

.real-name {
  color: #888;
  font-size: 22rpx;
}

.card-actions {
  flex-shrink: 0;
}

.del-btn {
  color: #ff6b6b;
  font-size: 24rpx;
  padding: 8rpx 16rpx;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding-top: 200rpx;
}

.loading-text {
  color: #888;
  font-size: 26rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(13, 13, 13, 0.95);
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.add-btn {
  height: 88rpx;
  border-radius: 999rpx;
  background: #d4af37;
  color: #16130b;
  font-size: 30rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  width: 100%;
}
</style>
