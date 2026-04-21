<template>
  <view class="logistics-page">
    <view class="header">
      <view class="page-title">物流查询</view>
      <view class="search-box">
        <u-icon name="search" size="18" color="#999"></u-icon>
        <input 
          class="search-input" 
          v-model="keyword" 
          placeholder="输入订单号/快递单号/收件人"
          @confirm="searchLogistics"
        />
        <u-icon v-if="keyword" name="close-circle" size="16" color="#999" @click="clearKeyword"></u-icon>
      </view>
    </view>

    <!-- 物流公司统计 -->
    <view class="stats-section">
      <view class="stats-grid">
        <view class="stats-item">
          <view class="stats-value">{{ stats.total }}</view>
          <view class="stats-label">总订单数</view>
        </view>
        <view class="stats-item">
          <view class="stats-value">{{ stats.inTransit }}</view>
          <view class="stats-label">运输中</view>
        </view>
        <view class="stats-item">
          <view class="stats-value">{{ stats.delivered }}</view>
          <view class="stats-label">已签收</view>
        </view>
        <view class="stats-item">
          <view class="stats-value">{{ stats.exception }}</view>
          <view class="stats-label">异常件</view>
        </view>
      </view>
    </view>

    <!-- 物流列表 -->
    <view class="logistics-list">
      <view class="list-header">
        <view 
          class="tab-item" 
          :class="{ active: statusFilter === 'all' }"
          @click="changeStatus('all')"
        >
          全部
        </view>
        <view 
          class="tab-item" 
          :class="{ active: statusFilter === 'in_transit' }"
          @click="changeStatus('in_transit')"
        >
          运输中
        </view>
        <view 
          class="tab-item" 
          :class="{ active: statusFilter === 'delivered' }"
          @click="changeStatus('delivered')"
        >
          已签收
        </view>
        <view 
          class="tab-item" 
          :class="{ active: statusFilter === 'exception' }"
          @click="changeStatus('exception')"
        >
          异常
        </view>
      </view>

      <view class="logistics-card" v-for="item in filteredList" :key="item.id">
        <view class="card-header">
          <view class="order-info">
            <text class="order-no">订单号: {{ item.orderNo }}</text>
            <text class="express-no">快递单号: {{ item.expressNo }}</text>
          </view>
          <view class="status-tag" :class="item.status">{{ getStatusText(item.status) }}</view>
        </view>

        <view class="card-body">
          <view class="receiver-info">
            <view class="info-row">
              <u-icon name="account" size="14" color="#666"></u-icon>
              <text>{{ item.receiverName }}</text>
            </view>
            <view class="info-row">
              <u-icon name="phone" size="14" color="#666"></u-icon>
              <text>{{ item.receiverPhone }}</text>
            </view>
            <view class="info-row">
              <u-icon name="map" size="14" color="#666"></u-icon>
              <text class="address">{{ item.address }}</text>
            </view>
          </view>

          <view class="express-info">
            <view class="express-company">
              <image class="company-logo" :src="item.companyLogo" mode="aspectFit"></image>
              <text>{{ item.expressCompany }}</text>
            </view>
            <view class="express-time">{{ item.lastUpdateTime }}</view>
          </view>
        </view>

        <!-- 物流轨迹 -->
        <view class="tracking-section">
          <view class="tracking-header" @click="toggleExpand(item.id)">
            <text>物流轨迹</text>
            <u-icon :name="expandedIds.includes(item.id) ? 'arrow-up' : 'arrow-down'" size="14" color="#999"></u-icon>
          </view>
          
          <view class="tracking-timeline" v-if="expandedIds.includes(item.id)">
            <view class="timeline-item" v-for="(trace, index) in item.traces" :key="index" :class="{ latest: index === 0 }">
              <view class="timeline-dot"></view>
              <view class="timeline-content">
                <view class="trace-desc">{{ trace.desc }}</view>
                <view class="trace-time">{{ trace.time }}</view>
              </view>
            </view>
          </view>
        </view>

        <view class="card-footer">
          <view class="action-btn" @click="refreshTrace(item)">
            <u-icon name="refresh" size="14"></u-icon>
            <text>刷新</text>
          </view>
          <view class="action-btn" @click="copyExpressNo(item.expressNo)">
            <u-icon name="copy" size="14"></u-icon>
            <text>复制单号</text>
          </view>
          <view class="action-btn" @click="handleException(item)" v-if="item.status === 'exception'">
            <u-icon name="warning" size="14"></u-icon>
            <text>处理异常</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="filteredList.length === 0">
        <u-icon name="file-text" size="80" color="#ddd"></u-icon>
        <text class="empty-text">暂无物流信息</text>
      </view>
    </view>

    <!-- 异常处理弹窗 -->
    <u-popup v-model="showExceptionModal" mode="center" border-radius="16">
      <view class="exception-modal">
        <view class="modal-title">处理物流异常</view>
        <view class="modal-body">
          <view class="form-item">
            <text class="label">异常原因</text>
            <textarea class="textarea" v-model="exceptionForm.reason" placeholder="请输入异常原因"></textarea>
          </view>
          <view class="form-item">
            <text class="label">处理方式</text>
            <radio-group @change="onExceptionTypeChange">
              <label class="radio-item">
                <radio value="re_deliver" />
                <text>重新发货</text>
              </label>
              <label class="radio-item">
                <radio value="refund" />
                <text>退款处理</text>
              </label>
              <label class="radio-item">
                <radio value="contact" />
                <text>联系买家</text>
              </label>
            </radio-group>
          </view>
        </view>
        <view class="modal-footer">
          <view class="btn cancel" @click="showExceptionModal = false">取消</view>
          <view class="btn confirm" @click="submitException">确认处理</view>
        </view>
      </view>
    </u-popup>

    <!-- 分页 -->
    <view class="pagination" v-if="total > pageSize">
      <u-button size="mini" :disabled="page <= 1" @click="prevPage">上一页</u-button>
      <text class="page-info">{{ page }}/{{ totalPage }}</text>
      <u-button size="mini" :disabled="page >= totalPage" @click="nextPage">下一页</u-button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      keyword: '',
      statusFilter: 'all',
      page: 1,
      pageSize: 10,
      total: 0,
      expandedIds: [],
      showExceptionModal: false,
      exceptionForm: {
        orderId: null,
        reason: '',
        type: 'contact'
      },
      stats: {
        total: 0,
        inTransit: 0,
        delivered: 0,
        exception: 0
      },
      logisticsList: []
    }
  },

  computed: {
    totalPage() {
      return Math.ceil(this.total / this.pageSize)
    },
    filteredList() {
      if (this.statusFilter === 'all') {
        return this.logisticsList
      }
      return this.logisticsList.filter(item => item.status === this.statusFilter)
    }
  },

  onLoad() {
    this.loadStats()
    this.loadLogisticsList()
  },

  methods: {
    searchLogistics() {
      this.page = 1
      this.loadLogisticsList()
    },

    clearKeyword() {
      this.keyword = ''
      this.searchLogistics()
    },

    changeStatus(status) {
      this.statusFilter = status
      this.page = 1
      this.loadLogisticsList()
    },

    loadStats() {
      // 模拟数据
      this.stats = {
        total: 1256,
        inTransit: 423,
        delivered: 812,
        exception: 21
      }
    },

    loadLogisticsList() {
      // 模拟数据
      this.logisticsList = [
        {
          id: 1,
          orderNo: 'ORD202604210001',
          expressNo: 'SF1234567890',
          expressCompany: '顺丰速运',
          companyLogo: '/static/icons/sf.png',
          receiverName: '张三',
          receiverPhone: '138****5678',
          address: '广东省深圳市南山区科技园南路XX号',
          status: 'in_transit',
          lastUpdateTime: '2026-04-21 10:30',
          traces: [
            { desc: '快件已到达【深圳宝安机场中转场】', time: '2026-04-21 10:30' },
            { desc: '快件已从【广州白云机场航空部】发出，即将到达【深圳宝安机场中转场】', time: '2026-04-21 08:15' },
            { desc: '快件已从【广州白云航空枢纽】发出，揽件员：林XX', time: '2026-04-21 06:20' },
            { desc: '商家正在发货', time: '2026-04-20 18:00' }
          ]
        },
        {
          id: 2,
          orderNo: 'ORD202604210002',
          expressNo: 'YT9876543210',
          expressCompany: '圆通速递',
          companyLogo: '/static/icons/yt.png',
          receiverName: '李四',
          receiverPhone: '139****8765',
          address: '北京市朝阳区望京街道XX大厦',
          status: 'delivered',
          lastUpdateTime: '2026-04-20 15:42',
          traces: [
            { desc: '已签收，感谢使用顺丰快递', time: '2026-04-20 15:42' },
            { desc: '快件已放置在代收点', time: '2026-04-20 12:30' },
            { desc: '快件正在派送中', time: '2026-04-20 10:00' },
            { desc: '快件已到达【北京朝阳分部】', time: '2026-04-20 08:15' }
          ]
        },
        {
          id: 3,
          orderNo: 'ORD202604200003',
          expressNo: 'ZTO2345678901',
          expressCompany: '中通快递',
          companyLogo: '/static/icons/zto.png',
          receiverName: '王五',
          receiverPhone: '137****2345',
          address: '上海市浦东新区陆家嘴金融中心XX号',
          status: 'exception',
          lastUpdateTime: '2026-04-19 16:30',
          traces: [
            { desc: '快件派送异常，收件人电话无法接通', time: '2026-04-19 16:30' },
            { desc: '快件正在派送中', time: '2026-04-19 14:00' },
            { desc: '快件已到达【上海浦东分部】', time: '2026-04-19 10:00' }
          ]
        }
      ]
      this.total = 3
    },

    getStatusText(status) {
      const map = {
        'in_transit': '运输中',
        'delivered': '已签收',
        'exception': '异常'
      }
      return map[status] || status
    },

    toggleExpand(id) {
      const index = this.expandedIds.indexOf(id)
      if (index > -1) {
        this.expandedIds.splice(index, 1)
      } else {
        this.expandedIds.push(id)
      }
    },

    refreshTrace(item) {
      uni.showLoading({ title: '刷新中...' })
      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({ title: '物流信息已更新', icon: 'success' })
      }, 1000)
    },

    copyExpressNo(no) {
      uni.setClipboardData({
        data: no,
        success: () => {
          uni.showToast({ title: '单号已复制', icon: 'success' })
        }
      })
    },

    handleException(item) {
      this.exceptionForm = {
        orderId: item.id,
        reason: '',
        type: 'contact'
      }
      this.showExceptionModal = true
    },

    onExceptionTypeChange(e) {
      this.exceptionForm.type = e.detail.value
    },

    submitException() {
      if (!this.exceptionForm.reason) {
        uni.showToast({ title: '请输入异常原因', icon: 'none' })
        return
      }
      uni.showLoading({ title: '处理中...' })
      setTimeout(() => {
        uni.hideLoading()
        this.showExceptionModal = false
        uni.showToast({ title: '异常已处理', icon: 'success' })
        this.loadLogisticsList()
      }, 1000)
    },

    prevPage() {
      if (this.page > 1) {
        this.page--
        this.loadLogisticsList()
      }
    },

    nextPage() {
      if (this.page < this.totalPage) {
        this.page++
        this.loadLogisticsList()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.logistics-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 30rpx;
}

.header {
  background: #fff;
  padding: 30rpx;
  
  .page-title {
    font-size: 32rpx;
    font-weight: 600;
    margin-bottom: 20rpx;
  }
  
  .search-box {
    display: flex;
    align-items: center;
    background: #f5f5f5;
    border-radius: 40rpx;
    padding: 16rpx 24rpx;
    
    .search-input {
      flex: 1;
      margin-left: 12rpx;
      font-size: 28rpx;
    }
  }
}

.stats-section {
  padding: 30rpx;
  
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20rpx;
    
    .stats-item {
      background: #fff;
      border-radius: 12rpx;
      padding: 25rpx 15rpx;
      text-align: center;
      
      .stats-value {
        font-size: 36rpx;
        font-weight: 600;
        color: #2979ff;
      }
      
      .stats-label {
        font-size: 22rpx;
        color: #999;
        margin-top: 8rpx;
      }
    }
  }
}

