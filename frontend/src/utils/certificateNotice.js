import { getCurrentUserIdentity } from '@/utils/auth'

const STORAGE_KEY = 'certificateSignNotices'

function readAllNotices() {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    return Array.isArray(raw) ? raw : []
  } catch (error) {
    console.warn('[certificate-notice] 读取通知失败:', error)
    return []
  }
}

function writeAllNotices(notices) {
  try {
    uni.setStorageSync(STORAGE_KEY, notices)
  } catch (error) {
    console.warn('[certificate-notice] 写入通知失败:', error)
  }
}

function normalizeUserId(userId) {
  return String(userId || '').trim()
}

function buildNoticeKey(payload = {}) {
  return [
    'certificate-sign',
    payload.artworkId || '',
    payload.holderId || '',
    payload.tradeStage || 1
  ].join(':')
}

export function upsertCertificateSignNotice(payload = {}) {
  const userId = normalizeUserId(payload.userId)
  const artworkId = String(payload.artworkId || '').trim()
  if (!userId || !artworkId) return null

  const tradeStage = Number(payload.tradeStage || 1)
  const noticeKey = buildNoticeKey({ ...payload, tradeStage })
  const notices = readAllNotices()
  const existingIndex = notices.findIndex(item => item.noticeKey === noticeKey && normalizeUserId(item.userId) === userId)
  const title = tradeStage > 1 ? '作品再次收藏，待签署证书' : '作品已被收藏，待签署证书'
  const content = tradeStage > 1
    ? `《${payload.artworkTitle || '该作品'}》已完成再次收藏，请尽快签署收藏证书，签署后可获得认证费用。`
    : `《${payload.artworkTitle || '该作品'}》已被收藏，请尽快签署收藏证书，签署后可获得认证费用。`

  const nextNotice = {
    id: existingIndex > -1 ? notices[existingIndex].id : `cert-${Date.now()}-${artworkId}`,
    noticeKey,
    noticeType: 'certificate_sign',
    type: 'system',
    userId,
    artworkId,
    artworkTitle: payload.artworkTitle || '未命名作品',
    certificateCode: payload.certificateCode || '',
    collectorName: payload.collectorName || '',
    holderId: String(payload.holderId || ''),
    tradeStage,
    createTime: existingIndex > -1 ? notices[existingIndex].createTime : Date.now(),
    updateTime: Date.now(),
    isRead: existingIndex > -1 ? notices[existingIndex].isRead : false,
    title,
    content,
    tags: ['收藏证书', tradeStage > 1 ? '再次收藏' : '首次收藏'],
    link: `/pages/gallery/detail?id=${artworkId}`
  }

  if (existingIndex > -1) notices.splice(existingIndex, 1, nextNotice)
  else notices.unshift(nextNotice)
  writeAllNotices(notices)
  return nextNotice
}

export function getUserCertificateSignNotices(userId = getCurrentUserIdentity().id) {
  const targetUserId = normalizeUserId(userId)
  if (!targetUserId) return []
  return readAllNotices()
    .filter(item => normalizeUserId(item.userId) === targetUserId)
    .sort((a, b) => Number(b.updateTime || b.createTime || 0) - Number(a.updateTime || a.createTime || 0))
}

export function getUnreadCertificateSignNoticeCount(userId = getCurrentUserIdentity().id) {
  return getUserCertificateSignNotices(userId).filter(item => !item.isRead).length
}

export function markCertificateSignNoticeRead(noticeId, userId = getCurrentUserIdentity().id) {
  const targetUserId = normalizeUserId(userId)
  if (!noticeId || !targetUserId) return
  const notices = readAllNotices()
  const next = notices.map(item => {
    if (item.id !== noticeId || normalizeUserId(item.userId) !== targetUserId) return item
    return { ...item, isRead: true, updateTime: Date.now() }
  })
  writeAllNotices(next)
}

export function markCertificateSignNoticesReadByArtwork(artworkId, userId = getCurrentUserIdentity().id) {
  const targetUserId = normalizeUserId(userId)
  const targetArtworkId = String(artworkId || '').trim()
  if (!targetUserId || !targetArtworkId) return
  const notices = readAllNotices()
  const next = notices.map(item => {
    if (String(item.artworkId || '') !== targetArtworkId || normalizeUserId(item.userId) !== targetUserId) return item
    return { ...item, isRead: true, updateTime: Date.now() }
  })
  writeAllNotices(next)
}

export function removeCertificateSignNoticesByArtwork(artworkId, userId = getCurrentUserIdentity().id) {
  const targetUserId = normalizeUserId(userId)
  const targetArtworkId = String(artworkId || '').trim()
  if (!targetUserId || !targetArtworkId) return
  const next = readAllNotices().filter(item => {
    return !(String(item.artworkId || '') === targetArtworkId && normalizeUserId(item.userId) === targetUserId)
  })
  writeAllNotices(next)
}
