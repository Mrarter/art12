<template>
  <div class="page">
    <div class="page-header">
      <div><h1>正式订单</h1><p>管理艺术品成交订单、支付状态、平台佣金、艺术家结算、收藏证书与流通记录。</p></div>
      <div class="actions"><button class="ghost" @click="resetFilters">重置筛选</button><button class="primary" @click="refresh">刷新</button></div>
    </div>
    <div class="stats">
      <div class="stat"><label>待支付</label><b>{{ stats.s0 }}</b></div>
      <div class="stat"><label>今日成交</label><b>{{ stats.s1 }}</b></div>
      <div class="stat"><label>待生成证书</label><b>{{ stats.s2 }}</b></div>
      <div class="stat"><label>待结算金额</label><b>{{ stats.s3 }}</b></div>
    </div>
    <div class="card">
      <div class="filters">
        <div class="field"><label>关键词</label><input v-model="filters.keyword" placeholder="搜索编号 / 作品 / 艺术家 / 藏家" /></div>
        <div class="field"><label>订单状态</label><select v-model="filters.status"><option value="">全部</option><option value="PENDING">待处理</option><option value="DONE">已完成</option></select></div>
        <div class="field"><label>展示</label><select v-model="filters.visible"><option value="">全部</option><option value="true">展示</option><option value="false">隐藏</option></select></div>
        <div class="field"><label>类型</label><input v-model="filters.type" placeholder="类型" /></div>
      </div>
      <div class="filter-actions"><button class="primary" @click="refresh">查询</button></div>
    </div>
    <div class="card" style="padding:0;overflow:hidden">
      <table>
        <thead><tr><th>对象</th><th>状态</th><th>金额/数据</th><th>标签</th><th>更新时间</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td><div class="work-cell"><img v-if="item.coverUrl" :src="item.coverUrl" /><div><div class="strong">{{ item.title }}</div><div class="small">{{ item.sub }}</div></div></div></td>
            <td><span class="badge" :class="item.badgeClass">{{ item.statusText }}</span></td>
            <td><div class="price">{{ item.amountText }}</div><div class="small">{{ item.extra }}</div></td>
            <td><div class="tag-list"><span class="tag" v-for="tag in item.tags" :key="tag">{{ tag }}</span></div></td>
            <td>{{ item.updatedAt }}</td>
            <td class="right"><button class="text" @click="openDetail(item)">详情</button><button class="text danger" @click="mockAction(item)">处理</button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="list.length===0" class="empty">暂无数据</div>
    </div>
    <div v-if="detailVisible" class="modal" @click.self="detailVisible=false">
      <div class="modal-box">
        <div class="modal-head"><div><h2>正式订单详情</h2><p>{{ current?.title }}</p></div><button class="text" style="color:white;font-size:28px" @click="detailVisible=false">×</button></div>
        <div class="modal-body">
          <div class="section"><h3>详情数据</h3><pre>{{ current }}</pre></div>
          <div class="section"><h3>备注</h3><textarea v-model="remark" placeholder="添加备注"></textarea><button class="primary" style="margin-top:12px" @click="addLog">添加备注</button></div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'
import request from '@/api/request'

const stats = reactive({ s0:0, s1:0, s2:0, s3:0 })
const filters = reactive({ keyword:'', status:'', visible:'', type:'' })
const list = ref([])
const detailVisible = ref(false)
const current = ref(null)
const remark = ref('')
const loading = ref(false)

onMounted(() => { refresh() })

async function refresh(){
  loading.value = true
  try {
    const params = { page: 1, size: 20 }
    if (filters.keyword) params.userName = filters.keyword
    if (filters.status) params.status = filters.status
    const res = await request.get('/order/list', { params })
    const data = res?.records || res?.list || []
    list.value = data.map(mapOrderRow)
    stats.s0 = data.filter(i => i.status === 'pending' || i.rawStatus === 'WAIT_PAY').length
    stats.s1 = data.filter(i => i.rawStatus === 'PAID').length
    stats.s2 = data.filter(i => i.rawStatus === 'WAIT_DELIVER').length
    stats.s3 = data.reduce((s, i) => s + (Number(i.totalAmount) || 0), 0)
  } catch (e) {
    console.error('加载订单列表失败', e)
    list.value = []
  } finally {
    loading.value = false
  }
}

