package com.dc.tools.spring.log;

import com.dc.tools.common.utils.SystemClock;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogAspectHandler extends AbstractLogResultInterceptorHandler {

    private final LogSender logSender;

    @Override
    protected Object handleResult(LogDetail.LogDetailBuilder builder, MethodInvocationArguments invocationArguments) throws Throwable {
        //判断是否发生异常
        boolean isBusinessExcept = false;

        Object returnValue = null;
        MethodInvocation invocation = invocationArguments.getInvocation();
        long startTime = SystemClock.now();

        CompletableFuture<Object> future = new CompletableFuture<>();

        try {
            //执行的异常信息
            Throwable executionException = null;

            try {
                returnValue = invocation.proceed();
                future.complete(returnValue);
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
                //将数据添加到log2
                long endTime = SystemClock.now();
                //方法执行耗时
                builder.timeUsed(startTime - endTime);

                if (ex != null) {
                    builder.sourceType(SourceType.EXCEPTION);
                }

                builder.result(result)
                        .exception(ex);
                logSender.sendLog(builder.build());

            });


        }

        return returnValue;

    }

    @Override
    public boolean support(Class<?> returnType) {
        return true;
    }
}
