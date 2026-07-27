# 《art12 仓库结构清理与启动规范调整任务书》

## 一、任务背景

当前项目仓库地址：

https://github.com/Mrarter/art12

这是一个艺术品流通小程序项目，项目名称为「拾艺局」或「艺本艺术品流通服务平台」相关开发仓库。

当前仓库已经包含：

- 微信小程序前端：`frontend`
- Vue3 管理后台：`admin`
- Spring Boot / Spring Cloud 后端：`backend`
- 数据库脚本：`backend/sql`
- 项目文档：`docs`、`documents`
- 若干测试目录、备份目录、历史文件、本地配置文件

当前问题不是继续新增业务功能，而是先完成一次 **仓库结构收口、配置安全处理、启动路径规范化、CodeBuddy 可识别化整理**。

本任务目标是：  
让整个仓库变得更清晰、更安全、更容易启动，并且让后续 CodeBuddy / Codex 能够准确识别主项目目录，不被历史备份、测试目录、本地配置干扰。

---

## 二、重要原则

### 1. 本次任务不开发新业务功能

不要新增以下业务功能：

- 艺术家主页新功能
- 作品流通记录新功能
- 动态涨价新功能
- 艺荐官分销新功能
- 拍卖新功能
- 社区新功能
- 订单新功能
- 后台页面新功能

本次只做：

- 仓库结构整理
- 配置文件安全化
- README 修正
- 启动文档统一
- 环境变量规范化
- CodeBuddy 项目说明文件补充

---

### 2. 不要删除有效源码

不要直接删除以下主项目目录：

```text
frontend
admin
backend
docs
documents
```

可以整理、移动、归档历史目录，但不能破坏当前主工程。

---

### 3. 不要修改核心业务逻辑

除非是修复本地路径、环境变量、启动配置，否则不要改动：

```text
业务 Controller
业务 Service
业务 Mapper
业务 Entity
前端业务页面逻辑
后台业务页面逻辑
数据库业务字段
```

---

## 三、当前主项目目录判断

请以以下目录作为当前有效主项目：

```text
art12/
├── frontend/       # 微信小程序 / Uni-app 前端主项目
├── admin/          # Vue3 管理后台主项目
├── backend/        # Spring Boot / Spring Cloud 后端主项目
├── backend/sql/    # 数据库初始化脚本
├── docs/           # 当前开发文档目录
├── documents/      # 历史/补充文档目录
└── README.md       # 项目总说明
```

以下目录视为备份、测试、历史目录，需要归档，不作为主项目入口：

```text
frontend_vue_backup/
test-miniprogram/
test-shiyiju/
pppp/
待导入内容/
uploads/
logs/
nacos/
```

如果这些目录存在，请移动到：

```text
archive/
```

目标结构：

```text
archive/
├── frontend_vue_backup/
├── test-miniprogram/
├── test-shiyiju/
├── pppp/
├── 待导入内容/
├── uploads/
├── logs/
└── nacos/
```

注意：  
如果某个目录不存在，跳过即可，不要报错中断。

---

## 四、第一阶段任务：仓库结构清理

### 1. 创建归档目录

在根目录创建：

```bash
mkdir -p archive
```

### 2. 移动历史目录

如果以下目录存在，请执行移动：

```bash
git mv frontend_vue_backup archive/frontend_vue_backup 2>/dev/null || mv frontend_vue_backup archive/frontend_vue_backup 2>/dev/null || true
git mv test-miniprogram archive/test-miniprogram 2>/dev/null || mv test-miniprogram archive/test-miniprogram 2>/dev/null || true
git mv test-shiyiju archive/test-shiyiju 2>/dev/null || mv test-shiyiju archive/test-shiyiju 2>/dev/null || true
git mv pppp archive/pppp 2>/dev/null || mv pppp archive/pppp 2>/dev/null || true
git mv 待导入内容 archive/待导入内容 2>/dev/null || mv 待导入内容 archive/待导入内容 2>/dev/null || true
git mv uploads archive/uploads 2>/dev/null || mv uploads archive/uploads 2>/dev/null || true
git mv logs archive/logs 2>/dev/null || mv logs archive/logs 2>/dev/null || true
git mv nacos archive/nacos 2>/dev/null || mv nacos archive/nacos 2>/dev/null || true
```

