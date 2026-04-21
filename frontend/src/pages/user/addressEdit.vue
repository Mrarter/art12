<template>
  <view class="address-edit-page">
    <view class="form-section card">
      <!-- 收货人 -->
      <view class="form-item">
        <view class="form-label">收货人</view>
        <input
          class="form-input"
          v-model="form.consignee"
          placeholder="请输入收货人姓名"
          maxlength="20"
        />
      </view>

      <!-- 手机号 -->
      <view class="form-item">
        <view class="form-label">手机号</view>
        <input
          class="form-input"
          v-model="form.phone"
          type="number"
          placeholder="请输入收货人手机号"
          maxlength="11"
        />
      </view>

      <!-- 所在地区 -->
      <view class="form-item">
        <view class="form-label">所在地区</view>
        <picker mode="region" :value="region" @change="onRegionChange">
          <view class="picker-value">
            {{ regionText || '请选择' }}
            <u-icon name="arrow-right" size="14" color="#999"></u-icon>
          </view>
        </picker>
      </view>

      <!-- 详细地址 -->
      <view class="form-item vertical">
        <view class="form-label">详细地址</view>
        <textarea
          class="form-textarea"
          v-model="form.detail"
          placeholder="请输入详细地址，如街道、门牌号等"
          maxlength="100"
        ></textarea>
      </view>

      <!-- 邮编 -->
      <view class="form-item">
        <view class="form-label">邮编</view>
        <input
          class="form-input"
          v-model="form.postCode"
          type="number"
          placeholder="选填"
          maxlength="6"
        />
      </view>
    </view>

    <!-- 默认地址设置 -->
    <view class="default-section card">
      <view class="default-row">
        <view class="default-info">
          <text class="default-label">设为默认地址</text>
          <text class="default-tip">下单时自动使用该地址</text>
        </view>
        <switch :checked="form.isDefault" @change="toggleDefault" color="#667eea"></switch>
      </view>
    </view>

    <!-- 地图定位 -->
    <view class="location-section card" @click="selectOnMap">
      <view class="location-icon">
        <u-icon name="map" size="24" color="#667eea"></u-icon>
      </view>
      <view class="location-content">
        <text class="location-title">在地图上选择</text>
        <text class="location-tip">可以通过地图快速定位</text>
      </view>
      <u-icon name="arrow-right" size="16" color="#ccc"></u-icon>
    </view>

    <!-- 保存按钮 -->
    <view class="save-section">
      <view
        class="save-btn"
        :class="{ disabled: !canSave }"
        @click="saveAddress"
      >
        保存地址
      </view>
      <view class="delete-btn" v-if="addressId" @click="deleteAddress">
        删除地址
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      addressId: null,
      region: [],
      regionText: '',
      form: {
        consignee: '',
        phone: '',
        province: '',
        city: '',
        district: '',
        detail: '',
        postCode: '',
        isDefault: false
      }
    }
  },

  computed: {
    canSave() {
      return (
        this.form.consignee &&
        this.form.phone &&
        this.region.length > 0 &&
        this.form.detail
      )
    }
  },

  onLoad(options) {
    if (options.id) {
      this.addressId = options.id
      this.loadAddress(options.id)
    }
  },

  methods: {
    onRegionChange(e) {
      const values = e.detail.value
      this.region = values
      this.regionText = values.join('')
      this.form.province = values[0]
      this.form.city = values[1]
      this.form.district = values[2]
    },

    toggleDefault(e) {
      this.form.isDefault = e.detail.value
    },

    loadAddress(id) {
      // 模拟加载地址数据
      console.log('加载地址:', id)
    },

    selectOnMap() {
      uni.chooseLocation({
        success: (res) => {
          console.log('选择位置:', res)
          uni.showToast({ title: '位置已选择', icon: 'success' })
        }
      })
    },

    saveAddress() {
      if (!this.canSave) {
        uni.showToast({ title: '请填写完整信息', icon: 'none' })
        return
      }

      if (!/^1[3-9]\d{9}$/.test(this.form.phone)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }

      uni.showLoading({ title: '保存中...' })
      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1000)
      }, 800)
    },

    deleteAddress() {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除该地址吗？',
        success: (res) => {
          if (res.confirm) {
            uni.showLoading({ title: '删除中...' })
            setTimeout(() => {
              uni.hideLoading()
              uni.showToast({ title: '删除成功', icon: 'success' })
              setTimeout(() => {
                uni.navigateBack()
              }, 1000)
            }, 500)
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.address-edit-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 20rpx;
  padding-bottom: 200rpx;
}

.form-section {
  padding: 0 30rpx;

  .form-item {
    display: flex;
    align-items: center;
    padding: 28rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    &.vertical {
      flex-direction: column;
      align-items: flex-start;
    }

    .form-label {
      width: 160rpx;
      font-size: 28rpx;
      color: #333;
      flex-shrink: 0;
    }

    .form-input {
      flex: 1;
      font-size: 28rpx;
      color: #333;
    }

    .picker-value {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 28rpx;
      color: #333;
    }

    .form-textarea {
      width: 100%;
      height: 120rpx;
      margin-top: 16rpx;
      padding: 20rpx;
      font-size: 28rpx;
      color: #333;
      background: #f5f5f5;
      border-radius: 8rpx;
      box-sizing: border-box;
    }
  }
}

.default-section {
  margin-top: 20rpx;
  padding: 30rpx;

  .default-row {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .default-info {
      .default-label {
        font-size: 28rpx;
        color: #333;
      }

      .default-tip {
        display: block;
        font-size: 24rpx;
        color: #999;
        margin-top: 4rpx;
      }
    }
  }
}

.location-section {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  padding: 30rpx;

  .location-icon {
    width: 80rpx;
    height: 80rpx;
    background: rgba(102, 126, 234, 0.1);
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .location-content {
    flex: 1;
    margin-left: 20rpx;

    .location-title {
      display: block;
      font-size: 28rpx;
      color: #333;
    }

    .location-tip {
      display: block;
      font-size: 24rpx;
      color: #999;
      margin-top: 4rpx;
    }
  }
}

.save-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;

  .save-btn {
    height: 88rpx;
    line-height: 88rpx;
    text-align: center;
    font-size: 32rpx;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 44rpx;

    &.disabled {
      background: #ccc