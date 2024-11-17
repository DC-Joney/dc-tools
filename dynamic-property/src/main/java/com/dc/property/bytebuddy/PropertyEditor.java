package com.dc.property.bytebuddy;

import java.lang.annotation.*;

/**
 * 用于标识什么 bean property 在修改时需要进行回调
 *
 * @author zy
 * @see SetterMethodInstanceInterceptor
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PropertyEditor {
}
