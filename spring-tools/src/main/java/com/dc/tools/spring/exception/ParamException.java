package com.dc.tools.spring.exception;

import cn.hutool.core.util.StrUtil;

/**
 * 参数异常
 *
 * @author huangzikuan
 * @date 2020/02/27 19:37:28
 */
public class ParamException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ParamException(String message) {
        super(message);
    }

    public ParamException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public ParamException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }

    public static void assertIsTrue(boolean b, String errorMsg) {
        if (!b) {
            throw new ParamException(errorMsg);
        }
    }
}
