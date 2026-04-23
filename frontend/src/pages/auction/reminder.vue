<template>
  <view class="reminder-page">
    <!-- 顶部提示 -->
    <view class="tip-section">
      
      <text class="tip-text">开启竞拍提醒，在拍卖开始前及时收到通知</text>
    </view>

    <!-- 已关注的专场 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">我的竞拍提醒</text>
        <text class="reminder-count">{{ reminders.length }}个提醒</text>
      </view>

      <view class="reminder-list">
        <view class="reminder-card" v-for="item in reminders" :key="item.id">
          <view class="card-header">
            <image class="cover-image" :src="item.coverImage" mode="aspectFill"></image>
            <view class="card-info">
              <text class="session-name">{{ item.sessionName }}</text>
              <text class="lot-name" v-if="item.lotName">拍品: {{ item.lotName }}</text>
              <view class="session-time">
                
                <text>{{ formatTime(item.startTime) }}</text>
              </view>
            </view>
            <view class="status-toggle">
              <switch 
                :checked="item.enabled" 
                @change="toggleReminder(item)"
                color="#2979ff"
              />
            </view>
          </view>

          <view class="card-body">
            <view class="notify-type">
              <view class="type-item" :class="{ active: item.notifyBefore }">
                
                <text>开始前提醒</text>
              </view>
              <view class="type-item" :class="{ active: item.notifyOutbid }">
                
                <text>被超越时</text>
              </view>
              <view class="type-item" :class="{ active: item.notifyEnd }">
                
                <text>即将结束</text>
              </view>
            </view>

            <view class="card-actions">
              <view class="action-btn" @click="editReminder(item)">
                
                <text>编辑</text>
              </view>
              <view class="action-btn delete" @click="deleteReminder(item)">
                
                <text>删除</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <view class="empty-state" v-if="reminders.length === 0">
          
          <text class="empty-text">暂无竞拍提醒</text>
          <text class="empty-sub">去拍卖专场设置提醒吧</text>
          <view class="empty-btn" @click="goAuctionList">浏览专场</view>
        </view>
      </view>
    </view>

    <!-- 设置提醒弹窗 -->
    <!-- 弹窗开始 -->
      <view class="edit-modal">
        <view class="modal-title">{{ isEdit ? '编辑提醒' : '设置提醒' }}</view>

        <view class="modal-body">
          <view class="form-item">
            <text class="label">提醒时间</text>
            <picker mode="selector" :range="remindTimeOptions" range-key="label" @change="onRemindTimeChange">
              <view class="picker-value">
                {{ currentEdit.remindMinutes }}分钟前
                
              </view>
            </picker>
          </view>

          <view class="form-item">
            <text class="label">通知方式</text>
            <view class="checkbox-group">
              <label class="checkbox-item">
                <checkbox :checked="currentEdit.notifyPush" @change="currentEdit.notifyPush = !currentEdit.notifyPush" />
                <text>应用内推送</text>
              </label>
              <label class="checkbox-item">
                <checkbox :checked="currentEdit.notifySms" @change="currentEdit.notifySms = !currentEdit.notifySms" />
                <text>短信通知</text>
              </label>
            </view>
          </view>

          <view class="form-item">
            <text class="label">提醒类型</text>
            <view class="checkbox-group">
              <label class="checkbox-item">
                <checkbox :checked="currentEdit.notifyBefore" @change="currentEdit.notifyBefore = !currentEdit.notifyBefore" />
                <text>开始前提醒</text>
              </label>
              <label class="checkbox-item">
                <checkbox :checked="currentEdit.notifyOutbid" @change="currentEdit.notifyOutbid = !currentEdit.notifyOutbid" />
                <text>被超越时提醒</text>
              </label>
              <label class="checkbox-item">
                <checkbox :checked="currentEdit.notifyEnd" @change="currentEdit.notifyEnd = !currentEdit.notifyEnd" />
                <text>即将结束提醒</text>
              </label>
            </view>
          </view>
        </view>

        <view class="modal-footer">
          <view class="btn cancel" @click="showEditModal = false">取消</view>
          <view class="btn confirm" @click="saveReminder">保存</view>
        </view>
      </view>
