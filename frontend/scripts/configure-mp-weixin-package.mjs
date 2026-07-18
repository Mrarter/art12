import fs from 'node:fs'
import path from 'node:path'

const projectConfigPath = path.resolve('dist/build/mp-weixin/project.config.json')
const projectConfig = JSON.parse(fs.readFileSync(projectConfigPath, 'utf8'))

projectConfig.packOptions = projectConfig.packOptions || {}
const ignoredValues = new Set(['static/landing', 'static/favicon.png', 'static/images/.DS_Store'])
projectConfig.packOptions.ignore = [
  ...(projectConfig.packOptions.ignore || []).filter(item => !ignoredValues.has(item?.value)),
  { type: 'folder', value: 'static/landing' },
  { type: 'file', value: 'static/favicon.png' },
  { type: 'file', value: 'static/images/.DS_Store' }
]

fs.writeFileSync(projectConfigPath, `${JSON.stringify(projectConfig, null, 2)}\n`)
console.log('Excluded H5-only and metadata assets from the WeChat package')
