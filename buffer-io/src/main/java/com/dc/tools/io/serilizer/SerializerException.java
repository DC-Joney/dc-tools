package com.dc.tools.io.serilizer;

import cn.hutool.core.util.StrUtil;

/**
 * 序列化失败时抛出的异常
 *
 * @author zhangyang
 */
public class SerializerException extends RuntimeException {

    public SerializerException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public SerializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializerException(Throwable cause) {
        super(cause);
    }
}
