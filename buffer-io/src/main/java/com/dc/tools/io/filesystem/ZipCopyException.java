package com.dc.tools.io.filesystem;

import cn.hutool.core.util.StrUtil;

public class ZipCopyException extends RuntimeException {

    public ZipCopyException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public ZipCopyException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }
}
