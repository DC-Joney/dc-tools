package com.dc.tools.spring.validate.annotation;

import java.lang.annotation.*;

/**
 * 通过el内置的方法校验，参数是否合法，无返回值
 *
 * @author zhangyang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckMethod {

    /**
     * Spring EL
     *
     * 通过方法校验是否合法
     */
    String method();

    Class<?>[] scope() default {};
}