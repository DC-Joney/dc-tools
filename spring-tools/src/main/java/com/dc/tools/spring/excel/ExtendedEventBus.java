package com.dc.tools.spring.excel;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 扩展自 {@link EventBus}
 * <p>
 * 扩展 EventBus 内部类方法，包括 {@link ExtendedEventBus#isRegistered(Class)}
 * 在原有类内部由于类都是包级别的，所以所有访问都是无法获取的
 *
 * @author zhangyang
 * @date 2020-10-12
 */
@Slf4j
@GwtIncompatible
public class ExtendedEventBus extends EventBus {

    private static final Object LOCK = new Object();
    private static final String SUBSCRIBER_FIELD_NAME = "subscribers";
    private static final String SUBSCRIBER_METHOD_NAME = "getSubscribersForTesting";

    /**
     * 内部引用 {@link EventBus#subscribers} 对象
     */
    @Getter(AccessLevel.PROTECTED)
    private Object subscribers;

    /**
     * 内部引用 {@link com.google.common.eventbus.SubscriberRegistry#subscribers}  对象
     */
    @Getter(AccessLevel.PROTECTED)
    private Map subscribersMap;

    /**
     * 内部方法调用 {@code Method}
     */
    @Getter(AccessLevel.PROTECTED)
    private static volatile Method method;


    public ExtendedEventBus() {
        this("Default EventBus");
    }

    public ExtendedEventBus(String identifier) {
        super(identifier);
        init();
    }

    public ExtendedEventBus(SubscriberExceptionHandler exceptionHandler) {
        super(exceptionHandler);
        init();
    }

    private void init() {
        try {

            Field field = EventBus.class.getDeclaredField(SUBSCRIBER_FIELD_NAME);
            ReflectionUtils.makeAccessible(field);
            subscribers = field.get(this);
            if (subscribers == null) {
                throw new UnsupportedOperationException("The EventBus field " + SUBSCRIBER_FIELD_NAME + "api version is changed");
            }

            if (subscribersMap == null) {
                Field subscribersMapField = ReflectionUtils.findField(subscribers.getClass(), SUBSCRIBER_FIELD_NAME);

                if (subscribersMapField == null) {
                    throw new UnsupportedOperationException("The SubscriberRegistry field " + SUBSCRIBER_FIELD_NAME + "api version is changed");
                }
                ReflectionUtils.makeAccessible(subscribersMapField);
                subscribersMap = (Map) subscribersMapField.get(subscribers);
            }

            if (method == null) {
                synchronized (LOCK) {
                    //如果 method 不为空
                    if (method == null) {
                        method = ReflectionUtils.findMethod(subscribers.getClass(), SUBSCRIBER_METHOD_NAME, Class.class);
                        if (method != null)
                            ReflectionUtils.makeAccessible(method);
                    }
                }
                if (method == null) {
                    throw new UnsupportedOperationException("The SubscriberRegistry :" + SUBSCRIBER_METHOD_NAME + " api version is changed");
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new IllegalArgumentException("Cannot find field for " + SUBSCRIBER_FIELD_NAME + ", api version is changed");
        }
    }


    /**
     * 判断该class 是否在{@code EventBus} 中注册过
     *
     * @param classKey 注册过的类名称
     * @apiNote no synchronization
     */
    public boolean isRegistered(Class<?> classKey) {
        Object result = ReflectionUtils.invokeMethod(method, subscribers, classKey);
        if (result instanceof Set) {
            return !((Set) result).isEmpty();
        }

        throw new UnsupportedOperationException("The api version is changed");
    }

    /**
     * 当卸载EventBus的时候不允许其他任务操作
     *
     * @param event 提交的event事件
     */
    @Override
    public void post(Object event) {
        super.post(event);
    }


    /**
     * 卸载EventBus 内部所有的事件对象
     */
    public void unregisterAll() {
        log.warn("The operation will be clear all event bus object");
        subscribersMap.clear();
    }


    /**
     * 返回注册到EventBus 中的事件对象 数量
     */
    public int registerSize() {
        return subscribersMap.size();
    }

}
