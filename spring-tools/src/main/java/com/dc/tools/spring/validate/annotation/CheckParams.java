package com.dc.tools.spring.validate.annotation;

import java.lang.annotation.*;

/**
 * Repeat with {@link CheckParam}
 *
 * @author zhangyang
 * @see CheckParam
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckParams {

    CheckParam[] value();
}
