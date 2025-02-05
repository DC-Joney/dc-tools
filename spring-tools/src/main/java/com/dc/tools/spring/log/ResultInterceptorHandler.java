package com.dc.tools.spring.log;

public interface ResultInterceptorHandler {

    Object handleResult(MethodInvocationArguments invocationArguments) throws Throwable;

    default boolean support(Class<?> returnType) {
        return true;
    };
}
