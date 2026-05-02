<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>作品价格调控</span>
          <el-button type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query">
        <el-form-item label="作品">
          <el-input v-model="query.keyword" placeholder="作品名称/ID" clearable />
        </el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form>

      <el-table :data="list" border style="width: 100%; margin-top: 16px">
        <el-table-column prop="artworkId" label="作品ID" width="100" />
        <el-table-column prop="title" label="作品名称" min-width="160" />
        <el-table-column prop="artistName" label="艺术家" width="140" />
        <el-table-column prop="currentPrice" label="当前价格" width="140" />
        <el-table-column prop="collectCount" label="收藏数" width="100" />
        <el-table-column prop="saleCount" label="成交数" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="openAdjust(row)">人工调价</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="人工调价" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="作品ID">
          <el-input v-model="form.artworkId" disabled />
        </el-form-item>
        <el-form-item label="当前价格">
          <el-input v-model="form.oldPrice" disabled />
        </el-form-item>
        <el-form-item label="新价格">
          <el-input-number v-model="form.newPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="调价原因">
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="必须填写原因" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">确认调价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getArtworkPriceList, manualAdjustArtworkPrice } from '@/api/artworkPrice'

const query = ref({ keyword: '' })
const list = ref([])
const visible = ref(false)
const form = ref({
  artworkId: '',
  oldPrice: 0,
  newPrice: 0,
  reason: ''
})

async function loadData() {
  const res = await getArtworkPriceList(query.value)
  list.value = res.records || res || []
}

function openAdjust(row) {
  form.value = {
    artworkId: row.artworkId,
    oldPrice: row.currentPrice,
    newPrice: row.currentPrice,
    reason: ''
  }
  visible.value = true
}

async function submitAdjust() {
  if (!form.value.reason) {
    ElMessage.warning('请填写调价原因')
    return
  }

  await manualAdjustArtworkPrice(form.value)
  ElMessage.success('调价成功，已写入价格日志')
  visible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
