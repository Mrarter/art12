<template>
  <view class="notice-detail-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <text class="icon">‹</text>
      </view>
      <text class="nav-title">{{ type === 'activity' ? '活动详情' : '公告详情' }}</text>
      <view class="share-btn" @click="shareNotice">
        <text class="icon">↗</text>
      </view>
    </view>

    <!-- 公告内容 -->
    <scroll-view class="content-scroll" scroll-y>
      <!-- 封面图 -->
      <image 
        class="cover-image" 
        v-if="detail.cover" 
        :src="detail.cover" 
        mode="widthFix"
      ></image>

      <!-- 文章内容 -->
      <view class="article-content">
        <!-- 标签和时间 -->
        <view class="article-header">
          <view class="article-tag" :class="detail.type">{{ getTypeName(detail.type) }}</view>
          <text class="article-time">{{ detail.publishTime }}</text>
        </view>

        <!-- 标题 -->
        <text class="article-title">{{ detail.title }}</text>

        <!-- 作者信息 -->
        <view class="author-info">
          <image class="author-avatar" :src="detail.authorAvatar || '/static/icons/avatar-default.png'" mode="aspectFill"></image>
          <view class="author-detail">
            <text class="author-name">{{ detail.authorName || '拾艺局官方' }}</text>
            <text class="author-title">{{ type === 'activity' ? '活动运营' : '平台公告' }}</text>
          </view>
        </view>

        <!-- 正文 -->
        <view class="article-body">
          <rich-text :nodes="detail.content"></rich-text>
        </view>

        <!-- 活动相关信息 -->
        <view class="activity-info" v-if="type === 'activity' && detail.activityInfo">
          <view class="info-title">活动信息</view>
          <view class="info-item">
            <text class="info-label">活动时间</text>
            <text class="info-value">{{ detail.activityInfo.startTime }} - {{ detail.activityInfo.endTime }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">活动地点</text>
            <text class="info-value">{{ detail.activityInfo.location }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">参与方式</text>
            <text class="info-value">{{ detail.activityInfo.method }}</text>
          </view>
        </view>

        <!-- 参与按钮 -->
        <view class="action-section" v-if="type === 'activity'">
          <button class="participate-btn" @click="participate">立即参与</button>
        </view>

        <!-- 底部信息 -->
        <view class="footer-info">
          <text>阅读 {{ detail.viewCount }}</text>
          <text>点赞 {{ detail.likeCount }}</text>
        </view>

        <!-- 相关推荐 -->
        <view class="related-section" v-if="relatedList.length > 0">
          <view class="section-title">相关推荐</view>
          <view 
            class="related-item"
            v-for="item in relatedList"
            :key="item.id"
            @click="goRelated(item.id)"
          >
            <image class="related-cover" :src="item.cover || '/static/icons/notice-cover1.png'" mode="aspectFill"></image>
            <view class="related-info">
              <text class="related-title">{{ item.title }}</text>
              <text class="related-time">{{ formatTime(item.publishTime) }}</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="action-item" @click="toggleLike">
        <text class="icon" :class="{ liked: detail.isLiked }">{{ detail.isLiked ? '♥' : '♡' }}</text>
        <text>{{ detail.likeCount || 0 }}</text>
      </view>
      <view class="action-item" @click="goComment">
        <text class="icon">💬</text>
        <text>{{ detail.commentCount || 0 }}</text>
      </view>
      <view class="action-item" @click="shareNotice">
        <text class="icon">↗</text>
        <text>分享</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      id: null,
      type: 'announce',
      detail: {},
      relatedList: []
    }
  },

  onLoad(options) {
    if (options.id) {
      this.id = options.id
      this.type = options.type || 'announce'
      this.loadDetail()
      this.loadRelated()
    }
  },

  onShareAppMessage() {
    return {
      title: this.detail.title,
      path: `/pages/notice/detail?id=${this.id}&type=${this.type}`
    }
  },

  methods: {
    loadDetail() {
      // 模拟详情数据
      this.detail = {
        id: this.id,
        type: this.type === 'activity' ? 'activity' : 'normal',
        title: this.type === 'activity' 
          ? '【五一特惠】艺术品专场拍卖会' 
          : '关于平台艺术品交易规则调整的通知',
        content: `
          <p>亲爱的用户：</p>
          <p>为了更好地保障用户权益，提升平台服务质量，我们对艺术品交易规则进行了优化调整，现将主要变更内容通知如下：</p>
          
          <h3>一、售后服务升级</h3>
          <p>1. 退货期限由原来的7天延长至15天</p>
          <p>2. 新增「鉴赏期」服务，签收后7天内可无理由退货</p>
          <p>3. 运费险覆盖范围扩大至全场艺术品</p>
          
          <h3>二、纠纷处理优化</h3>
          <p>1. 引入第三方专业鉴定机构</p>
          <p>2. 纠纷响应时间缩短至24小时内</p>
          <p>3. 新增「先行赔付」机制</p>
          
          <h3>三、交易安全保障</h3>
          <p>1. 大额交易引入人脸识别验证</p>
          <p>2. 新增交易冷静期功能</p>
          <p>3. 资金托管范围扩大</p>
          
          <p>以上规则调整将于2026年5月1日起正式生效。感谢您的理解与支持！</p>
          <p>如有疑问，请联系客服热线：400-888-8888</p>
        `,
        publishTime: '2026-04-21 10:00:00',
        viewCount: 2568,
        likeCount: 326,
        commentCount: 58,
        isLiked: false,
        cover: '/static/icons/notice-cover1.png',
        authorName: '拾艺局官方',
        authorAvatar: '/static/icons/avatar-default.png',
        activityInfo: this.type === 'activity' ? {
          startTime: '2026-05-01 00:00:00',
          endTime: '2026-05-05 23:59:59',
          location: '拾艺局App线上专场',
          method: '线上竞拍，通过App参与'
        } : null
      }
    },

    loadRelated() {
      this.relatedList = [
        { id: 1, title: '艺术家入驻审核流程优化公告', publishTime: '2026-04-20 15:30:00' },
        { id: 2, title: '新用户专享福利活动', publishTime: '2026-04-20 10:00:00' },
        { id: 3, title: '关于规范艺术品描述的公告', publishTime: '2026-04-17 11:00:00' }
      ]
    },

    goBack() {
      uni.navigateBack()
    },

    getTypeName(type) {
      const names = {
        important: '重要',
        normal: '公告',
        system: '系统',
        activity: '活动'
      }
      return names[type] || '公告'
    },

    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return `${date.getMonth() + 1}/${date.getDate()}`
    },

    toggleLike() {
      this.detail.isLiked = !this.detail.isLiked
      this.detail.likeCount += this.detail.isLiked ? 1 : -1
      uni.showToast({
        title: this.detail.isLiked ? '点赞成功' : '取消点赞',
        icon: 'success'
      })
    },

    goComment() {
      uni.showToast({ title: '评论功能开发中', icon: 'none' })
    },

    shareNotice() {
      uni.showShareMenu({
        withShareTicket: true
      })
    },

    participate() {
      uni.showToast({ title: '活动即将开始', icon: 'success' })
    },

    goRelated(id) {
      uni.redirectTo({
        url: `/pages/notice/detail?id=${id}&type=${this.type}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.notice-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 100rpx;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 88rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
  z-index: 100;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);

  .back-btn,
  .share-btn {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    .icon {
      font-size: 40rpx;
      color: #333;
    }
  }

  .nav-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }
}

.content-scroll {
  padding-top: 88rpx;
  padding-bottom: 120rpx;
}

.cover-image {
  width: 100%;
  display: block;
}

.article-content {
  background: #fff;
  padding: 30rpx;
}

.article-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;

  .article-tag {
    padding: 6rpx 16rpx;
    border-radius: 6rpx;
    font-size: 22rpx;
    color: #fff;
    margin-right: 16rpx;

    &.activity {
      background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
    }

    &.normal {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
  }

  .article-time {
    font-size: 24rpx;
    color: #999;
  }
}

.article-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #333;
  line-height: 1.4;
  display: block;
  margin-bottom: 30rpx;
}

.author-info {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-top: 1rpx solid #f0f0f0;
  border-bottom: 1rpx solid #f0f0f0;
  margin-bottom: 30rpx;

  .author-avatar {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    margin-right: 16rpx;
    background: #f0f0f0;
  }

  .author-detail {
    .author-name {
      font-size: 28rpx;
      color: #333;
      font-weight: 500;
      display: block;
      margin-bottom: 4rpx;
    }

    .author-title {
      font-size: 24rpx;
      color: #999;
    }
  }
}

.article-body {
  font-size: 30rpx;
  color: #333;
  line-height: 1.8;

  p {
    margin-bottom: 20rpx;
  }

  h3 {
    font-size: 32rpx;
    font-weight: 600;
    margin: 30rpx 0 16rpx;
  }
}

.activity-info {
  margin-top: 40rpx;
  padding: 24rpx;
  background: #f9f9f9;
  border-radius: 12rpx;

  .info-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 20rpx;
  }

  .info-item {
    display: flex;
    margin-bottom: 16rpx;

    .info-label {
      width: 140rpx;
      font-size: 26rpx;
      color: #999;
    }

    .info-value {
      flex: 1;
      font-size: 26rpx;
      color: #333;
    }
  }
}

.action-section {
  margin-top: 40rpx;

  .participate-btn {
    width: 100%;
    height: 88rpx;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    font-size: 32rpx;
    border-radius: 44rpx;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.footer-info {
  display: flex;
  justify-content: center;
  gap: 40rpx;
  margin-top: 40rpx;
  padding-top: 30rpx;
  border-top: 1rpx solid #f0f0f0;

  text {
    font-size: 24rpx;
    color: #999;
  }
}

.related-section {
  margin-top: 40rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 20rpx;
  }

  .related-item {
    display: flex;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:active {
      opacity: 0.7;
    }

    &:last-child {
      border-bottom: none;
    }

    .related-cover {
      width: 160rpx;
      height: 100rpx;
      border-radius: 8rpx;
      margin-right: 16rpx;
      background: #f0f0f0;
    }

    .related-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .related-title {
        font-size: 28rpx;
        color: #333;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .related-time {
        font-size: 24rpx;
        color: #999;
      }
    }
  }
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 100rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 0 60rpx;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.05);

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .icon {
      font-size: 40rpx;
      color: #666;
      margin-bottom: 4rpx;

      &.liked {
        color: #e74c3c;
      }
    }

    text {
      font-size: 22rpx;
      color: #666;
    }
  }
}
</style>
