package com.dc.tools.spring.quartz.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.*;
import org.springframework.core.annotation.SynthesizingMethodParameter;

import java.lang.reflect.Method;

/**
 * 执行具体方法
 *
 * @author zhangyang
 * @date 2020-09-29
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class JobMethod {

    Method method;

    Method bridgedMethod;

    @Getter
    MethodParameter[] parameters;

    @Getter
    Object bean;

    @Getter
    BeanFactory beanFactory;

    /**
     * 方法名称解析器
     */
    ParameterNameDiscoverer discoverer;

    public JobMethod(Method method, Object bean, BeanFactory beanFactory) {
        this(method, bean, beanFactory, new DefaultParameterNameDiscoverer());
    }

    public JobMethod(Method method, Object bean, BeanFactory beanFactory, ParameterNameDiscoverer discoverer) {
        this.method = method;
        this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
        this.parameters = initMethodParameters();
        this.bean = bean;
        this.beanFactory = beanFactory;
        this.discoverer = discoverer;
    }


    private MethodParameter[] initMethodParameters() {
        int count = this.bridgedMethod.getParameterTypes().length;
        MethodParameter[] result = new MethodParameter[count];
        for (int i = 0; i < count; i++) {
            MethodParameter parameter = new SynthesizingMethodParameter(method, i);
            GenericTypeResolver.resolveParameterType(parameter, this.bean.getClass());
            parameter.initParameterNameDiscovery(discoverer);
            result[i] = parameter;
        }
        return result;
    }


    Object invokeMethod(Object[] args) throws Exception {
        Object[] methodArgs = resolveArguments(args);
        return bridgedMethod.invoke(bean, methodArgs);
    }

    /**
     * 对传入的args 进行
     *
     * @return
     */
    protected abstract Object[] resolveArguments(Object[] args);


    /**
     * 返回 包装任务的实际执行方法
     */
    public Method getMethod() {
        return this.bridgedMethod;
    }

    /**
     * 返回方法名称
     */
    public String getMethodName() {
        return this.bridgedMethod.getName();
    }


    /**
     * 返回方法所在类名称
     */
    public String getSimpleClassName() {
        return this.bridgedMethod.getDeclaringClass().getSimpleName();
    }


    /**
     * 返回方法所在全类名称
     */
    public String getClassName() {
        return this.bridgedMethod.getDeclaringClass().getName();
    }

}
