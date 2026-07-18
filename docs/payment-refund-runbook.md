# 支付退款验收 Runbook

这份 Runbook 用来做微信支付退款的发布后验收。

## 1. 先确认基础配置

```bash
cd /Users/master/CodeBuddy/art12
scripts/check-payment-config.sh deploy/docker/.env
```

如果是 Lighthouse 目录，改成：

```bash
scripts/check-payment-config.sh deploy-lighthouse/.env
```

## 2. 重启服务

在部署机进入实际部署目录执行：

```bash
docker compose up -d --build
docker compose logs -f order
docker compose logs -f admin
```

## 3. 跑支付发布验收

```bash
cd /Users/master/CodeBuddy/art12
BASE_URL=https://a.art1.cn scripts/verify-payment-release.sh deploy-lighthouse/.env
```

这一步会检查：

- 网关 `/health`
- 管理员登录 `/api/admin/login`
- 支付单列表 `/admin/order/payment/list`
- 支付通知日志 `/admin/order/payment/notify-logs`
- 售后列表 `/admin/order/aftersale/list`

## 4. 跑一次真实退款联调

先让一个真实已支付订单进入退款申请。脚本参数依次是：

1. `user_id`
2. `order_id`
3. `refund_amount`
4. `refund_id`，可选

第一轮先不传 `refund_id`，脚本会把售后列表响应打印出来，方便你确认退款记录 ID：

```bash
BASE_URL=https://a.art1.cn scripts/run-refund-smoke-test.sh 1001 2002 0.01
```

拿到退款记录 `id` 后，再执行一次审核通过：

```bash
BASE_URL=https://a.art1.cn scripts/run-refund-smoke-test.sh 1001 2002 0.01 3003
```

这一步会依次做：

- 用户侧调用 `/api/order/orders/{id}/refund`
- 管理端登录
- 查询 `/admin/order/aftersale/list`
- 审核通过 `/admin/order/aftersale/handle`
- 查询 `/admin/order/payment/list`
- 查询 `/admin/order/payment/notify-logs`

## 5. 期望结果

- `refund_record` 进入已处理
- `refund_order` 状态更新为 `REFUNDED`
- `payment_order` 状态更新为 `REFUNDED`
- 管理后台支付单列表能查到对应订单
- 微信退款回调进入通知日志

## 6. 失败时先看哪里

- `order` 服务日志：微信退款请求、证书读取、回调处理
- `admin` 服务日志：售后审核、渠道退款调用
- `payment_notify_log`：微信/支付宝回调入库情况
- `refund_order`：统一退款单状态
- `refund_record`：后台售后处理结果
