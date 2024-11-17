package com.dc.tools.spring.log2;

import com.dc.tools.spring.log2.config.Log2Config;
import com.turing.log2.context.Log2Context;
import com.turing.log2.context.Log2ThreadLocalInfo;
import com.turing.log2.entity.Log2Template;
import com.turing.log2.utils.Log2Utils;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.util.concurrent.Queues;

import java.util.Optional;

@Slf4j
@Order(-2)
@SuppressWarnings("Duplicates")
public class ReactorResultHandler implements ResultInterceptorHandler {

    private static final ReactiveAdapterRegistry registry;

    static {
        registry = ReactiveAdapterRegistry.getSharedInstance();
    }


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

        int order = 0;

        String typeName = "gt";

        long startTime = System.currentTimeMillis();

        //支持无线的被压元素，需要考虑到在整体订阅过程中如果返回源是很大，那么数据不缓存或者队列长度不够时则会丢数据
        UnicastProcessor<Object> upstream = UnicastProcessor.create(Queues.unbounded().get());

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

            if (Log2Config.debug) {
                time2 = System.nanoTime();
            }

            //执行的异常信息
            Throwable executionException = null;

            try {
                returnValue = invocation.proceed();
                ReactiveAdapter adapter = registry.getAdapter(returnValue.getClass());
                if (adapter != null) {
                    //支持其他类型的订阅源
                    Mono<Object> newValue = Mono.fromDirect(adapter.toPublisher(returnValue))
                            .doOnNext(upstream::onNext)
                            .doOnError(upstream::onError)
                            .doFinally(signalType -> upstream.onComplete());

                    return adapter.fromPublisher(newValue);
                }


            } catch (Exception cause) {
                log.error(Log2Utils.getExceptionMessage(cause, log2Context, "businessException:AOP调用方法" + enterMethod + "出错"));
                isBusinessExcept = true;
                executionException = cause;
            }

            //当执行异常时回调下游进行处理，将异常数据上报到log2
            if (isBusinessExcept) {
                upstream.onError(executionException);
                //将upstream结束掉
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

            //只获取第一个元素
            upstream.buffer()
                    .next()
                    .toFuture()
                    .whenCompleteAsync((result, ex) -> {
                        //将数据添加到log2
                        addLog2Context(finalLog2Context, startTime, ex != null, finalTypeName, methodName,
                                sourceClass.getName(), finalOrder, arguments, result, finalTime, time1);

                        //将数据上报到Log2
                        if (StringUtils.isNotBlank(finalLog2Context.inPath) && enterMethod.equals(finalLog2Context.inPath)) {
                            if (Log2Config.debug) {
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

        if (Log2Config.debug) {
            long time4 = System.nanoTime();
            System.out.println("【Log2Debug】Log2时间消耗=" + (time4 - time3 + (time2 - time1)) + "纳秒（1,000,000纳秒=1毫秒）");
        }
    }

    @Override
    public boolean support(Class<?> returnType) {
        return registry.getAdapter(returnType) != null;
    }


}
