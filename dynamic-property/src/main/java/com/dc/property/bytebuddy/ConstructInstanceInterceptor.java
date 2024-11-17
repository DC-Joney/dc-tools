package com.dc.property.bytebuddy;

/**
 * 构造器拦截器
 *
 * @author zy
 */
public interface ConstructInstanceInterceptor extends InstanceInterceptor {

    void onConstruct(Object obj, Object[] arguments);

}
