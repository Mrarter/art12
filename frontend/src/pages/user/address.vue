<template>
  <view class="address-page">
    <!-- 地址列表 -->
    <view class="address-list" v-if="addresses.length > 0">
      <view 
        class="address-item" 
        v-for="item in addresses" 
        :key="item.id"
        @click="selectAddress(item)"
      >
        <view class="item-info">
          <view class="info-header">
            <text class="name">{{ item.consignee || item.receiverName }}</text>
            <text class="phone">{{ item.phone }}</text>
            <view class="default-tag" v-if="item.isDefault">默认</view>
          </view>
          <text class="address-text">{{ item.province }}{{ item.city }}{{ item.district }}{{ item.address || item.detailAddress }}</text>
        </view>
        <view class="item-actions">
          <view class="action-btn edit" @click.stop="editAddress(item)">
            <u-icon name="edit-pen" size="18" color="#667eea"></u-icon>
          </view>
          <view class="action-btn delete" @click.stop="deleteAddress(item)">
            <u-icon name="trash" size="18" color="#e74c3c"></u-icon>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <image class="empty-icon" src="/static/icons/empty-address.png" mode="aspectFit"></image>
      <text class="empty-text">暂无收货地址</text>
      <text class="empty-hint">添加收货地址，方便购物</text>
    </view>

    <!-- 添加按钮 -->
    <view class="add-section">
      <view class="add-btn" @click="addAddress">
        <u-icon name="plus" size="20" color="#667eea"></u-icon>
        <text>添加新地址</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getAddressList, deleteAddress as deleteAddressApi } from '@/api/user.js'

export default {
  data() {
    return {
      addresses: [],
      selectMode: false
    }
  },

  onLoad(options) {
    // 是否为选择模式
    if (options.select === 'true') {
      this.selectMode = true
      uni.setNavigationBarTitle({ title: '选择收货地址' })
    }
  },

  onShow() {
    this.loadAddresses()
  },

  methods: {
    async loadAddresses() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getAddressList()
        this.addresses = res || []
        uni.hideLoading()
      } catch (e) {
        uni.hideLoading()
        console.error('获取地址列表失败', e)
        // 使用模拟数据
        this.addresses = [
          {
            id: 1,
            consignee: '张三',
            receiverName: '张三',
            phone: '13800138000',
            province: '北京市',
            city: '北京市',
            district: '朝阳区',
            address: '建国路88号',
            detailAddress: '建国路88号',
            isDefault: true
          },
          {
            id: 2,
            consignee: '李四',
            receiverName: '李四',
            phone: '13900139000',
            province: '上海市',
            city: '上海市',
            district: '黄浦区',
            address: '南京东路100号',
            detailAddress: '南京东路100号',
            isDefault: false
          }
        ]
      }
    },

    selectAddress(item) {
      if (!this.selectMode) return
      
      // 通过事件总线或页面通信返回选中的地址
      const pages = getCurrentPages()
      const prevPage = pages[pages.length - 2]
      
      if (prevPage && prevPage.selectAddress) {
        prevPage.selectAddress(item)
      } else if (prevPage && prevPage.$vm && prevPage.$vm.selectedAddress) {
        prevPage.$vm.selectedAddress = item
      }
      
      uni.navigateBack()
    },

    addAddress() {
      uni.navigateTo({ url: '/pages/user/addressEdit' })
    },

    editAddress(item) {
      uni.navigateTo({ 
        url: `/pages/user/addressEdit?id=${item.id}`,
        events: {
          refresh: () => {
            this.loadAddresses()
          }
        }
      })
    },

    deleteAddress(item) {
      uni.showModal({
        title: '提示',
        content: '确定要删除该地址吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteAddressApi(item.id)
              uni.showToast({ title: '删除成功', icon: 'success' })
              this.loadAddresses()
            } catch (e) {
              uni.showToast({ title: '删除失败', icon: 'none' })
            }
          }
        }
      })
    },

    setDefault(item) {
      if (item.isDefault) return
      
      // 调用设置默认地址接口
      uni.showToast({ title: '设置成功', icon: 'success' })
      this.loadAddresses()
    }
  }
}
</script>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.address-list {
  padding: 20rpx;
}

.address-item {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.item-info {
  flex: 1;
  
  .info-header {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;
    
    .name {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
      margin-right: 16rpx;
    }
    
    .phone {
      font-size: 28rpx;
      color: #666;
    }
    
    .default-tag {
      margin-left: 16rpx;
      font-size: 20rpx;
      color: #667eea;
      background: rgba(102, 126, 234, 0.1);
      padding: 4rpx 12rpx;
      border-radius: 8rpx;
    }
  }
  
  .address-text {
    font-size: 26rpx;
    color: #999;
    line-height: 1.4;
  }
}

.item-actions {
  display: flex;
  gap: 20rpx;
  
  .action-btn {
    padding: 12rpx;
    
    &.edit {
      background: rgba(102, 126, 234, 0.1);
      border-radius: 8rpx;
    }
    
    &.delete {
      background: rgba(231, 76, 60, 0.1);
      border-radius: 8rpx;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 16rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #ccc;
}

.add-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 44rpx;
  
  text {
    margin-left: 12rpx;
    font-size: 30rpx;
    color: #fff;
    font-weight: 500;
  }
}
</style>