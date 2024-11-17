package com.dc.property.bytebuddy.advice;

@FunctionalInterface
public interface ClassInterceptorEndpoint {

    ClassInterceptorDefinition getDefinition();

}
