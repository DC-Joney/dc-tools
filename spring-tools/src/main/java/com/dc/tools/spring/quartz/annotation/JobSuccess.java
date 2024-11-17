package com.dc.tools.spring.quartz.annotation;

import java.lang.annotation.*;

/**
 * 任务成功时的回调
 *
 * @author zhangyang
 * @date 2020-09-30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface JobSuccess {

    /**
     * 任务名称
     */
    String job();

    /**
     * 组名称
     */
    String group() default "";


}
