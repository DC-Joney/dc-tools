package com.dc.tools.spring.mvc;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * 返回结果类
 */
@Data
@Accessors(chain = true)
public class Result<T> {

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误简短信息
     */
    private String message;

    /**
     * 正常返回的数据
     */
    private T data;

    /**
     * 展现给用户的消息提醒
     */
    private String dialog;

    /**
     * 是否正常返回
     */
    private Boolean success;

    /**
     * 追踪日志 id
     */
    private String requestId;

    /**
     * 追踪日志 id
     */
    private String globalId;

    public Result() {

    }

    public Result(Integer code, String message, T data, String dialog, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.dialog = dialog;
        this.success = success;
    }


    public boolean isEmpty() {
		return data == null;
	}


    public static <T> Result<T> success() {
        return new Result<T>(
                ResultStatusCommon.OK.getCode(),
                ResultStatusCommon.OK.getMessage(),
                null,
                ResultStatusCommon.OK.getMessage(),
                true);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultStatusCommon.OK.getCode(),
                ResultStatusCommon.OK.getMessage(),
                data,
                ResultStatusCommon.OK.getMessage(),
                true);
    }

    public static <T> Result<T> failure(ResultStatus resultStatus) {
        return new Result<>(
                resultStatus.getCode(),
                resultStatus.getMessage(),
                null,
                resultStatus.getMessage(),
                false);
    }

    public static <T> Result<T> failure(ResultStatus resultStatus, String message, T data) {
        return new Result<>(
                resultStatus.getCode(),
                message,
                data,
                message,
                false);
    }

    public static <T> Result<T> failure(ResultStatus resultStatus, String message, T data, String dialog) {
        return new Result<>(
                resultStatus.getCode(),
                message,
                data,
                dialog,
                false);
    }

    public static <T> Result<T> failure(Integer code, String message, T data) {
        return new Result<>(
                code,
                message,
                data,
                message,
                false);
    }

    public static <T> Result<T> failure(Integer code, String message, String dialog) {
        return new Result<>(
                code,
                message,
                null,
                dialog,
                false);
    }

    public static <T> Result<T> failure(Integer code, String message, T data, String dialog) {
        return new Result<>(
                code,
                message,
                data,
                dialog,
                false);
    }

    public static <T> Result<T> failure(ResultStatus resultStatus, String dialog) {
        return new Result<>(
                resultStatus.getCode(),
                resultStatus.getMessage(),
                null,
                dialog,
                false);
    }

    public static <T> Result<T> failure(ResultStatus resultStatus, T data) {
        return new Result<>(
                resultStatus.getCode(),
                resultStatus.getMessage(),
                data,
                resultStatus.getMessage(),
                false);
    }

    public boolean isSuccess() {
        return Objects.equals(ResultStatusCommon.OK.getCode(), getCode());
    }

    @Override
    public String toString() {
        return "Result [code=" + code + ", message=" + message + ", data=" + data + ", dialog=" + dialog + ", success="
                + success + "]";
    }

}