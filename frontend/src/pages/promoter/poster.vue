<template>
  <view class="poster-page">
    <!-- 海报展示区域 -->
    <view class="poster-preview">
      <canvas canvas-id="posterCanvas" id="posterCanvas" class="poster-canvas"></canvas>
    </view>

    <!-- 操作按钮 -->
    <view class="action-bar">
      <view class="action-btn save" @click="savePoster">
        
        <text>保存图片</text>
      </view>
      <view class="action-btn share" @click="sharePoster">
        <text>↗</text>
        <text>分享海报</text>
      </view>
    </view>

    <!-- 海报样式选择 -->
    <view class="style-section">
      <view class="section-title">选择海报风格</view>
      <scroll-view class="style-list" scroll-x>
        <view 
          class="style-item" 
          :class="{ active: currentStyle === 'default' }"
          @click="changeStyle('default')"
        >
          <image class="style-thumb" src="/static/poster/style-default.png" mode="aspectFill"></image>
          <text class="style-name">经典风格</text>
        </view>
        <view 
          class="style-item" 
          :class="{ active: currentStyle === 'elegant' }"
          @click="changeStyle('elegant')"
        >
          <image class="style-thumb" src="/static/poster/style-elegant.png" mode="aspectFill"></image>
          <text class="style-name">雅致风格</text>
        </view>
        <view 
          class="style-item" 
          :class="{ active: currentStyle === 'modern' }"
          @click="changeStyle('modern')"
        >
          <image class="style-thumb" src="/static/poster/style-modern.png" mode="aspectFill"></image>
          <text class="style-name">现代风格</text>
        </view>
        <view 
          class="style-item" 
          :class="{ active: currentStyle === 'simple' }"
          @click="changeStyle('simple')"
        >
          <image class="style-thumb" src="/static/poster/style-simple.png" mode="aspectFill"></image>
          <text class="style-name">简约风格</text>
        </view>
      </scroll-view>
    </view>

    <!-- 邀请码展示 -->
    <view class="invite-code-section">
      <view class="section-title">我的邀请码</view>
      <view class="code-card">
        <view class="code-display">
          <text class="code-value">{{ inviteCode }}</text>
          <view class="copy-btn" @click="copyCode">
            
            <text>复制</text>
          </view>
        </view>
        <view class="code-tip">分享给好友，好友注册时填入此码即可成为您的徒弟</view>
      </view>
    </view>

    <!-- 二维码区域 -->
    <view class="qrcode-section">
      <view class="section-title">扫码注册</view>
      <view class="qrcode-card">
        <image class="qrcode-image" :src="qrcodeUrl" mode="aspectFit"></image>
        <text class="qrcode-tip">微信扫码立即注册</text>
      </view>
    </view>

    <!-- 分享提示 -->
    <view class="share-tips" v-if="showShareTips">
      <view class="tips-content">
        <text>点击右上角 ··· 分享给好友</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getQrCode } from '@/api/promoter.js'

