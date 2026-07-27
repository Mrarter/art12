package com.shiyiju.user.service;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 腾讯云短信服务
 *
 * 文档：https://cloud.tencent.com/document/product/382/55981
 */
@Slf4j
@Service
public class SmsService {

    @Value("${sms.secret-id:}")
    private String secretId;

    @Value("${sms.secret-key:}")
    private String secretKey;

    @Value("${sms.sdk-app-id:}")
    private String sdkAppId;

    @Value("${sms.sign-name:艺本艺术}")
    private String signName;

    @Value("${sms.template-id:}")
    private String templateId;

    @Value("${sms.enabled:false}")
    private boolean enabled;

    @Value("${sms.code-expire-minutes:5}")
    private int codeExpireMinutes;

    /**
     * 判断是否启用真实短信发送（API密钥 + 模板 全部配置且启用）
     */
    public boolean isRealSendEnabled() {
        return enabled
                && !isBlank(secretId)
                && !isBlank(secretKey)
                && !isBlank(sdkAppId)
                && !isBlank(templateId);
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号（E.164格式：+8613800138000 或 国内11位）
     * @param code  验证码
     * @return true=发送成功, false=发送失败
     */
    public boolean sendVerifyCode(String phone, String code) {
        if (!enabled) {
            log.info("【短信-模拟】phone={}, code={}（真实发送未启用，请配置 sms.enabled=true）", phone, code);
            return true;
        }

        // 校验配置完整性
        if (isBlank(secretId) || isBlank(secretKey) || isBlank(sdkAppId) || isBlank(templateId)) {
            log.warn("【短信】配置不完整，降级为模拟发送。需要 secret-id / secret-key / sdk-app-id / template-id");
            log.info("【短信-模拟】phone={}, code={}", phone, code);
            return true;
        }

        try {
            // 格式化手机号为 E.164 格式
            String phoneE164 = normalizePhone(phone);

            // 初始化认证对象
            Credential cred = new Credential(secretId, secretKey);

            // 实例化 SMS 客户端
            SmsClient client = new SmsClient(cred, "ap-guangzhou");

            // 构建请求
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(sdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);

            // 模板参数：{1}=验证码，{2}=有效期分钟
            String[] templateParams = {code, String.valueOf(Math.max(1, codeExpireMinutes))};
            req.setTemplateParamSet(templateParams);

            // 手机号（E.164格式）
            String[] phoneNumbers = {phoneE164};
            req.setPhoneNumberSet(phoneNumbers);

            // 发送
            SendSmsResponse resp = client.SendSms(req);

            // 检查结果
            SendStatus[] statuses = resp.getSendStatusSet();
            if (statuses != null && statuses.length > 0) {
                SendStatus status = statuses[0];
                if ("Ok".equals(status.getCode())) {
                    log.info("【短信】发送成功 phone={}, serialNo={}", phone, status.getSerialNo());
                    return true;
                } else {
                    log.error("【短信】发送失败 phone={}, code={}, message={}",
                            phone, status.getCode(), status.getMessage());
                    return false;
                }
            }

            log.warn("【短信】返回结果为空 phone={}", phone);
            return false;

        } catch (TencentCloudSDKException e) {
            log.error("【短信】SDK异常 phone={}, error={}", phone, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("【短信】发送异常 phone={}, error={}", phone, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 格式化手机号为 E.164 格式
     * 国内手机号自动补 +86 前缀
     */
    private String normalizePhone(String phone) {
        if (phone == null) return "";
        // 已经是 E.164 格式
        if (phone.startsWith("+")) return phone;
        // 11位国内手机号
        if (phone.matches("^1\\d{10}$")) return "+86" + phone;
        return phone;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
