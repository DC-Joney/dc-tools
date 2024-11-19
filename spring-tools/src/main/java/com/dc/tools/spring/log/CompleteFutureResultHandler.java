package com.dc.tools.spring.log;

import com.dc.tools.common.utils.SystemClock;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.Order;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Order(100)
@SuppressWarnings("Duplicates")
public  class CompleteFutureResultHandler extends AbstractLogResultInterceptorHandler {

    private LogSender logSender;

    @Override
    protected Object handleResult(LogDetail.LogDetailBuilder builder, MethodInvocationArguments invocationArguments) throws Throwable {
        //判断是否发生异常
        boolean isBusinessExcept = false;

        Object returnValue = null;
        MethodInvocation invocation = invocationArguments.getInvocation();


        CompletableFuture<Object> future = new CompletableFuture<>();
        long startTime = SystemClock.now();

        try {

            //执行的异常信息
            Throwable executionException = null;

            try {
                returnValue = invocation.proceed();
                CompletableFuture<?> resultFuture = (CompletableFuture<?>) returnValue;

                resultFuture.whenComplete((result, ex)-> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                        return;
                    }

                    future.complete(result);
                });


            } catch (Exception cause) {
                isBusinessExcept = true;
                executionException = cause;
            }

            //当执行异常时回调下游进行处理，将异常数据上报到log2
            if (isBusinessExcept) {
                future.completeExceptionally(executionException);
                throw executionException;
            }


        } finally {

            future.whenCompleteAsync((result, ex) -> {
                long endTime = SystemClock.now();
                //方法执行耗时
                builder.timeUsed(startTime - endTime);

                if (ex != null) {
                    builder.sourceType(SourceType.EXCEPTION);
                }

                builder.result(result);
                logSender.sendLog(builder.build());
            });

        }

        return returnValue;
    }


    @Override
    public boolean support(Class<?> returnType) {
        return CompletableFuture.class.isAssignableFrom(returnType);
    }




}
