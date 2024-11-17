package com.dc.tools.common.thread;



import com.dc.tools.common.collection.ConcurrentList;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Future 扩展，用于扩展Future 监听器实现
 * @author  zhangyang
 */
public class FutureAdaptor<T> extends FutureTask<T> {

    private static final int SUCCESS = 1;
    private static final int EXCEPTIONAL = -1;
    private static final int INTERRUPTED = -2;

    private volatile Throwable exception;

    private volatile T result;

    /**
     * 标志future 是否已经执行完成
     */
    private volatile boolean done;

    private final ConcurrentList<GenericListener<T>> listeners = new ConcurrentList<>();

    private volatile int state = 0;

    public FutureAdaptor(Callable<T> callable) {
        super(callable);
    }

    public FutureAdaptor(Runnable runnable, T result) {
        super(runnable, result);
    }

    public boolean isSuccess() {
        return !isCancelled() && exception == null && state > 0;
    }

    /**
     * 添加Future回调监听器，当Future执行完成后 会回调监听器，这里没有开心的线程去执行，而是用线程池的线程执行
     * @param listener 回调监听器
     */
    public FutureAdaptor<T> addListener(GenericListener<T> listener){
        listeners.add(listener);
        //如果当前Future已经执行结束了则直接执行当前Listener
        if (isDone()) {
            listener.onComplete(this);
        }
        return this;
    }

    public Throwable getCause() {
        return exception;
    }

    public T getResult() {
        return result;
    }

    /**
     * Future 内部的Done方法不是很符合当前要求
     */
    @Override
    public boolean isDone(){
        return this.done;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancel = super.cancel(mayInterruptIfRunning);
        if (cancel) {
            state = INTERRUPTED;
        }

        return cancel;
    }

    /**
     * 扩展FutureTask done方法，用于实现监听器回调
     */
    @Override
    protected void done() {
        Throwable cause;
        try {
            result = get();
            state = SUCCESS;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            state = INTERRUPTED;
        } catch (ExecutionException ex) {
            cause = ex.getCause();
            if (cause != null) {
                state = EXCEPTIONAL;
                cause = ex;
                exception = cause;
            }
        } catch (Throwable ex) {
            cause = ex;
            exception = cause;
            state = EXCEPTIONAL;
        }finally {
            //表示当前Future已经执行完成了
            done = true;
        }

        listeners.forEach(listener -> listener.onComplete(this));
    }
}
