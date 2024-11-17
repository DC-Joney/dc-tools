package com.dc.property.bytebuddy.advice;

/**
 * 构造器拦截器
 *
 * @author zy
 */
public interface ConstructInterceptor  extends Interceptor{

    void onConstruct(Object obj, Object[] arguments);

}
