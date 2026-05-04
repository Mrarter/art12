<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>作品价格日志</span>
          <el-button type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query">
        <el-form-item label="作品ID">
          <el-input v-model="query.artworkId" clearable />
        </el-form-item>
        <el-form-item label="原因">
          <el-select v-model="query.changeReason" clearable placeholder="全部" style="width: 160px">
            <el-option label="每日涨价" value="DAILY" />
            <el-option label="成交触发" value="SALE" />
            <el-option label="收藏触发" value="COLLECT" />
            <el-option label="评分触发" value="SCORE" />
            <el-option label="人工调价" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form>

      <el-table :data="list" border style="width: 100%; margin-top: 16px">
        <el-table-column prop="artworkId" label="作品ID" width="100" />
        <el-table-column prop="artistId" label="艺术家ID" width="110" />
        <el-table-column prop="oldPrice" label="原价格" width="120" />
        <el-table-column prop="newPrice" label="新价格" width="120" />
        <el-table-column prop="changeRate" label="涨跌幅" width="120" />
        <el-table-column prop="changeReason" label="原因" width="120" />
        <el-table-column prop="remark" label="备注" min-width="220" />
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getArtworkPriceLogs } from '@/api/artworkPrice'

const query = ref({
  artworkId: '',
  changeReason: ''
})
const list = ref([])

async function loadData() {
  const res = await getArtworkPriceLogs(query.value)
  list.value = res.records || res || []
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
