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
import com.dc.tools.notify.listener.Subscriber;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The default share event publisher implementation for slow event.
 *
 * @author zongtanghu
 * @apiNote Forked from <a href="https://github.com/alibaba/nacos">Nacos</a>.
 */
public class DefaultSharePublisher extends DefaultPublisher<SlowEvent> {
    
    private final Map<Class<? extends SlowEvent>, Set<Subscriber<? extends SlowEvent>>> subMappings = new ConcurrentHashMap<>();
    
    private final Lock lock = new ReentrantLock();
    
    /**
     * Add listener for default share publisher.
     *
     * @param subscriber    {@link Subscriber}
     * @param subscribeType subscribe event type, such as slow event or general event.
     */
    @SuppressWarnings("unchecked")
    public void addSubscriber(Subscriber<? extends SlowEvent> subscriber, Class<? extends SlowEvent> subscribeType) {
        // Actually, do a classification based on the slowEvent type.
        // For adding to parent class attributes synchronization.
        subscribers.add((Subscriber<SlowEvent>) subscriber);
        
        lock.lock();
        try {
            Set<Subscriber<? extends SlowEvent>> sets = subMappings.get(subscribeType);
            if (sets == null) {
                Set<Subscriber<? extends SlowEvent>> newSet = new ConcurrentHashSet<>();
                newSet.add(subscriber);
                subMappings.put(subscribeType, newSet);
                return;
            }
            sets.add(subscriber);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Remove listener for default share publisher.
     *
     * @param subscriber    {@link Subscriber}
     * @param subscribeType subscribe event type, such as slow event or general event.
     */
    @SuppressWarnings("unchecked")
    public void removeSubscriber(Subscriber<? extends SlowEvent> subscriber, Class<? extends SlowEvent> subscribeType) {
        // Actually, do a classification based on the slowEvent type.
        // For removing to parent class attributes synchronization.
        subscribers.remove(subscriber);
        
        lock.lock();
        try {
            Set<Subscriber<? extends SlowEvent>> sets = subMappings.get(subscribeType);
            
            if (sets != null) {
                sets.remove(subscriber);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void receiveEvent(SlowEvent event) {
        final long currentEventSequence = event.sequence();
        // get subscriber set based on the slow EventType.
        final Class<? extends SlowEvent> slowEventType = event.getClass();

        // Get for Map, the algorithm is O(1).
        Set<Subscriber<? extends SlowEvent>> subscribers = subMappings.get(slowEventType);
        if (null == subscribers) {
            LOGGER.debug("[NotifyCenter] No subscribers for slow event {}", slowEventType.getName());
            return;
        }

        // Notification single event subscriber
        for (Subscriber<? extends SlowEvent> subscriber : subscribers) {
            // Whether to ignore expiration events
            if (subscriber.ignoreExpireEvent() && lastEventSequence > currentEventSequence) {
                LOGGER.debug("[NotifyCenter] the {} is unacceptable to this subscriber, because had expire",
                        event.getClass());
                continue;
            }

            // Notify single subscriber for slow event.
            notifySubscriber((Subscriber<SlowEvent>) subscriber, event);
        }
    }

}
