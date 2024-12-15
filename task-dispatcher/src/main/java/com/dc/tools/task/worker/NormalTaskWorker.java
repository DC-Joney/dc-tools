package com.dc.tools.task.worker;

import com.codahale.metrics.MetricRegistry;
import com.dc.tools.common.utils.SystemClock;
import com.dc.tools.task.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于执行正常任务的worker线程
 *
 * @author zy
 */
public class NormalTaskWorker extends AbstractTaskWorker<Task> implements TaskWorker<Task> {

    private static final AtomicLong index = new AtomicLong();

    /**
     * 超时时间
     */
    private final long timeout;

    /**
     * 任务管理器
     */
    private final TaskManager taskManager;

    private volatile long lastRestTime;


    public NormalTaskWorker(TaskManager taskManager, MetricRegistry registry, long timeout) {
        super("fast-worker-" + index.getAndIncrement(), taskManager, registry);
        this.timeout = timeout;
        this.taskManager = taskManager;
    }


    @Override
    public void execute(Task task, TaskContext taskContext) {
        super.execute(task, taskContext);
        wakeup();
    }

    @Override
    public TaskType workerType() {
        return TaskType.NORMAL;
    }


    @Override
    public void run() {
        while (isRunning()) {
            long nowTime = SystemClock.now();
            //每隔10s 判断下是否需要从新拆分任务
            if (nowTime - lastRestTime > 10000) {
                double ratio = metrics().handleRatio();
                //如果处理的速率 < 1/2的时候 从新拆分任务
                if (ratio < 0.5) {
                    tasks.drain(task-> {
                        //从新路由任务减轻当前worker的压力，路由1/3的任务到其他的worker节点
                        taskManager.addTask(task.getDelegate(), task.getTaskContext());
                    }, tasks.size() / 3);
                }

                lastRestTime = nowTime;
            }

            ContextTask contextTask = tasks.poll();

            //版本号
            long version = getVersion();
            //空闲时间
            long freeTime = metrics().freeTime();

            if (contextTask == null && isRunning() && freeTime >= timeout && taskManager instanceof TaskDispatchCenter) {
                //从taskManager中删除当前线程
                ((TaskDispatchCenter) taskManager).removeWorker(this);
                //当前线程休眠100ms，保证一定可以接收到余下的任务
                continue;
            }

            if (contextTask == null) {
                await(version, (int) timeout, TimeUnit.MILLISECONDS);
                continue;
            }

            processTask(contextTask);
        }

        try {
            //线程休眠100ms，把余下的任务处理完成后在退出
            Thread.sleep(100);
        } catch (InterruptedException e) {
            //TODO: 打印日志
        }

        //处理余下的任务，如果处理失败会将该部分任务数据从新路由到新的worker
        while (tasks.peek() != null) {
            ContextTask contextTask = tasks.poll();
            taskManager.addTask(contextTask.getDelegate(), contextTask.getTaskContext());
        }
    }


}
