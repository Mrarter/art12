<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">艺术家认证管理</span>
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon>
      添加艺术家
      </el-button>
      <el-button type="success" link @click="handleExport" :loading="exportLoading">
        导出CSV
      </el-button>
    </div>
    
    <!-- 状态 Tab 切换 -->
    <div class="status-tabs">
      <el-radio-group v-model="activeTab" @change="handleTabChange">
        <el-radio-button label="pending">
          待审核 <el-badge :value="pendingCount" :hidden="pendingCount === 0" type="warning" />
        </el-radio-button>
        <el-radio-button label="approved">
          已认证 <el-badge :value="approvedCount" :hidden="approvedCount === 0" type="success" />
        </el-radio-button>
        <el-radio-button label="rejected">
          未通过 <el-badge :value="rejectedCount" :hidden="rejectedCount === 0" type="danger" />
        </el-radio-button>
        <el-radio-button label="hidden">
          已隐藏 <el-badge :value="hiddenCount" :hidden="hiddenCount === 0" type="info" />
        </el-radio-button>
        <el-radio-button label="all">
          全部
        </el-radio-button>
      </el-radio-group>
    </div>
    
    <!-- 搜索筛选 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="昵称/姓名">
          <el-input v-model="searchForm.keyword" placeholder="请输入昵称或姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="ID/UID">
          <el-input v-model="searchForm.userId" placeholder="请输入ID或UID" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="认证等级">
          <el-select v-model="searchForm.badge" placeholder="全部" clearable style="width: 130px">
            <el-option label="全部" value="" />
            <el-option label="普通认证" value="verified" />
            <el-option label="认证艺术家" value="verified_plus" />
            <el-option label="人气艺术家" value="popular" />
            <el-option label="大师级" value="master" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 批量操作栏 -->
    <div v-if="selectedRows.length > 0" class="batch-actions">
      <span class="selected-count">已选择 {{ selectedRows.length }} 个艺术家</span>
      <el-button type="primary" link @click="clearSelection">取消选择</el-button>
      <el-divider direction="vertical" />
      <el-button type="success" @click="handleBatchApprove" :loading="batchLoading">
        批量通过
      </el-button>
      <el-button type="danger" @click="showBatchRejectDialog" :loading="batchLoading">
        批量拒绝
      </el-button>
      <el-button type="warning" @click="handleBatchHide" :loading="batchLoading">
        批量隐藏
      </el-button>
      <el-button type="danger" @click="handleBatchDelete" :loading="batchLoading">
        批量删除
      </el-button>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" width="80">
        <template #default="{ row }">
          <span class="id-display" @click="handleCopyId(row.displayId || row.id)">{{ row.displayId || row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户信息" min-width="200">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :src="getFullImageUrl(row.avatar || row.userAvatar)" :size="50" fit="cover" class="clickable-avatar" @click="openUserProfile(row)" />
            <div class="user-detail">
              <p class="nickname">
                {{ row.nickname || row.userNickname || '未知用户' }}
                <el-tag v-if="row.certified" type="success" size="small">已认证</el-tag>
                <el-tag v-else type="info" size="small">未认证</el-tag>
              </p>
              <p class="phone">{{ row.phone || row.userPhone }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="realName" label="真实姓名" width="120">
        <template #default="{ row }">
          {{ row.realName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="idCard" label="身份证号" width="180">
        <template #default="{ row }">
          {{ row.idCard || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130">
        <template #default="{ row }">
          {{ row.phone || row.userPhone || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="badge" label="认证等级" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.badge === 'master'" type="danger" size="small">大师级</el-tag>
          <el-tag v-else-if="row.badge === 'popular'" type="warning" size="small">人气艺术家</el-tag>
          <el-tag v-else-if="row.badge === 'verified'" type="success" size="small">认证艺术家</el-tag>
          <span v-else-if="row.certified" class="certified">普通认证</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="认证材料" width="120">
        <template #default="{ row }">
          <el-button 
            v-if="row.images || row.artworks" 
            type="primary" 
            link 
            @click="viewMaterials(row)"
          >
            查看
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resume" label="艺术家简介" min-width="200">
        <template #default="{ row }">
          <span v-if="row.resume || row.bio" class="resume-text" :title="row.resume || row.bio">
            {{ (row.resume || row.bio).substring(0, 30) }}{{ (row.resume || row.bio).length > 30 ? '...' : '' }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="180">
        <template #default="{ row }">
          {{ row.createTime || row.createdAt || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <!-- 待审核状态 -->
          <template v-if="row.status === 0 || row.status === 'pending'">
            <el-button type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
          <!-- 已认证状态 -->
          <template v-else-if="row.status === 1 || row.status === 'approved'">
            <el-button type="warning" link @click="showBadgeDialog(row)">设置等级</el-button>
            <el-button type="danger" link @click="handleHide(row)">隐藏艺术家</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
          <template v-else-if="row.status === 3 || row.status === 'hidden'">
            <el-button type="primary" link @click="handleUnhide(row)">重新显示</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
          <!-- 已拒绝状态 -->
          <template v-else>
            <el-button type="primary" link @click="handleReapply(row)">重新认证</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
    
    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="600px" destroy-on-close>
      <div class="user-profile" v-if="currentUser">
        <!-- 用户基本信息 -->
        <div class="profile-header">
          <div class="avatar-wrapper">
            <el-avatar :src="getFullImageUrl(profileForm.avatar)" :size="80" fit="cover" />
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary">更换头像</el-button>
            </el-upload>
          </div>
          <div class="profile-info">
            <h3>{{ profileForm.nickname || currentUser.nickname || currentUser.userNickname || '未知用户' }}
              <el-tag v-if="currentUser.isVip" type="warning" size="small">VIP</el-tag>
            </h3>
            <p class="user-id">ID: {{ currentUser.displayId || currentUser.userId || currentUser.id }}</p>
            <div class="identity-tags">
              <el-tag v-if="currentUser.isArtist" type="success" size="small">艺术家</el-tag>
              <el-tag v-if="currentUser.isPromoter" type="warning" size="small">艺荐官</el-tag>
              <el-tag v-if="!currentUser.isArtist && !currentUser.isPromoter" type="info" size="small">普通用户</el-tag>
            </div>
          </div>
        </div>
        
        <!-- 编辑表单 -->
        <el-form ref="profileFormRef" :model="profileForm" label-width="90px" class="profile-form">
          <el-divider content-position="left">基本信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          
          <el-divider content-position="left">艺术家信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="真实姓名">
                <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="身份证号">
                <el-input v-model="profileForm.idCard" placeholder="请输入身份证号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="艺术家简介">
            <el-input 
              v-model="profileForm.resume" 
              type="textarea" 
              :rows="2" 
              placeholder="请输入艺术家简介"
            />
          </el-form-item>
          
          <el-divider content-position="left">身份配置</el-divider>
          <el-form-item label="身份">
            <el-checkbox-group v-model="profileForm.identities">
              <el-checkbox label="artist">艺术家</el-checkbox>
              <el-checkbox label="promoter">艺荐官</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          
          <el-divider content-position="left">账户信息</el-divider>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="info-item">
                <span class="label">账户余额</span>
                <span class="value">¥{{ currentUser.balance || 0 }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="label">累计消费</span>
                <span class="value">¥{{ currentUser.totalConsume || 0 }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="info-item">
                <span class="label">订单数量</span>
                <span class="value">{{ currentUser.orderCount || 0 }}</span>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="info-item">
                <span class="label">注册时间</span>
                <span class="value">{{ currentUser.registerTime || currentUser.createTime || '-' }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="label">注册来源</span>
                <span class="value">{{ getSourceText(currentUser.source) }}</span>
              </div>
            </el-col>
          </el-row>
          
          <el-divider content-position="left">艺术家作品 ({{ userArtworks.total || 0 }})</el-divider>
          <div v-loading="artworksLoading" class="artworks-section">
            <div v-if="userArtworks.list && userArtworks.list.length > 0" class="artwork-grid">
              <div v-for="artwork in userArtworks.list" :key="artwork.id" class="artwork-item">
                <el-image :src="getFullImageUrl(artwork.cover)" :alt="artwork.title" fit="cover" class="artwork-cover" />
                <div class="artwork-info">
                  <p class="artwork-title">{{ artwork.title }}</p>
                  <p class="artwork-price">¥{{ artwork.price || 0 }}</p>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无作品" :image-size="60" />
          </div>
          <div v-if="userArtworks.total > userArtworks.list?.length" class="load-more">
            <el-button link type="primary" @click="loadMoreArtworks">加载更多</el-button>
          </div>
          <div class="add-artwork-btn">
            <el-button type="primary" size="small" @click="openArtworkDialog">
              <el-icon><Plus /></el-icon>
              添加作品
            </el-button>
          </div>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer-between">
          <el-button
            v-if="currentRecord?.certified || currentRecord?.status === 1"
            type="danger"
            plain
            @click="handleRevokeFromDetail"
          >
            取消认证
          </el-button>
          <span v-else></span>
          <span>
            <el-button @click="detailVisible = false">取消</el-button>
            <el-button type="primary" :loading="editLoading" @click="saveProfile">保存修改</el-button>
          </span>
        </div>
      </template>
    </el-dialog>
    
    <!-- 认证材料弹窗 -->
    <el-dialog v-model="materialsVisible" title="认证材料" width="700px">
      <div v-if="currentRecord" class="materials">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="真实姓名">{{ currentRecord.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentRecord.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">{{ currentRecord.resume || currentRecord.bio || '-' }}</el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentRecord.images || currentRecord.artworks" class="images-section">
          <p class="section-title">证件照片：</p>
          <div class="image-list">
            <el-image 
              v-for="(img, index) in (currentRecord.images || '').split(',').filter(Boolean)" 
              :key="'img-' + index"
              :src="img" 
              :preview-src-list="(currentRecord.images || '').split(',').filter(Boolean)"
              style="width: 120px; height: 90px; margin-right: 10px; margin-bottom: 10px"
              fit="cover"
            />
          </div>
        </div>
        
        <div v-if="currentRecord.exhibits" class="images-section">
          <p class="section-title">参展证明：</p>
          <div class="image-list">
            <el-image 
              v-for="(img, index) in currentRecord.exhibits.split(',').filter(Boolean)" 
              :key="'exhibit-' + index"
              :src="img" 
              :preview-src-list="currentRecord.exhibits.split(',').filter(Boolean)"
              style="width: 120px; height: 90px; margin-right: 10px; margin-bottom: 10px"
              fit="cover"
            />
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝原因" width="500px">
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input 
            v-model="rejectReason" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量拒绝原因弹窗 -->
    <el-dialog v-model="batchRejectDialogVisible" title="批量拒绝原因" width="500px">
      <el-form>
        <p style="margin-bottom: 15px; color: #666;">将为选中的 <strong>{{ selectedRows.length }}</strong> 个艺术家拒绝认证</p>
        <el-form-item label="拒绝原因">
          <el-input 
            v-model="batchRejectReason" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="confirmBatchReject">确定拒绝</el-button>
      </template>
    </el-dialog>
    
    <!-- 添加艺术家弹窗 -->
    <el-dialog v-model="addVisible" title="添加艺术家" width="600px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-alert type="info" :closable="false" style="margin-bottom: 15px;">
          创建后将同时创建艺术家认证记录
        </el-alert>
        <div class="add-form-header">
          <div class="avatar-section">
            <el-avatar :src="getFullImageUrl(addForm.avatar)" :size="80" fit="cover" />
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleAddAvatarUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary">上传头像</el-button>
            </el-upload>
          </div>
          <div class="add-form-main">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="addForm.phone" placeholder="请输入用户手机号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="昵称">
                  <el-input v-model="addForm.nickname" placeholder="可选，默认为'用户'+手机号后4位" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
        <el-divider content-position="left">艺术家信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="addForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="addForm.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="认证等级" prop="badge">
          <el-select v-model="addForm.badge" placeholder="请选择认证等级">
            <el-option label="普通认证" value="" />
            <el-option label="认证艺术家" value="verified" />
            <el-option label="人气艺术家" value="popular" />
            <el-option label="大师级" value="master" />
          </el-select>
        </el-form-item>
        <el-form-item label="艺术家简介">
          <el-input 
            v-model="addForm.resume" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入艺术家简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="confirmAdd">确定添加</el-button>
      </template>
    </el-dialog>

    <!-- 设置等级弹窗 -->
    <el-dialog v-model="badgeVisible" title="设置艺术家等级" width="400px">
      <el-form label-width="100px">
        <el-form-item label="当前用户">
          {{ currentRecord?.nickname || currentRecord?.userNickname }}
        </el-form-item>
        <el-form-item label="认证等级">
          <el-select v-model="selectedBadge" placeholder="请选择认证等级">
            <el-option label="普通认证" value="" />
            <el-option label="认证艺术家" value="verified" />
            <el-option label="人气艺术家" value="popular" />
            <el-option label="大师级" value="master" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="badgeVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBadge">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加作品弹窗 -->
    <el-dialog v-model="artworkDialogVisible" title="添加作品" width="700px" destroy-on-close>
      <el-tabs v-model="artworkTab" class="artwork-tabs">
        <el-tab-pane label="选择已有作品" name="select">
          <div class="existing-works">
            <div class="works-filter">
              <el-input v-model="artworkKeyword" placeholder="搜索作品标题" clearable style="width: 200px" @change="searchExistingWorks" />
              <el-button type="primary" size="small" @click="searchExistingWorks">搜索</el-button>
              <el-button size="small" @click="loadExistingWorks">刷新</el-button>
            </div>
            <div v-loading="existingWorksLoading" class="works-grid">
              <div v-for="work in existingWorks" :key="work.id" 
                   class="work-card" 
                   :class="{ selected: selectedExistingId === work.id }"
                   @click="selectExistingWork(work)">
                <el-image :src="getFullImageUrl(work.cover)" fit="cover" class="work-cover" />
                <div class="work-info">
                  <p class="work-title">{{ work.title }}</p>
                  <p class="work-author">{{ work.authorName }}</p>
                  <p class="work-price">¥{{ work.price }}</p>
                </div>
                <div v-if="selectedExistingId === work.id" class="selected-badge">
                  <el-icon><Check /></el-icon>
                </div>
              </div>
              <el-empty v-if="existingWorks.length === 0 && !existingWorksLoading" description="暂无作品" :image-size="60" />
            </div>
            <div v-if="existingWorksTotal > existingWorks.length" class="load-more-works">
              <el-button link type="primary" @click="loadMoreExistingWorks">加载更多</el-button>
            </div>
            <div class="select-action">
              <el-button type="primary" :disabled="!selectedExistingId" :loading="artworkLoading" @click="confirmSelectExisting">
                确认选择 ({{ selectedExistingTitle || '' }})
              </el-button>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="创建新作品" name="create">
          <el-form ref="artworkFormRef" :model="artworkForm" label-width="100px">
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="artworkForm.title" placeholder="请输入作品标题" />
        </el-form-item>
        <el-form-item label="作品作者" prop="authorName">
          <el-input v-model="artworkForm.authorName" placeholder="请输入作者姓名" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作品分类">
              <el-select v-model="artworkForm.categoryId" placeholder="请选择分类" clearable>
                <el-option v-for="cat in artworkCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作品类型">
              <el-select v-model="artworkForm.ownershipType" placeholder="请选择">
                <el-option label="原创" :value="1" />
                <el-option label="收藏" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="售价(元)" prop="price">
              <el-input-number v-model="artworkForm.price" :min="0" :precision="2" placeholder="请输入售价" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="市场价(元)">
              <el-input-number v-model="artworkForm.originalPrice" :min="0" :precision="2" placeholder="请输入原价" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="画种">
              <el-input v-model="artworkForm.artType" placeholder="如：油画、水墨画" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创作年份">
              <el-input-number v-model="artworkForm.year" :min="1900" :max="2099" placeholder="年份" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="作品尺寸">
          <el-input v-model="artworkForm.size" placeholder="如：100x80cm" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="artworkForm.stock" :min="1" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="作品封面">
          <div class="cover-uploader">
            <el-image v-if="artworkForm.cover" :src="getFullImageUrl(artworkForm.cover)" fit="cover" class="cover-preview" />
            <el-upload
              class="cover-upload"
              :show-file-list="false"
              :http-request="handleArtworkCoverUpload"
              accept="image/*"
            >
              <el-button v-if="!artworkForm.cover" type="primary" size="small">上传封面</el-button>
              <el-button v-else type="warning" size="small">更换封面</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="作品描述">
          <el-input v-model="artworkForm.description" type="textarea" :rows="3" placeholder="请输入作品描述" />
        </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="artworkDialogVisible = false">取消</el-button>
        <el-button v-if="artworkTab === 'create'" type="primary" :loading="artworkLoading" @click="submitArtwork">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Check, Download } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import request, { getFullImageUrl, uploadFile } from '@/api/request'
import { requestApi } from '@/api/request'
import { copyId } from '@/utils/id'

const route = useRoute()
const loading = ref(false)
const materialsVisible = ref(false)
const rejectVisible = ref(false)
const addVisible = ref(false)
const addLoading = ref(false)
const badgeVisible = ref(false)
const detailVisible = ref(false)
const editLoading = ref(false)
const currentRecord = ref({})
const currentUser = ref({})
const profileFormRef = ref()
const artworksLoading = ref(false)
const userArtworks = ref({ list: [], total: 0 })
const artworksPage = ref(1)
const artworksSize = 8
const rejectReason = ref('')
const selectedBadge = ref('')
const activeTab = ref('pending')
const searchForm = reactive({
  keyword: '',
  phone: '',
  userId: '',
  badge: '',
  dateRange: []
})
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)
const hiddenCount = ref(0)
const tableData = ref([])
const addFormRef = ref()
const artworkDialogVisible = ref(false)
const artworkFormRef = ref()
const artworkLoading = ref(false)
const artworkCategories = ref([])
const artworkTab = ref('select')
const existingWorks = ref([])
const existingWorksLoading = ref(false)
const existingWorksPage = ref(1)
const existingWorksTotal = ref(0)
const existingWorksSize = 12
const artworkKeyword = ref('')
const selectedExistingId = ref(null)
const selectedExistingTitle = ref('')

const exportLoading = ref(false)

// 批量操作相关变量
const selectedRows = ref([])
const batchLoading = ref(false)
const batchRejectDialogVisible = ref(false)
const batchRejectReason = ref('')

const artworkForm = reactive({
  title: '',
  authorName: '',
  categoryId: '',
  cover: '',
  price: null,
  originalPrice: null,
  stock: 1,
  description: '',
  ownershipType: 1,
  artType: '',
  size: '',
  year: null
})

const profileForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  identities: [],
  realName: '',
  idCard: '',
  resume: ''
})

const addForm = reactive({
  phone: '',
  nickname: '',
  avatar: '',
  realName: '',
  idCard: '',
  badge: '',
  resume: ''
})

const addRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const getStatusType = (status) => {
  if (status === 1 || status === 'approved') return 'success'
  if (status === 0 || status === 'pending') return 'warning'
  if (status === 2 || status === 'rejected') return 'danger'
  if (status === 3 || status === 'hidden') return 'info'
  return 'info'
}

const getStatusText = (status) => {
  if (status === 1 || status === 'approved') return '已认证'
  if (status === 0 || status === 'pending') return '待审核'
  if (status === 2 || status === 'rejected') return '已拒绝'
  if (status === 3 || status === 'hidden') return '已隐藏'
  return status
}

const getSourceText = (source) => {
  const map = { wechat: '微信', ios: 'iOS', android: 'Android', web: 'Web', '': '-' }
  return map[source] || source || '-'
}

// 复制ID
const handleCopyId = (id) => {
  if (!id) {
    ElMessage.warning('ID为空')
    return
  }
  copyId(id,
    () => ElMessage.success('已复制ID'),
    () => ElMessage.error('复制失败')
  )
}

// 打开用户资料弹窗
const openUserProfile = async (row) => {
  currentRecord.value = row
  // 从行数据获取用户信息，保留displayId用于显示
  const userId = row.userId || row.id
  currentUser.value = { 
    ...row,
    userId,
    displayId: row.displayId || String(userId).padStart(4, '0'),
    isArtist: row.certified || row.status === 1,
    isPromoter: false
  }
  
  // 获取完整用户信息
  try {
    const detail = await request.get(`/user/${userId}`)
    if (detail) {
      // 确保保留displayId
      const savedDisplayId = currentUser.value.displayId
      currentUser.value = { ...currentUser.value, ...detail }
      currentUser.value.displayId = savedDisplayId
      currentUser.value.userId = userId
    }
  } catch (e) {
    // 使用行数据
  }
  
  // 设置表单数据
  Object.assign(profileForm, {
    nickname: row.nickname || row.userNickname || '',
    phone: row.phone || row.userPhone || '',
    email: row.email || '',
    avatar: row.avatar || row.userAvatar || '',
    identities: row.certified ? ['artist'] : [],
    realName: row.realName || '',
    idCard: row.idCard || '',
    resume: row.resume || row.bio || ''
  })

  // 加载艺术家作品
  artworksPage.value = 1
  userArtworks.value = { list: [], total: 0 }
  await loadUserArtworks(userId)

  detailVisible.value = true
}

const openUserProfileFromRoute = async () => {
  const userId = route.query.userId
  if (!userId) return

  const idText = String(userId)
  activeTab.value = 'all'
  await loadData()

  const matched = tableData.value.find(item => {
    return String(item.userId || item.id) === idText ||
      String(item.displayId || '') === idText ||
      String(item.uid || '') === idText
  })

  await openUserProfile(matched || {
    id: Number(idText),
    userId: Number(idText),
    displayId: route.query.authorUid && route.query.authorUid !== '-' ? String(route.query.authorUid) : idText,
    realName: route.query.artistName || '',
    nickname: route.query.artistName || ''
  })
}

// 加载用户作品
const loadUserArtworks = async (userId) => {
  artworksLoading.value = true
  try {
    const res = await request.get(`/user/${userId}/artworks`, {
      params: { page: artworksPage.value, size: artworksSize }
    })
    if (artworksPage.value === 1) {
      userArtworks.value = { list: res.list || [], total: res.total || 0 }
    } else {
      userArtworks.value.list = [...userArtworks.value.list, ...(res.list || [])]
    }
  } catch (e) {
    console.error('加载作品失败', e)
  } finally {
    artworksLoading.value = false
  }
}

// 加载更多作品
const loadMoreArtworks = () => {
  const userId = currentUser.value.userId
  if (!userId) return
  artworksPage.value++
  loadUserArtworks(userId)
}

// 上传头像
const handleAvatarUpload = async (options) => {
  const { file, onSuccess, onError } = options
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    onError(new Error('请选择图片文件'))
    return
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    onError(new Error('图片大小不能超过 10MB'))
    return
  }

  try {
    const result = await uploadFile(file)
    profileForm.avatar = result?.url || result || ''
    ElMessage.success('头像上传成功')
    onSuccess()
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
    onError(e)
  }
}

// 保存用户资料
const saveProfile = async () => {
  try {
    editLoading.value = true
    const userId = currentUser.value.userId || currentUser.value.id
    await request.put(`/user/${userId}`, {
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      phone: profileForm.phone,
      email: profileForm.email,
      identities: profileForm.identities,
      realName: profileForm.realName,
      idCard: profileForm.idCard,
      resume: profileForm.resume
    })
    detailVisible.value = false
    ElMessage.success('保存成功')
    await loadData()
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    editLoading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    phone: '',
    userId: '',
    badge: '',
    dateRange: []
  })
  pagination.page = 1
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
  const params = { 
    page: pagination.page, 
    size: pagination.size,
    status: activeTab.value === 'all' ? undefined : activeTab.value,
    keyword: searchForm.keyword || undefined,
    phone: searchForm.phone || undefined,
    userId: searchForm.userId || undefined,
    badge: searchForm.badge || undefined,
    startDate: searchForm.dateRange && searchForm.dateRange[0] || undefined,
    endDate: searchForm.dateRange && searchForm.dateRange[1] || undefined
  }
  const data = await request.get('/user/artist/list', { params })
    tableData.value = data.records || data.list || []
    pagination.total = data.total || 0
    pendingCount.value = data.pendingCount || 0
    approvedCount.value = data.approvedCount || 0
    rejectedCount.value = data.rejectedCount || 0
    hiddenCount.value = data.hiddenCount || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    pendingCount.value = 0
    approvedCount.value = 0
    rejectedCount.value = 0
    hiddenCount.value = 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pagination.page = 1
  loadData()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const params = { 
      page: 1, size: 10000,
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      keyword: searchForm.keyword || undefined,
      phone: searchForm.phone || undefined,
      userId: searchForm.userId || undefined,
      badge: searchForm.badge || undefined,
      startDate: searchForm.dateRange && searchForm.dateRange[0] || undefined,
      endDate: searchForm.dateRange && searchForm.dateRange[1] || undefined
    }
    const data = await request.get('/user/artist/list', { params })
    const list = data.records || data.list || []
    if (list.length === 0) { ElMessage.warning('没有数据可以导出'); return }
    
    const headers = ['ID', '昵称', '真实姓名', '手机号', '认证等级', '认证状态', '注册时间']
    const rows = list.map(item => [
      item.displayId || item.id || '',
      item.nickname || item.userNickname || '',
      item.realName || '',
      item.phone || item.userPhone || '',
      item.badge || '',
      item.status === 1 ? '已认证' : item.status === 0 ? '待审核' : item.status === 2 ? '未通过' : '已隐藏',
      item.createTime || item.registerTime || ''
    ])
    
    let csvContent = headers.join(',') + '\n'
    rows.forEach(row => { csvContent += row.map(cell => `"${cell}"`).join(',') + '\n' })
    
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `艺术家列表_${new Date().toISOString().split('T')[0]}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success(`成功导出 ${list.length} 条数据`)
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || '未知错误'))
  } finally {
    exportLoading.value = false
  }
}

const viewMaterials = (row) => {
  currentRecord.value = row
  materialsVisible.value = true
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该艺术家认证吗？', '提示', { type: 'success' })
    await request.post('/user/artist/approve', { id: row.id })
    ElMessage.success('已通过认证')
    await loadData()
  } catch (e) {}
}

const handleReject = (row) => {
  currentRecord.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await request.post('/user/artist/reject', { id: currentRecord.value.id, reason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    await loadData()
  } catch (e) {}
}

const assertAdminPermission = () => {
  try {
    const info = JSON.parse(localStorage.getItem('admin_info') || '{}')
    if (info?.role === 'admin' || info?.role === 'super') return true
  } catch (e) {}
  ElMessage.error('需要管理员权限')
  return false
}

// 隐藏艺术家
const handleHide = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定隐藏艺术家"${row.realName || row.nickname || row.userNickname || '-'}"吗？隐藏后前端艺术家入口不展示，但认证资料仍保留。`,
      '隐藏艺术家',
      {
        confirmButtonText: '确认隐藏',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await request.post('/user/artist/hide', { id: row.id })
    ElMessage.success('已隐藏艺术家')
    await loadData()
  } catch (e) {}
}

const handleUnhide = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定重新显示艺术家"${row.realName || row.nickname || row.userNickname || '-'}"吗？`,
      '重新显示',
      {
        confirmButtonText: '确认显示',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    await request.post('/user/artist/approve', { id: row.id, badge: row.badge || '' })
    ElMessage.success('已重新显示')
    await loadData()
  } catch (e) {}
}

// 取消认证
const handleRevoke = async (row) => {
  if (!assertAdminPermission()) return
  try {
    await ElMessageBox.confirm(
      `确定取消艺术家"${row.realName || row.nickname || row.userNickname || '-'}"的认证吗？该操作会移除用户艺术家身份。`,
      '取消认证二次确认',
      {
        confirmButtonText: '确认取消认证',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await request.post('/user/artist/revoke', { id: row.id })
    ElMessage.success('已取消认证')
    await loadData()
  } catch (e) {}
}

const handleRevokeFromDetail = async () => {
  const row = currentRecord.value
  if (!row?.id) return
  await handleRevoke(row)
  detailVisible.value = false
}

// 重新认证
const handleReapply = async (row) => {
  try {
    await ElMessageBox.confirm('确定重新发起认证吗？', '提示', { type: 'info' })
    await request.post('/user/artist/reapply', { id: row.id })
    ElMessage.success('已重新发起认证')
    await loadData()
  } catch (e) {}
}

// 删除艺术家认证
const handleDelete = async (row) => {
  if (!assertAdminPermission()) return
  try {
    const artworkCount = Number(row.artworkCount || 0)
    if (artworkCount > 0) {
      ElMessage.warning(`该艺术家名下还有 ${artworkCount} 件作品，请先删除艺术家名下的作品`)
      return
    }

    await ElMessageBox.confirm(`确定要删除艺术家"${row.realName || row.nickname || row.userNickname}"的认证记录吗？删除前请确认已删除该艺术家名下所有作品。`, '删除艺术家二次确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/user/artist/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || e?.response?.data?.message || '删除失败')
    }
  }
}

// 添加艺术家
const showAddDialog = () => {
  Object.assign(addForm, { phone: '', nickname: '', avatar: '', realName: '', idCard: '', badge: '', resume: '' })
  addVisible.value = true
}

// 添加艺术家 - 上传头像
const handleAddAvatarUpload = async (options) => {
  const { file, onSuccess, onError } = options
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    onError(new Error('请选择图片文件'))
    return
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    onError(new Error('图片大小不能超过 10MB'))
    return
  }

  try {
    const result = await uploadFile(file)
    addForm.avatar = result?.url || result || ''
    ElMessage.success('头像上传成功')
    onSuccess()
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
    onError(e)
  }
}

const confirmAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    const result = await request.post('/user/artist/add', addForm)
    // 显示完整信息包括 UID
    const uidInfo = result.userUid ? `，UID：${result.userUid}` : ''
    const msg = result.message || (result.isNewUser ? `新用户已创建，用户ID：${result.userId}${uidInfo}` : `用户ID：${result.userId}${uidInfo}`)
    ElMessage.success({ message: '添加成功！' + msg, duration: 8000 })
    addVisible.value = false
    await loadData()
  } catch (e) {}
}

// 设置等级
const showBadgeDialog = (row) => {
  currentRecord.value = row
  selectedBadge.value = row.badge || ''
  badgeVisible.value = true
}

const confirmBadge = async () => {
  try {
    await request.post('/user/artist/badge', { 
      id: currentRecord.value.id, 
      badge: selectedBadge.value 
    })
    ElMessage.success('等级设置成功')
    badgeVisible.value = false
    await loadData()
  } catch (e) {}
}

// 打开添加作品弹窗
const openArtworkDialog = () => {
  artworkTab.value = 'select'
  existingWorks.value = []
  existingWorksPage.value = 1
  existingWorksTotal.value = 0
  artworkKeyword.value = ''
  selectedExistingId.value = null
  selectedExistingTitle.value = ''
  artworkForm.title = ''
  artworkForm.authorName = profileForm.realName || profileForm.nickname || ''
  artworkForm.categoryId = ''
  artworkForm.cover = ''
  artworkForm.price = null
  artworkForm.originalPrice = null
  artworkForm.stock = 1
  artworkForm.description = ''
  artworkForm.ownershipType = 1
  artworkForm.artType = ''
  artworkForm.size = ''
  artworkForm.year = null
  loadCategories()
  loadExistingWorks()
  artworkDialogVisible.value = true
}

// 加载分类
const loadCategories = async () => {
  if (artworkCategories.value.length > 0) return
  try {
    const res = await requestApi.get('/product/categories')
    artworkCategories.value = res || []
  } catch (e) {
    artworkCategories.value = []
  }
}

// 加载已有作品
const loadExistingWorks = async () => {
  existingWorksLoading.value = true
  try {
    const res = await requestApi.get('/product/list', {
      params: {
        page: existingWorksPage.value,
        pageSize: existingWorksSize,
        title: artworkKeyword.value || undefined
      }
    })
    if (existingWorksPage.value === 1) {
      existingWorks.value = res.records || res.list || []
    } else {
      existingWorks.value = [...existingWorks.value, ...(res.records || res.list || [])]
    }
    existingWorksTotal.value = res.total || existingWorks.value.length
  } catch (e) {
    console.error('加载作品失败', e)
    existingWorks.value = []
  } finally {
    existingWorksLoading.value = false
  }
}

// 搜索已有作品
const searchExistingWorks = () => {
  existingWorksPage.value = 1
  loadExistingWorks()
}

// 加载更多已有作品
const loadMoreExistingWorks = () => {
  existingWorksPage.value++
  loadExistingWorks()
}

// 选择已有作品
const selectExistingWork = (work) => {
  selectedExistingId.value = work.id
  selectedExistingTitle.value = work.title
}

// 确认选择已有作品
const confirmSelectExisting = async () => {
  if (!selectedExistingId.value) {
    ElMessage.warning('请选择一个作品')
    return
  }
  
  try {
    artworkLoading.value = true
    await requestApi.post('/product/create', {
      authorId: currentUser.value.userId || currentUser.value.id,
      title: selectedExistingTitle.value,
      cover: existingWorks.value.find(w => w.id === selectedExistingId.value)?.coverImage || '',
      price: existingWorks.value.find(w => w.id === selectedExistingId.value)?.price || 0,
      originalPrice: existingWorks.value.find(w => w.id === selectedExistingId.value)?.originalPrice || 0,
      authorName: existingWorks.value.find(w => w.id === selectedExistingId.value)?.authorName || '',
      categoryId: existingWorks.value.find(w => w.id === selectedExistingId.value)?.categoryId || undefined,
      artType: existingWorks.value.find(w => w.id === selectedExistingId.value)?.artType || '',
      size: existingWorks.value.find(w => w.id === selectedExistingId.value)?.size || '',
      year: existingWorks.value.find(w => w.id === selectedExistingId.value)?.year || undefined,
      ownershipType: existingWorks.value.find(w => w.id === selectedExistingId.value)?.ownershipType || 1,
      stock: 1,
      description: existingWorks.value.find(w => w.id === selectedExistingId.value)?.description || ''
    })
    ElMessage.success('作品添加成功')
    artworkDialogVisible.value = false
    // 刷新作品列表
    artworksPage.value = 1
    await loadUserArtworks(currentUser.value.userId || currentUser.value.id)
  } catch (e) {
    ElMessage.error('添加失败：' + (e.message || '未知错误'))
  } finally {
    artworkLoading.value = false
  }
}

// 上传作品封面
const handleArtworkCoverUpload = async (options) => {
  const { file, onSuccess, onError } = options
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    onError(new Error('请选择图片文件'))
    return
  }
  
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB')
    onError(new Error('图片大小不能超过 10MB'))
    return
  }

  try {
    const result = await uploadFile(file)
    artworkForm.cover = result?.url || result || ''
    ElMessage.success('封面上传成功')
    onSuccess()
  } catch (e) {
    ElMessage.error(e.message || '封面上传失败')
    onError(e)
  }
}

// 提交作品
const submitArtwork = async () => {
  if (!artworkForm.title) {
    ElMessage.warning('请输入作品标题')
    return
  }
  if (!artworkForm.price) {
    ElMessage.warning('请输入售价')
    return
  }
  if (!artworkForm.cover) {
    ElMessage.warning('请上传作品封面')
    return
  }
  
  // 如果原价为空，则原价=价格
  if (!artworkForm.originalPrice) {
    artworkForm.originalPrice = artworkForm.price
  }
  
  try {
    artworkLoading.value = true
    await requestApi.post('/product/create', {
      authorId: currentUser.value.userId || currentUser.value.id,
      ...artworkForm
    })
    ElMessage.success('作品添加成功')
    artworkDialogVisible.value = false
    // 刷新作品列表
    artworksPage.value = 1
    await loadUserArtworks(currentUser.value.userId || currentUser.value.id)
  } catch (e) {
    ElMessage.error('添加失败：' + (e.message || '未知错误'))
  } finally {
    artworkLoading.value = false
  }
}

// ==================== 批量操作方法 ====================

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 清除选择
const clearSelection = () => {
  selectedRows.value = []
}

// 批量通过
const handleBatchApprove = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择艺术家')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要通过选中的 ${selectedRows.value.length} 个艺术家认证吗？`, 
      '批量通过确认', 
      { type: 'success' }
    )
    
    batchLoading.value = true
    const ids = selectedRows.value.map(row => row.id)
    await request.post('/artist/batchApprove', {
      ids
    })
    ElMessage.success('批量通过成功')
    clearSelection()
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量通过失败：' + (e.message || '未知错误'))
    }
  } finally {
    batchLoading.value = false
  }
}

// 显示批量拒绝对话框
const showBatchRejectDialog = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择艺术家')
    return
  }
  
  batchRejectReason.value = ''
  batchRejectDialogVisible.value = true
}

// 确认批量拒绝
const confirmBatchReject = async () => {
  if (!batchRejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  
  try {
    batchLoading.value = true
    const ids = selectedRows.value.map(row => row.id)
    await request.post('/artist/batchReject', {
      ids,
      reason: batchRejectReason.value
    })
    ElMessage.success('批量拒绝成功')
    batchRejectDialogVisible.value = false
    clearSelection()
    await loadData()
  } catch (e) {
    ElMessage.error('批量拒绝失败：' + (e.message || '未知错误'))
  } finally {
    batchLoading.value = false
  }
}

// 批量隐藏
const handleBatchHide = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择艺术家')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要隐藏选中的 ${selectedRows.value.length} 个艺术家吗？`, 
      '批量隐藏确认', 
      { type: 'warning' }
    )
    
    batchLoading.value = true
    const ids = selectedRows.value.map(row => row.id)
    await request.post('/artist/batchHide', {
      ids
    })
    ElMessage.success('批量隐藏成功')
    clearSelection()
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量隐藏失败：' + (e.message || '未知错误'))
    }
  } finally {
    batchLoading.value = false
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择艺术家')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个艺术家吗？此操作不可恢复！`, 
      '批量删除确认', 
      { 
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning' 
      }
    )
    
    batchLoading.value = true
    const ids = selectedRows.value.map(row => row.id)
    await request.post('/user/artist/batchDelete', {
      ids
    })
    ElMessage.success('批量删除成功')
    clearSelection()
    await loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('批量删除失败：' + (e.message || '未知错误'))
    }
  } finally {
    batchLoading.value = false
  }
}

