package com.dc.tools.spring.bean;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 字段别名，用于 {@code copyProperty} 时，注入不同来源的属性值
 * @author zhangyang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CopyAliases.class)
public @interface CopyAlias {

    /**
     * 字段别名
     */
    @AliasFor("name")
    String value() default "";


    @AliasFor("value")
    String name() default "";

    /**
     * 源目标Class
     */
    Class<?> source() default Void.class;
}
