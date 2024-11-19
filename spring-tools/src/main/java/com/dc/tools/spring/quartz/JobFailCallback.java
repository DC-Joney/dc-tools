package com.dc.tools.spring.quartz;

import com.dc.tools.spring.quartz.job.JobDefinition;
import org.quartz.JobExecutionContext;

/**
 * Job 任务执行失败回调
 *
 * @author zhangyang
 * @date 2020-09-30
 * @see 
 */
public interface JobFailCallback {

    /**
     * @param jobName   任务名称
     * @param groupName 组名称
     * @return 是否支持该job回调
     */
    boolean support(String jobName, String groupName);


    /**
     * 当任务执行失败时，进行回调
     *
     * @param definition       任务信息
     * @param executionContext 任务上下文
     * @param error            异常信息
     */
    void fail(JobDefinition definition, JobExecutionContext executionContext, Throwable error) throws Exception;

}
