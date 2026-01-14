package com.lvxin.base.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 对象存储服务接口
 * 定义对象存储的通用操作
 */
public interface ObjectStorageService {

    /**
     * 上传文件到对象存储
     *
     * @param file       要上传的文件
     * @param bucketName 存储桶名称
     * @param objectKey  对象键（文件路径）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String bucketName, String objectKey);

    /**
     * 上传文件到对象存储（使用默认存储桶）
     *
     * @param file      要上传的文件
     * @param objectKey 对象键（文件路径）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String objectKey);

    /**
     * 删除文件
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象键（文件路径）
     * @return 是否删除成功
     */
    boolean deleteFile(String bucketName, String objectKey);

    /**
     * 删除文件（使用默认存储桶）
     *
     * @param objectKey 对象键（文件路径）
     * @return 是否删除成功
     */
    boolean deleteFile(String objectKey);

    /**
     * 检查文件是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象键（文件路径）
     * @return 文件是否存在
     */
    boolean fileExists(String bucketName, String objectKey);

    /**
     * 检查文件是否存在（使用默认存储桶）
     *
     * @param objectKey 对象键（文件路径）
     * @return 文件是否存在
     */
    boolean fileExists(String objectKey);

    /**
     * 获取文件访问URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象键（文件路径）
     * @return 文件访问URL
     */
    String getFileUrl(String bucketName, String objectKey);

    /**
     * 获取文件访问URL（使用默认存储桶）
     *
     * @param objectKey 对象键（文件路径）
     * @return 文件访问URL
     */
    String getFileUrl(String objectKey);
}
