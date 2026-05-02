<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>艺术家资质审核</span>
          <el-button type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query">
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" clearable placeholder="全部" style="width: 160px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="PASS" />
            <el-option label="已驳回" value="REJECT" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form>

      <el-table :data="list" border style="width: 100%; margin-top: 16px">
        <el-table-column prop="artistId" label="艺术家ID" width="100" />
        <el-table-column prop="artistName" label="艺术家" width="140" />
        <el-table-column prop="schoolName" label="毕业院校" min-width="160" />
        <el-table-column prop="academicTitle" label="职称" width="100" />
        <el-table-column prop="socialPlatform" label="平台" width="100" />
        <el-table-column prop="followerCount" label="粉丝数" width="120" />
        <el-table-column prop="auditStatus" label="状态" width="110" />
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看</el-button>
            <el-button size="small" type="success" @click="audit(row, 'PASS')">通过</el-button>
            <el-button size="small" type="danger" @click="audit(row, 'REJECT')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="资质详情" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="毕业院校">{{ current.schoolName }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ current.degree }}</el-descriptions-item>
        <el-descriptions-item label="职称">{{ current.academicTitle }}</el-descriptions-item>
        <el-descriptions-item label="协会">{{ current.associationName }}</el-descriptions-item>
        <el-descriptions-item label="社交平台">{{ current.socialPlatform }}</el-descriptions-item>
        <el-descriptions-item label="粉丝数">{{ current.followerCount }}</el-descriptions-item>
        <el-descriptions-item label="账号链接" :span="2">{{ current.socialAccountUrl }}</el-descriptions-item>
        <el-descriptions-item label="展览经历" :span="2">{{ current.exhibitions }}</el-descriptions-item>
        <el-descriptions-item label="获奖经历" :span="2">{{ current.awards }}</el-descriptions-item>
      </el-descriptions>

      <el-input
        v-model="auditRemark"
        type="textarea"
        :rows="4"
        placeholder="审核备注"
        style="margin-top: 16px"
      />

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="audit(current, 'PASS')">审核通过</el-button>
        <el-button type="danger" @click="audit(current, 'REJECT')">驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getIdentityAuditList, auditArtistIdentity } from '@/api/artistIdentity'
import { recalculateArtistScore } from '@/api/artistScore'

const query = ref({ auditStatus: 'PENDING' })
const list = ref([])
const detailVisible = ref(false)
const current = ref({})
const auditRemark = ref('')

async function loadData() {
  const res = await getIdentityAuditList(query.value)
  list.value = res.records || res || []
}

function openDetail(row) {
  current.value = row
  auditRemark.value = row.auditRemark || ''
  detailVisible.value = true
}

async function audit(row, status) {
  await auditArtistIdentity({
    artistId: row.artistId,
    auditStatus: status,
    auditRemark: auditRemark.value
  })

  if (status === 'PASS') {
    await recalculateArtistScore(row.artistId)
  }

  ElMessage.success(status === 'PASS' ? '审核通过，评分已重算' : '已驳回')
  detailVisible.value = false
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
