#!/usr/bin/env python3
import json
import urllib.request
import urllib.parse
import time
import random

# 获取所有用户
def get_all_users():
    all_users = []
    page = 1
    page_size = 100
    
    while True:
        url = f"http://localhost:8090/admin/user/list?page={page}&size={page_size}"
        with urllib.request.urlopen(url) as response:
            data = json.loads(response.read().decode('utf-8'))
            if data['code'] != 200:
                print(f"获取用户失败: {data}")
                break
            
            records = data['data']['records']
            all_users.extend(records)
            
            if len(records) < page_size:
                break
            page += 1
    
    return all_users

# 生成UID的函数
def generate_uid(seq):
    date = time.strftime('%Y%m%d')
    seq_str = f'{seq:06d}'
    chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    rand1 = chars[random.randint(0, 35)]
    rand2 = chars[random.randint(0, 35)]
    return f'ART{date}{seq_str}{rand1}{rand2}'

# 批量更新用户UID
def batch_update_uids(user_ids, uids):
    payload = {
        'userIds': user_ids,
        'uids': uids
    }
    
    req = urllib.request.Request(
        'http://localhost:8090/admin/user/batchUpdateUids',
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='POST'
    )
    
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode('utf-8'))
        return result

# 主程序
print("获取所有用户...")
all_users = get_all_users()
print(f"获取到 {len(all_users)} 个用户")

print("\n生成的UID映射:")
user_ids = []
uids = []
for i, user in enumerate(all_users):
    user_id = user['userId']
    uid = generate_uid(i + 1)
    user_ids.append(user_id)
    uids.append(uid)
    print(f"  ID {user_id:3d} ({user['nickname']:10s}) -> {uid}")

print(f"\n正在更新 {len(user_ids)} 个用户的UID...")
result = batch_update_uids(user_ids, uids)
print(f"更新结果: {result}")

if result.get('code') == 200:
    print("\n✅ 所有用户UID更新成功!")
else:
    print(f"\n❌ 更新失败: {result.get('message')}")
