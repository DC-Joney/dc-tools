package com.dc.pool.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * ThreadPool based on Group isolation
 *
 * @author tynan.liu
 * @date 2022/6/24 17:25
 * @since 1.3.12
 *
 * @apiNote Fork from <a href="https://github.com/sofastack/sofa-jraft">Soft-Jraft</a>
 **/
public class ThreadPoolsFactory {
    private static final Logger LOG = LoggerFactory
            .getLogger(ThreadPoolsFactory.class);
    /**
     * It is used to handle global closure tasks
     */
    private static final ConcurrentMap<String, ThreadPoolExecutor> GROUP_THREAD_POOLS = new ConcurrentHashMap<>();

    private static class GlobalThreadPoolHolder {
        private static final ThreadPoolExecutor INSTANCE = ThreadPoolUtil
                .newBuilder()
                .poolName("DEFAULT_POOL_EXECUTOR")
                .enableMetric(true)
                .coreThreads(Runtime.getRuntime().availableProcessors())
                .maximumThreads(Runtime.getRuntime().availableProcessors() * 2)
                .keepAliveSeconds(60L)
                .workQueue(new ArrayBlockingQueue<>(1 << 11))
                .threadFactory(new NamedThreadFactory("JRaft-Group-Default-Executor-", true)).build();
    }

    /**
     * You can specify the ThreadPoolExecutor yourself here
     *
     * @param groupId  group id
     * @param executor To specify ThreadPoolExecutor
     */
    public static void registerThreadPool(String groupId, ThreadPoolExecutor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }

        if (GROUP_THREAD_POOLS.putIfAbsent(groupId, executor) != null) {
            throw new IllegalArgumentException(String.format("The group: %s has already registered the ThreadPool",
                    groupId));
        }
    }

    protected static ThreadPoolExecutor getExecutor(String groupId) {
        return GROUP_THREAD_POOLS.getOrDefault(groupId, GlobalThreadPoolHolder.INSTANCE);
    }

    /**
     * Run a task in thread pool,returns the future object.
     */
    public static Future<?> runInThread(String groupId, final Runnable runnable) {
        return GROUP_THREAD_POOLS.getOrDefault(groupId, GlobalThreadPoolHolder.INSTANCE).submit(runnable);
    }

}