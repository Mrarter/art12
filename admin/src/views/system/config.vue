<template>
  <div class="page-container">
    <div class="page-header">
      <span class="title">系统配置</span>
      <el-button type="primary" @click="handleSave">保存配置</el-button>
    </div>
    
    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 交易设置 -->
      <el-tab-pane label="交易设置" name="trade">
        <el-form ref="tradeFormRef" :model="tradeForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">订单设置</el-divider>
          
          <el-form-item label="订单超时时间">
            <el-input-number v-model="tradeForm.orderTimeout" :min="5" :max="120" />
            <span class="tips">分钟（未支付自动取消）</span>
          </el-form-item>
          
          <el-form-item label="退款处理周期">
            <el-input-number v-model="tradeForm.refundDays" :min="1" :max="30" />
            <span class="tips">个工作日</span>
          </el-form-item>
          
          <el-form-item label="允许重复购买">
            <el-switch v-model="tradeForm.allowRepeatBuy" />
            <span class="tips">关闭后，同一用户只能购买同一作品一次</span>
          </el-form-item>
          
          <el-divider content-position="left">价格显示</el-divider>
          
          <el-form-item label="价格单位">
            <el-radio-group v-model="tradeForm.priceUnit">
              <el-radio label="fen">分（精确显示）</el-radio>
              <el-radio label="yuan">元（分转元）</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 分销设置 -->
      <el-tab-pane label="分销设置" name="promotion">
        <el-form ref="promotionFormRef" :model="promotionForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">经纪人分成比例</el-divider>
          
          <el-form-item label="一级分成（直接推广）">
            <el-input-number v-model="promotionForm.directCommission" :min="0" :max="50" :precision="2" />
            <span class="tips">% 的订单金额</span>
          </el-form-item>
          
          <el-form-item label="二级分成（团队奖励）">
            <el-input-number v-model="promotionForm.teamCommission" :min="0" :max="20" :precision="2" />
            <span class="tips">% 的订单金额（购买者本身也是经纪人时发放给其上级）</span>
          </el-form-item>
          
          <el-form-item label="经纪人分成结算时机">
            <el-select v-model="promotionForm.settlementType">
              <el-option label="支付成功后" value="after_pay" />
              <el-option label="确认收货后" value="after_confirm" />
              <el-option label="超过退款期后" value="after_refund" />
            </el-select>
          </el-form-item>
          
          <el-divider content-position="left">提现设置</el-divider>
          
          <el-form-item label="最低提现金额">
            <el-input-number v-model="promotionForm.minWithdraw" :min="1" :max="100000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>
          
          <el-form-item label="提现手续费">
            <el-input-number v-model="promotionForm.withdrawFee" :min="0" :max="10" :precision="1" />
            <span class="tips">%</span>
          </el-form-item>
          
          <el-form-item label="提现周期">
            <el-input-number v-model="promotionForm.withdrawDays" :min="1" :max="30" />
            <span class="tips">个工作日内到账</span>
          </el-form-item>
          
          <el-divider content-position="left">经纪人门槛</el-divider>
          
          <el-form-item label="成为经纪人条件">
            <el-radio-group v-model="promotionForm.promoterCondition">
              <el-radio label="free">免费注册</el-radio>
              <el-radio label="purchase">累计消费满</el-radio>
              <el-radio label="approval">人工审核</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="累计消费门槛" v-if="promotionForm.promoterCondition === 'purchase'">
            <el-input-number v-model="promotionForm.purchaseThreshold" :min="0" :max="100000" :precision="0" />
            <span class="tips">元</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 平台抽佣 -->
      <el-tab-pane label="平台抽佣" name="platformCommission">
        <el-form ref="platformCommissionFormRef" :model="platformCommissionForm" label-width="210px" style="max-width: 900px">
          <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
            <template #title>
              平台抽佣用于配置全平台所有作品的普通订单、藏家转售和平台收款账户；比例按订单实付金额计算。
            </template>
          </el-alert>

          <el-divider content-position="left">抽佣开关</el-divider>

          <el-form-item label="启用平台抽佣">
            <el-switch v-model="platformCommissionForm.enabled" />
            <span class="tips">关闭后不计算平台抽佣</span>
          </el-form-item>

          <el-divider content-position="left">全平台所有作品</el-divider>

          <el-form-item label="作品订单平台抽佣">
            <el-input-number v-model="platformCommissionForm.primarySaleRate" :min="0" :max="100" :precision="2" />
            <span class="tips">% 的订单实付金额，适用于全平台所有作品普通购买订单</span>
          </el-form-item>

          <el-divider content-position="left">转售订单</el-divider>

          <el-form-item label="转售平台服务费">
            <el-input-number v-model="platformCommissionForm.resalePlatformFeeRate" :min="0" :max="100" :precision="2" />
            <span class="tips">% 的转售成交价</span>
          </el-form-item>

          <el-form-item label="艺术家持续收益">
            <el-input-number v-model="platformCommissionForm.resaleArtistIncomeRate" :min="0" :max="100" :precision="2" />
            <span class="tips">% 的转售成交价，支付给原艺术家</span>
          </el-form-item>

          <el-form-item label="最低平台服务费">
            <el-input-number v-model="platformCommissionForm.minPlatformFee" :min="0" :max="100000" :precision="2" />
            <span class="tips">元，计算结果低于该金额时按该金额收取</span>
          </el-form-item>

          <el-form-item label="结算时机">
            <el-select v-model="platformCommissionForm.settlementType" style="width: 240px">
              <el-option label="支付成功后" value="after_pay" />
              <el-option label="确认收货后" value="after_confirm" />
              <el-option label="超过退款期后" value="after_refund" />
            </el-select>
          </el-form-item>

          <el-divider content-position="left">平台账户</el-divider>

          <el-form-item label="平台收款钱包UID">
            <el-input
              v-model="platformCommissionForm.platformWalletUid"
              placeholder="请输入平台收款用户UID"
              style="width: 360px"
              clearable
            />
            <span class="tips">平台服务费入账的钱包用户 UID</span>
          </el-form-item>

          <el-divider content-position="left">财务数据</el-divider>

          <div class="finance-stats-grid">
            <div
              v-for="item in platformCommissionFinanceItems"
              :key="item.key"
              class="finance-stat-card"
            >
              <span>{{ item.label }}</span>
              <b>{{ formatMoney(item.amount) }}</b>
              <small>{{ item.description }}，共 {{ item.count || 0 }} 笔</small>
            </div>
          </div>

          <el-divider content-position="left">佣金流水</el-divider>

          <div class="commission-flow-section">
            <div class="commission-flow-toolbar">
              <el-input
                v-model="commissionFlowQuery.keyword"
                placeholder="搜索订单号、作品、买家、卖家"
                clearable
                style="width: 320px"
                @keyup.enter="loadPlatformCommissionFlows"
                @clear="loadPlatformCommissionFlows"
              />
              <el-button @click="loadPlatformCommissionFlows">查询</el-button>
            </div>
            <el-table
              v-loading="commissionFlowLoading"
              :data="commissionFlows"
              class="commission-flow-table"
              row-key="billId"
              @row-click="openCommissionFlowDetail"
            >
              <el-table-column prop="recordTime" label="时间" min-width="170" />
              <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
              <el-table-column prop="dealAmount" label="成交额" width="130">
                <template #default="{ row }">{{ formatMoney(row.dealAmount) }}</template>
              </el-table-column>
              <el-table-column prop="amount" label="佣金金额" width="130">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="buyerName" label="买家" min-width="120" show-overflow-tooltip />
              <el-table-column prop="sellerName" label="卖家/艺术家" min-width="130" show-overflow-tooltip />
              <el-table-column prop="artworkTitle" label="作品" min-width="150" show-overflow-tooltip />
              <el-table-column prop="clientType" label="客户端" width="110" />
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="openCommissionFlowDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="commission-flow-pagination">
              <el-pagination
                v-model:current-page="commissionFlowQuery.page"
                v-model:page-size="commissionFlowQuery.size"
                :total="commissionFlowTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @current-change="loadPlatformCommissionFlows"
                @size-change="handleCommissionFlowSizeChange"
              />
            </div>
          </div>
        </el-form>
      </el-tab-pane>

      <!-- 优惠券配置 -->
      <el-tab-pane label="优惠券配置" name="coupon">
        <el-form ref="couponFormRef" :model="couponForm" label-width="220px" style="max-width: 960px">
          <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
            <template #title>
              优惠券配置用于控制买家现金券、艺术家分成抵用券等权益规则；具体发放批次可在后续优惠券管理中引用这些默认值。
            </template>
          </el-alert>

          <el-divider content-position="left">基础规则</el-divider>

          <el-form-item label="启用优惠券">
            <el-switch v-model="couponForm.enabled" />
            <span class="tips">关闭后前台不展示优惠券入口，订单不可使用优惠券</span>
          </el-form-item>

          <el-form-item label="允许叠加使用">
            <el-switch v-model="couponForm.stackEnabled" />
            <span class="tips">开启后现金券和经纪人分成抵用券可在同一订单中同时使用</span>
          </el-form-item>

          <el-divider content-position="left">现金券</el-divider>

          <el-form-item label="启用现金券">
            <el-switch v-model="couponForm.cashCouponEnabled" />
            <span class="tips">用于买家订单金额直接抵扣</span>
          </el-form-item>

          <el-form-item label="默认面额">
            <el-input-number v-model="couponForm.cashDefaultAmount" :min="0" :max="100000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>

          <el-form-item label="最低订单金额">
            <el-input-number v-model="couponForm.cashMinOrderAmount" :min="0" :max="1000000" :precision="2" />
            <span class="tips">元，订单实付金额达到后可使用</span>
          </el-form-item>

          <el-form-item label="最高抵扣金额">
            <el-input-number v-model="couponForm.cashMaxDiscountAmount" :min="0" :max="100000" :precision="2" />
            <span class="tips">元，防止大额叠加超出运营预算</span>
          </el-form-item>

          <el-form-item label="有效期">
            <el-input-number v-model="couponForm.cashValidDays" :min="1" :max="365" />
            <span class="tips">天</span>
          </el-form-item>

          <el-form-item label="单用户领取上限">
            <el-input-number v-model="couponForm.cashUserLimit" :min="0" :max="9999" />
            <span class="tips">张，0 表示不限</span>
          </el-form-item>

          <el-divider content-position="left">艺术家经纪人分成抵用券</el-divider>

          <el-form-item label="启用经纪人分成抵用券">
            <el-switch v-model="couponForm.artistCommissionCouponEnabled" />
            <span class="tips">用于艺术家发布/成交时抵扣平台抽佣或服务费</span>
          </el-form-item>

          <el-form-item label="默认抵扣比例">
            <el-input-number v-model="couponForm.artistCommissionDefaultRate" :min="0" :max="100" :precision="2" />
            <span class="tips">% 的应付经纪人分成</span>
          </el-form-item>

          <el-form-item label="最高抵扣金额">
            <el-input-number v-model="couponForm.artistCommissionMaxAmount" :min="0" :max="1000000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>

          <el-form-item label="有效期">
            <el-input-number v-model="couponForm.artistCommissionValidDays" :min="1" :max="365" />
            <span class="tips">天</span>
          </el-form-item>

          <el-form-item label="适用范围">
            <el-select v-model="couponForm.artistCommissionScope" style="width: 280px">
              <el-option label="艺术家首发订单" value="artist_primary_sale" />
              <el-option label="艺术家转售持续收益" value="artist_resale_income" />
              <el-option label="全部艺术家分成" value="all_artist_commission" />
            </el-select>
          </el-form-item>

          <el-form-item label="单用户领取上限">
            <el-input-number v-model="couponForm.artistCommissionUserLimit" :min="0" :max="9999" />
            <span class="tips">张，0 表示不限</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 热度增长 -->
      <el-tab-pane label="热度增长" name="trafficGrowth">
        <el-form ref="trafficFormRef" :model="trafficForm" label-width="210px" style="max-width: 1000px">
          <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
            <template #title>
              统一配置页面、作品、艺术家的展示热度增长；用于运营冷启动和日常热度托底。
            </template>
          </el-alert>

          <el-divider content-position="left">所有页面</el-divider>

          <el-form-item label="页面浏览量增长">
            <el-switch v-model="trafficForm.pageViewGrowthEnabled" />
            <span class="tips">开启后，全站页面浏览量按日/周/月展示增长</span>
          </el-form-item>
          <el-form-item label="页面每日浏览量">
            <el-input-number v-model="trafficForm.pageDailyViewGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/天</span>
          </el-form-item>
          <el-form-item label="页面每周浏览量">
            <el-input-number v-model="trafficForm.pageWeeklyViewGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/周</span>
          </el-form-item>
          <el-form-item label="页面每月浏览量">
            <el-input-number v-model="trafficForm.pageMonthlyViewGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/月</span>
          </el-form-item>

          <el-form-item label="页面收藏量增长">
            <el-switch v-model="trafficForm.pageFavoriteGrowthEnabled" />
            <span class="tips">开启后，全站页面收藏量按日/周/月展示增长</span>
          </el-form-item>
          <el-form-item label="页面每日收藏量">
            <el-input-number v-model="trafficForm.pageDailyFavoriteGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/天</span>
          </el-form-item>
          <el-form-item label="页面每周收藏量">
            <el-input-number v-model="trafficForm.pageWeeklyFavoriteGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/周</span>
          </el-form-item>
          <el-form-item label="页面每月收藏量">
            <el-input-number v-model="trafficForm.pageMonthlyFavoriteGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/月</span>
          </el-form-item>

          <el-divider content-position="left">作品日常热度</el-divider>

          <el-form-item label="作品热度增长">
            <el-switch v-model="trafficForm.artworkHeatGrowthEnabled" />
            <span class="tips">开启后，作品浏览、点赞、收藏按日展示增长</span>
          </el-form-item>
          <el-form-item label="作品每日浏览量">
            <el-input-number v-model="trafficForm.artworkDailyViewGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/天</span>
          </el-form-item>
          <el-form-item label="作品每日点赞量">
            <el-input-number v-model="trafficForm.artworkDailyLikeGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/天</span>
          </el-form-item>
          <el-form-item label="作品每日收藏量">
            <el-input-number v-model="trafficForm.artworkDailyFavoriteGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/天</span>
          </el-form-item>

          <el-divider content-position="left">艺术家日常热度</el-divider>

          <el-form-item label="艺术家热度增长">
            <el-switch v-model="trafficForm.artistHeatGrowthEnabled" />
            <span class="tips">开启后，艺术家关注、点赞按日展示增长</span>
          </el-form-item>
          <el-form-item label="艺术家每日关注量">
            <el-input-number v-model="trafficForm.artistDailyFollowGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">人/天</span>
          </el-form-item>
          <el-form-item label="艺术家每日点赞量">
            <el-input-number v-model="trafficForm.artistDailyLikeGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">次/天</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 价格增长机制 -->
      <el-tab-pane label="价格增长" name="priceGrowth">
        <el-form ref="priceFormRef" :model="priceForm" label-width="200px" style="max-width: 1000px">
          <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
            <template #title>
              <strong>价格增长规则：</strong>预估价格 = 原价 × 时间系数 × 艺术家知名度系数 × 热度系数；成交后按单次销售加成单独计算涨价
            </template>
          </el-alert>

          <div class="price-calculator">
            <div class="calculator-header">
              <div>
                <div class="calculator-title">价格增长计算器</div>
                <div class="calculator-desc">使用当前配置实时预估：预估价格不计入销售加成，成交加成单独展示</div>
              </div>
              <div class="calculator-sale-hint">
                <span>艺术家 1 次成交后价格：¥{{ priceCalculatorResult.oneSalePrice }}（+{{ priceCalculatorResult.oneSaleGrowthRate }}%）</span>
                <span>艺术家 2 次成交后价格：¥{{ priceCalculatorResult.twoSalePrice }}（+{{ priceCalculatorResult.twoSaleGrowthRate }}%）</span>
              </div>
              <div class="calculator-result">
                <span>预估价格</span>
                <strong>¥{{ priceCalculatorResult.finalPrice }}</strong>
                <em>不含销售加成</em>
              </div>
            </div>

          <div class="calculator-grid">
              <el-form-item label="原价">
                <el-input-number v-model="priceCalculator.originalPrice" :min="0" :max="100000000" :precision="2" />
                <span class="tips">元</span>
              </el-form-item>
              <el-form-item label="艺术家等级">
                <el-select v-model="priceCalculator.artistLevel" style="width: 180px">
                  <el-option label="普通艺术家" value="default" />
                  <el-option label="认证艺术家" value="verified" />
                  <el-option label="人气艺术家" value="popular" />
                  <el-option label="大师级艺术家" value="master" />
                </el-select>
              </el-form-item>
              <div class="calculator-readonly">
                <span>增长天数</span>
                <b>{{ priceCalculatorConfig.days }}</b>
                <em>天</em>
              </div>
              <div class="calculator-readonly">
                <span>浏览量</span>
                <b>{{ priceCalculatorConfig.views }}</b>
                <em>次</em>
                <small>含 {{ formatPercent(priceForm.viewGrowthRandomRate) }} 随机浮动</small>
              </div>
              <div class="calculator-readonly">
                <span>收藏量</span>
                <b>{{ priceCalculatorConfig.favorites }}</b>
                <em>次</em>
              </div>
              <div class="calculator-readonly">
                <span>一次成交预计涨价</span>
                <b>¥{{ priceCalculatorResult.oneSaleIncrease }}</b>
                <em>/次</em>
              </div>
            </div>

            <div class="calculator-breakdown">
              <div>
                <span>时间系数</span>
                <b>{{ priceCalculatorResult.timeFactor }}</b>
              </div>
              <div>
                <span>艺术家系数</span>
                <b>{{ priceCalculatorResult.artistFactor }}</b>
              </div>
              <div>
                <span>热度系数</span>
                <b>{{ priceCalculatorResult.heatFactor }}</b>
              </div>
              <div>
                <span>单次成交系数</span>
                <b>{{ priceCalculatorResult.oneSaleFactor }}</b>
              </div>
              <div>
                <span>累计涨幅</span>
                <b class="growth">+{{ priceCalculatorResult.growthRate }}%</b>
              </div>
            </div>
          </div>
          
          <el-divider content-position="left">时间因素</el-divider>
          
          <el-form-item label="基础日增长率">
            <el-input-number v-model="priceForm.baseDailyRate" :min="0" :max="1" :precision="4" :step="0.0001" />
            <span class="tips">每天增长的比例，如 0.0002 表示 0.02%/天</span>
          </el-form-item>
          
          <el-form-item label="成熟期日增长率">
            <el-input-number v-model="priceForm.matureDailyRate" :min="0" :max="1" :precision="4" :step="0.0001" />
            <span class="tips">发布超过阈值天数后的日增长率</span>
          </el-form-item>
          
          <el-form-item label="成熟期天数阈值">
            <el-input-number v-model="priceForm.matureDays" :min="0" :max="365" />
            <span class="tips">天（超过此天数使用成熟期增长率）</span>
          </el-form-item>
          
          <el-divider content-position="left">艺术家知名度系数</el-divider>
          
          <el-form-item label="普通艺术家">
            <el-input-number v-model="priceForm.defaultBadgeRate" :min="1" :max="5" :precision="3" :step="0.001" />
            <span class="tips">倍（基准系数）</span>
          </el-form-item>
          
          <el-form-item label="认证艺术家">
            <el-input-number v-model="priceForm.verifiedBadgeRate" :min="1" :max="5" :precision="3" :step="0.001" />
            <span class="tips">倍</span>
          </el-form-item>
          
          <el-form-item label="人气艺术家">
            <el-input-number v-model="priceForm.popularBadgeRate" :min="1" :max="5" :precision="3" :step="0.001" />
            <span class="tips">倍</span>
          </el-form-item>
          
          <el-form-item label="大师级艺术家">
            <el-input-number v-model="priceForm.masterBadgeRate" :min="1" :max="10" :precision="3" :step="0.001" />
            <span class="tips">倍</span>
          </el-form-item>
          
          <el-divider content-position="left">热度系数</el-divider>
          
          <el-form-item label="浏览量阈值">
            <el-input-number v-model="priceForm.viewThreshold" :min="0" :max="100000" />
            <span class="tips">次（达到此浏览量触发加成）</span>
          </el-form-item>
          <el-form-item label="浏览量加成系数">
            <el-input-number v-model="priceForm.viewRate" :min="1" :max="3" :precision="3" :step="0.001" />
            <span class="tips">倍</span>
          </el-form-item>

          <el-divider content-position="left">浏览量自动增长</el-divider>

          <el-form-item label="自动增长开关">
            <el-switch v-model="priceForm.viewAutoGrowthEnabled" />
            <span class="tips">开启后，作品展示浏览量会叠加全局每日、每周、每月增长</span>
          </el-form-item>
          <el-form-item label="随机浮动比例">
            <el-input-number v-model="priceForm.viewGrowthRandomRate" :min="0" :max="1" :precision="3" :step="0.001" />
            <span class="tips">基数上下浮动，0.580 表示每个作品按基数上下 58% 随机</span>
          </el-form-item>
          <el-form-item label="每日浏览量增长">
            <el-input-number v-model="priceForm.dailyViewGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">基数/天，实际应用按随机浮动后叠加到作品</span>
          </el-form-item>
          <el-form-item label="每周浏览量增长">
            <el-input-number v-model="priceForm.weeklyViewGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">基数/周，按作品上线周数随机叠加</span>
          </el-form-item>
          <el-form-item label="每月浏览量增长">
            <el-input-number v-model="priceForm.monthlyViewGrowth" :min="0" :max="999999" :precision="0" />
            <span class="tips">基数/月，按作品上线月数随机叠加</span>
          </el-form-item>
          
          <el-form-item label="收藏量阈值">
            <el-input-number v-model="priceForm.favoriteThreshold" :min="0" :max="10000" />
            <span class="tips">次（达到此收藏量触发加成）</span>
          </el-form-item>
          <el-form-item label="收藏量加成系数">
            <el-input-number v-model="priceForm.favoriteRate" :min="1" :max="3" :precision="3" :step="0.001" />
            <span class="tips">倍</span>
          </el-form-item>
          
          <el-divider content-position="left">销售加成</el-divider>
          
          <el-form-item label="单次销售加成">
            <el-input-number v-model="priceForm.saleRate" :min="0" :max="1" :precision="3" :step="0.001" />
            <span class="tips">每售出一次，价格上涨此比例（如 0.05 = 5%）</span>
          </el-form-item>
          
          <el-form-item label="最多计算销售次数">
            <el-input-number v-model="priceForm.maxSaleCount" :min="1" :max="100" />
            <span class="tips">次（超过此次数不再累加）</span>
          </el-form-item>
          
          <el-divider content-position="left">涨幅限制</el-divider>
          
          <el-form-item label="最大涨幅倍数">
            <el-input-number v-model="priceForm.maxGrowthMultiple" :min="1" :max="20" :precision="3" :step="0.001" />
            <span class="tips">倍（价格最多上涨到此倍数的原价）</span>
          </el-form-item>
          
          <el-form-item label="价格增长开关">
            <el-switch v-model="priceForm.enabled" />
            <span class="tips">关闭后作品价格不再自动增长</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 拍卖设置 -->
      <el-tab-pane label="拍卖设置" name="auction">
        <el-form ref="auctionFormRef" :model="auctionForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">保证金设置</el-divider>
          
          <el-form-item label="拍卖保证金">
            <el-input-number v-model="auctionForm.auctionDeposit" :min="0" :max="1000000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>
          
          <el-form-item label="保证金退还">
            <el-switch v-model="auctionForm.depositRefund" />
            <span class="tips">竞拍失败后自动退还</span>
          </el-form-item>
          
          <el-divider content-position="left">出价设置</el-divider>
          
          <el-form-item label="延时加价幅度">
            <el-input-number v-model="auctionForm.bidIncrement" :min="0" :max="100000" :precision="2" />
            <span class="tips">元</span>
          </el-form-item>
          
          <el-form-item label="延时周期">
            <el-input-number v-model="auctionForm.delayCycles" :min="1" :max="10" />
            <span class="tips">轮（临近结束时每次延长此轮数）</span>
          </el-form-item>
          
          <el-form-item label="延时时长">
            <el-input-number v-model="auctionForm.delayMinutes" :min="1" :max="30" />
            <span class="tips">分钟</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 审核设置 -->
      <el-tab-pane label="审核设置" name="audit">
        <el-form ref="auditFormRef" :model="auditForm" label-width="180px" style="max-width: 900px">
          <el-divider content-position="left">审核开关</el-divider>
          
          <el-form-item label="艺术家认证审核">
            <el-switch v-model="auditForm.artistAudit" />
            <span class="tips">开启后需要后台审核艺术家认证申请</span>
          </el-form-item>
          
          <el-form-item label="作品审核">
            <el-switch v-model="auditForm.artworkAudit" />
            <span class="tips">开启后需要后台审核作品上架</span>
          </el-form-item>
          
          <el-form-item label="动态审核">
            <el-switch v-model="auditForm.postAudit" />
            <span class="tips">开启后需要后台审核艺术圈动态</span>
          </el-form-item>
          
          <el-divider content-position="left">敏感词过滤</el-divider>
          
          <el-form-item label="敏感词过滤">
            <el-switch v-model="auditForm.sensitiveFilter" />
          </el-form-item>
          
          <el-form-item label="敏感词库">
            <el-input
              type="textarea"
              v-model="auditForm.sensitiveWords"
              :rows="5"
              placeholder="每行一个敏感词"
              style="width: 500px"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <el-drawer
      v-model="commissionFlowDetailVisible"
      title="佣金流水详情"
      size="520px"
      destroy-on-close
    >
      <div v-loading="commissionFlowDetailLoading" class="commission-detail">
        <template v-if="commissionFlowDetail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="时间">{{ commissionFlowDetail.recordTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ commissionFlowDetail.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="成交金额">{{ formatMoney(commissionFlowDetail.dealAmount) }}</el-descriptions-item>
            <el-descriptions-item label="佣金金额">{{ formatMoney(commissionFlowDetail.amount) }}</el-descriptions-item>
            <el-descriptions-item label="成交双方">
              {{ commissionFlowDetail.buyerName || '-' }} / {{ commissionFlowDetail.sellerName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="买家UID">{{ commissionFlowDetail.buyerUid || '-' }}</el-descriptions-item>
            <el-descriptions-item label="买家手机号">{{ commissionFlowDetail.buyerPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="卖家UID">{{ commissionFlowDetail.sellerUid || '-' }}</el-descriptions-item>
            <el-descriptions-item label="卖家手机号">{{ commissionFlowDetail.sellerPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作品">{{ commissionFlowDetail.artworkTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作品ID">{{ commissionFlowDetail.artworkId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户端">{{ commissionFlowDetail.clientType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支付渠道">{{ commissionFlowDetail.paymentChannel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="渠道流水">{{ commissionFlowDetail.channelTradeNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              {{ commissionFlowDetail.orderStatus || '-' }} / {{ commissionFlowDetail.paymentStatus || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ commissionFlowDetail.paidAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ commissionFlowDetail.completedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商品金额">{{ formatMoney(commissionFlowDetail.goodsAmount) }}</el-descriptions-item>
            <el-descriptions-item label="运费">{{ formatMoney(commissionFlowDetail.freightAmount) }}</el-descriptions-item>
            <el-descriptions-item label="优惠">{{ formatMoney(commissionFlowDetail.discountAmount) }}</el-descriptions-item>
            <el-descriptions-item label="钱包余额变化">
              {{ formatMoney(commissionFlowDetail.beforeBalance) }} -> {{ formatMoney(commissionFlowDetail.afterBalance) }}
            </el-descriptions-item>
            <el-descriptions-item label="备注">{{ commissionFlowDetail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const activeTab = ref('trade')

// 交易设置表单
const tradeFormRef = ref()
const tradeForm = reactive({
  orderTimeout: 30,
  refundDays: 7,
  allowRepeatBuy: false,
  priceUnit: 'fen'
})

// 分销设置表单
const promotionFormRef = ref()
const promotionForm = reactive({
  directCommission: 5,
  teamCommission: 2,
  settlementType: 'after_pay',
  minWithdraw: 100,
  withdrawFee: 0,
  withdrawDays: 3,
  promoterCondition: 'free',
  purchaseThreshold: 1000
})

// 平台抽佣表单
const platformCommissionFormRef = ref()
const platformCommissionForm = reactive({
  enabled: true,
  primarySaleRate: 10,
  resalePlatformFeeRate: 15,
  resaleArtistIncomeRate: 5,
  minPlatformFee: 0,
  platformWalletUid: '',
  settlementType: 'after_pay'
})

const platformCommissionFinance = reactive({
  artistCertification: {},
  afterPayCommission: {},
  afterConfirmSettlement: {},
  afterRefundSettlement: {}
})

const commissionFlows = ref([])
const commissionFlowTotal = ref(0)
const commissionFlowLoading = ref(false)
const commissionFlowDetailVisible = ref(false)
const commissionFlowDetailLoading = ref(false)
const commissionFlowDetail = ref(null)
const commissionFlowQuery = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

// 优惠券配置表单
const couponFormRef = ref()
const couponForm = reactive({
  enabled: true,
  stackEnabled: false,
  cashCouponEnabled: true,
  cashDefaultAmount: 10,
  cashMinOrderAmount: 100,
  cashMaxDiscountAmount: 100,
  cashValidDays: 30,
  cashUserLimit: 1,
  artistCommissionCouponEnabled: true,
  artistCommissionDefaultRate: 5,
  artistCommissionMaxAmount: 500,
  artistCommissionValidDays: 30,
  artistCommissionScope: 'artist_primary_sale',
  artistCommissionUserLimit: 1
})

// 热度增长表单
const trafficFormRef = ref()
const trafficForm = reactive({
  pageViewGrowthEnabled: false,
  pageDailyViewGrowth: 0,
  pageWeeklyViewGrowth: 0,
  pageMonthlyViewGrowth: 0,
  pageFavoriteGrowthEnabled: false,
  pageDailyFavoriteGrowth: 0,
  pageWeeklyFavoriteGrowth: 0,
  pageMonthlyFavoriteGrowth: 0,
  artworkHeatGrowthEnabled: false,
  artworkDailyViewGrowth: 0,
  artworkDailyLikeGrowth: 0,
  artworkDailyFavoriteGrowth: 0,
  artistHeatGrowthEnabled: false,
  artistDailyFollowGrowth: 0,
  artistDailyLikeGrowth: 0
})

// 价格增长表单
const priceFormRef = ref()
const priceForm = reactive({
  enabled: true,
  // 时间因素
  baseDailyRate: 0.0002,
  matureDailyRate: 0.0003,
  matureDays: 30,
  // 艺术家知名度系数
  defaultBadgeRate: 1.0,
  verifiedBadgeRate: 1.5,
  popularBadgeRate: 2.0,
  masterBadgeRate: 3.0,
  // 热度系数
  viewThreshold: 100,
  viewRate: 1.1,
  viewAutoGrowthEnabled: false,
  viewGrowthRandomRate: 0.58,
  dailyViewGrowth: 0,
  weeklyViewGrowth: 0,
  monthlyViewGrowth: 0,
  favoriteThreshold: 5,
  favoriteRate: 1.1,
  // 销售加成
  saleRate: 0.05,
  maxSaleCount: 10,
  // 涨幅限制
  maxGrowthMultiple: 5.0
})

const priceCalculator = reactive({
  originalPrice: 100,
  artistLevel: 'default'
})

const priceCalculatorRandom = {
  daily: Math.random(),
  weekly: Math.random(),
  monthly: Math.random()
}

const roundNumber = (value, precision = 2) => {
  const numeric = Number(value || 0)
  return Number.isFinite(numeric) ? numeric.toFixed(precision) : (0).toFixed(precision)
}

const clampRate = (value) => Math.min(1, Math.max(0, Number(value || 0)))

const randomizeByRate = (base, randomUnit, rate) => {
  const numericBase = Math.max(0, Number(base || 0))
  const normalizedRate = clampRate(rate)
  const factor = 1 - normalizedRate + randomUnit * normalizedRate * 2
  return numericBase * factor
}

const formatPercent = (value) => `${roundNumber(clampRate(value) * 100, 1)}%`

const formatMoney = (value) => {
  const numeric = Number(value || 0)
  return `¥${numeric.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

const platformCommissionFinanceItems = computed(() => [
  {
    key: 'artistCertification',
    label: platformCommissionFinance.artistCertification?.label || '艺术家认证支付费用',
    amount: platformCommissionFinance.artistCertification?.amount,
    count: platformCommissionFinance.artistCertification?.count,
    description: platformCommissionFinance.artistCertification?.description || '统计认证类支付订单与认证相关钱包流水'
  },
  {
    key: 'afterPayCommission',
    label: platformCommissionFinance.afterPayCommission?.label || '支付成功后平台抽佣营收',
    amount: platformCommissionFinance.afterPayCommission?.amount,
    count: platformCommissionFinance.afterPayCommission?.count,
    description: platformCommissionFinance.afterPayCommission?.description || '已支付未退款订单按当前抽佣比例统计'
  },
  {
    key: 'afterConfirmSettlement',
    label: platformCommissionFinance.afterConfirmSettlement?.label || '确认收货后到账金额',
    amount: platformCommissionFinance.afterConfirmSettlement?.amount,
    count: platformCommissionFinance.afterConfirmSettlement?.count,
    description: platformCommissionFinance.afterConfirmSettlement?.description || '已完成订单按当前抽佣比例统计'
  },
  {
    key: 'afterRefundSettlement',
    label: platformCommissionFinance.afterRefundSettlement?.label || '超过退款期后到账金额',
    amount: platformCommissionFinance.afterRefundSettlement?.amount,
    count: platformCommissionFinance.afterRefundSettlement?.count,
    description: platformCommissionFinance.afterRefundSettlement?.description || '已过退款周期订单按当前抽佣比例统计'
  }
])

const priceCalculatorConfig = computed(() => {
  const days = Math.max(0, Number(priceForm.matureDays || 0))
  const fluctuationRate = clampRate(priceForm.viewGrowthRandomRate)
  const dailyGrowth = randomizeByRate(priceForm.dailyViewGrowth, priceCalculatorRandom.daily, fluctuationRate)
  const weeklyGrowth = randomizeByRate(priceForm.weeklyViewGrowth, priceCalculatorRandom.weekly, fluctuationRate)
  const monthlyGrowth = randomizeByRate(priceForm.monthlyViewGrowth, priceCalculatorRandom.monthly, fluctuationRate)
  const autoGrowthViews = priceForm.viewAutoGrowthEnabled
    ? (dailyGrowth * days) +
      (weeklyGrowth * Math.floor(days / 7)) +
      (monthlyGrowth * Math.floor(days / 30))
    : 0
  const views = Math.max(Number(priceForm.viewThreshold || 0), autoGrowthViews)

  return {
    days,
    views: Math.round(Math.max(0, views)),
    favorites: Math.max(0, Number(priceForm.favoriteThreshold || 0))
  }
})

const priceCalculatorResult = computed(() => {
  const originalPrice = Number(priceCalculator.originalPrice || 0)
  const days = priceCalculatorConfig.value.days
  const matureDays = Math.max(0, Number(priceForm.matureDays || 0))
  const baseDailyRate = Number(priceForm.baseDailyRate || 0)
  const matureDailyRate = Number(priceForm.matureDailyRate || 0)
  const baseDays = Math.min(days, matureDays)
  const matureGrowthDays = Math.max(0, days - matureDays)
  const timeFactor = Math.pow(1 + baseDailyRate, baseDays) * Math.pow(1 + matureDailyRate, matureGrowthDays)

  const artistFactorMap = {
    default: priceForm.defaultBadgeRate,
    verified: priceForm.verifiedBadgeRate,
    popular: priceForm.popularBadgeRate,
    master: priceForm.masterBadgeRate
  }
  const artistFactor = Number(artistFactorMap[priceCalculator.artistLevel] || 1)

  let heatFactor = 1
  if (priceCalculatorConfig.value.views >= Number(priceForm.viewThreshold || 0)) {
    heatFactor *= Number(priceForm.viewRate || 1)
  }
  if (priceCalculatorConfig.value.favorites >= Number(priceForm.favoriteThreshold || 0)) {
    heatFactor *= Number(priceForm.favoriteRate || 1)
  }

  const maxPrice = originalPrice * Math.max(1, Number(priceForm.maxGrowthMultiple || 1))
  const rawPrice = originalPrice * timeFactor * artistFactor * heatFactor
  const finalPrice = originalPrice > 0 ? Math.min(rawPrice, maxPrice) : 0
  const growthRate = originalPrice > 0 ? ((finalPrice / originalPrice - 1) * 100) : 0
  const oneSaleFactor = 1 + Math.max(0, Number(priceForm.saleRate || 0))
  const oneSalePrice = Math.min(finalPrice * oneSaleFactor, maxPrice)
  const oneSaleIncrease = Math.max(0, oneSalePrice - finalPrice)
  const twoSalePrice = Math.min(finalPrice * Math.pow(oneSaleFactor, 2), maxPrice)
  const twoSaleIncrease = Math.max(0, twoSalePrice - finalPrice)
  const oneSaleGrowthRate = finalPrice > 0 ? (oneSaleIncrease / finalPrice) * 100 : 0
  const twoSaleGrowthRate = finalPrice > 0 ? (twoSaleIncrease / finalPrice) * 100 : 0

  return {
    finalPrice: roundNumber(finalPrice),
    oneSalePrice: roundNumber(oneSalePrice),
    oneSaleIncrease: roundNumber(oneSaleIncrease),
    oneSaleGrowthRate: roundNumber(oneSaleGrowthRate),
    twoSalePrice: roundNumber(twoSalePrice),
    twoSaleIncrease: roundNumber(twoSaleIncrease),
    twoSaleGrowthRate: roundNumber(twoSaleGrowthRate),
    growthRate: roundNumber(growthRate),
    timeFactor: roundNumber(timeFactor, 4),
    artistFactor: roundNumber(artistFactor, 3),
    heatFactor: roundNumber(heatFactor, 3),
    oneSaleFactor: roundNumber(oneSaleFactor, 4)
  }
})

// 拍卖设置表单
const auctionFormRef = ref()
const auctionForm = reactive({
  auctionDeposit: 1000,
  depositRefund: true,
  bidIncrement: 100,
  delayCycles: 3,
  delayMinutes: 5
})

// 审核设置表单
const auditFormRef = ref()
const auditForm = reactive({
  artistAudit: true,
  artworkAudit: true,
  postAudit: false,
  sensitiveFilter: true,
  sensitiveWords: '***\n***\n***'
})

// 加载所有配置
const loadAllConfig = async () => {
  try {
    const data = await request.get('/config/all')
    
    // 合并到各表单
    if (data.trade) Object.assign(tradeForm, data.trade)
    if (data.promotion) Object.assign(promotionForm, data.promotion)
    if (data.platformCommission) Object.assign(platformCommissionForm, data.platformCommission)
    if (data.coupon) Object.assign(couponForm, data.coupon)
    if (data.trafficGrowth) Object.assign(trafficForm, data.trafficGrowth)
    if (data.priceGrowth) Object.assign(priceForm, data.priceGrowth)
    try {
      Object.assign(priceForm, await request.get('/config/priceGrowth'))
    } catch (e) {
      console.warn('价格调控配置加载失败，保留系统配置值', e)
    }
    if (data.auction) Object.assign(auctionForm, data.auction)
    if (data.audit) Object.assign(auditForm, data.audit)
    await loadPlatformCommissionFinance()
    await loadPlatformCommissionFlows()
  } catch (e) {
    ElMessage.error('配置加载失败')
  }
}

const loadPlatformCommissionFinance = async () => {
  try {
    const data = await request.get('/config/platformCommission/finance')
    Object.assign(platformCommissionFinance, data || {})
  } catch (e) {
    console.warn('平台抽佣财务数据加载失败', e)
  }
}

const loadPlatformCommissionFlows = async () => {
  commissionFlowLoading.value = true
  try {
    const data = await request.get('/config/platformCommission/flows', {
      params: {
        page: commissionFlowQuery.page,
        size: commissionFlowQuery.size,
        keyword: commissionFlowQuery.keyword
      }
    })
    commissionFlows.value = data?.records || []
    commissionFlowTotal.value = Number(data?.total || 0)
  } catch (e) {
    console.warn('平台佣金流水加载失败', e)
  } finally {
    commissionFlowLoading.value = false
  }
}

const handleCommissionFlowSizeChange = () => {
  commissionFlowQuery.page = 1
  loadPlatformCommissionFlows()
}

const openCommissionFlowDetail = async (row) => {
  if (!row?.billId) return
  commissionFlowDetailVisible.value = true
  commissionFlowDetailLoading.value = true
  commissionFlowDetail.value = null
  try {
    commissionFlowDetail.value = await request.get(`/config/platformCommission/flows/${row.billId}`)
  } catch (e) {
    ElMessage.error('佣金流水详情加载失败')
  } finally {
    commissionFlowDetailLoading.value = false
  }
}

// 保存配置
const handleSave = async () => {
  try {
    await request.post('/config/priceGrowth', priceForm)
    await request.post('/config/update', {
      trade: tradeForm,
      promotion: promotionForm,
      platformCommission: platformCommissionForm,
      coupon: couponForm,
      trafficGrowth: trafficForm,
      priceGrowth: priceForm,
      auction: auctionForm,
      audit: auditForm
    })
    await loadAllConfig()
    ElMessage.success('配置保存成功')
  } catch (e) {
    ElMessage.error('配置保存失败')
  }
}

onMounted(() => {
  loadAllConfig()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.title {
  font-size: 20px;
  font-weight: 600;
}

.config-tabs {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.tips {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.el-divider {
  margin: 20px 0;
}

.price-calculator {
  margin-bottom: 24px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.calculator-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.calculator-title {
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.calculator-desc {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.calculator-sale-hint {
  flex: 1;
  min-width: 220px;
  padding-top: 28px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
  text-align: right;
}

.calculator-sale-hint span {
  display: block;
}

.calculator-result {
  min-width: 180px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #111827;
  color: #cbd5e1;
  text-align: right;
}

.calculator-result span {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
}

.calculator-result strong {
  display: block;
  color: #f5c84c;
  font-size: 26px;
  line-height: 1;
}

.calculator-result em {
  display: block;
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.calculator-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 24px;
}

.calculator-grid :deep(.el-form-item) {
  margin-bottom: 14px;
}

.calculator-readonly {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  min-height: 32px;
  margin-bottom: 14px;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.calculator-readonly span {
  color: #606266;
  font-size: 14px;
}

.calculator-readonly b {
  margin-left: auto;
  color: #1f2937;
  font-size: 15px;
}

.calculator-readonly em {
  margin-left: 6px;
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.calculator-readonly small {
  flex-basis: 100%;
  padding: 0 0 6px;
  color: #909399;
  font-size: 12px;
}

.calculator-breakdown {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.calculator-breakdown div {
  padding: 10px 12px;
  border-radius: 6px;
  background: #fff;
}

.calculator-breakdown span {
  display: block;
  color: #909399;
  font-size: 12px;
}

.calculator-breakdown b {
  display: block;
  margin-top: 4px;
  color: #1f2937;
  font-size: 16px;
}

.calculator-breakdown .growth {
  color: #f56c6c;
}

.finance-stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-left: 210px;
}

.finance-stat-card {
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.finance-stat-card span {
  display: block;
  color: #606266;
  font-size: 13px;
}

.finance-stat-card b {
  display: block;
  margin-top: 6px;
  color: #1f2937;
  font-size: 22px;
  line-height: 1.2;
}

.finance-stat-card small {
  display: block;
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.commission-flow-section {
  margin-left: 210px;
}

.commission-flow-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.commission-flow-table {
  width: 100%;
}

.commission-flow-table :deep(.el-table__row) {
  cursor: pointer;
}

.commission-flow-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.commission-detail :deep(.el-descriptions__label) {
  width: 128px;
}

.commission-detail :deep(.el-descriptions__content) {
  word-break: break-all;
}
</style>
