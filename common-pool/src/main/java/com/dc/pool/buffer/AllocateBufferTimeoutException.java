package com.dc.pool.buffer;

import cn.hutool.core.util.StrUtil;

/**
 * <p>Allocate buffer exception </p>
 * <p>
 * 当从{@link BufferPool}中获取内存超时的情况下，会抛出此异常
 *
 * @author zhangyang
 */
public class AllocateBufferTimeoutException extends BufferPoolException {

    public AllocateBufferTimeoutException(String message) {
        super(message);
    }

    public AllocateBufferTimeoutException(String message, Object...args) {
        super(StrUtil.format(message, args));
    }

    public AllocateBufferTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public AllocateBufferTimeoutException(Throwable cause) {
        super(cause);
    }
}