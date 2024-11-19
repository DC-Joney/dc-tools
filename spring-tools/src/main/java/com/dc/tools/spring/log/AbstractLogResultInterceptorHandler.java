package com.dc.tools.spring.log;

import com.dc.tools.common.SequenceUtil;
import com.dc.tools.common.utils.SystemClock;
import com.dc.tools.spring.log.annotation.Log;
import com.dc.tools.spring.log.annotation.LogIgnore;
import com.google.common.collect.Lists;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.MethodParameter;

import java.util.List;

public abstract class AbstractLogResultInterceptorHandler implements ResultInterceptorHandler{


    @Override
    public Object handleResult(MethodInvocationArguments invocationArguments) throws Throwable {
        MethodInvocation invocation = invocationArguments.getInvocation();

        LogDetail.LogDetailBuilder builder = LogDetail.builder();
        //方法执行class
        Class<?> sourceClass = invocation.getThis().getClass();
        //方法入参
        Object[] arguments = invocationArguments.changedArguments();
        MethodParameter[] methodParameters = invocationArguments.getMethodParameters();
        List<Object> argumentsList = Lists.newArrayList(arguments);

        for (MethodParameter methodParameter : methodParameters) {
            if (!methodParameter.hasParameterAnnotation(LogIgnore.class)) {
                //删除需要被忽略的参数
                argumentsList.remove(methodParameter.getParameterIndex());
            }
        }

        Log logAnnotation = invocationArguments.getMethodAnnotation(Log.class);
        builder.methodName(invocation.getMethod().getName())
                .arguments(argumentsList)
                .className(sourceClass.getName())
                .time(SystemClock.now())
                .id(SequenceUtil.nextId())
                .methodType(logAnnotation.type())
                .threadId(Thread.currentThread().getId());


        return handleResult(builder, invocationArguments);
    }


    protected abstract Object handleResult(LogDetail.LogDetailBuilder builder, MethodInvocationArguments arguments) throws Throwable;
}
