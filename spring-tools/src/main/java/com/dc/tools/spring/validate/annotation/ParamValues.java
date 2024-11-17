package com.dc.tools.spring.validate.annotation;

import java.lang.annotation.*;

/**
 * 用于为特定的字段注入相应的值
 *
 * @author zhangyang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamValues {
    ParamValue[] value() default {};

}
