package com.dc.tools.common.predicate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 双向链表，用来为if else 做链式连接
 *
 * @author zhangyang
 * @date 2020-08-24
 */
@Slf4j
@NotThreadSafe
@UtilityClass
public class IfMatchFilterFactory {


    /**
     * @return 创建无返回值的过滤链
     */
    static IfMatchNoResultFilter noResultFilter() {
        return new IfMatchNoResultFilter();

    }


    /**
     * @return 创建有返回值的过滤链
     */
    static IfMatchResultFilter resultFilter() {
        return new IfMatchResultFilter();

    }

    /**
     * IF ELSE 队列节点
     *
     * @param <W>
     */
    static class IfNode<W> {

        /**
         * 前置节点
         */
        IfNode<W> prev;

        /**
         * 后置节点
         */
        IfNode<W> next;

        /**
         * 当前if的状态
         */
        @NonNull
        IfState ifState;

        /**
         * 节点上挂载的数据值
         */
        W ifWrapper;

        IfNode(IfState ifState, W ifWrapper) {
            this.ifState = ifState;
            this.ifWrapper = ifWrapper;
        }
    }


    /**
     * if 节点的双向链表维护
     *
     * @param <N>
     */
    private static class IfMatchLinkedNode<N> {

        /**
         * 头节点
         */
        protected IfNode<N> head;

        /**
         * 尾节点
         */
        protected IfNode<N> tail;


        private void addNode(IfState ifState, N nodeWrapper) {

            IfNode<N> node = new IfNode<>(ifState, nodeWrapper);

            //初始化头节点，无论是 if 还是 else 节点全部放在结尾
            if (isEmpty()) {
                head = new IfNode<>(IfState.NO, null);
                tail = node;
                head.next = tail;
                tail.prev = head;
                return;
            }

            //判断如果尾节点已经是 else节点，并且传入的node节点 也是 else节点则报错，链表只允许存在一个else 节点
            if (tail.ifState == IfState.ELSE && node.ifState == IfState.ELSE) {
                throw new IllegalArgumentException("The ifState of  ELSE is exists");
            }

            // 如果尾 else 节点已经存在，则在 尾节点的上一个节点进行插入
            if (tail.ifState == IfState.ELSE) {

                //尾节点的上一个节点
                IfNode<N> ifTailNode = tail.prev;

                //将尾节点的前驱节点的next节点指向新带入的node节点
                ifTailNode.next = tail.prev = node;
                node.next = tail;
                node.prev = ifTailNode;
                return;
            }

            //顺序插入所有节点
            IfNode<N> tailNode = tail;
            tailNode.next = tail = node;
            tail.prev = tailNode;
        }

        /**
         * 判断链表是否为空
         */
        public boolean isEmpty() {
            return (head == null || head.next == null) && tail == null;
        }

        /**
         * 清空链表
         */
        public void clear() {

            //help gc
            this.head = null;
            this.tail = null;
        }

    }


    interface IfMatchFilter {

        <R> R execute();


        boolean isEmpty();

    }


    /**
     * 无返回值链表
     */
    static class IfMatchNoResultFilter implements IfMatchFilter {


        IfMatchLinkedNode<ConsumerWrapper<?>> linkedNode = new IfMatchLinkedNode<>();


        /**
         * 添加下游节点
         *
         * @param isExecute 是否执行
         * @param ifState   if状态值
         * @param consumer  业务逻辑
         */
        <E> void addNext(Predicate<E> predicate, IfState ifState, Consumer<E> consumer, E instance) {
            linkedNode.addNode(ifState, new ConsumerWrapper<>(predicate, consumer, instance));
        }


        /**
         * 添加下游节点
         *
         * @param convert 对底下的数据进行转换
         */
        <IN, OUT> void addNext(Function<IN, OUT> convert) {
            linkedNode.addNode(IfState.ORIGIN_CHANGE, new <OUT>ConsumerWrapper<IN>(e -> true, convert));
        }


        /**
         * 添加下游节点
         *
         * @param isExecute 是否执行
         * @param ifState   if状态值
         * @param consumer  业务逻辑
         */
        <E> void addNext(boolean isExecute, IfState ifState, Consumer<E> consumer, E instance) {
            linkedNode.addNode(ifState, new ConsumerWrapper<>(wrap -> isExecute, consumer, instance));
        }


