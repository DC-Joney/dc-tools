package com.dc.tools.spring.log.config;

import com.dc.tools.spring.log.*;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

/**
 * 放置 @Bean 方法时需要格外小心，考虑将条件隔离在单独的 Configuration 类中
 * <p>
 * 也就是说最好放使用类注解，不要用方法注解，类注解spring使用了ASM解析，不会真正的加载ConditionalOnClass里面的class
 * ，即使不存在也不会报错（实际上会但是catch了）。但是放在方法上，用的是java.lang.reflect.Method.getDeclaredAnnotations(Method.java:630)来获取方法注解信息，Class不存在就报错了。
 */
@Slf4j
@Configuration
public class LogConfig {

    @Bean
    public LogMethodInterceptor log2MethodInterceptor( List<ResultInterceptorHandler> interceptorHandlers, List<MethodArgumentChanger> changers) {
        return new LogMethodInterceptor(interceptorHandlers, changers);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogAnnotationAdvisor log2AnnotationAdvisor(LogMethodInterceptor interceptor) {
        return new LogAnnotationAdvisor(interceptor);
    }

    @Bean
    public LogAspectHandler log2AspectHandler(LogSender logSender) {
        return new LogAspectHandler(logSender);
    }

    @Bean
    public GlobalIdMethodArgumentChanger globalIdMethodArgumentChanger(){
        return new GlobalIdMethodArgumentChanger();
    }

    @Bean
    public TraceIdMethodArgumentChanger traceIdMethodArgumentChanger(){
        return new TraceIdMethodArgumentChanger();
    }

    @Bean
    public CompleteFutureResultHandler completeFutureResultHandler() {
        return new CompleteFutureResultHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LogSender logSender (){
        return new LogSender() {
            @Override
            public void sendLog(LogDetail logDetail) {
                log.info("received log details is: {}", logDetail);
            }
        };
    }

    @Configuration
    @ConditionalOnClass({DeferredResult.class})
    static class DeferredResultConfig {

        @Bean
        public DeferredResultHandler deferredResultHandler(LogSender logSender) {
            return new DeferredResultHandler(logSender);
        }

    }


    @Configuration
    @ConditionalOnClass({ReactiveAdapterRegistry.class, Publisher.class})
    static class ReactorResultConfig {

        @Bean
        public ReactorResultHandler reactorResultHandler(LogSender logSender) {
            return new ReactorResultHandler( logSender);
        }

    }

    static class PatternLayoutConfig {

        static {
            try {
                Class.forName(TraceIdMDCPatternLogbackLayout.class.getName());
            } catch (ClassNotFoundException e) {
                log.error("Cannot find PatternLayout for traceId");
            }
        }

    }

}
