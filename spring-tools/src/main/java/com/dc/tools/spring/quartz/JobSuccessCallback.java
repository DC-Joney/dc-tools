package com.dc.tools.spring.quartz;

import com.turing.common.quartz.job.JobDefinition;
import org.quartz.JobExecutionContext;

/**
 * Job 任务执行成功回调
 *
 * @author zhangyang
 * @date 2020-09-30
 * @see com.turing.common.quartz.support.JobHandlerAdaptor
 */
public interface JobSuccessCallback {

    /**
     * @param jobName   任务名称
     * @param groupName 组名称
     * @return 是否支持该job回调
     */
    boolean support(String jobName, String groupName);


    /**
     * 当任务执行成功时进行回调
     *
     * @param executionContext job 任务上下文
     */
    void success(JobDefinition jobDefinition, JobExecutionContext executionContext);

}