        /**
         * 执行代码逻辑
         */
        public <R> R execute() {
            try {
                //判断链表是否为空
                if (!linkedNode.isEmpty()) {

                    //遍历所有节点
                    for (IfNode<ConsumerWrapper<?>> node = linkedNode.head; node.next != null; ) {
                        node = node.next;
                        if (node.ifWrapper != null) {

                            ConsumerWrapper<?> wrapper = node.ifWrapper;

                            //当条件判断为真时进入处理逻辑
                            if (wrapper.predicate.test(wrapper.instance)) {

                                //如果节点中存在修改下游源头数据的则进行修改
                                if (node.ifState == IfState.ORIGIN_CHANGE) {
                                    IfNode<ConsumerWrapper<?>> wrapperNode = node.next;
                                    while (wrapperNode.next != null) {
                                        ConsumerWrapper<?> current = wrapperNode.ifWrapper;
                                        if (current.convert != null) {
                                            current.convert = current.convert.andThen(node.ifWrapper.convert);
                                            continue;
                                        }
                                        current.convert = node.ifWrapper.convert;
                                        wrapperNode = wrapperNode.next;
                                    }
                                    continue;
                                }

                                if (wrapper.convert != null) {
                                    node.ifWrapper = new ConsumerWrapper<>(wrapper.predicate, wrapper.consumer, wrapper.convert.apply(wrapper.instance));
                                }

                                //wrapper 的值可能被改变过
                                wrapper = node.ifWrapper;

                                wrapper.consumer.accept(wrapper.instance);

                                break;
                            }
                        }
                    }
                }
            } finally {
                linkedNode.clear();
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return linkedNode.isEmpty();
        }
    }


    static class IfMatchResultFilter implements IfMatchFilter {


        IfMatchLinkedNode<FunctionWrapper<?, ?>> linkedNode = new IfMatchLinkedNode<>();


        /**
         * 当前 builder的名称
         */
        @Getter
        @Setter
        String name;


        /**
         * 中间连接操作符
         */
        @Setter
        @Getter
        String separator;


        /**
         * 添加下游节点
         *
         * @param predicate 是否执行
         * @param ifState   if状态值
         * @param convert   业务逻辑
         */
        <E, R> void addNext(Predicate<E> predicate, IfState ifState, Function<E, R> convert, E instance) {
            linkedNode.addNode(ifState, new FunctionWrapper<>(predicate, convert, instance));
        }


        /**
         * 添加下游节点
         *
         * @param isExecute 是否执行
         * @param ifState   if状态值
         * @param convert   业务逻辑
         */
        <E, R> void addNext(boolean isExecute, IfState ifState, Function<E, R> convert, E instance) {
            linkedNode.addNode(ifState, new FunctionWrapper<>(s -> isExecute, convert, instance));
        }


        /**
         * 执行代码逻辑
         */
        @SuppressWarnings({"Duplicates", "unchecked"})
        public <R> R execute() {
            try {
                //判断 链表是否为空
                if (!linkedNode.isEmpty()) {

                    //遍历所有节点
                    for (IfNode<FunctionWrapper<?, ?>> node = linkedNode.head; node.next != null; ) {
                        node = node.next;
                        if (node.ifWrapper != null) {
                            FunctionWrapper<?, ?> wrapper = node.ifWrapper;
                            if (wrapper.originConvert != null) {
                                node.ifWrapper = new FunctionWrapper<>(wrapper.predicate, wrapper.convert, wrapper.originConvert.apply(wrapper.instance));
                            }

                            //wrapper 的值可能被改变过
                            wrapper = node.ifWrapper;
                            if (wrapper.predicate.test(wrapper.instance)) {
                                return (R) wrapper.convert.apply(wrapper.instance);
                            }
                        }
                    }
                }
            } finally {
                linkedNode.clear();
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return linkedNode.isEmpty();
        }
    }


    @SuppressWarnings("unchecked")
    private static class ConsumerWrapper<T> {

        Function<Object, Object> convert;


        Predicate<Object> predicate;

        /**
         * 处理逻辑
         */
        Consumer<Object> consumer;

        /**
         * 逻辑使用的对象
         */
        T instance;

        ConsumerWrapper(Predicate<T> predicate, Consumer<T> consumer, T instance) {
            this.predicate = (Predicate<Object>) predicate;
            this.consumer = (Consumer<Object>) consumer;
            this.instance = instance;
        }

        <OUT> ConsumerWrapper(Predicate<T> predicate, Function<T, OUT> convert) {
            this.predicate = (Predicate<Object>) predicate;
            this.convert = (Function<Object, Object>) convert;
        }

    }


    @AllArgsConstructor(staticName = "of")
    private static class FunctionWrapper<T, R> {

        Function<Object, Object> originConvert;

        Predicate<Object> predicate;

        /**
         * 处理逻辑
         */
        Function<Object, Object> convert;

        /**
         * 逻辑使用的对象
         */
        T instance;


        @SuppressWarnings("unchecked")
        FunctionWrapper(Predicate<T> predicate, Function<T, R> convert, T instance) {
            this.predicate = (Predicate<Object>) predicate;
            this.convert = (Function<Object, Object>) convert;
            this.instance = instance;
        }
    }


    protected enum IfState {

        /**
         * if 判断
         */
        IF,

        /**
         * else 判断
         */
        ELSE,

        /**
         * 用于头节点
         */
        NO,

        /**
         * 用于源头数据改变
         */
        ORIGIN_CHANGE

    }


}
