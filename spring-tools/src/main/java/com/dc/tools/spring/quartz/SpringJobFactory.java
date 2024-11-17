package com.dc.tools.spring.quartz;

import com.dc.tools.spring.quartz.job.ExtendedJobDetail;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.function.Supplier;

/**
 * {@link org.quartz.spi.JobFactory} 的扩展，适配于Spring 注入
 *
 * @author zhangyang
 * @date 2020-09-24
 */

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpringJobFactory extends PropertySettingJobFactory implements ApplicationContextAware {

    ApplicationContext context;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {

        Job job;

        if (bundle.getJobDetail() instanceof ExtendedJobDetail) {
            Supplier<Job> jobSupplier = ((ExtendedJobDetail) bundle.getJobDetail()).getJobSupplier();
            job = jobSupplier.get();
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.putAll(scheduler.getContext());
            jobDataMap.putAll(bundle.getJobDetail().getJobDataMap());
            jobDataMap.putAll(bundle.getTrigger().getJobDataMap());
            setBeanProps(job, jobDataMap);
        } else {
            job = super.newJob(bundle, scheduler);

            //初始化生成的job任务
            AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();
            beanFactory.autowireBeanProperties(job, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
            beanFactory.initializeBean(job, job.getClass().getSimpleName());
        }

        return job;
    }
}
