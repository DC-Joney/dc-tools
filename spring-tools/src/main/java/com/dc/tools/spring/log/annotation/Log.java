package com.dc.tools.spring.log.annotation;

import com.dc.tools.spring.log.MethodType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    MethodType type() default MethodType.LOCAL;

}
