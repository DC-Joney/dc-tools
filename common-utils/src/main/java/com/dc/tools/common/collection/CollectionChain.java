package com.dc.tools.common.collection;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 支持链式调用的Collection
 * @author zhangyang
 */
public class CollectionChain<T> implements Iterable<T>{

    private final Collection<T> delegate;

    public CollectionChain(){
        delegate = new ArrayList<>();
    }

    public CollectionChain(Supplier<Collection<T>> supplier){
        delegate = Objects.requireNonNull(supplier.get(),"Wrapper of collection must not be null");
    }

    public CollectionChain<T> add(T data){
        delegate.add(data);
        return this;
    }

    public CollectionChain<T> addAll(CollectionChain<T> another){
       delegate.addAll(another.toCollection());
        return this;
    }

    public CollectionChain<T> addAll(Collection<T> collection){
        delegate.addAll(collection);
        return this;
    }

    public CollectionChain<T> remove(T data){
        delegate.remove(data);
        return this;
    }

    public Collection<T> toCollection(){
        return delegate;
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(delegate, 0);
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    public Stream<T> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
