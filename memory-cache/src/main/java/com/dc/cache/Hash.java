package com.dc.cache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import jodd.util.Bits;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Generate sign hash for bytes
 *
 * @author Nikita Koksharov
 * @apiNote Fork from redission
 */
public class Hash {

    private static final long[] KEY = {0x9e3779b97f4a7c15L, 0xf39cc0605cedc834L, 0x1082276bf3a27251L, 0xf86c6a11d0c18e95L};

    private Hash() {
    }

    public static byte[] hash128toArray(ByteBuf objectState) {
        long[] hash = hash128(objectState);

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer((2 * Long.SIZE) / Byte.SIZE);
        try {
            buf.writeLong(hash[0]).writeLong(hash[1]);
            byte[] dst = new byte[buf.readableBytes()];
            buf.readBytes(dst);
            return dst;
        } finally {
            buf.release();
        }
    }

    public static long hash64(ByteBuf objectState) {
        HighwayHash h = calcHash(objectState);
        return h.finalize64();
    }

    public static long[] hash128(ByteBuf objectState) {
        HighwayHash h = calcHash(objectState);
        return h.finalize128();
    }

    protected static HighwayHash calcHash(ByteBuf objectState) {
        HighwayHash h = new HighwayHash(KEY);
        int i;
        int length = objectState.readableBytes();
        int offset = objectState.readerIndex();
        byte[] data = new byte[32];
        for (i = 0; i + 32 <= length; i += 32) {
            objectState.getBytes(offset + i, data);
            h.updatePacket(data, 0);
        }
        if ((length & 31) != 0) {
            data = new byte[length & 31];
            objectState.getBytes(offset + i, data);
            h.updateRemainder(data, 0, length & 31);
        }
        return h;
    }

    public static String hash128toBase64(ByteBuf objectState) {
        long[] hash = hash128(objectState);
        byte[] bytes = new byte[16];
        Bits.putLong(bytes,0, hash[0]);
        Bits.putLong(bytes,8, hash[1]);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return base64.substring(0, base64.length() - 2);
    }

}
