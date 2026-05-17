---
name: fix-publish-duplicate
overview: 修复发布作品时的重复提交问题，从后端内容级幂等校验、前端事件保护、重复触发排查三个维度彻底解决。
todos:
  - id: db-add-fingerprint
    content: "[skill:code-explorer] 在 artwork 表添加 content_fingerprint VARCHAR(64) UNIQUE 列，同时为 Artwork 实体类添加对应字段"
    status: completed
  - id: backend-service-dedup
    content: 修改 ProductService.createArtwork()：insert 前按 title+authorId+日期计算 SHA256 指纹查重，已存在返回旧 ID，不存在则正常插入
    status: completed
    dependencies:
      - db-add-fingerprint
  - id: backend-controller-dedup
    content: 优化 ProductController：requestId 改为服务端基于 contentHash+时间窗口生成，10 秒内相同 hash 拦截
    status: completed
  - id: frontend-button-harden
    content: 前端 publish.vue：移除成功路径的 submitting 重置（失败才重置）；添加 @touchstart.prevent；加深 disabled 样式
    status: completed
  - id: frontend-content-hash
    content: 前端 publish.vue：submit 时计算 contentHash，与上次提交比较，30 秒内相同内容直接拦截
    status: completed
  - id: compile-restart-verify
    content: 编译后端 product 模块，重启服务，使用 agent-browser 验证前端按钮禁用效果
    status: completed
    dependencies:
      - backend-service-dedup
      - backend-controller-dedup
---

修复"发布作品"按钮重复点击导致创建多条相同作品的问题。

## 问题根因排查

**全链路排查结果**（覆盖前端→API→Controller→Service→DB）：

1. **客户端 `requestId` 每次生成不同值**：`Date.now() + Math.random()`。后端 `ConcurrentHashMap` 缓存永远匹配不到，形同虚设
2. **`ProductService.createArtwork()` 无内容级去重**：直接 `artworkMapper.insert(artwork)`，不检查是否已存在相同标题+作者的作品
3. **`submitting` 在 `finally` 中重置过早**：成功后重置后又可点击，1.5s 后才跳转页面
4. **无数据库唯一约束兜底**：SQLite 表无唯一索引，任何应用层绕过都可直接写库
5. **`@Transactional` 并发窗口**：两个请求可同时通过检查后都执行 insert

## 核心修复目标

1. 数据库层：添加内容指纹字段 + UNIQUE 约束，从根杜绝重复
2. 后端 Service：insert 前先查是否存在相同内容，存在则返回旧 ID
3. 后端 Controller：`requestId` 改为服务端基于 `contentHash` 生成，10 秒窗口内拦截
4. 前端按钮：**成功路径不重置 `submitting`**，仅失败才重置。+touch 防双击
5. 前端内容 hash：`SHA256(title + authorName + cover)` 本地拦截 30 秒内同内容

## 技术方案

### 数据层改动（唯一约束兜底）

`artwork` 表新增 `content_fingerprint` VARCHAR(64) UNIQUE 列，值为 `SHA256(title + author_id + YYYYMMDD)`。这是最底层保障，任何应用层绕过都无法突破。

### 后端 Service 层（内容级幂等）

`ProductService.createArtwork()` 方法改造：

```
createArtwork(dto):
  1. 计算 contentFingerprint = SHA256(title + authorId + todayDate)
  2. SELECT id FROM artwork WHERE content_fingerprint = ? 
     AND create_time > (now - 5min)
  3. 如果查到记录 → log.warn + return 已有 id（不创建）
  4. 如果未查到 → 正常 insert（并回填 fingerprint）
```

比 Controller 层的 `ConcurrentHashMap` 更可靠：**不依赖进程内存、跨重启有效、数据库事务保证原子性**。

### 后端 Controller 层（快速拒绝）

保留 `ConcurrentHashMap` 作为第一道防线。改造：不再依赖客户端 `requestId`，改为服务端计算：

```
requestKey = SHA256(contentHash + (timestamp / 10000))
```

相同 `contentHash` 在 10 秒内被拦截。同时保留客户端 `requestId` 兼容。

### 前端按钮彻底禁用

三个层面防止重复触发：

1. **同步标记**：`submitting` 在方法入口设为 `true`，成功路径**不重置**（页面即将跳转），仅失败路径重置
2. **CSS 禁用**：`.submit-btn.disabled { pointer-events: none; opacity: 0.5 }`
3. **触摸事件**：`@touchstart.prevent` 防止触摸设备双击

### 前端内容 hash 本地防重

```
submit() 开头:
  contentHash = simpleHash(title + authorName + coverUrl)
  if contentHash === lastContentHash && now - lastSubmitTime < 30000:
    return  // 30秒内相同内容不重复提交
  lastContentHash = contentHash
  lastSubmitTime = now
```

### 目录结构（修改文件清单）

```
frontend/src/pages/artist/publish.vue        [MODIFY] 按钮禁用 + 内容hash + 成功不重置submitting
backend/.../controller/ProductController.java  [MODIFY] requestId改为服务端基于contentHash生成
backend/.../dto/ArtworkUpdateDTO.java          [MODIFY] 新增 contentFingerprint 字段（由服务端计算，客户端不感知）
backend/.../service/ProductService.java        [MODIFY] createArtwork 新增内容级查重
```

## Agent Extensions

### Skill

- **agent-browser**: 用于在修复完成后打开前端页面，快速验证发布按钮禁用效果和页面跳转行为