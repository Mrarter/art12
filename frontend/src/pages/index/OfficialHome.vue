<template>
  <scroll-view class="official-page" scroll-y>
    <view class="site-nav wrap">
      <image class="nav-logo" src="/static/landing/yiben-logo.jpg" mode="widthFix" />
      <view class="nav-links">
        <text @click="scrollToSection('value')">平台机制</text>
        <text @click="scrollToSection('journey')">作品流动</text>
        <text @click="goAbout">关于我们</text>
      </view>
      <view class="nav-actions">
        <view class="nav-button nav-button-gold" @click="goAppHome">进入 App</view>
        <view class="nav-button nav-button-line" @click="goAppLogin">艺术家入驻</view>
      </view>
    </view>

    <view class="hero wrap">
      <view class="hero-copy">
        <text class="eyebrow gold">YIBEN ART · LONG-TERM VALUE</text>
        <text class="hero-title">让创作<text>永远有价值</text></text>
        <text class="hero-description">
          艺术品不止完成一次交易。它在每一次认证、传播与再次流通中，持续建立新的价值关系。
        </text>
        <view class="hero-actions">
          <view class="button button-gold" @click="scrollToSection('value')">了解作品如何流动</view>
          <view class="button button-quiet" @click="goAppLogin">成为合作经纪人</view>
        </view>
      </view>

      <view class="hero-art">
        <view class="art-glow" />
        <image
          class="hero-portrait"
          src="/static/landing/hd-artworks/hero-portrait.jpg"
          mode="aspectFill"
        />
        <image
          class="hero-still"
          src="/static/landing/hd-artworks/hero-still.jpg"
          mode="aspectFill"
        />
        <view class="art-note">
          <text class="art-note-label">作品 NO. 0721</text>
          <text class="art-note-copy">每一次流通，都是价值的增长。</text>
        </view>
      </view>
    </view>

    <view id="value" class="revenue-section">
      <view class="wrap">
        <view class="section-heading revenue-heading">
          <view>
            <text class="eyebrow gold">VALUE DISTRIBUTION · 价值分配机制</text>
            <text class="section-title large">作品每一次流动，<br />都让价值再次发生。</text>
          </view>
          <text class="section-copy heading-copy">从首次收藏到再次认证，艺术家始终参与作品的长期价值。</text>
        </view>

        <view class="revenue-layout">
          <view class="artist-visual">
            <image
              class="artist-image"
              src="/static/landing/hd-artworks/artist-burgundy-hd.png"
              mode="aspectFill"
              lazy-load
            />
          </view>

          <view class="revenue-cards">
            <view class="revenue-card artist-card">
              <view class="card-topline">
                <text class="card-role">艺本艺术 / 艺术家收益</text>
              </view>
              <text class="card-title">创作完成后，<br />收益仍会继续。</text>
              <view class="artist-income-grid">
                <view>
                  <text class="income-index">01 首次成交</text>
                  <text class="income-title">获得作品首次销售收入</text>
                </view>
                <view>
                  <text class="income-index">02 再次流通</text>
                  <text class="income-title">真迹认证</text>
                </view>
              </view>
              <text class="artist-card-foot">作品再次交易并完成真伪认证时，艺术家可继续获得服务回报。</text>
            </view>

            <view class="secondary-revenue-grid">
              <view class="revenue-card broker-card">
                <text class="card-role">BROKER / 经纪人</text>
                <text class="secondary-card-title">人人都是经纪人</text>
                <text class="income-copy">人人都是艺术家的经纪人。促成成交，获得经纪人佣金。</text>
              </view>
              <view class="revenue-card collector-card">
                <text class="card-role">COLLECTOR / 收藏家</text>
                <text class="secondary-card-title">找到更有价值的心头好</text>
                <text class="income-copy">藏品更具认证后的再次流通路径，拥有持续被看见的可能。</text>
              </view>
            </view>
          </view>
        </view>

        <view class="circulation-track">
          <view v-for="(step, index) in circulationTrack" :key="step" class="track-step">
            <view class="track-dot" />
            <text>{{ step }}</text>
          </view>
        </view>
        <text class="disclaimer">“永远有价值”指持续记录、认证与流通机会，不构成作品固定收益承诺。</text>
      </view>
    </view>

    <view id="journey" class="journey-section wrap">
      <view class="section-heading">
        <view>
          <text class="eyebrow gold">作品流动</text>
          <text class="section-title large">作品的长期旅程</text>
        </view>
        <text class="section-copy heading-copy">六个节点组成一个可以不断循环的价值闭环。</text>
      </view>
      <view class="journey-grid">
        <view v-for="(step, index) in journey" :key="step.title" class="journey-step">
          <view class="journey-number">0{{ index + 1 }}</view>
          <view>
            <text class="journey-title">{{ step.title }}</text>
            <text class="journey-copy">{{ step.copy }}</text>
          </view>
        </view>
      </view>
      <view class="journey-return">
        <text>每一次新交易，都继续更新作品的认证、价格与成交记录。</text>
        <view class="journey-button" @click="goAppHome">再次回到市场</view>
      </view>
    </view>

    <view class="pricing-section wrap">
      <image
        class="pricing-image"
        src="/static/landing/hd-artworks/boat-sunset-hd.png"
        mode="aspectFill"
        lazy-load
      />
      <view class="pricing-copy">
        <text class="eyebrow gold">动态价格参考</text>
        <text class="section-title large">价格不是静止标签，<br />而是艺术家发展轨迹的表达。</text>
        <view class="factor-list">
          <view v-for="(factor, index) in factors" :key="factor.title" class="factor-row">
            <text class="factor-index">0{{ index + 1 }}</text>
            <text class="factor-title">{{ factor.title }}</text>
            <text class="factor-copy">{{ factor.copy }}</text>
          </view>
        </view>
        <view class="text-link" @click="goAppHome">查看价格机制 →</view>
      </view>
    </view>

    <view id="works" class="works-section wrap">
      <view class="section-heading">
        <view>
          <text class="eyebrow gold">精选作品</text>
          <text class="section-title large">正在被看见的作品</text>
        </view>
        <text class="section-copy heading-copy">展示、收藏与再次流通，在同一个作品档案中持续发生。</text>
      </view>
      <view class="works-grid">
        <view v-for="work in works" :key="work.title" class="work-card">
          <view class="work-image-wrap">
            <image class="work-image" :src="work.src" mode="aspectFill" lazy-load />
            <text class="work-status">持续估值</text>
          </view>
          <text class="work-title">{{ work.title }}</text>
          <text class="work-meta">{{ work.meta }}</text>
        </view>
      </view>
    </view>

    <view class="cta-section wrap">
      <view class="cta-card">
        <view>
          <text class="cta-title">让你的创作，进入长期价值旅程。</text>
          <text class="cta-copy">艺术家入驻 · 经纪人合作 · 收藏家进场</text>
        </view>
        <view class="button button-dark" @click="goAppHome">进入艺本艺术</view>
      </view>
    </view>

    <view class="footer">
      <view class="wrap footer-inner">
        <text class="footer-message">艺本艺术 / 让创作永远有价值</text>
        <view class="footer-record">
          <text class="footer-company">哒纷奇艺术（杭州）有限公司</text>
          <text>ICP备案号</text>
          <text class="footer-link" @click="openIcpRecord">浙ICP备2026041466号-1</text>
          <text>|</text>
          <text class="footer-link" @click="openIcpRecord">浙ICP备2024083266号-3</text>
        </view>
      </view>
    </view>
  </scroll-view>
