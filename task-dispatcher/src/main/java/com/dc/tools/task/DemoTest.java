package com.dc.tools.task;

import com.dc.tools.task.processor.MultiTaskProcessor;
import com.dc.tools.task.retry.BackoffPolicy;
import com.dc.tools.task.retry.FixDelayBackoffPolicy;
import com.dc.tools.task.retry.RetryTask;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DemoTest {

    public static void main(String[] args) throws InterruptedException {
        TaskManager instance = TaskDispatchCenter.getInstance();

        TaskProcessorDemo processorDemo = new TaskProcessorDemo("1111");
        TaskProcessorDemo1 processorDemo1 = new TaskProcessorDemo1("2222");
        instance.addDefaultProcessor(processorDemo1);

        MultiTaskProcessor<Task> taskProcessor = instance.newProcessor("taskDemo");
        taskProcessor.addLast(processorDemo)
                .addLast(processorDemo1);

        Duration duration = Duration.ofSeconds(200);
        System.out.println(duration.toMillis());
        instance.addTask(new DemoTask());
        instance.addTask(new DemoTask1());
        TimeUnit.SECONDS.sleep(10000);

    }

    static class DemoTask implements RetryTask {
        @Override
        public void before(TaskContext taskContext) {
            System.out.println("1111");
        }

        @Override
        public void after(Exception ex, TaskContext taskContext) {
            System.out.println("222222");
        }

        @Override
        public int maxRetries() {
            return 3;
        }

        @Override
        public BackoffPolicy backoffPolicy() {
            return new FixDelayBackoffPolicy(Duration.ofSeconds(2));
        }

        @Override
        public String taskName() {
            return "taskDemo";
        }
    }

    static class DemoTask1 implements RetryTask {
        @Override
        public void before(TaskContext taskContext) {
            System.out.println("DemoTask11111");
        }

        @Override
        public void after(Exception ex, TaskContext taskContext) {
            System.out.println("DemoTask122222");
        }

        @Override
        public int maxRetries() {
            return 3;
        }

        @Override
        public BackoffPolicy backoffPolicy() {
            return new FixDelayBackoffPolicy(Duration.ofSeconds(2));
        }

        @Override
        public String taskName() {
            return "taskDemo1";
        }
    }

    static class TaskProcessorDemo implements TaskProcessor<Task> {

        private String processName;

        public TaskProcessorDemo(String processName) {
            this.processName = processName;
        }



        @Override
        public boolean process(Task task, TaskContext taskContext) throws Exception {
            System.out.println("Processing 111111111");
            throw new RuntimeException("123");
        }

        @Override
        public String processorName() {
            return processName;
        }
    }

    static class TaskProcessorDemo1 implements TaskProcessor<Task> {

        private String processName;

        public TaskProcessorDemo1(String processName) {
            this.processName = processName;
        }

        @Override
        public boolean process(Task task, TaskContext taskContext) {
            log.info("TaskProcessorDemo1 execute");
            return true;
        }


        @Override
        public String processorName() {
            return processName;
        }
    }


}
