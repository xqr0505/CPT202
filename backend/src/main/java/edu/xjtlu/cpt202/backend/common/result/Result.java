package edu.xjtlu.cpt202.backend.common.result;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper.
 * @author QiranXiao
 * @date 2026/3/26
 */
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public Result() {
        this.timestamp = LocalDateTime.now();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> Result<T> success() {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(ResultCodeEnum resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
