package com.lvxin.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cos")
public class CosConfig {

    /**
     * 密钥Id
     */
    private String secretId;

    /**
     * 密钥Key
     */
    private String secretKey;

    /**
     * 默认桶名
     */
    private String bucket;

    /**
     * 区域，如 ap-shanghai
     */
    private String region;

    /**
     * 访问域名（可选，若配置则优先使用）
     * 例如：cdn.example.com 或 bucket.cos.ap-shanghai.myqcloud.com
     */
    private String domain;
}


