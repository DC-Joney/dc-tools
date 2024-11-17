package com.dc.tools.spring.validate.annotation;

import java.lang.annotation.*;

/**
 * 用于标记在做Check时，是否需要级联Check
 *
 * @author zhangyang
 * @see CheckParam
 * @see CheckParams
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NestedCheck {

}
