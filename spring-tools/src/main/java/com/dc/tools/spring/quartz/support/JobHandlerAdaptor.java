package com.dc.tools.spring.quartz.support;

import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.spring.quartz.JobFailCallback;
import com.dc.tools.spring.quartz.JobSuccessCallback;
import com.dc.tools.spring.quartz.job.JobDefinition;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;

/**
 * job任务适配器，适配方法与 任务
 *
 * @author zhangyang
 * @date 2020-09-29
 */
@Setter
@Builder
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobHandlerAdaptor implements Comparable<JobHandlerAdaptor> {

    @Getter(AccessLevel.PROTECTED)
    JobMetaData jobMetaData;

    /**
     * 执行成功回调
     */
    List<JobSuccessCallback> successHandlers;

    /**
     * 执行失败回调
     */
    List<JobFailCallback> failHandlers;


    @Getter
    JobDefinition jobDefinition;


    PlatformTransactionManager transactionManager;

    /**
     * 用于标识EntityManager 的状态
     */
    private static final String CREATE_ENTITY_MANAGER = "QUARTZ_BIND_NEW_ENTITY_MANAGER_";

    /**
     * 执行定时任务方法
     *
     * @param args             方法参数
     * @param executionContext 定时任务上下文
     */
    public Object executeJob(Object[] args, JobExecutionContext executionContext) throws Exception {
        JobKey jobKey = jobDefinition.getJobDetail().getKey();
        CloseTasks closeTasks = new CloseTasks();
        TransactionStatus transactionStatus =  beforeExecute();
        try {
            closeTasks.addTask(()-> afterClean(transactionStatus), "Close job transaction");
            Object result = jobDefinition.getJobMethod().invokeMethod(args);
            //任务执行成功后，进行回调
            for (JobSuccessCallback successHandler : successHandlers) {
                if (successHandler.support(jobKey.getName(), jobKey.getGroup())) {
                    successHandler.success(jobDefinition, executionContext);
                }
            }

            return result;
        } catch (Exception error) {
            for (JobFailCallback failHandler : failHandlers) {
                if (failHandler.support(jobKey.getName(), jobKey.getGroup())) {
                    failHandler.fail(jobDefinition, executionContext, error);
                }
            }

            transactionManager.rollback(transactionStatus);
            throw new JobExecutionException("Executing job of [" + jobDefinition.getJobName() + "] is error.", error.getCause());
        } finally {
            //释放Resource资源
            closeTasks.close();
        }
    }


    /**
     * 判断当前线程是否挂载entityManager 资源，如果没有挂载则创建entityManager
     */
    private TransactionStatus beforeExecute() {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        return transactionManager.getTransaction(definition);
    }


    /**
     * 判断当前线程是否是由自己挂载的entityManager 资源，如果是则手动关闭清除
     */
    private void afterClean(TransactionStatus status) {
        //如果当前EntityManager 已经在其他地方关闭，则直接忽略
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
           transactionManager.commit(status);
        }
    }


    public String getMethodName() {
        return jobDefinition.getMethodName();
    }


    public List<JobSuccessCallback> getSuccessHandlers() {
        return Collections.unmodifiableList(successHandlers);
    }


    public List<JobFailCallback> getFailHandlers() {
        return Collections.unmodifiableList(failHandlers);
    }


    @Override
    public int compareTo(@NonNull JobHandlerAdaptor other) {
        return jobMetaData.getJobMethod().getMethodName().compareTo(other.jobMetaData.getJobMethod().getMethodName());
    }
}
