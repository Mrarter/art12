#!/usr/bin/env python3
"""
修复用户头像脚本

功能：
1. 遍历 users / user_account 表，查找所有 avatar 为 NULL 或空字符串的用户
2. 使用指定的默认头像图片，调整尺寸至 200x200
3. 将调整后的图片保存到上传目录
4. 更新数据库中相应用户的 avatar 字段

用法：
  python fix_default_avatars.py \
    --source /path/to/default-avatar.png \
    --dest-dir /path/to/uploads/avatars \
    --cdn-url http://cdn.example.com

环境变量（可替代命令行参数）：
  DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
"""

import argparse
import os
import sys
from io import BytesIO

try:
    from PIL import Image
except ImportError:
    print("请先安装 Pillow: pip install Pillow")
    sys.exit(1)

try:
    import pymysql
except ImportError:
    print("请先安装 pymysql: pip install pymysql")
    sys.exit(1)

# ─── 配置 ────────────────────────────────────────────────────────────────────

AVATAR_SIZE = (200, 200)           # 目标尺寸
DRY_RUN = False                    # 设为 True 则只打印不修改

# ─── 数据库连接 ──────────────────────────────────────────────────────────────

def get_db_config(args):
    """从参数或环境变量读取数据库配置"""
    return {
        "host": os.getenv("DB_HOST", args.db_host or "127.0.0.1"),
        "port": int(os.getenv("DB_PORT", args.db_port or "3306")),
        "user": os.getenv("DB_USER", args.db_user or "root"),
        "password": os.getenv("DB_PASS", args.db_pass or "123456"),
        "database": os.getenv("DB_NAME", args.db_name or "shiyiju_local"),
        "charset": "utf8mb4",
    }


def connect_db(cfg):
    conn = pymysql.connect(**cfg, cursorclass=pymysql.cursors.DictCursor)
    print(f"[DB] 已连接：{cfg['host']}:{cfg['port']}/{cfg['database']}")
    return conn


# ─── 查询 ────────────────────────────────────────────────────────────────────

def find_empty_avatar_users(conn):
    """返回 users 和 user_account 中 avatar 为空的用户列表"""
    records = []

    for table, field in [("users", "avatar"), ("user_account", "avatar_url")]:
        try:
            sql = f"SELECT id, `{field}` FROM {table} WHERE `{field}` IS NULL OR TRIM(`{field}`) = ''"
            with conn.cursor() as cur:
                cur.execute(sql)
                rows = cur.fetchall()
            for row in rows:
                records.append({"table": table, "field": field, "id": row["id"], "old_value": row[field]})
            print(f"[DB] {table} 表：{len(rows)} 个用户缺少头像")
        except pymysql.err.ProgrammingError as e:
            print(f"[DB] {table} 表查询失败（可能不存在）：{e}")

    return records


# ─── 图片处理 ────────────────────────────────────────────────────────────────

def process_avatar(source_path, dest_dir, filename):
    """
    读取 source_path，缩放裁剪到 AVATAR_SIZE，保存到 dest_dir/filename
    返回 (保存路径, 是否成功)
    """
    try:
        img = Image.open(source_path).convert("RGBA")

        # 居中裁剪为正方形
        w, h = img.size
        side = min(w, h)
        left = (w - side) // 2
        top = (h - side) // 2
        img = img.crop((left, top, left + side, top + side))

        # 缩放到目标尺寸
        img = img.resize(AVATAR_SIZE, Image.LANCZOS)

        os.makedirs(dest_dir, exist_ok=True)
        dest_path = os.path.join(dest_dir, filename)

        # 保存为 PNG 保持透明（如有）
        img.save(dest_path, "PNG", optimize=True)

        actual_w, actual_h = img.size
        file_size = os.path.getsize(dest_path)
        print(f"  [IMG] 保存到 {dest_path}  ({actual_w}x{actual_h}, {file_size / 1024:.1f} KB)")

        return dest_path, True
    except Exception as e:
        print(f"  [IMG] 处理失败: {e}")
        return None, False


# ─── 更新数据库 ──────────────────────────────────────────────────────────────

