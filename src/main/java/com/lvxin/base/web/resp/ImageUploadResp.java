package com.lvxin.base.web.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 图片上传响应
 */
@Data
@Builder
public class ImageUploadResp {

    /**
     * 图片URL
     */
    private String url;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 上传时间（时间戳）
     */
    private Long uploadTime;
}
