package com.dc.tools.common;

import lombok.experimental.UtilityClass;

/**
 * 用于生成唯一性id
 *
 * @author zy
 */
@UtilityClass
public class SequenceUtil {

    private final IdGenerator idGenerator = new RandomIdGenerator();


    public String nextIdString() {
        return String.valueOf(idGenerator.nextId());
    }

    public long nextId() {
        return idGenerator.nextId();
    }
}
