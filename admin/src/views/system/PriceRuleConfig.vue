<template>
  <div class="price-rule-page">
    <div class="page-header">
      <div>
        <h1>自动涨价规则</h1>
        <p>配置作品上线时间、艺术家等级、浏览收藏热度与流通记录对价格的实际影响。</p>
      </div>
      <div class="actions">
        <button class="ghost" @click="resetConfig">恢复默认</button>
        <button class="primary" :disabled="loading" @click="saveConfig">
          {{ loading ? '保存中...' : '保存规则' }}
        </button>
      </div>
    </div>

    <div class="stats">
      <div class="stat">
        <label>全局自动涨价</label>
        <b>{{ config.enabled ? '开启' : '关闭' }}</b>
      </div>
      <div class="stat">
        <label>基础日涨幅</label>
        <b>{{ percent(config.baseDailyRate) }}</b>
      </div>
      <div class="stat">
        <label>成熟期日涨幅</label>
        <b>{{ percent(config.matureDailyRate) }}</b>
      </div>
      <div class="stat">
        <label>最大涨幅倍数</label>
        <b>{{ config.maxGrowthMultiple }}x</b>
      </div>
    </div>

    <div class="card">
      <h2>全局规则</h2>
      <div class="grid">
        <div class="field">
          <label>自动涨价</label>
          <select v-model="config.enabled">
            <option :value="true">开启</option>
            <option :value="false">关闭</option>
          </select>
        </div>
        <div class="field">
          <label>最大涨幅倍数</label>
          <input v-model.number="config.maxGrowthMultiple" type="number" min="1" step="0.1" />
        </div>
      </div>
      <div class="formula">最终价格 = 原始价格 × min(时间系数 × 艺术家系数 × 热度系数 × 流通系数, 最大涨幅倍数)</div>
    </div>

    <div class="card">
      <h2>时间因素</h2>
      <div class="grid">
        <div class="field">
          <label>基础日增长率</label>
          <input v-model.number="config.baseDailyRate" type="number" min="0" step="0.0001" />
        </div>
        <div class="field">
          <label>成熟期天数</label>
          <input v-model.number="config.matureDays" type="number" min="0" step="1" />
        </div>
        <div class="field">
          <label>成熟期日增长率</label>
          <input v-model.number="config.matureDailyRate" type="number" min="0" step="0.0001" />
        </div>
      </div>
    </div>

    <div class="card">
      <h2>艺术家等级系数</h2>
      <div class="grid">
        <div class="field">
          <label>普通艺术家</label>
          <input v-model.number="config.defaultBadgeRate" type="number" min="0.1" step="0.1" />
        </div>
        <div class="field">
          <label>认证艺术家</label>
          <input v-model.number="config.verifiedBadgeRate" type="number" min="0.1" step="0.1" />
        </div>
        <div class="field">
          <label>人气艺术家</label>
          <input v-model.number="config.popularBadgeRate" type="number" min="0.1" step="0.1" />
        </div>
        <div class="field">
          <label>大师级艺术家</label>
          <input v-model.number="config.masterBadgeRate" type="number" min="0.1" step="0.1" />
        </div>
      </div>
    </div>

    <div class="card">
      <h2>热度与流通系数</h2>
      <div class="grid">
        <div class="field">
          <label>浏览量阈值</label>
          <input v-model.number="config.viewThreshold" type="number" min="0" step="1" />
        </div>
        <div class="field">
          <label>浏览量加成</label>
          <input v-model.number="config.viewRate" type="number" min="1" step="0.01" />
        </div>
        <div class="field">
          <label>收藏量阈值</label>
          <input v-model.number="config.favoriteThreshold" type="number" min="0" step="1" />
        </div>
        <div class="field">
          <label>收藏量加成</label>
          <input v-model.number="config.favoriteRate" type="number" min="1" step="0.01" />
        </div>
        <div class="field">
          <label>单次流通加成</label>
          <input v-model.number="config.saleRate" type="number" min="0" step="0.01" />
        </div>
        <div class="field">
          <label>最多计算流通次数</label>
          <input v-model.number="config.maxSaleCount" type="number" min="0" step="1" />
        </div>
      </div>
    </div>

    <div class="card">
      <h2>实际关联项</h2>
      <div class="weight editable-weight">
        <div>
          <b>作品上线天数</b>
          <p>前阈值天数按基础日增长率，之后按成熟期日增长率。</p>
        </div>
        <div class="factor-editor">
          <label>阈值天数 <input v-model.number="config.matureDays" type="number" min="0" step="1" /></label>
          <label>基础日涨 <input v-model.number="config.baseDailyRate" type="number" min="0" step="0.0001" /></label>
          <label>成熟日涨 <input v-model.number="config.matureDailyRate" type="number" min="0" step="0.0001" /></label>
        </div>
      </div>
      <div class="weight editable-weight">
        <div>
          <b>艺术家等级</b>
          <p>按照普通、认证、人气、大师四类艺术家系数参与乘算。</p>
        </div>
        <div class="factor-editor four">
          <label>普通 <input v-model.number="config.defaultBadgeRate" type="number" min="0.1" step="0.1" /></label>
          <label>认证 <input v-model.number="config.verifiedBadgeRate" type="number" min="0.1" step="0.1" /></label>
          <label>人气 <input v-model.number="config.popularBadgeRate" type="number" min="0.1" step="0.1" /></label>
          <label>大师 <input v-model.number="config.masterBadgeRate" type="number" min="0.1" step="0.1" /></label>
        </div>
      </div>
      <div class="weight editable-weight">
        <div>
          <b>浏览量</b>
          <p>达到阈值后启用浏览量加成。</p>
        </div>
        <div class="factor-editor two">
          <label>阈值 <input v-model.number="config.viewThreshold" type="number" min="0" step="1" /></label>
          <label>加成 <input v-model.number="config.viewRate" type="number" min="1" step="0.01" /></label>
        </div>
      </div>
      <div class="weight editable-weight">
        <div>
          <b>收藏量</b>
          <p>达到阈值后启用收藏量加成。</p>
        </div>
        <div class="factor-editor two">
          <label>阈值 <input v-model.number="config.favoriteThreshold" type="number" min="0" step="1" /></label>
          <label>加成 <input v-model.number="config.favoriteRate" type="number" min="1" step="0.01" /></label>
        </div>
      </div>
      <div class="weight editable-weight">
        <div>
          <b>流通次数</b>
          <p>每次流通增加指定比例，并限制最多计算次数。</p>
        </div>
        <div class="factor-editor two">
          <label>单次加成 <input v-model.number="config.saleRate" type="number" min="0" step="0.01" /></label>
          <label>最多次数 <input v-model.number="config.maxSaleCount" type="number" min="0" step="1" /></label>
        </div>
      </div>
      <div class="weight editable-weight">
        <div>
          <b>涨幅上限</b>
          <p>所有系数乘算后会按最大涨幅倍数封顶。</p>
        </div>
        <div class="factor-editor one">
          <label>最大倍数 <input v-model.number="config.maxGrowthMultiple" type="number" min="1" step="0.1" /></label>
        </div>
      </div>
    </div>

    <div class="card">
      <h2>涨价模拟器</h2>
      <div class="sim">
        <div class="grid">
          <div class="field">
            <label>原始价格</label>
            <input v-model.number="sim.originalPrice" type="number" min="0" />
          </div>
          <div class="field">
            <label>作品上线天数</label>
            <input v-model.number="sim.onlineDays" type="number" min="0" />
          </div>
          <div class="field">
            <label>艺术家等级</label>
            <select v-model="sim.badge">
              <option value="default">普通艺术家</option>
              <option value="verified">认证艺术家</option>
              <option value="popular">人气艺术家</option>
              <option value="master">大师级艺术家</option>
            </select>
          </div>
          <div class="field">
            <label>浏览量</label>
            <input v-model.number="sim.viewCount" type="number" min="0" />
          </div>
          <div class="field">
            <label>收藏量</label>
            <input v-model.number="sim.favoriteCount" type="number" min="0" />
          </div>
          <div class="field">
            <label>流通次数</label>
            <input v-model.number="sim.saleCount" type="number" min="0" />
          </div>
        </div>
        <div class="result">
          <p>时间系数：{{ multiplier(timeMultiplier) }}</p>
          <p>艺术家系数：{{ multiplier(badgeMultiplier) }}</p>
          <p>热度系数：{{ multiplier(heatMultiplier) }}</p>
          <p>流通系数：{{ multiplier(saleMultiplier) }}</p>
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