</template>

<script setup>
import { onMounted } from 'vue'
import { onShareAppMessage, onShareTimeline, onShow } from '@dcloudio/uni-app'

const circulationTrack = ['首次成交', '经纪人分销', '藏家持有', '再次认证']

const journey = [
  { title: '创作与确权', copy: '建立艺术家与作品的原始身份档案。' },
  { title: '展示与分销', copy: '经纪人参与传播，帮助作品连接买家。' },
  { title: '首次成交', copy: '艺术家获得作品首次销售收入。' },
  { title: '动态估值', copy: '多维数据持续更新市场参考价格。' },
  { title: '再次流通', copy: '收藏家重新上架持有的艺术作品。' },
  { title: '认证与收益', copy: '艺术家认证真伪并获得认证服务费。' }
]

const factors = [
  { title: '创作年限', copy: '长期创作积累' },
  { title: '互联网热度', copy: '公开关注与传播' },
  { title: '身份变化', copy: '展览、荣誉与职业经历' },
  { title: '市场记录', copy: '成交及再次流通数据' }
]

const works = [
  {
    title: '人物研究 No.07',
    meta: '动态参考价 ¥28,600',
    src: '/static/landing/hd-artworks/girl-portrait-hd.png'
  },
  {
    title: '渡口之后',
    meta: '动态参考价 ¥36,200',
    src: '/static/landing/hd-artworks/boat-sunset-hd.png'
  },
  {
    title: '静物与橙',
    meta: '动态参考价 ¥18,900',
    src: '/static/landing/hd-artworks/orange-still-hd.png'
  }
]

