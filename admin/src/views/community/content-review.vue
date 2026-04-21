<template>
  <view class="content-review-container">
    <!-- 页面标题 -->
    <view class="page-header">
      <h2>艺术圈内容审核</h2>
      <view class="header-actions">
        <el-radio-group v-model="tabType" size="small">
          <el-radio-button label="post">帖子审核</el-radio-button>
          <el-radio-button label="comment">评论审核</el-radio-button>
          <el-radio-button label="report">举报处理</el-radio-button>
        </el-radio-group>
      </view>
    </view>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stat-card pending">
          <div class="stat-value">{{ stats.pendingCount }}</div>
          <div class="stat-label">待审核</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card passed">
          <div class="stat-value">{{ stats.passedCount }}</div>
          <div class="stat-label">已通过</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card rejected">
          <div class="stat-value">{{ stats.rejectedCount }}</div>
          <div class="stat-label">已拒绝</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card report">
          <div class="stat-value">{{ stats.reportCount }}</div>
          <div class="stat-label">待处理举报</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" size="small">
        <el-form-item label="内容类型">
          <el-select v-model="filterForm.contentType" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="图文帖子" value="image_text" />
            <el-option label="纯文字" value="text" />
            <el-option label="视频" value="video" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="passed" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="敏感词">
          <el-select v-model="filterForm.sensitiveType" placeholder="全部" clearable>
            <el-option label="全部" value="" />
            <el-option label="包含敏感词" value="has_sensitive" />
            <el-option label="无敏感词" value="no_sensitive" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="搜索内容/用户" clearable @keyup.enter.native="searchData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 帖子审核列表 -->
    <el-card v-if="tabType === 'post'" class="content-list">
      <el-table :data="postList" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="发布者" width="150">
          <template slot-scope="scope">
            <div class="user-cell">
              <el-avatar :size="32" :src="scope.row.userAvatar">
                <i class="el-icon-user-solid"></i>
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ scope.row.nickname }}</div>
                <div class="user-id">ID: {{ scope.row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="300">
          <template slot-scope="scope">
            <div class="content-cell">
              <div class="content-text">{{ scope.row.content }}</div>
              <div class="content-media" v-if="scope.row.images?.length">
                <el-image
                  v-for="(img, idx) in scope.row.images.slice(0, 3)"
                  :key="idx"
                  :src="img"
                  :preview-src-list="scope.row.images"
                  class="content-image"
                  fit="cover"
                />
                <span v-if="scope.row.images.length > 3" class="image-more">+{{ scope.row.images.length - 3 }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="话题" width="120">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.topic" type="info" size="small">#{{ scope.row.topic }}</el-tag>
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="敏感词" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.hasSensitive" type="danger" size="small">
              {{ scope.row.sensitiveWords?.length || 0 }}个
            </el-tag>
            <span v-else class="text-success">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="commentCount" label="评论" width="80" />
        <el-table-column prop="createTime" label="发布时间" width="160" />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="viewDetail(scope.row)">查看</el-button>
            <el-button v-if="scope.row.status === 'pending'" type="text" size="small" @click="approvePost(scope.row)">通过</el-button>
            <el-button v-if="scope.row.status === 'pending'" type="text" size="small" class="text-danger" @click="rejectPost(scope.row)">拒绝</el-button>
            <el-button type="text" size="small" class="text-danger" @click="deletePost(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        :current-page="pagination.page"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; text-align: right;"
      />
    </el-card>

    <!-- 评论审核列表 -->
    <el-card v-if="tabType === 'comment'" class="content-list">
      <el-table :data="commentList" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="评论者" width="150">
          <template slot-scope="scope">
            <div class="user-cell">
              <el-avatar :size="32" :src="scope.row.userAvatar">
                <i class="el-icon-user-solid"></i>
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ scope.row.nickname }}</div>
                <div class="user-id">ID: {{ scope.row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评论内容" min-width="200">
          <template slot-scope="scope">
            <div class="comment-cell">
              <div class="comment-text">{{ scope.row.content }}</div>
              <div class="comment-target" v-if="scope.row.replyTo">
                <span class="reply-icon">@</span>
                回复 <span class="reply-user">{{ scope.row.replyTo }}</span>: {{ scope.row.replyContent }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属帖子" min-width="150">
          <template slot-scope="scope">
            <div class="post-preview">{{ scope.row.postContent?.slice(0, 30) }}...</div>
          </template>
        </el-table-column>
        <el-table-column label="敏感词" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.hasSensitive" type="danger" size="small">
              {{ scope.row.sensitiveWords?.length || 0 }}个
            </el-tag>
            <span v-else class="text-success">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="viewCommentDetail(scope.row)">查看</el-button>
            <el-button type="text" size="small" class="text-danger" @click="deleteComment(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        :current-page="pagination.page"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; text-align: right;"
      />
    </el-card>

    <!-- 举报处理列表 -->
    <el-card v-if="tabType === 'report'" class="content-list">
      <el-table :data="reportList" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="举报人" width="150">
          <template slot-scope="scope">
            <div class="user-cell">
              <el-avatar :size="32" :src="scope.row.reporterAvatar">
                <i class="el-icon-user-solid"></i>
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ scope.row.reporterNickname }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="被举报内容" min-width="200">
          <template slot-scope="scope">
            <div class="report-target">
              <div class="target-type">
                <el-tag size="small" :type="scope.row.targetType === 'post' ? 'success' : 'warning'">
                  {{ scope.row.targetType === 'post' ? '帖子' : '评论' }}
                </el-tag>
              </div>
              <div class="target-content">{{ scope.row.targetContent?.slice(0, 50) }}...</div>
              <div class="target-user">
                发布者: {{ scope.row.targetUserNickname }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="举报原因" width="150">
          <template slot-scope="scope">
            <el-tag :type="getReportReasonType(scope.row.reason)" size="small">
              {{ getReportReasonText(scope.row.reason) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报描述" min-width="150">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.description" placement="top">
              <span class="text-ellipsis">{{ scope.row.description }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="证据" width="100">
          <template slot-scope="scope">
            <el-image
              v-if="scope.row.images?.length"
              :src="scope.row.images[0]"
              :preview-src-list="scope.row.images"
              class="evidence-image"
              fit="cover"
            />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="举报时间" width="160" />
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getReportStatusType(scope.row.status)" size="small">
              {{ getReportStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="viewReportDetail(scope.row)">查看</el-button>
            <el-button v-if="scope.row.status === 'pending'" type="text" size="small" @click="handleReport(scope.row, 'valid')">有效</el-button>
            <el-button v-if="scope.row.status === 'pending'" type="text" size="small" @click="handleReport(scope.row, 'invalid')">无效</el-button>
            <el-button type="text" size="small" class="text-danger" @click="ignoreReport(scope.row)">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        :current-page="pagination.page"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; text-align: right;"
      />
    </el-card>

    <!-- 内容详情弹窗 -->
    <el-dialog title="内容详情" :visible.sync="showDetail" width="700px">
      <div v-if="currentContent" class="detail-content">
        <div class="detail-user">
          <el-avatar :size="48" :src="currentContent.userAvatar">
            <i class="el-icon-user-solid"></i>
          </el-avatar>
          <div class="user-detail">
            <div class="nickname">{{ currentContent.nickname }}</div>
            <div class="meta">ID: {{ currentContent.userId }} | {{ currentContent.createTime }}</div>
          </div>
          <el-tag :type="getStatusType(currentContent.status)" size="small" style="margin-left: auto;">
            {{ getStatusText(currentContent.status) }}
          </el-tag>
        </div>
        <div class="detail-text">{{ currentContent.content }}</div>
        <div class="detail-media" v-if="currentContent.images?.length">
          <el-image
            v-for="(img, idx) in currentContent.images"
            :key="idx"
            :src="img"
            :preview-src-list="currentContent.images"
            class="detail-image"
            fit="cover"
          />
        </div>
        <div class="detail-stats">
          <span><i class="el-icon-like"></i> {{ currentContent.likeCount || 0 }} 点赞</span>
          <span><i class="el-icon-chat-line-square"></i> {{ currentContent.commentCount || 0 }} 评论</span>
          <span><i class="el-icon-share"></i> {{ currentContent.shareCount || 0 }} 分享</span>
        </div>
        <div class="detail-sensitive" v-if="currentContent.hasSensitive">
          <el-alert
            title="检测到敏感词"
            type="warning"
            :closable="false"
          >
            <template>
              <div>敏感词: <el-tag v-for="word in currentContent.sensitiveWords" :key="word" size="small" style="margin-right: 8px;">{{ word }}</el-tag></div>
            </template>
          </el-alert>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button type="primary" @click="approvePost(currentContent)">通过</el-button>
        <el-button type="danger" @click="rejectPost(currentContent)">拒绝</el-button>
      </div>
    </el-dialog>

    <!-- 拒绝原因弹窗 -->
    <el-dialog title="拒绝原因" :visible.sync="showRejectDialog" width="500px">
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因">
          <el-select v-model="rejectForm.reason" placeholder="请选择原因">
            <el-option label="包含敏感信息" value="sensitive" />
            <el-option label="广告营销内容" value="advertisement" />
            <el-option label="违规违法内容" value="illegal" />
            <el-option label="抄袭搬运内容" value="plagiarism" />
            <el-option label="恶意刷屏行为" value="spam" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input v-model="rejectForm.description" type="textarea" :rows="3" placeholder="请输入详细说明" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认拒绝</el-button>
      </div>
    </el-dialog>

    <!-- 敏感词配置 -->
    <el-dialog title="敏感词配置" :visible.sync="showSensitiveDialog" width="600px">
      <div class="sensitive-config">
        <div class="config-header">
          <el-button type="primary" size="small" @click="addSensitiveWord">添加敏感词</el-button>
          <el-upload
            class="upload-btn"
            action="#"
            :show-file-list="false"
            :before-upload="beforeUpload"
            accept=".txt,.csv"
          >
            <el-button size="small" type="success">批量导入</el-button>
          </el-upload>
        </div>
        <el-table :data="sensitiveWords" stripe border size="small" max-height="300">
          <el-table-column prop="word" label="敏感词" />
          <el-table-column label="级别" width="100">
            <template slot-scope="scope">
              <el-tag :type="scope.row.level === 1 ? 'danger' : 'warning'" size="small">
                {{ scope.row.level === 1 ? '严重' : '一般' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="匹配方式" width="100">
            <template slot-scope="scope">
              {{ scope.row.matchType === 'exact' ? '精确' : '模糊' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="editSensitiveWord(scope.row)">编辑</el-button>
              <el-button type="text" size="small" class="text-danger" @click="deleteSensitiveWord(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="showSensitiveDialog = false">关闭</el-button>
        <el-button type="primary" @click="saveSensitiveWords">保存</el-button>
      </div>
    </el-dialog>
  </view>
</template>

<script>
export default {
  name: 'ContentReview',
  data() {
    return {
      tabType: 'post',
      stats: {
        pendingCount: 128,
        passedCount: 3456,
        rejectedCount: 234,
        reportCount: 56
      },
      filterForm: {
        contentType: '',
        status: 'pending',
        sensitiveType: '',
        keyword: ''
      },
      postList: [],
      commentList: [],
      reportList: [],
      pagination: {
        page: 1,
        pageSize: 10,
        total: 0
      },
      showDetail: false,
      showRejectDialog: false,
      showSensitiveDialog: false,
      currentContent: null,
      rejectForm: {
        reason: '',
        description: ''
      },
      sensitiveWords: [
        { word: '政治敏感', level: 1, matchType: 'fuzzy' },
        { word: '色情内容', level: 1, matchType: 'fuzzy' },
        { word: '暴力血腥', level: 1, matchType: 'fuzzy' },
        { word: '赌博', level: 2, matchType: 'exact' },
        { word: '毒品', level: 1, matchType: 'exact' },
        { word: '诈骗', level: 2, matchType: 'fuzzy' },
        { word: '广告', level: 2, matchType: 'exact' },
        { word: '微信', level: 2, matchType: 'exact' },
        { word: 'QQ号', level: 2, matchType: 'exact' },
        { word: '联系方式', level: 2, matchType: 'fuzzy' }
      ]
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.loadStats()
      this.loadList()
    },
    
    loadStats() {
      // 模拟统计数据
      this.stats = {
        pendingCount: 128,
        passedCount: 3456,
        rejectedCount: 234,
        reportCount: 56
      }
    },
    
    loadList() {
      // 模拟帖子列表
      this.postList = [
        {
          id: 1001,
          userId: 2001,
          nickname: '艺术爱好者',
          userAvatar: '',
          content: '这幅画太美了！艺术家用细腻的笔触描绘出大自然的宁静与美好。#艺术 #油画',
          images: ['', '', ''],
          topic: '艺术',
          hasSensitive: false,
          sensitiveWords: [],
          likeCount: 256,
          commentCount: 45,
          shareCount: 12,
          status: 'pending',
          createTime: '2026-04-21 10:30:00'
        },
        {
          id: 1002,
          userId: 2002,
          nickname: '收藏家张三',
          userAvatar: '',
          content: '今天在画廊看到这幅作品，非常震撼！艺术家对光影的处理独具匠心。',
          images: [''],
          topic: null,
          hasSensitive: true,
          sensitiveWords: ['画廊', '艺术品'],
          likeCount: 128,
          commentCount: 23,
          shareCount: 5,
          status: 'pending',
          createTime: '2026-04-21 09:15:00'
        },
        {
          id: 1003,
          userId: 2003,
          nickname: '艺术评论员',
          userAvatar: '',
          content: '当代艺术的边界正在被不断拓展，艺术家们通过作品表达对社会的思考。',
          images: [],
          topic: '当代艺术',
          hasSensitive: false,
          sensitiveWords: [],
          likeCount: 89,
          commentCount: 12,
          shareCount: 3,
          status: 'passed',
          createTime: '2026-04-20 18:20:00'
        }
      ]
      
      // 模拟评论列表
      this.commentList = [
        {
          id: 3001,
          userId: 2010,
          nickname: '艺术新手',
          userAvatar: '',
          content: '这幅画用什么颜料画的？',
          replyTo: '艺术爱好者',
          replyContent: '应该是油画颜料',
          postContent: '这幅画太美了！艺术家用细腻的笔触...',
          hasSensitive: false,
          sensitiveWords: [],
          likeCount: 5,
          status: 'pending',
          createTime: '2026-04-21 11:00:00'
        },
        {
          id: 3002,
          userId: 2011,
          nickname: '资深藏家',
          userAvatar: '',
          content: '这幅作品的市场价值很高',
          postContent: '今天在画廊看到这幅作品...',
          hasSensitive: false,
          sensitiveWords: [],
          likeCount: 12,
          status: 'pending',
          createTime: '2026-04-21 10:45:00'
        }
      ]
      
      // 模拟举报列表
      this.reportList = [
        {
          id: 4001,
          reporterId: 2100,
          reporterNickname: '热心用户',
          reporterAvatar: '',
          targetType: 'post',
          targetId: 1005,
          targetUserId: 2005,
          targetUserNickname: '可疑用户',
          targetContent: '加我微信 xxx666888，有更多高清作品图',
          reason: 'advertisement',
          description: '该用户在帖子中发布微信号码进行广告引流',
          images: [],
          status: 'pending',
          createTime: '2026-04-21 10:20:00'
        },
        {
          id: 4002,
          reporterId: 2101,
          reporterNickname: '艺术爱好者',
          reporterAvatar: '',
          targetType: 'comment',
          targetId: 3010,
          targetUserId: 2006,
          targetUserNickname: '匿名用户',
          targetContent: '抄袭作品，毫无创意',
          reason: 'plagiarism',
          description: '这条评论涉嫌诽谤抄袭',
          images: [],
          status: 'pending',
          createTime: '2026-04-21 09:30:00'
        }
      ]
      
      this.pagination.total = 50
    },
    
    searchData() {
      this.pagination.page = 1
      this.loadList()
    },
    
    resetFilter() {
      this.filterForm = {
        contentType: '',
        status: 'pending',
        sensitiveType: '',
        keyword: ''
      }
      this.searchData()
    },
    
    handleSizeChange(val) {
      this.pagination.pageSize = val
      this.loadList()
    },
    
    handlePageChange(val) {
      this.pagination.page = val
      this.loadList()
    },
    
    viewDetail(row) {
      this.currentContent = { ...row }
      this.showDetail = true
    },
    
    viewCommentDetail(row) {
      this.currentContent = { ...row }
      this.showDetail = true
    },
    
    viewReportDetail(row) {
      this.currentContent = { ...row }
      this.showDetail = true
    },
    
    approvePost(row) {
      this.$confirm('确认通过该内容审核?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }).then(() => {
        row.status = 'passed'
        this.$message.success('审核通过')
        this.showDetail = false
      }).catch(() => {})
    },
    
    rejectPost(row) {
      this.currentContent = row
      this.rejectForm = { reason: '', description: '' }
      this.showRejectDialog = true
    },
    
    confirmReject() {
      if (!this.rejectForm.reason) {
        this.$message.warning('请选择拒绝原因')
        return
      }
      this.currentContent.status = 'rejected'
      this.$message.success('已拒绝')
      this.showRejectDialog = false
      this.showDetail = false
    },
    
    deletePost(row) {
      this.$confirm('确认删除该帖子?', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.postList = this.postList.filter(item => item.id !== row.id)
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    
    deleteComment(row) {
      this.$confirm('确认删除该评论?', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.commentList = this.commentList.filter(item => item.id !== row.id)
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    
    handleReport(row, type) {
      const action = type === 'valid' ? '认定有效' : '认定无效'
      this.$confirm(`确认${action}该举报?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        if (type === 'valid') {
          // 认定有效，同时处理被举报内容
          this.$prompt('请输入处理说明', '处理举报', {
            confirmButtonText: '确定',
            cancelButtonText: '取消'
          }).then(({ value }) => {
            row.status = 'handled_valid'
            this.$message.success('已处理，被举报内容将被限制展示')
          }).catch(() => {})
        } else {
          row.status = 'handled_invalid'
          this.$message.success('已标记为无效举报')
        }
      }).catch(() => {})
    },
    
    ignoreReport(row) {
      this.$confirm('确认忽略该举报?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        row.status = 'ignored'
        this.$message.success('已忽略')
      }).catch(() => {})
    },
    
    getStatusType(status) {
      const types = {
        pending: 'warning',
        passed: 'success',
        rejected: 'danger'
      }
      return types[status] || ''
    },
    
    getStatusText(status) {
      const texts = {
        pending: '待审核',
        passed: '已通过',
        rejected: '已拒绝'
      }
      return texts[status] || status
    },
    
    getReportReasonType(reason) {
      const types = {
        advertisement: 'danger',
        plagiarism: 'warning',
        illegal: 'danger',
        spam: 'warning',
        harassment: 'warning',
        other: 'info'
      }
      return types[reason] || 'info'
    },
    
    getReportReasonText(reason) {
      const texts = {
        advertisement: '广告营销',
        plagiarism: '抄袭搬运',
        illegal: '违规违法',
        spam: '恶意刷屏',
        harassment: '骚扰他人',
        other: '其他'
      }
      return texts[reason] || reason
    },
    
    getReportStatusType(status) {
      const types = {
        pending: 'warning',
        handled_valid: 'danger',
        handled_invalid: 'info',
        ignored: 'info'
      }
      return types[status] || ''
    },
    
    getReportStatusText(status) {
      const texts = {
        pending: '待处理',
        handled_valid: '已处理',
        handled_invalid: '无效',
        ignored: '已忽略'
      }
      return texts[status] || status
    },
    
    addSensitiveWord() {
      this.$prompt('请输入敏感词', '添加敏感词').then(({ value }) => {
        if (value) {
          this.sensitiveWords.push({ word: value, level: 2, matchType: 'fuzzy' })
          this.$message.success('添加成功')
        }
      }).catch(() => {})
    },
    
    editSensitiveWord(row) {
      this.$prompt('请输入敏感词', '编辑敏感词', {
        inputValue: row.word
      }).then(({ value }) => {
        if (value) {
          row.word = value
          this.$message.success('修改成功')
        }
      }).catch(() => {})
    },
    
    deleteSensitiveWord(row) {
      this.$confirm('确认删除该敏感词?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.sensitiveWords = this.sensitiveWords.filter(item => item.word !== row.word)
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    
    beforeUpload(file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const words = e.target.result.split('\n').filter(w => w.trim())
        words.forEach(word => {
          if (!this.sensitiveWords.find(w => w.word === word)) {
            this.sensitiveWords.push({ word, level: 2, matchType: 'fuzzy' })
          }
        })
        this.$message.success(`成功导入 ${words.length} 个敏感词`)
      }
      reader.readAsText(file)
      return false
    },
    
    saveSensitiveWords() {
      this.$message.success('敏感词配置已保存')
      this.showSensitiveDialog = false
    }
  }
}
</script>

<style scoped>
.content-review-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-card .stat-value {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
}

.stat-card .stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-card.pending { border-left: 4px solid #E6A23C; }
.stat-card.passed { border-left: 4px solid #67C23A; }
.stat-card.rejected { border-left: 4px solid #F56C6C; }
.stat-card.report { border-left: 4px solid #909399; }

.filter-card {
  margin-bottom: 20px;
}

.content-list {
  min-height: 400px;
}

.user-cell {
  display: flex;
  align-items: center;
}

.user-info {
  margin-left: 10px;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.user-id {
  font-size: 12px;
  color: #909399;
}

.content-cell {
  max-width: 300px;
}

.content-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.content-media {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.content-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
}

.image-more {
  width: 60px;
  height: 60px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-size: 12px;
}

.text-muted {
  color: #909399;
}

.text-success {
  color: #67C23A;
}

.text-danger {
  color: #F56C6C;
}

.text-ellipsis {
  display: inline-block;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-cell {
  font-size: 14px;
}

.comment-text {
  color: #606266;
}

.comment-target {
  margin-top: 8px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
}

.reply-icon {
  color: #409EFF;
  font-weight: bold;
}

.reply-user {
  color: #409EFF;
}

.post-preview {
  font-size: 12px;
  color: #909399;
}

.report-target {
  font-size: 14px;
}

.target-type {
  margin-bottom: 4px;
}

.target-content {
  color: #606266;
  line-height: 1.4;
}

.target-user {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.evidence-image {
  width: 50px;
  height: 50px;
  border-radius: 4px;
}

.detail-content {
  padding: 10px 0;
}

.detail-user {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.detail-user .user-detail {
  margin-left: 12px;
  flex: 1;
}

.detail-user .nickname {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.detail-user .meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.detail-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
  margin-bottom: 16px;
}

.detail-media {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
}

.detail-stats {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.detail-stats span {
  color: #909399;
  font-size: 14px;
}

.detail-stats i {
  margin-right: 4px;
}

.sensitive-config {
  padding: 10px 0;
}

.config-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.upload-btn {
  display: inline-block;
}
</style>
