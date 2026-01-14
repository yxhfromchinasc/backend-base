package com.lvxin.base.exception;

/**
 * 错误码定义
 */
public enum ErrorCode {
    // 通用
    SUCCESS(200, "OK"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务
    USER_NOT_EXISTS(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PHONE_ALREADY_EXISTS(1003, "手机号已被使用"),
    SMS_CODE_INVALID(1004, "验证码无效"),
    SMS_CODE_EXPIRED(1005, "验证码已过期"),
    SMS_SEND_FAILED(1006, "验证码发送失败"),
    SMS_CODE_FREQUENTLY(1007, "上一条验证码在有效期");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer code() {
        return code;
    }

    public String message() {
        return message;
    }
}
