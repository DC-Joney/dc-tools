package com.dc.tools.task;

import com.dc.tools.common.IdGenerator;
import com.dc.tools.common.RandomIdGenerator;
import com.dc.tools.common.utils.StringUtils;
import com.dc.tools.task.exception.TaskException;
import com.dc.tools.task.processor.DefaultMultiProcessor;
import com.dc.tools.task.processor.MultiTaskProcessor;
import com.dc.tools.task.worker.DelayTaskWorker;
import com.dc.tools.task.worker.NormalTaskWorker;
import com.dc.tools.task.worker.SlowTaskWorker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务调度中心
 *
 * @author zy
 */
public class TaskDispatchCenter implements TaskManager {

    /**
     * 任务处理器
     */
    private final Map<String, MultiTaskProcessor<Task>> multiProcessors = new ConcurrentHashMap<>();

    /**
     * 存储所有的任务处理器
     */
    private final Map<String, TaskProcessorWrapper> processorMap = new ConcurrentHashMap<>();


    /**
     * 当任务不为 {@link ExecutionTask} 类型 以及没有对应的  {@link TaskProcessor} 处理器时则采用默认处理器进行处理
     */
    private final MultiTaskProcessor<Task> defaultProcessor = new DefaultMultiProcessor();

    /**
     * 用于处理及时的任务
     */
    private TaskWorker<Task>[] normalWorkers;

    /**
     * 延迟任务处理器
     */
    private TaskWorker<DelayTask>[] delayWorkers;

    /**
     * 慢任务处理器
     */
    private TaskWorker<Task>[] slowTaskWorkers;


    private final List<TaskWorker<Task>> workers;

    /**
     * 任务路由器
     */
    private TaskRouter taskRouter;

    /**
     * 任务id生成器
     */
    @Setter
    private TaskIdGenerator idGenerator;

    /**
     * 管理器名称
     */
    private final String managerName;

    /**
     * 核心worker数量
     */
    private final int coreSize;

    /**
     * 最大worker数量
     */
    private final int maxWorkerSize;


    /**
     * worker 触发关闭的最小处理比率
     */
    private final double minRatio;

    /**
     * 当前任务中心的状态
     */
    private volatile int state;

    private static final AtomicIntegerFieldUpdater<TaskDispatchCenter> UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(TaskDispatchCenter.class, "state");

    /**
     * @param managerName   任务管理器名称
     * @param coreSize      核心worker数量
     * @param maxWorkerSize 最大worker数量
     * @param minRatio      当worker的处理率 < timeRatio时则会尝试关闭
     */
    public TaskDispatchCenter(String managerName, int coreSize, int maxWorkerSize, double minRatio) {
        this.managerName = managerName;
        this.taskRouter = new DefaultTaskRouter();
        this.idGenerator = new DefaultTaskIdGenerator();
        this.coreSize = coreSize;
        this.maxWorkerSize = maxWorkerSize;
        this.minRatio = minRatio;
        this.workers = new CopyOnWriteArrayList<>();
        this.state = coreSize << 1;
        initAllWorkers();
    }

    /**
     * 初始化所有的worker线程
     */
    @SuppressWarnings("unchecked")
    private void initAllWorkers() {
        int processors = Runtime.getRuntime().availableProcessors();

        normalWorkers = new TaskWorker[processors];
        for (int i = 0; i < normalWorkers.length; i++) {
            normalWorkers[i] = new NormalTaskWorker(this);
            normalWorkers[i].start();
        }

        slowTaskWorkers = new TaskWorker[processors >>> 1];
        for (int i = 0; i < slowTaskWorkers.length; i++) {
            slowTaskWorkers[i] = new SlowTaskWorker(this);
            slowTaskWorkers[i].start();
        }

        delayWorkers = new TaskWorker[processors >>> 1];
        for (int i = 0; i < delayWorkers.length; i++) {
            delayWorkers[i] = new DelayTaskWorker(this);
            delayWorkers[i].start();
        }


    }

    public static TaskManager getInstance() {
        return new TaskDispatchCenter("default task manager",0,10,0.2d);
    }