export default {
  data() {
    return {
      currentStyle: 'default',
      inviteCode: 'ABC123456',
      qrcodeUrl: '',
      showShareTips: false,
      posterData: {
        userName: '张三',
        userAvatar: '/static/avatar/default.jpg',
        qrCodeUrl: '',
        appName: '拾艺局',
        slogan: '发现艺术，分享价值',
        inviteCode: 'ABC123456',
        bgImage: '/static/poster/bg-default.jpg'
      }
    }
  },

  onLoad() {
    this.loadQrCode()
    this.generatePoster()
  },

  methods: {
    async loadQrCode() {
      try {
        const res = await getQrCode({ inviteCode: this.inviteCode })
        this.qrcodeUrl = res.data.qrCodeUrl
        this.posterData.qrCodeUrl = res.data.qrCodeUrl
      } catch (e) {
        // 使用模拟数据
        this.qrcodeUrl = '/static/qrcode-demo.png'
        this.posterData.qrCodeUrl = '/static/qrcode-demo.png'
      }
    },

    changeStyle(style) {
      this.currentStyle = style
      const bgMap = {
        'default': '/static/poster/bg-default.jpg',
        'elegant': '/static/poster/bg-elegant.jpg',
        'modern': '/static/poster/bg-modern.jpg',
        'simple': '/static/poster/bg-simple.jpg'
      }
      this.posterData.bgImage = bgMap[style]
      this.generatePoster()
    },

    generatePoster() {
      const ctx = uni.createCanvasContext('posterCanvas')
      const width = 600
      const height = 900

      // 绘制背景
      ctx.drawImage(this.posterData.bgImage, 0, 0, width, height)

      // 绘制标题
      ctx.setFillStyle('#FFFFFF')
      ctx.setFontSize(48)
      ctx.setTextAlign('center')
      ctx.fillText(this.posterData.appName, width / 2, 120)

      // 绘制标语
      ctx.setFontSize(24)
      ctx.setFillStyle('rgba(255,255,255,0.8)')
      ctx.fillText(this.posterData.slogan, width / 2, 160)

      // 绘制用户头像
      ctx.save()
      ctx.beginPath()
      ctx.arc(width / 2, 280, 60, 0, 2 * Math.PI)
      ctx.clip()
      ctx.drawImage(this.posterData.userAvatar, width / 2 - 60, 220, 120, 120)
      ctx.restore()

      // 绘制用户昵称
      ctx.setFillStyle('#FFFFFF')
      ctx.setFontSize(32)
      ctx.fillText(this.posterData.userName, width / 2, 380)

      // 绘制邀请码标签
      ctx.setFillStyle('rgba(255,255,255,0.2)')
      ctx.fillRect(width / 2 - 100, 420, 200, 50)
      ctx.setFillStyle('#FFFFFF')
      ctx.setFontSize(24)
      ctx.fillText('邀请码', width / 2, 452)

      // 绘制邀请码
      ctx.setFontSize(36)
      ctx.setFillStyle('#FFD700')
      ctx.fillText(this.posterData.inviteCode, width / 2, 520)

      // 绘制二维码
      ctx.drawImage(this.posterData.qrCodeUrl, width / 2 - 100, 560, 200, 200)

      // 绘制提示文字
      ctx.setFillStyle('rgba(255,255,255,0.7)')
      ctx.setFontSize(20)
      ctx.fillText('长按识别二维码注册', width / 2, 800)
      ctx.fillText('成为我的艺人推荐官', width / 2, 830)

      ctx.draw()
    },

    savePoster() {
      uni.showLoading({ title: '保存中...' })
      
      uni.canvasToTempFilePath({
        canvasId: 'posterCanvas',
        success: (res) => {
          uni.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: () => {
              uni.hideLoading()
              uni.showToast({ title: '保存成功', icon: 'success' })
            },
            fail: (err) => {
              uni.hideLoading()
              if (err.errMsg.includes('auth deny')) {
                uni.showModal({
                  title: '提示',
                  content: '需要您授权保存图片到相册',
                  success: (modalRes) => {
                    if (modalRes.confirm) {
                      uni.openSetting()
                    }
                  }
                })
              } else {
                uni.showToast({ title: '保存失败', icon: 'none' })
              }
            }
          })
        },
        fail: () => {
          uni.hideLoading()
          uni.showToast({ title: '生成失败', icon: 'none' })
        }
      })
    },

    sharePoster() {
      this.showShareTips = true
      setTimeout(() => {
        this.showShareTips = false
      }, 3000)
    },

    copyCode() {
      uni.setClipboardData({
        data: this.inviteCode,
        success: () => {
          uni.showToast({ title: '邀请码已复制', icon: 'success' })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.poster-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  padding-bottom: 40rpx;
}

.poster-preview {
  display: flex;
  justify-content: center;
  padding: 40rpx 0;
  
  .poster-canvas {
    width: 600rpx;
    height: 900rpx;
    border-radius: 16rpx;
    box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.3);
  }
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: 40rpx;
  padding: 0 60rpx;
  margin-bottom: 40rpx;
  
  .action-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12rpx;
    padding: 24rpx 0;
    border-radius: 50rpx;
    font-size: 28rpx;
    
    &.save {
      background: rgba(255, 255, 255, 0.15);
      color: #fff;
      border: 2rpx solid rgba(255, 255, 255, 0.3);
    }
    
    &.share {
      background: linear-gradient(135deg, #FFD700, #FFA500);
      color: #333;
    }
  }
}

.style-section,
.invite-code-section,
.qrcode-section {
  padding: 0 30rpx;
  margin-bottom: 30rpx;
  
  .section-title {
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.7);
    margin-bottom: 20rpx;
  }
}

.style-list {
  display: flex;
  white-space: nowrap;
  
  .style-item {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    margin-right: 20rpx;
    opacity: 0.6;
    transition: all 0.3s;
    
    &.active {
      opacity: 1;
      transform: scale(1.1);
    }
    
    .style-thumb {
      width: 120rpx;
      height: 180rpx;
      border-radius: 12rpx;
      border: 4rpx solid transparent;
    }
    
    &.active .style-thumb {
      border-color: #FFD700;
    }
    
    .style-name {
      font-size: 22rpx;
      color: #fff;
      margin-top: 10rpx;
    }
  }
}

.code-card {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16rpx;
  padding: 30rpx;
  
  .code-display {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 20rpx;
    
    .code-value {
      font-size: 40rpx;
      font-weight: 600;
      color: #FFD700;
      letter-spacing: 4rpx;
    }
    
    .copy-btn {
      display: flex;
      align-items: center;
      gap: 6rpx;
      padding: 8rpx 16rpx;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 20rpx;
      font-size: 22rpx;
      color: #fff;
    }
  }
  
  .code-tip {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.5);
    text-align: center;
    margin-top: 15rpx;
  }
}

.qrcode-card {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16rpx;
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .qrcode-image {
    width: 240rpx;
    height: 240rpx;
    background: #fff;
    border-radius: 12rpx;
    padding: 10rpx;
  }
  
  .qrcode-tip {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.7);
    margin-top: 20rpx;
  }
}

.share-tips {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding-top: 120rpx;
  padding-right: 40rpx;
  
  .tips-content {
    background: #fff;
    padding: 20rpx 30rpx;
    border-radius: 12rpx;
    font-size: 28rpx;
    color: #333;
    animation: slideIn 0.3s ease;
    
    &::after {
      content: '';
      position: absolute;
      top: 80rpx;
      right: 60rpx;
      border: 15rpx solid transparent;
      border-top-color: #fff;
    }
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(50rpx);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
