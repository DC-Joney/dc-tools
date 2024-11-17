package com.dc.property.bytebuddy;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InterceptorDescriptor {

    /**
     * void.class 既为默认所有添加拦截, 否则只针对对应的class
     */
    Class<?> value() default void.class;




}
