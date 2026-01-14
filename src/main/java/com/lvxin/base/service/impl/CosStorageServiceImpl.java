package com.lvxin.base.service.impl;


import com.lvxin.base.config.CosConfig;
import com.lvxin.base.service.ObjectStorageService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
 
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Objects;

@Slf4j
@Service
public class CosStorageServiceImpl implements ObjectStorageService {

    @Resource
    private CosConfig cosConfig;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        COSCredentials cred = new BasicCOSCredentials(cosConfig.getSecretId(), cosConfig.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosConfig.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        this.cosClient = new COSClient(cred, clientConfig);
    }

    private String getBucket(String bucketName) {
        return (bucketName == null || bucketName.isBlank()) ? cosConfig.getBucket() : bucketName;
    }

    private String normalizeKey(String objectKey) {
        String key = Objects.requireNonNull(objectKey, "objectKey cannot be null").replace("\\", "/");
        while (key.startsWith("/")) key = key.substring(1);
        return key;
    }

    private String buildUrl(String bucketName, String objectKey) {
        String key = normalizeKey(objectKey);
        if (cosConfig.getDomain() != null && !cosConfig.getDomain().isBlank()) {
            String domain = cosConfig.getDomain();
            String schema = domain.startsWith("http") ? "" : "https://";
            String sep = domain.endsWith("/") ? "" : "/";
            return schema + domain + sep + key;
        }
        String bucket = getBucket(bucketName);
        return "https://" + bucket + ".cos." + cosConfig.getRegion() + ".myqcloud.com/" + key;
    }

    @Override
    public String uploadFile(MultipartFile file, String bucketName, String objectKey) {
        Objects.requireNonNull(file, "file cannot be null");
        Objects.requireNonNull(objectKey, "objectKey cannot be null");
        try {
            String bucket = getBucket(bucketName);
            String key = normalizeKey(objectKey);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest request = new PutObjectRequest(bucket, key, new ByteArrayInputStream(file.getBytes()), metadata);
            request.setCannedAcl(CannedAccessControlList.PublicRead);
            cosClient.putObject(request);

            String url = buildUrl(bucket, key);
            log.info("COS 上传成功: bucket={}, key={}, url={}", bucket, key, url);
            return url;
        } catch (Exception e) {
            log.error("上传文件失败, objectKey={}", objectKey, e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String objectKey) {
        return uploadFile(file, cosConfig.getBucket(), objectKey);
    }

    @Override
    public boolean deleteFile(String bucketName, String objectKey) {
        try {
            String bucket = getBucket(bucketName);
            String key = normalizeKey(objectKey);
            cosClient.deleteObject(new DeleteObjectRequest(bucket, key));
            return true;
        } catch (Exception ex) {
            log.warn("COS 删除失败, key={}", objectKey, ex);
        return false;
        }
    }

    @Override
    public boolean deleteFile(String objectKey) {
        return deleteFile(cosConfig.getBucket(), objectKey);
    }

    @Override
    public boolean fileExists(String bucketName, String objectKey) {
        try {
            String bucket = getBucket(bucketName);
            String key = normalizeKey(objectKey);
            return cosClient.doesObjectExist(bucket, key);
        } catch (Exception e) {
            log.warn("COS 查询是否存在失败, key={}", objectKey, e);
        return false;
        }
    }

    @Override
    public boolean fileExists(String objectKey) {
        return fileExists(cosConfig.getBucket(), objectKey);
    }

    @Override
    public String getFileUrl(String bucketName, String objectKey) {
        return buildUrl(bucketName, objectKey);
    }

    @Override
    public String getFileUrl(String objectKey) {
        return getFileUrl(cosConfig.getBucket(), objectKey);
    }
}
