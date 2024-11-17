package com.dc.tools.spring.validate.annotation;

import java.lang.annotation.*;

/**
 * 通过el 内置的表达式判断参数是否合法
 *
 * @author zhangyang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CheckParams.class)
public @interface CheckParam {

    /**
     * Spring EL
     * <p/>
     *
     * 前置判断，根据前置条件判断是否需要进行检查，如果不需要，则不做任何检查
     */
    String before() default "";

    /**
     * Spring EL
     * <p>
     * 返回 true 或者false，用于校验参数是否合法
     */
    String condition();

    /**
     * 当校验结果为false时，抛出message类型的异常信息
     */
    String message() default "";

    /**
     * 当前校验的scope范围，如果不填写，默认为全部范围，如果填写则只校验scope相关的范围
     */
    Class<?>[] scope() default {};

}