const APP_WEB_ORIGIN = 'https://a.art1.cn'
const homeShareTitle = '艺本艺术｜让创作永远有价值'

const openAppUrl = (path = '') => {
  // #ifdef H5
  window.location.href = `${APP_WEB_ORIGIN}${path}`
  return
  // #endif
  uni.switchTab({ url: '/pages/gallery/index' })
}

const goAppHome = () => openAppUrl('')

const goAppLogin = () => {
  // #ifdef H5
  openAppUrl('/#/pages/login/index')
  return
  // #endif
  uni.navigateTo({ url: '/pages/login/index' })
}

const goAbout = () => {
  // #ifdef H5
  window.location.href = `${APP_WEB_ORIGIN}/#/pages/about/index`
  return
  // #endif
  uni.navigateTo({ url: '/pages/about/index' })
}

const scrollToSection = (id) => {
  // #ifdef H5
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  // #endif
}

const openIcpRecord = () => {
  // #ifdef H5
  window.open('https://beian.miit.gov.cn/#/Integrated/index', '_blank', 'noopener,noreferrer')
  return
  // #endif
  uni.setClipboardData({ data: 'https://beian.miit.gov.cn/#/Integrated/index' })
}

const hideOfficialTabBar = () => {
  if (typeof uni?.hideTabBar === 'function') uni.hideTabBar({ animation: false })
}

onMounted(hideOfficialTabBar)
onShow(hideOfficialTabBar)

onShareAppMessage(() => ({ title: homeShareTitle, path: '/pages/index/index?from=share' }))
onShareTimeline(() => ({ title: homeShareTitle, query: 'from=share' }))
</script>

<style scoped lang="scss">
.official-page {
  --ink: #0a0908;
  --paper: #f1ede4;
  --gold: #d9a92e;
  --muted: rgba(246, 240, 229, 0.62);
  width: 100%;
  height: 100vh;
  color: #f6f0e5;
  background:
    radial-gradient(circle at 76% 9%, rgba(218, 171, 49, 0.1), transparent 22%),
    #090806;
  font-family: "Noto Serif SC", "Songti SC", "STSong", serif;
}

.wrap {
  width: min(1180px, calc(100% - 64px));
  margin: 0 auto;
  box-sizing: border-box;
}

.site-nav {
  min-height: 88px;
  display: flex;
  align-items: center;
  gap: 36px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}

.nav-logo {
  width: 148px;
  background: #fff;
}

.nav-links {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 30px;
  color: rgba(255, 255, 255, 0.7);
  font-family: "PingFang SC", sans-serif;
  font-size: 14px;
}

.nav-links text,
.footer-link,
.text-link {
  cursor: pointer;
}

.nav-actions,
.hero-actions {
  display: flex;
  gap: 12px;
}

.nav-button,
.button {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border-radius: 999px;
  cursor: pointer;
  font-family: "PingFang SC", sans-serif;
  font-weight: 600;
}

.nav-button {
  height: 38px;
  padding: 0 20px;
  font-size: 13px;
}

.nav-button-gold,
.button-gold {
  color: #181208;
  background: linear-gradient(135deg, #edc858, #c8931e);
}

.nav-button-line,
.button-quiet {
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.03);
}

.hero {
  min-height: 620px;
  padding: 58px 0 68px;
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  align-items: center;
  gap: 80px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.eyebrow {
  display: block;
  margin-bottom: 22px;
  color: rgba(255, 255, 255, 0.42);
  font-family: "Helvetica Neue", sans-serif;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.eyebrow.gold {
  color: #d8ad46;
}

.hero-title {
  display: flex;
  flex-direction: column;
  font-size: clamp(54px, 5.3vw, 78px);
  line-height: 1.03;
  font-weight: 400;
  letter-spacing: -0.035em;
}

.hero-description,
.section-copy,
.income-copy,
.journey-copy,
.cta-copy {
  font-family: "PingFang SC", sans-serif;
}

.hero-description {
  max-width: 510px;
  margin-top: 28px;
  color: var(--muted);
  font-size: 16px;
  line-height: 1.9;
}

.hero-actions {
  margin-top: 34px;
}

.button {
  min-width: 150px;
  height: 46px;
  padding: 0 24px;
  font-size: 14px;
}

.hero-art {
  position: relative;
  height: 540px;
}

.art-glow {
  position: absolute;
  width: 410px;
  height: 410px;
  top: 58px;
  left: 50px;
  border-radius: 50%;
  background: rgba(210, 150, 30, 0.18);
  filter: blur(80px);
}

.hero-portrait {
  position: absolute;
  top: 0;
  left: 12%;
  width: 58%;
  height: 500px;
  border: 1px solid rgba(233, 185, 67, 0.62);
  border-radius: 180px 180px 18px 18px;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.48);
}

.hero-still {
  position: absolute;
  top: 94px;
  right: 0;
  width: 31%;
  height: 220px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 14px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.42);
}

