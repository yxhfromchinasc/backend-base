package com.lvxin.base.exception;

import com.lvxin.base.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器（业务异常 + 常见请求错误 + 兜底）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        // 401 未授权错误使用 WARN 级别，其他业务异常使用 ERROR 级别
        if (ex.getCode() != null && ex.getCode().equals(ErrorCode.UNAUTHORIZED.code())) {
            log.warn("business_exception code={} msg={}", ex.getCode(), ex.getMessage());
        } else {
            log.error("business_exception code={} msg={}", ex.getCode(), ex.getMessage());
        }
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleBadRequest(Exception ex) {
        log.error("bad_request ex", ex);
        return Result.error(ErrorCode.BAD_REQUEST.code(), ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("method_not_allowed: {} {}", ex.getMethod(), ex.getMessage());
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED.code(), ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("no_resource_found: {} {}", ex.getResourcePath(), ex.getMessage());
        return Result.error(404, "资源不存在");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        log.error("unsupported_media_type ex", ex);
        return Result.error(ErrorCode.UNSUPPORTED_MEDIA_TYPE.code(), ex.getMessage());
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.error("文件上传大小超限异常", ex);
        
        // 提取最大允许大小信息
        String errorMessage = ex.getMessage();
        String friendlyMessage = "文件上传失败：文件大小超过系统限制";
        
        // 尝试从异常信息中提取大小信息
        if (errorMessage != null) {
            // 查找字节数信息
            if (errorMessage.contains("1048576")) {
                friendlyMessage = "文件上传失败：文件大小超过 1MB 限制，请压缩文件后重新上传";
            } else if (errorMessage.contains("bytes")) {
                // 尝试提取字节数并转换为MB
                try {
                    String[] parts = errorMessage.split(" ");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].matches("\\d+") && i + 1 < parts.length && parts[i + 1].equals("bytes")) {
                            long bytes = Long.parseLong(parts[i]);
                            long mb = bytes / 1024 / 1024;
                            friendlyMessage = String.format("文件上传失败：文件大小超过 %dMB 限制，请压缩文件后重新上传", mb);
                            break;
                        }
                    }
                } catch (Exception e) {
                    // 解析失败，使用默认消息
                }
            }
        }
        
        return Result.error(ErrorCode.BAD_REQUEST.code(), friendlyMessage);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOtherExceptions(Exception ex) {
        log.error("internal_error ex", ex);
        return Result.error(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message());
    }
}
