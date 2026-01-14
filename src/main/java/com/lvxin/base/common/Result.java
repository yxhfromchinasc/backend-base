package com.lvxin.base.common;

import com.lvxin.base.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 统一返回结果
 */
@Setter
@Getter
public class Result<T> {

    private boolean success;
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.setSuccess(true);
        r.setCode(ErrorCode.SUCCESS.code());
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setSuccess(true);
        r.setCode(ErrorCode.SUCCESS.code());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(Integer code, String message, T data) {
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.setSuccess(false);
        r.setCode(ErrorCode.INTERNAL_ERROR.code());
        r.setMessage(message);
        return r;
    }

}
