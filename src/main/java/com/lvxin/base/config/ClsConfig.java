package com.lvxin.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 CLS 配置类
 * 用于配置日志上传到腾讯云 CLS 服务的相关参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cls")
public class ClsConfig {

    /**
     * 是否启用 CLS 日志上传
     */
    private Boolean enabled = false;

    /**
     * 域名信息，请参见：https://cloud.tencent.com/document/product/614/18940#.E5.9F.9F.E5.90.8D
     * API 上传日志 Tab 中的域名，例如：ap-beijing.cls.tencentcs.com
     */
    private String endpoint;

    /**
     * 云 API 密钥 ID
     * 建议通过环境变量 TENCENTCLOUD_SECRET_ID 注入
     */
    private String secretId;

    /**
     * 云 API 密钥 Key
     * 建议通过环境变量 TENCENTCLOUD_SECRET_KEY 注入
     */
    private String secretKey;

    /**
     * 日志主题 ID
     */
    private String topicId;

    /**
     * 批量上传大小（默认 100）
     */
    private Integer batchSize = 100;

    /**
     * 日志保留时间（秒），默认 0 表示立即上传
     */
    private Integer lingerMs = 2000;

    /**
     * 重试次数（默认 3）
     */
    private Integer maxRetries = 3;
}

