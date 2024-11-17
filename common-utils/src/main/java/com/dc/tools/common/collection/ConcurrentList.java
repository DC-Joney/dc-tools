package com.dc.tools.common.collection;

import com.google.common.collect.Lists;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 并发安全集合类，依赖于jctools下的并发集合
 *
 * @author zhangyang
 * @date 2020-11-10
 */
@ThreadSafe
public class ConcurrentList<T> implements Iterable<T> {

    /**
     * Many produser single consumer
     */
    private final ConcurrentLinkedDeque<T> arrayQueue;


    public ConcurrentList() {
        arrayQueue = new ConcurrentLinkedDeque<>();
    }

    /**
     * 添加元素到 集合中
     *
     * @param value 添加的数据
     */
    public ConcurrentList<T> addFirst(T value) {
        arrayQueue.addFirst(value);
        return this;
    }


    /**
     * 添加元素到 集合中
     *
     * @param value 添加的数据
     */
    public ConcurrentList<T> add(T value) {
        arrayQueue.offer(value);
        return this;
    }

    /**
     * 添加集合到 容器中
     */
    public ConcurrentList<T> addAll(Collection<T> collection) {
        arrayQueue.addAll(collection);
        return this;
    }

    /**
     * 添加集合到 容器中
     */
    public ConcurrentList<T> addAll(ConcurrentList<T> collection) {
        arrayQueue.addAll(collection.arrayQueue);
        return this;
    }


    /**
     * 返回集合的大小
     */
    public long size() {
        return arrayQueue.size();
    }

    /**
     * 判断集合是否为空
     */
    public boolean isEmpty() {
        return arrayQueue.isEmpty();
    }


    public Collection<T> toCollection() {
        return Lists.newArrayList(arrayQueue);
    }

    /**
     * 返回list集合
     *
     * @return 返回具体的集合
     */
    public List<T> toList() {
        return Lists.newArrayList(arrayQueue);
    }


    @Override
    public Iterator<T> iterator() {
        return arrayQueue.iterator();
    }
}
