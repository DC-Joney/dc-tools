package com.dc.tools.spring.bean;

import java.lang.annotation.*;

/**
 * 字段别名，用于 {@code copyProperty} 时，忽略注入相应属性值，区分来源
 * @author zhangyang
 * @see CopyIgnoreAll
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CopyIgnores.class)
public @interface CopyIgnore {

    /**
     * 源目标Class
     */
    Class<?> source();
}
