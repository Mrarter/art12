/**
 * ID工具函数
 * 用于处理19位标准化ID的显示和操作
 */

/**
 * 格式化ID显示（截断显示，保留完整ID可复制）
 * @param {string} id - 原始ID
 * @param {number} visibleLength - 显示长度，默认8
 * @returns {object} { display, full, copyText }
 */
export function formatId(id, visibleLength = 8) {
  if (!id) return { display: '-', full: '', copyText: '' }
  
  const full = String(id)
  const display = full.length > visibleLength 
    ? full.slice(0, visibleLength) + '...' 
    : full
    
  return { display, full, copyText: full }
}

/**
 * 验证ID格式是否合法
 * @param {string} id - 待验证的ID
 * @returns {boolean}
 */
export function isValidId(id) {
  if (!id || typeof id !== 'string') return false
  if (id.length !== 19) return false
  
  // 检查日期部分
  const dateStr = id.substring(3, 11)
  const datePattern = /^\d{8}$/
  if (!datePattern.test(dateStr)) return false
  
  // 检查后8位是否为字母或数字
  const suffix = id.substring(11)
  const suffixPattern = /^[A-Z0-9]{8}$/
  return suffixPattern.test(suffix)
}

/**
 * 从ID中提取类型描述
 * @param {string} id - ID
 * @returns {string}
 */
export function getIdType(id) {
  if (!id || id.length < 3) return '未知'
  
  const prefix = id.substring(0, 3)
  const typeMap = {
    'USR': '用户',
    'SES': '专场',
    'LOT': '拍品',
    'POST': '帖子',
    'CMT': '评论',
    'TOP': '话题',
    'WTH': '提现',
    'COM': '佣金',
    'BID': '竞拍',
    'ASM': '售后',
    'ART': '作品'
  }
  
  return typeMap[prefix] || '未知'
}

/**
 * 从ID中提取日期
 * @param {string} id - ID
 * @returns {string}
 */
export function getIdDate(id) {
  if (!id || id.length < 11) return ''
  return id.substring(3, 11)
}

/**
 * 生成简短的显示标签（类型+日期）
 * @param {string} id - ID
 * @returns {string}
 */
export function getIdTag(id) {
  if (!id) return ''
  const type = getIdType(id)
  const date = getIdDate(id)
  return `${type} · ${date}`
}

/**
 * ID列配置生成器（用于Element Plus Table）
 * @param {string} prop - 属性名
 * @param {string} label - 列标签
 * @param {number} width - 列宽
 * @returns {object} 列配置
 */
export function createIdColumn(prop = 'id', label = 'ID', width = 180) {
  return {
    prop,
    label,
    width,
    showOverflowTooltip: true,
    formatter: (row) => {
      const id = row[prop]
      if (!id) return '-'
      return id
    }
  }
}

/**
 * 复制ID到剪贴板
 * @param {string} id - 要复制的ID
 * @param {Function} successCallback - 成功回调
 * @param {Function} errorCallback - 失败回调
 */
export async function copyId(id, successCallback, errorCallback) {
  if (!id) {
    errorCallback?.('ID为空')
    return
  }
  
  try {
    await navigator.clipboard.writeText(String(id))
    successCallback?.('已复制ID')
  } catch (e) {
    // 降级处理
    const textarea = document.createElement('textarea')
    textarea.value = String(id)
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    
    try {
      document.execCommand('copy')
      successCallback?.('已复制ID')
    } catch (err) {
      errorCallback?.('复制失败')
    }
    
    document.body.removeChild(textarea)
  }
}

// 导出类型映射表供其他地方使用
export const ID_TYPE_MAP = {
  'USR': { name: '用户', color: 'primary' },
  'SES': { name: '专场', color: 'success' },
  'LOT': { name: '拍品', color: 'warning' },
  'POST': { name: '帖子', color: 'info' },
  'CMT': { name: '评论', color: '' },
  'TOP': { name: '话题', color: 'danger' },
  'WTH': { name: '提现', color: 'success' },
  'COM': { name: '佣金', color: 'warning' },
  'BID': { name: '竞拍', color: 'info' },
  'ASM': { name: '售后', color: 'danger' },
  'ART': { name: '作品', color: 'primary' }
}
