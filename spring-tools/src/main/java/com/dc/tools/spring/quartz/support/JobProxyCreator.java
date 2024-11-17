package com.dc.tools.spring.quartz.support;

import com.dc.tools.spring.quartz.ProxyJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 生成Job对象代理
 *
 * @author zhangyang
 * @date 2020-09-29
 */
public class JobProxyCreator {

    /**
     * 创建一个新的job代理对象
     */
    public static Job createJob(JobHandlerAdaptor jobHandlerAdaptor, PlatformTransactionManager transactionManager) {
        return (Job) Proxy.newProxyInstance(jobHandlerAdaptor.getClass().getClassLoader()
                , new Class[]{ProxyJob.class, Serializable.class}, new JobInvocationHandler(jobHandlerAdaptor, transactionManager));
    }


    static class JobInvocationHandler implements InvocationHandler {

        private final JobHandlerAdaptor jobHandlerAdaptor;
        private final PlatformTransactionManager transactionManager;

        JobInvocationHandler(JobHandlerAdaptor jobHandlerAdaptor, PlatformTransactionManager transactionManager) {
            this.jobHandlerAdaptor = jobHandlerAdaptor;
            this.transactionManager = transactionManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            switch (method.getName()) {
                case "equals":
                    // Only consider equal when proxies are identical.
                    return (proxy == args[0]);
                case "hashCode":
                    // Use hashCode of Job proxy.
                    return hashCode();
                case "toString":
                    return String.format("Job proxy %s for target [ %s ]", jobHandlerAdaptor.getJobDefinition().getJobName(),
                            jobHandlerAdaptor.getMethodName());
                case "execute":
                    JobExecutionContext executionContext = null;
                    for (Object arg : args) {
                        if (arg instanceof JobExecutionContext) {
                            executionContext = (JobExecutionContext) arg;
                            break;
                        }
                    }

                    jobHandlerAdaptor.executeJob(args, executionContext);
                default:
                    throw new UnsupportedOperationException("Can not support other operator!");
            }

        }

    }
}
