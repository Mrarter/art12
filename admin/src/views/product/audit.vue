<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">作品审核</span>
      <el-radio-group v-model="status" @change="loadData">
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="approved">已通过</el-radio-button>
        <el-radio-button label="rejected">已拒绝</el-radio-button>
      </el-radio-group>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="artworkCode" label="作品编号" width="200">
        <template #default="{ row }">
          <div class="id-cell" @click="handleCopyId(row.artworkCode)">
            <span class="id-text">{{ row.artworkCode || '-' }}</span>
            <el-icon class="copy-icon"><DocumentCopy /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="作品信息" min-width="280">
        <template #default="{ row }">
          <div class="artwork-info">
            <el-image :src="row.cover" :preview-src-list="[row.cover]" style="width: 80px; height: 80px" fit="cover" />
            <div class="detail">
              <p class="title">{{ row.title }}</p>
              <p class="artist">{{ row.artistName }}</p>
              <p class="category">{{ row.categoryName }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="120">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.auditStatus)">
            {{ getStatusText(row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ row.submitTime || row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.auditStatus === 'pending'">
            <el-button type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
          </template>
          <template v-else>
            <el-button type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button type="info" link disabled>{{ row.auditStatus === 'approved' ? '已通过' : '已拒绝' }}</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
    
    <!-- 作品详情弹窗 -->
    <el-dialog v-model="detailVisible" title="作品详情" width="800px">
      <div v-if="currentArtwork" class="artwork-detail">
        <el-row :gutter="20">
          <el-col :span="10">
            <el-image :src="currentArtwork.cover" style="width: 100%; height: 300px" fit="contain" />
          </el-col>
          <el-col :span="14">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="作品名称">{{ currentArtwork.title }}</el-descriptions-item>
              <el-descriptions-item label="艺术家">{{ currentArtwork.artistName }}</el-descriptions-item>
              <el-descriptions-item label="分类">{{ currentArtwork.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="价格">¥{{ currentArtwork.price }}</el-descriptions-item>
              <el-descriptions-item label="尺寸">{{ currentArtwork.size }}</el-descriptions-item>
              <el-descriptions-item label="材质">{{ currentArtwork.material }}</el-descriptions-item>
              <el-descriptions-item label="创作年份">{{ currentArtwork.year }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ currentArtwork.description }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
      </div>
      <template v-if="currentArtwork?.auditStatus === 'pending'" #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button type="danger" @click="handleRejectFromDetail">拒绝</el-button>
        <el-button type="success" @click="handleApproveFromDetail">通过</el-button>
      </template>
    </el-dialog>
    
    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝原因" width="500px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input 
            v-model="rejectReason" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入拒绝原因，将通知艺术家"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import request from '@/api/request'
import { copyId } from '@/utils/id'

const loading = ref(false)
const status = ref('pending')
const tableData = ref([])
const detailVisible = ref(false)
const rejectVisible = ref(false)
const currentArtwork = ref({})
const rejectReason = ref('')

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待审核', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

// 复制作品编号
const handleCopyId = async (id) => {
  if (!id) {
    ElMessage.warning('作品编号为空')
    return
  }
  copyId(id,
    () => ElMessage.success('已复制作品编号'),
    () => ElMessage.error('复制失败')
  )
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size, auditStatus: status.value }
    const data = await request.get('/product/audit/list', { params })
    tableData.value = (data.list || []).map((item, index) => ({
      ...item,
      artworkCode: item.artworkCode || `ART${new Date().toISOString().slice(0,10).replace(/-/g,'')}${String(index + 1).padStart(3, '0')}${String.fromCharCode(65 + index)}`
    }))
    pagination.total = data.total || 0
  } catch (e) {
    tableData.value = [
      { artworkId: 1, artworkCode: 'ART202604250001X5K3', title: '山水画', artistName: '张三', categoryName: '国画', price: 50000, auditStatus: 'pending', submitTime: '2024-04-25 10:30:00' },
      { artworkId: 2, artworkCode: 'ART202604250002M8P7', title: '油画风景', artistName: '李四', categoryName: '油画', price: 80000, auditStatus: 'pending', submitTime: '2024-04-25 09:15:00' },
      { artworkId: 3, artworkCode: 'ART202604250003W3F2', title: '书法作品', artistName: '王五', categoryName: '书法', price: 30000, auditStatus: 'approved', submitTime: '2024-04-24 14:20:00' }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentArtwork.value = row
  detailVisible.value = true
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该作品审核吗？', '提示', { type: 'success' })
    await request.post('/product/audit/approve', { artworkId: row.artworkId })
    ElMessage.success('已通过审核')
    await loadData()
  } catch (e) {}
}

const handleReject = (row) => {
  currentArtwork.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const handleApproveFromDetail = async () => {
  try {
    await request.post('/product/audit/approve', { artworkId: currentArtwork.value.artworkId })
    ElMessage.success('已通过审核')
    detailVisible.value = false
    await loadData()
  } catch (e) {}
}

const handleRejectFromDetail = () => {
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await request.post('/product/audit/reject', {
      artworkId: currentArtwork.value.artworkId,
      reason: rejectReason.value
    })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    detailVisible.value = false
    await loadData()
  } catch (e) {}
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.artwork-info {
  display: flex;
  gap: 12px;
  
  .detail {
    .title {
      font-weight: 500;
      margin-bottom: 4px;
    }
    .artist {
      font-size: 13px;
      color: #666;
    }
    .category {
      font-size: 12px;
      color: #999;
    }
  }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ID单元格样式 */
.id-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: #409eff;

  .id-text {
    letter-spacing: 0.5px;
  }

  .copy-icon {
    opacity: 0;
    transition: opacity 0.2s;
    font-size: 12px;
  }

  &:hover .copy-icon {
    opacity: 1;
  }
}
</style>
