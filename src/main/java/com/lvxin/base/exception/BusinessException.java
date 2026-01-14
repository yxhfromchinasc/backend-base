package com.lvxin.base.exception;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.INTERNAL_ERROR.code();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
