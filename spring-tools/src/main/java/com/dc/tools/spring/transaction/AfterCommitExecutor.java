package com.dc.tools.spring.transaction;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


/**
 * 事务完成之后再执行操作
 * @author zhangyang
 */
@Slf4j
public class AfterCommitExecutor implements TransactionSynchronization {

    private static final ThreadLocal<List<TransactionTask>> CALLABLES = ThreadLocal.withInitial(LinkedList::new);

    private final ExecutorService threadPool;

    public AfterCommitExecutor(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * 添加task 任务，只会影响到当前线程中的事物处理
     *
     * @param transactionTask 任务
     */
    public void addTransactionTask(TransactionTask transactionTask) {
        log.info("Submitting new transactionTask {} to run after commit", transactionTask.taskName());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.info("Transaction synchronization is NOT ACTIVE. Executing right now runnable ");
            try {
                transactionTask.handle();
            } catch (Exception e) {
                log.info("After commit callback execute error, cause is: ", e);
            }
            return;
        }

        List<TransactionTask> transactionTasks = CALLABLES.get();

        //Spring 默认通过 Set 进行存储，所以不会重复添加
        TransactionSynchronizationManager.registerSynchronization(this);
        transactionTasks.add(transactionTask);
    }


    @Override
    public void afterCommit() {
        List<TransactionTask> transactionTasks = CALLABLES.get();
        log.info("Transaction successfully committed, executing {} Callables", transactionTasks.size());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try {
            transactionTasks.stream()
                    .map(task -> CompletableFuture.runAsync(task::handleUnchecked, threadPool)
                            .exceptionally(ex -> {
                                log.info("Execute transaction task error, task name is: {}, cause is: ", task.taskName(), ex.getCause());
                                return null;
                            }))
                    .collect(Collectors.toCollection(() -> futures));

        } catch (Exception e) {
            log.error("Execute transaction after callback error, cause is: ", e);
        } finally {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }
    }

    @Override
    public void afterCompletion(int status) {
        log.info("Transaction completed with status {}", status == STATUS_COMMITTED ? "COMMITTED" : "ROLLED_BACK");
        CALLABLES.remove();
    }


    public interface TransactionTask {

        /**
         * After transaction commit handler
         *
         * @throws Exception throws exception
         */
        void handle() throws Exception;

        /**
         * After transaction commit handler for unchecked
         */
        @SneakyThrows
        default void handleUnchecked() {
            handle();
        }

        /**
         * Task name, default is uuid
         */
        default String taskName() {
            return UUID.randomUUID().toString();
        }
    }

    @AllArgsConstructor
    private static class CallableAdaptor implements Callable<Object> {

        private final TransactionTask transactionTask;

        @Override
        public Object call() throws Exception {
            transactionTask.handle();
            return null;
        }
    }

}