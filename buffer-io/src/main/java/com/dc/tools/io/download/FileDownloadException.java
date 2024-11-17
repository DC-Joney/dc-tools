package com.dc.tools.io.download;

/**
 * 文件下载异常
 * @author zhangyang
 */
public class FileDownloadException extends RuntimeException{

    public FileDownloadException(String message) {
        super(message);
    }

    public FileDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
