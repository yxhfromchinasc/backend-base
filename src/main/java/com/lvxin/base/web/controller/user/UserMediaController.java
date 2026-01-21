package com.lvxin.base.web.controller.user;

import com.lvxin.base.common.Result;
import com.lvxin.base.service.MediaService;
import com.lvxin.base.util.CurrentUserUtil;
import com.lvxin.base.web.req.ImageUploadReq;
import com.lvxin.base.web.resp.ImageUploadResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/media")
@Tag(name = "C端-多媒体", description = "C端用户图片上传接口")
public class UserMediaController {

    @Resource
    private MediaService mediaService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload/image")
    public Result<ImageUploadResp> uploadImage(@Valid @ModelAttribute ImageUploadReq req) {
        // C端用户上传需要登录
        Long userId = CurrentUserUtil.getCurrentUserId();
        if (userId == null) {
             throw new com.lvxin.base.exception.BusinessException(
                     com.lvxin.base.exception.ErrorCode.UNAUTHORIZED, "请先登录"
             );
        }
        
        // 可选：限制C端用户的上传目录或类型
        // 目前直接复用 MediaService 逻辑
        return Result.success(mediaService.uploadImage(req));
    }
}
