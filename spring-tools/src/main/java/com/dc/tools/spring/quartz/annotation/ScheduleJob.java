package com.dc.tools.spring.quartz.annotation;

import java.lang.annotation.*;

/**
 * 定时任务注解, 将方法适配为定时任务
 *
 * @author zhangyang
 * @date 2020-09-29
 * @see com.dc.tools.spring.quartz.support.ScheduleJobAnnotationPostProcessor
 * @see ScheduleJob
 * @see com.dc.tools.spring.quartz.support.JobMethod
 * @see com.dc.tools.spring.quartz.job.JobDefinition
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduleJob {

    /**
     * job 名称
     */
    String name() default "";


    /**
     * job 任务描述
     */
    String description() default "";

    /**
     * Trigger名称，如果为空 默认为 'className + methodName + {@link ScheduleJob#TRIGGER_SUFFIX}'
     */
    String trigger() default "";


    /**
     * Job组名称，如果为空 默认为 'jobName + {@link ScheduleJob#JOB_GROUP_SUFFIX}'
     */
    String jobGroup() default "";


    /**
     * Trigger组名称，如果为空 默认为 'triggerName + {@link ScheduleJob#TRIGGER_GROUP_SUFFIX}'
     */
    String triggerGroup() default "";


    /**
     * 定时任务表达式
     */
    String cron() default "";


    /**
     * 当不在注解中设置以上信息时，可以设置引用 jobBean的名称
     */
    String jobRef() default "";


    /**
     * 当不在注解中设置以上信息时，可以设置引用 triggerBean的名称
     */
    String triggerRef() default "";


    String JOB_GROUP_SUFFIX = "_JOB_GROUP";

    String TRIGGER_SUFFIX = "_TRIGGER";

    String TRIGGER_GROUP_SUFFIX = "_TRIGGER_GROUP";

}
