<template>
  <div class="price-rule-page">
    <div class="page-header">
      <div><h1>自动涨价规则</h1><p>配置艺术品价格随艺术家成长、作品热度、收藏记录与平台推荐动态上涨。</p></div>
      <div class="actions"><button class="ghost" @click="resetConfig">恢复默认</button><button class="primary" @click="saveConfig">保存规则</button></div>
    </div>
    <div class="stats">
      <div class="stat"><label>全局自动涨价</label><b>{{ config.globalEnabled ? '开启' : '关闭' }}</b></div>
      <div class="stat"><label>基础日涨幅</label><b>{{ percent(config.baseDailyIncreaseRate) }}</b></div>
      <div class="stat"><label>最低日涨幅</label><b>{{ percent(config.minDailyIncreaseRate) }}</b></div>
      <div class="stat"><label>最高日涨幅</label><b>{{ percent(config.maxDailyIncreaseRate) }}</b></div>
    </div>
    <div class="card">
      <h2>全局规则</h2>
      <div class="grid">
        <div class="field"><label>全局自动涨价</label><select v-model="config.globalEnabled"><option :value="true">开启</option><option :value="false">关闭</option></select></div>
        <div class="field"><label>允许艺术家手动定价</label><select v-model="config.allowArtistManualPrice"><option :value="true">允许</option><option :value="false">不允许</option></select></div>
        <div class="field"><label>允许平台人工干预</label><select v-model="config.allowPlatformManualAdjust"><option :value="true">允许</option><option :value="false">不允许</option></select></div>
      </div>
    </div>
    <div class="card">
      <h2>每日涨幅范围</h2>
      <div class="grid">
        <div class="field"><label>基础每日涨幅</label><input type="number" step="0.001" v-model.number="config.baseDailyIncreaseRate"/></div>
        <div class="field"><label>最低每日涨幅</label><input type="number" step="0.001" v-model.number="config.minDailyIncreaseRate"/></div>
        <div class="field"><label>最高每日涨幅</label><input type="number" step="0.01" v-model.number="config.maxDailyIncreaseRate"/></div>
      </div>
      <div class="formula">今日价格 = 昨日价格 × (1 + 今日涨幅)</div>
    </div>
    <div class="card">
      <h2>涨价因素权重</h2>
      <div v-for="item in weightItems" :key="item.key" class="weight">
        <div><b>{{ item.title }}</b><p>{{ item.desc }}</p></div>
        <div class="winput"><input type="number" step="0.001" v-model.number="config.weights[item.key]"/><span>{{ percent(config.weights[item.key]) }}</span></div>
      </div>
    </div>
    <div class="card">
      <h2>涨价模拟器</h2>
      <div class="sim">
        <div class="grid">
          <div class="field"><label>昨日价格</label><input type="number" v-model.number="sim.yesterdayPrice"/></div>
          <div class="field"><label>关注人数</label><input type="number" v-model.number="sim.watchCount"/></div>
          <div class="field"><label>粉丝数</label><input type="number" v-model.number="sim.fansCount"/></div>
          <div class="field"><label>浏览量</label><input type="number" v-model.number="sim.viewCount"/></div>
          <div class="field"><label>分享量</label><input type="number" v-model.number="sim.shareCount"/></div>
          <div class="field"><label>收藏记录</label><input type="number" v-model.number="sim.collectionCount"/></div>
        </div>
        <div class="result">
          <p>计算涨幅：{{ percent(rawRate) }}</p>
          <p>最终涨幅：<b>{{ percent(finalRate) }}</b></p>
          <p>今日价格：<b>{{ money(todayPrice) }}</b></p>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPriceRule, savePriceRule } from '@/api/priceRule'

