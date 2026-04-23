<template>
  <view class="withdraw-log-page">
    <!-- 状态筛选 -->
    <view class="filter-tabs">
      <view 
        class="tab-item" 
        :class="{ active: statusFilter === 'all' }"
        @click="changeStatus('all')"
      >
        全部
      </view>
      <view 
        class="tab-item" 
        :class="{ active: statusFilter === 'pending' }"
        @click="changeStatus('pending')"
      >
        待处理
      </view>
      <view 
        class="tab-item" 
        :class="{ active: statusFilter === 'processing' }"
        @click="changeStatus('processing')"
      >
        处理中
      </view>
      <view 
        class="tab-item" 
        :class="{ active: statusFilter === 'completed' }"
        @click="changeStatus('completed')"
      >
        已到账
      </view>
      <view 
        class="tab-item" 
        :class="{ active: statusFilter === 'rejected' }"
        @click="changeStatus('rejected')"
      >
        已拒绝
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-section">
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-label">累计提现</text>
          <text class="stat-value primary">¥{{ stats.totalAmount }}</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-label">待处理</text>
          <text class="stat-value warning">¥{{ stats.pendingAmount }}</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-label">已到账</text>
          <text class="stat-value success">¥{{ stats.completedAmount }}</text>
        </view>
      </view>
    </view>

    <!-- 提现记录列表 -->
    <view class="log-list">
      <view class="list-item" v-for="item in filteredList" :key="item.id" @click="showDetail(item)">
        <view class="item-header">
          <view class="item-left">
            <text class="item-amount">¥{{ item.amount }}</text>
            <view class="status-badge" :class="item.status">
              {{ getStatusText(item.status) }}
            </view>
          </view>
          <view class="item-right">
            <text class="item-time">{{ item.createTime }}</text>
          </view>
        </view>

        <view class="item-body">
          <view class="info-row">
            <text class="info-label">手续费</text>
            <text class="info-value">¥{{ item.fee }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">实际到账</text>
            <text class="info-value highlight">¥{{ item.actualAmount }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">到账方式</text>
            <text class="info-value">{{ item.paymentMethod }}</text>
          </view>
          <view class="info-row" v-if="item.completedTime">
            <text class="info-label">到账时间</text>
            <text class="info-value">{{ item.completedTime }}</text>
          </view>
          <view class="info-row" v-if="item.reason">
            <text class="info-label">拒绝原因</text>
            <text class="info-value error">{{ item.reason }}</text>
          </view>
        </view>

        <view class="item-footer" v-if="item.status === 'pending'">
          <view class="action-btn cancel" @click.stop="cancelWithdraw(item)">取消申请</view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="filteredList.length === 0">
        
        <text class="empty-text">暂无提现记录</text>
        <view class="empty-action" @click="goWithdraw">去提现</view>
      </view>
    </view>

    <!-- 加载更多 -->
    <view class="load-more" v-if="filteredList.length > 0 && hasMore">
      <text v-if="loading">加载中...</text>
      <text v-else @click="loadMore">加载更多</text>
    </view>

    <!-- 详情弹窗 -->
    <!-- 弹窗开始 -->
      <view class="detail-modal" v-if="currentItem">
        <view class="modal-title">提现详情</view>
        
        <view class="detail-section">
          <view class="section-title">基本信息</view>
          <view class="detail-row">
            <text class="detail-label">申请编号</text>
            <text class="detail-value">{{ currentItem.id }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">申请时间</text>
            <text class="detail-value">{{ currentItem.createTime }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">提现金额</text>
            <text class="detail-value primary">¥{{ currentItem.amount }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">手续费</text>
            <text class="detail-value">¥{{ currentItem.fee }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">实际到账</text>
            <text class="detail-value highlight">¥{{ currentItem.actualAmount }}</text>
          </view>
        </view>

        <view class="detail-section">
          <view class="section-title">到账信息</view>
          <view class="detail-row">
            <text class="detail-label">到账方式</text>
            <text class="detail-value">{{ currentItem.paymentMethod }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">到账账号</text>
            <text class="detail-value">{{ currentItem.account }}</text>
          </view>
          <view class="detail-row" v-if="currentItem.completedTime">
            <text class="detail-label">到账时间</text>
            <text class="detail-value">{{ currentItem.completedTime }}</text>
          </view>
        </view>

        <view class="detail-section" v-if="currentItem.reason">
          <view class="section-title">处理信息</view>
          <view class="detail-row">
            <text class="detail-label">状态</text>
            <text class="detail-value error">{{ getStatusText(currentItem.status) }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">原因</text>
            <text class="detail-value error">{{ currentItem.reason }}</text>
          </view>
        </view>

        <view class="detail-section" v-if="currentItem.traces && currentItem.traces.length > 0">
          <view class="section-title">处理进度</view>
          <view class="trace-timeline">
            <view class="trace-item" v-for="(trace, index) in currentItem.traces" :key="index">
              <view class="trace-dot" :class="{ active: index === 0 }"></view>
              <view class="trace-content">
                <view class="trace-title">{{ trace.title }}</view>
                <view class="trace-time">{{ trace.time }}</view>
              </view>
            </view>
          </view>
        </view>
      </view>
<!-- 弹窗结束 -->
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

// 状态
const statusFilter = ref('all')
const loading = ref(false)
const hasMore = ref(true)
const page = ref(1)
const showDetailModal = ref(false)
const currentItem = ref(null)

// 统计数据
const stats = ref({
  totalAmount: 56800.00,
  pendingAmount: 2888.00,
  completedAmount: 52120.00
})

// 提现记录列表（模拟数据）
const allList = ref([
  {
    id: 'WD202604210001',
    amount: 2888.00,
    fee: 14.44,
    actualAmount: 2873.56,
    paymentMethod: '微信零钱',
    account: 'wechat_***8821',
    status: 'pending',
    createTime: '2026-04-21 14:30:00',
    completedTime: null,
    reason: null,
    traces: [
      { title: '提现申请已提交', time: '2026-04-21 14:30:00' }
    ]
  },
  {
    id: 'WD202604190002',
    amount: 5200.00,
    fee: 26.00,
    actualAmount: 5174.00,
    paymentMethod: '微信零钱',
    account: 'wechat_***8821',
    status: 'completed',
    createTime: '2026-04-19 10:20:00',
    completedTime: '2026-04-20 09:15:00',
    reason: null,
    traces: [
      { title: '提现申请已提交', time: '2026-04-19 10:20:00' },
      { title: '审核通过', time: '2026-04-19 14:00:00' },
      { title: '财务处理中', time: '2026-04-19 16:30:00' },
      { title: '已到账', time: '2026-04-20 09:15:00' }
    ]
  },
  {
    id: 'WD202604150003',
    amount: 1500.00,
    fee: 7.50,
    actualAmount: 1492.50,
    paymentMethod: '微信零钱',
    account: 'wechat_***8821',
    status: 'rejected',
    createTime: '2026-04-15 09:00:00',
    completedTime: null,
    reason: '账户信息有误，请核实后重新申请',
    traces: [
      { title: '提现申请已提交', time: '2026-04-15 09:00:00' },
      { title: '审核中', time: '2026-04-15 11:00:00' },
      { title: '审核拒绝', time: '2026-04-15 14:00:00' }
    ]
  },
  {
    id: 'WD202604120004',
    amount: 3800.00,
    fee: 19.00,
    actualAmount: 3781.00,
    paymentMethod: '微信零钱',
    account: 'wechat_***8821',
    status: 'completed',
    createTime: '2026-04-12 16:00:00',
    completedTime: '2026-04-14 10:30:00',
    reason: null,
    traces: [
      { title: '提现申请已提交', time: '2026-04-12 16:00:00' },
      { title: '审核通过', time: '2026-04-12 18:00:00' },
      { title: '财务处理中', time: '2026-04-13 09:00:00' },
      { title: '已到账', time: '2026-04-14 10:30:00' }
    ]
  },
  {
    id: 'WD202604100005',
    amount: 2000.00,
    fee: 10.00,
    actualAmount: 1990.00,
    paymentMethod: '微信零钱',
    account: 'wechat_***8821',
    status: 'completed',
    createTime: '2026-04-10 11:00:00',
    completedTime: '2026-04-11 15:20:00',
    reason: null,
    traces: []
  }
])

// 筛选后的列表
const filteredList = computed(() => {
  if (statusFilter.value === 'all') {
    return allList.value
  }
  return allList.value.filter(item => item.status === statusFilter.value)
})

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    pending: '待处理',
    processing: '处理中',
    completed: '已到账',
    rejected: '已拒绝'
  }
  return map[status] || status
}

// 切换状态筛选
const changeStatus = (status) => {
  statusFilter.value = status
  page.value = 1
  hasMore.value = true
}

// 加载更多
const loadMore = () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  setTimeout(() => {
    page.value++
    loading.value = false
    hasMore.value = false // 模拟数据加载完毕
  }, 1000)
}

// 查看详情
const showDetail = (item) => {
  currentItem.value = item
  showDetailModal.value = true
}

// 取消提现
const cancelWithdraw = (item) => {
  uni.showModal({
    title: '提示',
    content: '确定要取消此提现申请吗？',
    success: (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '处理中...' })
        setTimeout(() => {
          uni.hideLoading()
          uni.showToast({ title: '已取消', icon: 'success' })
          item.status = 'rejected'
          item.reason = '用户主动取消'
        }, 1000)
      }
    }
  })
}

// 去提现
const goWithdraw = () => {
  uni.navigateBack()
}

onLoad(() => {
  // 加载提现记录
})
</script>

<style lang="scss" scoped>
.withdraw-log-page {
  min-height: 100vh;
  background: #f5f6f8;
}

.filter-tabs {
  display: flex;
  background: #fff;
  padding: 20rpx 0;
  position: sticky;
  top: 0;
  z-index: 10;

  .tab-item {
    flex: 1;
    text-align: center;
    font-size: 28rpx;
    color: #666;
    padding: 16rpx 0;
    position: relative;

    &.active {
      color: #667eea;
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 4rpx;
        background: #667eea;
        border-radius: 2rpx;
      }
    }
  }
}

.stats-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;

  .stats-row {
    display: flex;
    justify-content: space-between;

    .stat-item {
      flex: 1;
      text-align: center;

      .stat-label {
        font-size: 24rpx;
        color: #999;
        display: block;
        margin-bottom: 8rpx;
      }

      .stat-value {
        font-size: 32rpx;
        font-weight: 600;
        color: #333;

        &.primary {
          color: #667eea;
        }

        &.warning {
          color: #ff7f50;
        }

        &.success {
          color: #07c160;
        }
      }
    }

    .stat-divider {
      width: 1rpx;
      background: #eee;
    }
  }
}

