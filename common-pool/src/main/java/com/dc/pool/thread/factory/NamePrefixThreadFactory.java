package com.dc.pool.thread.factory;




import com.dc.tools.common.annotaion.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 生成自定义前缀的Factory
 * @author zhangyang
 */
public class NamePrefixThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final ClassLoader classLoader;

    public NamePrefixThreadFactory(ClassLoader classLoader, String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix;
        this.classLoader = classLoader;
    }

    public NamePrefixThreadFactory(String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix;
        this.classLoader = getClass().getClassLoader();
    }

    @Override
    public Thread newThread(@NonNull Runnable r) {
        Thread t = new Thread(group, r, namePrefix + "-" + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);

        if (classLoader != null)
            t.setContextClassLoader(classLoader);

        return t;
    }
}