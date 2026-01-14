package com.lvxin.base.util;

import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 文件验证工具类
 * 提供文件验证的通用方法
 */
public class FileValidationUtil {

    /**
     * 支持的图片格式
     */
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 默认最大文件大小（5MB）
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 最大文件名长度
     */
    private static final int MAX_FILENAME_LENGTH = 255;

    /**
     * 验证图片文件（使用默认最大文件大小）
     *
     * @param file 图片文件
     * @throws BusinessException 验证失败时抛出异常
     */
    public static void validateImageFile(MultipartFile file) {
        validateImageFile(file, DEFAULT_MAX_FILE_SIZE);
    }

    /**
     * 验证图片文件（指定最大文件大小）
     *
     * @param file 图片文件
     * @param maxFileSize 最大文件大小（字节）
     * @throws BusinessException 验证失败时抛出异常
     */
    public static void validateImageFile(MultipartFile file, long maxFileSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            long maxFileSizeMB = maxFileSize / 1024 / 1024;
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    String.format("图片文件不能超过%dMB",
                             maxFileSizeMB));
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不支持的图片格式，仅支持 JPG、PNG、GIF、WebP 格式");
        }

        // 检查文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名不能为空");
        }

        // 检查文件名长度
        if (originalFilename.length() > MAX_FILENAME_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名过长");
        }
    }

    /**
     * 验证文件大小
     *
     * @param file    文件
     * @param maxSize 最大大小（字节）
     * @throws BusinessException 验证失败时抛出异常
     */
    public static void validateFileSize(MultipartFile file, long maxSize) {
        if (file.getSize() > maxSize) {
            long fileSizeMB = file.getSize() / 1024 / 1024;
            long maxSizeMB = maxSize / 1024 / 1024;
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "未知文件";
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    String.format("文件过大：当前文件大小 %dMB，超过允许的最大大小 %dMB。文件名：%s。请压缩文件后重新上传。", 
                            fileSizeMB, maxSizeMB, fileName));
        }
    }

    /**
     * 验证文件类型
     *
     * @param file         文件
     * @param allowedTypes 允许的文件类型列表
     * @throws BusinessException 验证失败时抛出异常
     */
    public static void validateFileType(MultipartFile file, List<String> allowedTypes) {
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不支持的文件格式");
        }
    }

    /**
     * 验证文件名
     *
     * @param file 文件
     * @throws BusinessException 验证失败时抛出异常
     */
    public static void validateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空");
        }

        if (originalFilename.length() > MAX_FILENAME_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名过长");
        }
    }
}
