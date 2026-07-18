<template>
  <view class="artist-identity-page">
    <view class="page-glow page-glow-left"></view>
    <view class="page-glow page-glow-right"></view>

    <view v-if="normalizedStatus === 'approved'" class="certificate-preview-card">
      <view class="certificate-preview-head">
        <text class="section-kicker">认证证书</text>
        <text class="certificate-preview-tag">平台示意版</text>
      </view>
      <view class="certificate-stage">
        <view class="certificate-sheet">
          <image class="certificate-frame" :src="certificateTemplate" mode="aspectFit" />
          <view class="certificate-mask"></view>
          <view class="certificate-watermark"></view>
          <view class="certificate-inner">
            <text class="certificate-cn-title">认证证书</text>
            <text class="certificate-en-title">ARTIST CERTIFICATE</text>
            <text class="certificate-owner">{{ certificateOwner }}</text>
            <text class="certificate-body">
              经艺本艺术平台审核确认，兹证明该艺术家已完成平台身份认证，其在平台发布并确权的作品将与艺术家身份长期绑定，后续流通、认证与成交记录可持续追溯。
            </text>
            <view class="certificate-meta">
              <text>证书编号：{{ certificateNo }}</text>
              <text>认证日期：{{ certificateDate }}</text>
            </view>
            <view class="certificate-footer">
              <view class="certificate-sign-block">
                <text class="certificate-sign-label">平台认证</text>
                <text class="certificate-sign-name">艺本艺术</text>
              </view>
              <view class="certificate-sign-block align-right">
                <text class="certificate-sign-label">认证状态</text>
                <text class="certificate-sign-status">{{ statusLabel }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="hero-card">
      <view class="hero-kicker">艺本艺术 · Artist Identity</view>
      <view class="hero-title-row">
        <text class="hero-title">认证艺术家</text>
        <view class="status-pill" :class="statusTone">{{ statusLabel }}</view>
      </view>
      <text class="hero-desc">
        艺术家完成平台认证后，作品将与艺术家身份长期绑定。作品后续每一次展示、流通与成交，都会持续回到艺术家名下，形成长期收益与评分积累。
      </text>

      <view class="hero-highlight">
        <view class="highlight-item" v-for="item in heroHighlights" :key="item.label">
          <text class="highlight-label">{{ item.label }}</text>
          <text class="highlight-value">{{ item.value }}</text>
        </view>
      </view>
    </view>

    <view class="section-card mechanism-card">
      <view class="section-head">
        <text class="section-kicker">绑定与收益</text>
        <text class="section-title">作品不是一次成交，而是长期资产</text>
      </view>
      <view class="mechanism-grid">
        <view class="mechanism-item" v-for="item in mechanisms" :key="item.title">
          <text class="mechanism-badge">{{ item.badge }}</text>
          <text class="mechanism-title">{{ item.title }}</text>
          <text class="mechanism-desc">{{ item.desc }}</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-kicker">身份价值</text>
        <text class="section-title">认证后你将获得</text>
      </view>
      <view class="benefit-list">
        <view class="benefit-item" v-for="item in benefits" :key="item.title">
          <view class="benefit-icon">{{ item.icon }}</view>
          <view class="benefit-copy">
            <text class="benefit-title">{{ item.title }}</text>
            <text class="benefit-desc">{{ item.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section-card showcase-card">
      <view class="section-head">
        <text class="section-kicker">平台展示</text>
        <text class="section-title">认证艺术家会出现在哪里</text>
      </view>
      <view class="showcase-list">
        <view class="showcase-item" v-for="item in showcases" :key="item.title">
          <text class="showcase-title">{{ item.title }}</text>
          <text class="showcase-desc">{{ item.desc }}</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-kicker">申请标准</text>
        <text class="section-title">平台审核关注这些信息</text>
      </view>
      <view class="requirement-list">
        <view class="requirement-item" v-for="(item, index) in requirements" :key="item.title">
          <view class="requirement-index">0{{ index + 1 }}</view>
          <view class="requirement-copy">
            <text class="requirement-title">{{ item.title }}</text>
            <text class="requirement-desc">{{ item.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section-card notes-card">
      <view class="section-head">
        <text class="section-kicker">说明与规则</text>
        <text class="section-title">认证不是荣誉标签，而是公开经营身份</text>
      </view>
      <view class="note-list">
        <text class="note-item" v-for="item in notes" :key="item">• {{ item }}</text>
      </view>
      <view class="note-fee">
        <text class="note-fee-title">费用与分成说明</text>
        <text class="note-fee-desc">
          年度服务费、平台服务分成等费用以认证条款与交易规则为准，详细内容请阅读页面下方入口中的正式条款。
        </text>
      </view>
    </view>

    <view class="action-card">
      <view class="action-copy">
        <text class="action-title">{{ actionTitle }}</text>
        <text class="action-desc">{{ actionDesc }}</text>
      </view>
      <button class="primary-btn" @click="handlePrimaryAction">{{ primaryActionText }}</button>
      <button class="ghost-btn" @click="goAgreement">查看认证条款</button>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/modules/user.js'
import certificateTemplate from '@/static/certificates/artist-cert-template.png'

export default {
  data() {
    return {
      certificateTemplate,
      artistCertStatus: null,
      routeStatus: '',
      routeRejectReason: '',
      heroHighlights: [
        { label: '作品绑定', value: '长期关联' },
        { label: '流通收益', value: '持续获得' },
        { label: '艺术家评分', value: '成交累积' }
      ],
      benefits: [
        {
          icon: '绑',
          title: '艺术家与作品强绑定',
          desc: '艺术家完成认证并发布作品后，作品将在平台内与艺术家身份长期绑定，后续展示、流通与成交都将持续追溯到作者本人。'
        },
        {
          icon: '证',
          title: '每次成交都可参与认证',
          desc: '当绑定作品在平台发生再次流通或成交时，艺术家可继续参与作品认证与确权，强化作品来源可信度。'
        },
        {
          icon: '益',
          title: '作品流通持续带来收益',
          desc: '作品每一次有效成交，都可能为艺术家带来认证服务收益，让作品在首发成交之外继续创造长期收入。'
        },
        {
          icon: '分',
          title: '成交越多，评分越高',
          desc: '艺术家在平台的累计成交金额、作品流通表现和成交稳定度，会持续沉淀为评分资产，提升平台内的信任等级与展示价值。'
        }
      ],
      mechanisms: [
        {
          badge: '01',
          title: '作品终身绑定艺术家',
          desc: '作品完成平台确权后，将与艺术家身份建立长期关联，后续流通链路中始终保留作者归属。'
        },
        {
          badge: '02',
          title: '每次成交可获得认证收益',
          desc: '作品在平台再次成交时，艺术家可通过认证与确权参与流通服务，并按平台规则获得约 2% - 5% 的认证服务收益。'
        },
        {
          badge: '03',
          title: '累计成交沉淀艺术家评分',
          desc: '作品成交金额、流通活跃度和成交稳定性会持续计入艺术家平台成绩，成交越大、评分越高。'
        }
      ],
      showcases: [
        {
          title: '作品详情与流通记录',
          desc: '作品展示页、流通页和成交记录中，平台会持续保留艺术家信息，让作品在每次交易中都与作者身份和收益关系对应。'
        },
        {
          title: '艺术家个人主页',
          desc: '认证通过后，艺术家主页不仅展示个人介绍与代表作，也承载作品成交、流通与累计收益表现，形成长期个人资产。'
        },
        {
          title: '平台评分与身份体系',
          desc: '成交越多、流通越活跃、作品表现越稳定，艺术家在平台的评分越高，后续在展示、筛选和合作中的价值也越高。'
        }
      ],
      requirements: [
        {
          title: '真实身份信息',
          desc: '提交真实姓名、身份证信息与本人证件照片，并完成必要的人脸核验。'
        },
        {
          title: '代表作品样本',
          desc: '提供 20 件本人代表作品，平台会从风格稳定性、完成度与原创性维度进行综合判断。'
        },
        {
          title: '艺术领域与简介',
          desc: '清晰填写擅长方向、个人介绍与创作背景，方便平台理解你的创作路径与对外展示方式。'
        },
        {
          title: '持续经营规范',
          desc: '通过认证后，发布、售卖、展示与传播行为都需遵守平台规则与交易要求。'
        }
      ],
      notes: [
        '认证后，平台中的作品将与艺术家身份建立长期关联，用于展示、流通和后续成交追溯。',
        '作品在平台每一次有效成交，都可能成为艺术家继续参与认证并获得服务收益的基础。',
        '认证服务收益可按平台规则计算，参考区间可为单笔成交金额的 2% - 5%，具体以平台正式规则为准。',
        '艺术家累计成交金额越大、流通表现越稳定，其平台评分越高，对应的信任度和展示价值也会持续提升。'
      ]
    }
  },
  computed: {
    userStore() {
      return useUserStore()
    },
    certificateOwner() {
      return this.userStore.userInfo?.nickname || '认证艺术家'
    },
    certificateNo() {
      const userId = this.userStore.userInfo?.id || this.userStore.userInfo?.userId || '0000'
      return `YB-ART-${String(userId).padStart(4, '0')}`
    },
    certificateDate() {
      const now = new Date()
      const year = now.getFullYear()
      const month = String(now.getMonth() + 1).padStart(2, '0')
      const day = String(now.getDate()).padStart(2, '0')
      return `${year}.${month}.${day}`
    },
    normalizedStatus() {
      const raw = this.routeStatus || this.artistCertStatus?.status
      if (!raw) return this.userStore.isArtist ? 'approved' : 'none'
      const status = String(raw).toLowerCase()
      if (['approved', 'success', 'passed'].includes(status)) return 'approved'
      if (['pending', 'reviewing', 'processing'].includes(status)) return 'pending'
      if (['rejected', 'failed', 'refused'].includes(status)) return 'rejected'
      return this.userStore.isArtist ? 'approved' : 'none'
    },
    statusLabel() {
      const map = {
        approved: '已认证',
        pending: '审核中',
        rejected: '未通过',
        none: '未申请'
      }
      return map[this.normalizedStatus] || '未申请'
    },
    statusTone() {
      return this.normalizedStatus
    },
    actionTitle() {
      const map = {
        approved: '你的作品已开始沉淀长期价值',
        pending: '你的艺术家身份正在审核中',
        rejected: '本次认证未通过，可重新完善资料',
        none: '加入平台后，让作品与身份长期绑定'
      }
      return map[this.normalizedStatus] || '准备好后即可发起艺术家认证'
    },
    actionDesc() {
      const rejectReason = this.routeRejectReason || this.artistCertStatus?.rejectReason || this.artistCertStatus?.reason || ''
      if (this.normalizedStatus === 'approved') return '你已拥有作品绑定、流通认证、持续收益与评分累积等平台能力。'
      if (this.normalizedStatus === 'pending') return '认证资料已提交，请在认证页面查看审核进度与状态更新。'
      if (this.normalizedStatus === 'rejected') {
        return rejectReason ? `未通过原因：${rejectReason}` : '请根据审核意见补充资料后再次提交认证申请。'
      }
      return '完成认证后，作品将与艺术家身份长期绑定，并在后续流通与成交中持续沉淀收益和评分。'
    },
    primaryActionText() {
      const map = {
        approved: '查看认证信息',
        pending: '查看审核进度',
        rejected: '重新认证',
        none: '去认证'
      }
      return map[this.normalizedStatus] || '去认证'
    }
  },
  onLoad(options = {}) {
    this.routeStatus = options.status || ''
    this.routeRejectReason = options.rejectReason ? decodeURIComponent(options.rejectReason) : ''
  },
  methods: {
    handlePrimaryAction() {
      uni.navigateTo({ url: '/pages/artist/cert' })
    },
    goAgreement() {
      uni.navigateTo({ url: '/pages/user-extra/agreement?type=artist_cert' })
    }
  }
}
</script>

<style lang="scss" scoped>
.artist-identity-page {
  position: relative;
  min-height: 100vh;
  padding: 28rpx 24rpx 48rpx;
  background:
    radial-gradient(circle at 12% 0%, rgba(223, 175, 58, 0.18), transparent 28%),
    radial-gradient(circle at 100% 12%, rgba(84, 126, 108, 0.12), transparent 24%),
    linear-gradient(180deg, #0c0c0d 0%, #080808 100%);
  overflow: hidden;
}

.page-glow {
  position: absolute;
  width: 360rpx;
  height: 360rpx;
  border-radius: 50%;
  filter: blur(90rpx);
  opacity: 0.45;
  pointer-events: none;
}

.page-glow-left {
  top: -120rpx;
  left: -120rpx;
  background: rgba(225, 179, 54, 0.18);
}

.page-glow-right {
  top: 180rpx;
  right: -120rpx;
  background: rgba(78, 139, 113, 0.14);
}

.hero-card,
.section-card,
.action-card,
.certificate-preview-card {
  position: relative;
  z-index: 1;
  border-radius: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.08);
  background: rgba(19, 19, 20, 0.92);
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.24);
}

.certificate-preview-card {
  padding: 22rpx 22rpx 26rpx;
  background:
    linear-gradient(135deg, rgba(215, 167, 52, 0.12), rgba(215, 167, 52, 0.03) 42%),
    linear-gradient(180deg, rgba(20, 20, 20, 0.95), rgba(12, 12, 13, 0.98));
}

.certificate-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.certificate-preview-tag {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(245, 239, 229, 0.66);
  font-size: 22rpx;
}

.certificate-stage {
  overflow: hidden;
  border-radius: 22rpx;
  background: linear-gradient(180deg, #f4f1ea 0%, #ece7dc 100%);
}

.certificate-sheet {
  position: relative;
  aspect-ratio: 1.31;
  overflow: hidden;
}

.certificate-frame {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.certificate-mask {
  position: absolute;
  top: 14%;
  left: 14%;
  right: 14%;
  bottom: 14%;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.94);
}

.certificate-watermark {
  position: absolute;
  top: 24%;
  left: 50%;
  width: 52%;
  height: 40%;
  transform: translateX(-50%);
  background:
    radial-gradient(circle at center, rgba(53, 72, 116, 0.09), rgba(53, 72, 116, 0.02) 55%, transparent 70%);
  filter: blur(2rpx);
}

.certificate-inner {
  position: relative;
  z-index: 1;
  height: 100%;
  padding: 78rpx 88rpx 72rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  box-sizing: border-box;
}

.certificate-cn-title {
  color: #cd5650;
  font-size: 42rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}

.certificate-en-title {
  margin-top: 6rpx;
  color: rgba(205, 86, 80, 0.92);
  font-size: 16rpx;
  letter-spacing: 2rpx;
}

.certificate-owner {
  margin-top: 42rpx;
  color: #273454;
  font-size: 30rpx;
  font-weight: 700;
}

.certificate-body {
  margin-top: 22rpx;
  color: #38425b;
  font-size: 20rpx;
  line-height: 1.75;
}

.certificate-meta {
  width: 100%;
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  color: #48516b;
  font-size: 18rpx;
}

.certificate-footer {
  width: 100%;
  margin-top: auto;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
}

.certificate-sign-block {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  align-items: flex-start;
}

.certificate-sign-block.align-right {
  align-items: flex-end;
}

.certificate-sign-label {
  color: #59627b;
  font-size: 16rpx;
}

.certificate-sign-name,
.certificate-sign-status {
  color: #c6514f;
  font-size: 24rpx;
  font-weight: 700;
}

.hero-card {
  margin-top: 22rpx;
  padding: 34rpx 30rpx 30rpx;
  background:
    linear-gradient(135deg, rgba(216, 168, 56, 0.16), rgba(216, 168, 56, 0.02) 38%),
    linear-gradient(180deg, rgba(20, 20, 20, 0.94), rgba(13, 13, 14, 0.98));
}

.hero-kicker,
.section-kicker {
  font-size: 22rpx;
  letter-spacing: 2rpx;
  color: rgba(246, 224, 174, 0.74);
}

.hero-title-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 18rpx;
  flex-wrap: wrap;
}

.hero-title {
  font-size: 54rpx;
  font-weight: 700;
  color: #f8f3e9;
  line-height: 1.12;
}

.status-pill {
  min-width: 116rpx;
  height: 48rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 600;
}

.status-pill.approved {
  background: rgba(84, 190, 128, 0.14);
  color: #71df9c;
}

.status-pill.pending {
  background: rgba(212, 175, 55, 0.14);
  color: #e4bf58;
}

.status-pill.rejected {
  background: rgba(220, 118, 118, 0.14);
  color: #f19999;
}

.status-pill.none {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.76);
}

.hero-desc {
  margin-top: 18rpx;
  font-size: 27rpx;
  line-height: 1.72;
  color: rgba(244, 238, 226, 0.8);
}

.hero-highlight {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18rpx;
  margin-top: 28rpx;
}

.highlight-item {
  padding: 20rpx 18rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.04);
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.highlight-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.5);
}

.highlight-value {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  color: #f6f1e7;
  font-weight: 600;
}

.section-card,
.action-card {
  margin-top: 22rpx;
  padding: 28rpx;
}

.mechanism-card {
  background:
    linear-gradient(135deg, rgba(215, 167, 52, 0.1), rgba(215, 167, 52, 0.02) 38%),
    linear-gradient(180deg, rgba(21, 21, 22, 0.96), rgba(13, 13, 14, 0.98));
}

.section-head {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.section-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #f7f2e8;
  line-height: 1.24;
}

.benefit-list,
.showcase-list,
.requirement-list,
.note-list {
  margin-top: 22rpx;
}

.mechanism-grid {
  margin-top: 22rpx;
  display: grid;
  gap: 16rpx;
}

.mechanism-item {
  padding: 24rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.04);
  border: 1rpx solid rgba(255, 255, 255, 0.06);
}

