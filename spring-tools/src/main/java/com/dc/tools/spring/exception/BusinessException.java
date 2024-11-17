package com.dc.tools.spring.exception;

import cn.hutool.core.util.StrUtil;

/**
 * 业务异常
 *
 * @author huangzikuan
 * @date 2020/02/27 19:37:28
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;


    public BusinessException(String message) {
        super(message);
        this.code = BusinessResultStatus.BUSINESS_EXCEPTION.getCode();
    }

    public BusinessException(String message, Object... args) {
        super(StrUtil.format(message, args));
        this.code = BusinessResultStatus.BUSINESS_EXCEPTION.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultStatus resultStatus) {
        super(resultStatus.getMessage());
        this.code = resultStatus.getCode();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = BusinessResultStatus.BUSINESS_EXCEPTION.getCode();
    }


    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }


}