const defaults = {
  enabled: true,
  baseDailyRate: 0.0002,
  matureDailyRate: 0.0003,
  matureDays: 30,
  defaultBadgeRate: 1,
  verifiedBadgeRate: 1.5,
  popularBadgeRate: 2,
  masterBadgeRate: 3,
  viewThreshold: 100,
  viewRate: 1.1,
  favoriteThreshold: 5,
  favoriteRate: 1.1,
  saleRate: 0.05,
  maxSaleCount: 10,
  maxGrowthMultiple: 5
}

const config = reactive({ ...defaults })
const sim = reactive({
  originalPrice: 10000,
  onlineDays: 30,
  badge: 'verified',
  viewCount: 500,
  favoriteCount: 8,
  saleCount: 2
})
const loading = ref(false)

const numericKeys = Object.keys(defaults).filter(key => key !== 'enabled')

const normalizeConfig = (data = {}) => {
  const next = { ...defaults, ...data }
  next.enabled = Boolean(next.enabled)
  numericKeys.forEach(key => {
    next[key] = Number(next[key] ?? defaults[key])
  })
  return next
}

onMounted(async () => {
  try {
    Object.assign(config, normalizeConfig(await getPriceRule()))
  } catch (e) {
    console.warn('加载价格规则配置失败，使用默认值', e)
  }
})

