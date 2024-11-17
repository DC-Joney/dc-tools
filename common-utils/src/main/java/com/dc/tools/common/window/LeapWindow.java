package com.dc.tools.common.window;

import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.common.utils.StringUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 提供滑动窗口，用于数据统计
 * <p>
 * 使用方式： {@link LeapWindowExample}
 * </p>
 *
 * @author dcjoney
 * @apiNote 该窗口只可停止一次，停止后就无法再次启动，并且会忽略所有通过 {@link Publish#publish(Object)} 推送的值 <br/>
 * 如果需要重新使用，请通过 {@link LeapWindow#isTerminated()}来判断是否需要从新创建
 */
@Slf4j
@ThreadSafe
public class LeapWindow<T, A, R> implements Disposable, Cloneable, Closeable {

    private static final String DEFAULT_WINDOW_NAME_PREFIX = "slipping-window-";

    /**
     * 窗口计数器
     */
    private static final AtomicLong counter = new AtomicLong();

    /**
     * global prefix window name
     */
    private static volatile String globalNamePrefix = DEFAULT_WINDOW_NAME_PREFIX;

    private final int windowSize;

    private final Duration interval;

    /**
     * 用于发送数据，如果未启动则不做任何操作
     */
    private Publish<T> publish;

    private final Supplier<A> supplier;

    /**
     * 针对传入数据转换窗口数据的Function
     */
    private final BiFunction<A, ? super T, A> convert;

    /**
     * 针对所有窗口的数据聚合
     */
    private final Supplier<R> reduceSupplier;

    private final BiFunction<R, ? super A, R> reduceFunction;

    private final AtomicBoolean START_STATE = new AtomicBoolean(false);
    private final AtomicBoolean DISPOSE_STATE = new AtomicBoolean(false);
    private final Scheduler windowScheduler;
    private final String windowName;

    private final LatestValue<R> lastValue = new LatestValue<>();

    /**
     * @param windowSize     窗口大小
     * @param interval       每个窗口的间隔时间
     * @param supplier       针对publish:{@linkplain T}的单个窗口的收集容器
     * @param convert        通过publish 传入的数据向中间层数据转换，用于收集或者聚合每个窗口内传入的数据 <br/>
     *                       比如在单个窗口内对所有传入的数据进行add操作，就是 100ms:data = publish:data + publish:data + .... + publish:data:100ms
     * @param reduceSupplier 多个窗口的收集容器
     * @param reduceFunction 用于将所有窗口的数据进行聚合。<br/>
     *                       比如需要对三个窗口内的数据进行聚合，就是 100ms:data + 100ms:data + 100ms:data
     */
    public LeapWindow(int windowSize,
                      Duration interval,
                      Supplier<A> supplier,
                      BiFunction<A, ? super T, A> convert,
                      Supplier<R> reduceSupplier,
                      BiFunction<R, ? super A, R> reduceFunction) {
        this(null, windowSize, interval, supplier, convert, reduceSupplier, reduceFunction);
    }


    public LeapWindow(String windowName, int windowSize,
                      Duration interval,
                      Supplier<A> supplier,
                      BiFunction<A, ? super T, A> convert,
                      Supplier<R> reduceSupplier,
                      BiFunction<R, ? super A, R> reduceFunction) {
        this.windowSize = windowSize;
        this.interval = interval;
        this.supplier = supplier;
        this.convert = convert;
        this.reduceSupplier = reduceSupplier;
        this.reduceFunction = reduceFunction;
        this.windowName = StringUtils.hasText(windowName) ? windowName : globalNamePrefix + counter.incrementAndGet();
        this.windowScheduler = Schedulers.newParallel(windowName);
    }

    /**
     * 启动滑动窗口
     */
    public Publish<T> start() {
        //自旋直到程序完全启动
        boolean started = START_STATE.get();
        while (!DISPOSE_STATE.get() && !started) {
            if (START_STATE.compareAndSet(false, true)) {
                publish = new Publish<>(DISPOSE_STATE);
                startWindow(publish);
                started = true;

                /*
                 * !DISPOSE_STATE.get() && !started 该判断非原子操作，所以会导致在并发过程中可能由于先关闭再启动而导致窗口依旧为启动状态
                 */
                if (DISPOSE_STATE.get()) {
                    this.dispose();
                }
            }
        }

        return publish;
    }

    /**
     * 内置处理
     */
    private void startWindow(Publish<T> publish) {
        Flux.<T>create(publish::setSink)
                .window(interval, windowScheduler)
                .flatMap(request -> request.reduceWith(supplier, convert))
                .window(windowSize, 1)
                .flatMap(request -> request.reduceWith(reduceSupplier, reduceFunction))
                .share()
                .log()
                .subscribe(lastValue);
    }

    /**
     * 关闭当前窗口, 不需要为其限制次数，
     * <p>
     * 因为由于并发可能会导致在dispose之后，进行了start操作。如果限制次数，会导致当前窗口无法关闭
     * <p/>
     */
    @Override
    public void dispose() {
        //如果还未启动，则先将关闭标志设置为true，表示启动后进行关闭
        DISPOSE_STATE.compareAndSet(false, true);

        if (START_STATE.get()) {
            lastValue.dispose();
            if (publish != null && publish.sink != null) {
                publish.sink.complete();
            }
        }

    }


    /**
     * @return 当前窗口的状态
     */
    public boolean isTerminated() {
        return DISPOSE_STATE.get();
    }


    public R getValue() {
        return lastValue.getLastValue();
    }

    @Override
    public Object clone() {
        return copy();
    }

    @Override
    public void close() throws IOException {
        if (!isTerminated())
            this.dispose();
    }

    /**
     * 基于当前的窗口创建一个新的窗口
     */
    public LeapWindow<T, A, R> copy() {
        return new LeapWindow<>(windowName, windowSize, interval, supplier, convert, reduceSupplier, reduceFunction);
    }


    /**
     * 基于当前的窗口创建一个新的窗口,并且关闭原有的窗口
     */
    public LeapWindow<T, A, R> finish() {
        this.dispose();
        return this;
    }


    /**
     * 基于当前的窗口创建一个新的窗口,并且关闭原有的窗口
     */
    public LeapWindow<T, A, R> finishCopy() {
        this.dispose();
        return new LeapWindow<>(windowName, windowSize, interval, supplier, convert, reduceSupplier, reduceFunction);
    }


    /**
     * 基于当前的窗口创建一个新的窗口并且启动
     */
    public Publish<T> copyStart() {
        return copy().start();
    }

    /**
     * setting global prefix name
     */
    public static void setNamePrefix(String namePrefix) {
        LeapWindow.globalNamePrefix = namePrefix;
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class Publish<T> {

        final AtomicBoolean disposeState;

        /**
         * 用于发送数据
         */
        private FluxSink<T> sink;

        public void publish(T data) {

            //如果窗口是关闭状态，则不再载入数据
            if (!disposeState.get()) {
                sink.next(data);
                return;
            }

            Publish.log.info("Will be ignore data for {}", data);
        }

        void setSink(FluxSink<T> sink) {
            this.sink = sink;
        }
    }

    public static class LatestValue<R> extends BaseSubscriber<R> {

        @Getter
        private volatile R lastValue;

        @Override
        protected void hookOnSubscribe(@NonNull Subscription subscription) {
            requestUnbounded();
        }

        @Override
        protected void hookOnNext(@NonNull R value) {
            this.lastValue = value;
        }
    }


    @Deprecated
    @Accessors(fluent = true)
    public static class LeapWindowBuilder<T, A, R> {
        private Publish<T> publish;
        private Supplier<A> supplier;
        private BiFunction<A, ? super T, A> windowFunction;
        private Supplier<R> reduceSupplier;
        private BiFunction<R, ? super A, R> reduceFunction;
    }


}
