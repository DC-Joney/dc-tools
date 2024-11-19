/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc.tools.notify;

import com.dc.tools.common.spi.CommonServiceLoader;
import com.dc.tools.common.utils.ClassUtils;
import com.dc.tools.common.utils.MapUtil;
import com.dc.tools.common.utils.ThreadUtils;
import com.dc.tools.notify.listener.SmartSubscriber;
import com.dc.tools.notify.listener.Subscriber;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

/**
 * Unified Event Notify Center.
 *
 * @apiNote Forked from <a href="https://github.com/alibaba/nacos">Nacos</a>.
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class NotifyCenter {

    public static int ringBufferSize;

    public static int shareBufferSize;

    private static final AtomicBoolean CLOSED = new AtomicBoolean(false);

    private static BiFunction<Class<? extends Event>, Integer, EventPublisher<? extends Event>> publisherFactory = null;

    private static final NotifyCenter INSTANCE = new NotifyCenter();

    private DefaultSharePublisher sharePublisher;

    private static Class<? extends EventPublisher> clazz = null;

    /**
     * Publisher management container.
     */
    private final Map<String, EventPublisher<? extends Event>> publisherMap = new ConcurrentHashMap<>(16);

    static {
        // Internal ArrayBlockingQueue buffer size. For applications with high write throughput,
        // this value needs to be increased appropriately. default value is 16384
        String ringBufferSizeProperty = "notify.ring-buffer-size";
        ringBufferSize = Integer.getInteger(ringBufferSizeProperty, 1 << 14);

        // The size of the public publisher's message staging queue buffer
        String shareBufferSizeProperty = "notify.share-buffer-size";
        shareBufferSize = Integer.getInteger(shareBufferSizeProperty, 1 << 10);

        final Collection<EventPublisher> publishers = CommonServiceLoader.load(EventPublisher.class).sort();
        Iterator<EventPublisher> iterator = publishers.iterator();

        if (iterator.hasNext()) {
            clazz = iterator.next().getClass();
        } else {
            clazz = DefaultPublisher.class;
        }

        publisherFactory = new BiFunction<Class<? extends Event>, Integer, EventPublisher<? extends Event>>() {

            @Override
            public EventPublisher apply(Class<? extends Event> cls, Integer buffer) {
                try {
                    EventPublisher publisher = clazz.newInstance();
                    publisher.init(cls, buffer);
                    return publisher;
                } catch (Throwable ex) {
                    log.error("Service class newInstance has error : ", ex);
                    throw new NotifyException("Service class newInstance has error : ", ex);
                }
            }
        };

        try {

            // Create and init DefaultSharePublisher instance.
            INSTANCE.sharePublisher = new DefaultSharePublisher();
            INSTANCE.sharePublisher.init(SlowEvent.class, shareBufferSize);

        } catch (Throwable ex) {
            log.error("Service class newInstance has error : ", ex);
        }

        ThreadUtils.addShutdownHook(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public static Map<String, EventPublisher<? extends Event>> getPublisherMap() {
        return INSTANCE.publisherMap;
    }

    /**
     * 获取Event对应的 publisher
     *
     * @param subscribeType event topic
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> EventPublisher<T> getPublisher(Class<? extends T> subscribeType) {
        if (ClassUtils.isAssignable(SlowEvent.class, subscribeType)) {
            return (EventPublisher<T>) INSTANCE.sharePublisher;
        }

        String topic = ClassUtils.getShortName(subscribeType);
        EventPublisher<? extends Event> publisher = INSTANCE.publisherMap.get(subscribeType.getCanonicalName());
        if (publisher == null) {
            synchronized (NotifyCenter.class) {
                // MapUtils.computeIfAbsent is a unsafe method.
                MapUtil.computeIfAbsent(INSTANCE.publisherMap, topic, publisherFactory, subscribeType, ringBufferSize);
            }
        }

        return (EventPublisher<T>) INSTANCE.publisherMap.get(topic);
    }

    /**
     * 获取Slow Event对应的 publisher
     */
    public static EventPublisher<? extends SlowEvent> getSharePublisher() {
        return INSTANCE.sharePublisher;
    }

    /**
     * Shutdown the several publisher instance which notify center has.
     */
    public static void shutdown() {
        if (!CLOSED.compareAndSet(false, true)) {
            return;
        }
        log.warn("[NotifyCenter] Start destroying Publisher");

        for (Map.Entry<String, EventPublisher<? extends Event>> entry : INSTANCE.publisherMap.entrySet()) {
            try {
                EventPublisher<? extends Event> eventPublisher = entry.getValue();
                eventPublisher.shutdown();
            } catch (Throwable e) {
                log.error("[EventPublisher] shutdown has error : ", e);
            }
        }

        try {
            INSTANCE.sharePublisher.shutdown();
        } catch (Throwable e) {
            log.error("[SharePublisher] shutdown has error : ", e);
        }

        log.warn("[NotifyCenter] Destruction of the end");
    }

    /**
     * Register a Subscriber. If the Publisher concerned by the Subscriber does not exist, then PublihserMap will
     * preempt a placeholder Publisher first.
     *
     * @param consumer subscriber
     * @param <T>      event type
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> void registerSubscriber(final Subscriber<T> consumer) {
        // If you want to listen to multiple events, you do it separately,
        // based on subclass's subscribeTypes method return list, it can register to publisher.
        if (consumer instanceof SmartSubscriber) {
            for (Class<? extends Event> subscribeType : ((SmartSubscriber) consumer).subscribeTypes()) {
                // For case, producer: defaultSharePublisher -> consumer: smartSubscriber.
                if (ClassUtils.isAssignable(SlowEvent.class, subscribeType)) {
                    Subscriber<SlowEvent> subscriber = (Subscriber<SlowEvent>) consumer;
                    INSTANCE.sharePublisher.addSubscriber(subscriber, (Class<? extends SlowEvent>) subscribeType);
                } else {
                    Class<? extends T> subscribeTypes = (Class<? extends T>) subscribeType;
                    // For case, producer: defaultPublisher -> consumer: subscriber.
                    addSubscriber(consumer, subscribeTypes);
                }
            }
            return;
        }

        Class<? extends T> subscribeType = consumer.subscribeType();
        if (ClassUtils.isAssignable(SlowEvent.class, subscribeType)) {
            Subscriber<SlowEvent> subscriber = (Subscriber<SlowEvent>) consumer;
            INSTANCE.sharePublisher.addSubscriber(subscriber, (Class<? extends SlowEvent>) subscribeType);
            return;
        }

        addSubscriber(consumer, subscribeType);
    }

    /**
     * Add a subscriber to publisher.
     *
     * @param consumer      subscriber instance.
     * @param subscribeType subscribeType.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Event> void addSubscriber(final Subscriber<T> consumer, Class<? extends T> subscribeType) {

        final String topic = ClassUtils.getShortName(subscribeType);
        synchronized (NotifyCenter.class) {
            // MapUtils.computeIfAbsent is a unsafe method.
            MapUtil.computeIfAbsent(INSTANCE.publisherMap, topic, publisherFactory, subscribeType, ringBufferSize);
        }
        EventPublisher<T> publisher = (EventPublisher<T>) INSTANCE.publisherMap.get(topic);
        publisher.addSubscriber(consumer);
    }

    /**
     * Deregister subscriber.
     *
     * @param consumer subscriber instance.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> void deregisterSubscriber(final Subscriber<T> consumer) {
        if (consumer instanceof SmartSubscriber) {
            for (Class<? extends Event> subscribeType : ((SmartSubscriber) consumer).subscribeTypes()) {
                if (ClassUtils.isAssignable(SlowEvent.class, subscribeType)) {
                    Subscriber<SlowEvent> subscriber = (Subscriber<SlowEvent>) consumer;
                    INSTANCE.sharePublisher.addSubscriber(subscriber, (Class<? extends SlowEvent>) subscribeType);
                } else {
                    Class<? extends T> subscribeTypes = (Class<? extends T>) subscribeType;
                    removeSubscriber(consumer, subscribeTypes);
                }
            }
            return;
        }

        final Class<? extends T> subscribeType = consumer.subscribeType();
        if (ClassUtils.isAssignable(SlowEvent.class, subscribeType)) {
            Subscriber<SlowEvent> subscriber = (Subscriber<SlowEvent>) consumer;
            INSTANCE.sharePublisher.addSubscriber(subscriber, (Class<? extends SlowEvent>) subscribeType);
            return;
        }

        if (removeSubscriber(consumer, subscribeType)) {
            return;
        }
        throw new NoSuchElementException("The subscriber has no event publisher");
    }

    /**
     * Remove subscriber.
     *
     * @param consumer      subscriber instance.
     * @param subscribeType subscribeType.
     * @return whether remove subscriber successfully or not.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Event> boolean removeSubscriber(final Subscriber<T> consumer, Class<? extends T> subscribeType) {
        final String topic = ClassUtils.getShortName(subscribeType);
        EventPublisher<T> eventPublisher = (EventPublisher<T>) INSTANCE.publisherMap.get(topic);
        if (eventPublisher != null) {
            eventPublisher.removeSubscriber(consumer);
            return true;
        }
        return false;
    }

    /**
     * Request publisher publish event Publishers load lazily, calling publisher. Start () only when the event is
     * actually published.
     *
     * @param event class Instances of the event.
     */
    public static <T extends Event> boolean publishEvent(final T event) {
        try {
            return publishEvent(event.getClass(), event);
        } catch (Throwable ex) {
            log.error("There was an exception to the message publishing : ", ex);
            return false;
        }
    }

    /**
     * Request publisher publish event Publishers load lazily, calling publisher.
     *
     * @param eventType class Instances type of the event type.
     * @param event     event instance.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Event> boolean publishEvent(final Class<? extends T> eventType, final T event) {
        if (ClassUtils.isAssignable(SlowEvent.class, eventType)) {
            return INSTANCE.sharePublisher.publish((SlowEvent) event);
        }

        final String topic = ClassUtils.getShortName(eventType);

        EventPublisher<T> publisher = (EventPublisher<T>) INSTANCE.publisherMap.get(topic);
        if (publisher != null) {
            return publisher.publish(event);
        }
        log.warn("There are no [{}] publishers for this event, please register", topic);
        return false;
    }


    /**
     * Register publisher.
     *
     * @param eventType    class Instances type of the event type.
     * @param queueMaxSize the publisher's queue max size.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> EventPublisher<T> registerToPublisher(final Class<? extends T> eventType, final int queueMaxSize) {
        if (ClassUtils.isAssignable(SlowEvent.class, eventType)) {
            return (EventPublisher<T>) INSTANCE.sharePublisher;
        }

        final String topic = ClassUtils.getShortName(eventType);
        synchronized (NotifyCenter.class) {
            // MapUtils.computeIfAbsent is a unsafe method.
            MapUtil.computeIfAbsent(INSTANCE.publisherMap, topic, publisherFactory, eventType, queueMaxSize);
        }

        return (EventPublisher<T>) INSTANCE.publisherMap.get(topic);
    }

    /**
     * Deregister publisher.
     *
     * @param eventType class Instances type of the event type.
     */
    public static <T extends Event> void deregisterPublisher(final Class<T> eventType) {
        final String topic = ClassUtils.getShortName(eventType);
        EventPublisher<?> publisher = INSTANCE.publisherMap.remove(topic);
        try {
            publisher.shutdown();
        } catch (Throwable ex) {
            log.error("There was an exception when publisher shutdown : ", ex);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        EventPublisher<Event> publisher = NotifyCenter.getPublisher(Event.class);

        publisher.addSubscriber(new Subscriber<Event>() {
            @Override
            public void onEvent(Event event) {
                System.out.println(event.sequence());
            }

            @Override
            public Class<? extends Event> subscribeType() {
                return Event.class;
            }
        });

        TimeUnit.SECONDS.sleep(10);
    }

}
