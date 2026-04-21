<template>
  <view class="agreement-page">
    <!-- 协议标题 -->
    <view class="agreement-header">
      <text class="agreement-title">{{ agreementTitle }}</text>
      <text class="agreement-date">更新日期：2024年1月1日</text>
    </view>

    <!-- 协议内容 -->
    <view class="agreement-content card">
      <!-- 用户协议内容 -->
      <template v-if="agreementType === 'user'">
        <view class="content-section">
          <view class="section-title">一、服务条款的确认和接纳</view>
          <view class="section-content">
            拾艺局所有权和运营权归拾艺局科技有限公司所有。用户在使用拾艺局提供的服务时，应遵守以下条款。本协议具有合同效力，用户一经注册或使用本服务即视为对本协议全部内容的接受。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、服务内容</view>
          <view class="section-content">
            1. 拾艺局是一个艺术品在线交易平台，为用户提供艺术品展示、交易、支付、物流等服务。<br>
            2. 拾艺局保留随时变更、修改或终止服务的权利。<br>
            3. 拾艺局会尽力维护服务的稳定性和安全性，但不对因不可抗力、第三方原因等造成的服务中断承担责任。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、用户注册</view>
          <view class="section-content">
            1. 用户应保证注册信息的真实性、完整性和合法性。<br>
            2. 用户须年满18周岁，具有完全民事行为能力。<br>
            3. 用户须妥善保管账户信息，因个人原因导致的账户被盗用，由用户承担全部责任。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、交易规则</view>
          <view class="section-content">
            1. 用户在平台上进行的艺术品交易应遵循诚实信用原则。<br>
            2. 用户应确保所交易的艺术品为合法所有，不侵犯第三方权益。<br>
            3. 交易完成后，买卖双方应履行各自的义务，包括付款、交货等。<br>
            4. 平台有权对异常交易进行监控和处理。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">五、知识产权</view>
          <view class="section-content">
            1. 平台上的所有内容，包括但不限于文字、图片、视频、代码等，其知识产权归拾艺局或相应权利人所有。<br>
            2. 用户上传的作品应保证拥有合法的知识产权，不侵犯他人权益。<br>
            3. 用户授权平台在合法范围内使用其上传的内容。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、争议解决</view>
          <view class="section-content">
            1. 本协议的解释和执行均适用中华人民共和国法律。<br>
            2. 因本协议引起的争议，双方应友好协商解决。<br>
            3. 协商不成的，任何一方可向拾艺局所在地人民法院提起诉讼。
          </view>
        </view>
      </template>

      <!-- 隐私政策内容 -->
      <template v-else-if="agreementType === 'privacy'">
        <view class="content-section">
          <view class="section-title">一、信息收集</view>
          <view class="section-content">
            1. 我们收集您主动提供的信息，包括注册信息、交易信息、联系信息等。<br>
            2. 我们自动收集您使用服务时产生的信息，包括设备信息、访问日志、行为数据等。<br>
            3. 我们可能从合作伙伴处获取相关信息，以提供更精准的服务。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、信息使用</view>
          <view class="section-content">
            1. 使用您的信息提供、改进我们的服务。<br>
            2. 使用您的信息进行数据分析，优化产品体验。<br>
            3. 使用您的信息向您推送个性化的内容和广告。<br>
            4. 在法律法规允许的范围内使用您的信息。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、信息共享</view>
          <view class="section-content">
            1. 未经您同意，我们不会与任何第三方共享您的个人信息。<br>
            2. 在法律法规要求的情况下，我们可能披露您的信息。<br>
            3. 为提供服务和保护用户权益，我们可能与合作伙伴共享匿名化数据。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、信息保护</view>
          <view class="section-content">
            1. 我们采用业界领先的安全技术保护您的信息安全。<br>
            2. 我们建立完善的数据安全管理制度。<br>
            3. 定期进行安全审计和风险评估。<br>
            4. 发生安全事件时，我们会及时通知您。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">五、您的权利</view>
          <view class="section-content">
            1. 您有权访问、修改您的个人信息。<br>
            2. 您有权删除您的账户和个人信息。<br>
            3. 您有权撤回同意。<br>
            4. 您有权联系我们了解更多信息。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、Cookie政策</view>
          <view class="section-content">
            1. 我们使用Cookie提升您的使用体验。<br>
            2. 您可以管理或删除Cookie。<br>
            3. 禁用Cookie可能影响部分功能的使用。
          </view>
        </view>
      </template>

      <!-- 艺术家入驻协议 -->
      <template v-else-if="agreementType === 'artist'">
        <view class="content-section">
          <view class="section-title">一、入驻条件</view>
          <view class="section-content">
            1. 申请人须为原创艺术作品的创作者。<br>
            2. 申请人须年满18周岁，具有完全民事行为能力。<br>
            3. 申请人需提供真实有效的身份证明材料。<br>
            4. 申请人需遵守平台规则，不从事违法违规活动。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、权利义务</view>
          <view class="section-content">
            <strong>权利：</strong><br>
            1. 入驻艺术家享有平台提供的各项服务。<br>
            2. 入驻艺术家可参与平台组织的各类活动。<br>
            3. 入驻艺术家可获得平台提供的推广资源。<br><br>
            <strong>义务：</strong><br>
            1. 保证上传作品为本人原创，不侵犯他人权益。<br>
            2. 及时处理买家咨询和订单。<br>
            3. 保证作品质量，按时发货。<br>
            4. 遵守平台规则，维护平台秩序。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、收益分成</view>
          <view class="section-content">
            1. 艺术家销售作品的收益按照平台规定进行分成。<br>
            2. 分成比例根据艺术家等级和销售业绩动态调整。<br>
            3. 收益将在订单完成后按照平台结算周期进行结算。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、违规处理</view>
          <view class="section-content">
            1. 发现违规行为，平台有权采取警告、下架、封号等措施。<br>
            2. 违规行为造成损失的，由违规方承担相应责任。<br>
            3. 艺术家有权申诉，平台将进行核实处理。
          </view>
        </view>
      </template>

      <!-- 艺荐官协议 -->
      <template v-else-if="agreementType === 'promoter'">
        <view class="content-section">
          <view class="section-title">一、艺荐官职责</view>
          <view class="section-content">
            1. 艺荐官负责推广平台商品，分享给潜在买家。<br>
            2. 艺荐官需维护平台形象，遵守推广规范。<br>
            3. 艺荐官应积极引导用户完成交易。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、佣金制度</view>
          <view class="section-content">
            1. 艺荐官根据有效推广订单获得佣金。<br>
            2. 佣金比例为商品成交金额的5%-15%。<br>
            3. 佣金按月结算，扣除相关费用后发放。<br>
            4. 艺荐官等级越高，佣金比例越高。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、违规处理</view>
          <view class="section-content">
            1. 严禁使用虚假推广、刷单等违规手段。<br>
            2. 严禁传播违法、有害信息。<br>
            3. 违规者将被取消艺荐官资格并追究责任。
          </view>
        </view>
      </template>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      agreementType: 'user'
    }
  },

  computed: {
    agreementTitle() {
      const titles = {
        user: '用户服务协议',
        privacy: '隐私政策',
        artist: '艺术家入驻协议',
        promoter: '艺荐官协议'
      }
      return titles[this.agreementType] || '用户服务协议'
    }
  },

  onLoad(options) {
    if (options.type) {
      this.agreementType = options.type
    }
  }
}
</script>

<style lang="scss" scoped>
.agreement-page {
  min-height: 100vh;
  background: #f5f6f7;
  padding: 20rpx;
}

.agreement-header {
  padding: 40rpx 30rpx;
  text-align: center;

  .agreement-title {
    display: block;
    font-size: 40rpx;
    font-weight: 600;
    color: #333;
  }

  .agreement-date {
    display: block;
    font-size: 24rpx;
    color: #999;
    margin-top: 16rpx;
  }
}

.agreement-content {
  padding: 30rpx;

  .content-section {
    margin-bottom: 40rpx;

    &:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #333;
      margin-bottom: 16rpx;
    }

    .section-content {
      font-size: 26rpx;
      color: #666;
      line-height: 1.8;
      text-align: justify;
    }
  }
}
</style>
