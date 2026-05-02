# 后台菜单与权限建议

## 菜单结构

```text
艺术家管理
├── 艺术家评分
├── 资质审核
└── 评分详情

交易调控
├── 价格调控
└── 价格日志
```

## 权限点

```text
artist:score:list
artist:score:detail
artist:score:recalculate
artist:score:manual-adjust

artist:identity:list
artist:identity:audit
artist:identity:detail

artwork:price:list
artwork:price:manual-adjust
artwork:price:log
```

## 角色建议

### 超级管理员

拥有全部权限。

### 运营管理员

- 查看评分
- 资质审核
- 查看价格日志
- 发起价格调整

### 财务/风控

- 查看价格日志
- 审核价格调整
- 查看人工调分记录

### 客服

- 只读查看艺术家评分与资质状态