.logistics-list {
  .list-header {
    display: flex;
    background: #fff;
    padding: 0 30rpx;
    border-bottom: 1rpx solid #f5f5f5;
    
    .tab-item {
      flex: 1;
      text-align: center;
      padding: 25rpx 0;
      font-size: 28rpx;
      color: #666;
      position: relative;
      
      &.active {
        color: #2979ff;
        font-weight: 600;
        
        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 50%;
          transform: translateX(-50%);
          width: 60rpx;
          height: 4rpx;
          background: #2979ff;
          border-radius: 2rpx;
        }
      }
    }
  }
}

.logistics-card {
  background: #fff;
  margin: 20rpx 30rpx;
  border-radius: 16rpx;
  overflow: hidden;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 25rpx 30rpx;
    border-bottom: 1rpx solid #f5f5f5;
    
    .order-info {
      .order-no {
        font-size: 26rpx;
        color: #333;
        display: block;
      }
      
      .express-no {
        font-size: 24rpx;
        color: #999;
        margin-top: 8rpx;
        display: block;
      }
    }
    
    .status-tag {
      padding: 6rpx 16rpx;
      border-radius: 20rpx;
      font-size: 22rpx;
      
      &.in_transit {
        background: #e6f7ff;
        color: #1890ff;
      }
      
      &.delivered {
        background: #f6ffed;
        color: #52c41a;
      }
      
      &.exception {
        background: #fff1f0;
        color: #ff4d4f;
      }
    }
  }
  
  .card-body {
    display: flex;
    justify-content: space-between;
    padding: 25rpx 30rpx;
    border-bottom: 1rpx solid #f5f5f5;
    
    .receiver-info {
      flex: 1;
      
      .info-row {
        display: flex;
        align-items: flex-start;
        margin-bottom: 12rpx;
        font-size: 26rpx;
        color: #333;
        
        u-icon {
          margin-right: 10rpx;
          margin-top: 4rpx;
        }
        
        .address {
          flex: 1;
          color: #666;
        }
      }
    }
    
    .express-info {
      text-align: right;
      
      .express-company {
        display: flex;
        align-items: center;
        font-size: 26rpx;
        color: #333;
        
        .company-logo {
          width: 40rpx;
          height: 40rpx;
          margin-right: 10rpx;
        }
      }
      
      .express-time {
        font-size: 22rpx;
        color: #999;
        margin-top: 8rpx;
      }
    }
  }
  
  .tracking-section {
    .tracking-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20rpx 30rpx;
      font-size: 26rpx;
      color: #2979ff;
    }
    
    .tracking-timeline {
      padding: 0 30rpx 25rpx;
      
      .timeline-item {
        display: flex;
        padding-left: 30rpx;
        position: relative;
        
        &::before {
          content: '';
          position: absolute;
          left: 8rpx;
          top: 30rpx;
          bottom: -20rpx;
          width: 2rpx;
          background: #e5e5e5;
        }
        
        &:last-child::before {
          display: none;
        }
        
        &.latest {
          .timeline-dot {
            background: #2979ff;
            border-color: #2979ff;
          }
          
          .trace-desc {
            color: #333;
            font-weight: 600;
          }
        }
        
        .timeline-dot {
          width: 18rpx;
          height: 18rpx;
          border-radius: 50%;
          background: #fff;
          border: 3rpx solid #ccc;
          position: absolute;
          left: 0;
          top: 8rpx;
        }
        
        .timeline-content {
          padding-bottom: 25rpx;
          
          .trace-desc {
            font-size: 26rpx;
            color: #666;
            line-height: 1.5;
          }
          
          .trace-time {
            font-size: 22rpx;
            color: #999;
            margin-top: 8rpx;
          }
        }
      }
    }
  }
  
  .card-footer {
    display: flex;
    justify-content: flex-end;
    gap: 30rpx;
    padding: 20rpx 30rpx;
    border-top: 1rpx solid #f5f5f5;
    
    .action-btn {
      display: flex;
      align-items: center;
      gap: 8rpx;
      font-size: 24rpx;
      color: #666;
      
      &:active {
        opacity: 0.7;
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
}

.exception-modal {
  width: 600rpx;
  padding: 40rpx;
  
  .modal-title {
    font-size: 32rpx;
    font-weight: 600;
    text-align: center;
    margin-bottom: 30rpx;
  }
  
  .modal-body {
    .form-item {
      margin-bottom: 30rpx;
      
      .label {
        font-size: 26rpx;
        color: #333;
        margin-bottom: 15rpx;
        display: block;
      }
      
      .textarea {
        width: 100%;
        height: 150rpx;
        border: 1rpx solid #e5e5e5;
        border-radius: 12rpx;
        padding: 20rpx;
        font-size: 28rpx;
        box-sizing: border-box;
      }
      
      .radio-item {
        display: flex;
        align-items: center;
        margin-bottom: 15rpx;
        
        text {
          margin-left: 10rpx;
          font-size: 26rpx;
        }
      }
    }
  }
  
  .modal-footer {
    display: flex;
    gap: 30rpx;
    
    .btn {
      flex: 1;
      text-align: center;
      padding: 20rpx 0;
      border-radius: 40rpx;
      font-size: 28rpx;
      
      &.cancel {
        background: #f5f5f5;
        color: #666;
      }
      
      &.confirm {
        background: #2979ff;
        color: #fff;
      }
    }
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 30rpx;
  padding: 30rpx;
  
  .page-info {
    font-size: 26rpx;
    color: #666;
  }
}
</style>