### 3. 根目录保留内容

根目录最终建议保留：

```text
admin/
backend/
frontend/
docs/
documents/
archive/
.gitignore
README.md
PROJECT_CONTEXT.md
CODEBUDDY_TASKS.md
.env.example
```

如果有 Word、PDF、临时文件，请移入：

```text
docs/archive/
```

例如：

```text
prd 12.docx
```

可以移动为：

```text
docs/archive/拾艺局_PRD_历史版本_2026.docx
```

---

## 五、第二阶段任务：敏感配置处理

### 1. 检查是否存在以下敏感文件

```text
.env.local
.env
project.private.config.json
frontend/project.private.config.json
admin/.env.local
backend/**/application-local.yml
```

其中：

```text
.env.local
project.private.config.json
frontend/project.private.config.json
```

不应该继续提交到 Git 仓库。

---

### 2. 从 Git 追踪中移除敏感文件

如果文件已经被 Git 追踪，请执行：

```bash
git rm --cached .env.local 2>/dev/null || true
git rm --cached .env 2>/dev/null || true
git rm --cached project.private.config.json 2>/dev/null || true
git rm --cached frontend/project.private.config.json 2>/dev/null || true
git rm --cached admin/.env.local 2>/dev/null || true
```

注意：  
这里是 `--cached`，表示从 Git 追踪移除，但不一定删除本地文件。

---

### 3. 更新 `.gitignore`

请确保根目录 `.gitignore` 包含以下内容：

```gitignore
# environment
.env
.env.local
.env.*.local

# WeChat private config
project.private.config.json
**/project.private.config.json

# logs
logs/
*.log

# upload files
uploads/

# dependencies
node_modules/
target/

# IDE
.idea/
.vscode/
.DS_Store

# build output
dist/
unpackage/
```

如果已有 `.gitignore`，请合并，不要覆盖已有有效规则。

---

### 4. 创建 `.env.example`

在根目录创建：

```text
.env.example
```

内容如下：

```env
# =========================
# Shiyiju Local Environment Example
# =========================

# MySQL
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=shiyiju
MYSQL_USER=root
MYSQL_PASSWORD=your_mysql_password

# Redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=please_change_this_secret
JWT_EXPIRE_SECONDS=86400

# Nacos
NACOS_SERVER_ADDR=127.0.0.1:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos

# WeChat Mini Program
WECHAT_APPID=your_wechat_appid
WECHAT_SECRET=your_wechat_secret

# File Upload
UPLOAD_LOCAL_PATH=/tmp/shiyiju/uploads
UPLOAD_CDN_URL=http://127.0.0.1:8087

# Frontend
VITE_DEV_LAN_HOST=127.0.0.1
VITE_API_BASE_URL=http://127.0.0.1:8080
```

---

## 六、第三阶段任务：修复前端 API 硬编码问题

### 1. 检查文件

重点检查：

```text
frontend/src/api/request.js
frontend/src/utils/request.js
frontend/src/config/*
```

当前前端如果存在类似：

```js
const DEV_LAN_HOST = '192.168.1.109'
```

或：

```js
baseURL: 'http://192.168.1.109:8082'
```

请改为环境变量。

---

### 2. 推荐调整方式

在 `frontend/.env.development` 中新增：

```env
VITE_DEV_LAN_HOST=127.0.0.1
VITE_API_GATEWAY=http://127.0.0.1:8080
VITE_FILE_HOST=http://127.0.0.1:8087
```

如果需要局域网真机调试，开发者自己改成本机局域网 IP：

```env
VITE_DEV_LAN_HOST=192.168.1.109
VITE_API_GATEWAY=http://192.168.1.109:8080
VITE_FILE_HOST=http://192.168.1.109:8087
```

---

### 3. request.js 调整目标

将硬编码 IP 改为：

```js
const DEV_LAN_HOST = import.meta.env.VITE_DEV_LAN_HOST || '127.0.0.1'

const API_GATEWAY = import.meta.env.VITE_API_GATEWAY || `http://${DEV_LAN_HOST}:8080`

