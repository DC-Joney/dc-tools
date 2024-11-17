package com.dc.tools.io.buffer;

import com.dc.tools.io.buffer.unit.DataSize;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Slf4j
@UtilityClass
public class ByteBufferUtil {

    /**
     * 判断是否小于某个尺寸
     */
    public boolean smallSize(ByteBuffer buffer, DataSize dataSize) {
        DataSize large = DataSize.ofBytes(Long.MAX_VALUE);
        return betweenSize(buffer, dataSize, large);
    }


    public boolean largeSize(ByteBuffer buffer, DataSize dataSize) {
        DataSize small = DataSize.ofBytes(0L);
        return betweenSize(buffer, small, dataSize);
    }

    public boolean betweenSize(ByteBuffer buffer, DataSize small, DataSize large) {
        long readBytes = buffer.remaining();
        long smallLimit = small.toBytes();
        long largeLimit = large.toBytes();
        return readBytes >= smallLimit && readBytes <= largeLimit;
    }


}
