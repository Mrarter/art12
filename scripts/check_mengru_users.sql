-- ========================================
-- 查看所有"孟儒"用户及相关数据
-- ========================================

-- 1. 查看所有"孟儒"用户（user_account 表）
SELECT 
    'user_account' AS source_table,
    u.id,
    u.uid,
    u.nickname,
    u.identity,
    u.status,
    u.created_at,
    u.update_time
FROM user_account u
WHERE u.nickname = '孟儒'
ORDER BY u.id;

-- 2. 查看 artist_profile 表中的"孟儒"
SELECT 
    'artist_profile' AS source_table,
    ap.id,
    ap.user_id,
    ap.user_uid,
    ap.real_name,
    ap.artist_name,
    ap.artist_code,
    ap.status,
    ap.created_at
FROM artist_profile ap
WHERE ap.real_name = '孟儒' OR ap.artist_name = '孟儒'
ORDER BY ap.id;

-- 3. 查看 artist_certifications 表中的"孟儒"
SELECT 
    'artist_certifications' AS source_table,
    ac.id,
    ac.user_id,
    ac.real_name,
    ac.artist_code,
    ac.status,
    ac.create_time
FROM artist_certifications ac
WHERE ac.real_name = '孟儒'
ORDER BY ac.id;

-- 4. 查看 artwork 表中作者为"孟儒"的作品
SELECT 
    a.id,
    a.title,
    a.author_id,
    a.author_name,
    a.author_uid,
    a.status,
    a.create_time
FROM artwork a
WHERE a.author_name = '孟儒'
ORDER BY a.id;

-- 5. 汇总：用户及其艺术家档案关联
SELECT 
    u.id AS user_id,
    u.uid AS user_uid,
    u.nickname,
    u.identity,
    ap.id AS profile_id,
    ap.real_name AS profile_real_name,
    ap.artist_code AS profile_artist_code,
    ac.id AS cert_id,
    ac.real_name AS cert_real_name,
    ac.artist_code AS cert_artist_code,
    (SELECT COUNT(*) FROM artwork aw WHERE aw.author_id = u.id) AS artwork_count
FROM user_account u
LEFT JOIN artist_profile ap ON ap.user_id = u.id
LEFT JOIN artist_certifications ac ON ac.user_id = u.id
WHERE u.nickname = '孟儒'
ORDER BY u.id;

-- ========================================
-- 统计"孟儒"用户的总数
-- ========================================
SELECT 
    'user_account' AS table_name,
    COUNT(*) AS count
FROM user_account 
WHERE nickname = '孟儒'
UNION ALL
SELECT 
    'artist_profile' AS table_name,
    COUNT(*) AS count
FROM artist_profile 
WHERE real_name = '孟儒' OR artist_name = '孟儒'
UNION ALL
SELECT 
    'artist_certifications' AS table_name,
    COUNT(*) AS count
FROM artist_certifications 
WHERE real_name = '孟儒'
UNION ALL
SELECT 
    'artwork' AS table_name,
    COUNT(*) AS count
FROM artwork 
WHERE author_name = '孟儒';
