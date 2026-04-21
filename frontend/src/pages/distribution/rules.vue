<template>
  <view class="rules-page">
    <!-- 协议内容 -->
    <view class="content">
      <!-- 分销概述 -->
      <view class="section">
        <text class="section-title">分销概述</text>
        <view class="section-content">
          <text class="paragraph">
            艺12分销体系是平台为用户提供的一种赚钱方式。用户通过分享平台商品或邀请好友加入，成功交易后可获得相应佣金奖励。
          </text>
        </view>
      </view>

      <!-- 分销等级 -->
      <view class="section">
        <text class="section-title">分销等级</text>
        <view class="level-list">
          <view class="level-item">
            <view class="level-header">
              <text class="level-name">普通分销商</text>
              <text class="level-condition">无条件加入</text>
            </view>
            <view class="level-info">
              <view class="info-row">
                <text class="label">佣金比例</text>
                <text class="value">商品价格的 3%</text>
              </view>
              <view class="info-row">
                <text class="label">提现门槛</text>
                <text class="value">满 100 元可提现</text>
              </view>
            </view>
          </view>
          <view class="level-item gold">
            <view class="level-header">
              <text class="level-name">金牌分销商</text>
              <text class="level-condition">累计收益满 5000 元</text>
            </view>
            <view class="level-info">
              <view class="info-row">
                <text class="label">佣金比例</text>
                <text class="value">商品价格的 5%</text>
              </view>
              <view class="info-row">
                <text class="label">团队奖励</text>
                <text class="value">团队成员成交的 1%</text>
              </view>
              <view class="info-row">
                <text class="label">提现门槛</text>
                <text class="value">满 50 元可提现</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 佣金规则 -->
      <view class="section">
        <text class="section-title">佣金规则</text>
        <view class="rule-list">
          <view class="rule-item">
            <text class="rule-title">1. 佣金计算</text>
            <text class="rule-content">
              佣金 = 商品成交价 × 佣金比例
            </text>
            <text class="rule-content">
              示例：商品价格 10000 元，金牌分销商可得 500 元佣金
            </text>
          </view>
          <view class="rule-item">
            <text class="rule-title">2. 佣金结算</text>
            <text class="rule-content">
              • 订单完成后 7 天无退款，佣金结算
            </text>
            <text class="rule-content">
              • 佣金可直接提现或用于购物
            </text>
          </view>
          <view class="rule-item">
            <text class="rule-title">3. 团队奖励</text>
            <text class="rule-content">
              金牌分销商可获得团队成员成交金额的 1% 作为团队奖励
            </text>
          </view>
        </view>
      </view>

      <!-- 订单追踪 -->
      <view class="section">
        <text class="section-title">订单追踪</text>
        <view class="order-flow">
          <view class="flow-step">
            <text class="step-num">1</text>
            <text class="step-name">分享</text>
          </view>
          <view class="flow-arrow">→</view>
          <view class="flow-step">
            <text class="step-num">2</text>
            <text class="step-name">好友下单</text>
          </view>
          <view class="flow-arrow">→</view>
          <view class="flow-step">
            <text class="step-num">3</text>
            <text class="step-name">交易完成</text>
          </view>
          <view class="flow-arrow">→</view>
          <view class="flow-step">
            <text class="step-num">4</text>
            <text class="step-name">佣金到账</text>
          </view>
        </view>
      </view>

      <!-- 常见问题 -->
      <view class="section">
        <text class="section-title">常见问题</text>
        <view class="qa-list">
          <view class="qa-item">
            <text class="question">Q: 如何成为金牌分销商？</text>
            <text class="answer">A: 累计获得佣金满 5000 元即可自动升级为金牌分销商。</text>
          </view>
          <view class="qa-item">
            <text class="question">Q: 佣金什么时候可以提现？</text>
            <text class="answer">A: 订单完成 7 天后佣金到账，达到提现门槛即可申请提现。</text>
          </view>
          <view class="qa-item">
            <text class="question">Q: 退款订单佣金如何处理？</text>
            <text class="answer">A: 已结算的佣金将从下次收益中扣除。</text>
          </view>
          <view class="qa-item">
            <text class="question">Q: 如何邀请好友加入？</text>
            <text class="answer">A: 在"我的"页面分享您的邀请码或海报，好友注册时填写邀请码即可。</text>
          </view>
        </view>
      </view>

      <!-- 更新说明 -->
      <view class="section">
        <text class="section-title">规则更新</text>
        <view class="update-log">
          <view class="log-item">
            <text class="log-date">2024年3月1日</text>
            <text class="log-content">新增金牌分销商团队奖励机制</text>
          </view>
          <view class="log-item">
            <text class="log-date">2024年1月15日</text>
            <text class="log-content">佣金提现周期调整为 T+7</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {}
  }
}
</script>

<style lang="scss" scoped>
.rules-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 24rpx;
}

.content {
  padding-bottom: 60rpx;
}

.section {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.paragraph {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

.level-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.level-item {
  background: #f8f8f8;
  border-radius: 12rpx;
  padding: 24rpx;
}

.level-item.gold {
  background: linear-gradient(135deg, #fff9e6 0%, #fff5d6 100%);
  border: 2rpx solid #f5af19;
}

.level-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.level-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.level-condition {
  font-size: 22rpx;
  color: #999;
}

.level-info {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
}

.info-row .label {
  font-size: 26rpx;
  color: #666;
}

.info-row .value {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.rule-item {
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f5f5f5;
}

.rule-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.rule-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.rule-content {
  font-size: 26rpx;
  color: #666;
  line-height: 1.8;
  display: block;
}

.order-flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
}

.flow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}

.step-num {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 50%;
  text-align: center;
}

.step-name {
  font-size: 24rpx;
  color: #666;
}

.flow-arrow {
  color: #ccc;
  font-size: 28rpx;
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.qa-item {
  padding: 16rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
}

.question {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 10rpx;
}

.answer {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.update-log {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.log-item {
  display: flex;
  gap: 16rpx;
}

.log-date {
  font-size: 24rpx;
  color: #999;
  min-width: 140rpx;
}

.log-content {
  font-size: 26rpx;
  color: #333;
}
</style>