package com.dc.property.bytebuddy;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Method;

/**
 * 方法拦截器
 *
 * @author zy
 */
public interface MethodInstanceInterceptor extends InstanceInterceptor {

    /**
     * 执行的拦截器
     * @param instance instance
     * @param callable call super callable
     * @param allArguments method arguments
     * @param method call method
     */
    Object intercept(DynamicInstance instance, OverrideCallable callable, Object[] allArguments, Method method) throws Exception;


    /**
     * 被拦截的method
     */
    ElementMatcher.Junction<MethodDescription> methodMatcher();



}
