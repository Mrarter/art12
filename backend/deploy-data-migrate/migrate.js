const mysql = require('mysql2/promise');
(async () => {
  const conn = await mysql.createConnection({ host: process.env.DB_HOST || '127.0.0.1', port: process.env.DB_PORT || 3306, user: process.env.DB_USER || 'root', password: process.env.DB_PASS || '', database: process.env.DB_NAME || 'shiyiju' });
  for (const t of ['artwork', 'banner', 'artwork_category', 'artist_profile']) {
    await conn.execute(`DELETE FROM \`${t}\``);
    await conn.execute(`ALTER TABLE \`${t}\` AUTO_INCREMENT=1`);
  }
  await conn.execute("INSERT INTO artwork_category (id,name,weight,status) VALUES (1,'国画',1,1),(2,'油画',2,1),(3,'雕塑',3,1),(4,'书法',4,1),(5,'版画',5,1)");
  await conn.execute("INSERT INTO banner (title,image_url,sort,status) VALUES ('当代艺术展','/upload/images/banner1.jpg',1,1),('新锐艺术家专区','/upload/images/banner2.jpg',2,1),('收藏家专区','/upload/images/banner3.jpg',3,1)");
  for (const a of [[1,'张大千传人','李明','传承国画艺术','专注于中国传统山水画创作，作品被多家美术馆收藏。',3,1250,45,128,1],[2,'油画先锋','王芳','用色彩表达情感','当代抽象油画艺术家，作品远销海外。',4,3200,68,256,1],[3,'新锐画家','陈思','艺术是生活的调味品','90后新锐艺术家，作品充满活力。',1,580,23,35,0],[4,'水墨画家','刘伟','简约而不简单','水墨画专业硕士，专注写意山水。',2,890,41,78,0],[5,'当代艺术家','赵敏','艺术改变生活','知名当代艺术家，作品被各大拍卖行追捧。',5,5600,95,420,1]]) {
    await conn.execute("INSERT INTO artist_profile (id,artist_name,real_name,slogan,bio,level_id,follower_count,work_count,sale_count,is_signed) VALUES (?,?,?,?,?,?,?,?,?,?)", a);
  }
  const ws = [
    ['云山秋色',1,'张大千传人',1,'国画',8800,'/upload/images/artwork1.jpg','[]','金秋时节，云山缭绕，意境悠远。','ART00001'],
    ['溪山行旅图',1,'张大千传人',1,'国画',26800,'/upload/images/artwork2.jpg','[]','传统工笔山水，细节丰富。','ART00002'],
    ['都市狂想曲',2,'油画先锋',2,'油画',12800,'/upload/images/artwork3.jpg','[]','现代都市的喧嚣与激情。','ART00003'],
    ['暮色温柔',2,'油画先锋',2,'油画',9800,'/upload/images/artwork4.jpg','[]','夕阳下的城市剪影。','ART00004'],
    ['青春的色彩',3,'新锐画家',2,'油画',3800,'/upload/images/artwork5.jpg','[]','年轻人眼中的多彩世界。','ART00005'],
    ['烟雨江南',4,'水墨画家',1,'国画',6800,'/upload/images/artwork6.jpg','[]','烟雨蒙蒙的江南水乡。','ART00006'],
    ['时间的痕迹',5,'当代艺术家',2,'油画',95000,'/upload/images/artwork7.jpg','[]','当代艺术装置，探讨时间与记忆。','ART00007'],
    ['星空之下',5,'当代艺术家',2,'油画',38500,'/upload/images/artwork8.jpg','[]','仰望星空，思考人生。','ART00008']
  ];
  for (const w of ws) {
    await conn.execute("INSERT INTO artwork (title,author_id,author_name,category_id,category_name,price,cover_image,images,description,status,artwork_code,stock) VALUES (?,?,?,?,?,?,?,?,?,1,?,1)", w);
  }
  console.log('✅ 分类:5 Banner:3 艺术家:5 作品:8');
  const [r] = await conn.execute("SELECT TABLE_NAME,TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='shiyiju' AND TABLE_NAME IN ('artwork','banner','artwork_category','artist_profile')");
  for (const t of r) console.log(`  ${t.TABLE_NAME}: ${t.TABLE_ROWS} 条`);
  await conn.end();
})().catch(e => { console.error('失败:', e.message); process.exit(1); });
