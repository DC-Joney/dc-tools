package com.dc.tools.spring.log2;

import com.dc.tools.spring.log2.config.Log2Config;
import com.turing.log2.context.Log2Context;
import com.turing.log2.context.Log2ThreadLocalInfo;
import com.turing.log2.entity.Log2Template;
import com.turing.log2.utils.Log2Utils;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Order(100)
@SuppressWarnings("Duplicates")
public  class CompleteFutureResultHandler implements ResultInterceptorHandler {


    @Override
    public Object handleResult(MethodInvocationArguments invocationArguments) throws Throwable {
        long time1 = 0L;
        long time2 = 0L;
        MethodInvocation invocation = invocationArguments.getInvocation();

        Object returnValue = null;
        Log2Context log2Context = null;
        Class<?> sourceClass = invocation.getThis().getClass();
        String methodName = invocation.getMethod().getName();
        //入口方法
        String enterMethod = sourceClass.getName() + "." + methodName;
        //判断是否发生异常
        boolean isBusinessExcept = false;

        Object[] arguments = invocationArguments.changedArguments();

        CompletableFuture<Object> future = new CompletableFuture<>();
        int order = 0;

        String typeName = "gt";

        long startTime = System.currentTimeMillis();
        try {
            log2Context = new Log2Context();
            if (log2Context.localId == 0L) {
                log2Context.localId = Log2Utils.createId();
            }

            order = log2Context.order++;

            typeName = Optional.ofNullable(log2Context.inPath)
                    .filter(StringUtils::isNotBlank)
                    .map(path -> "async").orElse("async");

            if (StringUtils.isBlank(log2Context.inPath)) {
                log2Context.inPath = enterMethod;
            }

            if (com.turing.log2.entity.Log2Config.debug) {
                time2 = System.nanoTime();
            }

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
                log.error(Log2Utils.getExceptionMessage(cause, log2Context, "businessException:AOP调用方法" + enterMethod + "出错"));
                isBusinessExcept = true;
                executionException = cause;
            }

            //当执行异常时回调下游进行处理，将异常数据上报到log2
            if (isBusinessExcept) {
                future.completeExceptionally(executionException);
                throw executionException;
            }


        } catch (Exception cause) {
            log.error(Log2Utils.getExceptionMessage(cause, log2Context, "AOP切面方法出错2，该方法正在调用" + enterMethod));

            //如果存在异常继续抛出异常
            if (isBusinessExcept) {
                throw cause;
            }
        } finally {

            Log2Context finalLog2Context = log2Context;
            long finalTime = time2;
            int finalOrder = order;
            String finalTypeName = typeName;

            future.whenCompleteAsync((result, ex) -> {
                //将数据添加到log2
                addLog2Context(finalLog2Context, startTime, ex != null, finalTypeName, methodName,
                        sourceClass.getName(), finalOrder, arguments, result, finalTime, time1);

                //将数据上报到Log2
                if (StringUtils.isNotBlank(finalLog2Context.inPath) && enterMethod.equals(finalLog2Context.inPath)) {
                    if (com.turing.log2.entity.Log2Config.debug) {
                        System.out.println("【Log2Debug】日志发送:" + finalLog2Context.toString());
                    }

                    finalLog2Context.send(finalLog2Context);
                }

            });

            //清除所有Log2Context的资源
            Log2ThreadLocalInfo.logSession.remove();
        }

        return returnValue;
    }

    public void addLog2Context(Log2Context log2Context, long aspectTime, boolean isBusinessExcept, String typeName, String methodName, String sourceClass
            , int order, Object[] arguments, Object returnValue, long time2, long time1) {

        long time3 = 0;

        long seqTime = Log2Utils.getSeqTime(log2Context);
        long time = log2Context.time;
        long timeUsed = System.currentTimeMillis() - aspectTime;

        if (Log2Config.debug) {
            time3 = System.nanoTime();
        }

        String logTypeName = isBusinessExcept ? typeName + "-except" : typeName;

        Log2Template log2Template = log2Context.aspectBulid(log2Context, sourceClass,
                methodName, logTypeName, order + "," + log2Context.order++, time,
                seqTime, timeUsed, arguments, returnValue);

        log2Context.save(log2Template);

        if (com.turing.log2.entity.Log2Config.debug) {
            long time4 = System.nanoTime();
            System.out.println("【Log2Debug】Log2时间消耗=" + (time4 - time3 + (time2 - time1)) + "纳秒（1,000,000纳秒=1毫秒）");
        }
    }

    @Override
    public boolean support(Class<?> returnType) {
        return CompletableFuture.class.isAssignableFrom(returnType);
    }




}
