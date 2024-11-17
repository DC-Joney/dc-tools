package com.dc.tools.trace;

/**
 * @author zhangyang
 */
public class AsyncRunnable implements Runnable, AsyncTrace {

    private final Runnable delegate;

    private final String requestId;

     AsyncRunnable(Runnable runnable) {
        this.delegate = runnable;
        this.requestId = TraceUtils.dumpTrace();
    }

    @Override
    public void run() {
        try {
            TraceUtils.intoTrace(this);
            delegate.run();
        } finally {
            TraceUtils.remove();
        }
    }

    @Override
    public String requestId() {
        return requestId;
    }

    public static Runnable async(Runnable runnable) {
        return new AsyncRunnable(runnable);
    }
}