function mapOrderRow(item) {
  const statusText = item.statusText || item.status || ''
  return {
    id: item.id,
    title: item.orderNo || '',
    sub: `${item.artworkTitle || ''}｜${item.buyerName || ''}`,
    statusText,
    badgeClass: item.rawStatus === 'WAIT_PAY' ? 'red' : item.rawStatus === 'PAID' ? 'green' : item.rawStatus === 'DONE' ? '' : 'gray',
    amountText: `¥${(Number(item.totalAmount) || 0).toLocaleString()}`,
    extra: item.rawStatus === 'WAIT_DELIVER' ? '待发货' : `创建于${(item.createTime || '').slice(0, 10)}`,
    tags: [item.orderType === 'PRIMARY' ? '首次收藏' : '再次流通', item.paymentStatus === 'PAID' ? '已付款' : '待付款'].filter(Boolean),
    updatedAt: (item.createTime || '').slice(0, 16),
    coverUrl: item.cover || '',
    _raw: item
  }
}

function resetFilters(){ filters.keyword=''; filters.status=''; filters.visible=''; filters.type=''; refresh() }
function openDetail(item){ current.value=item; detailVisible.value=true }
function mockAction(item){ /* 占位 - 需要发货/取消等操作 */ }
function addLog(){ if(!remark.value.trim()) return; alert('备注已添加'); remark.value='' }
</script>
<style scoped>
.page{min-height:100vh;padding:24px;background:#f7f4ef;color:#1f1f1f}.page-header{display:flex;justify-content:space-between;margin-bottom:24px}.page-header h1{margin:0;font-size:28px}.page-header p{margin:8px 0 0;color:#8a8178}.actions{display:flex;gap:12px}.primary,.ghost,.text{border:none;border-radius:999px;cursor:pointer}.primary{padding:10px 18px;background:#b3261e;color:#fff}.ghost{padding:10px 18px;background:#f5f0e8;color:#1f1f1f}.text{background:transparent;color:#1f1f1f;margin-left:8px}.text.danger{color:#b3261e}.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px}.stat,.card{background:#fff;border-radius:18px;box-shadow:0 8px 28px rgba(43,33,24,.05)}.stat{padding:22px}.stat label{color:#8a8178}.stat b{display:block;margin-top:8px;color:#b3261e;font-size:28px}.card{padding:20px;margin-bottom:20px}.filters{display:grid;grid-template-columns:2fr 1fr 1fr 1fr;gap:16px}.field{display:flex;flex-direction:column;gap:8px}.field label{color:#8a8178;font-size:13px}input,select,textarea{box-sizing:border-box;width:100%;border:1px solid #e2d8cc;border-radius:12px;padding:10px 12px;background:#fff;color:#1f1f1f;outline:none}textarea{min-height:90px}.filter-actions{text-align:right;margin-top:18px}table{width:100%;border-collapse:collapse;background:#fff}th{padding:14px 16px;background:#faf7f2;color:#8a8178;text-align:left;font-weight:500}td{padding:16px;border-top:1px solid #f0eae2;vertical-align:middle}.right{text-align:right}.small{color:#8a8178;font-size:12px;margin-top:4px}.strong{font-weight:700}.price{color:#b3261e;font-weight:700}.badge{display:inline-flex;height:26px;align-items:center;padding:0 10px;border-radius:999px;background:#1f1f1f;color:#fff;font-size:12px}.badge.red{background:#b3261e}.badge.gray{background:#8a8178}.badge.green{background:#315f42}.work-cell{display:flex;align-items:center;gap:12px}.work-cell img{width:54px;height:70px;border-radius:10px;object-fit:cover;background:#e8e2d8}.tag-list{display:flex;flex-wrap:wrap;gap:6px}.tag{height:24px;padding:0 10px;border-radius:999px;background:#f5f0e8;color:#6b6259;font-size:12px;line-height:24px}.modal{position:fixed;inset:0;background:rgba(0,0,0,.38);z-index:1000;display:flex;align-items:flex-start;justify-content:center;padding-top:48px}.modal-box{width:920px;max-height:86vh;overflow:auto;border-radius:22px;background:#f7f4ef}.modal-head{display:flex;justify-content:space-between;padding:24px 28px;background:#1f1f1f;color:#fff}.modal-head h2{margin:0}.modal-body{padding:20px}.section{padding:20px;margin-bottom:16px;background:#fff;border-radius:18px;}.empty{padding:50px;text-align:center;color:#8a8178}
</style>
