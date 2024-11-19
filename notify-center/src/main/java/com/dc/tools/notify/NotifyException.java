package com.dc.tools.notify;

import cn.hutool.core.util.StrUtil;

public class NotifyException extends RuntimeException{

    public NotifyException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public NotifyException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }
}
