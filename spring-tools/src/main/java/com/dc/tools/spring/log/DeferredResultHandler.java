package com.dc.tools.spring.log;

import com.dc.tools.common.utils.SystemClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Order(-1)
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class DeferredResultHandler extends AbstractLogResultInterceptorHandler {

    private final LogSender sender;


    @Override
    protected Object handleResult(LogDetail.LogDetailBuilder builder, MethodInvocationArguments arguments) throws Throwable{

        //判断是否发生异常
        boolean isBusinessExcept = false;

        Object returnValue = null;
        MethodInvocation invocation = arguments.getInvocation();


        CompletableFuture<Object> future = new CompletableFuture<>();
        long startTime = SystemClock.now();
        try {

            //执行的异常信息
            Throwable executionException = null;

            try {
                returnValue = invocation.proceed();
                DeferredResult<?> deferredResult = (DeferredResult<?>) returnValue;
                deferredResult.onCompletion(() -> {
                    if (deferredResult.getResult() != null) {
                        Object result = deferredResult.getResult();
                        if (Throwable.class.isAssignableFrom(result.getClass())) {
                            future.completeExceptionally((Throwable) result);
                            return;
                        }

                        future.complete(deferredResult.getResult());
                    }

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
                sender.sendLog(builder.build());
            });
        }

        return returnValue;
    }



    @Override
    public boolean support(Class<?> returnType) {
        return DeferredResult.class.isAssignableFrom(returnType);
    }


}
