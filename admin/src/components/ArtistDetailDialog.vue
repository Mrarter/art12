<template>
  <el-dialog v-model="internalVisible" title="用户详情" width="800px" destroy-on-close @close="onClose">
    <el-tabs v-model="detailActiveTab" type="border-card" v-if="currentUser">
      <!-- Tab1: 用户信息 -->
      <el-tab-pane label="用户信息" name="info">
        <div class="user-profile">
          <div class="profile-header">
            <div class="avatar-wrapper">
              <el-avatar :src="getFullImageUrl(profileForm.avatar)" :size="80" fit="cover" />
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :http-request="handleAvatarUpload"
                accept="image/*"
              >
                <el-button size="small" type="primary">更换头像</el-button>
              </el-upload>
            </div>
            <div class="profile-info">
              <h3>{{ profileForm.nickname || (currentUser.nickname || currentUser.userNickname || '未知用户') }}
                <el-tag v-if="currentUser.isVip" type="warning" size="small">VIP</el-tag>
              </h3>
              <p class="user-id">ID: {{ currentUser.displayId || currentUser.userId || currentUser.id }}</p>
              <div class="identity-tags">
                <el-tag v-if="currentUser.isArtist" type="success" size="small">艺术家</el-tag>
                <el-tag v-if="currentUser.isPromoter" type="warning" size="small">经纪人</el-tag>
                <el-tag v-if="!currentUser.isArtist && !currentUser.isPromoter" type="info" size="small">普通用户</el-tag>
              </div>
            </div>
          </div>
          <el-form ref="profileFormRef" :model="profileForm" label-width="90px" class="profile-form" @keydown="handleProfileFormKeydown">
            <el-divider content-position="left">基本信息</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号">
                  <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-divider content-position="left">艺术家信息</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="真实姓名">
                  <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="身份证号">
                  <el-input v-model="profileForm.idCard" placeholder="请输入身份证号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="主页标题">
                  <el-input v-model="profileForm.artistTitle" placeholder="如：当代油画艺术家" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="展示标签">
                  <el-input v-model="profileForm.artistTags" placeholder="多个标签用逗号分隔" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="主页头图">
              <div class="cover-uploader">
                <el-image v-if="profileForm.homepageCover" :src="getFullImageUrl(profileForm.homepageCover)" fit="cover" class="cover-preview" />
                <el-upload
                  class="cover-upload"
                  :show-file-list="false"
                  :http-request="handleHomepageCoverUpload"
                  accept="image/*"
                >
                  <el-button v-if="!profileForm.homepageCover" type="primary" size="small">上传头图</el-button>
                  <el-button v-else type="warning" size="small">更换头图</el-button>
                </el-upload>
              </div>
            </el-form-item>
            <el-form-item label="艺术家简介">
              <el-input v-model="profileForm.resume" type="textarea" :rows="2" placeholder="请输入艺术家简介" />
            </el-form-item>
            <el-divider content-position="left">身份配置</el-divider>
            <el-form-item label="身份">
              <el-checkbox-group v-model="profileForm.identities">
                <el-checkbox label="artist">艺术家</el-checkbox>
                <el-checkbox label="promoter">经纪人</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-divider content-position="left">账户信息</el-divider>
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="info-item"><span class="label">账户余额</span><span class="value">¥{{ formatAmount(currentUser.balance) }}</span></div>
              </el-col>
              <el-col :span="8">
                <div class="info-item"><span class="label">累计消费</span><span class="value">¥{{ formatAmount(currentUser.totalConsume) }}</span></div>
              </el-col>
              <el-col :span="8">
                <div class="info-item"><span class="label">订单数量</span><span class="value">{{ currentUser.orderCount || 0 }}</span></div>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="info-item"><span class="label">注册时间</span><span class="value">{{ currentUser.registerTime || currentUser.createTime || '-' }}</span></div>
              </el-col>
              <el-col :span="12">
                <div class="info-item"><span class="label">注册来源</span><span class="value">{{ getSourceText(currentUser.source) }}</span></div>
              </el-col>
            </el-row>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- Tab2: 评分详情 -->
      <el-tab-pane label="评分详情" name="score">
        <div class="score-detail">
          <el-row :gutter="16" style="margin-bottom: 20px;">
            <el-col :span="8">
              <el-statistic title="总评分" :value="currentScoreData.totalScore || 0" />
            </el-col>
            <el-col :span="8">
              <div class="level-box">
                <div class="level-label">当前等级</div>
                <ScoreLevelTag :level="currentScoreData.level" />
              </div>
            </el-col>
            <el-col :span="8" style="text-align: right;">
              <el-button type="primary" size="small" @click="recalculateScore(currentRecord)" style="margin-right: 8px;">重算评分</el-button>
              <el-button type="warning" size="small" @click="openScoreAdjust">人工调分</el-button>
            </el-col>
          </el-row>
          <el-table :data="scoreItems()" border size="small">
            <el-table-column prop="name" label="评分维度" width="140" />
            <el-table-column prop="value" label="分值" width="100" />
            <el-table-column prop="max" label="上限" width="100" />
            <el-table-column prop="desc" label="说明" />
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab3: 资质审核 -->
      <el-tab-pane label="资质审核" name="identity">
        <div class="identity-detail" v-if="currentIdentityData.artistId">
          <el-form :model="currentIdentityData" label-width="100px" ref="identityFormRef">
            <el-row :gutter="20">
              <el-col :span="12"><el-form-item label="毕业院校"><el-input v-model="currentIdentityData.schoolName" placeholder="请输入毕业院校" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="学历"><el-input v-model="currentIdentityData.degree" placeholder="请输入学历" /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12"><el-form-item label="职称"><el-input v-model="currentIdentityData.academicTitle" placeholder="请输入职称" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="协会"><el-input v-model="currentIdentityData.associationName" placeholder="请输入协会名称" /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12"><el-form-item label="社交平台"><el-input v-model="currentIdentityData.socialPlatform" placeholder="如：小红书、抖音" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="粉丝数"><el-input-number v-model="currentIdentityData.followerCount" :min="0" style="width:100%" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="账号链接"><el-input v-model="currentIdentityData.socialAccountUrl" placeholder="请输入社交账号链接" /></el-form-item>
            <el-form-item label="展览经历"><el-input v-model="currentIdentityData.exhibitions" type="textarea" :rows="2" placeholder="请输入展览经历" /></el-form-item>
            <el-form-item label="获奖经历"><el-input v-model="currentIdentityData.awards" type="textarea" :rows="2" placeholder="请输入获奖经历" /></el-form-item>
          </el-form>
          <el-tag :type="identityStatusType()" style="margin-top:16px;">审核状态：{{ identityStatusText() }}</el-tag>
          <el-input v-model="identityAuditRemark" type="textarea" :rows="3" placeholder="审核备注" style="margin-top:16px" />
          <div style="margin-top:16px;display:flex;justify-content:space-between;">
            <el-button type="primary" @click="saveIdentityData">保存身份信息</el-button>
            <span>
              <el-button type="success" @click="auditIdentityInDetail('PASS')">审核通过</el-button>
              <el-button type="danger" @click="auditIdentityInDetail('REJECT')">驳回</el-button>
            </span>
          </div>
        </div>
        <el-empty v-else description="该艺术家暂无资质审核记录" :image-size="60" />
      </el-tab-pane>

      <!-- Tab4: 作品管理 -->
      <el-tab-pane label="作品管理" name="artworks">
        <div class="artworks-section" v-loading="artworksLoading">
            <div v-if="userArtworks.list && userArtworks.list.length > 0" class="artwork-grid">
            <div v-for="artwork in userArtworks.list" :key="artwork.id" class="artwork-item" style="min-height:180px;">
              <div class="artwork-cover-wrapper">
                <el-image 
                  v-if="artwork.cover || artwork.coverImage"
                  :src="getFullImageUrl(artwork.cover || artwork.coverImage)" 
                  :alt="artwork.title" 
                  fit="cover" 
                  class="artwork-cover"
                  :lazy="true"
                >
                  <template #error>
                    <div class="artwork-cover-empty">
                      <el-icon :size="32" color="#c0c4cc"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="artwork-cover-empty">
                  <el-icon :size="32" color="#c0c4cc"><Picture /></el-icon>
                </div>
              </div>
              <div class="artwork-info">
                <p class="artwork-title">{{ artwork.title }}</p>
                <p class="artwork-price">¥{{ formatAmount(artwork.price) }}</p>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无作品" :image-size="60" />
        </div>
        <div v-if="userArtworks.total > userArtworks.list?.length" class="load-more">
          <el-button link type="primary" @click="loadMoreArtworks">加载更多</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <div class="dialog-footer-between">
        <el-button v-if="detailActiveTab === 'info' && (currentRecord?.certified || currentRecord?.status === 1)" type="danger" plain @click="handleRevokeFromDetail">取消认证</el-button>
        <span v-else-if="detailActiveTab !== 'info'"></span>
        <span v-else></span>
        <span>
          <el-button @click="onClose">关闭</el-button>
          <el-button v-if="detailActiveTab === 'info'" type="primary" :loading="editLoading" @click="saveProfile">保存修改</el-button>
        </span>
      </div>
    </template>

    <!-- 人工调分子弹窗 -->
    <el-dialog v-model="scoreAdjustVisible" title="人工调分" width="420px" append-to-body destroy-on-close>
      <el-form :model="scoreAdjustForm" label-width="100px">
        <el-form-item label="调整分值">
          <el-input-number v-model="scoreAdjustForm.adjustScore" :min="0" :max="1000" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="scoreAdjustForm.reason" type="textarea" :rows="3" placeholder="请填写调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreAdjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="submitScoreAdjust">确认</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import request, { getFullImageUrl, uploadFile } from '@/api/request'
