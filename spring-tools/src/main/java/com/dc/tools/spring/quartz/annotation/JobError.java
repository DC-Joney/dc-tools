package com.dc.tools.spring.quartz.annotation;

import java.lang.annotation.*;

/**
 * 任务失败时的回调
 *
 * @author zhangyang
 * @date 2020-09-30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface JobError {

    /**
     * 任务名称
     */
    String job();

    /**
     * 组名称
     */
    String group() default "";


    Class<?> exceptionClass() default Exception.class;

}