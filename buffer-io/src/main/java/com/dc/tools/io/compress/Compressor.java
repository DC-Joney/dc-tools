package com.dc.tools.io.compress;

public interface Compressor {

    /**
     * 压缩数据
     * @param source 数据来源
     */
    byte[] compress(byte[] source);


    /**
     * 解压数据
     * @param source 数据来源
     */
    byte[] decompress(byte[] source);

}
