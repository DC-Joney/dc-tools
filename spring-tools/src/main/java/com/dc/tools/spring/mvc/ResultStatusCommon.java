package com.dc.tools.spring.mvc;

/**
 * 响应状态码
 *
 * @author zhangyang
 */
public enum ResultStatusCommon implements ResultStatus {

    /**
     * 正常返回
     */
    OK(0, "ok"),

    /**
     * 业务异常
     */
    BUSINESS_EXCEPTION(1, "业务异常"),

    /**
     * 参数异常
     */
    PARAM_EXCEPTION(2, "参数异常"),

    /**
     * 非法访问
     */
    ILLEGAL_VISIT(9, "非法访问"),

    /**
     * 服务异常
     */
    SERVER_EXCEPTION(10, "服务异常"),

    /**
     * 运行异常
     */
    RUN_TIME_EXCEPTION(11, "运行异常");

    private final int code;

    private final String message;

    ResultStatusCommon(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}
