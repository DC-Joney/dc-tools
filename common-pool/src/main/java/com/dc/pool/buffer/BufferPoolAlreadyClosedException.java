package com.dc.pool.buffer;

/**
 * When buffer pool already close. throw current exception
 *
 * @author zhangyang
 */
public class BufferPoolAlreadyClosedException extends BufferPoolException {

    public BufferPoolAlreadyClosedException(String message) {
        super(message);
    }

    public BufferPoolAlreadyClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BufferPoolAlreadyClosedException(Throwable cause) {
        super(cause);
    }
}
