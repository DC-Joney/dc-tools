package com.dc.tools.spring.quartz;

import com.dc.tools.spring.quartz.job.JobDefinition;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

/**
 * 定时任务失败回调抽象接口，用于捕捉失败回调执行出错信息
 *
 * @author zhangyang
 * @date 2020-10-12
 */
@Slf4j
public abstract class AbstractLogFailCallback implements JobFailCallback {

    public abstract String getErrorMessage();

    public abstract void failCallback(JobDefinition definition, JobExecutionContext executionContext, Throwable error);

    @Override
    public void fail(JobDefinition definition, JobExecutionContext executionContext, Throwable error) throws Exception {

        try {

            //去掉reflect 原有异常
            Throwable cause = error.getCause();
            failCallback(definition, executionContext, cause);

        } catch (Exception ex) {
            log.error(getErrorMessage() + "原因为:", ex);
        }
    }


}
