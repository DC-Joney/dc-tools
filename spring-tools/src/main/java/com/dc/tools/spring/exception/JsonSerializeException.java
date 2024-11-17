package com.dc.tools.spring.exception;

/**
 * Json 序列化异常
 *
 * @author zy
 */
public class JsonSerializeException extends RuntimeException{

    public JsonSerializeException(String message) {
        super(message);
    }

    public JsonSerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonSerializeException(Throwable cause) {
        super(cause);
    }

    protected JsonSerializeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