def update_avatar(conn, table, field, user_id, new_url, dry_run=False):
    """更新指定用户的 avatar 字段"""
    sql = f"UPDATE {table} SET `{field}` = %s WHERE id = %s"
    if dry_run:
        print(f"  [DB] (DRY-RUN) 将更新 {table}.id={user_id}: {field} = {new_url}")
        return True

    try:
        with conn.cursor() as cur:
            cur.execute(sql, (new_url, user_id))
        conn.commit()
        print(f"  [DB] ✓ 已更新 {table}.id={user_id}")
        return True
    except Exception as e:
        conn.rollback()
        print(f"  [DB] ✗ 更新失败 {table}.id={user_id}: {e}")
        return False


# ─── 主流程 ──────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="修复用户默认头像")
    parser.add_argument("--source", required=True, help="默认头像源图片路径")
    parser.add_argument("--dest-dir", default="/Users/master/CodeBuddy/art12/uploads/avatars", help="处理后图片保存目录")
    parser.add_argument("--cdn-url", default="http://localhost:8087", help="CDN / 文件服务器 URL 前缀")
    parser.add_argument("--url-prefix", help="avatar 字段中使用的 URL 前缀（若不指定则自动拼接 cdn-url + 相对路径）")
    parser.add_argument("--db-host")
    parser.add_argument("--db-port")
    parser.add_argument("--db-name")
    parser.add_argument("--db-user")
    parser.add_argument("--db-pass")
    parser.add_argument("--dry-run", action="store_true", help="仅预览，不执行修改")
    parser.add_argument("--only-table", choices=["users", "user_account"], help="仅处理指定表")
    args = parser.parse_args()

    # 检查源图片
    if not os.path.isfile(args.source):
        print(f"[ERR] 源图片不存在: {args.source}")
        sys.exit(1)

    # 连接数据库
    cfg = get_db_config(args)
    conn = connect_db(cfg)

    try:
        # 1. 查询无头像用户
        all_users = find_empty_avatar_users(conn)

        if args.only_table:
            all_users = [u for u in all_users if u["table"] == args.only_table]

        if not all_users:
            print("[DONE] 没有需要修复的用户。")
            return

        print(f"\n共 {len(all_users)} 个用户需要设置默认头像。")

        # 2. 处理图片
        if args.dry_run:
            processed_path = "(预览模式，不保存图片)"
            url_prefix = args.url_prefix or f"{args.cdn_url}/avatars"
            filename = "default_avatar.png"
            print(f"[IMG] (DRY-RUN) 将处理: {args.source} → {args.dest_dir}/{filename} ({AVATAR_SIZE[0]}x{AVATAR_SIZE[1]})")
        else:
            # 生成唯一文件名（取源文件扩展名，无则用 .png）
            ext = os.path.splitext(args.source)[1] or ".png"
            filename = f"default_avatar{ext}"

            # 先清理已有的同名文件，确保所有人指向同一张图
            existing = os.path.join(args.dest_dir, filename)
            if os.path.exists(existing):
                os.remove(existing)
                print(f"[IMG] 移除旧文件: {existing}")

            dest, ok = process_avatar(args.source, args.dest_dir, filename)
            if not ok:
                print("[ERR] 图片处理失败，终止执行。")
                sys.exit(1)
            processed_path = dest

            url_prefix = args.url_prefix or f"{args.cdn_url}/avatars"

        avatar_url = f"{url_prefix}/{filename}"
        print(f"[URL] 头像 URL: {avatar_url}")

        # 3. 逐条更新
        success_count = 0
        for user in all_users:
            table = user["table"]
            field = user["field"]
            uid = user["id"]
            old_val = user["old_value"]

            if update_avatar(conn, table, field, uid, avatar_url, dry_run=args.dry_run):
                success_count += 1

        # 4. 汇总
        print(f"\n{'='*50}")
        print(f"处理完成！")
        print(f"  扫描用户: {len(all_users)}")
        if args.dry_run:
            print(f"  模拟更新: {success_count}（未实际写入）")
            print(f"  移除 --dry-run 标志即可执行实际更新")
        else:
            print(f"  实际更新: {success_count}")
            print(f"  默认头像: {processed_path}")
            print(f"  DB URL:   {avatar_url}")
        print(f"{'='*50}")

    finally:
        conn.close()


if __name__ == "__main__":
    main()
