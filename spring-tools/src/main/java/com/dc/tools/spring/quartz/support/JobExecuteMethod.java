package com.dc.tools.spring.quartz.support;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;

/**
 * 包装实际的 任务执行方法
 *
 * @author zhangyang
 * @date 2020-09-30
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobExecuteMethod extends JobMethod {

    @Getter
    String beanName;

    public JobExecuteMethod(Method method, Object bean, BeanFactory beanFactory, ParameterNameDiscoverer discoverer, String beanName) {
        super(method, bean, beanFactory, discoverer);
        this.beanName = beanName;
    }


    @Override
    protected Object[] resolveArguments(Object[] args) {

        Object[] methodArgs = new Object[getParameters().length];

        for (int i = 0; i < getParameters().length; i++) {
            MethodParameter parameter = getParameters()[i];
            for (Object methodArg : args) {
                if (parameter.getParameterType().isAssignableFrom(methodArg.getClass())) {
                    methodArgs[i] = methodArg;
                }
            }
        }

        return methodArgs;
    }
}