import { getArtistScoreDetail, recalculateArtistScore, manualAdjustArtistScore } from '@/api/artistScore'
import { getIdentityDetail, auditArtistIdentity, saveArtistIdentity } from '@/api/artistIdentity'
import ScoreLevelTag from '@/components/ScoreLevelTag.vue'

const props = defineProps({
  visible: Boolean,
  userId: { type: [Number, String], default: null },
  initialData: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['close', 'saved'])

// 内部状态
const internalVisible = ref(false)
const detailVisible = ref(false)
const editLoading = ref(false)
const currentRecord = ref({})
const currentUser = ref({})
const profileFormRef = ref()
const identityFormRef = ref()
const detailActiveTab = ref('info')
const artworksLoading = ref(false)
const userArtworks = ref({ list: [], total: 0 })
const artworksPage = ref(1)
const artworksSize = 8
const scoreAdjustVisible = ref(false)
const scoreAdjustForm = ref({ artistId: '', adjustScore: 0, reason: '' })
const adjustLoading = ref(false)
const currentScoreData = ref({})
const currentIdentityData = ref({})
const identityAuditRemark = ref('')

const profileForm = reactive({
  nickname: '', phone: '', email: '', avatar: '', homepageCover: '',
  identities: [], realName: '', idCard: '',
  artistTitle: '', artistTags: '', resume: ''
})

// 身份状态计算
const identityStatusType = () => {
  const s = currentIdentityData.value?.auditStatus
  if (s === 'PASS') return 'success'
  if (s === 'REJECT') return 'danger'
  return 'warning'
}
const identityStatusText = () => {
  const s = currentIdentityData.value?.auditStatus
  if (s === 'PASS') return '已通过'
  if (s === 'REJECT') return '已驳回'
  return '待审核'
}

const scoreItems = () => [
  { name: '销售表现', value: currentScoreData.value.salesScore || 0, max: 300, desc: '成交金额、成交数量、销售增长率' },
  { name: '市场影响力', value: currentScoreData.value.influenceScore || 0, max: 200, desc: '关注、收藏、浏览、分享' },
  { name: '活跃度', value: currentScoreData.value.activityScore || 0, max: 100, desc: '上新、登录、互动' },
  { name: '作品信息完整度', value: currentScoreData.value.qualityScore || 0, max: 50, desc: '作品封面、描述、尺寸、年份、材质等' },
  { name: '藏家评价', value: currentScoreData.value.reviewScore || 0, max: 100, desc: '评价、复购、评论质量' },
  { name: '学术资质', value: currentScoreData.value.academicScore || 0, max: 200, desc: '美院、职称、协会、展览、获奖' },
  { name: '互联网资质', value: currentScoreData.value.internetScore || 0, max: 50, desc: '艺术博主身份、粉丝、内容质量、转化' },
  { name: '手动调整', value: currentScoreData.value.adjustmentScore || 0, max: 1000, desc: '人工调分累积值' }
]

const getSourceText = (source) => {
  const map = { wx: '微信小程序', app: '手机应用', h5: 'H5页面', admin: '后台导入', weixin: '微信小程序' }
  return map[source] || source || '未知'
}

const formatAmount = (value) => {
  return (Number(value || 0) / 100).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 打开用户资料弹窗
const openUserProfile = async (row) => {
  currentRecord.value = row
  const userId = row.userId || row.id
  currentUser.value = { ...row, userId, displayId: row.displayId || String(userId).padStart(4, '0'), isArtist: row.certified || row.status === 1, isPromoter: false }

  try {
    const detail = await request.get(`/user/${userId}`)
    if (detail) {
      const savedDisplayId = currentUser.value.displayId
      currentUser.value = { ...currentUser.value, ...detail }
      currentUser.value.displayId = savedDisplayId
      currentUser.value.userId = userId
    }
  } catch (e) { /* ignore */ }

  const data = currentUser.value
  Object.assign(profileForm, {
    nickname: data.nickname || data.userNickname || '',
    phone: data.phone || data.userPhone || '',
    email: data.email || '',
    avatar: data.avatar || data.userAvatar || '',
    homepageCover: data.homepageCover || '',
    identities: Array.isArray(data.identities) ? data.identities : (data.certified ? ['artist'] : []),
    realName: data.realName || '',
    idCard: data.idCard || '',
    artistTitle: data.artistTitle || '',
    artistTags: Array.isArray(data.artistTags) ? data.artistTags.join(', ') : (data.artistTags || ''),
    resume: data.resume || data.bio || ''
  })

  const artistId = row.id
  if (artistId) {
    try { currentScoreData.value = (await getArtistScoreDetail(artistId)) || {} } catch (e) { currentScoreData.value = {} }
    try {
      const identityData = await getIdentityDetail(artistId)
      currentIdentityData.value = identityData || {}
      identityAuditRemark.value = identityData?.auditRemark || ''
    } catch (e) { currentIdentityData.value = {} }
  }

  artworksPage.value = 1
  userArtworks.value = { list: [], total: 0 }
  await loadUserArtworks(userId)

  detailActiveTab.value = 'info'
  internalVisible.value = true
}

// 加载用户作品
const loadUserArtworks = async (userId) => {
  artworksLoading.value = true
  try {
    const res = await request.get(`/user/${userId}/artworks`, { params: { page: artworksPage.value, size: artworksSize } })
    if (artworksPage.value === 1) {
      userArtworks.value = { list: res.list || [], total: res.total || 0 }
    } else {
      userArtworks.value.list = [...userArtworks.value.list, ...(res.list || [])]
    }
  } catch (e) { console.error('加载作品失败', e) }
  finally { artworksLoading.value = false }
}

const loadMoreArtworks = () => {
  const userId = currentUser.value.userId
  if (!userId) return
  artworksPage.value++
  loadUserArtworks(userId)
}

// 上传头像
const handleAvatarUpload = async (options) => {
  const { file, onSuccess, onError } = options
  if (!file.type.startsWith('image/')) { ElMessage.error('请选择图片文件'); onError(new Error('请选择图片文件')); return }
  if (file.size > 10 * 1024 * 1024) { ElMessage.error('图片大小不能超过 10MB'); onError(new Error('图片大小不能超过 10MB')); return }
  try {
    const result = await uploadFile(file)
    profileForm.avatar = result?.url || result || ''
    ElMessage.success('头像上传成功')
    onSuccess()
  } catch (e) { ElMessage.error(e.message || '头像上传失败'); onError(e) }
}

const handleHomepageCoverUpload = async (options) => {
  const { file, onSuccess, onError } = options
  if (!file.type.startsWith('image/')) { ElMessage.error('请选择图片文件'); onError(new Error('请选择图片文件')); return }
  if (file.size > 10 * 1024 * 1024) { ElMessage.error('图片大小不能超过 10MB'); onError(new Error('图片大小不能超过 10MB')); return }
  try {
    const result = await uploadFile(file)
    profileForm.homepageCover = result?.url || result || ''
    ElMessage.success('主页头图上传成功')
    onSuccess()
  } catch (e) { ElMessage.error(e.message || '主页头图上传失败'); onError(e) }
}

// 保存用户资料
const saveProfile = async () => {
  try {
    editLoading.value = true
    const userId = currentUser.value.userId || currentUser.value.id
    await request.put(`/user/${userId}`, {
      nickname: profileForm.nickname, avatar: profileForm.avatar, phone: profileForm.phone,
      email: profileForm.email, identities: profileForm.identities, realName: profileForm.realName,
      idCard: profileForm.idCard, artistTitle: profileForm.artistTitle, artistTags: profileForm.artistTags,
      homepageCover: profileForm.homepageCover, resume: profileForm.resume
    })
    internalVisible.value = false
    ElMessage.success('保存成功')
    emit('saved')
  } catch (e) { ElMessage.error('保存失败：' + (e.message || '未知错误')) }
  finally { editLoading.value = false }
}

const handleProfileFormKeydown = (event) => {
  if (event.key !== 'Enter' || detailActiveTab.value !== 'info' || editLoading.value) return
  const target = event.target
  if (target?.tagName?.toLowerCase() === 'textarea' || target?.classList?.contains('el-textarea__inner')) return
  event.preventDefault()
  saveProfile()
}

// 评分相关
const recalculateScore = async (row) => {
  if (!row?.id) { ElMessage.warning('无艺术家ID'); return }
  try {
    await recalculateArtistScore(row.id)
    const scoreData = await getArtistScoreDetail(row.id)
    currentScoreData.value = scoreData || {}
    ElMessage.success('评分已重新计算')
  } catch (e) { ElMessage.error('重算评分失败') }
}

const openScoreAdjust = () => {
  const artistId = currentRecord.value.id
  if (!artistId) { ElMessage.warning('无艺术家ID'); return }
  scoreAdjustForm.value = { artistId, adjustScore: currentScoreData.value.adjustmentScore || 0, reason: '' }
  scoreAdjustVisible.value = true
}

const submitScoreAdjust = async () => {
  if (!scoreAdjustForm.value.reason) { ElMessage.warning('请填写调整原因'); return }
  try {
    const oldAdjust = currentScoreData.value.adjustmentScore || 0
    const newAdjust = scoreAdjustForm.value.adjustScore
    const delta = newAdjust - oldAdjust
    if (delta === 0) { ElMessage.warning('调分值未发生变化'); return }
    await manualAdjustArtistScore({ artistId: scoreAdjustForm.value.artistId, adjustScore: delta, reason: scoreAdjustForm.value.reason })
    ElMessage.success('人工调分成功')
    scoreAdjustVisible.value = false
    const scoreData = await getArtistScoreDetail(scoreAdjustForm.value.artistId)
    currentScoreData.value = scoreData || {}
  } catch (e) { /* ignore */ }
}

// 身份信息
const saveIdentityData = async () => {
  try {
    await saveArtistIdentity(currentIdentityData.value)
    ElMessage.success('身份信息已保存')
    const artistId = currentRecord.value.id
    if (artistId) {
      await recalculateArtistScore(artistId)
      const scoreData = await getArtistScoreDetail(artistId)
      currentScoreData.value = scoreData || {}
    }
  } catch (e) { /* ignore */ }
}

const auditIdentityInDetail = async (status) => {
  const artistId = currentRecord.value.id
  if (!artistId) return
  try {
    await auditArtistIdentity({ artistId, auditStatus: status, auditRemark: identityAuditRemark.value })
    if (status === 'PASS') {
      await recalculateArtistScore(artistId)
      ElMessage.success('审核通过，评分已重算')
    } else { ElMessage.success('已驳回') }
    internalVisible.value = false
  } catch (e) { /* ignore */ }
}

// 取消认证
const handleRevokeFromDetail = async () => {
  const artistId = currentRecord.value.id
  if (!artistId) return
  try {
    await request.post(`/user/artist/revoke`, { id: artistId })
    ElMessage.success('已取消认证')
    internalVisible.value = false
  } catch (e) { ElMessage.error('取消认证失败') }
}

const onClose = () => {
  internalVisible.value = false
  emit('close')
}

// 监听 visible prop
watch(() => props.visible, (val) => {
  if (val) {
    openUserProfile({ ...props.initialData, id: props.userId || props.initialData.id, userId: Number(props.userId) || props.initialData.userId })
  }
})

// 也监听初始数据变化
watch(() => props.userId, (newVal) => {
  if (newVal && props.visible) {
    openUserProfile({ ...props.initialData, id: Number(newVal), userId: Number(newVal) })
  }
})
</script>

<style scoped>
.profile-header { display: flex; gap: 20px; margin-bottom: 20px; }
.avatar-wrapper { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.profile-info h3 { margin: 0 0 8px; font-size: 18px; display: flex; align-items: center; gap: 8px; }
.user-id { color: #909399; font-size: 12px; margin: 4px 0; }
.identity-tags { display: flex; gap: 4px; margin-top: 4px; }
.cover-preview { width: 200px; height: 100px; border-radius: 4px; margin-bottom: 8px; display: block; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-item .label { font-size: 12px; color: #909399; }
.info-item .value { font-size: 14px; font-weight: 500; color: #303133; }
.artwork-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.artwork-item { border: 1px solid #ebeef5; border-radius: 4px; overflow: hidden; }
.artwork-cover-wrapper { width: 100%; height: 140px; background: #f5f7fa; }
.artwork-cover { width: 100%; height: 140px; }
.artwork-cover-empty { display: flex; align-items: center; justify-content: center; width: 100%; height: 140px; background: #f5f7fa; }
.artwork-info { padding: 8px; }
.artwork-title { margin: 0; font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.artwork-price { margin: 4px 0 0; color: #e6a23c; font-size: 14px; font-weight: 600; }
.level-box { display: flex; flex-direction: column; gap: 4px; }
.level-label { font-size: 12px; color: #909399; }
.load-more { text-align: center; margin-top: 16px; }
.dialog-footer-between { display: flex; justify-content: space-between; align-items: center; width: 100%; }
</style>
