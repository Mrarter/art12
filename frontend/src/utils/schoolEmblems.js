const ELITE_ART_SCHOOLS = [
  '中央美术学院',
  '中国美术学院',
  '清华大学美术学院',
  '清华美院',
  '鲁迅美术学院',
  '广州美术学院',
  '四川美术学院',
  '西安美术学院',
  '天津美术学院',
  '湖北美术学院'
]

const STRONG_ART_SCHOOLS = [
  '南京艺术学院',
  '山东艺术学院',
  '吉林艺术学院',
  '广西艺术学院',
  '云南艺术学院',
  '新疆艺术学院',
  '上海大学美术学院',
  '首都师范大学美术学院',
  '福建师范大学美术学院'
]

const RANKED_ART_SCHOOLS = [
  '北京服装学院',
  '浙江师范大学',
  '湖南师范大学',
  '华东师范大学',
  '华南师范大学',
  '东北师范大学',
  '陕西师范大学',
  '西南大学',
  '浙江大学',
  '同济大学'
]

const SCHOOL_ALIASES = {
  清华美院: '清华大学美术学院'
}

const TIER_THEME = {
  elite: {
    tierClass: 'elite',
    color: '#d8b35f',
    softColor: '#f2d072',
    opacity: 0.78,
    shine: true
  },
  strong: {
    tierClass: 'strong',
    color: '#bda36e',
    softColor: '#d7bf86',
    opacity: 0.62,
    shine: false
  },
  ranked: {
    tierClass: 'ranked',
    color: '#9f987e',
    softColor: '#c7bd93',
    opacity: 0.48,
    shine: false
  },
  normal: {
    tierClass: 'normal',
    color: '#858073',
    softColor: '#a99f82',
    opacity: 0.38,
    shine: false
  }
}

function escapeXml(value = '') {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function svgToDataUrl(svg) {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function normalizeSchoolName(name = '') {
  return SCHOOL_ALIASES[name] || name
}

function findSchoolName(text = '') {
  const normalizedText = String(text)
  const allSchools = [...ELITE_ART_SCHOOLS, ...STRONG_ART_SCHOOLS, ...RANKED_ART_SCHOOLS]
  const matched = allSchools.find(name => normalizedText.includes(name))
  if (matched) return normalizeSchoolName(matched)
  const fallback = normalizedText.match(/([\u4e00-\u9fa5]{2,}(?:美术学院|艺术学院|大学|学院))/)
  return normalizeSchoolName(fallback?.[1] || '')
}

function resolveTier(schoolName = '') {
  if (ELITE_ART_SCHOOLS.some(name => normalizeSchoolName(name) === schoolName)) return 'elite'
  if (STRONG_ART_SCHOOLS.includes(schoolName)) return 'strong'
  if (RANKED_ART_SCHOOLS.includes(schoolName)) return 'ranked'
  return 'normal'
}

function buildSchoolSealSvg(schoolName, theme) {
  const name = escapeXml(schoolName || '艺术院校')
  const shortName = name.length > 8 ? name.slice(0, 8) : name
  return `
<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240" fill="none">
  <defs>
    <radialGradient id="g" cx="50%" cy="44%" r="58%">
      <stop offset="0%" stop-color="${theme.softColor}" stop-opacity=".34"/>
      <stop offset="56%" stop-color="${theme.color}" stop-opacity=".12"/>
      <stop offset="100%" stop-color="${theme.color}" stop-opacity="0"/>
    </radialGradient>
    <filter id="glow" x="-40%" y="-40%" width="180%" height="180%">
      <feGaussianBlur stdDeviation="4" result="blur"/>
      <feMerge>
        <feMergeNode in="blur"/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>
  <circle cx="120" cy="120" r="108" fill="url(#g)" opacity=".78"/>
  <circle cx="120" cy="120" r="98" stroke="${theme.color}" stroke-width="3" opacity="${theme.opacity}" filter="url(#glow)"/>
  <circle cx="120" cy="120" r="78" stroke="${theme.color}" stroke-width="1.6" opacity="${theme.opacity * 0.75}"/>
  <path d="M58 128h124M75 128V94l45-24 45 24v34M91 128V99h18v29M131 128V99h18v29" stroke="${theme.color}" stroke-width="4.2" stroke-linecap="round" stroke-linejoin="round" opacity="${theme.opacity}"/>
  <path d="M84 151h72M96 166h48" stroke="${theme.color}" stroke-width="4" stroke-linecap="round" opacity="${theme.opacity * 0.8}"/>
  <text x="120" y="50" text-anchor="middle" fill="${theme.color}" font-size="16" font-family="serif" font-weight="700" opacity="${theme.opacity}">${shortName}</text>
  <text x="120" y="202" text-anchor="middle" fill="${theme.color}" font-size="13" font-family="serif" font-weight="700" letter-spacing="2" opacity="${theme.opacity * 0.75}">FINE ARTS</text>
</svg>`.trim()
}

export function resolveSchoolEmblem(text = '') {
  const schoolName = findSchoolName(text)
  if (!schoolName) return null
  const tier = resolveTier(schoolName)
  const theme = TIER_THEME[tier]
  return {
    schoolName,
    tier,
    tierClass: theme.tierClass,
    shine: theme.shine,
    src: svgToDataUrl(buildSchoolSealSvg(schoolName, theme))
  }
}

export function resolveSchoolEmblemFromEntries(entries = []) {
  return resolveSchoolEmblem(entries.map(entry => `${entry.primary || ''} ${entry.secondary || ''}`).join(' '))
}
