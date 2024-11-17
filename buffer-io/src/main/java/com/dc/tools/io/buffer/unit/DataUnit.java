/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc.tools.io.buffer.unit;

import java.util.Objects;

/**
 * A standard set of data size units.
 *
 * @author zy
 */
public enum DataUnit {

    /**
     * Bytes.
     */
    BYTES("B") {
        @Override
        long toBytes(long unitSize) {
            return toBytes(unitSize, 0);
        }

        @Override
        long fromBytes(long bytes) {
            return fromBytes(bytes, 0);
        }

        @Override
        long size() {
            return DATA_UNIT_SIZE[0];
        }
    },

    /**
     * Kilobytes.
     */
    KILOBYTES("KB") {
        @Override
        long toBytes(long unitSize) {
            return toBytes(unitSize, 1);
        }

        @Override
        long fromBytes(long bytes) {
            return fromBytes(bytes, 1);
        }

        @Override
        long size() {
            return DATA_UNIT_SIZE[1];
        }
    },

    /**
     * Megabytes.
     */
    MEGABYTES("MB") {
        @Override
        long toBytes(long unitSize) {
            return toBytes(unitSize, 2);
        }

        @Override
        long fromBytes(long bytes) {
            return fromBytes(bytes, 2);
        }

        @Override
        long size() {
            return DATA_UNIT_SIZE[2];
        }
    },

    /**
     * Gigabytes.
     */
    GIGABYTES("GB") {
        @Override
        long toBytes(long unitSize) {
            return toBytes(unitSize, 3);
        }

        @Override
        long fromBytes(long bytes) {
            return fromBytes(bytes, 3);
        }

        @Override
        long size() {
            return DATA_UNIT_SIZE[3];
        }
    },

    /**
     * Terabytes.
     */
    TERABYTES("TB") {
        @Override
        long toBytes(long unitSize) {
            return toBytes(unitSize, 4);
        }

        @Override
        long fromBytes(long bytes) {
            return fromBytes(bytes, 4);
        }

        @Override
        long size() {
            return DATA_UNIT_SIZE[4];
        }
    };


    private static final int[] SHIFT = new int[]{0, 10, 20, 30, 40};

    static final long[] DATA_UNIT_SIZE = new long[]{
            1L << SHIFT[0],
            1L << SHIFT[1],
            1L << SHIFT[2],
            1L << SHIFT[3],
            1L << SHIFT[4],
    };


    protected static long toBytes(long unitSize, int index) {
        return (unitSize << SHIFT[index]);
    }

    protected static long fromBytes(long bytes, int index) {
        return (bytes >>> SHIFT[index]) + ((bytes & (DATA_UNIT_SIZE[index] - 1)) > (DATA_UNIT_SIZE[index] >>> 1) ? 1 : 0);
    }

    private final String suffix;


    DataUnit(String suffix) {
        this.suffix = suffix;
    }

    abstract long size();


    abstract long toBytes(long unitSize);


    abstract long fromBytes(long bytes);

    /**
     * Return the {@link DataUnit} matching the specified {@code suffix}.
     *
     * @param suffix one of the standard suffix
     * @return the {@link DataUnit} matching the specified {@code suffix}
     * @throws IllegalArgumentException if the suffix does not match any
     *                                  of this enum's constants
     */
    public static DataUnit fromSuffix(String suffix) {
        for (DataUnit candidate : values()) {
            if (Objects.equals(candidate.suffix, suffix)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
    }


}
