const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const tabbarDir = path.join(__dirname, '../src/static/tabbar');
const luxuryFiles = fs.readdirSync(tabbarDir).filter(f => f.includes('luxury') && f.endsWith('.svg'));

async function convertSvgToPng() {
  for (const file of luxuryFiles) {
    const svgPath = path.join(tabbarDir, file);
    const pngName = file.replace('.svg', '.png');
    const pngPath = path.join(tabbarDir, pngName);
    
    try {
      await sharp(svgPath)
        .resize(81, 81, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
        .png()
        .toFile(pngPath);
      console.log(`✓ Converted: ${file} -> ${pngName}`);
    } catch (err) {
      console.error(`✗ Failed: ${file}`, err.message);
    }
  }
}

convertSvgToPng();
