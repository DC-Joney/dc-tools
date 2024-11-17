package com.dc.tools.spring.bean;

import java.lang.annotation.*;

/**
 * 字段别名，用于 {@code copyProperty} 时，忽略注入属性值，不区分来源
 * @author zhangyang
 *
 * @see CopyIgnore
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CopyIgnoreAll {
}
