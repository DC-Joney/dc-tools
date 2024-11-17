package com.dc.tools.spring.validate.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 对前端传入的参数进行校验，不过只是针对@RequestBody类型 请求参数
 *
 * @see com.dc.tools.spring.validate.mvc.CheckAfterRequestAdvice
 * @see com.dc.tools.spring.validate.CheckExpressions
 *
 * @author zhangyang
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Check {

    /**
     * 校验的分组，请见 {@linkplain com.dc.tools.spring.validate.CheckExpressions#check(Object, Class[])}
     */
    @AliasFor("groups")
    Class<?>[] value() default {};

    /**
     * 校验的分组，请见 {@linkplain com.dc.tools.spring.validate.CheckExpressions#check(Object, Class[])}
     */
    @AliasFor("value")
    Class<?>[] groups() default {};

}