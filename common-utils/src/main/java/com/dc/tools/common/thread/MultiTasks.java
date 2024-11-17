package com.dc.tools.common.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代码实现有问题
 * @author zy
 */
@Deprecated
public class MultiTasks<V> {

    private ExecutionTask<V> root;

    private ExecutionTask<V> tail;

    private ExecutorService pool;

    public MultiTasks(ExecutorService pool) {
        this.pool = pool;
    }

    public void addTask(ExecutionTask<V> executionTask) {
        if (root == null) {
            root = tail = executionTask;
        }

        executionTask.prev = tail;
        tail.next = executionTask;
        tail = executionTask;
    }

    public void execute() {
        ExecutionTask<V> current = root;
        while (current != null) {
            pool.submit(current);
            current = current.next;
        }
    }


    public CompletableFuture<V> getResult() {
        //如果tail节点已经是wake状态则说明所有的节点均已经执行完毕
        if (tail.isWake()) {
            ExecutionTask<V> current = root;
            while (current.isEmpty()) {
                current = root.next;
            }

            return current.getResult();
        }
        return executeResult(root);
    }

    public CompletableFuture<V> executeResult(ExecutionTask<V> current) {
        if (current == null) {
            return CompletableFuture.completedFuture(null);
        }

        return current.getResult().thenCompose((v) -> {
            if (v == null) {
                return executeResult(current.next);
            }

            return CompletableFuture.completedFuture(v);
        });
    }


    public static class ExecutionTask<V> implements Callable<V> {


        static AtomicInteger PRIORITY = new AtomicInteger();

        Callable<V> delegate;

        CompletableFuture<V> future = new CompletableFuture<>();

        /**
         * 是否执行完成
         */
        AtomicBoolean complete = new AtomicBoolean(false);

        /**
         * 是否已经执行完成
         */
        private volatile boolean executed = false;

        private volatile V result;

        /**
         * 优先级顺序
         */
        private final int priority;


        private ExecutionTask<V> next;

        private ExecutionTask<V> prev;

        public ExecutionTask(Callable<V> delegate) {
            this.priority = PRIORITY.getAndIncrement();
            this.delegate = delegate;
        }

        public int getPriority() {
            return priority;
        }

        void notifyNext() {
            //如果下游节点不为空则唤醒下游节点
            if (next != null)
                next.wakeUp();
        }

        public void wakeUp() {
            //如果任务已经执行完成则直接将结果存放到future，否则就先打个标记等任务正真完成时存放
            if (complete.compareAndSet(false, true) && executed) {
                future.complete(result);
            }
        }

        public boolean isWake() {
            return complete.get();
        }

        public boolean isEmpty() {
            return result == null;
        }

        public CompletableFuture<V> getResult() {
            return future;
        }

        @Override
        public V call() throws Exception {
            try {
                result = delegate.call();
                //表示当前任务已经执行完成
                executed = true;

                //打个标记，这里的意义是表示上游已经执行完成了这里将任务添加进去
                if (complete.get()) {
                    future.complete(result);
                }

                //唤醒下游节点
                notifyNext();
            } finally {
                //说明是头结点
                if (prev == null) {
                    wakeUp();
                }
            }

            return result;
        }


    }


}
