package com.dc.tools.spring.log2;

import org.aopalliance.aop.Advice;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

public class LogAnnotationAdvisor extends StaticMethodMatcherPointcutAdvisor {

    public LogAnnotationAdvisor(Advice advice) {
        super(advice);
    }

    @Override
    public boolean matches(@NonNull Method method, Class<?> targetClass) {
        return AnnotationUtils.findAnnotation(method, Log.class) != null
                || AnnotationUtils.findAnnotation(method, LogTrace.class) != null;
    }


}
