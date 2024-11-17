package com.dc.tools.spring.utils;

import com.dc.tools.common.utils.ClassUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * 用于获取当前对象的AOP Proxy
 *
 * @see org.springframework.context.annotation.EnableAspectJAutoProxy
 * @see org.springframework.aop.config.AopConfigUtils#forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry)
 * @see org.springframework.context.annotation.AutoProxyRegistrar
 */
@Slf4j
@UtilityClass
public class AopTargetUtils {

    /**
     * 获取当前调用的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(T originInstance) {
        try {
            if (AopUtils.isAopProxy(originInstance)) {
                return originInstance;
            }

            Object currentProxy = AopContext.currentProxy();
            Class<?> targetClass = AopUtils.getTargetClass(currentProxy);
            //如果当前AopContext中的proxy对象与当前
            if (originInstance.getClass().isAssignableFrom(targetClass)) {
                return (T) currentProxy;
            }
        } catch (IllegalStateException e) {
            log.error("Cannot find current proxy for instanceof {}, fallback to current instance", com.dc.tools.common.utils.ClassUtils.getShortName(originInstance.getClass()));
        }

        log.error("Fallback to originInstance, because aop context cannot find proxy instance");
        return originInstance;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTarget(Object proxy) {
        if (AopUtils.isAopProxy(proxy)) {
            Object singletonTarget = AopProxyUtils.getSingletonTarget(proxy);
            if (singletonTarget == null) {
                log.error("Cannot resolve current proxy for original instance {}", ClassUtils.getUserClass(proxy.getClass()));
                return (T) proxy;
            }
        }

        return (T) proxy;
    }
}
