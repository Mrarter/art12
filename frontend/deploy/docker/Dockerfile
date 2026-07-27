# 艺本艺术前端 Dockerfile
# 构建 Vue 3 应用为 Nginx 静态资源

# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 复制依赖文件
COPY package*.json ./

# 安装依赖
RUN npm install

# 复制源代码
COPY . .

# 仅构建 H5 产物，最终静态目录位于 dist/build/h5
RUN npm run build:h5

# 运行阶段
FROM nginx:alpine

# 仅复制 H5 根目录，避免把整个 dist/build 层级挂进来导致 Nginx 根目录错位
COPY --from=builder /app/dist/build/h5/ /usr/share/nginx/html/

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
