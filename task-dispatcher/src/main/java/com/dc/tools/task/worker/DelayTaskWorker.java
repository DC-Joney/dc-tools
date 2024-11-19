package com.dc.tools.task.worker;

import com.dc.tools.common.thread.ServiceThread;
import com.dc.tools.common.utils.SystemClock;
import com.dc.tools.task.*;
import com.dc.tools.timer.DelayWheel;
import com.dc.tools.timer.TimerMetrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DelayTaskWorker extends ServiceThread implements TaskWorker<DelayTask> {

    private final TimerMetrics metrics;

    private final Map<Long, ContextTask> taskMap = new ConcurrentHashMap<>(1024);


    private final TaskManager taskManager;

    private static final AtomicLong index = new AtomicLong();

    /**
     * 计算延迟任务的时间轮实现
     */
    private final DelayWheel delayWheel = new DelayWheel();


    public DelayTaskWorker(TaskManager taskManager) {
        super("delay-worker-" + index.getAndIncrement());
        this.metrics = new TimerMetrics();
        this.taskManager = taskManager;
    }


    @Override
    public void execute(DelayTask task, TaskContext taskContext) {
        Task targetTask = task;
        //如果是包装后的延迟任务
        if (targetTask instanceof DelayedTaskWrapper) {
            targetTask = ((DelayedTaskWrapper) task).getDelegate();
        }

        if (taskContext == null) {
            taskContext = new TaskContext();
        }

        taskContext.putIfAbsent(TaskContext.TASK_WORKER, this);
        Long taskId = taskContext.taskId();
        if (taskId == null) {
            taskId = taskManager.idGenerator().nextId();
            taskContext.setTaskId(taskId);
        }

        taskContext.putIfAbsent(TaskContext.TASK_MANAGER, taskManager);

        ContextTask contextTask = new ContextTask(targetTask, taskContext);
        taskMap.put(taskId, contextTask);
        String taskName = targetTask.taskName();
        //获取当前的时间
        long nowTime = SystemClock.now();
        delayWheel.addTask(taskName, taskId, nowTime + task.delayTime());
        metrics.record(1);
        wakeup();
    }

    @Override
    protected void onWaitEnd() {

    }

    @Override
    public TaskType workerType() {
        return TaskType.DELAYED;
    }

    @Override
    public TaskWorkerMetrics metrics() {
        return null;
    }

    @Override
    public void run() {
        while (isRunning()) {

            //当前时间
            long nowTime = SystemClock.now();

            long version = getVersion();

            //获取超时的任务
            List<DelayWheel.Task> expireTasks = delayWheel.advance(nowTime);
            metrics.finish(expireTasks.size());

            //下发超时任务, 或者通过多线程处理延迟任务 都可以
            for (DelayWheel.Task expireTask : expireTasks) {
                ContextTask contextTask = taskMap.remove(expireTask.getTaskId());
                Task task = contextTask.getDelegate();
                TaskContext taskContext = contextTask.getTaskContext();
                //标注为内部流转的节点不需要生成新的taskId
                taskContext.put(TaskContext.INTERNAL, TaskContext.INTERNAL);
                int interestType = task.taskType().interestType();
                taskContext.setTaskType(interestType & (~TaskType.DELAYED_TYPE));


                //TODO: 是否需要删除这里的实现，而是放在正真的任务执行前
                Integer state = taskContext.get(TaskContext.TASK_LIFE_CYCLE, Integer.class);
                if (state == null) {
                    taskContext.put(TaskContext.TASK_LIFE_CYCLE, 1);
                    try {
                        task.before(taskContext);
                    }catch (Exception e) {
                        //TODO 添加异常信息 表示在执行生命周期方法时出现异常
                    }
                }


                taskManager.addTask(task, taskContext);
            }

            //查找时间轮中最早的时间
            long earliestTime = delayWheel.findEarliestTime();

            //如果时间轮中没有任务则阻塞
            if (earliestTime < 0) {
                //直接阻塞等待
                await(version);
                continue;
            }

            //如果时间轮中有任务则阻塞等待
            if (earliestTime > 0) {
                //进行睡眠
                await(version, (int) earliestTime, TimeUnit.MILLISECONDS);
            }
        }


    }

    @Override
    public void shutdown() {
        stop();
    }
}
