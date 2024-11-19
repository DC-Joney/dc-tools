package com.dc.tools.task.worker;

import com.codahale.metrics.MetricRegistry;
import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.common.thread.ServiceThread;
import com.dc.tools.common.utils.CollectionUtils;
import com.dc.tools.common.utils.SystemClock;
import com.dc.tools.task.*;
import com.dc.tools.task.processor.MultiTaskProcessor;
import com.dc.tools.task.retry.BackoffPolicy;
import com.dc.tools.task.retry.RetryContext;
import com.dc.tools.task.retry.RetryTask;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractTaskWorker<T extends Task> extends ServiceThread implements TaskWorker<T>, Executor {

    protected final TaskManager taskManager;

    private final AtomicBoolean started = new AtomicBoolean();

    protected final MpscUnboundedArrayQueue<ContextTask> tasks = new MpscUnboundedArrayQueue<>(1024);

    /**
     * 指标统计注册
     */
    private final MetricRegistry registry;


    /**
     * 当前worker线程的统计信息
     */
    private final WorkerStats workerStats;



    public AbstractTaskWorker(String serviceName, TaskManager taskManager, MetricRegistry registry) {
        super(serviceName);
        this.taskManager = taskManager;
        this.registry = registry;
        this.workerStats = new WorkerStats(registry, serviceName, () -> (long) tasks.size());
    }

    public AbstractTaskWorker(String serviceName, TaskManager taskManager) {
        super(serviceName);
        this.registry = new MetricRegistry();
        this.taskManager = taskManager;
        this.workerStats = new WorkerStats(registry, serviceName, () -> (long) tasks.size());
    }

    @Override
    protected void onWaitEnd() {

    }

    @Override
    public void execute(T task, TaskContext taskContext) {

        workerStats.incReceivedTask();

        //如果当前worker已经关闭了，则不在添加任务
        if (!started.get()) {
            taskManager.addTask(task, taskContext);
        }

        if (taskContext == null) {
            taskContext = new TaskContext();
        }

        Long taskId = taskContext.taskId();
        if (taskId == null) {
            taskContext.setTaskId(taskManager.idGenerator().nextId());
        }

        taskContext.putIfAbsent(TaskContext.TASK_MANAGER, taskManager);

        ContextTask contextTask = new ContextTask(task, taskContext);
        taskContext.putIfAbsent(TaskContext.TASK_WORKER, this);
        tasks.offer(contextTask);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(@NonNull Runnable command) {
        execute((T) new RunnableTaskWrapper(command), null);
    }

    @Override
    public TaskWorkerMetrics metrics() {
        return workerStats;
    }


    @SuppressWarnings("all")
    protected void processTask(ContextTask contextTask) {

        long startTime = SystemClock.now();

        TaskContext taskContext = contextTask.getTaskContext();
        Task targetTask = contextTask.getDelegate();
        //获取任务的处理器
        MultiTaskProcessor<? super Task> taskProcessor = taskManager.getProcessor(targetTask.taskName());
        boolean exceptState = false;
        Exception exception = null;

        Integer taskState = taskContext.get(TaskContext.TASK_LIFE_CYCLE, Integer.class);
        if (taskState == null) {
            taskContext.put(TaskContext.TASK_LIFE_CYCLE, 1);
            try {
                targetTask.before(taskContext);
            } catch (Exception e) {
                //TODO 添加异常信息 表示在执行生命周期方法时出现异常
                log.error("Execute task before method error, taskName is: {}, cause is: {}", targetTask.taskName(), e);
            }
        }

        Integer processorState = taskContext.get(TaskContext.PROCESSOR_LIFE_CYCLE, Integer.class);
        if (processorState == null) {
            taskContext.put(TaskContext.PROCESSOR_LIFE_CYCLE, 1);
            try {
                taskProcessor.before(targetTask, taskContext);
            } catch (Exception e) {
                log.error("Execute taskProcessor before method error, taskName is: {}, cause is: {}", targetTask.taskName(), e);

                //TODO 添加异常信息 表示在执行生命周期方法时出现异常
            }
        }

        try {
            boolean process = taskProcessor.process(targetTask, taskContext);

            if (!process) {
                //添加处理失败的状态
                exceptState = true;
                return;
            }

            //否则判断任务是否为可执行的任务
            if (targetTask instanceof ExecutionTask) {
                if (!((ExecutionTask) targetTask).execute(taskContext)) {
                    exceptState = true;
                }

                return;
            }

            //调用默认处理器进行处理
            MultiTaskProcessor<Task> defaultProcessor = taskManager.getDefaultProcessor();
            if (!defaultProcessor.process(targetTask, taskContext)) {
                exceptState = true;
            }

        } catch (Exception e) {
            exceptState = true;
            exception = e;
            //TODO: 打印日志
        } finally {
            //记录完成的任务信息，重试任务算多个任务
            workerStats.incHandledTask();
            //记录任务消耗的时间
            workerStats.recordTime(SystemClock.now() - startTime);
            boolean needRetry = handleRetry(taskContext, targetTask, exceptState, exception);
            if (!needRetry) {
                //执行生命周期方法回调
                if (taskContext.contains(TaskContext.TASK_LIFE_CYCLE)) {
                    try {
                        targetTask.after(exception, taskContext);
                    } catch (Exception e) {
                        //TODO 添加异常信息 表示在执行生命周期方法时出现异常
                        log.error("Execute task after method error, taskName is: {}, cause is: {}", targetTask.taskName(), e);
                    }
                }

                if (targetTask instanceof ResultAsyncTask && exception != null) {
                    ((ResultAsyncTask<?>) targetTask).setException(exception);
                }

                if (taskContext.contains(TaskContext.PROCESSOR_LIFE_CYCLE)) {
                    try {
                        taskProcessor.after(targetTask, taskContext, exception);
                    } catch (Exception e) {
                        //TODO 添加异常信息 表示在执行生命周期方法时出现异常
                        log.error("Execute taskProcessor after method error, taskName is: {}, cause is: {}", targetTask.taskName(), e);
                    }
                }

                List<TaskCallback> taskCallbacks = taskContext.taskCallbacks();
                if (!CollectionUtils.isEmpty(taskCallbacks)) {
                    for (TaskCallback taskCallback : taskCallbacks) {
                        try {
                            taskCallback.onCallback(exception);
                        } catch (Exception e) {
                            //TODO 异常处理
                        }
                    }
                }

                //清除重试相关的属性
                taskContext.remove(TaskContext.RETRY, TaskContext.TASK_ID, TaskContext.TASK_LIFE_CYCLE,
                        TaskContext.INTERNAL, TaskContext.TASK_WORKER, TaskContext.PROCESSOR_LIFE_CYCLE);
            }


        }
    }

    private boolean handleRetry(TaskContext taskContext, Task targetTask, boolean exceptState, Exception exception) {

        if (exceptState && targetTask instanceof RetryTask) {
            RetryTask retryTask = (RetryTask) targetTask;
            //如果异常不为空 并且 忽略该异常的话 则不在进行重试
            if (exception != null && retryTask.ignoreException(exception)) {
                return false;
            }

            //拿到重试的上下文信息
            RetryContext retryContext = taskContext.get(TaskContext.RETRY, RetryContext.class);
            if (retryContext == null) {
                retryContext = new RetryContext(retryTask.maxRetries(), retryTask.backoffPolicy());
                taskContext.putIfAbsent(TaskContext.RETRY, retryContext);
            }

            //当前重试的次数
            AtomicInteger retryCount = retryContext.getRetryCount();
            //最大重试的次数
            int maxRetryTimes = retryContext.getMaxRetryTimes();
            //重试的策略
            BackoffPolicy backoffPolicy = retryContext.getBackoffPolicy();
            //如果任务的重试次数 <= 最大重试次数则进行重试
            if (retryCount.getAndIncrement() < maxRetryTimes) {
                //下次执行的时间
                long nextTime = backoffPolicy.nextTime();
                //标注为内部流转的节点不需要生成新的taskId
                taskContext.put(TaskContext.INTERNAL, TaskContext.INTERNAL);
                //当前执行的任务类型
                int interestType = workerType().interestType();
                //如果重试的时间 > 0 则投递到延迟任务worker 从新计算
                if (nextTime > 0) {
                    //设置从新路由的taskType类型
                    taskContext.setTaskType(interestType | TaskType.DELAYED_TYPE);
                    targetTask = new DelayedTaskWrapper(retryTask, nextTime);
                } else {
                    //设置从新路由的taskType类型
                    taskContext.setTaskType(interestType);
                }

                //对任务从新进行路由分发x
                taskManager.addTask(targetTask, taskContext);
                return true;
            }

        }

        return false;
    }


    @Override
    public void start() {
        super.start();
    }

    @Override
    public void shutdown() {
        if (started.compareAndSet(false, true)) {
            stop();
        }
    }


}
