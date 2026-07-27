const tcb = require('@cloudbase/node-sdk')
const mysql = require('mysql2/promise')

// ================== MySQL 配置 ==================
const MYSQL_CONFIG = {
  host: process.env.DB_HOST || '8.217.235.30',
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASS || '',
  database: process.env.DB_NAME || 'shiyiju'
}

// ================== 字段映射 ==================
/** artwork → artwork */
const artworkMapping = (doc) => ({
  title: doc.title || '',
  author_id: doc.authorId || doc.author_id || 0,
  author_name: doc.authorName || doc.author_name || '',
  author_uid: doc.authorUid || doc.author_uid || null,
  author_badge: doc.authorBadge || doc.author_badge || null,
  author_avatar: doc.authorAvatar || doc.author_avatar || null,
  category_id: doc.categoryId || doc.category_id || null,
  category_name: doc.categoryName || doc.category_name || null,
  cover_image: doc.coverImage || doc.cover_image || doc.cover || '',
  images: JSON.stringify(doc.images || doc.imageUrls || []),
  description: doc.description || doc.desc || '',
  price: doc.price || 0,
  original_price: doc.originalPrice || doc.original_price || null,
  status: doc.status !== undefined ? doc.status : 1,
  stock: doc.stock || 1,
  sale_count: doc.saleCount || doc.salesCount || doc.sales_count || 0,
  favorite_count: doc.favoriteCount || doc.favorite_count || 0,
  view_count: doc.viewCount || doc.view_count || 0,
  tags: doc.tags ? (Array.isArray(doc.tags) ? doc.tags.join(',') : doc.tags) : null,
  artwork_code: doc.artworkCode || doc.artwork_code || null,
  size: doc.size || null,
  material: doc.material || null,
  create_year: doc.createYear || doc.year || null,
  art_type: doc.artType || doc.art_type || null,
  style: doc.style || null,
  edition: doc.edition || null,
  region: doc.region || null,
  distributable: doc.distributable !== undefined ? (doc.distributable ? 1 : 0) : 1,
  commission_rate: doc.commissionRate || doc.commission_rate || 0.0500
})

/** shop_home_swiper_image → banner */
const swiperMapping = (doc) => ({
  title: doc.name || doc.title || '',
  image_url: doc.imageUrl || doc.image || doc.picUrl || '',
  link_type: doc.linkType || doc.link_type || null,
  link_value: doc.linkValue || doc.link_value || doc.url || null,
  sort: doc.sort || doc.sort_order || doc.weight || 0,
  status: doc.status !== undefined ? doc.status : 1,
  start_time: doc.startTime || doc.start_time || null,
  end_time: doc.endTime || doc.end_time || null
})

/** shop_spu_cate → artwork_category */
const categoryMapping = (doc) => ({
  id: doc.id || 0,
  name: doc.name || '',
  icon: doc.icon || null,
  weight: doc.weight || doc.sort || doc.sort_order || 0,
  status: doc.status !== undefined ? doc.status : 1,
  parent_id: doc.parentId || doc.parent_id || null
})

/** user_account → sys_user */
const userMapping = (doc) => ({
  openid: doc.openid || doc.openId || doc.open_id || '',
  nickname: doc.nickname || doc.nickName || doc.name || '',
  avatar_url: doc.avatarUrl || doc.avatar || doc.headImg || '',
  phone: doc.phone || doc.mobile || null,
  status: doc.status !== undefined ? doc.status : 1
})

// ================== 读取 NoSQL 并插入 MySQL ==================
async function migrateCollection(tcbDb, mysqlConn, collectionName, mysqlTable, mapFn, idField) {
  console.log(`\n=== 迁移: ${collectionName} → ${mysqlTable} ===`)
  
  const collection = tcbDb.collection(collectionName)
  const countResult = await collection.count()
  const total = countResult.total
  console.log(`NoSQL 中 ${collectionName} 共 ${total} 条记录`)
  
  if (total === 0) {
    console.log(`跳过空集合 ${collectionName}`)
    return
  }

  // 分页查询
  const limit = 5
  let offset = 0
  let inserted = 0
  let errors = 0

  while (offset < total) {
    try {
      const { data: docs } = await collection
        .where({})
        .skip(offset)
        .limit(limit)
        .get()

      for (const doc of docs) {
        try {
          const row = mapFn(doc)
          
          // 检查是否已存在
          if (idField && doc[idField]) {
            const [existing] = await mysqlConn.execute(
              `SELECT id FROM \`${mysqlTable}\` WHERE \`${idField}\` = ? LIMIT 1`,
              [doc[idField]]
            )
            if (existing.length > 0) {
              console.log(`  ⏭️ 跳过已存在的记录: ${idField}=${doc[idField]}`)
              continue
            }
          }

          const keys = Object.keys(row)
          const values = Object.values(row)
          const placeholders = keys.map(() => '?').join(',')
          const columns = keys.map(k => '`' + k + '`').join(',')

          await mysqlConn.execute(
            `INSERT INTO \`${mysqlTable}\` (${columns}) VALUES (${placeholders})`,
            values
          )
          inserted++
          console.log(`  ✅ 插入成功: ${doc.name || doc.title || doc._id}`)
        } catch (err) {
          errors++
          console.error(`  ❌ 插入失败: ${doc._id} - ${err.message}`)
        }
      }
    } catch (err) {
      errors++
      console.error(`  ❌ 分页查询失败(offset=${offset}): ${err.message}`)
    }
    offset += limit
  }

  console.log(`结果: 成功=${inserted}, 失败=${errors}, 总计=${total}`)
  return { inserted, errors, total }
}

// ================== 主函数 ==================
exports.main = async function(event, context) {
  console.log('===== 开始数据迁移 =====')
  
  // 初始化 TCB
  const tcbApp = tcb.init({ context, keepalive: true })
  const db = tcbApp.database()
  
  let mysqlConn = null
  
  try {
    // 连接 MySQL
    mysqlConn = await mysql.createConnection(MYSQL_CONFIG)
    console.log('✅ 已连接 MySQL')
    
    // 按依赖顺序迁移
    const results = []
    
    // 1. 用户
    results.push(await migrateCollection(db, mysqlConn, 'user_account', 'sys_user', userMapping, 'openid'))
    
    // 2. 分类
    results.push(await migrateCollection(db, mysqlConn, 'shop_spu_cate', 'artwork_category', categoryMapping, 'id'))
    
    // 3. Banner
    results.push(await migrateCollection(db, mysqlConn, 'shop_home_swiper_image', 'banner', swiperMapping, null))
    
    // 4. 作品
    results.push(await migrateCollection(db, mysqlConn, 'artwork', 'artwork', artworkMapping, null))
    
    // 5. 艺术家
    results.push(await migrateCollection(db, mysqlConn, 'artist_profile', 'artist_profile', (doc) => doc, null))
    
    console.log('\n===== 迁移完成 =====')
    return { success: true, results }
    
  } catch (err) {
    console.error('迁移失败:', err.message)
    return { success: false, error: err.message }
  } finally {
    if (mysqlConn) await mysqlConn.end()
  }
}
