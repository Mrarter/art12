const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '..')
const publicDir = path.join(projectRoot, 'public')
const outputDir = path.join(projectRoot, 'dist', 'build', 'h5')

if (!fs.existsSync(publicDir) || !fs.existsSync(outputDir)) {
  process.exit(0)
}

for (const entryName of fs.readdirSync(publicDir)) {
  const sourcePath = path.join(publicDir, entryName)
  const targetPath = path.join(outputDir, entryName)
  const stat = fs.statSync(sourcePath)

  if (stat.isDirectory()) {
    fs.cpSync(sourcePath, targetPath, { recursive: true })
    console.log(`Copied directory ${entryName} to H5 root`)
    continue
  }

  fs.copyFileSync(sourcePath, targetPath)
  console.log(`Copied ${entryName} to H5 root`)
}
