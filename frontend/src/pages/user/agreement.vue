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
            艺本艺术所有权和运营权归艺本艺术科技有限公司所有。用户在使用艺本艺术提供的服务时，应遵守以下条款。本协议具有合同效力，用户一经注册或使用本服务即视为对本协议全部内容的接受。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、服务内容</view>
          <view class="section-content">
            1. 艺本艺术是一个艺术品在线交易平台，为用户提供艺术品展示、交易、支付、物流等服务。<br>
            2. 艺本艺术保留随时变更、修改或终止服务的权利。<br>
            3. 艺本艺术会尽力维护服务的稳定性和安全性，但不对因不可抗力、第三方原因等造成的服务中断承担责任。
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
            1. 平台上的所有内容，包括但不限于文字、图片、视频、代码等，其知识产权归艺本艺术或相应权利人所有。<br>
            2. 用户上传的作品应保证拥有合法的知识产权，不侵犯他人权益。<br>
            3. 用户授权平台在合法范围内使用其上传的内容。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、争议解决</view>
          <view class="section-content">
            1. 本协议的解释和执行均适用中华人民共和国法律。<br>
            2. 因本协议引起的争议，双方应友好协商解决。<br>
            3. 协商不成的，任何一方可向艺本艺术所在地人民法院提起诉讼。
          </view>
        </view>
      </template>

      <!-- 隐私政策内容 -->
      <template v-else-if="agreementType === 'privacy'">
        <view class="artist-summary privacy-summary">
          <view class="summary-item">
            <text class="summary-label">收集原则</text>
            <text class="summary-value">最小必要</text>
          </view>
          <view class="summary-item">
            <text class="summary-label">敏感信息</text>
            <text class="summary-value">单独授权</text>
          </view>
          <view class="summary-item highlight">
            <text class="summary-label">用户权利</text>
            <text class="summary-value">可访问、更正、删除与撤回授权</text>
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">一、信息收集</view>
          <view class="section-content">
            1. 我们遵循合法、正当、必要和诚信原则收集个人信息。<br>
            2. 您主动提供的信息包括账号资料、联系方式、收货地址、实名认证资料、艺术家认证资料、作品信息、订单信息、支付与售后信息。<br>
            3. 您使用服务时产生的信息包括设备信息、浏览记录、搜索记录、收藏/关注行为、登录日志、交易记录和必要的风控记录。<br>
            4. 身份证件、人脸核验、银行卡、支付宝、微信收款账户等属于敏感个人信息，我们会在具体场景中提示用途并取得必要授权。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、信息使用</view>
          <view class="section-content">
            1. 用于账号登录、实名认证、艺术家认证、订单履约、作品展示、收藏证书生成、支付结算、物流配送和售后服务。<br>
            2. 用于安全风控、交易风险识别、反欺诈、争议处理和平台规则执行。<br>
            3. 用于改进产品体验、统计分析和服务质量评估。统计分析会尽量采用去标识化或匿名化数据。<br>
            4. 如用于个性化推荐、活动通知或营销信息，我们会提供退订或关闭方式。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、信息共享</view>
          <view class="section-content">
            1. 未经您同意，我们不会向无关第三方共享可识别个人身份的信息。<br>
            2. 为完成支付、物流、实名认证、短信通知、云存储、客服和争议处理，我们可能向必要合作方提供完成服务所需的最小信息。<br>
            3. 在法律法规、司法机关或行政机关依法要求时，我们可能依法披露相关信息。<br>
            4. 对外统计、展示或研究场景中，我们会优先使用匿名化或去标识化数据。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、信息保护</view>
          <view class="section-content">
            1. 我们通过权限控制、加密传输、访问审计、数据备份和安全监控保护您的信息。<br>
            2. 对身份证件、人脸核验、支付账户等敏感信息，我们会采取更严格的访问控制。<br>
            3. 我们仅在实现本政策所述目的所必需的期间内保存您的个人信息，法律法规另有要求的除外。<br>
            4. 如发生个人信息安全事件，我们将按照法律法规要求及时告知您事件影响、处置措施和防范建议。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">五、您的权利</view>
          <view class="section-content">
            1. 您有权访问、更正、补充您的个人信息。<br>
            2. 在符合法律法规要求的情况下，您可以申请删除个人信息、注销账号或撤回授权。<br>
            3. 撤回授权可能导致部分服务无法继续使用，例如实名认证、支付结算、订单履约或艺术家认证。<br>
            4. 如您需要行使相关权利，可通过平台客服、意见反馈或设置页面联系我们。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、未成年人保护与政策更新</view>
          <view class="section-content">
            1. 我们的交易、认证和入驻服务原则上面向具备完全民事行为能力的成年人。<br>
            2. 如未成年人使用平台服务，应在监护人同意和指导下进行。<br>
            3. 本政策如发生重大变更，我们会通过页面提示、站内消息或其他合理方式通知您。<br>
            4. 您继续使用平台服务，即表示您已了解更新后的政策内容。
          </view>
        </view>
      </template>

      <!-- 艺术家入驻协议 -->
      <template v-else-if="agreementType === 'artist'">
        <view class="artist-summary">
          <view class="summary-item">
            <text class="summary-label">年度服务费</text>
            <text class="summary-value">¥3,600 / 年</text>
          </view>
          <view class="summary-item">
            <text class="summary-label">平台服务分成</text>
            <text class="summary-value">15% / 笔</text>
          </view>
          <view class="summary-item highlight">
            <text class="summary-label">当前阶段权益</text>
            <text class="summary-value">邀请好友赠送年费</text>
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">一、认证与入驻条件</view>
          <view class="section-content">
            1. 申请人须为原创艺术作品的创作者。<br>
            2. 申请人须年满18周岁，具有完全民事行为能力。<br>
            3. 申请人需提供真实有效的身份证明材料。<br>
            4. 申请人需提供20件本人原创代表作品，供平台进行风格、质量与持续创作能力审核。<br>
            5. 申请人需遵守平台规则，不从事违法违规活动。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、申请流程</view>
          <view class="section-content">
            1. 阅读认证指南，确认年费、平台服务分成和作品提交要求。<br>
            2. 录入真实个人信息，包括真实姓名、身份证号、艺术领域、所在城市、联系方式和个人简介。<br>
            3. 上传20件本人代表作品及必要的身份证明材料。<br>
            4. 平台将在提交后进行资料核验、作品审核和风险评估，并通过站内通知或联系方式反馈结果。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、费用与分成</view>
          <view class="section-content">
            1. 艺术家认证年度服务费为人民币3,600元/年。<br>
            2. 当前推广阶段，艺术家分享平台并邀请好友完成注册，可获得平台赠送的3,600元年度服务费权益。具体领取规则以页面展示及平台通知为准。<br>
            3. 艺术家作品发生交易时，平台按每笔消费金额收取15%的平台服务分成。<br>
            4. 订单款项将在交易完成、售后期届满或平台确认无争议后，按照平台结算周期进行结算。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、权利义务</view>
          <view class="section-content">
            <strong>权利：</strong><br>
            1. 入驻艺术家享有平台提供的各项服务。<br>
            2. 入驻艺术家可参与平台组织的各类活动。<br>
            3. 入驻艺术家可获得平台提供的推广资源、艺术家主页、作品发布与数据看板等能力。<br><br>
            <strong>义务：</strong><br>
            1. 保证上传作品为本人原创，不侵犯他人权益。<br>
            2. 及时处理买家咨询和订单。<br>
            3. 保证作品质量，按时发货。<br>
            4. 遵守平台规则，维护平台秩序。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">五、作品与知识产权</view>
          <view class="section-content">
            1. 艺术家应确保提交、发布和销售的作品为本人原创或已获得合法授权。<br>
            2. 艺术家授权平台在推广、展示、交易、证书生成和服务履约所需范围内使用作品图片、作品信息和艺术家资料。<br>
            3. 如因作品权属、肖像权、著作权或其他第三方权益产生争议，由艺术家承担相应责任。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、违规处理</view>
          <view class="section-content">
            1. 发现违规行为，平台有权采取警告、下架、封号等措施。<br>
            2. 违规行为造成损失的，由违规方承担相应责任。<br>
            3. 艺术家有权申诉，平台将进行核实处理。
          </view>
        </view>
      </template>

      <!-- 经纪人协议 -->
      <template v-else-if="agreementType === 'promoter'">
        <view class="content-section">
          <view class="section-title">一、经纪人职责</view>
          <view class="section-content">
            1. 经纪人负责推广平台商品，分享给潜在买家。<br>
            2. 经纪人需维护平台形象，遵守推广规范。<br>
            3. 经纪人应积极引导用户完成交易。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、分成制度</view>
          <view class="section-content">
            1. 经纪人根据有效推广订单获得分成。<br>
            2. 分成比例为商品成交金额的5%-15%。<br>
            3. 分成按月结算，扣除相关费用后发放。<br>
            4. 经纪人等级越高，分成比例越高。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、违规处理</view>
          <view class="section-content">
            1. 严禁使用虚假推广、刷单等违规手段。<br>
            2. 严禁传播违法、有害信息。<br>
            3. 违规者将被取消经纪人资格并追究责任。
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
        promoter: '经纪人协议'
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
  background:
    radial-gradient(circle at 20% 0, rgba(205, 165, 74, 0.18), transparent 34%),
    #0b0b0c;
  padding: 24rpx;
  box-sizing: border-box;
}

