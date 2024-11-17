package com.dc.tools.io.filesystem;

import cn.hutool.core.util.StrUtil;

public class ParallelZipException extends RuntimeException {

    public ParallelZipException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public ParallelZipException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }
}
