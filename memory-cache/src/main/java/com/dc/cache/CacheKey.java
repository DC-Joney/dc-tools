package com.dc.cache;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Cache key for hashing
 *
 * @apiNote Change from redission
 */
@Slf4j
public class CacheKey implements Serializable {

    private static final long serialVersionUID = 5790732187795028243L;

    /**
     * 这里是用的hash 是为了减少hash冲突的概率，尽量使用hash来解决hash冲突
     * 如果hash一样才会使用 byteBuf来进行比较，如果byteBuf缓存的数据比较多会比较浪费性能
     */
    private final byte[] keyHash;

    /**
     * 添加store load 屏障，保证 byteBuf 可见性
     *
     * @apiNote 如果不添加store load 屏障，可能会导致在初始化CacheKey由于指令重排序而引起的 byteBuf不可见问题
     */
    @Getter
    private final ByteBuf byteBuf;

    /**
     * 这里对于数据在使用crc校验，但是在一些情况下还是会出现冲突的情况，这里是避免直接通过byteBuf来比较
     */
    private final long crc32;

    public CacheKey(ByteBuf byteBuf) {
        super();
        this.keyHash = Hash.hash128toArray(byteBuf);
        this.byteBuf = byteBuf;
        this.crc32 = initCrc32(byteBuf);

    }

    /**
     * 计算ByteBuf的CRC32位校验和
     *
     * @param byteBuf 缓存的key串
     */
    private long initCrc32(ByteBuf byteBuf) {
        CRC32 crc32 = new CRC32();
        ByteBuffer buffer = byteBuf.nioBuffer(0, byteBuf.writerIndex());
        crc32.update(buffer);
        return crc32.getValue();
    }

    public byte[] getKeyHash() {
        return keyHash;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(keyHash);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;


        if (getClass() != obj.getClass())
            return false;

        CacheKey other = (CacheKey) obj;

        //TODO: bug, 正常不会出现这样的问题，需要以后解决
        //在整体并发处理的过程中，会出现刚添加数据时正好这个数据被删除了就会导致byteBuf的refCnt = 0
        if (other.byteBuf.refCnt() == 0) {
            return false;
        }

        //优先使用hash比较
        if (!Arrays.equals(keyHash, other.keyHash))
            return false;

        if (!(crc32 == other.crc32))
            return false;



        if (byteBuf.refCnt() == 0) {
            log.error("other buf refCount is 0");
        }

        return byteBuf.equals(other.byteBuf);
    }

    /**
     * 将 bytes 转为 string是个比较耗时的过程，如果bytes较少可忽略不计，
     * 如果bytes较大不建议转为String 除非需要可视化
     */
    @Override
    public String toString() {
        return "CacheKey [keyHash=" + Arrays.toString(keyHash) + "]";
    }

    /**
     * 释放当前引用的byteBuf对象
     */
    public void release() {
        ReferenceCountUtil.release(byteBuf);
    }


    public static CacheKey toCacheKey(ByteBuf encodedKey) {
        return new CacheKey(encodedKey);
    }

}