<!-- 弹窗结束 -->

    <!-- 删除确认弹窗 -->
    <u-modal 
      v-model="showDeleteModal" 
      title="确认删除" 
      content="确定要删除这个竞拍提醒吗？"
      :show-cancel-button="true"
      @confirm="confirmDelete"
    ></u-modal>
  </view>
</template>

<script>
import { getAuctionReminders, setReminder, deleteReminder as deleteReminderApi, updateReminder } from '@/api/auction.js'

export default {
  data() {
    return {
      reminders: [],
      showEditModal: false,
      showDeleteModal: false,
      isEdit: false,
      currentEdit: {
        id: null,
        sessionId: null,
        lotId: null,
        remindMinutes: 15,
        notifyPush: true,
        notifySms: false,
        notifyBefore: true,
        notifyOutbid: true,
        notifyEnd: false
      },
      deleteTarget: null,
      remindTimeOptions: [
        { label: '5分钟前', value: 5 },
        { label: '10分钟前', value: 10 },
        { label: '15分钟前', value: 15 },
        { label: '30分钟前', value: 30 },
        { label: '1小时前', value: 60 },
        { label: '2小时前', value: 120 }
      ]
    }
  },

  onLoad(options) {
    if (options.sessionId) {
      this.currentEdit.sessionId = Number(options.sessionId)
      this.currentEdit.lotId = options.lotId ? Number(options.lotId) : null
    }
    this.loadReminders()
  },

  onShow() {
    this.loadReminders()
  },

  methods: {
    async loadReminders() {
      try {
        const res = await getAuctionReminders()
        if (res.code === 200) {
          this.reminders = res.data || []
        }
      } catch (e) {
        // 模拟数据
        this.reminders = [
          {
            id: 1,
            sessionId: 1,
            lotId: null,
            sessionName: '2026春季当代艺术专场',
            lotName: '',
            coverImage: '/static/icons/auction-1.png',
            startTime: '2026-04-25 19:00:00',
            enabled: true,
            remindMinutes: 15,
            notifyPush: true,
            notifySms: false,
            notifyBefore: true,
            notifyOutbid: true,
            notifyEnd: false
          },
          {
            id: 2,
            sessionId: 2,
            lotId: 101,
            sessionName: '名家书画专场',
            lotName: '张大千《山水长卷》',
            coverImage: '/static/icons/auction-2.png',
            startTime: '2026-04-26 20:00:00',
            enabled: true,
            remindMinutes: 30,
            notifyPush: true,
            notifySms: true,
            notifyBefore: true,
            notifyOutbid: false,
            notifyEnd: true
          }
        ]
      }
    },

    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const month = date.getMonth() + 1
      const day = date.getDate()
      const hour = date.getHours()
      const minute = date.getMinutes()
      return `${month}月${day}日 ${hour}:${String(minute).padStart(2, '0')}`
    },

    async toggleReminder(item) {
      item.enabled = !item.enabled
      try {
        await updateReminder(item.id, { enabled: item.enabled })
        uni.showToast({ 
          title: item.enabled ? '提醒已开启' : '提醒已关闭', 
          icon: 'success' 
        })
      } catch (e) {
        // 回滚
        item.enabled = !item.enabled
        uni.showToast({ title: '设置失败', icon: 'none' })
      }
    },

    editReminder(item) {
      this.isEdit = true
      this.currentEdit = { ...item }
      this.showEditModal = true
    },

    onRemindTimeChange(e) {
      this.currentEdit.remindMinutes = this.remindTimeOptions[e.detail.value].value
    },

    async saveReminder() {
      try {
        if (this.isEdit) {
          await updateReminder(this.currentEdit.id, this.currentEdit)
        } else {
          await setReminder(this.currentEdit)
        }
        this.showEditModal = false
        uni.showToast({ title: '保存成功', icon: 'success' })
        this.loadReminders()
      } catch (e) {
        // 模拟成功
        this.showEditModal = false
        uni.showToast({ title: '保存成功', icon: 'success' })
        if (!this.isEdit) {
          this.reminders.push({
            id: Date.now(),
            ...this.currentEdit,
            sessionName: '新专场',
            coverImage: '/static/icons/auction-default.png',
            startTime: '2026-04-28 20:00:00',
            enabled: true
          })
        }
      }
    },

    deleteReminder(item) {
      this.deleteTarget = item
      this.showDeleteModal = true
    },

    async confirmDelete() {
      try {
        await deleteReminderApi(this.deleteTarget.id)
        this.reminders = this.reminders.filter(r => r.id !== this.deleteTarget.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (e) {
        // 模拟成功
        this.reminders = this.reminders.filter(r => r.id !== this.deleteTarget.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
    },

    goAuctionList() {
      uni.switchTab({
        url: '/pages/auction/index'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.reminder-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 30rpx;
}

.tip-section {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx 30rpx;
  background: #e6f7ff;
  
  .tip-text {
    font-size: 26rpx;
    color: #1890ff;
  }
}

.section {
  padding: 30rpx;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
    
    .section-title {
      font-size: 32rpx;
      font-weight: 600;
    }
    
    .reminder-count {
      font-size: 24rpx;
      color: #999;
    }
  }
}

.reminder-list {
  .reminder-card {
    background: #fff;
    border-radius: 16rpx;
    margin-bottom: 20rpx;
    overflow: hidden;
    
    .card-header {
      display: flex;
      padding: 24rpx;
      align-items: center;
      
      .cover-image {
        width: 120rpx;
        height: 120rpx;
        border-radius: 12rpx;
        background: #f0f0f0;
        margin-right: 20rpx;
      }
      
      .card-info {
        flex: 1;
        
        .session-name {
          font-size: 28rpx;
          font-weight: 600;
          color: #333;
          display: block;
        }
        
        .lot-name {
          font-size: 24rpx;
          color: #666;
          margin-top: 6rpx;
          display: block;
        }
        
        .session-time {
          display: flex;
          align-items: center;
          gap: 6rpx;
          font-size: 22rpx;
          color: #999;
          margin-top: 10rpx;
        }
      }
    }
    
    .card-body {
      padding: 0 24rpx 24rpx;
      
      .notify-type {
        display: flex;
        gap: 20rpx;
        padding-bottom: 20rpx;
        border-bottom: 1rpx solid #f5f5f5;
        
        .type-item {
          display: flex;
          align-items: center;
          gap: 6rpx;
          font-size: 24rpx;
          color: #ccc;
          padding: 8rpx 16rpx;
          background: #f5f5f5;
          border-radius: 20rpx;
          
          &.active {
            color: #2979ff;
            background: #e6f7ff;
          }
        }
      }
      
      .card-actions {
        display: flex;
        justify-content: flex-end;
        gap: 30rpx;
        padding-top: 20rpx;
        
        .action-btn {
          display: flex;
          align-items: center;
          gap: 6rpx;
          font-size: 24rpx;
          color: #666;
          
          &.delete {
            color: #ff4d4f;
          }
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
  
  .empty-sub {
    font-size: 24rpx;
    color: #ccc;
    margin-top: 10rpx;
  }
  
  .empty-btn {
    margin-top: 30rpx;
    padding: 16rpx 50rpx;
    background: #2979ff;
    color: #fff;
    border-radius: 40rpx;
    font-size: 28rpx;
  }
}

.edit-modal {
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
      
      .picker-value {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 20rpx 24rpx;
        background: #f5f5f5;
        border-radius: 12rpx;
        font-size: 28rpx;
      }
      
      .checkbox-group {
        .checkbox-item {
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
  }
  
  .modal-footer {
    display: flex;
    gap: 30rpx;
    margin-top: 30rpx;
    
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
</style>
