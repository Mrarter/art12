<template>
  <view class="help-center">
    <view class="search-bar">
      <input class="search-input" placeholder="搜索帮助内容" v-model="searchKeyword" />
      <text class="search-btn" @click="doSearch">搜索</text>
    </view>

    <view class="category-section">
      <text class="section-title">常见问题</text>
      <view class="category-grid">
        <view class="category-item" v-for="item in visibleCategories" :key="item.type" @click="goToCategory(item.type)">
          <image class="category-icon" :src="item.icon" mode="aspectFit" />
          <text class="category-name">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <view class="faq-section">
      <text class="section-title">热门问题</text>
      <view class="faq-list">
        <view class="faq-item" v-for="(item, index) in visibleHotQuestions" :key="index" @click="showAnswer(item)">
          <view class="faq-content">
            <text class="faq-q">Q: {{ item.question }}</text>
            <text class="faq-a">{{ item.answer }}</text>
          </view>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <view class="service-section">
      <text class="section-title">快捷服务</text>
      <view class="service-grid">
        <view class="service-item" @click="callService">
          <image class="service-icon" src="/static/icon-phone.png" mode="aspectFit" />
          <text class="service-name">联系客服</text>
        </view>
        <view class="service-item" @click="submitFeedback">
          <image class="service-icon" src="/static/icon-feedback.png" mode="aspectFit" />
          <text class="service-name">意见反馈</text>
        </view>
        <view class="service-item" @click="checkProgress">
          <image class="service-icon" src="/static/icon-progress.png" mode="aspectFit" />
          <text class="service-name">处理进度</text>
        </view>
        <view class="service-item" @click="viewAgreement">
          <image class="service-icon" src="/static/icon-agreement.png" mode="aspectFit" />
          <text class="service-name">用户协议</text>
        </view>
      </view>
    </view>

    <view class="service-tip">
      <text>在线客服工作时间：9:00 - 21:00</text>
      <text>电话客服：400-888-8888</text>
    </view>
  </view>
</template>

<script>
import { AUCTION_ENABLED } from '@/utils/platform.js'

const CATEGORY_ITEMS = [
  { type: 'account', name: '账号问题', icon: '/static/help-account.png' },
  { type: 'transaction', name: '交易问题', icon: '/static/help-transaction.png' },
  { type: 'payment', name: '支付问题', icon: '/static/help-payment.png' },
  { type: 'delivery', name: '配送问题', icon: '/static/help-delivery.png' },
  { type: 'auction', name: '拍卖问题', icon: '/static/help-auction.png' },
  { type: 'artist', name: '艺术家入驻', icon: '/static/help-artist.png' }
]

const HOT_QUESTIONS = [
  {
    question: '如何成为艺术家？',
    answer: '进入"我的"页面，点击"申请成为艺术家"，填写资料并提交审核，审核通过后即可发布作品。'
  },
  {
    question: '拍品如何缴纳保证金？',
    answer: '进入拍卖详情页，点击"缴纳保证金"按钮，选择支付方式完成缴纳。保证金会在竞拍结束后原路退还。'
  },
  {
    question: '订单取消后多久退款？',
    answer: '订单取消后，退款会在1-7个工作日内原路返回到您的支付账户。'
  },
  {
    question: '如何设置竞拍提醒？',
    answer: '在拍卖详情页点击"提醒我"按钮，设置提醒时间，系统会在开拍前15分钟通知您。'
  },
  {
    question: '作品售出后多久收款？',
    answer: '买家确认收货后7天内无退款申请，款项会自动转入您的账户余额。'
  },
  {
    question: '如何邀请好友赚分成？',
    answer: '在"分销中心"页面点击"分享赚更多"，生成分享海报发给好友即可。'
  }
]

export default {
  data() {
    return {
      searchKeyword: ''
    }
  },
  computed: {
    visibleCategories() {
      return CATEGORY_ITEMS.filter(item => AUCTION_ENABLED || item.type !== 'auction')
    },
    visibleHotQuestions() {
      return HOT_QUESTIONS.filter(item => {
        if (AUCTION_ENABLED) return true
        return !item.question.includes('拍') && !item.answer.includes('拍卖') && !item.answer.includes('竞拍')
      })
    }
  },
  methods: {
    doSearch() {
      if (this.searchKeyword) {
        uni.showToast({ title: '搜索: ' + this.searchKeyword, icon: 'none' })
      }
    },
    goToCategory(category) {
      uni.navigateTo({ url: `/pages/help/category?type=${category}` })
    },
    showAnswer(item) {
      uni.showModal({
        title: item.question,
        content: item.answer,
        showCancel: false
      })
    },
    callService() {
      uni.makePhoneCall({
        phoneNumber: '400-888-8888'
      })
    },
    submitFeedback() {
      uni.navigateTo({ url: '/pages/user/feedback' })
    },
    checkProgress() {
      uni.navigateTo({ url: '/pages/message/list' })
    },
    viewAgreement() {
      uni.navigateTo({ url: '/pages/user-extra/agreement' })
    }
  }
}
</script>

<style lang="scss" scoped>
.help-center {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.search-bar {
  background: #fff;
  padding: 20rpx 24rpx;
  display: flex;
  gap: 16rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
}

.search-btn {
  width: 120rpx;
  height: 72rpx;
  line-height: 72rpx;
  background: #667eea;
  color: #fff;
  text-align: center;
  border-radius: 36rpx;
  font-size: 28rpx;
}

.category-section,
.faq-section,
.service-section {
  background: #fff;
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 24rpx;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30rpx;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.category-icon {
  width: 80rpx;
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 20rpx;
  padding: 18rpx;
  box-sizing: border-box;
}

.category-name {
  font-size: 24rpx;
  color: #333;
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.faq-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
}

.faq-content {
  flex: 1;
}

.faq-q {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.faq-a {
  display: block;
  font-size: 24rpx;
  color: #666;
  line-height: 1.6;
}

.arrow {
  color: #bbb;
  font-size: 30rpx;
  line-height: 1;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24rpx;
}

.service-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.service-icon {
  width: 72rpx;
  height: 72rpx;
}

.service-name {
  font-size: 24rpx;
  color: #333;
}

.service-tip {
  margin: 0 24rpx;
  padding: 24rpx 30rpx;
  color: #666;
  font-size: 24rpx;
  line-height: 1.8;
}

.service-tip text {
  display: block;
}
</style>
