package com.dc.tools.common.spi;

import java.lang.annotation.*;

/**
 * 用于标识 ServiceLoader API instance load
 * <p/>
 * Fork from Soft-JRaft
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    String name() default "";

    int priority() default 0;
}