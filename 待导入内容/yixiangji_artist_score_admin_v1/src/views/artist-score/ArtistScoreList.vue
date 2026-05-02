<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>艺术家评分列表</span>
          <el-button type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query">
        <el-form-item label="艺术家">
          <el-input v-model="query.keyword" placeholder="名称/ID" clearable />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.level" placeholder="全部" clearable style="width: 140px">
            <el-option label="A+" value="A+" />
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
            <el-option label="D" value="D" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form>

      <el-table :data="list" border style="width: 100%; margin-top: 16px">
        <el-table-column prop="artistId" label="艺术家ID" width="100" />
        <el-table-column prop="artistName" label="艺术家" min-width="140" />
        <el-table-column prop="totalScore" label="总分" width="100" />
        <el-table-column prop="level" label="等级" width="100">
          <template #default="{ row }">
            <ScoreLevelTag :level="row.level" />
          </template>
        </el-table-column>
        <el-table-column prop="academicScore" label="学术资质" width="100" />
        <el-table-column prop="internetScore" label="互联网资质" width="110" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" fixed="right" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="goDetail(row.artistId)">详情</el-button>
            <el-button size="small" type="primary" @click="recalculate(row.artistId)">重算</el-button>
            <el-button size="small" type="warning" @click="openAdjust(row)">调分</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="adjustVisible" title="人工调分" width="500px">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="艺术家ID">
          <el-input v-model="adjustForm.artistId" disabled />
        </el-form-item>
        <el-form-item label="调整分值">
          <el-input-number v-model="adjustForm.adjustScore" :min="-100" :max="100" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="4" placeholder="必须填写原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ScoreLevelTag from '@/components/ScoreLevelTag.vue'
import { getArtistScoreList, recalculateArtistScore, manualAdjustArtistScore } from '@/api/artistScore'
import { useRouter } from 'vue-router'

const router = useRouter()
const query = ref({ keyword: '', level: '' })
const list = ref([])

const adjustVisible = ref(false)
const adjustForm = ref({
  artistId: '',
  adjustScore: 0,
  reason: ''
})

async function loadData() {
  const res = await getArtistScoreList(query.value)
  list.value = res.records || res || []
}

function goDetail(artistId) {
  router.push(`/artist-score/detail/${artistId}`)
}

async function recalculate(artistId) {
  await recalculateArtistScore(artistId)
  ElMessage.success('评分已重新计算')
  loadData()
}

function openAdjust(row) {
  adjustForm.value = {
    artistId: row.artistId,
    adjustScore: 0,
    reason: ''
  }
  adjustVisible.value = true
}

async function submitAdjust() {
  if (!adjustForm.value.reason) {
    ElMessage.warning('请填写调整原因')
    return
  }
  await manualAdjustArtistScore(adjustForm.value)
  ElMessage.success('人工调分成功')
  adjustVisible.value = false
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
