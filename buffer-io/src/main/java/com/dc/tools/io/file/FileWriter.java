package com.dc.tools.io.file;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * 用于下层文件数据写入
 *
 * @author zy
 */
public interface FileWriter extends Closeable {

    /**
     * 需要写入的数据
     * @param dataBuffer 写入的数据
     */
    void writeLine(ByteBuffer dataBuffer) throws Exception;


    /**
     * 获取文件的写入路径
     */
    Path filePath();
}
