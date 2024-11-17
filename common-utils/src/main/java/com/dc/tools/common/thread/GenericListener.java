package com.dc.tools.common.thread;

/**
 * Future listener for future done callback
 *
 * @author zhangyang
 */
public interface GenericListener<T> {

    /**
     * When future done call {@code onComplete} method
     * @param future future wrapper
     */
    void onComplete(FutureAdaptor<T> future);
}
