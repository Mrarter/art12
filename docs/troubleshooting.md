# 问题排查记录

本文档记录项目中遇到的问题、原因及解决方案，供后续快速排查参考。

---

## 1. 小程序前端 API 请求地址配置错误

**问题现象**：瀑布流和 Banner 显示的数据与运营后台不一致

**根本原因**：
- 小程序前端 `BASE_URL` 配置为 `localhost:8082`（直接访问 product 服务）
- 应该通过网关 `localhost:8080` 访问，否则无法获取正确的路由和认证

**涉及文件**：
- `frontend/src/api/request.js`

**修复方案**：
```javascript
// 错误配置
const BASE_URL = 'http://localhost:8082/api'

// 正确配置
const BASE_URL = 'http://localhost:8080/api'
```

**排查要点**：
- 先确认后端服务是否通过网关统一入口
- 检查前端 API 请求的实际 URL 是否正确

---

## 2. MyBatis-Plus NULL 值条件查询问题

**问题现象**：Banner 数据只有部分显示，`start_time` 或 `end_time` 为 NULL 的记录无法查询到

**根本原因**：
MyBatis-Plus 的 `wrapper.and()` 方法中嵌套条件可能被错误解析，导致 NULL 检查条件丢失。

错误示例：
```java
// 会被解析为：status = 1 AND start_time <= ? AND end_time >= ?（NULL 检查丢失）
wrapper.le(Banner::getStartTime, LocalDateTime.now());
wrapper.ge(Banner::getEndTime, LocalDateTime.now());
```

**涉及文件**：
- `backend/shiyiju-product/src/main/java/com/shiyiju/product/service/ProductService.java`

**修复方案**：
```java
LocalDateTime now = LocalDateTime.now();
wrapper.and(w -> w
    .and(n -> n.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now))
    .and(n -> n.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now))
);
```

**排查要点**：
- 当查询包含 `IS NULL` 条件但实际 SQL 没有出现时，检查 wrapper 条件嵌套方式
- 使用日志确认实际执行的 SQL 语句

---

## 3. 前端分页数据格式处理不一致

**问题现象**：列表数据为空或只显示一条，实际数据库有多条

**根本原因**：
后端返回 `PageResult` 对象，字段为 `records`，但前端多处代码直接当作数组处理。

后端返回格式：
```json
{
  "code": 0,
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

前端错误处理：
```javascript
// 直接使用返回值作为数组
const list = await getRecommend(params)
productList.value = list  // 错误！list 是 PageResult 对象，不是数组
```

**涉及文件**：
- `frontend/src/pages/index/index.vue`
- `frontend/src/pages/artist/manage.vue`
- `frontend/src/pages/gallery/index.vue`
- `frontend/src/pages/order/list.vue`

**修复方案**：
```javascript
const result = await getRecommend(params)
// 处理 PageResult 格式：{ records: [], total: xxx }
const list = result?.records || result?.list || result || []
```

**排查要点**：
- 检查后端 Controller 返回类型是否为 `PageResult` 或 `PageInfo`
- 确认前端 API 调用的处理逻辑是否正确提取数据数组

---

## 4. 后端 VO 字段与前端期望字段名不匹配

**问题现象**：前端显示的数据字段为空，如 `item.isHot`、`item.authorName` 为 undefined

**根本原因**：
- 后端 VO 使用 `@JsonProperty` 映射字段名（如 `artistName`、`isNew`）
- 前端期望的是 MyBatis 实体类字段名（如 `authorName`、`isHot`）
- 或后端 VO 缺少前端需要的字段

**涉及文件**：
- `backend/shiyiju-common/src/main/java/com/shiyiju/common/vo/ArtworkVO.java`
- `frontend/src/pages/index/index.vue`

**修复方案**：
1. 在 VO 类中添加缺失字段并使用 `@JsonProperty` 映射：
```java
@JsonProperty("isHot")
private Boolean isHot;
```

2. 在 Service 的 convertToVO 方法中计算字段值：
```java
boolean isHot = (artwork.getSaleCount() != null && artwork.getSaleCount() > 0)
        || (artwork.getFavoriteCount() != null && artwork.getFavoriteCount() > 5);
vo.setIsHot(isHot);
```

**排查要点**：
- 对比前端模板使用的字段名和后端 API 返回的 JSON 字段名
- 检查 VO 类是否有对应的 `@JsonProperty` 注解

---

## 5. 后端服务未正确重启/编译

**问题现象**：代码修改后 API 返回结果没有变化

**根本原因**：
- 未重新编译项目（mvn compile/package）
- 未重启后端服务
- 启动参数使用了旧的 JAR 包

**排查要点**：
```bash
# 1. 确保编译最新代码
cd backend
mvn clean package -pl shiyiju-product -am -q -DskipTests

# 2. 检查服务进程
ps aux | grep shiyiju-product

# 3. 重启服务
pkill -f "shiyiju-product"
nohup java -jar shiyiju-product/target/shiyiju-product-1.0.0-SNAPSHOT.jar > ../logs/product.log 2>&1 &

# 4. 验证服务启动
sleep 10
curl http://localhost:8082/actuator/health
```

---

## 排查流程（快速检查清单）

1. **检查 API 地址**：`frontend/src/api/request.js` 的 BASE_URL
2. **检查网关路由**：`localhost:8080/api/...` vs `localhost:8082/...`
3. **直接测试 API**：使用 curl 确认返回数据正确
4. **检查 SQL 日志**：确认查询条件是否正确生成
5. **检查分页格式**：确认 `records` 字段是否正确提取
6. **检查字段名**：对比前端模板和后端 VO 的字段名
7. **确认服务重启**：代码修改后必须重启后端服务

---

*最后更新：2026-04-22*
