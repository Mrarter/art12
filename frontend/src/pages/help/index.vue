<template>
  <view class="help-page">
    <!-- 页面标题 -->
    <view class="page-header">
      <view class="header-left" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#333"></u-icon>
      </view>
      <text class="header-title">帮助中心</text>
      <view class="header-right"></view>
    </view>

    <!-- 搜索框 -->
    <view class="search-section">
      <view class="search-box">
        <u-icon name="search" size="18" color="#999"></u-icon>
        <input class="search-input" type="text" v-model="keyword" placeholder="搜索帮助问题" @confirm="onSearch" />
      </view>
    </view>

    <!-- 热门问题 -->
    <view class="help-section">
      <view class="section-title">热门问题</view>
      <view class="question-list">
        <view class="question-item" v-for="item in hotQuestions" :key="item.id" @click="toggleQuestion(item)">
          <view class="question-header">
            <view class="question-icon">
              <u-icon name="help" size="16" color="#667eea"></u-icon>
            </view>
            <text class="question-title">{{ item.title }}</text>
            <u-icon :name="item.expanded ? 'arrow-up' : 'arrow-down'" size="14" color="#ccc"></u-icon>
          </view>
          <view class="question-answer" v-if="item.expanded">
            <rich-text :nodes="item.answer"></rich-text>
          </view>
        </view>
      </view>
    </view>

    <!-- 问题分类 -->
    <view class="help-section">
      <view class="section-title">问题分类</view>
      <view class="category-grid">
        <view class="category-item" v-for="cat in categories" :key="cat.id" @click="goCategory(cat)">
          <image class="category-icon" :src="cat.icon" mode="aspectFit"></image>
          <text class="category-name">{{ cat.name }}</text>
        </view>
      </view>
    </view>

    <!-- 常见问题分类列表 -->
    <view class="help-section" v-for="cat in questionCategories" :key="cat.id">
      <view class="section-title">{{ cat.name }}</view>
      <view class="question-list simple">
        <view class="question-item simple" v-for="q in cat.questions" :key="q.id" @click="toggleQuestion(q)">
          <text class="question-title">{{ q.title }}</text>
          <u-icon :name="q.expanded ? 'arrow-up' : 'arrow-down'" size="14" color="#ccc"></u-icon>
          <view class="question-answer" v-if="q.expanded">
            <text class="answer-text">{{ q.answer }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 联系客服 -->
    <view class="contact-section">
      <view class="contact-card">
        <view class="contact-title">没找到答案？</view>
        <view class="contact-desc">联系我们的客服团队获取帮助</view>
        <button class="btn-contact" @click="contactService">
          <u-icon name="kefu-ermai" size="18" color="#fff"></u-icon>
          <text>联系客服</text>
        </button>
      </view>
    </view>

    <!-- 常见问题列表 -->
    <view class="faq-section">
      <view class="section-title">更多问题</view>
      <view class="faq-list">
        <view class="faq-item" v-for="faq in faqList" :key="faq.id" @click="goFaqDetail(faq)">
          <view class="faq-content">
            <text class="faq-title">{{ faq.title }}</text>
            <text class="faq-desc">{{ faq.desc }}</text>
          </view>
          <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      keyword: '',
      hotQuestions: [
        {
          id: 1,
          title: '如何购买艺术品？',
          answer: '您可以通过以下步骤购买艺术品：<br/>1. 在画廊中找到喜欢的作品<br/>2. 点击"立即购买"或"加入购物车"<br/>3. 确认订单信息并完成支付<br/>4. 等待艺术家发货<br/>5. 确认收货后即可评价',
          expanded: false
        },
        {
          id: 2,
          title: '如何成为艺术家？',
          answer: '成为艺术家的步骤：<br/>1. 在个人中心点击"艺术家入口"<br/>2. 提交艺术家认证申请<br/>3. 上传作品集和个人简介<br/>4. 等待审核通过<br/>5. 审核通过后即可发布作品',
          expanded: false
        },
        {
          id: 3,
          title: '艺荐官如何赚取佣金？',
          answer: '成为艺荐官后：<br/>1. 分享作品链接给好友<br/>2. 好友通过您的链接购买<br/>3. 您可获得一级佣金（5%）<br/>4. 好友再推荐其他人购买<br/>5. 您可获得二级佣金（2%）',
          expanded: false
        },
        {
          id: 4,
          title: '拍卖保证金如何退还？',
          answer: '保证金退还规则：<br/>1. 竞拍未成功，保证金自动退还<br/>2. 竞拍成功后未付款，保证金扣除<br/>3. 竞拍成功后完成付款，保证金可提现或用于购买',
          expanded: false
        },
        {
          id: 5,
          title: '如何联系艺术家？',
          answer: '联系艺术家的方式：<br/>1. 在作品详情页点击"联系艺术家"<br/>2. 通过站内消息发送咨询<br/>3. 查看艺术家主页获取联系方式',
          expanded: false
        }
      ],
      categories: [
        { id: 1, name: '账号问题', icon: '/static/icons/help-account.png' },
        { id: 2, name: '交易问题', icon: '/static/icons/help-trade.png' },
        { id: 3, name: '拍卖问题', icon: '/static/icons/help-auction.png' },
        { id: 4, name: '佣金问题', icon: '/static/icons/help-commission.png' }
      ],
      questionCategories: [
        {
          id: 1,
          name: '账号与登录',
          questions: [
            { id: 101, title: '手机号无法登录怎么办？', answer: '请检查手机号是否正确，或尝试使用微信登录。如仍有问题，请联系客服。', expanded: false },
            { id: 102, title: '如何修改昵称和头像？', answer: '进入个人中心，点击头像区域即可修改昵称和头像。', expanded: false },
            { id: 103, title: '如何更换绑定手机号？', answer: '在设置页面点击"绑定手机"，验证后即可更换。', expanded: false }
          ]
        },
        {
          id: 2,
          name: '订单与支付',
          questions: [
            { id: 201, title: '支持哪些支付方式？', answer: '支持微信支付、支付宝支付。', expanded: false },
            { id: 202, title: '订单取消后多久退款？', answer: '订单取消后，退款将在1-3个工作日内原路返回。', expanded: false },
            { id: 203, title: '如何申请退款？', answer: '在订单详情页点击"申请退款"，填写原因后提交。', expanded: false }
          ]
        }
      ],
      faqList: [
        { id: 1, title: '艺术家认证指南', desc: '了解如何成为认证艺术家' },
        { id: 2, title: '艺荐官等级规则', desc: '了解艺荐官等级权益' },
        { id: 3, title: '拍卖规则说明', desc: '了解拍卖参与规则' },
        { id: 4, title: '艺术品保养指南', desc: '如何保养您的艺术品' },
        { id: 5, title: '物流与配送', desc: '艺术品配送说明' }
      ]
    }
  },
  
  methods: {
    goBack() {
      uni.navigateBack()
    },
    
    onSearch() {
      uni.showToast({ title: '搜索：' + this.keyword, icon: 'none' })
    },
    
    toggleQuestion(item) {
      item.expanded = !item.expanded
    },
    
    goCategory(cat) {
      uni.showToast({ title: '进入：' + cat.name, icon: 'none' })
    },
    
    contactService() {
      uni.makePhoneCall({
        phoneNumber: '400-888-8888',
        fail: () => {
          uni.showToast({ title: '客服电话：400-888-8888', icon: 'none' })
        }
      })
    },
    
    goFaqDetail(faq) {
      uni.navigateTo({ url: `/pages/help/detail?id=${faq.id}&title=${faq.title}` })
    }
  }
}
</script>

