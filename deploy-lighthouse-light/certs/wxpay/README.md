# WeChat Pay Refund Certificate

Place the WeChat Pay merchant certificate here before deployment:

```text
deploy-lighthouse-light/certs/wxpay/apiclient_cert.p12
```

The order and admin containers mount it as:

```text
/opt/shiyiju/certs/wxpay/apiclient_cert.p12
```

Do not commit the real certificate.
