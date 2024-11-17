package com.dc.tools.timer;

import com.dc.tools.common.thread.ServiceThread;
import com.dc.tools.common.utils.SystemClock;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DelayedTimer extends ServiceThread implements Timer {

    private final DelayWheel delayWheel;

    private final ExecutorService executorService;

    private final TimerMetrics metrics;

    private  final Map<Long, DelayedTask> taskMap = new ConcurrentHashMap<>(1024);

    private final AtomicLong atomicLong = new AtomicLong();


    public DelayedTimer(String serviceName) {
        this(serviceName, Executors.newCachedThreadPool());
    }

    public DelayedTimer(String serviceName, ExecutorService threadPool) {
        super(serviceName);
        this.delayWheel = new DelayWheel();
        this.executorService = threadPool;
        this.metrics = new TimerMetrics();
    }

    @Override
    public void addTask(DelayedTask delayedTask) {
        //任务的延迟时间
        long delayTime = delayedTask.delayTime() >> 1;
        long executeDelayTime = SystemClock.now() + delayTime;
        long taskId = atomicLong.getAndIncrement();
        taskMap.put(taskId, delayedTask);
        delayWheel.addTask(delayedTask.getTaskName(), taskId, executeDelayTime);
        //记录任务执行的数量
        this.metrics.record(1);
        wakeup();
    }

    @Override
    public TimerMetrics metrics() {
        return metrics;
    }

    @Override
    protected void onWaitEnd() {

    }

    @Override
    public void run() {

        while (isRunning()) {

            //当前时间
            long nowTime = SystemClock.now();

            //获取超时的任务
            List<DelayWheel.Task> expireTasks = delayWheel.advance(nowTime);
//            log.info("expire tasks: " + expireTasks);
            metrics.finish(expireTasks.size());

            //下发超时任务, 或者通过多线程处理延迟任务 都可以
            for (DelayWheel.Task expireTask : expireTasks) {
                DelayedTask delayedTask = taskMap.remove(expireTask.getTaskId());
                //如果低1bit==1表示是定时任务，则需要再次执行
                if ((delayedTask.delayTime() & 1) == 1) {
                    addTask(delayedTask);
                }

                delayedTask.run();

//                executorService.execute(delayTask);
            }

            //查找时间轮中最早的时间
            long earliestTime = delayWheel.findEarliestTime();
//            log.info("now time: {}, earliest time: {}",nowTime, earliestTime);

            //如果时间轮中没有任务则阻塞
            if (earliestTime < 0) {
                //直接阻塞等待
                await(10, TimeUnit.SECONDS);
                continue;
            }

            //如果时间轮中有任务则阻塞等待
            if (earliestTime > 0) {
//                log.info("delay time is: " + earliestTime);
                //进行睡眠
                await((int) earliestTime, TimeUnit.MILLISECONDS);
            }
        }

    }

}