<style scoped>
.help-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40rpx;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 88rpx;
  padding: 0 30rpx;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left,
.header-right {
  width: 60rpx;
}

.header-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}

/* 搜索框 */
.search-section {
  padding: 20rpx 30rpx;
  background: #fff;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 32rpx;
  padding: 16rpx 24rpx;
}

.search-input {
  flex: 1;
  margin-left: 12rpx;
  font-size: 28rpx;
}

/* 帮助分类 */
.help-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

/* 热门问题 */
.question-list .question-item {
  border-bottom: 1rpx solid #f0f0f0;
  padding: 24rpx 0;
}

.question-list .question-item:last-child {
  border-bottom: none;
}

.question-header {
  display: flex;
  align-items: center;
}

.question-icon {
  width: 40rpx;
  height: 40rpx;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
}

.question-title {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.question-answer {
  margin-top: 16rpx;
  padding: 20rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.8;
}

.question-list.simple .question-item {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
}

.question-list.simple .question-title {
  flex: 1;
}

.question-list.simple .question-answer {
  width: 100%;
  margin-left: 56rpx;
}

.answer-text {
  display: block;
  margin-top: 12rpx;
}

/* 问题分类 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx;
}

.category-icon {
  width: 64rpx;
  height: 64rpx;
  margin-bottom: 12rpx;
}

.category-name {
  font-size: 24rpx;
  color: #666;
}

/* 联系客服 */
.contact-section {
  margin: 20rpx;
}

.contact-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20rpx;
  padding: 40rpx;
  text-align: center;
}

.contact-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}

.contact-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 12rpx;
  margin-bottom: 30rpx;
}

.btn-contact {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 240rpx;
  height: 72rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 36rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.5);
}

.btn-contact text {
  color: #fff;
  font-size: 28rpx;
  margin-left: 8rpx;
}

/* FAQ列表 */
.faq-section {
  background: #fff;
  margin: 20rpx;
  border-radius: 16rpx;
  padding: 30rpx;
}

.faq-list .faq-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.faq-list .faq-item:last-child {
  border-bottom: none;
}

.faq-content {
  flex: 1;
}

.faq-title {
  font-size: 28rpx;
  color: #333;
  display: block;
}

.faq-desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
}
</style>
