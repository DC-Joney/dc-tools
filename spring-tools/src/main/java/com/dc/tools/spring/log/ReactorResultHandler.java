package com.dc.tools.spring.log;

import com.dc.tools.common.utils.SystemClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.util.concurrent.Queues;

@Slf4j
@Order(-2)
@SuppressWarnings("Duplicates")
@RequiredArgsConstructor
public class ReactorResultHandler extends AbstractLogResultInterceptorHandler {

    private static final ReactiveAdapterRegistry registry;

    static {
        registry = ReactiveAdapterRegistry.getSharedInstance();
    }

    private final LogSender logSender;

    @Override
    protected Object handleResult(LogDetail.LogDetailBuilder builder, MethodInvocationArguments arguments) throws Throwable {
        builder.sourceType(SourceType.ASYNC);

        //支持无线的被压元素，需要考虑到在整体订阅过程中如果返回源是很大，那么数据不缓存或者队列长度不够时则会丢数据
        UnicastProcessor<Object> upstream = UnicastProcessor.create(Queues.unbounded().get());

        MethodInvocation invocation = arguments.getInvocation();
        Object returnValue = null;
        boolean isBusinessExcept = false;

        long startTime = SystemClock.now();

        try {

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
                isBusinessExcept = true;
                executionException = cause;
            }

            //当执行异常时回调下游进行处理，将异常数据上报到log2
            if (isBusinessExcept) {
                upstream.onError(executionException);
                //将upstream结束掉
                throw executionException;
            }

        } finally {

            //只获取第一个元素
            upstream.buffer()
                    .next()
                    .toFuture()
                    .whenCompleteAsync((result, ex) -> {

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
        return registry.getAdapter(returnType) != null;
    }


}
