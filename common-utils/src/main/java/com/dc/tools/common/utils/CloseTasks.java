package com.dc.tools.common.utils;

import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 用于关闭任务
 *
 * @author zhangyang
 */
@Slf4j
@ThreadSafe
public class CloseTasks implements Closeable {

    /**
     * 最多放256个关闭任务
     */
    private final ConcurrentList<CloseTaskWrapper> closeTasks;

    public static CloseTasks empty() {
        return new CloseTasks(null);
    }

    public CloseTasks() {
        this.closeTasks = new ConcurrentList<>();
    }

    public CloseTasks(ConcurrentList<CloseTaskWrapper> closeTasks) {
        this.closeTasks = closeTasks;
    }

    /**
     * 添加需要被关闭的任务
     *
     * @param task     需要被关闭的任务
     * @param taskName 任务名称
     */
    public CloseTasks addTask(CloseTask task, String taskName, Object... args) {
        closeTasks.add(CloseTaskWrapper.create(task, StrUtil.format(taskName, args)));
        return this;
    }

    /**
     * 添加需要被关闭的任务,并且将其放在首位，针对一些特殊的场景需要依赖顺序时才需要
     *
     * @param task     需要被关闭的任务
     * @param taskName 任务名称
     */
    public CloseTasks addFirst(CloseTask task, String taskName, Object... args) {
        closeTasks.addFirst(CloseTaskWrapper.create(task, StrUtil.format(taskName, args)));
        return this;
    }

    /**
     * 添加需要被关闭的任务
     *
     * @param tasks    其他的closeTasks任务
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
    public CloseTasks addTask(Supplier<Closeable> supplier, String taskName, Object... args) {
        Closeable closeable = null;

        try {
            closeable = supplier.get();
            closeTasks.add(CloseTaskWrapper.create(closeable::close, StrUtil.format(taskName, args)));
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


    public static CloseTasks from(CloseTask task, String taskName) {
        return new CloseTasks().addTask(task, taskName);
    }

    @Override
    public void close() {
        for (CloseTaskWrapper closeTask : closeTasks) {
            try {
                closeTask.close();
                log.info("Close task name: {}", closeTask.taskName);
            } catch (Exception ex) {
                log.warn("Close task of {} error, please check it, cause is: ", closeTask.taskName, ex);
            }
        }
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


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(staticName = "create")
    public static class CloseTaskWrapper implements Closeable {

        final CloseTask closeTask;

        final String taskName;

        /**
         * 主要为了保护当前task避免被执行多次而导致出错
         */
        AtomicBoolean CLOSE = new AtomicBoolean(false);

        @Override
        public void close() throws IOException {
            if (CLOSE.compareAndSet(false, true))
                closeTask.close();
        }
    }

}
