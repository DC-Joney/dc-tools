package com.dc.tools.spring.quartz.support;

import com.dc.tools.spring.quartz.JobFailCallback;
import com.dc.tools.spring.quartz.JobSuccessCallback;
import com.dc.tools.spring.quartz.job.JobDefinition;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 注解 适配器
 *
 * @author zhangyang
 * @apiNote 暂时不对注解进行实现，后续再加入
 * @date 2020-09-30
 * @see com.turing.common.quartz.annotation.JobError
 * @see com.turing.common.quartz.annotation.JobSuccess
 */
public class AnnotationJobMethodAdaptor implements JobSuccessCallback, JobFailCallback {

    private final JobMethod callbackMethod;
    private final String jobName;
    private final String groupName;

    public AnnotationJobMethodAdaptor(JobMethod method, String jobName, String groupName) {
        this.callbackMethod = method;
        this.jobName = jobName;
        this.groupName = groupName;
    }

    @Override
    public boolean support(String jobName, String groupName) {
        return jobName.equals(this.jobName) && groupName.equals(this.groupName);
    }

    @Override
    public void fail(JobDefinition definition, JobExecutionContext executionContext, Throwable error) throws Exception {
        try {
            callbackMethod.invokeMethod(new Object[]{executionContext, error});
        } catch (Exception e) {
            throw new JobExecutionException("");
        }
    }

    @Override
    public void success(JobDefinition definition, JobExecutionContext executionContext) {
        try {
            callbackMethod.invokeMethod(new Object[]{executionContext});
        } catch (Exception e) {
            throw new RuntimeException("");
        }
    }

}
