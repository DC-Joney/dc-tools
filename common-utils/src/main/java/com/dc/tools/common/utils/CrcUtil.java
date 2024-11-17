package com.dc.tools.common.utils;

import java.nio.ByteBuffer;

/**
 * CRC utilities to compute CRC64 checksum.
 *
 * <p>Fork from Soft-JRaft</p>
 *
 * @author boyan(boyan@antfin.com)
 */
public final class CrcUtil {

    private static final ThreadLocal<CRC64> CRC_64_THREAD_LOCAL = ThreadLocal.withInitial(CRC64::new);

    /**
     * Compute CRC64 checksum for byte[].
     *
     * @param array source array
     * @return checksum value
     */
    public static long crc64(final byte[] array) {
        if (array == null) {
            return 0;
        }
        return crc64(array, 0, array.length);
    }

    /**
     * Compute CRC64 checksum for byte[].
     *
     * @param array  source array
     * @param offset starting position in the source array
     * @param length the number of array elements to be computed
     * @return checksum value
     */
    public static long crc64(final byte[] array, final int offset, final int length) {
        final CRC64 crc64 = CRC_64_THREAD_LOCAL.get();
        crc64.update(array, offset, length);
        final long ret = crc64.getValue();
        crc64.reset();
        return ret;
    }

    /**
     * Compute CRC64 checksum for {@code ByteBuffer}.
     *
     * @param buf source {@code ByteBuffer}
     * @return checksum value
     */
    public static long crc64(final ByteBuffer buf) {
        final int pos = buf.position();
        final int rem = buf.remaining();
        if (rem <= 0) {
            return 0;
        }
        // Currently we have not used DirectByteBuffer yet.
        if (buf.hasArray()) {
            return crc64(buf.array(), pos + buf.arrayOffset(), rem);
        }
        final byte[] b = new byte[rem];
        BufferUtils.mark(buf);
        buf.get(b);
        BufferUtils.reset(buf);
        return crc64(b);
    }

    private CrcUtil() {
    }
}