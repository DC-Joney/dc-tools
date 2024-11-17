package com.dc.tools.spring.validate.annotation;

import java.lang.annotation.*;

/**
 * 用于为特定的字段注入相应的值
 *
 * @author zhangyang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ParamValues.class)
public @interface ParamValue {


    /**
     * 将Spel表达式的值设置到对应的字段中
     */
    String value();


    /**
     * 用于判断是否应该注入对应的值, 默认值为空，表示condition成立
     */
    String condition() default "";


    /**
     * 当字段赋值完成时，需要校验，或者直接在Field上通过CheckParam校验
     */
    CheckParam[] checks() default {};

    /**
     * 是否当字段值为null的时候才进行注入
     * <p>
     * false: 不管字段值是否为null，都会进行覆盖
     * <p>
     * true: 当field == null时，或者field字段类型为{@linkplain String}, 并且字段值为空字符串时才会进行注入
     */
    boolean whenNull() default false;


    /**
     * 在 check 之前注入值，还是在check完成以后再进行注入
     */
    boolean checkBefore() default true;

    /**
     * 注入数据时注意的范围11
     */
    Class<?>[] scope() default {};

}
