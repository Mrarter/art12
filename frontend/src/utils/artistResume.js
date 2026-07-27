export const RESUME_SECTION_DEFINITIONS = [
  { key: 'education', title: '教育经历', icon: '/static/art-icons/icon-certificate.svg' },
  { key: 'exhibitions', title: '个展 / 联展', icon: '/static/art-icons/icon-gallery.svg' },
  { key: 'awards', title: '获奖经历', icon: '/static/art-icons/icon-star.svg' },
  { key: 'collections', title: '机构收藏', icon: '/static/art-icons/icon-trust.svg' },
  { key: 'media', title: '媒体报道', icon: '/static/art-icons/icon-document.svg' }
]

export function buildDefaultResumeEntries(name = '艺术家') {
  return {
    education: [
      { year: '2024', primary: '中国美术学院  油画系  硕士研究生', secondary: '导师：杨参军 教授' },
      { year: '2020', primary: '中国美术学院  油画系  本科', secondary: '学士学位' }
    ],
    exhibitions: [
      { year: '2024', primary: `个展 | “光与岸” ${name}油画作品展`, secondary: '杭州 · 艺空间' },
      { year: '2023', primary: '联展 | 青年艺术100年度展', secondary: '北京 · 今日美术馆' },
      { year: '2022', primary: '联展 | 西湖青年艺术展', secondary: '杭州 · 浙江美术馆' }
    ],
    awards: [
      { year: '2024', primary: '第三届“青年油画家提名展”  优秀奖', secondary: '' },
      { year: '2023', primary: '“未来之星”全国青年艺术大赛  银奖', secondary: '' },
      { year: '2022', primary: '浙江省大学生艺术展演  一等奖', secondary: '' }
    ],
    collections: [
      { year: '2024', primary: '浙江美术馆', secondary: '收藏作品《静物0751》' },
      { year: '2023', primary: '杭州西湖美术馆', secondary: '收藏作品《测试作品-已修复》' },
      { year: '2022', primary: '宁波美术馆', secondary: '收藏作品《小女孩》' }
    ],
    media: [
      { year: '2024', primary: '《美术观察》专访', secondary: `${name}的光影诗篇` },
      { year: '2023', primary: '雅昌艺术网专访', secondary: `青年艺术家${name}的创作之路` },
      { year: '2022', primary: '《艺术与设计》杂志', secondary: '新锐推荐' }
    ]
  }
}

export function parseArtistResume(rawValue) {
  if (!rawValue) return null
  if (typeof rawValue === 'object') return normalizeResumeConfig(rawValue)
  if (typeof rawValue !== 'string') return null
  const value = rawValue.trim()
  if (!value.startsWith('{')) return null
  try {
    return normalizeResumeConfig(JSON.parse(value))
  } catch (error) {
    console.warn('[artist-resume] 履历数据解析失败:', error)
    return null
  }
}

export function normalizeResumeConfig(value) {
  if (!value || typeof value !== 'object') return null
  const rawSections = value.sections
  if (!rawSections || typeof rawSections !== 'object' || Array.isArray(rawSections)) return null
  const sections = {}
  RESUME_SECTION_DEFINITIONS.forEach(({ key }) => {
    if (!Array.isArray(rawSections[key])) return
    sections[key] = rawSections[key]
      .map(normalizeResumeEntry)
      .filter(entry => entry.year || entry.primary || entry.secondary)
  })
  return { version: 1, sections }
}

export function normalizeResumeEntry(entry = {}) {
  return {
    year: String(entry.year || '').trim(),
    primary: String(entry.primary || '').trim(),
    secondary: String(entry.secondary || '').trim()
  }
}

export function serializeArtistResume(sections = {}) {
  return JSON.stringify(normalizeResumeConfig({ version: 1, sections }) || { version: 1, sections: {} })
}
