package com.dc.property.bytebuddy;

import cn.hutool.core.util.StrUtil;

/**
 * @author zy
 */
public class InterceptWrapException extends RuntimeException {

    public InterceptWrapException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public InterceptWrapException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }

    public InterceptWrapException(Throwable cause) {
        super(cause);
    }
}