const defaults={globalEnabled:true,allowArtistManualPrice:true,allowPlatformManualAdjust:true,baseDailyIncreaseRate:.001,minDailyIncreaseRate:.001,maxDailyIncreaseRate:.2,weights:{workOnlineDays:.0002,artistOnlineDays:.0001,watchCount:.0003,fansCount:.00005,viewCount:.00002,shareCount:.0002,collectionCount:.006,circulationCount:.004,artistScore:.0005,platformRecommended:.01}}
const config=reactive(JSON.parse(JSON.stringify(defaults)))
const weightItems=[{key:'workOnlineDays',title:'作品上线天数',desc:'作品上线时间越长，价格缓慢成长。'},{key:'artistOnlineDays',title:'艺术家上线天数',desc:'艺术家持续活跃加成。'},{key:'watchCount',title:'作品关注人数',desc:'潜在收藏意向越强。'},{key:'fansCount',title:'艺术家粉丝数',desc:'艺术家关注度更高。'},{key:'viewCount',title:'浏览量',desc:'作品曝光热度。'},{key:'shareCount',title:'分享量',desc:'传播能力。'},{key:'collectionCount',title:'收藏记录数',desc:'再次流通履历价值。'},{key:'circulationCount',title:'流通次数',desc:'完成多次流通后的履历价值。'},{key:'artistScore',title:'艺术家评分',desc:'评分越高成长系数越高。'},{key:'platformRecommended',title:'平台推荐',desc:'平台推荐额外涨幅。'}]
const sim=reactive({yesterdayPrice:10000,watchCount:80,fansCount:2000,viewCount:5000,shareCount:120,collectionCount:1})
const loading = ref(false)

onMounted(async () => {
  try {
    const data = await getPriceRule()
    if (data) {
      config.globalEnabled = data.enabled ?? config.globalEnabled
      config.baseDailyIncreaseRate = data.baseDailyRate ?? config.baseDailyIncreaseRate
      config.minDailyIncreaseRate = data.baseDailyRate ?? config.minDailyIncreaseRate
      config.maxDailyIncreaseRate = data.maxGrowthMultiple ?? config.maxDailyIncreaseRate
    }
  } catch (e) {
    console.warn('加载价格规则配置失败，使用默认值', e)
  }
})

const rawRate=computed(()=>config.baseDailyIncreaseRate+Math.min(sim.watchCount/1000,1)*config.weights.watchCount+Math.min(sim.fansCount/10000,1)*config.weights.fansCount+Math.min(sim.viewCount/100000,1)*config.weights.viewCount+Math.min(sim.shareCount/5000,1)*config.weights.shareCount+sim.collectionCount*config.weights.collectionCount)
const finalRate=computed(()=>Math.max(config.minDailyIncreaseRate,Math.min(rawRate.value,config.maxDailyIncreaseRate)))
const todayPrice=computed(()=>Math.round(sim.yesterdayPrice*(1+finalRate.value)))
function percent(v){return `${(Number(v)*100).toFixed(2)}%`} function money(v){return `¥${String(v).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`}
async function saveConfig(){
  try {
    await savePriceRule({
      enabled: config.globalEnabled,
      baseDailyRate: config.baseDailyIncreaseRate,
      minDailyIncreaseRate: config.minDailyIncreaseRate,
      maxDailyIncreaseRate: config.maxDailyIncreaseRate
    })
    ElMessage.success('规则已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}
function resetConfig(){
  Object.assign(config, JSON.parse(JSON.stringify(defaults)))
  ElMessage.info('已恢复默认值，点击保存生效')
}
</script>
<style scoped>
.price-rule-page{min-height:100vh;padding:24px;background:#f7f4ef;color:#1f1f1f}.page-header{display:flex;justify-content:space-between;margin-bottom:24px}.page-header h1{margin:0;font-size:28px}.page-header p{color:#8a8178}.actions{display:flex;gap:12px}.primary,.ghost{border:none;border-radius:999px;padding:10px 18px;cursor:pointer}.primary{background:#b3261e;color:#fff}.ghost{background:#f5f0e8}.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px}.stat,.card{background:#fff;border-radius:18px;box-shadow:0 8px 28px rgba(43,33,24,.05)}.stat{padding:22px}.stat label{color:#8a8178}.stat b{display:block;margin-top:8px;color:#b3261e;font-size:28px}.card{padding:24px;margin-bottom:20px}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}.field{display:flex;flex-direction:column;gap:8px}.field label{color:#8a8178;font-size:13px}input,select{border:1px solid #e2d8cc;border-radius:12px;padding:10px 12px}.formula{margin-top:20px;padding:18px;border-radius:16px;background:#faf7f2;color:#b3261e;font-weight:700}.weight{display:flex;justify-content:space-between;align-items:center;margin-top:14px;padding:18px;border-radius:16px;background:#faf7f2}.weight p{margin:6px 0 0;color:#8a8178}.winput{display:flex;align-items:center;gap:12px;width:220px}.winput span{color:#b3261e;font-weight:700}.sim{display:grid;grid-template-columns:2fr 1fr;gap:20px}.result{padding:22px;border-radius:18px;background:#1f1f1f;color:#fff}.result b{color:#ffb4aa;font-size:24px}
</style>
