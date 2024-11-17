package com.dc.tools.spring.mvc;

/**
 * Feign 调用状态
 *
 * @author zy
 */
public enum FeignResultStatus implements ResultStatus {
    /**
     * FeignClient 调用超时
     */
    FEIGN_INVOKE_TIME_OUT(3, "FeignClient 调用超时"),

    /**
     * feign 调用异常
     */
    FEIGN_INVOKE_EXCEPTION(4, "FeignClient 调用异常");

    private final int code;

    private final String message;

    FeignResultStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }


}