:global(page),
:global(uni-page-body) {
  background: #0b0b0c;
}

.agreement-header {
  padding: 44rpx 10rpx 30rpx;
  text-align: left;

  .agreement-title {
    display: block;
    font-size: 46rpx;
    line-height: 58rpx;
    font-weight: 800;
    color: #f8f3e8;
  }

  .agreement-date {
    display: block;
    font-size: 24rpx;
    color: rgba(248, 243, 232, 0.48);
    margin-top: 16rpx;
  }
}

.agreement-content {
  padding: 30rpx;
  border-radius: 26rpx;
  background: rgba(24, 24, 26, 0.96);
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24rpx 60rpx rgba(0, 0, 0, 0.28);

  .artist-summary {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16rpx;
    margin-bottom: 34rpx;
  }

  .privacy-summary .summary-value {
    font-size: 26rpx;
  }

  .summary-item {
    padding: 22rpx;
    border-radius: 18rpx;
    background: rgba(255, 255, 255, 0.045);
    border: 1rpx solid rgba(255, 255, 255, 0.08);

    &.highlight {
      grid-column: span 2;
      background: rgba(205, 165, 74, 0.12);
      border-color: rgba(205, 165, 74, 0.3);
    }
  }

  .summary-label {
    display: block;
    font-size: 22rpx;
    color: rgba(248, 243, 232, 0.5);
    margin-bottom: 8rpx;
  }

  .summary-value {
    display: block;
    font-size: 28rpx;
    line-height: 36rpx;
    font-weight: 800;
    color: #d9b95f;
  }

  .content-section {
    margin-bottom: 28rpx;
    padding-bottom: 28rpx;
    border-bottom: 1rpx solid rgba(255, 255, 255, 0.07);

    &:last-child {
      margin-bottom: 0;
      padding-bottom: 0;
      border-bottom: 0;
    }

    .section-title {
      font-size: 31rpx;
      line-height: 42rpx;
      font-weight: 800;
      color: #f8f3e8;
      margin-bottom: 16rpx;
    }

    .section-content {
      font-size: 26rpx;
      color: rgba(248, 243, 232, 0.68);
      line-height: 1.9;
      text-align: justify;

      strong {
        color: #f1dfad;
        font-weight: 700;
      }
    }
  }
}
</style>