onMounted(async () => {
  await loadData()
  await nextTick()
  await openUserProfileFromRoute()
})
</script>

<style scoped>
/* 批量操作栏样式 */
.batch-actions {
  margin-bottom: 15px;
  padding: 12px 18px;
  background: #f0f9ff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.selected-count {
  font-weight: bold;
  color: #409eff;
  margin-right: 5px;
}

.page-container {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.status-tabs {
  margin-bottom: 20px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .el-avatar {
    flex-shrink: 0;
    border-radius: 50%;
    overflow: hidden;
  }
}
.user-detail {
  flex: 1;
  min-width: 0;
  
  .nickname {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .phone {
    font-size: 12px;
    color: #999;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.certified {
  color: #67c23a;
  font-size: 12px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.materials {
  .images-section {
    margin-top: 20px;
  }
  .section-title {
    margin-bottom: 10px;
    font-weight: 500;
  }
  .image-list {
    display: flex;
    flex-wrap: wrap;
  }
}

/* 用户详情弹窗样式 */
.user-profile {
  .profile-header {
    display: flex;
    gap: 20px;
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #eee;
  }
  
  .avatar-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }
  
  .avatar-uploader {
    display: block;
  }
  
  .profile-info {
    flex: 1;
    
    h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .user-id {
      margin: 0 0 8px 0;
      font-size: 12px;
      color: #999;
    }
  }
  
  .profile-form {
    .info-item {
      .label {
        display: block;
        font-size: 12px;
        color: #999;
        margin-bottom: 4px;
      }
      .value {
        font-size: 14px;
        color: #333;
      }
    }
  }
}

.identity-tags {
  display: flex;
  gap: 4px;
}

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.clickable-avatar:hover {
  transform: scale(1.05);
}

.resume-text {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 4位数ID显示样式 */
.id-display {
  font-family: 'Consolas', 'Monaco', monospace;
  font-weight: 600;
  color: #409eff;
  letter-spacing: 1px;
  font-size: 11px;
  cursor: pointer;
  
  &:hover {
    text-decoration: underline;
  }
}

.artworks-section {
  min-height: 100px;
  padding: 10px 0;
}

.artwork-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.artwork-item {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.3s;
  
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
  
  .artwork-cover {
    width: 100%;
    height: 100px;
    display: block;
  }
  
  .artwork-info {
    padding: 8px;
    
    .artwork-title {
      margin: 0 0 4px 0;
      font-size: 12px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .artwork-price {
      margin: 0;
      font-size: 12px;
      color: #f56c6c;
      font-weight: bold;
    }
  }
}

.load-more {
  text-align: center;
  padding: 10px 0;
}

.add-artwork-btn {
  text-align: center;
  padding: 12px 0;
  border-top: 1px dashed #eee;
  margin-top: 10px;
}

.cover-uploader {
  display: flex;
  align-items: center;
  gap: 15px;
}

.cover-preview {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  border: 1px solid #eee;
}

.dialog-footer-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

/* 作品弹窗Tab样式 */
.artwork-tabs {
  margin-bottom: 10px;
}

.existing-works {
  .works-filter {
    display: flex;
    gap: 10px;
    margin-bottom: 15px;
    align-items: center;
  }

  .works-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    max-height: 350px;
    overflow-y: auto;
    padding: 5px;
  }

  .work-card {
    border: 2px solid transparent;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.2s;
    position: relative;

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }

    &.selected {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }

    .work-cover {
      width: 100%;
      height: 100px;
      display: block;
    }

    .work-info {
      padding: 8px;

      .work-title {
        margin: 0 0 4px 0;
        font-size: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .work-author {
        margin: 0 0 4px 0;
        font-size: 11px;
        color: #999;
      }

      .work-price {
        margin: 0;
        font-size: 12px;
        color: #f56c6c;
        font-weight: bold;
      }
    }

    .selected-badge {
      position: absolute;
      top: 5px;
      right: 5px;
      width: 24px;
      height: 24px;
      background: #409eff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }
  }

  .load-more-works {
    text-align: center;
    padding: 10px 0;
  }

  .select-action {
    text-align: center;
    padding-top: 15px;
    border-top: 1px solid #eee;
    margin-top: 15px;
  }
}
</style>
