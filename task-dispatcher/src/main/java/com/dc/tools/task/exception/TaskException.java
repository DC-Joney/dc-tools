package com.dc.tools.task.exception;

import cn.hutool.core.util.StrUtil;

/**
 * 任务异常
 *
 * @author zy
 */
public class TaskException extends RuntimeException {

    public TaskException(String message, Object... args) {
        super(StrUtil.format(message, args));
    }

    public TaskException(Throwable cause, String message, Object... args) {
        super(StrUtil.format(message, args), cause);
    }

    public TaskException(Throwable cause) {
        super(cause);
    }
}