const timeMultiplier = computed(() => {
  const days = Math.max(Number(sim.onlineDays || 0), 0)
  if (days > config.matureDays) {
    return 1 + config.matureDailyRate * (days - config.matureDays)
  }
  return 1 + config.baseDailyRate * days
})

const badgeMultiplier = computed(() => {
  const map = {
    default: config.defaultBadgeRate,
    verified: config.verifiedBadgeRate,
    popular: config.popularBadgeRate,
    master: config.masterBadgeRate
  }
  return Number(map[sim.badge] || config.defaultBadgeRate)
})

const heatMultiplier = computed(() => {
  const viewMultiplier = Number(sim.viewCount || 0) >= config.viewThreshold ? config.viewRate : 1
  const favoriteMultiplier = Number(sim.favoriteCount || 0) >= config.favoriteThreshold ? config.favoriteRate : 1
  return viewMultiplier * favoriteMultiplier
})

const saleMultiplier = computed(() => {
  const count = Math.min(Math.max(Number(sim.saleCount || 0), 0), config.maxSaleCount)
  return Math.pow(1 + Number(config.saleRate || 0), count)
})

const rawMultiplier = computed(() => timeMultiplier.value * badgeMultiplier.value * heatMultiplier.value * saleMultiplier.value)
const finalMultiplier = computed(() => config.enabled ? Math.min(rawMultiplier.value, Number(config.maxGrowthMultiple || 1)) : 1)
const rawRate = computed(() => Math.max(rawMultiplier.value - 1, 0))
const finalRate = computed(() => Math.max(finalMultiplier.value - 1, 0))
const todayPrice = computed(() => Math.round(Number(sim.originalPrice || 0) * finalMultiplier.value))

function percent(value) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}

function multiplier(value) {
  return `${Number(value || 0).toFixed(3)}x`
}

function money(value) {
  return `¥${String(Math.round(Number(value || 0))).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`
}

async function saveConfig() {
  loading.value = true
  try {
    await savePriceRule({ ...config })
    ElMessage.success('规则已保存，作品使用全局配置时会按此规则计算')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

function resetConfig() {
  Object.assign(config, { ...defaults })
  ElMessage.info('已恢复默认值，点击保存后生效')
}
</script>

<style scoped>
.price-rule-page { min-height: 100vh; padding: 24px; background: #f7f4ef; color: #1f1f1f; }
.page-header { display: flex; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.page-header h1 { margin: 0; font-size: 28px; }
.page-header p { color: #8a8178; }
.actions { display: flex; gap: 12px; align-items: flex-start; }
.primary, .ghost { border: none; border-radius: 999px; padding: 10px 18px; cursor: pointer; }
.primary { background: #b3261e; color: #fff; }
.primary:disabled { cursor: not-allowed; opacity: .6; }
.ghost { background: #f5f0e8; }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat, .card { background: #fff; border-radius: 18px; box-shadow: 0 8px 28px rgba(43, 33, 24, .05); }
.stat { padding: 22px; }
.stat label { color: #8a8178; }
.stat b { display: block; margin-top: 8px; color: #b3261e; font-size: 28px; }
.card { padding: 24px; margin-bottom: 20px; }
.card h2 { margin: 0 0 18px; }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.field { display: flex; flex-direction: column; gap: 8px; }
.field label { color: #8a8178; font-size: 13px; }
input, select { border: 1px solid #e2d8cc; border-radius: 12px; padding: 10px 12px; background: #fff; }
.formula { margin-top: 20px; padding: 18px; border-radius: 16px; background: #faf7f2; color: #b3261e; font-weight: 700; }
.weight { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-top: 14px; padding: 18px; border-radius: 16px; background: #faf7f2; }
.weight p { margin: 6px 0 0; color: #8a8178; }
.factor-value { color: #b3261e; font-weight: 700; white-space: nowrap; }
.editable-weight { align-items: flex-start; }
.factor-editor { display: grid; grid-template-columns: repeat(3, minmax(120px, 1fr)); gap: 12px; min-width: 520px; }
.factor-editor.four { grid-template-columns: repeat(4, minmax(90px, 1fr)); }
.factor-editor.two { grid-template-columns: repeat(2, minmax(140px, 1fr)); }
.factor-editor.one { grid-template-columns: minmax(160px, 1fr); min-width: 180px; }
.factor-editor label { display: flex; flex-direction: column; gap: 6px; color: #8a8178; font-size: 12px; }
.factor-editor input { color: #1f1f1f; font-size: 14px; }
.sim { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
.result { padding: 22px; border-radius: 18px; background: #1f1f1f; color: #fff; }
.result p { margin: 8px 0; }
.result b { color: #ffb4aa; font-size: 24px; }
@media (max-width: 1100px) {
  .stats, .grid, .sim { grid-template-columns: 1fr; }
  .weight { flex-direction: column; }
  .factor-editor, .factor-editor.four, .factor-editor.two, .factor-editor.one { width: 100%; min-width: 0; grid-template-columns: 1fr; }
}
</style>