.art-note {
  position: absolute;
  right: 6%;
  bottom: 24px;
  width: 280px;
  padding: 22px;
  box-sizing: border-box;
  border: 1px solid rgba(218, 169, 46, 0.25);
  border-radius: 10px;
  background: rgba(19, 16, 12, 0.92);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(14px);
}

.art-note-label,
.art-note-copy {
  display: block;
  font-family: "PingFang SC", sans-serif;
}

.art-note-label {
  margin-bottom: 8px;
  color: #d4a43b;
  font-size: 10px;
}

.art-note-copy {
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.intro {
  padding: 72px 0 66px;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
}

.section-title {
  display: block;
  font-size: 32px;
  line-height: 1.35;
  font-weight: 400;
}

.section-title.large {
  font-size: clamp(34px, 4vw, 52px);
}

.section-copy {
  display: block;
  max-width: 650px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.8;
}

.revenue-section {
  padding: 78px 0 92px;
  color: #efe9df;
  background:
    radial-gradient(circle at 15% 28%, rgba(200, 143, 32, 0.14), transparent 29%),
    linear-gradient(180deg, #15120e, #0c0b09);
}

.section-heading {
  margin-bottom: 50px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 50px;
}

.heading-copy {
  max-width: 420px;
  margin: 0 0 4px;
}

.revenue-layout {
  display: grid;
  grid-template-columns: 0.7fr 1.3fr;
  gap: 22px;
}

.artist-visual {
  position: relative;
  min-height: 610px;
  overflow: hidden;
  border: 1px solid rgba(227, 179, 62, 0.2);
  border-radius: 14px;
  background: #20170c;
}

.artist-image {
  width: 100%;
  height: 100%;
  filter: saturate(0.92) contrast(1.04);
}

.revenue-cards {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.revenue-card {
  padding: 30px;
  box-sizing: border-box;
  border-radius: 4px;
}

.artist-card {
  flex: 1.25;
  border: 1px solid #c89625;
  color: #241a09;
  background:
    radial-gradient(circle at 85% 25%, rgba(255, 227, 141, 0.42), transparent 28%),
    linear-gradient(135deg, #e5c26c, #bb841b);
  box-shadow: 0 20px 60px rgba(144, 94, 10, 0.16);
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-role,
.card-title,
.income-index,
.income-title,
.income-copy {
  display: block;
}

.card-role {
  font-size: 17px;
  font-weight: 700;
}

.card-title {
  margin-top: 18px;
  font-size: 34px;
  line-height: 1.28;
}

.artist-income-grid {
  margin-top: 28px;
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  border-top: 0;
}

.artist-income-grid > view {
  padding: 24px 24px 0 0;
}

.artist-income-grid > view + view {
  padding-left: 28px;
  border-left: 1px solid rgba(39, 27, 6, 0.18);
}

.income-index {
  margin-bottom: 10px;
  font-family: "PingFang SC", sans-serif;
  font-size: 10px;
  letter-spacing: 0.08em;
  opacity: 0.62;
}

.income-title {
  font-size: 18px;
}

.income-copy {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  opacity: 0.7;
}

.secondary-revenue-grid {
  flex: 0.75;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.broker-card {
  color: #d7e3ee;
  border: 1px solid #315178;
  background: #132941;
}

.collector-card {
  color: #d7eadc;
  border: 1px solid #356546;
  background: #16331f;
}

.broker-card .card-role,
.collector-card .card-role {
  margin-bottom: 24px;
}

.artist-card-foot {
  display: block;
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid rgba(39, 27, 6, 0.18);
  font-family: "PingFang SC", sans-serif;
  font-size: 12px;
  opacity: 0.72;
}

.secondary-card-title {
  display: block;
  margin-bottom: 12px;
  font-size: 27px;
  line-height: 1.25;
}

.circulation-track {
  margin-top: 38px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid rgba(213, 166, 48, 0.45);
}

.track-step {
  position: relative;
  padding: 17px 10px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.72);
  font-family: "PingFang SC", sans-serif;
  font-size: 12px;
}

.track-dot {
  position: absolute;
  top: -5px;
  left: 50%;
  width: 9px;
  height: 9px;
  margin-left: -4px;
  border-radius: 50%;
  background: #d5a630;
}

.disclaimer {
  display: block;
  margin-top: 16px;
  color: rgba(255, 255, 255, 0.35);
  font-family: "PingFang SC", sans-serif;
  font-size: 10px;
  text-align: right;
}

.journey-section,
.works-section {
  padding-top: 110px;
  padding-bottom: 110px;
}

.journey-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.journey-step {
  min-height: 140px;
  padding: 18px;
  box-sizing: border-box;
  border: 1px solid rgba(219, 170, 48, 0.18);
  background: linear-gradient(145deg, rgba(219, 170, 48, 0.07), rgba(255, 255, 255, 0.018));
}

.journey-number {
  margin-bottom: 28px;
  color: #d4a42e;
  font-family: "Helvetica Neue", sans-serif;
  font-size: 10px;
}

.journey-title,
.journey-copy {
  display: block;
}

.journey-title {
  font-size: 16px;
}

.journey-copy {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.42);
  font-size: 11px;
  line-height: 1.6;
}

.journey-return {
  margin-top: 18px;
  padding: 16px;
  color: #d6ad4c;
  border-bottom: 1px solid rgba(218, 170, 45, 0.22);
  font-family: "PingFang SC", sans-serif;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.journey-button {
  min-width: 190px;
  padding: 12px 24px;
  border-radius: 999px;
  color: #1b1305;
  background: #d8a72c;
  text-align: center;
}

.pricing-section {
  min-height: 590px;
  padding: 42px;
  display: grid;
  grid-template-columns: 0.86fr 1.14fr;
  gap: 54px;
  align-items: stretch;
  border: 1px solid rgba(218, 171, 49, 0.2);
  background: #14120e;
}

.pricing-image {
  width: 100%;
  height: 100%;
  min-height: 500px;
}

.pricing-copy {
  padding: 36px 30px 20px 0;
}

.factor-list {
  margin-top: 38px;
}

.factor-row {
  padding: 14px 0;
  display: grid;
  grid-template-columns: 38px 1fr 1.1fr;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  font-family: "PingFang SC", sans-serif;
  font-size: 13px;
}

.factor-index {
  color: #d6a72d;
  font-size: 10px;
}

.factor-copy {
  color: rgba(255, 255, 255, 0.42);
}

.text-link {
  display: inline-block;
  margin-top: 26px;
  padding-bottom: 5px;
  color: #e0b33f;
  border-bottom: 1px solid rgba(224, 179, 63, 0.5);
  font-family: "PingFang SC", sans-serif;
  font-size: 12px;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.work-image-wrap {
  position: relative;
  height: 430px;
  overflow: hidden;
  background: #17130e;
}

.work-image {
  width: 100%;
  height: 100%;
}

.work-status {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 7px 10px;
  color: #1a1205;
  border-radius: 999px;
  background: #d9ac3b;
  font-family: "PingFang SC", sans-serif;
  font-size: 10px;
}

.work-title,
.work-meta {
  display: block;
}

.work-title {
  margin-top: 16px;
  font-size: 18px;
}

.work-meta {
  margin-top: 7px;
  color: #cfa337;
  font-family: "PingFang SC", sans-serif;
  font-size: 11px;
}

.cta-section {
  padding-bottom: 100px;
}

.cta-card {
  min-height: 250px;
  padding: 56px 48px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  color: #1e1609;
  text-align: left;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.35), transparent 24%),
    linear-gradient(135deg, #e4c466, #c79320);
}

.cta-title,
.cta-copy {
  display: block;
}

.cta-title {
  font-size: clamp(32px, 3.5vw, 46px);
}

.cta-copy {
  margin-top: 16px;
  font-size: 14px;
  opacity: 0.72;
}

.button-dark {
  min-width: 220px;
  color: #f4e5b9;
  background: #1a140a;
}

.button-light {
  border: 1px solid rgba(28, 20, 8, 0.35);
  background: rgba(255, 255, 255, 0.12);
}

.footer {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: #0e0c09;
}

.footer-inner {
  min-height: 108px;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 24px;
}

.footer-message {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.footer-record {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  color: rgba(255, 255, 255, 0.5);
  font-family: "PingFang SC", sans-serif;
  font-size: 12px;
}

.footer-company {
  color: rgba(255, 255, 255, 0.72);
}

.footer-link {
  color: #e5c36e;
  text-decoration: underline;
}

:global(.uni-tabbar),
:global(uni-tabbar),
:global(.uni-app--showtabbar uni-page-wrapper::after) {
  display: none !important;
}

@media (max-width: 900px) {
  .wrap {
    width: min(100% - 36px, 680px);
  }

  .site-nav {
    min-height: 70px;
  }

  .nav-logo {
    width: 120px;
  }

  .nav-links,
  .nav-button-line {
    display: none;
  }

  .nav-actions {
    margin-left: auto;
  }

  .hero {
    min-height: auto;
    padding: 70px 0 84px;
    grid-template-columns: 1fr;
    gap: 62px;
  }

  .hero-title {
    font-size: clamp(52px, 15vw, 80px);
  }

  .hero-art {
    height: 470px;
  }

  .hero-portrait {
    left: 4%;
    width: 69%;
    height: 430px;
  }

  .hero-still {
    top: 75px;
    width: 36%;
    height: 190px;
  }

  .art-note {
    right: 1%;
    bottom: 0;
    width: 245px;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .revenue-section,
  .journey-section,
  .works-section {
    padding-top: 78px;
    padding-bottom: 78px;
  }

  .revenue-layout,
  .pricing-section {
    grid-template-columns: 1fr;
  }

  .artist-visual {
    min-height: 500px;
  }

  .journey-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .pricing-section {
    width: min(100% - 36px, 680px);
    padding: 22px;
    gap: 20px;
  }

  .pricing-image {
    min-height: 460px;
  }

  .pricing-copy {
    padding: 28px 8px 20px;
  }

  .works-grid {
    gap: 12px;
  }

  .work-image-wrap {
    height: 300px;
  }

  .footer-inner {
    padding: 34px 0;
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }

  .footer-record {
    justify-content: center;
  }
}

@media (max-width: 560px) {
  .wrap {
    width: calc(100% - 28px);
  }

  .nav-button {
    height: 34px;
    padding: 0 16px;
  }

  .hero {
    padding-top: 54px;
  }

  .hero-description {
    font-size: 14px;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions .button {
    min-width: 0;
    flex: 1;
    padding: 0 12px;
    font-size: 12px;
  }

  .hero-art {
    height: 390px;
  }

  .hero-portrait {
    width: 74%;
    height: 355px;
    border-radius: 110px 110px 10px 10px;
  }

  .hero-still {
    top: 58px;
    height: 150px;
  }

  .art-note {
    width: 220px;
    padding: 16px;
  }

  .section-title {
    font-size: 27px;
  }

  .section-title.large {
    font-size: 34px;
  }

  .revenue-heading {
    margin-bottom: 34px;
  }

  .artist-visual {
    min-height: 420px;
    border-radius: 14px;
  }

  .artist-card,
  .revenue-card {
    padding: 22px;
  }

  .card-title {
    font-size: 21px;
  }

  .artist-income-grid,
  .secondary-revenue-grid {
    grid-template-columns: 1fr;
  }

  .artist-income-grid > view + view {
    margin-top: 20px;
    padding-left: 0;
    border-left: 0;
    border-top: 1px solid rgba(39, 27, 6, 0.18);
  }

  .circulation-track {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px 0;
  }

  .journey-return {
    align-items: flex-start;
    flex-direction: column;
  }

  .journey-button {
    width: 100%;
    box-sizing: border-box;
  }

  .journey-grid {
    grid-template-columns: 1fr;
  }

  .journey-step {
    min-height: auto;
    display: grid;
    grid-template-columns: 44px 1fr;
  }

  .journey-number {
    margin: 3px 0 0;
  }

  .pricing-section {
    width: calc(100% - 28px);
    padding: 14px;
  }

  .pricing-image {
    min-height: 390px;
  }

  .factor-row {
    grid-template-columns: 32px 1fr;
  }

  .factor-copy {
    display: none;
  }

  .works-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .work-image-wrap {
    height: 430px;
  }

  .cta-section {
    width: 100%;
    padding-bottom: 0;
  }

  .cta-card {
    padding: 64px 22px;
    align-items: flex-start;
    flex-direction: column;
  }

  .cta-title {
    font-size: 34px;
  }

  .footer-record {
    line-height: 1.8;
  }
}
</style>
