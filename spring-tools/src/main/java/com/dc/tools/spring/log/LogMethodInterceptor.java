package com.dc.tools.spring.log;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.List;

@Slf4j
public class LogMethodInterceptor implements MethodInterceptor, InitializingBean {

    private final List<ResultInterceptorHandler> handlers;

    private final MethodArgumentsChain chain;

    public LogMethodInterceptor(List<ResultInterceptorHandler> handlers, List<MethodArgumentChanger> changers) {
        this.handlers = handlers;
        this.chain = new MethodArgumentsChain(changers);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        AnnotationAwareOrderComparator.sort(handlers);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        //对参数进行修改方便记录进log2日志
        MethodInvocationArguments invocationArguments = chain.doChange(invocation);
        return handlers.stream()
                .filter(handler -> handler.support(invocationArguments.getReturnType()))
                .findFirst()
                .orElseGet(() -> arguments -> invocation.proceed())
                .handleResult(invocationArguments);
    }
}
