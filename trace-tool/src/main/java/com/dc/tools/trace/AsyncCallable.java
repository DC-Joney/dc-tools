package com.dc.tools.trace;

import java.util.concurrent.Callable;

/**
 * @author zhangyang
 * @param <V>
 */
public class AsyncCallable<V> implements Callable<V>, AsyncTrace {

    private final Callable<V> delegate;

    private final String requestId;

    AsyncCallable(Callable<V> callable) {
        this.delegate = callable;
        this.requestId = TraceUtils.dumpTrace();
    }


    @Override
    public V call() throws Exception {
        try {
            TraceUtils.intoTrace(this);
            return delegate.call();
        } finally {
            TraceUtils.remove();
        }
    }

    @Override
    public String requestId() {
        return requestId;
    }

    public static <V> AsyncCallable<V> async(Callable<V> callable) {
        return new AsyncCallable<>(callable);
    }
}
