package com.dc.tools.io.buffer;

import cn.hutool.core.util.StrUtil;

public class ReadFileException extends RuntimeException {

    public ReadFileException(String message) {
        super(message);
    }

    public ReadFileException(String message, Throwable cause, Object... args) {
        super(StrUtil.format(message, args), cause);
    }
}