.mechanism-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72rpx;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(224, 182, 58, 0.14);
  color: #e7c05c;
  font-size: 22rpx;
  font-weight: 700;
}

.mechanism-title {
  display: block;
  margin-top: 14rpx;
  color: #f7f1e6;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.36;
}

.mechanism-desc {
  display: block;
  margin-top: 10rpx;
  color: rgba(240, 235, 225, 0.72);
  font-size: 25rpx;
  line-height: 1.72;
}

.benefit-item {
  display: flex;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.06);
}

.benefit-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.benefit-icon {
  width: 70rpx;
  height: 70rpx;
  flex: 0 0 70rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(222, 182, 81, 0.2), rgba(222, 182, 81, 0.04));
  color: #e8c76d;
  font-size: 28rpx;
  font-weight: 700;
}

.benefit-copy,
.requirement-copy {
  flex: 1;
  min-width: 0;
}

.benefit-title,
.showcase-title,
.requirement-title,
.action-title,
.note-fee-title {
  display: block;
  color: #f6f1e6;
  font-size: 30rpx;
  font-weight: 600;
  line-height: 1.35;
}

.benefit-desc,
.showcase-desc,
.requirement-desc,
.action-desc,
.note-fee-desc,
.note-item {
  display: block;
  margin-top: 10rpx;
  color: rgba(240, 235, 225, 0.68);
  font-size: 25rpx;
  line-height: 1.72;
}

