<template>
  <div class="admin-layout">
    <div class="sidebar" :class="{ collapse: isCollapse }">
      <div class="logo">
        <span v-if="!isCollapse">拾艺局</span>
        <el-icon v-else><Picture /></el-icon>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>控制台</template>
        </el-menu-item>
        
        <el-sub-menu index="/user">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/user/list">用户列表</el-menu-item>
          <el-menu-item index="/user/artist">艺术家认证</el-menu-item>
          <el-menu-item index="/user/promoter">艺荐官管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/product">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>作品管理</span>
          </template>
          <el-menu-item index="/product/list">作品列表</el-menu-item>
          <el-menu-item index="/product/category">分类管理</el-menu-item>
          <el-menu-item index="/product/audit">审核管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/order">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>订单管理</span>
          </template>
          <el-menu-item index="/order/list">订单列表</el-menu-item>
          <el-menu-item index="/order/aftersale">售后管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/auction">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>拍卖管理</span>
          </template>
          <el-menu-item index="/auction/session">专场管理</el-menu-item>
          <el-menu-item index="/auction/lot">拍品管理</el-menu-item>
          <el-menu-item index="/auction/record">竞拍记录</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/promotion">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>分销管理</span>
          </template>
          <el-menu-item index="/promotion/commission">佣金记录</el-menu-item>
          <el-menu-item index="/promotion/withdraw">提现管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/community">
          <template #title>
            <el-icon><ChatDotRound /></el-icon>
            <span>社区管理</span>
          </template>
          <el-menu-item index="/community/post">帖子管理</el-menu-item>
          <el-menu-item index="/community/comment">评论管理</el-menu-item>
          <el-menu-item index="/community/topic">话题管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/system/banner">Banner管理</el-menu-item>
          <el-menu-item index="/system/config">参数配置</el-menu-item>
          <el-menu-item index="/system/admin">管理员</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
    
    <div class="main-content">
      <div class="header">
        <div class="left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span>{{ adminName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

const adminName = computed(() => {
  const info = localStorage.getItem('admin_info')
  return info ? JSON.parse(info).username : '管理员'
})

const activeMenu = computed(() => route.path)

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
  display: flex;
  
  .sidebar {
    width: 220px;
    background: #304156;
    transition: width 0.3s;
    overflow: hidden;
    
    &.collapse {
      width: 64px;
    }
    
    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      background: #2b3a4a;
      color: #fff;
      font-size: 18px;
      font-weight: bold;
      
      img {
        width: 32px;
        height: 32px;
      }
      
      .el-icon {
        font-size: 24px;
      }
    }
    
    .el-menu {
      border-right: none;
    }
  }
  
  .main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    
    .header {
      height: 60px;
      background: #fff;
      box-shadow: 0 1px 4px rgba(0,21,41,.08);
      display: flex;
      align-items: center;
      padding: 0 20px;
      justify-content: space-between;
      
      .collapse-btn {
        font-size: 20px;
        cursor: pointer;
        margin-right: 15px;
        color: #666;
      }
      
      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
      }
    }
    
    .content {
      flex: 1;
      padding: 20px;
      overflow-y: auto;
      background: #f0f2f5;
    }
  }
}
</style>
