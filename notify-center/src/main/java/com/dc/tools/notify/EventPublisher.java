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


import com.dc.tools.notify.listener.Subscriber;

/**
 * Event publisher.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 * @author zongtanghu
 * @apiNote Forked from <a href="https://github.com/alibaba/nacos">Nacos</a>.
 */
public interface EventPublisher<T extends Event> extends Closeable {

    /**
     * Initializes the event publisher.
     *
     * @param type       {@link Event >}
     * @param bufferSize Message staging queue size
     */
    void init(Class<T> type, int bufferSize);

    /**
     * The number of currently staged events.
     *
     * @return event size
     */
    long currentEventSize();

    /**
     * Add listener.
     *
     * @param subscriber {@link com.dc.tools.notify.listener.Subscriber}
     */
    void addSubscriber(Subscriber<T> subscriber);

    /**
     * Remove listener.
     *
     * @param subscriber {@link Subscriber}
     */
    void removeSubscriber(Subscriber<T> subscriber);

    /**
     * publish event.
     *
     * @param event {@link Event}
     * @return publish event is success
     */
    boolean publish(T event);

    /**
     * Notify listener.
     *
     * @param subscriber {@link Subscriber}
     * @param event      {@link Event}
     */
    void notifySubscriber(Subscriber<T> subscriber, T event);

}
