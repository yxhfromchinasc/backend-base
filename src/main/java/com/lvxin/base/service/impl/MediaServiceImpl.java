package com.lvxin.base.service.impl;

import com.lvxin.base.service.MediaService;
import com.lvxin.base.service.ObjectStorageService;
import com.lvxin.base.service.SystemConfigService;
import com.lvxin.base.util.FileValidationUtil;
import com.lvxin.base.util.HttpUtil;
import com.lvxin.base.web.req.ImageUploadReq;
import com.lvxin.base.web.resp.ImageUploadResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
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

    @Override
    public String downloadAndUploadImage(String imageUrl, String category) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            log.warn("图片URL为空，无法下载上传");
            return null;
        }

        try {
            log.info("开始从URL下载图片并上传: {}", imageUrl);

            // 1. 下载图片数据
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0");
            headers.put("Referer", "https://www.xiaohongshu.com/");
            
            byte[] imageData = HttpUtil.downloadFile(imageUrl, headers, 30);
            if (imageData == null || imageData.length == 0) {
                log.warn("下载图片失败，URL: {}", imageUrl);
                return null;
            }

            log.debug("图片下载成功，大小: {} bytes", imageData.length);

            // 2. 确定文件扩展名和MIME类型
            String extension = getImageExtensionFromUrl(imageUrl);
            String mimeType = getMimeTypeFromExtension(extension);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 3. 创建MultipartFile对象
            MultipartFile multipartFile = new ByteArrayMultipartFile(
                    imageData,
                    filename,
                    mimeType
            );

            // 4. 上传到自己的云存储
            String uploadedUrl = uploadImage(multipartFile, category);
            
            if (uploadedUrl != null) {
                log.info("图片下载并上传成功，原URL: {}, 新URL: {}", imageUrl, uploadedUrl);
            } else {
                log.warn("图片上传失败，原URL: {}", imageUrl);
            }

            return uploadedUrl;
        } catch (Exception e) {
            log.error("下载并上传图片失败，URL: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 从URL中提取图片扩展名
     */
    private String getImageExtensionFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return ".jpg"; // 默认扩展名
        }

        // 移除查询参数和锚点
        String path = url.split("[?#]")[0];
        
        // 检查常见图片扩展名
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return ".jpg";
        } else if (lowerPath.endsWith(".png")) {
            return ".png";
        } else if (lowerPath.endsWith(".gif")) {
            return ".gif";
        } else if (lowerPath.endsWith(".webp")) {
            return ".webp";
        } else if (lowerPath.endsWith(".bmp")) {
            return ".bmp";
        } else if (lowerPath.endsWith(".svg")) {
            return ".svg";
        }

        // 默认返回.jpg
        return ".jpg";
    }

    /**
     * 根据扩展名获取MIME类型
     */
    private String getMimeTypeFromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "image/jpeg";
        }

        String lowerExt = extension.toLowerCase();
        switch (lowerExt) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".bmp":
                return "image/bmp";
            case ".svg":
                return "image/svg+xml";
            default:
                return "image/jpeg";
        }
    }

    /**
     * 简单的MultipartFile实现，用于从字节数组创建MultipartFile
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String name, String contentType) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content != null ? content : new byte[0];
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content != null ? content : new byte[0]);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            throw new UnsupportedOperationException("transferTo not supported");
        }
    }
}
