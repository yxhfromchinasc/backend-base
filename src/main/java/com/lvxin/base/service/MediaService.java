package com.lvxin.base.service;

import com.lvxin.base.web.req.ImageUploadReq;
import com.lvxin.base.web.resp.ImageUploadResp;
import org.springframework.web.multipart.MultipartFile;

/**
 * 多媒体服务接口
 * 处理图片、视频等多媒体文件的业务逻辑
 */
public interface MediaService {

    /**
     * 上传图片
     *
     * @param req 图片上传请求
     * @return 图片上传响应
     */
    ImageUploadResp uploadImage(ImageUploadReq req);

    /**
     * 上传图片（简化方法）
     *
     * @param file     图片文件
     * @param category 图片分类
     * @return 图片访问URL
     */
    String uploadImage(MultipartFile file, String category);

    /**
     * 删除图片
     *
     * @param imageUrl 图片URL
     * @return 是否删除成功
     */
    boolean deleteImage(String imageUrl);

    /**
     * 检查图片是否存在
     *
     * @param imageUrl 图片URL
     * @return 图片是否存在
     */
    boolean imageExists(String imageUrl);

    /**
     * 从URL下载图片并上传到自己的云存储
     *
     * @param imageUrl 图片URL
     * @param category 图片分类
     * @return 上传后的图片URL，失败返回null
     */
    String downloadAndUploadImage(String imageUrl, String category);
}
