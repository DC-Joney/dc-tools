package com.dc.tools.spring.quartz.job;

import com.dc.tools.spring.quartz.ProxyJob;
import org.quartz.Job;
import org.quartz.JobDetail;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * @author zhangyang
 * @date 2020-09-30
 */
public interface ExtendedJobDetail extends JobDetail {


    /**
     * @see ExtendedJobDetail#getJobSupplier()
     */
    @Override
    @Deprecated
    default Class<? extends Job> getJobClass() {
        return ProxyJob.class;
    }


    /**
     * 获取Job 对象的产生器
     *
     * @return
     */
    Supplier<Job> getJobSupplier();


    /**
     * 获取Job 对象的产生器
     *
     * @return
     */
    Method getInvokeMethod();


    static ExtendedJobDetail create(JobDetail jobDetail, Supplier<Job> supplier, Method invokeMethod) {
        return new ExtendedJobDetailImpl(jobDetail, supplier, invokeMethod);
    }
}
