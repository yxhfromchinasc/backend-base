package com.lvxin.base.web.controller.admin;

import com.lvxin.base.common.Result;
import com.lvxin.base.service.MediaService;
import com.lvxin.base.web.annotation.AuthCheck;
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
@RequestMapping("/admin/media")
@Tag(name = "员工端-多媒体", description = "员工端图片上传接口")
public class AdminMediaController {

    @Resource
    private MediaService mediaService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload/image")
    @AuthCheck
    public Result<ImageUploadResp> uploadImage(@Valid @ModelAttribute ImageUploadReq req) {
        return Result.success(mediaService.uploadImage(req));
    }
}


