package com.dc.property.bytebuddy.advice;


import com.dc.property.bytebuddy.OverrideCallable;

import java.lang.reflect.Method;

public interface MethodInterceptor  extends Interceptor {

    /**
     * 执行的拦截器
     * @param instance instance
     * @param callable call super callable
     * @param allArguments method arguments
     * @param method call method
     */
    Object intercept(Object instance, OverrideCallable callable, Object[] allArguments, Method method) throws Exception;




}
