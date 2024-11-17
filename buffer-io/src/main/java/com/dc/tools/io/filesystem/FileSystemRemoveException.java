package com.dc.tools.io.filesystem;

import cn.hutool.core.util.StrUtil;

public class FileSystemRemoveException extends RuntimeException {

    public FileSystemRemoveException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public FileSystemRemoveException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }
}
