package com.dc.tools.common.annotaion;

import java.lang.annotation.*;

/**
 * 用于标注测试的Annotation
 *
 * Fork from <a href="https://github.com/sofastack/sofa-jraft">Soft-Jraft</a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface JustForTest {
}