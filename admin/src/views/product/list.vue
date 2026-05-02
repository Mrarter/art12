<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">作品列表</span>
      <div class="header-actions">
        <el-button type="warning" @click="showPriceConfig">
          <el-icon><Setting /></el-icon>价格增长配置
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
        <el-form-item>
          <el-button type="primary" native-type="submit">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" v-loading="loading" border stripe>
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
                :src="row.cover" 
                :preview-src-list="row.cover ? [row.cover] : []" 
                style="width: 80px; height: 80px" 
                fit="cover"
                :preview-teleported="true"
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
              <p class="category">
                <el-tag v-if="row.categoryName" size="small" type="info">{{ row.categoryName }}</el-tag>
                <el-tag v-if="row.artType" size="small" type="warning">{{ row.artType }}</el-tag>
              </p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="150">
        <template #default="{ row }">
          <p>¥{{ row.price }}</p>
          <p class="size-year" v-if="formatSizeYear(row)">{{ formatSizeYear(row) }}</p>
          <p class="original" v-if="row.originalPrice">原价: ¥{{ row.originalPrice }}</p>
          <p class="price-rise" v-if="row.priceRise > 0" style="color: #ff4d4f; font-size: 12px;">
            涨幅 +{{ (row.priceRise * 100).toFixed(1) }}%
          </p>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.ownershipType === 1 ? 'success' : 'warning'" size="small">
            {{ row.ownershipType === 1 ? '原创' : '收藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="favoriteCount" label="收藏数" width="80" />
      <el-table-column label="每日热度" width="120">
        <template #default="{ row }">
          <div class="daily-heat-cell">
            <span>浏览 {{ row.dailyViewCount || 0 }}</span>
            <span>点赞 {{ row.dailyLikeCount || 0 }}</span>
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
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
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
        
        <el-form-item label="佣金比例" prop="commissionRate">
          <el-input-number 
            v-model="distForm.commissionRate" 
            :min="0" 
            :max="100" 
            :precision="0"
            :disabled="!distForm.distributionEnabled"
          />
          <span class="unit">%</span>
          <span class="tip">（订单金额的百分比作为佣金）</span>
        </el-form-item>
        
        <el-form-item label="佣金计算">
          <div class="calc-preview">
            <p>商品售价：¥{{ distForm.price || 0 }}</p>
            <p>佣金比例：{{ distForm.commissionRate || 0 }}%</p>
            <p class="result">预估佣金：¥{{ ((distForm.price || 0) * (distForm.commissionRate || 0) / 100).toFixed(2) }}</p>
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
              <p class="label">分销佣金</p>
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
          <p class="link-tip">推广员可通过此链接分享作品获得佣金</p>
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
                  <el-avatar v-if="item.avatar" :src="item.avatar" size="small" />
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
              <img v-if="editForm.cover" :src="editForm.cover" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">支持 JPG/PNG，大小不超过 5MB</div>
          </div>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="editForm.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="editForm.originalPrice" :min="0" :precision="2" :controls="false" style="width: 100%" />
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
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="每日浏览量">
            <div class="range-inputs">
              <el-input-number v-model="editForm.dailyViewMin" :min="0" :max="999999" :precision="0" controls-position="right" />
              <span class="range-separator">至</span>
              <el-input-number v-model="editForm.dailyViewMax" :min="0" :max="999999" :precision="0" controls-position="right" />
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="每日点赞量">
            <div class="range-inputs">
              <el-input-number v-model="editForm.dailyLikeMin" :min="0" :max="999999" :precision="0" controls-position="right" />
              <span class="range-separator">至</span>
              <el-input-number v-model="editForm.dailyLikeMax" :min="0" :max="999999" :precision="0" controls-position="right" />
            </div>
          </el-form-item>
        </el-col>
      </el-row>
        
        <!-- 单个作品价格增长配置 -->
        <el-divider content-position="left">价格增长配置</el-divider>
        <div class="price-growth-config">
          <el-form-item label="自定义配置">
            <el-switch v-model="editForm.customPriceGrowthEnabled" active-text="启用" inactive-text="使用全局配置" />
          </el-form-item>
          
          <template v-if="editForm.customPriceGrowthEnabled">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="基础日增长率">
                  <div class="range-inputs">
                    <el-input-number v-model="editForm.customBaseDailyRateMin" :min="0" :precision="4" :step="0.0001" />
                    <span class="range-separator">至</span>
                    <el-input-number v-model="editForm.customBaseDailyRateMax" :min="0" :precision="4" :step="0.0001" />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="成熟期天数">
                  <div class="range-inputs">
                    <el-input-number v-model="editForm.customMatureDaysMin" :min="0" :max="365" :precision="0" />
                    <span class="range-separator">至</span>
                    <el-input-number v-model="editForm.customMatureDaysMax" :min="0" :max="365" :precision="0" />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="成熟期日增长率">
                  <div class="range-inputs">
                    <el-input-number v-model="editForm.customMatureDailyRateMin" :min="0" :precision="4" :step="0.0001" />
                    <span class="range-separator">至</span>
                    <el-input-number v-model="editForm.customMatureDailyRateMax" :min="0" :precision="4" :step="0.0001" />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="浏览量加成">
                  <div class="range-inputs">
                    <el-input-number v-model="editForm.customViewRateMin" :min="1" :max="10" :precision="2" />
                    <span class="range-separator">至</span>
                    <el-input-number v-model="editForm.customViewRateMax" :min="1" :max="10" :precision="2" />
                  </div>
                  <span class="unit">倍</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="收藏量加成">
                  <div class="range-inputs">
                    <el-input-number v-model="editForm.customFavoriteRateMin" :min="1" :max="10" :precision="2" />
                    <span class="range-separator">至</span>
                    <el-input-number v-model="editForm.customFavoriteRateMax" :min="1" :max="10" :precision="2" />
                  </div>
                  <span class="unit">倍</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大涨幅倍数">
                  <div class="range-inputs">
                    <el-input-number v-model="editForm.customMaxGrowthMultipleMin" :min="1" :max="100" :precision="1" />
                    <span class="range-separator">至</span>
                    <el-input-number v-model="editForm.customMaxGrowthMultipleMax" :min="1" :max="100" :precision="1" />
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </template>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, Edit, Setting, Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { requestApi } from '@/api/request'

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
  status: ''
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
  originalPrice: 0,
  ownershipType: 1,  // 默认原创
  description: '',
  status: 1,
  dailyViewCount: 0,
  dailyLikeCount: 0,
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

// 艺术家搜索相关
const artistSearchResults = ref([])
const artistSearchLoading = ref(false)
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

const getArtistUid = (artist = {}) => artist.uid || artist.userUid || artist.artistUid || artist.artistCode || artist.code || ''

const normalizeArtistOption = (artist = {}) => ({
  ...artist,
  name: artist.name || artist.nickname || artist.realName || artist.artistName || '',
  uid: getArtistUid(artist)
})

// el-autocomplete 期望的搜索函数格式
const searchArtists = (keyword, callback) => {
  if (!keyword || keyword.length < 1) {
    artistExactMatched.value = false
    callback([])
    return
  }
  artistSearchLoading.value = true
  requestApi.get('/user/artist/search', { 
    params: { keyword, limit: 10 } 
  }).then(res => {
    artistSearchResults.value = (res || []).map(normalizeArtistOption)
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

const rules = {
  title: [{ required: true, message: '请输入作品名称', trigger: 'blur' }],
  artistName: [{ required: true, message: '请输入艺术家名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const getArtworkUid = (row) => row.artworkUid || row.artworkCode || row.displayArtworkId || row.artworkId || '-'

const getAuthorUid = (row) => row.authorUid || row.displayAuthorId || '-'

const formatSizeYear = (row) => {
  const info = []
  if (row.size) info.push(row.size)
  if (row.year) info.push(`${row.year}年`)
  return info.join(' / ')
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
    const data = await request.get('/product/list', { params })
    // 映射后端数据格式到前端
    tableData.value = (data.records || data.list || []).map(item => ({
      artworkId: item.id,
      artworkUid: item.artworkUid || item.artworkCode,
      authorId: item.authorId,
      displayArtworkId: item.displayArtworkId,
      displayAuthorId: item.displayAuthorId,
      authorUid: item.authorUid,
      title: item.title,
      artistName: item.authorName || item.artistName,  // 后端返回 authorName
      cover: item.coverImage || item.cover || '', // 后端返回 coverImage
      categoryId: item.categoryId,
      categoryName: item.category,  // 后端返回 category
      artType: item.artType,        // 画种
      size: item.size,               // 尺寸
      year: item.year,              // 创作年份
      price: item.price ? item.price / 100 : 0,  // 分转元
      originalPrice: item.originalPrice ? item.originalPrice / 100 : 0,
      ownershipType: item.ownershipType || 1,
      artworkCode: item.artworkCode || item.artworkUid,
      status: item.status,
      weight: item.weight || 0,
      description: item.description,
      salesCount: item.salesCount || 0,
      favoriteCount: item.favoriteCount || 0,
      dailyViewCount: item.dailyViewCount || 0,
      dailyLikeCount: item.dailyLikeCount || 0,
      displayViewCount: item.displayViewCount || item.viewCount || 0,
      displayLikeCount: item.displayLikeCount || item.favoriteCount || 0,
      priceRise: item.priceRise || 0, // 价格增长率
      createTime: item.createTime,
      distributionEnabled: item.distributionEnabled || false,
      commissionRate: item.commissionRate || 10,
      distributionOrders: item.distributionOrders || 0,
      distributionEarnings: item.distributionEarnings || 0,
      distributionUsers: item.distributionUsers || 0
    }))
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载数据失败:', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
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
  Object.assign(searchForm, { artworkId: '', title: '', artistName: '', categoryId: '', status: '' })
  handleSearch()
}

const openArtistEditor = (row) => {
  if (!row.authorId) {
    ElMessage.warning('该作品未关联艺术家')
    return
  }
  router.push({
    path: '/user/artist',
    query: {
      userId: row.authorId,
      artistName: row.artistName || '',
      authorUid: getAuthorUid(row),
      open: 'profile'
    }
  })
}

const handleEdit = async (row) => {
  console.log('【DEBUG】handleEdit row.price:', row.price, 'row.originalPrice:', row.originalPrice)
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
    price: row.price,
    originalPrice: row.originalPrice || 0,
    ownershipType: row.ownershipType || 1,
    description: row.description || '',
    status: row.status
  })
  setDailyHeatRange(row.dailyViewCount || 0, row.dailyLikeCount || 0)
  artistExactMatched.value = Boolean(editForm.authorId)
  console.log('【DEBUG】handleEdit editForm.price:', editForm.price, 'editForm.originalPrice:', editForm.originalPrice)
  
  // 加载单个作品的价格增长配置
  try {
    const data = await requestApi.get(`/product/${row.artworkId}/priceGrowth`)
    Object.assign(editForm, {
      customPriceGrowthEnabled: data.customPriceGrowthEnabled || false
    })
    setPriceGrowthRanges(data)
  } catch (e) {
    console.error('加载价格增长配置失败', e)
    // 使用默认值
    Object.assign(editForm, {
      customPriceGrowthEnabled: false
    })
    setPriceGrowthRanges()
  }
  
  editVisible.value = true
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
    originalPrice: 0,
    ownershipType: 1,
    description: '',
    status: 1
  })
  setDailyHeatRange(0, 0)
  setPriceGrowthRanges()
  artistExactMatched.value = false
  editVisible.value = true
}

const beforeImageUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleImageUpload = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res = await requestApi.post('/product/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    editForm.cover = res.url
    ElMessage.success('图片上传成功')
  } catch (e) {
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
    const priceGrowthValues = {
      customBaseDailyRate: randomNumberInRange(editForm.customBaseDailyRateMin, editForm.customBaseDailyRateMax, 4),
      customMatureDailyRate: randomNumberInRange(editForm.customMatureDailyRateMin, editForm.customMatureDailyRateMax, 4),
      customMatureDays: randomIntInRange(editForm.customMatureDaysMin, editForm.customMatureDaysMax),
      customViewRate: randomNumberInRange(editForm.customViewRateMin, editForm.customViewRateMax, 2, 1),
      customFavoriteRate: randomNumberInRange(editForm.customFavoriteRateMin, editForm.customFavoriteRateMax, 2, 1),
      customMaxGrowthMultiple: randomNumberInRange(editForm.customMaxGrowthMultipleMin, editForm.customMaxGrowthMultipleMax, 1, 1)
    }
    Object.assign(editForm, priceGrowthValues)
    
    const params = {
      title: editForm.title,
      authorId: editForm.authorId,
      authorName: editForm.artistName,
      artType: editForm.artType || null,
      size: editForm.size || null,
      year: editForm.year || null,
      cover: editForm.cover || null,
      price: editForm.price != null ? Number(editForm.price) : null,
      originalPrice: editForm.originalPrice != null ? Number(editForm.originalPrice) : null,
      ownershipType: editForm.ownershipType || 1,
      description: editForm.description,
      status: editForm.status,
      dailyViewCount,
      dailyLikeCount
    }
    console.log('【DEBUG】保存参数 params.price:', params.price, 'params.originalPrice:', params.originalPrice)
    console.log('保存参数:', params)
    
    if (editForm.artworkId) {
      // 更新作品
      params.id = Number(editForm.artworkId)
      await requestApi.put('/product/update', params)
      
      // 保存单个作品的价格增长配置
      const priceGrowthParams = {
        customPriceGrowthEnabled: editForm.customPriceGrowthEnabled,
        ...priceGrowthValues
      }
      await requestApi.put(`/product/${editForm.artworkId}/priceGrowth`, priceGrowthParams)
      
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
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  console.log('切换状态:', { artworkId: row.artworkId, currentStatus: row.status, newStatus })
  try {
    await ElMessageBox.confirm(`确定要${action}该作品吗？`, '提示', { type: 'warning' })
    console.log('确认后调用API...')
    // 调用后端 API 更新状态
    const res = await requestApi.put('/product/update', {
      id: Number(row.artworkId),
      status: newStatus
    })
    console.log('API响应:', res)
    // 成功后刷新列表获取最新数据
    loadData()
    ElMessage.success(`${action}成功`)
  } catch (e) {
    console.error('操作失败:', e)
    const msg = e.response?.data?.message || e.message || '操作失败'
ElMessage.error(msg)
  }
}

// 权重变更
const handleWeightChange = async (row) => {
  try {
    await requestApi.put('/product/update', {
      id: Number(row.artworkId),
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
    await requestApi.delete(`/product/${row.artworkId}`)
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
  distLink.value = `https://shiyiju.com/artwork/${row.artworkId}?from=dist`
  distVisible.value = true
}

const handleSaveDistribution = async () => {
  distSaveLoading.value = true
  try {
    console.log('分销保存参数:', distForm.artworkId, distForm.distributionEnabled, distForm.commissionRate)
    // 调用后端 API 保存分销设置
    await requestApi.put('/product/update', {
      id: Number(distForm.artworkId),
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
    tr {
      width: 1541px;
      
      th {
        padding-top: 0;
        padding-bottom: 0;
      }
    }
  }
}
</style>
