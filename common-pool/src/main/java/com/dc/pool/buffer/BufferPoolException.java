package com.dc.pool.buffer;

import cn.hutool.core.util.StrUtil;

/**
 * Get buffer from buffer fail, throw exception
 *
 * @author zhangyang
 */
public class BufferPoolException extends RuntimeException {

    public BufferPoolException(String message) {
        super(message);
    }

    public BufferPoolException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferPoolException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }

    public BufferPoolException(Throwable cause) {
        super(cause);
    }
}
