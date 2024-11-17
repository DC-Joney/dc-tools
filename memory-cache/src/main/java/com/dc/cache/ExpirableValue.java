package com.dc.cache;


/**
 * Cache value is expired type
 *
 * @apiNote Fork from redission
 */
public interface ExpirableValue {

    /**
     * 判断 缓存的值是否已经过期
     *
     * @return boolean
     */
    boolean isExpired();

}
