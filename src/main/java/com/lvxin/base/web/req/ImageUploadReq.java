package com.lvxin.base.web.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传请求
 */
@Data
public class ImageUploadReq {

    /**
     * 图片文件
     */
    @NotNull(message = "图片文件不能为空")
    private MultipartFile file;

    /**
     * 图片分类（可选）
     */
    private String category;
}