.showcase-item {
  padding: 24rpx;
  border-radius: 24rpx;
  background:
    linear-gradient(135deg, rgba(215, 167, 52, 0.09), rgba(215, 167, 52, 0.02) 55%),
    rgba(255, 255, 255, 0.03);
  border: 1rpx solid rgba(255, 255, 255, 0.06);
  margin-top: 16rpx;
}

.showcase-item:first-child {
  margin-top: 0;
}

.requirement-item {
  display: flex;
  gap: 18rpx;
  margin-top: 18rpx;
}

.requirement-item:first-child {
  margin-top: 0;
}

.requirement-index {
  width: 72rpx;
  flex: 0 0 72rpx;
  font-size: 34rpx;
  line-height: 1;
  font-weight: 700;
  color: rgba(231, 192, 93, 0.9);
}

.notes-card {
  background:
    linear-gradient(180deg, rgba(20, 20, 21, 0.96), rgba(13, 13, 14, 0.96)),
    rgba(19, 19, 20, 0.92);
}

.note-fee {
  margin-top: 22rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.06);
}

.action-card {
  margin-bottom: calc(36rpx + env(safe-area-inset-bottom));
  background:
    linear-gradient(135deg, rgba(84, 190, 128, 0.08), rgba(84, 190, 128, 0.01) 34%),
    linear-gradient(180deg, rgba(18, 18, 18, 0.96), rgba(12, 12, 12, 0.98));
}

.action-copy {
  margin-bottom: 22rpx;
}

.primary-btn,
.ghost-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 600;
}

.primary-btn {
  background: linear-gradient(135deg, #e0b63a, #c9971f);
  color: #1f1605;
}

.primary-btn::after,
.ghost-btn::after {
  border: 0;
}

.ghost-btn {
  margin-top: 16rpx;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(246, 241, 230, 0.88);
}
</style>
