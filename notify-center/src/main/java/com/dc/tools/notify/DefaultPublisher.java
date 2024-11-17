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


import com.dc.tools.common.collection.ConcurrentHashSet;
import com.dc.tools.common.utils.CollectionUtils;
import com.dc.tools.notify.listener.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.dc.tools.notify.NotifyCenter.ringBufferSize;


/**
 * The default event publisher implementation.
 *
 * <p>Internally, use {@link ArrayBlockingQueue <Event/>} as a message staging queue.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 * @author zongtanghu
 * @apiNote Forked from <a href="https://github.com/alibaba/nacos">Nacos</a>.
 */
public class DefaultPublisher<T extends Event> extends Thread implements EventPublisher<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(NotifyCenter.class);

    /**
     * 当前Publisher 是否初始化
     */
    private volatile boolean initialized = false;

    /**
     * 当前Publisher 是否关闭
     */
    private volatile boolean shutdown = false;

    /**
     * 当前 Publisher 关注的 event事件类型
     */
    private Class<T> eventType;

    /**
     * Publisher 对应的所有的Subscriber
     */
    protected final ConcurrentHashSet<Subscriber<T>> subscribers = new ConcurrentHashSet<>();

    private int queueMaxSize = -1;

    /**
     * 用于接受事件信息
     */
    private BlockingQueue<T> queue;

    /**
     * 最后一个事件对应的sequence
     */
    protected volatile Long lastEventSequence = -1L;

    private static final AtomicReferenceFieldUpdater<DefaultPublisher, Long> UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(DefaultPublisher.class, Long.class, "lastEventSequence");


    @Override
    public void init(Class<T> type, int bufferSize) {
        setDaemon(true);
        setName("publisher-" + type.getName());
        this.eventType = type;
        this.queueMaxSize = bufferSize;

        //内部使用的是ring buffer的数组结构
        this.queue = new ArrayBlockingQueue<>(bufferSize);

        //启动当前Publisher
        start();
    }


    @Override
    public synchronized void start() {
        if (!initialized) {
            // start just called once
            super.start();
            if (queueMaxSize == -1) {
                queueMaxSize = ringBufferSize;
            }
            initialized = true;
        }
    }

    @Override
    public long currentEventSize() {
        return queue.size();
    }

    public Set<Subscriber<T>> getSubscribers() {
        return subscribers;
    }

    @Override
    public void run() {
       new EventTask().run();
    }

    private boolean hasSubscriber() {
        return !CollectionUtils.isEmpty(subscribers);
    }

    @Override
    public void addSubscriber(Subscriber<T> subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void removeSubscriber(Subscriber<T> subscriber) {
        subscribers.remove(subscriber);
    }

    private void checkIsStart() {
        if (!initialized) {
            throw new IllegalStateException("Publisher does not start");
        }
    }

    @Override
    public boolean publish(T event) {
        checkIsStart();
        try {
            boolean success = this.queue.offer(event, 500, TimeUnit.MILLISECONDS);
            if (!success) {
                LOGGER.warn("Unable to plug in due to interruption, synchronize sending time, event : {}", event);
                receiveEvent(event);
                return true;
            }

            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        this.queue.clear();
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Receive and notifySubscriber to process the event.
     *
     * @param event {@link Event}.
     */
    public void receiveEvent(T event) {

        //当前sequence对应的编号
        final long currentEventSequence = event.sequence();

        // Notification single event listener
        //遍历所有的subscriber 触发事件回调
        for (Subscriber<T> subscriber : subscribers) {
            // Whether to ignore expiration events
            if (subscriber.ignoreExpireEvent() && lastEventSequence > currentEventSequence) {
                LOGGER.debug("[NotifyCenter] the {} is unacceptable to this subscriber, because had expire",
                        event.getClass());
                continue;
            }

            // Because unifying smartSubscriber and subscriber, so here need to think of compatibility.
            // Remove original judge part of codes.
            notifySubscriber(subscriber, event);
        }
    }

    @Override
    public  void notifySubscriber(final Subscriber<T> subscriber, final T event) {
        LOGGER.debug("[NotifyCenter] the {} will received by {}", event, subscriber);

        final Runnable job = () -> subscriber.onEvent(event);
        final Executor executor = subscriber.executor();

        if (executor != null) {
            executor.execute(job);
        } else {
            try {
                job.run();
            } catch (Throwable e) {
                LOGGER.error("Event callback exception: ", e);
            }
        }
    }

    class EventTask implements Runnable {

        @Override
        public void run() {
            try {
                // This variable is defined to resolve the problem which message overstock in the queue.
                int waitTimes = 60;
                // To ensure that messages are not lost, enable EventHandler when
                // waiting for the first Subscriber to register
                for (; ; ) {
                    if (shutdown || hasSubscriber() || waitTimes <= 0) {
                        break;
                    }
                    TimeUnit.MILLISECONDS.sleep(1000);
                    waitTimes--;
                }

                for (; ; ) {
                    if (shutdown) {
                        break;
                    }
                    final T event = queue.take();
                    receiveEvent(event);
                    UPDATER.compareAndSet(DefaultPublisher.this, lastEventSequence, Math.max(lastEventSequence, event.sequence()));
                }
            } catch (Throwable ex) {
                LOGGER.error("Event listener exception : ", ex);
            }
        }
    }
}
