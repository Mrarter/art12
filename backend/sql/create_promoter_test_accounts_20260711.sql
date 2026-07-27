SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO users (
  uid, nickname, phone, password, identities, status,
  follower_count, following_count, register_time, last_login_time,
  create_time, update_time, deleted
)
SELECT
  CONCAT('USR20260711', LPAD(seq, 4, '0'), 'TST1'),
  CONCAT('分销测试经纪人', LPAD(seq, 2, '0')),
  CONCAT('13801138', LPAD(seq - 1, 3, '0')),
  SHA2(CONCAT('shiyiju:user:password:', 'Test@1380'), 256),
  'collector,promoter', 1, 0, 0, NOW(), NOW(), NOW(), NOW(), 0
FROM (
  SELECT 1 seq UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
) numbers
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  password = VALUES(password),
  identities = 'collector,promoter',
  status = 1,
  deleted = 0,
  update_time = NOW();

INSERT INTO promoter_record (
  user_id, invite_code, parent_id, level, team_size,
  total_orders, total_sales, status, sign_time, create_time, update_time
)
SELECT
  user.id,
  CONCAT('TEST138', LPAD(CAST(RIGHT(user.phone, 3) AS UNSIGNED), 3, '0')),
  NULL,
  CASE
    WHEN user.phone = '13801138000' THEN 4
    WHEN user.phone IN ('13801138001', '13801138005', '13801138006', '13801138007', '13801138008',
                        '13801138009', '13801138010', '13801138011', '13801138012', '13801138013') THEN 3
    WHEN user.phone IN ('13801138002', '13801138014', '13801138015', '13801138016') THEN 2
    ELSE 1
  END,
  0, 0, 0.00, 1, NOW(), NOW(), NOW()
FROM users user
WHERE user.phone BETWEEN '13801138000' AND '13801138024'
ON DUPLICATE KEY UPDATE
  invite_code = VALUES(invite_code),
  level = VALUES(level),
  status = 1,
  update_time = NOW();

-- 根节点的 10 人直属团队：8001 + 8005~8013。
UPDATE promoter_record child
JOIN users child_user ON child_user.id = child.user_id
JOIN users parent_user ON parent_user.phone = '13801138000'
SET child.parent_id = parent_user.id
WHERE child_user.phone IN (
  '13801138001', '13801138005', '13801138006', '13801138007', '13801138008',
  '13801138009', '13801138010', '13801138011', '13801138012', '13801138013'
);

-- 5 层主链及剩余成员分布。
UPDATE promoter_record child
JOIN users child_user ON child_user.id = child.user_id
JOIN users parent_user ON parent_user.phone = '13801138001'
SET child.parent_id = parent_user.id
WHERE child_user.phone IN ('13801138002', '13801138014', '13801138015', '13801138016');

UPDATE promoter_record child
JOIN users child_user ON child_user.id = child.user_id
JOIN users parent_user ON parent_user.phone = '13801138002'
SET child.parent_id = parent_user.id
WHERE child_user.phone IN ('13801138003', '13801138017', '13801138018', '13801138019');

UPDATE promoter_record child
JOIN users child_user ON child_user.id = child.user_id
JOIN users parent_user ON parent_user.phone = '13801138003'
SET child.parent_id = parent_user.id
WHERE child_user.phone IN ('13801138004', '13801138020', '13801138021', '13801138022');

UPDATE promoter_record child
JOIN users child_user ON child_user.id = child.user_id
JOIN users parent_user ON parent_user.phone = '13801138004'
SET child.parent_id = parent_user.id
WHERE child_user.phone IN ('13801138023', '13801138024');

UPDATE promoter_record promoter
SET promoter.team_size = (
  SELECT COUNT(*) FROM (
    SELECT child.parent_id FROM promoter_record child WHERE child.status = 1
  ) direct_children
  WHERE direct_children.parent_id = promoter.user_id
)
WHERE promoter.user_id IN (
  SELECT id FROM users WHERE phone BETWEEN '13801138000' AND '13801138024'
);

COMMIT;