const FILE_HOST = import.meta.env.VITE_FILE_HOST || `http://${DEV_LAN_HOST}:8087`
```

所有业务接口优先统一走 Gateway：

```text
http://127.0.0.1:8080
```

不要让小程序前端直接分别请求：

```text
8081
8082
8083
8084
8085
```

除非项目当前代码确实强依赖直连服务，无法一次性调整，则至少把这些地址全部改为环境变量。

---

## 七、第四阶段任务：修复后端本地绝对路径

### 1. 检查所有 application 配置

检查路径：

```text
backend/**/src/main/resources/application.yml
backend/**/src/main/resources/application-dev.yml
backend/**/src/main/resources/bootstrap.yml
```

搜索以下内容：

```text
/Users/
C:/
192.168.
localhost
root
password
uploads
logs
```

---

### 2. 本地绝对路径替换规则

如果存在类似：

```yaml
logging:
  file:
    name: /Users/master/CodeBuddy/art12/logs/product.log
```

改为：

```yaml
logging:
  file:
    name: ${LOG_FILE_PATH:./logs/product.log}
```

如果存在类似：

```yaml
upload:
  local:
    path: /Users/master/CodeBuddy/art12/uploads
```

改为：

```yaml
upload:
  local:
    path: ${UPLOAD_LOCAL_PATH:./uploads}
    cdn-url: ${UPLOAD_CDN_URL:http://127.0.0.1:8087}
```

数据库配置推荐改为：

```yaml
spring:
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://127.0.0.1:3306/shiyiju?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}
```

Redis 配置推荐改为：

```yaml
spring:
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
```

---

## 八、第五阶段任务：修正 README.md

请重写根目录 `README.md`，目标是让新开发者和 CodeBuddy 能够快速知道：

1. 这个项目是什么
2. 主目录在哪里
3. 怎么启动后端
4. 怎么启动小程序
5. 怎么启动后台
6. 数据库脚本在哪里
7. 哪些目录不要动

---

### README.md 推荐内容

```md
# 拾艺局 / 艺本艺术品流通服务平台

本项目是一个面向艺术家、藏家、艺荐官的艺术品流通服务平台。

核心业务包括：

- 艺术家发布作品
- 作品展示与收藏
- 艺术品购买与订单管理
- 艺荐官推广分佣
- 作品动态涨价
- 后台审核、调价、订单与佣金管理

## 一、项目目录

art12/
├── frontend/       # 微信小程序 / Uni-app 前端
├── admin/          # Vue3 管理后台
├── backend/        # Spring Boot / Spring Cloud 后端
├── backend/sql/    # 数据库脚本
├── docs/           # 项目文档
├── documents/      # 补充文档
├── archive/        # 历史备份与测试目录，不作为主项目
└── README.md

## 二、技术栈

### 小程序前端

- Uni-app
- Vue3
- Vite

### 管理后台

- Vue3
- Vite
- Element Plus

### 后端

- Java
- Spring Boot
- Spring Cloud
- MyBatis / MyBatis Plus
- MySQL
- Redis
- Nacos，可选

## 三、后端启动

进入后端目录：

```bash
cd backend
```

安装依赖：

```bash
mvn clean install -DskipTests
```

优先启动以下 MVP 服务：

```text
shiyiju-gateway      8080
shiyiju-user         8081
shiyiju-product      8082
shiyiju-order        8083
shiyiju-promotion    8085
shiyiju-admin        8090
```

MVP 阶段暂不强制启动：

```text
shiyiju-auction
shiyiju-community
shiyiju-message
shiyiju-file
```

## 四、小程序前端启动

```bash
cd frontend
npm install
npm run dev:mp-weixin
```

H5 调试：

```bash
npm run dev:h5
```

## 五、管理后台启动

```bash
cd admin
npm install
npm run dev
```

## 六、数据库初始化

数据库脚本位置：

```text
backend/sql/
```

优先执行：

```text
backend/sql/init_database.sql
```

如涉及价格增长、权限、ID 迁移，再按实际情况执行：

```text
backend/sql/price_growth_update.sql
backend/sql/admin_permission_tables.sql
backend/sql/id_migration.sql
```

## 七、环境变量

不要提交真实 `.env.local`。

请复制：

```text
.env.example
```

为：

```text
.env.local
```

然后根据本机环境填写数据库、Redis、微信 AppID、上传路径等配置。

## 八、开发注意事项

- 不要修改 archive 目录作为主项目。
- 不要直接提交 `.env.local`。
- 不要提交 `project.private.config.json`。
- 小程序接口优先走 Gateway：`http://127.0.0.1:8080`。
- 不要在代码中写死本机 IP。
- 不要在后端配置中写死 `/Users/...` 这类本机绝对路径。
```

---

## 九、第六阶段任务：新增 PROJECT_CONTEXT.md

在根目录创建：

```text
PROJECT_CONTEXT.md
```

内容如下：

```md
# PROJECT_CONTEXT.md

## 一、项目定位

本项目是「拾艺局 / 艺本艺术品流通服务平台」的开发仓库。

这是一个艺术品流通小程序，不是普通电商平台，也不是装饰画商城。

平台只售卖纯艺术品，核心目标是让好作品被更多人收藏。

## 二、核心用户角色

### 1. 普通用户 / 藏家

- 浏览作品
- 收藏作品
- 购买作品
- 查看订单
- 查看收藏记录

### 2. 艺术家

- 申请认证
- 编辑艺术家主页
- 发布作品
- 设置作品价格
- 查看作品流通情况

### 3. 艺荐官

- 分享作品
- 推广成交
- 查看分佣
- 查看团队
- 申请提现

### 4. 平台管理员

- 审核艺术家
- 审核作品
- 管理订单
- 管理分销
- 管理佣金
- 管理价格调控
- 查看经营数据

## 三、核心业务逻辑

### 1. 艺术品流通

每件艺术品默认库存为 1。

作品被购买后，前台显示为：

```text
已收藏
```

当作品再次上线流通时，需要显示：

```text
已被 X 位藏家收藏
```

### 2. 艺荐官分销

平台支持艺荐官推广作品。

基础规则：

- 平台默认抽佣 15%
- 分销总比例原则上不超过 30%
- 后台可配置分佣比例
- 艺荐官可以查看推广收益和团队数据

### 3. 动态涨价

作品价格可以根据后台配置规则动态增长。

价格增长因素包括：

- 艺术家在线天数
- 艺术家粉丝数
- 作品热度
- 收藏人数
- 后台配置的涨价比例

价格增长区间可配置。

### 4. 收藏与托管

藏家购买作品后，可以选择：

- 寄送实物作品
- 寄送收藏证书
- 平台托管作品
- 艺术家托管作品

托管费可由后台配置。

## 四、当前主项目目录

```text
frontend/       小程序前端
admin/          管理后台
backend/        后端服务
backend/sql/    数据库脚本
docs/           项目文档
documents/      历史或补充文档
archive/        旧代码、备份、测试目录
```

## 五、CodeBuddy 开发注意事项

1. 不要把 archive 目录作为主项目。
2. 不要优先改历史备份目录。
3. 不要随意删除现有业务代码。
4. 不要引入新的大型架构。
5. 不要把本地 IP、密码、绝对路径写死在代码里。
6. 新增功能前，先确认 frontend、admin、backend 三端是否已有对应模块。
7. 优先保证 MVP 主链路可运行。
```

---

## 十、第七阶段任务：新增 CODEBUDDY_TASKS.md

在根目录创建：

```text
CODEBUDDY_TASKS.md
```

内容如下：

```md
# CODEBUDDY_TASKS.md

## 当前阶段目标

当前阶段不是新增功能，而是整理仓库，使项目具备稳定开发基础。

## 本轮任务清单

### Task 1：仓库目录清理

目标：

- 创建 archive 目录
- 将测试、备份、历史目录移动到 archive
- 保留 frontend、admin、backend、docs、documents 作为主目录

验收标准：

- 根目录结构清晰
- CodeBuddy 能准确识别主项目
- 备份目录不再干扰主项目判断

---

### Task 2：敏感配置处理

目标：

- 移除 `.env.local` 的 Git 追踪
- 移除 `project.private.config.json` 的 Git 追踪
- 补充 `.env.example`
- 更新 `.gitignore`

验收标准：

- 仓库不再追踪真实本地环境配置
- `.env.example` 可作为本地配置模板
- `.gitignore` 能阻止后续误提交

---

### Task 3：前端 API 地址环境变量化

目标：

- 修改 frontend 中写死的 IP 地址
- 使用 `.env.development`
- 优先通过 Gateway 请求后端接口

验收标准：

- 不再硬编码 `192.168.x.x`
- 可以通过环境变量切换本机地址
- 小程序请求路径清晰

---

### Task 4：后端配置环境变量化

目标：

- 移除后端配置中的本机绝对路径
- 数据库、Redis、上传路径、日志路径改为环境变量
- 保留合理默认值

验收标准：

- 不再出现 `/Users/master/...`
- 不再强依赖某台电脑路径
- 新电脑 clone 后可通过配置启动

---

### Task 5：README 重写

目标：

- 修正当前 README 中不准确的路径
- 明确 frontend、admin、backend 的启动方式
- 明确数据库脚本位置
- 明确 MVP 阶段优先启动服务

验收标准：

- 新人可以根据 README 启动项目
- CodeBuddy 可以根据 README 理解项目结构
- README 不再指向错误目录

---

### Task 6：新增项目上下文说明

目标：

- 新增 `PROJECT_CONTEXT.md`
- 说明项目定位、用户角色、核心业务逻辑
- 告诉 CodeBuddy 哪些目录是主项目，哪些是 archive

验收标准：

- CodeBuddy 能理解这是艺术品流通平台
- CodeBuddy 不会误把项目当普通商城
- CodeBuddy 不会优先修改 archive 目录

---

## 禁止事项

本轮任务禁止：

- 新增业务页面
- 新增数据库表
- 大规模重构后端服务
- 改动分销业务逻辑
- 改动动态涨价业务逻辑
- 改动订单核心业务逻辑
- 删除主项目源码
- 删除数据库脚本
- 删除 docs 或 documents 中的正式文档

---

## 最终交付要求

完成后请输出：

1. 修改了哪些文件
2. 移动了哪些目录
3. 删除或取消追踪了哪些敏感文件
4. 新增了哪些模板文件
5. README 是否已修正
6. frontend 是否仍可安装依赖
7. admin 是否仍可安装依赖
8. backend 是否仍可 Maven 编译
9. 仍然存在的风险点
10. 下一步建议
```

---

## 十一、最终验收命令

CodeBuddy 完成后，请在仓库根目录执行：

```bash
git status
```

确认改动清晰。

然后执行：

```bash
cd frontend
npm install
npm run dev:h5
```

再执行：

```bash
cd ../admin
npm install
npm run dev
```

再执行：

```bash
cd ../backend
mvn clean install -DskipTests
```

如果 `mvn clean install` 报错，请不要大范围重构，先记录错误原因，并只修复与配置、依赖、路径相关的问题。

---

## 十二、最终输出格式要求

请 CodeBuddy 最终用以下格式汇报：

```md
# 本轮仓库整理完成报告

## 1. 已完成事项

- 

## 2. 已移动目录

- 

## 3. 已修改文件

- 

## 4. 已新增文件

- 

## 5. 已取消 Git 追踪的敏感文件

- 

## 6. 当前启动方式

### 小程序

```bash
cd frontend
npm install
npm run dev:mp-weixin
```

### 管理后台

```bash
cd admin
npm install
npm run dev
```

### 后端

```bash
cd backend
mvn clean install -DskipTests
```

## 7. 仍需人工确认的问题

- 

## 8. 下一步建议

- 
```

---

## 十三、特别提醒

本项目后续核心开发方向是：

```text
艺术家主页
作品流通记录
动态涨价
艺荐官分销
订单成交链路
后台审核与调控
```

但在本轮任务完成之前，不建议继续新增业务功能。

请先确保仓库结构清晰、安全配置正确、README 可用、三端项目路径明确。
