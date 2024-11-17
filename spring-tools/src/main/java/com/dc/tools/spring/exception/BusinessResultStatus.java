package com.dc.tools.spring.exception;

/**
 * 业务异常返回状态码
 * @author zhangyang
 */
public enum BusinessResultStatus implements ResultStatus{
    /**
     * 业务异常
     */
    BUSINESS_EXCEPTION() {
        @Override
        public int getCode() {
            return -1;
        }

        @Override
        public String getMessage() {
            return "business exception";
        }
    };


    @Override
    public abstract int getCode();

    @Override
    public abstract String getMessage();
}
