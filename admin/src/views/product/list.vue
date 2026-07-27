<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">作品列表</span>
      <div class="header-actions">
        <el-button type="warning" @click="goPriceControl">
          <el-icon><Setting /></el-icon>价格调控
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>增加作品
        </el-button>
      </div>
    </div>

    <!-- 价格增长配置弹窗 -->
    <el-dialog v-model="priceConfigVisible" title="作品价格增长配置" width="800px" destroy-on-close>
      <el-form :model="priceConfigForm" label-width="120px">
        <el-form-item label="功能开关">
          <el-switch v-model="priceConfigForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        
        <el-divider content-position="left">时间因素</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="基础日增长率">
              <el-input-number v-model="priceConfigForm.baseDailyRate" :min="0" :precision="4" :step="0.0001" />
              <span class="unit">（每天增长比例）</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成熟期天数">
              <el-input-number v-model="priceConfigForm.matureDays" :min="0" :max="365" />
              <span class="unit">天</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成熟期日增长率">
              <el-input-number v-model="priceConfigForm.matureDailyRate" :min="0" :precision="4" :step="0.0001" />
              <span class="unit">（超过天数后）</span>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-divider content-position="left">艺术家知名度系数</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="普通艺术家">
              <el-input-number v-model="priceConfigForm.defaultBadgeRate" :min="0.1" :max="10" :precision="2" :step="0.1" />
              <span class="unit">倍</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认证艺术家">
              <el-input-number v-model="priceConfigForm.verifiedBadgeRate" :min="0.1" :max="10" :precision="2" :step="0.1" />
              <span class="unit">倍</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人气艺术家">
              <el-input-number v-model="priceConfigForm.popularBadgeRate" :min="0.1" :max="10" :precision="2" :step="0.1" />
              <span class="unit">倍</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="大师级艺术家">
              <el-input-number v-model="priceConfigForm.masterBadgeRate" :min="0.1" :max="10" :precision="2" :step="0.1" />
              <span class="unit">倍</span>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-divider content-position="left">热度系数</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="浏览量阈值">
              <el-input-number v-model="priceConfigForm.viewThreshold" :min="0" />
              <span class="unit">次</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="浏览量加成">
              <el-input-number v-model="priceConfigForm.viewRate" :min="1" :max="10" :precision="2" />
              <span class="unit">倍</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收藏量阈值">
              <el-input-number v-model="priceConfigForm.favoriteThreshold" :min="0" />
              <span class="unit">次</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收藏量加成">
              <el-input-number v-model="priceConfigForm.favoriteRate" :min="1" :max="10" :precision="2" />
              <span class="unit">倍</span>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-divider content-position="left">销售加成与涨幅限制</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="单次销售加成">
              <el-input-number v-model="priceConfigForm.saleRate" :min="0" :max="1" :precision="4" :step="0.01" />
              <span class="unit">（每次加成比例）</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最多计算次数">
              <el-input-number v-model="priceConfigForm.maxSaleCount" :min="0" :max="100" />
              <span class="unit">次</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大涨幅倍数">
              <el-input-number v-model="priceConfigForm.maxGrowthMultiple" :min="1" :max="100" :precision="1" :step="0.5" />
              <span class="unit">倍</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="priceConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="savePriceConfig" :loading="priceConfigLoading">保存配置</el-button>
      </template>
    </el-dialog>
    
    <div class="search-form">
      <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
        <el-form-item label="作品uid">
          <el-input v-model="searchForm.artworkId" placeholder="请输入作品uid" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="作品名称">
          <el-input v-model="searchForm.title" placeholder="请输入作品名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="艺术家">
          <el-input v-model="searchForm.artistName" placeholder="请输入艺术家" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类/画种">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable filterable>
            <el-option-group label="作品分类">
              <el-option v-for="cat in categories" :key="'cat_'+cat.id" :label="cat.name" :value="'cat_'+cat.id" />
            </el-option-group>
            <el-option-group label="画种">
              <el-option v-for="type in artTypes" :key="'type_'+type" :label="type" :value="'type_'+type" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="上架" value="1" />
            <el-option label="下架" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <div class="sort-controls">
            <el-select v-model="searchForm.sortField" placeholder="时间" @change="handleSearch">
              <el-option label="时间" value="publishTime" />
              <el-option label="热度" value="heat" />
              <el-option label="权重" value="weight" />
              <el-option label="价格" value="price" />
            </el-select>
            <el-select v-model="searchForm.sortOrder" placeholder="排序方向" @change="handleSearch">
              <el-option label="从高到低" value="desc" />
              <el-option label="从低到高" value="asc" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe :row-class-name="getProductRowClassName">
      <el-table-column label="作品uid" width="150">
        <template #default="{ row }">
          <span class="artwork-code">{{ getArtworkUid(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="作品信息" min-width="280">
        <template #default="{ row }">
          <div class="artwork-info">
            <div class="cover-wrapper" @click="handleEdit(row)">
              <el-image 
                :src="getArtworkCoverUrl(row)" 
                :preview-src-list="getArtworkCoverUrl(row) ? [getArtworkCoverUrl(row)] : []" 
                style="width: 80px; height: 80px" 
                fit="cover"
                preview-teleported
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="edit-overlay">
                <el-icon><Edit /></el-icon>
              </div>
            </div>
            <div class="detail">
              <p class="title" @click="handleEdit(row)">{{ row.title }}</p>
              <p class="artist artist-link" @click.stop="openArtistEditor(row)">
                {{ row.artistName }}
                <span class="artist-id-inline">UID: {{ getAuthorUid(row) }}</span>
              </p>
              <p v-if="row.isResaleArtwork" class="holder-info">
                持有者 {{ row.holderNickname || '-' }}
                <span class="artist-id-inline">UID: {{ getHolderUid(row) }}</span>
              </p>
              <p class="category">
                <el-tag
                  v-for="tag in getArtworkCategoryTags(row)"
                  :key="tag.key"
                  size="small"
                  :type="tag.type"
                >
                  {{ tag.label }}
                </el-tag>
              </p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="150">
        <template #default="{ row }">
          <p>{{ formatMoneyYuan(getDisplayPrice(row)) }}</p>
          <p class="size-year" v-if="formatSizeYear(row)">{{ formatSizeYear(row) }}</p>
          <p class="original" v-if="row.activeResaleListing">转售挂单价: {{ formatMoneyYuan(getDisplayPrice(row)) }}</p>
          <p class="original" v-else-if="row.currentPrice && row.currentPrice !== row.price">发布价格: {{ formatMoneyYuan(getReleasePriceYuan(row)) }}</p>
          <p class="original" v-else-if="getReleasePriceYuan(row)">发布价格: {{ formatMoneyYuan(getReleasePriceYuan(row)) }}</p>
          <p class="price-rise" v-if="row.priceRise > 0" style="color: #ff4d4f; font-size: 12px;">
            涨幅 +{{ (row.priceRise * 100).toFixed(1) }}%
          </p>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isResaleArtwork ? 'warning' : 'success'" size="small">
            {{ row.isResaleArtwork ? '转售' : '原创' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收藏数" width="130">
        <template #default="{ row }">
          <div class="favorite-count-cell">
            <strong>{{ row.displayLikeCount || 0 }}</strong>
            <span>真实 {{ row.realFavoriteCount || 0 }}</span>
            <span v-if="row.configuredFavoriteCount">配置 +{{ row.configuredFavoriteCount }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="每日热度" width="120">
        <template #default="{ row }">
          <div class="daily-heat-cell">
            <span>浏览 {{ row.dailyViewCount || 0 }}</span>
            <span>收藏 {{ row.dailyLikeCount || 0 }}</span>
          </div>
        </template>
      </el-table-column>
<el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权重" width="130">
        <template #default="{ row }">
          <el-input-number 
            v-model="row.weight" 
            :min="0" 
            :max="9999" 
            size="small" 
            controls 
            class="weight-input"
            @change="handleWeightChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="分销" width="120">
        <template #default="{ row }">
          <div class="distribution-cell">
            <el-tag :type="row.distributionEnabled ? 'success' : 'info'" size="small">
              {{ row.distributionEnabled ? '已开启' : '未开启' }}
            </el-tag>
            <span class="commission-text" v-if="row.distributionEnabled">
              {{ row.commissionRate || 10 }}%
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="info" link @click="openArtworkPriceConfig(row)">价格配置</el-button>
          <el-button type="success" link @click="handleDistribution(row)">分销</el-button>
          <el-button type="warning" link @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <!-- 分销管理弹窗 -->
    <el-dialog v-model="distVisible" title="分销管理" width="600px" destroy-on-close>
      <div class="dist-info" v-if="distForm.artworkId">
        <div class="artwork-preview">
          <el-image :src="distForm.cover" style="width: 80px; height: 80px" fit="cover">
            <template #error>
              <div class="image-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="artwork-detail">
            <p class="name">{{ distForm.title }}</p>
            <p class="price">¥{{ distForm.price }}</p>
          </div>
        </div>
      </div>
      
      <el-form ref="distFormRef" :model="distForm" label-width="100px" class="dist-form">
        <el-form-item label="分销状态">
          <el-switch v-model="distForm.distributionEnabled" active-text="开启分销" inactive-text="关闭分销" />
        </el-form-item>
        
        <el-form-item label="分成比例" prop="commissionRate">
          <el-input-number 
            v-model="distForm.commissionRate" 
            :min="0" 
            :max="100" 
            :precision="0"
            :disabled="!distForm.distributionEnabled"
          />
          <span class="unit">%</span>
          <span class="tip">（订单金额的百分比作为分成）</span>
        </el-form-item>
        
        <el-form-item label="分成计算">
          <div class="calc-preview">
            <p>商品售价：¥{{ distForm.price || 0 }}</p>
            <p>分成比例：{{ distForm.commissionRate || 0 }}%</p>
            <p class="result">预估分成：¥{{ ((distForm.price || 0) * (distForm.commissionRate || 0) / 100).toFixed(2) }}</p>
          </div>
        </el-form-item>
        
        <el-divider content-position="left">分销统计</el-divider>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="stat-card">
              <p class="label">推广订单</p>
              <p class="value">{{ distForm.distributionOrders || 0 }}</p>
              <p class="unit">单</p>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <p class="label">分销分成</p>
              <p class="value">{{ distForm.distributionEarnings || 0 }}</p>
              <p class="unit">元</p>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <p class="label">推广人数</p>
              <p class="value">{{ distForm.distributionUsers || 0 }}</p>
              <p class="unit">人</p>
            </div>
          </el-col>
        </el-row>
        
        <el-form-item label="分销链接">
          <el-input :value="distLink" readonly>
            <template #append>
              <el-button @click="copyLink">复制</el-button>
            </template>
          </el-input>
          <p class="link-tip">推广员可通过此链接分享作品获得分成</p>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="distVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDistribution" :loading="distSaveLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑作品弹窗 -->
    <el-dialog v-model="editVisible" :title="editDialogTitle" width="800px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="作品名称" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入作品名称" />
        </el-form-item>
        <el-form-item label="艺术家" prop="artistName">
          <div class="artist-search-container">
            <el-autocomplete
              v-model="editForm.artistName"
              :fetch-suggestions="searchArtists"
              placeholder="输入艺术家名称或拼音首字母搜索"
              :trigger-on-focus="false"
              clearable
              style="width: 60%"
              @select="selectArtist"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #default="{ item }">
                <div class="artist-search-item">
                  <el-avatar v-if="item.avatar" :src="getFullImageUrl(item.avatar)" size="small" />
                  <el-avatar v-else size="small">{{ item.name?.charAt(0) }}</el-avatar>
                  <span class="artist-name">{{ item.name }}</span>
                  <span v-if="getArtistUid(item)" class="artist-code">UID: {{ getArtistUid(item) }}</span>
                  <el-tag v-if="item.certified" type="success" size="small">已认证</el-tag>
                </div>
              </template>
            </el-autocomplete>
            <span v-if="editForm.authorUid" class="artist-id-tag">UID: {{ editForm.authorUid }}</span>
          </div>
          <el-alert
            v-if="showArtistCreateTip"
            class="artist-create-tip"
            type="warning"
            :closable="false"
            show-icon
            title="数据库中未找到该艺术家，保存时将自动创建新艺术家并生成前端 UID"
          />
        </el-form-item>
        <el-form-item label="画种" prop="artType">
          <el-select v-model="editForm.artType" placeholder="请选择画种" style="width: 100%" clearable filterable>
            <el-option-group label="作品分类">
              <el-option v-for="cat in categories" :key="'cat_'+cat.id" :label="cat.name" :value="'分类:' + cat.name" />
            </el-option-group>
            <el-option-group label="画种">
              <el-option v-for="type in artTypes" :key="'type_'+type" :label="type" :value="type" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="尺寸">
          <el-input v-model="editForm.size" placeholder="如：100x80cm、四尺整张" />
        </el-form-item>
        <el-form-item label="创作年份">
          <el-input-number v-model="editForm.year" :min="1900" :max="2099" placeholder="如：2024" />
        </el-form-item>
        <el-form-item label="作品图片" prop="cover">
          <div class="upload-container">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :http-request="handleImageUpload"
              action="#"
            >
              <img v-if="editForm.cover" :src="getFullImageUrl(editForm.cover)" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">支持 JPG/PNG，大小不超过 10MB</div>
          </div>
        </el-form-item>
        <el-form-item :label="isEditResaleLocked ? '当前实时价格' : (editForm.artworkId ? '当前价格' : '价格')" prop="price">
          <el-input-number
            v-model="editForm.price"
            :min="0"
            :precision="2"
            :controls="false"
            :disabled="isEditResaleLocked"
            style="width: 100%"
          />
          <span style="color: #999; font-size: 12px;">
            {{ isEditResaleLocked ? '转售中展示前台成交价，保存时不回写上线价格' : (editForm.artworkId ? '单位：元，保存后会同步更新作品当前展示价格' : '单位：元') }}
          </span>
        </el-form-item>
        <el-form-item label="发布价格" prop="originalPrice">
          <el-input-number
            v-model="editForm.originalPrice"
            :min="0"
            :precision="2"
            :controls="false"
            style="width: 100%"
          />
          <span style="color: #999; font-size: 12px;">
            最初上线时的定价，单位：元
          </span>
        </el-form-item>
        <el-form-item label="作品类型" prop="ownershipType">
          <el-radio-group v-model="editForm.ownershipType">
            <el-radio :value="1">原创</el-radio>
            <el-radio :value="2">收藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作品描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入作品描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>

      <el-divider content-position="left">每日热度配置</el-divider>
      <div class="heat-config-summary">
        <div class="heat-summary-item">
          <span>每日浏览量</span>
          <b>{{ editForm.dailyViewCount || 0 }}</b>
          <small>保存时随机：{{ randomRangeText(editForm.dailyViewCount) }}</small>
        </div>
        <div class="heat-summary-item">
          <span>每日收藏量</span>
          <b>{{ editForm.dailyLikeCount || 0 }}</b>
          <small>保存时随机：{{ randomRangeText(editForm.dailyLikeCount) }}</small>
        </div>
        <div class="heat-summary-item">
          <span>预计涨价范围</span>
          <b>{{ editPriceIncreaseRangeText }}</b>
          <small>预计涨价数目：{{ editPriceIncreaseCountText }}</small>
        </div>
        <el-button
          v-if="editForm.artworkId"
          type="primary"
          plain
          @click="openArtworkPriceConfig(editForm)"
        >
          配置热度与涨价
        </el-button>
        <span v-else class="tip">新增作品保存后可配置热度与涨价</span>
      </div>
        
        <!-- 单个作品价格增长配置 -->
        <el-divider content-position="left">价格增长配置</el-divider>
        <div class="price-growth-config">
          <el-form-item label="配置方式">
            <el-tag :type="editForm.customPriceGrowthEnabled ? 'warning' : 'success'">
              {{ editForm.customPriceGrowthEnabled ? '自定义配置' : '使用全局调控' }}
            </el-tag>
            <el-button
              v-if="editForm.artworkId"
              class="config-button"
              type="primary"
              plain
              @click="openArtworkPriceConfig(editForm)"
            >
              单独配置
            </el-button>
            <span v-else class="tip">新增作品保存后可单独配置涨价规则</span>
          </el-form-item>
          <el-alert
            v-if="isEditResaleLocked"
            class="price-config-alert"
            type="warning"
            :closable="false"
            show-icon
            title="该作品正在转售，前台展示与购买流程使用转售挂单价，并继续参与自动涨价。"
          />
          <div class="price-growth-preview">
            <div>
              <span>最早上线价格</span>
              <b>{{ formatMoneyYuan(editEarliestOnlinePrice) }}</b>
            </div>
            <div>
              <span>{{ editCurrentPriceLabel }}</span>
              <b>{{ formatMoneyYuan(editDisplayPrice) }}</b>
            </div>
            <div>
              <span>累计涨幅</span>
              <b style="color: #ff4d4f;">{{ editGrowthText }}</b>
            </div>
            <div>
              <span>预计每日涨价</span>
              <b>{{ editPriceIncreaseRangeText }}</b>
            </div>
            <div>
              <span>每日涨价数目</span>
              <b>{{ editPriceIncreaseCountText }}</b>
            </div>
          </div>
          <el-divider content-position="left">涨价记录</el-divider>
          <div v-loading="priceLogLoading" class="price-log-panel">
            <template v-if="visiblePriceLogs.length">
              <div v-for="log in visiblePriceLogs" :key="log.id || `${log.createdAt}-${log.newPrice}-${log.changeReason}`" class="price-log-item">
                <div>
                  <b>{{ priceLogReasonText(log.changeReason) }}</b>
                  <p>{{ log.remark || '系统自动记录价格变化' }}</p>
                </div>
                <div class="price-log-meta">
                  <span>{{ formatPriceLogPrice(log.oldPrice) }} → {{ formatPriceLogPrice(log.newPrice) }}</span>
                  <em>{{ formatPriceLogChange(log) }}</em>
                  <small>{{ log.createdAt || '-' }}</small>
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无可计算的涨价记录" :image-size="64" />
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 单个作品价格增长配置弹窗 -->
    <el-dialog v-model="artworkPriceVisible" :title="artworkPriceDialogTitle" width="720px" destroy-on-close>
      <div v-loading="artworkPriceLoading">
        <el-alert
          class="price-config-alert"
          type="info"
          :closable="false"
          show-icon
          title="关闭自定义配置时，作品会使用「交易调控 / 涨价规则」里的全局配置。"
        />
        <el-form :model="artworkPriceForm" label-width="130px">
          <el-form-item label="配置模式">
            <el-switch
              v-model="artworkPriceForm.customPriceGrowthEnabled"
              active-text="自定义配置"
              inactive-text="使用全局配置"
            />
          </el-form-item>
          <template v-if="artworkPriceForm.customPriceGrowthEnabled">
            <el-divider content-position="left">时间因素</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="基础日增长率">
                  <el-input-number v-model="artworkPriceForm.customBaseDailyRate" :min="0" :precision="4" :step="0.0001" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="成熟期天数">
                  <el-input-number v-model="artworkPriceForm.customMatureDays" :min="0" :max="365" :precision="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="成熟期日增长率">
                  <el-input-number v-model="artworkPriceForm.customMatureDailyRate" :min="0" :precision="4" :step="0.0001" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-divider content-position="left">热度与涨幅</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="浏览量加成">
                  <el-input-number v-model="artworkPriceForm.customViewRate" :min="1" :max="10" :precision="2" />
                  <span class="unit">倍</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="收藏量加成">
                  <el-input-number v-model="artworkPriceForm.customFavoriteRate" :min="1" :max="10" :precision="2" />
                  <span class="unit">倍</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大涨幅倍数">
                  <el-input-number v-model="artworkPriceForm.customMaxGrowthMultiple" :min="1" :max="100" :precision="1" />
                  <span class="unit">倍</span>
                </el-form-item>
              </el-col>
            </el-row>
          </template>
          <el-divider content-position="left">作品热度</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="每日浏览量">
                <el-input-number v-model="artworkPriceForm.dailyViewCount" :min="0" :max="999999" :precision="0" />
                <div class="random-tip">保存时随机生成：{{ randomRangeText(artworkPriceForm.dailyViewCount) }}</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="每日收藏量">
                <el-input-number v-model="artworkPriceForm.dailyLikeCount" :min="0" :max="999999" :precision="0" />
                <div class="random-tip">保存时随机生成：{{ randomRangeText(artworkPriceForm.dailyLikeCount) }}</div>
              </el-form-item>
            </el-col>
          </el-row>
          <div class="price-config-preview">
            <span>展示浏览量：{{ artworkPriceForm.displayViewCount || 0 }}</span>
            <span>展示收藏量：{{ artworkPriceForm.displayLikeCount || 0 }}</span>
            <span>预计涨价范围：{{ artworkPriceIncreaseRangeText }}</span>
            <span>预计涨价数目：{{ artworkPriceIncreaseCountText }}</span>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="artworkPriceVisible = false">取消</el-button>
        <el-button type="primary" :loading="artworkPriceSaving" @click="saveArtworkPriceConfig">保存配置</el-button>
      </template>
    </el-dialog>

    <!-- 艺术家详情弹窗 -->
    <ArtistDetailDialog
      :visible="artistDialogVisible"
      :user-id="artistDialogUserId"
      :initial-data="artistDialogInitData"
      @close="artistDialogVisible = false"
      @saved="loadData"
    />

    <!-- 图片裁剪对话框 -->
    <ImageCropper
      :visible="cropperVisible"
      :file="cropperFile"
      @close="cropperVisible = false"
      @confirm="handleCropperConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, Edit, Setting, Search } from '@element-plus/icons-vue'
import { pinyin } from 'pinyin-pro'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { requestApi } from '@/api/request'
import { getFullImageUrl, uploadFile } from '@/api/request'
import { getArtworkPriceLogs } from '@/api/artworkPrice'
import ImageCropper from '@/components/ImageCropper.vue'
import ArtistDetailDialog from '@/components/ArtistDetailDialog.vue'

const SITE_ORIGIN = import.meta.env.VITE_SITE_ORIGIN || 'https://a.art1.cn'
const DEFAULT_ARTWORK_URL = '/images/default-artwork.png'
const router = useRouter()
const loading = ref(false)
const priceConfigVisible = ref(false)
const priceConfigLoading = ref(false)
const priceConfigForm = reactive({
  enabled: true,
  baseDailyRate: 0.0002,
  matureDailyRate: 0.0003,
  matureDays: 30,
  defaultBadgeRate: 1.0,
  verifiedBadgeRate: 1.5,
  popularBadgeRate: 2.0,
  masterBadgeRate: 3.0,
  viewThreshold: 100,
  viewRate: 1.1,
  favoriteThreshold: 5,
  favoriteRate: 1.1,
  saleRate: 0.05,
  maxSaleCount: 10,
  maxGrowthMultiple: 5.0
})

const showPriceConfig = async () => {
  priceConfigVisible.value = true
  try {
    const data = await request.get('/config/priceGrowth')
    Object.assign(priceConfigForm, data)
  } catch (e) {
    console.error('获取配置失败', e)
  }
}

const loadPriceGrowthConfig = async () => {
  try {
    const data = await request.silentGet('/config/priceGrowth')
    Object.assign(priceConfigForm, data)
  } catch (e) {
    console.warn('加载价格增长配置失败，使用默认配置:', e.message)
  }
}

const goPriceControl = () => {
  router.push('/price-control/price-rule')
}

const savePriceConfig = async () => {
  priceConfigLoading.value = true
  try {
    await request.post('/config/priceGrowth', priceConfigForm)
    ElMessage.success('配置保存成功')
    priceConfigVisible.value = false
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    priceConfigLoading.value = false
  }
}

const saveLoading = ref(false)
const tableData = ref([])
const categories = ref([])
const artTypes = ref(['国画', '油画', '水彩', '版画', '雕塑', '书法', '摄影', '数字艺术', '其他'])
const editVisible = ref(false)
const formRef = ref()
const distFormRef = ref()
const distVisible = ref(false)
const distSaveLoading = ref(false)
const distLink = ref('')

const searchForm = reactive({
  artworkId: '',
  title: '',
  artistName: '',
  categoryId: '',
  status: '',
  sortField: 'publishTime',
  sortOrder: 'desc'
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const editForm = reactive({
  artworkId: '',
  artworkUid: '',
  artworkCode: '',
  authorId: null,
  authorUid: '',
  title: '',
  artistName: '',
  artType: '',
  size: '',
  year: null,
  cover: '',
  price: 0,
  persistedPrice: 0,
  originalPrice: 0,
  releasePriceSnapshot: 0,
  currentPrice: 0,
  basePrice: 0,       // 原始/基础价格，用于计算每日涨幅
  priceRise: 0,
  tomorrowIncreaseMin: 0,
  tomorrowIncreaseMax: 0,
  ownershipType: 1,  // 默认原创
  description: '',
  status: 1,
  favoriteCount: 0,
  resaleListing: null,
  activeResaleListing: null,
  dailyViewCount: 0,
  dailyLikeCount: 0,
  displayViewCount: 0,
  displayLikeCount: 0,
  salesCount: 0,
  createTime: '',
  dailyViewMin: 0,
  dailyViewMax: 0,
  dailyLikeMin: 0,
  dailyLikeMax: 0,
  // 单个作品价格增长配置
  customPriceGrowthEnabled: false,
  customBaseDailyRate: 0.0002,
  customBaseDailyRateMin: 0.0002,
  customBaseDailyRateMax: 0.0002,
  customMatureDailyRate: 0.0003,
  customMatureDailyRateMin: 0.0003,
  customMatureDailyRateMax: 0.0003,
  customMatureDays: 30,
  customMatureDaysMin: 30,
  customMatureDaysMax: 30,
  customViewRate: 1.1,
  customViewRateMin: 1.1,
  customViewRateMax: 1.1,
  customFavoriteRate: 1.1,
  customFavoriteRateMin: 1.1,
  customFavoriteRateMax: 1.1,
  customMaxGrowthMultiple: 5.0,
  customMaxGrowthMultipleMin: 5.0,
  customMaxGrowthMultipleMax: 5.0
})

const artworkPriceVisible = ref(false)
const artworkPriceLoading = ref(false)
const artworkPriceSaving = ref(false)
const artworkPriceCurrent = ref(null)
const priceLogLoading = ref(false)
const priceLogs = ref([])
const artworkPriceForm = reactive({
  artworkId: '',
  artworkTitle: '',
  basePrice: 0,   // 原始/基础价格
  customPriceGrowthEnabled: false,
  customBaseDailyRate: 0.0002,
  customMatureDailyRate: 0.0003,
  customMatureDays: 30,
  customViewRate: 1.1,
  customFavoriteRate: 1.1,
  customMaxGrowthMultiple: 5.0,
  dailyViewCount: 0,
  dailyLikeCount: 0,
  displayViewCount: 0,
  displayLikeCount: 0
})

const artworkPriceDialogTitle = computed(() => {
  const title = artworkPriceForm.artworkTitle || artworkPriceCurrent.value?.title || '作品'
  return `${title} - 价格增长配置`
})

const formatMoneyYuan = (value) => {
  const amount = Number(value || 0)
  if (!Number.isFinite(amount)) return '¥0'
  return `¥${amount.toLocaleString(undefined, {
    minimumFractionDigits: amount % 1 === 0 ? 0 : 2,
    maximumFractionDigits: 2
  })}`
}

const getActiveResaleListing = (row = {}) => {
  const listing = row.activeResaleListing || row.resaleListing || null
  const status = String(listing?.status || '').toLowerCase()
  return listing && (!status || status === 'pending') ? listing : null
}

const getResalePriceYuan = (row = {}) => {
  const listing = getActiveResaleListing(row)
  const price = Number(listing?.resalePrice || 0)
  return Number.isFinite(price) && price > 0 ? price : 0
}

const getEditDisplayPrice = (row = {}) => {
  const resalePrice = getResalePriceYuan(row)
  return resalePrice || Number(row.currentPrice || row.price || 0)
}

const getReleasePriceYuan = (source = {}) => {
  const originalPrice = Number(source.originalPrice || 0)
  if (Number.isFinite(originalPrice) && originalPrice > 0) return originalPrice
  const basePrice = Number(source.basePrice || 0)
  if (Number.isFinite(basePrice) && basePrice > 0) return basePrice
  const price = Number(source.price || 0)
  if (Number.isFinite(price) && price > 0) return price
  const persistedPrice = Number(source.persistedPrice || 0)
  if (Number.isFinite(persistedPrice) && persistedPrice > 0) return persistedPrice
  const currentPrice = Number(source.currentPrice || 0)
  return Number.isFinite(currentPrice) ? currentPrice : 0
}

const getBasePriceFromDisplayPrice = (displayPrice, riseRate) => {
  const current = Number(displayPrice || 0)
  const rise = Number(riseRate || 0)
  if (!Number.isFinite(current) || current <= 0) return 0
  const multiplier = 1 + rise
  if (!Number.isFinite(multiplier) || multiplier <= 0) return current
  return Number((current / multiplier).toFixed(2))
}

const isEditResaleLocked = computed(() => getResalePriceYuan(editForm) > 0)
const editDisplayPrice = computed(() => {
  const resalePrice = getResalePriceYuan(editForm)
  return resalePrice || Number(editForm.price || 0)
})
const editCurrentPriceLabel = computed(() => isEditResaleLocked.value ? '转售挂单价' : '当前价格')
const getEarliestOnlinePriceYuanFromLogs = (logs = []) => {
  const prices = logs
    .map(log => Number(log?.oldPrice || 0))
    .filter(price => Number.isFinite(price) && price > 0)
  if (!prices.length) return 0
  return Math.min(...prices) / 100
}
const editEarliestOnlinePrice = computed(() => {
  return getReleasePriceYuan(editForm) || getEarliestOnlinePriceYuanFromLogs(priceLogs.value)
})
const editGrowthText = computed(() => {
  const base = Number(editEarliestOnlinePrice.value || 0)
  const current = Number(editDisplayPrice.value || 0)
  const rise = base > 0 && Number.isFinite(current) ? (current / base - 1) : Number(editForm.priceRise || 0)
  return `${rise > 0 ? '+' : ''}${(rise * 100).toFixed(2)}%`
})

const getPriceGrowthBaseYuan = (source = {}) => {
  const resalePrice = getResalePriceYuan(source)
  return Number(resalePrice || source.originalPrice || source.basePrice || source.price || 0)
}

const parseDateValue = (value) => {
  if (!value) return null
  const normalized = typeof value === 'string' ? value.replace(' ', 'T') : value
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

const getOnlineDays = (source = {}, dayOffset = 0) => {
  const createDate = parseDateValue(source.createTime)
  if (!createDate) return Math.max(0, dayOffset)
  const today = new Date()
  const start = new Date(createDate.getFullYear(), createDate.getMonth(), createDate.getDate())
  const end = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const days = Math.floor((end.getTime() - start.getTime()) / 86400000)
  return Math.max(0, days + dayOffset)
}

const getPriceGrowthConfigFor = (source = {}) => {
  const customEnabled = Boolean(source.customPriceGrowthEnabled)
  return {
    baseDailyRate: customEnabled && source.customBaseDailyRate != null
      ? Number(source.customBaseDailyRate)
      : Number(priceConfigForm.baseDailyRate || 0),
    matureDailyRate: customEnabled && source.customMatureDailyRate != null
      ? Number(source.customMatureDailyRate)
      : Number(priceConfigForm.matureDailyRate || priceConfigForm.baseDailyRate || 0),
    matureDays: customEnabled && source.customMatureDays != null
      ? Number(source.customMatureDays)
      : Number(priceConfigForm.matureDays || 0),
    artistRate: Number(priceConfigForm.defaultBadgeRate || 1),
    viewThreshold: Number(priceConfigForm.viewThreshold || 0),
    viewRate: customEnabled && source.customViewRate != null
      ? Number(source.customViewRate)
      : Number(priceConfigForm.viewRate || 1),
    favoriteThreshold: Number(priceConfigForm.favoriteThreshold || 0),
    favoriteRate: customEnabled && source.customFavoriteRate != null
      ? Number(source.customFavoriteRate)
      : Number(priceConfigForm.favoriteRate || 1),
    saleRate: Number(priceConfigForm.saleRate || 0),
    maxSaleCount: Number(priceConfigForm.maxSaleCount || 0),
    maxGrowthMultiple: customEnabled && source.customMaxGrowthMultiple != null
      ? Number(source.customMaxGrowthMultiple)
      : Number(priceConfigForm.maxGrowthMultiple || 1)
  }
}

const calculateConfiguredPrice = (source = {}, dayOffset = 0) => {
  const price = getPriceGrowthBaseYuan(source)
  if (!Number.isFinite(price) || price <= 0) return 0
  if (priceConfigForm.enabled === false) return price

  const config = getPriceGrowthConfigFor(source)
  const onlineDays = getOnlineDays(source, dayOffset)
  const matureDays = Math.max(0, config.matureDays || 0)
  const baseDays = matureDays > 0 ? Math.min(onlineDays, matureDays) : 0
  const matureGrowthDays = matureDays > 0 ? Math.max(onlineDays - matureDays, 0) : onlineDays
  const timeFactor = Math.pow(1 + (config.baseDailyRate || 0), baseDays) *
    Math.pow(1 + (config.matureDailyRate || 0), matureGrowthDays)

  const viewCount = Number(source.displayViewCount ?? source.viewCount ?? source.dailyViewCount ?? 0)
  const likeCount = Number(source.displayLikeCount ?? source.favoriteCount ?? source.dailyLikeCount ?? 0)
  const viewFactor = viewCount >= config.viewThreshold ? config.viewRate : 1
  const favoriteFactor = likeCount >= config.favoriteThreshold ? config.favoriteRate : 1
  const saleCount = Math.min(Math.max(Number(source.salesCount || source.saleCount || 0), 0), Math.max(config.maxSaleCount || 0, 0))
  const saleFactor = Math.pow(1 + (config.saleRate || 0), saleCount)
  const rawPrice = price * timeFactor * Math.max(config.artistRate || 1, 1) * viewFactor * favoriteFactor * saleFactor
  const maxPrice = price * Math.max(config.maxGrowthMultiple || 1, 1)
  return Number(Math.min(rawPrice, maxPrice).toFixed(2))
}

const getExpectedIncreaseRange = (source = {}) => {
  const price = getPriceGrowthBaseYuan(source)
  const explicitMin = Number(source.tomorrowIncreaseMin || 0)
  const explicitMax = Number(source.tomorrowIncreaseMax || 0)
  if (!source.ignorePresetIncrease && (explicitMin > 0 || explicitMax > 0)) {
    return {
      min: Math.min(explicitMin || explicitMax, explicitMax || explicitMin) / 100,
      max: Math.max(explicitMin, explicitMax) / 100
    }
  }

  if (!Number.isFinite(price) || price <= 0) return { min: 0, max: 0 }
  const todayPrice = calculateConfiguredPrice(source, 0)
  const tomorrowPrice = calculateConfiguredPrice(source, 1)
  const configuredIncrease = Math.max(tomorrowPrice - todayPrice, 0)
  if (configuredIncrease > 0) {
    return {
      min: configuredIncrease,
      max: configuredIncrease,
      currentPrice: todayPrice,
      nextPrice: tomorrowPrice
    }
  }
  const { baseDailyRate, matureDailyRate } = getPriceGrowthConfigFor(source)
  const minRate = Math.min(baseDailyRate || matureDailyRate || 0, matureDailyRate || baseDailyRate || 0)
  const maxRate = Math.max(baseDailyRate || 0, matureDailyRate || 0)
  return {
    min: price * minRate,
    max: price * maxRate
  }
}

const formatIncreaseRange = (range) => {
  const min = Number(range?.min || 0)
  const max = Number(range?.max || 0)
  if (min <= 0 && max <= 0) return '¥0'
  if (Math.abs(min - max) < 0.005) return formatMoneyYuan(min)
  return `${formatMoneyYuan(min)} - ${formatMoneyYuan(max)}`
}

const editPriceIncreaseSource = computed(() => ({
  ...editForm,
  ignorePresetIncrease: true
}))
const editPriceIncreaseRangeText = computed(() => formatIncreaseRange(getExpectedIncreaseRange(editPriceIncreaseSource.value)))
const editPriceIncreaseCountText = computed(() => {
  const range = getExpectedIncreaseRange(editPriceIncreaseSource.value)
  if (range.min <= 0 && range.max <= 0) return '暂无预计'
  return `最低 ${formatMoneyYuan(range.min)}，最高 ${formatMoneyYuan(range.max)}`
})
const artworkPriceIncreaseRangeText = computed(() => {
  const source = {
    ...artworkPriceForm,
    resaleListing: artworkPriceCurrent.value?.resaleListing || editForm.resaleListing,
    activeResaleListing: artworkPriceCurrent.value?.activeResaleListing || editForm.activeResaleListing,
    price: artworkPriceCurrent.value?.price || editForm.price,
    basePrice: artworkPriceCurrent.value?.price || editForm.basePrice || editForm.price,
    tomorrowIncreaseMin: artworkPriceCurrent.value?.tomorrowIncreaseMin || 0,
    tomorrowIncreaseMax: artworkPriceCurrent.value?.tomorrowIncreaseMax || 0
  }
  return formatIncreaseRange(getExpectedIncreaseRange(source))
})
const artworkPriceIncreaseCountText = computed(() => {
  const source = {
    ...artworkPriceForm,
    resaleListing: artworkPriceCurrent.value?.resaleListing || editForm.resaleListing,
    activeResaleListing: artworkPriceCurrent.value?.activeResaleListing || editForm.activeResaleListing,
    price: artworkPriceCurrent.value?.price || editForm.price,
    basePrice: artworkPriceCurrent.value?.price || editForm.basePrice || editForm.price,
    tomorrowIncreaseMin: artworkPriceCurrent.value?.tomorrowIncreaseMin || 0,
    tomorrowIncreaseMax: artworkPriceCurrent.value?.tomorrowIncreaseMax || 0
  }
  const range = getExpectedIncreaseRange(source)
  if (range.min <= 0 && range.max <= 0) return '暂无预计'
  return `最低 ${formatMoneyYuan(range.min)}，最高 ${formatMoneyYuan(range.max)}`
})

const visiblePriceLogs = computed(() => {
  const logs = []
  const range = getExpectedIncreaseRange(editPriceIncreaseSource.value)
  const yuanBase = getPriceGrowthBaseYuan(editPriceIncreaseSource.value)
  const currentPrice = Number(editDisplayPrice.value || 0)
  if (yuanBase > 0 && currentPrice > 0 && Math.abs(currentPrice - yuanBase) >= 0.005) {
    logs.push({
      id: 'current-preview',
      oldPrice: yuanBase * 100,
      newPrice: currentPrice * 100,
      changeRate: currentPrice / yuanBase - 1,
      changeReason: 'CURRENT',
      remark: '根据当前价格和发布价格实时计算累计涨幅',
      createdAt: '实时预览'
    })
  }
  if (range.min <= 0 && range.max <= 0) return logs
  const maxIncrease = Math.max(range.min, range.max)
  const forecastOldPrice = Number(range.currentPrice || yuanBase)
  const forecastNewPrice = Number(range.nextPrice || (forecastOldPrice + maxIncrease))
  logs.push({
    id: 'forecast',
    oldPrice: forecastOldPrice * 100,
    newPrice: forecastNewPrice * 100,
    changeAmount: maxIncrease,
    changeReason: 'FORECAST',
    remark: `按系统价格增长配置预估，下一日预计上涨 ${formatIncreaseRange(range)}`,
    createdAt: '实时预估'
  })
  return logs
})

// 艺术家搜索相关
const artistSearchResults = ref([])
const artistSearchLoading = ref(false)
const artistSearchCache = ref([])
const artistSearchCacheLoaded = ref(false)
const artistSearchCacheLoading = ref(false)
const artistSearchVisible = ref(false)
const artistExactMatched = ref(false)

const editDialogTitle = computed(() => {
  const uid = editForm.artworkUid || editForm.artworkCode || editForm.artworkId
  return editForm.artworkId ? `编辑作品（作品UID：${uid || '-'}）` : '添加作品'
})

const showArtistCreateTip = computed(() => {
  return Boolean(editForm.artistName?.trim()) && !artistExactMatched.value && !editForm.authorId
})

const normalizeRange = (minValue, maxValue) => {
  const min = Math.max(Number(minValue || 0), 0)
  const max = Math.max(Number(maxValue || 0), 0)
  return min <= max ? [min, max] : [max, min]
}

const randomIntInRange = (minValue, maxValue) => {
  const [min, max] = normalizeRange(minValue, maxValue)
  return Math.floor(Math.random() * (max - min + 1)) + min
}

const normalizeNumberRange = (minValue, maxValue, minLimit = 0) => {
  const min = Math.max(Number(minValue || 0), minLimit)
  const max = Math.max(Number(maxValue || 0), minLimit)
  return min <= max ? [min, max] : [max, min]
}

const randomNumberInRange = (minValue, maxValue, precision = 2, minLimit = 0) => {
  const [min, max] = normalizeNumberRange(minValue, maxValue, minLimit)
  const value = min + Math.random() * (max - min)
  return Number(value.toFixed(precision))
}

const setDailyHeatRange = (viewCount = 0, likeCount = 0) => {
  const view = Math.max(Number(viewCount || 0), 0)
  const like = Math.max(Number(likeCount || 0), 0)
  editForm.dailyViewCount = view
  editForm.dailyLikeCount = like
  editForm.dailyViewMin = view
  editForm.dailyViewMax = view
  editForm.dailyLikeMin = like
  editForm.dailyLikeMax = like
}

const setPriceGrowthRanges = (config = {}) => {
  const baseDailyRate = Number(config.customBaseDailyRate ?? 0.0002)
  const matureDailyRate = Number(config.customMatureDailyRate ?? 0.0003)
  const matureDays = Number(config.customMatureDays ?? 30)
  const viewRate = Number(config.customViewRate ?? 1.1)
  const favoriteRate = Number(config.customFavoriteRate ?? 1.1)
  const maxGrowthMultiple = Number(config.customMaxGrowthMultiple ?? 5.0)

  Object.assign(editForm, {
    customBaseDailyRate: baseDailyRate,
    customBaseDailyRateMin: baseDailyRate,
    customBaseDailyRateMax: baseDailyRate,
    customMatureDailyRate: matureDailyRate,
    customMatureDailyRateMin: matureDailyRate,
    customMatureDailyRateMax: matureDailyRate,
    customMatureDays: matureDays,
    customMatureDaysMin: matureDays,
    customMatureDaysMax: matureDays,
    customViewRate: viewRate,
    customViewRateMin: viewRate,
    customViewRateMax: viewRate,
    customFavoriteRate: favoriteRate,
    customFavoriteRateMin: favoriteRate,
    customFavoriteRateMax: favoriteRate,
    customMaxGrowthMultiple: maxGrowthMultiple,
    customMaxGrowthMultipleMin: maxGrowthMultiple,
    customMaxGrowthMultipleMax: maxGrowthMultiple
  })
}

const getRowArtworkId = (row) => row?.artworkId || row?.id

const priceLogReasonText = (reason = '') => {
  const map = {
    DAILY: '每日涨价',
    SALE: '成交触发',
    COLLECT: '收藏触发',
    SCORE: '评分触发',
    MANUAL: '人工调价',
    CURRENT: '当前累计涨幅',
    FORECAST: '预计涨价'
  }
  return map[reason] || reason || '价格变化'
}

const formatPriceLogPrice = (value) => {
  const amount = Number(value || 0)
  return `¥${(amount / 100).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

const formatPriceLogRate = (value) => {
  const rate = Number(value || 0)
  if (!Number.isFinite(rate) || rate === 0) return '0%'
  const normalized = Math.abs(rate) > 1 ? rate : rate * 100
  return `${normalized > 0 ? '+' : ''}${normalized.toFixed(2)}%`
}

const formatPriceLogChange = (log = {}) => {
  if (log.changeReason === 'FORECAST') {
    const amount = Number(log.changeAmount || 0)
    return amount > 0 ? `+${formatMoneyYuan(amount)}` : formatMoneyYuan(0)
  }
  return formatPriceLogRate(log.changeRate)
}

const loadPriceLogs = async (artworkId) => {
  priceLogs.value = []
  if (!artworkId) return
  priceLogLoading.value = true
  try {
    const data = await getArtworkPriceLogs({ artworkId })
    priceLogs.value = Array.isArray(data) ? data.slice(0, 5) : []
  } catch (e) {
    console.warn('加载涨价记录失败:', e)
  } finally {
    priceLogLoading.value = false
  }
}

const getEightyPercentRange = (value) => {
  const base = Math.max(Number(value || 0), 0)
  if (!Number.isFinite(base) || base <= 0) return { min: 0, max: 0 }
  return {
    min: Math.max(Math.floor(base * 0.2), 0),
    max: Math.max(Math.ceil(base * 1.8), 0)
  }
}

const randomByEightyPercent = (value) => {
  const { min, max } = getEightyPercentRange(value)
  return randomIntInRange(min, max)
}

const randomRangeText = (value) => {
  const { min, max } = getEightyPercentRange(value)
  return `${min} - ${max}`
}

const openArtworkPriceConfig = async (row) => {
  const artworkId = getRowArtworkId(row)
  if (!artworkId) {
    ElMessage.warning('请先保存作品，再配置价格增长规则')
    return
  }
  artworkPriceCurrent.value = row
  artworkPriceVisible.value = true
  artworkPriceLoading.value = true
  loadPriceGrowthConfig()
  Object.assign(artworkPriceForm, {
    artworkId,
    artworkTitle: row.title || row.artworkTitle || '',
    basePrice: getPriceGrowthBaseYuan(row) || row.price || row.originalPrice || row.currentPrice || 0,
    customPriceGrowthEnabled: false,
    customBaseDailyRate: 0.0002,
    customMatureDailyRate: 0.0003,
    customMatureDays: 30,
    customViewRate: 1.1,
    customFavoriteRate: 1.1,
    customMaxGrowthMultiple: 5.0,
    dailyViewCount: row.dailyViewCount || 0,
    dailyLikeCount: row.dailyLikeCount || 0,
    displayViewCount: row.displayViewCount || 0,
    displayLikeCount: row.displayLikeCount || row.favoriteCount || 0
  })
  try {
    const data = await request.silentGet(`/product/${artworkId}/priceGrowth`)
    Object.assign(artworkPriceForm, {
      ...data,
      artworkId,
      artworkTitle: data.artworkTitle || row.title || '',
      customPriceGrowthEnabled: Boolean(data.customPriceGrowthEnabled),
      customBaseDailyRate: Number(data.customBaseDailyRate ?? 0.0002),
      customMatureDailyRate: Number(data.customMatureDailyRate ?? 0.0003),
      customMatureDays: Math.max(Number(data.customMatureDays ?? 30), 1),
      customViewRate: Number(data.customViewRate ?? 1.1),
      customFavoriteRate: Number(data.customFavoriteRate ?? 1.1),
      customMaxGrowthMultiple: Number(data.customMaxGrowthMultiple ?? 5.0),
      dailyViewCount: Number(data.dailyViewCount ?? 0),
      dailyLikeCount: Number(data.dailyLikeCount ?? 0),
      displayViewCount: Number(data.displayViewCount ?? 0),
      displayLikeCount: Number(data.displayLikeCount ?? 0)
    })
  } catch (e) {
    console.warn('加载单作品价格配置失败，使用默认值:', e.message)
  } finally {
    artworkPriceLoading.value = false
  }
}

const saveArtworkPriceConfig = async () => {
  const artworkId = artworkPriceForm.artworkId
  if (!artworkId) return
  artworkPriceSaving.value = true
  try {
    const params = {
      customPriceGrowthEnabled: artworkPriceForm.customPriceGrowthEnabled,
      customBaseDailyRate: artworkPriceForm.customBaseDailyRate,
      customMatureDailyRate: artworkPriceForm.customMatureDailyRate,
      customMatureDays: artworkPriceForm.customMatureDays,
      customViewRate: artworkPriceForm.customViewRate,
      customFavoriteRate: artworkPriceForm.customFavoriteRate,
      customMaxGrowthMultiple: artworkPriceForm.customMaxGrowthMultiple,
      dailyViewCount: randomByEightyPercent(artworkPriceForm.dailyViewCount),
      dailyLikeCount: randomByEightyPercent(artworkPriceForm.dailyLikeCount)
    }
    await request.silentPut(`/product/${artworkId}/priceGrowth`, params)
    ElMessage.success(`价格配置已保存，浏览 ${params.dailyViewCount}，收藏 ${params.dailyLikeCount}`)
    artworkPriceVisible.value = false
    if (Number(editForm.artworkId) === Number(artworkId)) {
      editForm.customPriceGrowthEnabled = params.customPriceGrowthEnabled
      setDailyHeatRange(params.dailyViewCount, params.dailyLikeCount)
    }
    loadPriceLogs(artworkId)
    loadData()
  } catch (e) {
    ElMessage.error('价格配置保存失败：' + (e.message || '未知错误'))
  } finally {
    artworkPriceSaving.value = false
  }
}

const getArtistUid = (artist = {}) => artist.uid || artist.userUid || artist.artistUid || artist.artistCode || artist.code || ''

const normalizeArtistOption = (artist = {}) => ({
  ...artist,
  name: artist.name || artist.nickname || artist.realName || artist.artistName || '',
  value: artist.name || artist.nickname || artist.realName || artist.artistName || '',
  uid: getArtistUid(artist)
})

const normalizeKeyword = (value = '') => String(value || '').trim().toLowerCase()

const getArtistSearchText = (artist = {}) => {
  const name = String(artist.name || '').trim()
  const nickname = String(artist.nickname || '').trim()
  const realName = String(artist.realName || '').trim()
  const uid = String(artist.uid || '').trim()
  const fullPinyin = normalizeKeyword(pinyin(name, { toneType: 'none', type: 'array' }).join(''))
  const initials = normalizeKeyword(pinyin(name, { toneType: 'none', pattern: 'first', type: 'array' }).join(''))

  return {
    name: normalizeKeyword(name),
    nickname: normalizeKeyword(nickname),
    realName: normalizeKeyword(realName),
    uid: normalizeKeyword(uid),
    fullPinyin,
    initials
  }
}

const getArtistSearchScore = (artist = {}, keyword = '') => {
  const normalizedKeyword = normalizeKeyword(keyword)
  if (!normalizedKeyword) return -1

  const searchText = getArtistSearchText(artist)
  if (searchText.name === normalizedKeyword) return 100
  if (searchText.nickname === normalizedKeyword || searchText.realName === normalizedKeyword) return 95
  if (searchText.uid === normalizedKeyword) return 90
  if (searchText.name.includes(normalizedKeyword)) return 80
  if (searchText.nickname.includes(normalizedKeyword) || searchText.realName.includes(normalizedKeyword)) return 75
  if (searchText.fullPinyin === normalizedKeyword) return 70
  if (searchText.initials === normalizedKeyword) return 68
  if (searchText.fullPinyin.startsWith(normalizedKeyword)) return 66
  if (searchText.initials.startsWith(normalizedKeyword)) return 64
  if (searchText.fullPinyin.includes(normalizedKeyword)) return 62
  if (searchText.initials.includes(normalizedKeyword)) return 60
  if (searchText.uid.includes(normalizedKeyword)) return 55
  return -1
}

const sortArtistSuggestions = (artistList = [], keyword = '') => {
  return artistList
    .map(artist => ({ artist, score: getArtistSearchScore(artist, keyword) }))
    .filter(item => item.score >= 0)
    .sort((a, b) => b.score - a.score || String(a.artist.name || '').localeCompare(String(b.artist.name || ''), 'zh-Hans-CN'))
    .slice(0, 10)
    .map(item => item.artist)
}

const ensureArtistSearchCache = async () => {
  if (artistSearchCacheLoaded.value || artistSearchCacheLoading.value) return
  artistSearchCacheLoading.value = true
  try {
    const res = await request.silentGet('/user/artist-search', {
      params: { limit: 100 }
    })
    artistSearchCache.value = (Array.isArray(res) ? res : []).map(normalizeArtistOption)
    artistSearchCacheLoaded.value = true
  } catch (e) {
    console.error('加载艺术家搜索缓存失败', e)
    artistSearchCache.value = []
  } finally {
    artistSearchCacheLoading.value = false
  }
}

// el-autocomplete 期望的搜索函数格式
const searchArtists = (keyword, callback) => {
  if (!keyword || keyword.length < 1) {
    artistExactMatched.value = false
    callback([])
    return
  }
  artistSearchLoading.value = true
  Promise.all([
    request.silentGet('/user/artist-search', {
      params: { keyword, limit: 20 }
    }),
    ensureArtistSearchCache()
  ]).then(([res]) => {
    const mergedArtists = new Map()
    ;(Array.isArray(res) ? res : []).map(normalizeArtistOption).forEach(item => {
      mergedArtists.set(String(item.id || item.uid || item.name), item)
    })
    artistSearchCache.value.forEach(item => {
      mergedArtists.set(String(item.id || item.uid || item.name), item)
    })
    artistSearchResults.value = sortArtistSuggestions(Array.from(mergedArtists.values()), keyword)
    const trimmed = keyword.trim()
    artistExactMatched.value = artistSearchResults.value.some(item => item.name === trimmed)
    if (!artistExactMatched.value && editForm.artistName === keyword) {
      editForm.authorId = null
      editForm.authorUid = ''
    }
    callback(artistSearchResults.value)
  }).catch(e => {
    console.error('搜索艺术家失败', e)
    artistSearchResults.value = []
    artistExactMatched.value = false
    callback([])
  }).finally(() => {
    artistSearchLoading.value = false
  })
}

const selectArtist = (artist) => {
  editForm.authorId = artist.id
  editForm.authorUid = getArtistUid(artist)
  editForm.artistName = artist.name
  artistExactMatched.value = true
  artistSearchVisible.value = false
  artistSearchResults.value = []
}

const distForm = reactive({
  artworkId: '',
  title: '',
  cover: '',
  price: 0,
  distributionEnabled: false,
  commissionRate: 10,
  distributionOrders: 0,
  distributionEarnings: 0,
  distributionUsers: 0
})

// 艺术家信息编辑相关
const artistDialogVisible = ref(false)
const artistDialogUserId = ref(null)
const artistDialogInitData = ref({})

const rules = {
  title: [{ required: true, message: '请输入作品名称', trigger: 'blur' }],
  artistName: [{ required: true, message: '请输入艺术家名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const getArtworkUid = (row) => row.artworkUid || row.artworkCode || row.displayArtworkId || row.artworkId || '-'

const getAuthorUid = (row) => row.authorUid || row.displayAuthorId || '-'

const getHolderUid = (row) => row.holderUid || '-'

const formatSizeYear = (row) => {
  const info = []
  if (row.size) info.push(row.size)
  if (row.year) info.push(`${row.year}年`)
  return info.join(' / ')
}

const normalizeArtworkTagKey = (value) => {
  return String(value || '')
    .trim()
    .replace(/^(分类|画种)[:：]\s*/, '')
    .replace(/\s+/g, '')
}

const getArtworkCategoryTags = (row = {}) => {
  const tags = []
  const seen = new Set()
  const addTag = (label, type) => {
    const text = String(label || '').trim()
    const key = normalizeArtworkTagKey(text)
    if (!text || !key || seen.has(key)) return
    seen.add(key)
    tags.push({ key, label: text, type })
  }

  addTag(row.categoryName, 'info')
  addTag(row.artType, 'warning')
  return tags
}

const getProductRowClassName = ({ row }) => {
  return Number(row?.status) === 1 ? '' : 'offline-artwork-row'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size }
    if (searchForm.artworkId) {
      const keyword = String(searchForm.artworkId).trim()
      if (/^\d+$/.test(keyword)) {
        params.id = keyword
      } else {
        params.artworkCode = keyword
      }
    }
    if (searchForm.title) params.title = searchForm.title
    if (searchForm.artistName) params.authorName = searchForm.artistName
    // 支持分类和画种搜索
    if (searchForm.categoryId) {
      if (searchForm.categoryId.startsWith('cat_')) {
        params.categoryId = searchForm.categoryId.replace('cat_', '')
      } else if (searchForm.categoryId.startsWith('type_')) {
        params.artType = searchForm.categoryId.replace('type_', '')
      }
    }
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.sortField) {
      params.sortField = searchForm.sortField
      params.sortOrder = searchForm.sortOrder || 'desc'
    }
    const data = await request.get('/product/list', { params })
    // 映射后端数据格式到前端
    tableData.value = (data.records || data.list || []).map(item => {
      const resaleListing = item.resaleListing || item.activeResaleListing || null
      const resaleStatus = String(resaleListing?.status || '').toLowerCase()
      const activeResaleListing = resaleListing && (!resaleStatus || resaleStatus === 'pending')
        ? resaleListing
        : null
      return {
        artworkId: item.id,
        artworkUid: item.artworkUid || item.artworkCode,
        authorId: item.authorId,
        holderId: item.holderId,
        displayArtworkId: item.displayArtworkId,
        displayAuthorId: item.displayAuthorId,
        authorUid: item.authorUid,
        holderUid: item.holderUid,
        holderNickname: item.holderNickname,
        isResaleArtwork: Boolean(item.isResaleArtwork),
        title: item.title,
        artistName: item.authorName || item.artistName,  // 后端返回 authorName
        cover: getFullImageUrl(item.coverImage || item.cover || DEFAULT_ARTWORK_URL), // 后端返回 coverImage
        categoryId: item.categoryId,
        categoryName: item.category,  // 后端返回 category
        artType: item.artType,        // 画种
        size: item.size,               // 尺寸
        year: item.year,              // 创作年份
        price: Number(item.price || 0),
        originalPrice: Number(item.originalPrice || 0),
        currentPrice: Number(item.currentPrice || 0),
        resaleListing,
        activeResaleListing,
        tomorrowIncreaseMin: item.tomorrowIncreaseMin || 0,
        tomorrowIncreaseMax: item.tomorrowIncreaseMax || 0,
        ownershipType: item.ownershipType || 1,
        artworkCode: item.artworkCode || item.artworkUid,
        status: item.status,
        weight: item.weight || 0,
        description: item.description,
        salesCount: item.salesCount || 0,
        favoriteCount: item.favoriteCount || 0,
        realFavoriteCount: item.realFavoriteCount ?? item.favoriteCount ?? 0,
        configuredFavoriteCount: item.configuredFavoriteCount ?? item.dailyLikeCount ?? 0,
        dailyViewCount: item.dailyViewCount || 0,
        dailyLikeCount: item.dailyLikeCount || 0,
        displayViewCount: item.displayViewCount || item.viewCount || 0,
        displayLikeCount: item.displayLikeCount ?? item.favoriteCount ?? 0,
        priceRise: item.priceRise || 0, // 价格增长率
        createTime: item.createTime,
        distributionEnabled: item.distributionEnabled || false,
        commissionRate: item.commissionRate || 10,
        distributionOrders: item.distributionOrders || 0,
        distributionEarnings: item.distributionEarnings || 0,
        distributionUsers: item.distributionUsers || 0
      }
    })
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载数据失败:', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const getArtworkCoverUrl = (row = {}) => {
  return getFullImageUrl(row.cover || row.coverImage || row.cover_image || DEFAULT_ARTWORK_URL)
}

const loadCategories = async () => {
  try {
    categories.value = await request.get('/product/categories')
  } catch (e) {
    categories.value = [
      { id: 1, name: '国画' },
      { id: 2, name: '油画' },
      { id: 3, name: '书法' },
      { id: 4, name: '版画' },
      { id: 5, name: '雕塑' }
    ]
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const resetSearch = () => {
  Object.assign(searchForm, {
    artworkId: '',
    title: '',
    artistName: '',
    categoryId: '',
    status: '',
    sortField: 'publishTime',
    sortOrder: 'desc'
  })
  handleSearch()
}

const openArtistEditor = (row) => {
  const authorId = row.authorId
  if (!authorId) {
    ElMessage.warning('该作品未关联艺术家')
    return
  }
  artistDialogUserId.value = authorId
  artistDialogInitData.value = {
    nickname: row.artistName || '',
    avatar: row.authorAvatar || '',
    displayId: row.authorUid || row.displayAuthorId || '',
    certified: row.certified
  }
  artistDialogVisible.value = true
}

const handleEdit = async (row) => {
  loadPriceGrowthConfig()
  // 合并画种和分类显示
  let artTypeValue = row.artType || ''
  if (row.categoryName && !artTypeValue) {
    artTypeValue = '分类:' + row.categoryName
  }
  Object.assign(editForm, {
    artworkId: row.artworkId,
    artworkUid: getArtworkUid(row),
    artworkCode: row.artworkCode || '',
    authorId: row.authorId || null,
    authorUid: getAuthorUid(row) !== '-' ? getAuthorUid(row) : '',
    title: row.title,
    artistName: row.artistName,
    artType: artTypeValue,
    size: row.size || '',
    year: row.year || null,
    cover: row.cover || '',
    price: getEditDisplayPrice(row),
    persistedPrice: row.price || row.currentPrice || 0,
    originalPrice: getReleasePriceYuan(row),
    releasePriceSnapshot: getReleasePriceYuan(row),
    currentPrice: row.currentPrice || row.price || 0,
    // 原始/基础价格：用于计算每日涨幅；优先取 price（原始上架价），否则取 originalPrice
    basePrice: getReleasePriceYuan(row),
    priceRise: row.priceRise || 0,
    tomorrowIncreaseMin: row.tomorrowIncreaseMin || 0,
    tomorrowIncreaseMax: row.tomorrowIncreaseMax || 0,
    ownershipType: row.ownershipType || 1,
    description: row.description || '',
    status: row.status,
    favoriteCount: row.favoriteCount || 0,
    resaleListing: row.resaleListing || null,
    activeResaleListing: row.activeResaleListing || null,
    displayViewCount: row.displayViewCount || row.viewCount || 0,
    displayLikeCount: row.displayLikeCount ?? row.favoriteCount ?? 0,
    salesCount: row.salesCount || row.saleCount || 0,
    createTime: row.createTime || '',
    customPriceGrowthEnabled: Boolean(row.customPriceGrowthEnabled)
  })
  setDailyHeatRange(row.dailyViewCount || 0, row.dailyLikeCount || 0)
  loadPriceLogs(row.artworkId || row.id)
  artistExactMatched.value = Boolean(editForm.authorId)
  editVisible.value = true
}

const getDisplayPrice = (row) => {
  const resalePrice = Number(row.activeResaleListing?.resalePrice || row.resaleListing?.resalePrice || 0)
  if (resalePrice > 0) return resalePrice
  // 后端已返回元值，普通作品继续显示自动增长后的当前价。
  return Number(row.currentPrice || row.price || 0)
}

const handleAdd = () => {
  // 重置表单
  Object.assign(editForm, {
    artworkId: '',
    artworkUid: '',
    artworkCode: '',
    authorId: null,
    authorUid: '',
    title: '',
    artistName: '',
    artType: '',
    size: '',
    year: null,
    cover: '',
    price: 0,
    persistedPrice: 0,
    originalPrice: 0,
    releasePriceSnapshot: 0,
    currentPrice: 0,
    basePrice: 0,
    priceRise: 0,
    tomorrowIncreaseMin: 0,
    tomorrowIncreaseMax: 0,
    ownershipType: 1,
    description: '',
    status: 1,
    favoriteCount: 0,
    resaleListing: null,
    activeResaleListing: null,
    displayViewCount: 0,
    displayLikeCount: 0,
    salesCount: 0,
    createTime: '',
    customPriceGrowthEnabled: false
  })
  setDailyHeatRange(0, 0)
  priceLogs.value = []
  setPriceGrowthRanges()
  artistExactMatched.value = false
  editVisible.value = true
}

const beforeImageUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  const isLt10M = file.size / 1024 / 1024 < 10
  
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

const handleImageUpload = async (options) => {
  const { file, onSuccess, onError } = options

  try {
    const result = await uploadFile(file)
    const imageUrl = typeof result === 'string'
      ? result
      : (result?.url || result?.data || result?.path || '')
    if (!imageUrl) {
      throw new Error('上传接口未返回图片地址')
    }
    editForm.cover = imageUrl
    onSuccess?.(result)
    ElMessage.success('图片上传成功')
  } catch (e) {
    onError?.(e)
    ElMessage.error('图片上传失败：' + (e.message || '未知错误'))
  }
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  saveLoading.value = true
  try {
    if (showArtistCreateTip.value) {
      await ElMessageBox.confirm(
        `艺术家“${editForm.artistName.trim()}”不存在，保存后将自动创建艺术家并生成 UID。是否继续？`,
        '创建新艺术家',
        { type: 'warning', confirmButtonText: '继续保存', cancelButtonText: '取消' }
      )
    }
    // 调试：打印编辑表单中的价格值
    console.log('【DEBUG】editForm.price:', editForm.price, 'editForm.originalPrice:', editForm.originalPrice)
    const dailyViewCount = randomIntInRange(editForm.dailyViewMin, editForm.dailyViewMax)
    const dailyLikeCount = randomIntInRange(editForm.dailyLikeMin, editForm.dailyLikeMax)
    editForm.dailyViewCount = dailyViewCount
    editForm.dailyLikeCount = dailyLikeCount
    const originalPriceForSave = Number(editForm.originalPrice || editForm.price || 0)
    
    const params = {
      title: editForm.title,
      authorId: editForm.authorId,
      authorName: editForm.artistName,
      artType: editForm.artType || null,
      size: editForm.size || null,
      year: editForm.year || null,
      cover: editForm.cover || null,
      price: editForm.price != null
        ? Number(isEditResaleLocked.value ? (editForm.persistedPrice || editForm.basePrice || editForm.price) : editForm.price)
        : null,
      originalPrice: Number.isFinite(originalPriceForSave) ? originalPriceForSave : null,
      ownershipType: editForm.ownershipType || 1,
      description: editForm.description,
      status: editForm.status,
      favoriteCount: Math.max(Number(editForm.favoriteCount || 0), 0),
      dailyViewCount,
      dailyLikeCount
    }
    console.log('【DEBUG】保存参数 params.price:', params.price, 'params.originalPrice:', params.originalPrice)
    console.log('保存参数:', params)
    
    if (editForm.artworkId) {
      // 更新作品
      params.id = Number(editForm.artworkId)
      await request.put(`/product/artwork/${editForm.artworkId}`, params)
      console.log('更新结果:')
      ElMessage.success('更新成功')
    } else {
      // 新增作品
      const res = await requestApi.post('/product/create', params)
      console.log('创建结果:', res)
      ElMessage.success('创建成功')
    }
    
    // 成功后刷新列表
    loadData()
    editVisible.value = false
  } catch (e) {
    console.error('保存失败详情:', e)
    const msg = e.response?.data?.message || e.message || '未知错误'
    ElMessage.error('保存失败：' + msg)
  } finally {
    saveLoading.value = false
  }
}

const handleToggleStatus = async (row) => {
  const currentStatus = Number(row.status)
  const newStatus = currentStatus === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  console.log('切换状态:', { artworkId: row.artworkId, currentStatus, newStatus })
  try {
    await ElMessageBox.confirm(`确定要${action}该作品吗？`, '提示', { type: 'warning' })
    console.log('确认后调用API...')
    // 调用后端 API 更新状态
    const res = await request.put(`/product/artwork/${row.artworkId}`, {
      status: newStatus
    })
    console.log('API响应:', res)
    row.status = newStatus
    // 成功后刷新列表获取最新数据
    await loadData()
    ElMessage.success(`${action}成功`)
  } catch (e) {
    console.error('操作失败:', e)
    const msg = e.response?.data?.message || e.message || '操作失败'
    if (msg !== 'cancel' && msg !== 'close') {
      ElMessage.error(msg)
    }
  }
}

// 权重变更
const handleWeightChange = async (row) => {
  try {
    await request.put(`/product/artwork/${row.artworkId}`, {
      weight: row.weight || 0
    })
    // 刷新列表
    loadData()
  } catch (e) {
    ElMessage.error('权重保存失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该作品吗？', '提示', { type: 'warning' })
    // 使用管理后台的接口删除作品
    await request.delete(`/product/artwork/${row.artworkId}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '删除失败'
    ElMessage.error(msg)
  }
}

const handleDistribution = (row) => {
  Object.assign(distForm, {
    artworkId: row.artworkId,
    title: row.title,
    cover: row.cover || '',
    price: row.price || 0,
    distributionEnabled: row.distributionEnabled || false,
    commissionRate: row.commissionRate || 10,
    distributionOrders: row.distributionOrders || 0,
    distributionEarnings: row.distributionEarnings || 0,
    distributionUsers: row.distributionUsers || 0
  })
  distLink.value = `${SITE_ORIGIN}/artwork/${row.artworkId}?from=dist`
  distVisible.value = true
}

const handleSaveDistribution = async () => {
  distSaveLoading.value = true
  try {
    console.log('分销保存参数:', distForm.artworkId, distForm.distributionEnabled, distForm.commissionRate)
    // 调用后端 API 保存分销设置
    await request.put(`/product/artwork/${distForm.artworkId}`, {
      distributionEnabled: distForm.distributionEnabled,
      commissionRate: distForm.commissionRate
    })
    // 保存成功后刷新列表
    loadData()
    ElMessage.success('分销设置已保存')
    distVisible.value = false
  } catch (e) {
    console.error('分销保存失败:', e)
    const msg = e.response?.data?.message || e.message || '未知错误'
    ElMessage.error('保存失败：' + msg)
  } finally {
    distSaveLoading.value = false
  }
}

const copyLink = () => {
  navigator.clipboard.writeText(distLink.value).then(() => {
    ElMessage.success('链接已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .title {
    font-size: 18px;
    font-weight: 600;
  }
  
  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.artwork-info {
  display: flex;
  gap: 12px;
  
  .cover-wrapper {
    position: relative;
    cursor: pointer;
    
    &:hover .edit-overlay {
      opacity: 1;
    }
    
    .edit-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 20px;
      opacity: 0;
      transition: opacity 0.2s;
      border-radius: 4px;
    }
  }
  
  .detail {
    .title {
      font-weight: 500;
      margin-bottom: 4px;
      cursor: pointer;
      &:hover {
        color: #409eff;
      }
    }
    .artist {
      font-size: 13px;
      color: #666;
      cursor: pointer;
      &:hover {
        color: #409eff;
      }
      .artist-id-inline {
        display: block;
        margin-top: 3px;
        color: #909399;
        font-size: 12px;
        line-height: 1.35;
        word-break: break-all;
      }
    }
    .category {
      font-size: 12px;
      color: #999;
    }
  }
}

.original {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

:deep(.offline-artwork-row) {
  color: #a8abb2;
  background-color: #f5f7fa !important;
}

:deep(.offline-artwork-row td.el-table__cell) {
  background-color: #f5f7fa !important;
}

:deep(.offline-artwork-row .artwork-code),
:deep(.offline-artwork-row .title),
:deep(.offline-artwork-row .artist),
:deep(.offline-artwork-row .size-year),
:deep(.offline-artwork-row .original),
:deep(.offline-artwork-row .daily-heat-cell),
:deep(.offline-artwork-row .favorite-count-cell),
:deep(.offline-artwork-row .commission-text) {
  color: #a8abb2 !important;
}

:deep(.offline-artwork-row .cover-wrapper) {
  opacity: 0.55;
  filter: grayscale(1);
}

.art-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  
  .art-type {
    color: #409eff;
    font-weight: 500;
  }
  .art-size {
    color: #666;
  }
  .art-year {
    color: #999;
  }
}

.image-placeholder {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 24px;
}

.artwork-code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-weight: 600;
  color: #409eff;
}

.upload-container {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.avatar-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color 0.2s;
  }
  
  :deep(.el-upload:hover) {
    border-color: #409eff;
  }
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
}

.avatar {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  padding-top: 8px;
}

/* 分销样式 */
.commission-text {
  display: inline-block;
  font-size: 12px;
  color: #67c23a;
  margin-left: 6px;
  white-space: nowrap;
}

.distribution-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 96px;
}

.daily-heat-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
}

.sort-controls {
  display: flex;
  gap: 8px;
  width: 250px;

  .el-select {
    flex: 1;
    min-width: 0;
  }
}

.favorite-count-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #606266;
  font-size: 12px;
  line-height: 1.35;
}

.favorite-count-cell strong {
  color: #303133;
  font-size: 15px;
}

.size-year {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.artist-id-inline {
  display: block;
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
  line-height: 1.35;
  word-break: break-all;
}

.holder-info {
  margin: 4px 0 0;
  color: #606266;
  font-size: 12px;
  line-height: 1.35;
}

.dist-stats {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  
  .money {
    color: #f56c6c;
    font-weight: 500;
  }
}

.dist-info {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  
  .artwork-preview {
    display: flex;
    gap: 12px;
    align-items: center;
    
    .artwork-detail {
      .name {
        font-weight: 500;
        margin-bottom: 4px;
      }
      .price {
        color: #f56c6c;
        font-size: 16px;
      }
    }
  }
}

.dist-form {
  .unit {
    margin-left: 8px;
    color: #909399;
  }
  
  .tip {
    margin-left: 12px;
    color: #909399;
    font-size: 12px;
  }
  
  .calc-preview {
    background: #fdf6ec;
    padding: 12px 16px;
    border-radius: 4px;
    font-size: 13px;
    color: #909399;
    
    .result {
      color: #67c23a;
      font-weight: 500;
      margin-top: 8px;
    }
  }
  
  .link-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

.stat-card {
  text-align: center;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: #fff;
  
  .label {
    font-size: 12px;
    opacity: 0.9;
    margin-bottom: 8px;
  }
  
  .value {
    font-size: 24px;
    font-weight: bold;
  }
  
  .unit {
    font-size: 12px;
    opacity: 0.8;
    margin-top: 4px;
  }
}

.price-growth-config {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  
  .unit {
    margin-left: 8px;
    color: #909399;
  }
}

.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;

  .el-input-number {
    flex: 1;
    min-width: 0;
  }
}

.range-separator {
  color: #909399;
  white-space: nowrap;
}

.artist-id-tag {
  display: inline-block;
  padding: 4px 10px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  font-family: 'Courier New', monospace;
}

.artist-create-tip {
  margin-top: 8px;
}

.heat-config-summary,
.price-growth-preview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  align-items: stretch;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #f7f9fc;
  border: 1px solid #ebeef5;
}

.price-growth-preview {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 10px;
}

.heat-summary-item,
.price-growth-preview > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.heat-summary-item span,
.price-growth-preview span {
  color: #909399;
  font-size: 12px;
}

.heat-summary-item b,
.price-growth-preview b {
  color: #303133;
  font-size: 16px;
}

.heat-summary-item small {
  color: #909399;
  font-size: 12px;
}

.config-button {
  margin-left: 12px;
}

.price-config-alert {
  margin-bottom: 16px;
}

.price-config-preview {
  display: flex;
  gap: 24px;
  margin-top: 8px;
  padding: 12px 16px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
}

.random-tip {
  width: 100%;
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.price-log-panel {
  min-height: 72px;
}

.price-log-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.price-log-item:last-child {
  border-bottom: none;
}

.price-log-item b {
  color: #303133;
}

.price-log-item p {
  margin: 4px 0 0;
  color: #909399;
  font-size: 12px;
}

.price-log-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  white-space: nowrap;
}

.price-log-meta span {
  color: #303133;
  font-weight: 600;
}

.price-log-meta em {
  color: #f56c6c;
  font-style: normal;
  font-weight: 600;
}

.price-log-meta small {
  color: #909399;
}

/* 艺术家编辑弹窗样式 */
.artist-profile-header {
  display: flex;
  gap: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;

  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;

    .avatar-uploader {
      :deep(.el-upload) {
        cursor: pointer;
      }
    }
  }

  .artist-info-summary {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 8px;

    .artist-id {
      margin: 0;
      font-size: 13px;
      color: #909399;
      font-family: 'Consolas', 'Monaco', monospace;
    }

    .artist-name-preview {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
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

/* 艺术家搜索容器 */
.artist-search-container {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

/* 艺术家搜索下拉项 */
.artist-search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
  
  .artist-name {
    flex: 1;
    font-weight: 500;
  }
  
  .artist-code {
    color: #909399;
    font-size: 12px;
    font-family: 'Consolas', 'Monaco', monospace;
  }
}

/* 权重输入框样式 */
.weight-input {
  width: 96px;
  
  :deep(.el-input__wrapper) {
    width: 96px;
    height: 22px;
    padding-left: 18px;
    padding-right: 18px;
    box-sizing: border-box;
    
    .el-input__inner {
      width: 60px;
      height: 22px;
      padding-left: 2px;
      padding-right: 2px;
      font-size: 12px;
      line-height: 12px;
      color: #606266;
      text-align: center;
    }
  }
  
  :deep(.el-input-number__decrease),
  :deep(.el-input-number__increase) {
    width: 24px;
    height: 22px;
    line-height: 10px;
    
    [class*=el-icon] {
      font-size: 10px;
    }
  }
}

/* 表格表头样式 */
:deep(.el-table__header-wrapper) {
  .el-table__header {
    width: 1541px;
  }

  .el-table__header tr th {
    padding-top: 0;
    padding-bottom: 0;
  }
}
</style>
