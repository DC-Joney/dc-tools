package com.dc.tools.spring.quartz.job;

import lombok.Data;
import org.quartz.*;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * {@link JobDetail} 的扩展子类
 *
 * @author zhangyang
 * @date 2020-09-30
 */
@Data
public class ExtendedJobDetailImpl implements ExtendedJobDetail {

    private Supplier<Job> jobSupplier;

    private JobDetail delegate;

    private Method invokeMethod;

    public ExtendedJobDetailImpl(JobDetail jobDetail) {
        this(jobDetail, null, null);
    }

    public ExtendedJobDetailImpl(JobDetail jobDetail, Supplier<Job> supplier, Method jobMethod) {
        this.jobSupplier = initJobSupplier(jobDetail, supplier);
        this.delegate = jobDetail;
        this.invokeMethod = jobMethod;
    }

    /**
     * 初始化Job Supplier
     */
    private Supplier<Job> initJobSupplier(JobDetail jobDetail, Supplier<Job> supplier) {
        Supplier<Job> jobSupplier = supplier;
        if (jobSupplier == null) {
            if (jobDetail instanceof ExtendedJobDetail) {
                jobSupplier = ((ExtendedJobDetail) jobDetail).getJobSupplier();
            } else {
                jobSupplier = () -> {
                    try {
                        return jobDetail.getJobClass().newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalArgumentException(e);
                    }
                };
            }
        }
        return jobSupplier;
    }


    @Override
    public Supplier<Job> getJobSupplier() {
        return jobSupplier;
    }


    @Override
    public JobKey getKey() {
        return delegate.getKey();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public JobDataMap getJobDataMap() {
        return delegate.getJobDataMap();
    }

    @Override
    public boolean isDurable() {
        return delegate.isDurable();
    }

    @Override
    public boolean isPersistJobDataAfterExecution() {
        return delegate.isPersistJobDataAfterExecution();
    }

    @Override
    public boolean isConcurrentExectionDisallowed() {
        return delegate.isConcurrentExectionDisallowed();
    }

    @Override
    public boolean requestsRecovery() {
        return delegate.requestsRecovery();
    }

    @Override
    public JobBuilder getJobBuilder() {
        return delegate.getJobBuilder();
    }

    @Override
    public Object clone() {
        return new ExtendedJobDetailImpl((JobDetail) delegate.clone(), jobSupplier, invokeMethod);
    }
}
