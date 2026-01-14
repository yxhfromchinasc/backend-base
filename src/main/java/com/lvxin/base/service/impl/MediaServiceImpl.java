package com.lvxin.base.service.impl;

import com.lvxin.base.service.MediaService;
import com.lvxin.base.service.ObjectStorageService;
import com.lvxin.base.service.SystemConfigService;
import com.lvxin.base.util.FileValidationUtil;
import com.lvxin.base.web.req.ImageUploadReq;
import com.lvxin.base.web.resp.ImageUploadResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 多媒体服务实现类
 */
@Slf4j
@Service
public class MediaServiceImpl implements MediaService {

    @Resource
    private ObjectStorageService objectStorageService;

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 默认最大文件大小（5MB）
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Override
    public ImageUploadResp uploadImage(ImageUploadReq req) {
        log.info("开始上传图片，文件名: {}, 大小: {} bytes",
                req.getFile().getOriginalFilename(), req.getFile().getSize());

        // 从系统配置获取最大文件大小（单位：MB），如果未配置则使用默认值5MB
        long maxFileSize = getMaxFileSizeFromConfig();

        // 验证文件
        FileValidationUtil.validateImageFile(req.getFile(), maxFileSize);

        // 生成文件路径
        String objectKey = generateImagePath(req.getFile(), req.getCategory());

        // 上传到对象存储
        String url = objectStorageService.uploadFile(req.getFile(), objectKey);

        // 构建响应
        ImageUploadResp resp = ImageUploadResp.builder()
                .url(url)
                .fileName(req.getFile().getOriginalFilename())
                .fileSize(req.getFile().getSize())
                .category(req.getCategory())
                .uploadTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .build();

        log.info("图片上传成功，URL: {}", url);
        return resp;
    }

    @Override
    public String uploadImage(MultipartFile file, String category) {
        log.info("开始上传图片，文件名: {}, 分类: {}", file.getOriginalFilename(), category);

        // 从系统配置获取最大文件大小（单位：MB），如果未配置则使用默认值5MB
        long maxFileSize = getMaxFileSizeFromConfig();

        // 验证文件
        FileValidationUtil.validateImageFile(file, maxFileSize);

        // 生成文件路径
        String objectKey = generateImagePath(file, category);

        // 上传到对象存储
        String url = objectStorageService.uploadFile(file, objectKey);

        log.info("图片上传成功，URL: {}", url);
        return url;
    }

    @Override
    public boolean deleteImage(String imageUrl) {
        log.info("开始删除图片，URL: {}", imageUrl);

        try {
            // 从URL中提取对象键
            String objectKey = extractObjectKeyFromUrl(imageUrl);
            if (objectKey == null) {
                log.warn("无法从URL中提取对象键: {}", imageUrl);
                return false;
            }

            boolean result = objectStorageService.deleteFile(objectKey);
            log.info("图片删除结果: {}, URL: {}", result, imageUrl);
            return result;

        } catch (Exception e) {
            log.error("删除图片异常，URL: {}", imageUrl, e);
            return false;
        }
    }

    @Override
    public boolean imageExists(String imageUrl) {
        log.info("检查图片是否存在，URL: {}", imageUrl);

        try {
            // 从URL中提取对象键
            String objectKey = extractObjectKeyFromUrl(imageUrl);
            if (objectKey == null) {
                log.warn("无法从URL中提取对象键: {}", imageUrl);
                return false;
            }

            boolean exists = objectStorageService.fileExists(objectKey);
            log.info("图片存在性检查结果: {}, URL: {}", exists, imageUrl);
            return exists;

        } catch (Exception e) {
            log.error("检查图片存在性异常，URL: {}", imageUrl, e);
            return false;
        }
    }

    /**
     * 生成图片存储路径
     *
     * @param file     图片文件
     * @param category 图片分类
     * @return 存储路径
     */
    private String generateImagePath(MultipartFile file, String category) {
        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成唯一文件名
        String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 构建路径
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append("images/");

        if (category != null && !category.trim().isEmpty()) {
            pathBuilder.append(category).append("/");
        }

        // 按日期分目录
        LocalDateTime now = LocalDateTime.now();
        pathBuilder.append(now.getYear())
                .append("/")
                .append(String.format("%02d", now.getMonthValue()))
                .append("/")
                .append(String.format("%02d", now.getDayOfMonth()))
                .append("/");

        pathBuilder.append(uniqueFileName);

        return pathBuilder.toString();
    }

    /**
     * 从URL中提取对象键
     *
     * @param url 图片URL
     * @return 对象键
     */
    private String extractObjectKeyFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        try {
            // 这里需要根据实际的对象存储URL格式来解析
            // 假设URL格式为: https://domain.com/bucket/path/to/file.jpg
            // 需要提取 path/to/file.jpg 部分

            // 简单实现：从URL中提取路径部分
            // 实际使用时需要根据具体的对象存储服务商URL格式来调整
            String[] parts = url.split("/");
            if (parts.length > 3) {
                StringBuilder objectKey = new StringBuilder();
                for (int i = 3; i < parts.length; i++) {
                    if (i > 3) {
                        objectKey.append("/");
                    }
                    objectKey.append(parts[i]);
                }
                return objectKey.toString();
            }

            return null;
        } catch (Exception e) {
            log.error("从URL提取对象键失败: {}", url, e);
            return null;
        }
    }

    /**
     * 从系统配置获取最大文件大小
     *
     * @return 最大文件大小（字节）
     */
    private long getMaxFileSizeFromConfig() {
        try {
            // 从系统配置读取最大文件大小（单位：MB）
            String maxFileSizeMB = systemConfigService.getConfigValue("max_file_size_mb");
            if (maxFileSizeMB != null && !maxFileSizeMB.trim().isEmpty()) {
                try {
                    long maxFileSizeMBValue = Long.parseLong(maxFileSizeMB.trim());
                    long maxFileSize = maxFileSizeMBValue * 1024 * 1024; // 转换为字节
                    log.debug("从系统配置读取最大文件大小: {}MB ({} bytes)", maxFileSizeMBValue, maxFileSize);
                    return maxFileSize;
                } catch (NumberFormatException e) {
                    log.warn("系统配置 max_file_size_mb 格式错误: {}, 使用默认值", maxFileSizeMB, e);
                }
            }
        } catch (Exception e) {
            log.warn("读取系统配置 max_file_size_mb 失败，使用默认值", e);
        }
        log.debug("使用默认最大文件大小: {}MB ({} bytes)", DEFAULT_MAX_FILE_SIZE / 1024 / 1024, DEFAULT_MAX_FILE_SIZE);
        return DEFAULT_MAX_FILE_SIZE;
    }
}
