package com.dc.tools.common.utils;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 用于关闭任务
 *
 * @author zhangyang
 */
@ThreadSafe
public class CloseTasks implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(CloseTasks.class);


    private static final String DEFAULT_MANAGER_NAME = "DEFAULT_CLOSE_MANAGER";

    /**
     * 用于存放关闭的任务
     */
    private final Deque<CloseTaskWrapper> closeTasks;

    /**
     * manager name for all close task
     */
    private final String managerName;

    public static CloseTasks empty() {
        return new CloseTasks(DEFAULT_MANAGER_NAME);
    }

    public CloseTasks() {
        this.closeTasks = new ConcurrentLinkedDeque<>();
        this.managerName = DEFAULT_MANAGER_NAME;
    }

    public CloseTasks(String managerName, Object... args) {
        this.closeTasks = new ConcurrentLinkedDeque<>();
        this.managerName = StrUtil.format(managerName, args);
    }

    public CloseTasks(String taskManagerName, Collection<CloseTaskWrapper> closeTasks) {
        Assert.notNull(closeTasks, "closeTasks cannot be null");
        this.managerName = taskManagerName;
        this.closeTasks = new ConcurrentLinkedDeque<>();
        this.closeTasks.addAll(closeTasks);
    }

    /**
     * 添加需要被关闭的任务
     *
     * @param task     需要被关闭的任务
     * @param taskName 任务名称
     */
    public CloseTasks addTask(CloseTask task, String taskName, Object... args) {
        closeTasks.offerLast(CloseTaskWrapper.create(managerName, task, StrUtil.format(taskName, args)));
        return this;
    }

    /**
     * 添加需要被关闭的任务,并且将其放在首位，针对一些特殊的场景需要依赖顺序时才需要
     *
     * @param task     需要被关闭的任务
     * @param taskName 任务名称
     */
    public CloseTasks addFirst(CloseTask task, String taskName, Object... args) {
        closeTasks.offerFirst(CloseTaskWrapper.create(managerName, task, StrUtil.format(taskName, args)));
        return this;
    }

    /**
     * 添加需要被关闭的任务
     *
     * @param tasks 其他的closeTasks任务
     */
    public CloseTasks addTasks(CloseTasks tasks) {
        closeTasks.addAll(tasks.closeTasks);
        return this;
    }

    /**
     * 添加需要被关闭的任务
     *
     * @param supplier 需要被关闭的任务
     * @param taskName 任务名称
     */
    @Deprecated
    public CloseTasks addClosedTask(Supplier<Closeable> supplier, String taskName, Object... args) {
        Closeable closeable = null;

        try {
            closeable = supplier.get();
            closeTasks.add(CloseTaskWrapper.create(managerName, closeable::close, StrUtil.format(taskName, args)));
        } catch (Exception e) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException ex) {
                log.warn("Close task of {} error, please check it, cause is: ", taskName, ex);
            }
        }

        return this;
    }

    public void removeAllTasks() {
        closeTasks.clear();
    }

    @Override
    public void close() {
        for (CloseTaskWrapper closeTask : closeTasks) {
            try {
                closeTask.close();
            } catch (Exception ex) {
                log.error("Close manager: {}, close task of {} error, please check it, cause is: ", managerName, closeTask.taskName, ex);
            }
        }

        closeTasks.clear();
    }

    /**
     * Wrap uncheck close method
     *
     * @see Closeable
     */
    public interface CloseTask {

        /**
         * Unchecked close method
         *
         * @throws IOException 抛出异常
         */
        void close() throws IOException;

    }


    public static class CloseTaskWrapper implements Closeable {

        /**
         * 需要被关闭的任务
         */
        private final CloseTask closeTask;

        /**
         * 任务名称
         */
        private final String taskName;
        /**
         * 主要为了保护当前task避免被执行多次而导致出错
         */
        private final AtomicBoolean close = new AtomicBoolean(false);

        private final String managerName;

        public CloseTaskWrapper(String managerName, CloseTask closeTask, String taskName) {
            this.closeTask = closeTask;
            this.taskName = taskName;
            this.managerName = managerName;
        }


        public static CloseTaskWrapper create(CloseTask closeTask, String taskName) {
            return new CloseTaskWrapper(DEFAULT_MANAGER_NAME, closeTask, taskName);
        }

        public static CloseTaskWrapper create(String managerName, CloseTask closeTask, String taskName) {
            return new CloseTaskWrapper(managerName, closeTask, taskName);
        }


        @Override
        public void close() throws IOException {
            if (close.compareAndSet(false, true)) {
                log.info("Close manager: {} ,Close task name: {}", managerName, taskName);
                closeTask.close();
            }
        }
    }

}
