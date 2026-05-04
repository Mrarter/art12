---
name: merge-artist-score-identity-into-artist
overview: 将艺术家评分、资质审核功能合并到艺术家管理页面(artist.vue)，通过详情弹窗中的Tab面板集成，移除独立菜单项和路由。
todos:
  - id: merge-score-into-artist-table
    content: 在artist.vue表格中添加评分列，操作区添加评分/资质/重算按钮
    status: completed
  - id: tabify-detail-dialog
    content: 将详情弹窗改为el-tabs多面板（用户信息/评分详情/资质审核/作品管理）
    status: completed
    dependencies:
      - merge-score-into-artist-table
  - id: remove-standalone-routes-menus
    content: 删除Layout.vue和router中的评分列表/资质审核菜单及路由
    status: completed
  - id: remove-redundant-files
    content: 删除ArtistScoreList/IdentityAudit/ScoreDetail三个冗余页面文件
    status: completed
    dependencies:
      - tabify-detail-dialog
      - remove-standalone-routes-menus
  - id: verify-merge
    content: 重启admin前端，测试合并后页面功能正常
    status: completed
    dependencies:
      - tabify-detail-dialog
      - remove-standalone-routes-menus
      - remove-redundant-files
---

## 需求

将艺术家评分列表、资质审核、评分详情三个独立页面合并到艺术家管理页面(artist.vue)，统一操作入口，删除冗余独立页面。

### 合并内容

1. **表格增强**：在艺术家认证表格中新增评分等级、总分、学术资质、互联网资质列
2. **详情弹窗 Tab 化**：现有单面板用户详情弹窗改为多Tab（用户信息/评分详情/资质审核/作品管理）
3. **路由菜单调整**：移除独立的评分列表和资质审核菜单项
4. **删除冗余文件**：合并后删除三个独立页面文件

### 不合并的内容

- 交易调控（价格调控/价格日志）保持独立菜单不变

## 技术方案

### 1. 前端文件变更

#### artist.vue 表格增强

在现有表格中新增列（插入在"认证状态"列之后、"艺术家简介"列之前）：

- **评分等级**列：使用 ScoreLevelTag 组件显示 `row.scoreLevel`
- **总分**列：显示 `row.totalScore`
- **学术资质**列：显示 `row.academicScore`
- **互联网资质**列：显示 `row.internetScore`

操作区新增按钮：

- "评分详情"按钮：调用 `getArtistScoreDetail(artistId)` 获取评分数据，打开详情弹窗并切换到评分Tab
- "资质审核"按钮：调用 `getIdentityDetail(artistId)` 获取资质数据，打开详情弹窗并切换到资质Tab
- "重算评分"按钮：调用 `recalculateArtistScore(artistId)` API

数据合并方式：`loadData()` 后并行请求评分列表，按 `artistId` 建立 Map，渲染时 merge 到行数据。

#### 详情弹窗 Tab 化

将原有的单面板详情弹窗改为 el-tabs 结构：

- **Tab1 "用户信息"**：保留现有编辑表单、身份配置、账户信息
- **Tab2 "评分详情"**：显示总分+等级统计 + 7维评分维度表格 + 重算/调分按钮
- **Tab3 "资质审核"**：显示毕业院校/学历/职称/协会/社交平台/粉丝数等信息 + 审核状态 + 通过/驳回操作
- **Tab4 "作品管理"**：保留现有作品列表 + 添加作品

### 2. 路由/菜单变更

**Layout.vue**：删除第28-29行的"艺术家评分"和"资质审核"菜单项。

```
删除：
  <el-menu-item index="/user/artist-score-list">艺术家评分</el-menu-item>
  <el-menu-item index="/user/identity-audit">资质审核</el-menu-item>
```

**router/index.js**：删除第52-69行的 artist-score-list 和 identity-audit 路由定义。

```
删除：
  {
    path: 'artist-score-list', ...  // 第52-57行
  },
  {
    path: 'identity-audit', ...     // 第58-63行
  },
  {
    path: 'artist-score-detail/...', ...  // 第64-69行, 隐藏路由保留
  }
```

保留 `artist-score-detail/:artistId` 隐藏路由（虽然不再直接使用，但删除无影响，后续可以清理）。

### 3. 删除的文件

- `admin/src/views/artist-score/ArtistScoreList.vue`
- `admin/src/views/artist-score/ArtistIdentityAudit.vue`
- `admin/src/views/artist-score/ArtistScoreDetail.vue`

### 4. 保留的依赖

- `admin/src/components/ScoreLevelTag.vue`：引入到artist.vue
- `admin/src/api/artistScore.js`：API函数保留
- `admin/src/api/artistIdentity.js`：API函数保留

### 5. 测试验证

- 访问艺术家管理页，验证表格正常显示含评分列
- 验证搜索、翻页、Tab切换功能正常
- 验证操作列"评分详情/资质审核/重算评分"按钮功能正常
- 验证详情弹窗 Tab 切换正常
- 验证菜单中评分列表和资质审核已移除