.log-list {
  padding: 0 20rpx;

  .list-item {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;

    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;
      padding-bottom: 20rpx;
      border-bottom: 1rpx solid #f0f0f0;

      .item-left {
        display: flex;
        align-items: center;

        .item-amount {
          font-size: 36rpx;
          font-weight: bold;
          color: #333;
          margin-right: 16rpx;
        }

        .status-badge {
          padding: 4rpx 12rpx;
          border-radius: 8rpx;
          font-size: 22rpx;

          &.pending {
            background: rgba(255, 127, 80, 0.1);
            color: #ff7f50;
          }

          &.processing {
            background: rgba(102, 126, 234, 0.1);
            color: #667eea;
          }

          &.completed {
            background: rgba(7, 193, 96, 0.1);
            color: #07c160;
          }

          &.rejected {
            background: rgba(255, 69, 58, 0.1);
            color: #ff453a;
          }
        }
      }

      .item-right {
        .item-time {
          font-size: 24rpx;
          color: #999;
        }
      }
    }

    .item-body {
      .info-row {
        display: flex;
        justify-content: space-between;
        padding: 8rpx 0;

        .info-label {
          font-size: 26rpx;
          color: #999;
        }

        .info-value {
          font-size: 26rpx;
          color: #333;

          &.highlight {
            color: #07c160;
            font-weight: 500;
          }

          &.error {
            color: #ff453a;
          }
        }
      }
    }

    .item-footer {
      margin-top: 20rpx;
      padding-top: 20rpx;
      border-top: 1rpx solid #f0f0f0;
      display: flex;
      justify-content: flex-end;

      .action-btn {
        padding: 8rpx 24rpx;
        border-radius: 8rpx;
        font-size: 24rpx;

        &.cancel {
          background: #f5f6f8;
          color: #666;
        }
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;

  .empty-text {
    font-size: 28rpx;
    color: #999;
    margin-top: 20rpx;
  }

  .empty-action {
    margin-top: 30rpx;
    padding: 16rpx 48rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;
  }
}

.load-more {
  text-align: center;
  padding: 30rpx;
  font-size: 26rpx;
  color: #999;
}

// 详情弹窗
.detail-modal {
  padding: 30rpx;
  width: 650rpx;
  max-height: 80vh;
  overflow-y: auto;

  .modal-title {
    font-size: 34rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 30rpx;
  }

  .detail-section {
    margin-bottom: 30rpx;

    .section-title {
      font-size: 28rpx;
      font-weight: 500;
      color: #333;
      margin-bottom: 16rpx;
      padding-left: 16rpx;
      border-left: 4rpx solid #667eea;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 12rpx 0;

      .detail-label {
        font-size: 26rpx;
        color: #999;
      }

      .detail-value {
        font-size: 26rpx;
        color: #333;

        &.primary {
          color: #e74c3c;
          font-weight: 500;
        }

        &.highlight {
          color: #07c160;
          font-weight: 500;
        }

        &.error {
          color: #ff453a;
        }
      }
    }
  }

  .trace-timeline {
    .trace-item {
      display: flex;
      padding: 16rpx 0;

      .trace-dot {
        width: 16rpx;
        height: 16rpx;
        border-radius: 50%;
        background: #ddd;
        margin-right: 16rpx;
        margin-top: 6rpx;
        flex-shrink: 0;

        &.active {
          background: #667eea;
        }
      }

      .trace-content {
        .trace-title {
          font-size: 26rpx;
          color: #333;
          margin-bottom: 4rpx;
        }

        .trace-time {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }
}
</style>
