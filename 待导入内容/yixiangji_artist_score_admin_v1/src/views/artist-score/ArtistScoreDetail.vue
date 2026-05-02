<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>艺术家评分详情</span>
          <el-button type="primary" @click="loadData">刷新评分</el-button>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="6">
          <el-statistic title="总评分" :value="score.totalScore || 0" />
        </el-col>
        <el-col :span="6">
          <div class="level-box">
            <div class="level-label">当前等级</div>
            <ScoreLevelTag :level="score.level" />
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <el-table :data="scoreItems" border>
        <el-table-column prop="name" label="评分维度" width="160" />
        <el-table-column prop="value" label="分值" width="120" />
        <el-table-column prop="max" label="上限" width="120" />
        <el-table-column prop="desc" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ScoreLevelTag from '@/components/ScoreLevelTag.vue'
import { getArtistScoreDetail } from '@/api/artistScore'

const route = useRoute()
const artistId = route.params.artistId
const score = ref({})

const scoreItems = computed(() => [
  { name: '销售表现', value: score.value.salesScore || 0, max: 300, desc: '成交金额、成交数量、销售增长率' },
  { name: '市场影响力', value: score.value.influenceScore || 0, max: 200, desc: '关注、收藏、浏览、分享' },
  { name: '活跃度', value: score.value.activityScore || 0, max: 100, desc: '上新、登录、互动' },
  { name: '作品质量', value: score.value.qualityScore || 0, max: 150, desc: '平台评审与作品完整度' },
  { name: '藏家评价', value: score.value.reviewScore || 0, max: 100, desc: '评价、复购、评论质量' },
  { name: '学术资质', value: score.value.academicScore || 0, max: 100, desc: '美院、职称、协会、展览、获奖' },
  { name: '互联网资质', value: score.value.internetScore || 0, max: 50, desc: '艺术博主身份、粉丝、内容质量、转化' }
])

async function loadData() {
  score.value = await getArtistScoreDetail(artistId)
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
.level-box {
  padding-top: 6px;
}
.level-label {
  color: #888;
  margin-bottom: 8px;
}
</style>