    @Override
    @SuppressWarnings("unchecked")
    public void addProcessor(String taskName, TaskProcessor<? extends Task> processor) {
        TaskProcessorWrapper wrapper = processorMap.get(processor.processorName());
        if (wrapper == null) {
            wrapper = new TaskProcessorWrapper(processor);
            processorMap.putIfAbsent(processor.processorName(), wrapper);
        }

        //添加引用计数
        wrapper.incrementRef();

        MultiTaskProcessor<Task> taskProcessor = multiProcessors.get(taskName);

        if (taskProcessor != null) {
            throw new TaskException("The task for processor is already exited, please use getProcessor method");
        }

        if (!(processor instanceof MultiTaskProcessor)) {
            taskProcessor = new DefaultMultiProcessor();
            taskProcessor.addLast((TaskProcessor<Task>) processor);
        }

        multiProcessors.put(taskName, taskProcessor);
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task> MultiTaskProcessor<T> getProcessor(String taskName) {
        return (MultiTaskProcessor<T>) multiProcessors.get(taskName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task> MultiTaskProcessor<T> newProcessor(String taskName) {
        MultiTaskProcessor<Task> taskProcessor = multiProcessors.get(taskName);
        if (taskProcessor == null) {
            taskProcessor = new DefaultMultiProcessor();
            multiProcessors.put(taskName, taskProcessor);
        }

        return (MultiTaskProcessor<T>) taskProcessor;
    }

    @Override
    public TaskProcessor<? extends Task> getSingleProcessor(String taskName) {
        MultiTaskProcessor<? super Task> multiTaskProcessor = multiProcessors.get(taskName);
        return multiTaskProcessor != null ? multiTaskProcessor.getFirst() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addDefaultProcessor(TaskProcessor<? extends Task> processor) {
        defaultProcessor.addLast((TaskProcessor<Task>) processor);
    }

    @Override
    public void removeProcessor(String taskName) {
        multiProcessors.remove(taskName);
    }

    @Override
    public MultiTaskProcessor<Task> getDefaultProcessor() {
        return defaultProcessor;
    }

    @Override
    public TaskWorker<? super Task> route(Task task, TaskType taskType) {
        TaskWorker<?>[] taskWorkers = this.taskWorkers(taskType);
        if (taskWorkers.length == 0) {
            taskWorkers = normalWorkers;
        }

        return taskRouter.getWorker(task, taskType, taskWorkers);
    }

    @Override
    public TaskWorker<? super Task> route(Task task) {
        TaskType taskType = task.taskType();
        return route(task, taskType);
    }


    @Override
    public void addTask(Task task) {
        addTask(task, null);
    }


    @Override
    public void addDelayedTask(Task task, long delayTime, TimeUnit timeUnit) {
        this.addDelayedTask(task, null, delayTime, timeUnit);
    }

    @Override
    public void addDelayedTask(DelayTask delayedTask) {
        this.addDelayedTask(delayedTask, null);
    }

    @Override
    public TaskWorker<?>[] taskWorkers(TaskType taskType) {
        return taskType.isDelay() ? delayWorkers : (taskType.isSlow() ? slowTaskWorkers : normalWorkers);
    }

    @Override
    public void addTask(Task task, TaskContext taskContext) {

        if (taskContext == null) {
            taskContext = new TaskContext();
        }

        //如果为内部任务可能是重试的任务或者延迟的任务那么不会 生成新的任务id
        String internal = taskContext.getString(TaskContext.INTERNAL);
        if (StringUtils.isEmpty(internal)) {
            //存放任务计算出来的 task id
            taskContext.put(TaskContext.TASK_ID, idGenerator.nextId());
        }

        taskContext.putIfAbsent(TaskContext.TASK_MANAGER, this);

        //任务的类型
        TaskType taskType = taskContext.getTaskType();

        //如果没有新路由的任务类型则采用原任务类型进行路由
        if (taskType == null) {
            taskType = task.taskType();
        }

        //尝试去添加worker线程
        if (!tryAddWorker()) {
            //TODO: 抛弃任务，如果任务类型为重试任务则不会抛弃
            return;
        }

        //计算执行任务执行的worker节点
        TaskWorker<? super Task> taskWorker = route(task, taskType);
        taskWorker.execute(task, taskContext);
    }

    private boolean tryAddWorker() {
        for (TaskWorker<Task> normalWorker : normalWorkers) {
            double ratio = normalWorker.metrics().handleRatio();
            if (ratio < 0.2d && workers.size() > coreSize) {
                workers.remove(normalWorker);
                normalWorker.shutdown();
            }
        }

        for (; ; ) {
            int s = state;

            double ratio = 0;
            for (TaskWorker<Task> normalWorker : normalWorkers) {
                ratio = ratio + normalWorker.metrics().handleRatio();
            }

            //获取平均处理速率
            double avgRatio = ratio / workers.size();

            //如果平均的处理速率 < 50%, 那么可能需要扩容worker线程
            if (avgRatio > 0.5D) {
                return true;
            }

            //如果已经超过了最大worker的数量则直接返回
            if (workers.size() >= maxWorkerSize) {
                return false;
            }

            if ((s & 1) == 0 && UPDATER.compareAndSet(this, s, s | 1)) {
                //添加新的worker线程
                workers.add(new NormalTaskWorker(this));
                UPDATER.set(this, (s + 2) >> 1 << 1);
            }

        }


    }

    @Override
    public void addDelayedTask(Task task, TaskContext taskContext, long delayTime, TimeUnit timeUnit) {
        DelayedTaskWrapper taskWrapper = new DelayedTaskWrapper(task, timeUnit.toMillis(delayTime));
        this.addDelayedTask(taskWrapper, taskContext);
    }

    @Override
    public void addDelayedTask(DelayTask delayedTask, TaskContext taskContext) {
        if (taskContext == null) {
            taskContext = new TaskContext();
        }

        String internal = taskContext.getString(TaskContext.INTERNAL);

        //存放当前任务的id
        if (StringUtils.isEmpty(internal)) {
            taskContext.put(TaskContext.TASK_ID, idGenerator.nextId());
        }

        taskContext.putIfAbsent(TaskContext.TASK_MANAGER, this);

        //执行延迟任务
        TaskWorker<? super Task> worker = taskRouter.getWorker(delayedTask, TaskType.DELAYED, delayWorkers);
        worker.execute(delayedTask, taskContext);
    }

    @Override
    public void removeTaskName(String taskName) {

    }


    public void setTaskRouter(TaskRouter taskRouter) {
        this.taskRouter = taskRouter;
    }

    @Override
    public Collection<TaskProcessor<?>> getAllProcessors() {
        return Collections.unmodifiableCollection(multiProcessors.values());
    }


    @Override
    public TaskIdGenerator idGenerator() {
        return idGenerator;
    }

    @RequiredArgsConstructor
    static class TaskProcessorWrapper {

        @Getter
        private final TaskProcessor<? extends Task> target;

        private final AtomicLong refCounter = new AtomicLong(0);


        public long incrementRef() {
            return refCounter.incrementAndGet();
        }

        public long decrementRef() {
            return refCounter.decrementAndGet();
        }

    }


    static class DefaultTaskIdGenerator implements TaskIdGenerator {

        private final IdGenerator idGenerator = new RandomIdGenerator();

        @Override
        public long nextId() {
            return idGenerator.nextId();
        }
    }


    static class DefaultTaskRouter implements TaskRouter {

        private final AtomicLong normalIndex = new AtomicLong();

        private final AtomicLong slowIndex = new AtomicLong();

        private final AtomicLong delayIndex = new AtomicLong();


        @Override
        @SuppressWarnings("unchecked")
        public TaskWorker<? super Task> getWorker(Task task, TaskType taskType, TaskWorker<? extends Task>[] taskWorkers) {
            AtomicLong index = null;

            if (taskType.isDelay()) {
                index = delayIndex;
            } else if (taskType.isSlow())
                index = slowIndex;
            else
                index = normalIndex;

            //通过index寻找对应的worker线程
            long roundIndex = index.getAndIncrement();
            return (TaskWorker<? super Task>) taskWorkers[(int) (roundIndex % taskWorkers.length)];
        }
    }
}