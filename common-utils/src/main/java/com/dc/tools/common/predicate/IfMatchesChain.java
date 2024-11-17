package com.dc.tools.common.predicate;

import com.dc.tools.common.utils.Assert;
import com.dc.tools.common.utils.StringUtils;
import com.google.common.collect.Iterators;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * if else 执行过滤链
 *
 * @author zhangyang
 * @date 2020-08-27
 */
@SuppressWarnings("all")
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class IfMatchesChain {


    /**
     * @return 返回无返回if else builder
     */
    public static <R> IfMatchNoResultBuilder<R> ifMatchNoResult(R initValue) {
        return new IfMatchNoResultBuilders().and(initValue);
    }

    /**
     * @return 有返回if else builder
     */
    public static <IN, OUT> IfMatchResultBuilder<IN, OUT> ifMatchResult(IN initValue) {
        return new IfMatchResultBuilders().and(initValue);
    }


    /**
     * IfMatchNoResultBuilder 集合类
     * <p>
     * 用于存放无返回值的链表集合
     */
    @NoArgsConstructor
    public static class IfMatchNoResultBuilders implements Iterable<IfMatchNoResultBuilder> {

        @Getter
        private List<IfMatchNoResultBuilder> builders = new ArrayList<>();


        IfMatchNoResultBuilders(List<IfMatchNoResultBuilder> builders) {
            this.builders = builders;
        }

        static IfMatchNoResultBuilders fromAnother(IfMatchNoResultBuilders resultBuilders) {
            return new IfMatchNoResultBuilders(resultBuilders.builders);
        }

        public <R> IfMatchNoResultBuilder<R> and(R instance) {
            IfMatchNoResultBuilder<R> resultBuilder = new IfMatchNoResultBuilder<>(this, instance);
            builders.add(resultBuilder);
            return resultBuilder;
        }

        @Override
        public Iterator<IfMatchNoResultBuilder> iterator() {
            return Iterators.unmodifiableIterator(builders.iterator());
        }

        public Stream<IfMatchNoResultBuilder> stream() {
            return StreamSupport.stream(builders.spliterator(), false);
        }

    }


    /**
     * 无返回结果的IF else判断
     */
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IfMatchNoResultBuilder<R> {

        IfMatchFilterFactory.IfMatchNoResultFilter matchFilter = IfMatchFilterFactory.noResultFilter();

        IfMatchNoResultBuilders builders;

        R instance;

        IfMatchNoResultBuilder(IfMatchNoResultBuilders builders, R instance) {
            this.builders = builders;
            this.instance = instance;
        }


        IfMatchNoResultBuilder(IfMatchNoResultBuilders builders, IfMatchFilterFactory.IfMatchNoResultFilter matchFilter, R instance) {
            this.builders = builders;
            this.matchFilter = matchFilter;
            this.instance = instance;
        }


        /**
         * 会改变源头数据，当方法被调用后，会立即修改源头数据
         *
         * @param convert 对源头数据进行改变
         */
        public <OUT> IfMatchNoResultBuilder<OUT> changeOrigin(Function<R, OUT> convert) {

            Assert.notNull(convert, "The function convert must not be null");

            try {

                OUT convertData = Objects.requireNonNull(convert.apply(instance), "Convert data must not be null");
                return new IfMatchNoResultBuilder<>(IfMatchNoResultBuilders.fromAnother(builders), matchFilter, convertData);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        /**
         * 改变源头数据, 在调用该方法时并不会立即执行，而是保证链表前置节点中无 状态为true的返回值时，才会对其进行执行
         *
         * @param convert 对源头数据进行改变
         */
        public <OUT> IfMatchNoResultBuilder<OUT> lazyChangeOrigin(Function<R, OUT> convert) {
            matchFilter.addNext(convert);
            return new IfMatchNoResultBuilder<>(builders, null);
        }


        /**
         * @param predicate Represents a predicate (boolean-valued function) of one argument.
         * @param consumer  业务逻辑
         * @return 返回无返回if else builder
         */
        public IfMatchNoResultBuilder<R> ifMatch(Predicate<R> predicate, Consumer<R> consumer) {
            matchFilter.addNext(predicate, IfMatchFilterFactory.IfState.IF, consumer, instance);
            return this;
        }

        public IfMatchNoResultBuilder<R> elseMatch(Consumer<R> consumer) {
            matchFilter.addNext(true, IfMatchFilterFactory.IfState.ELSE, consumer, instance);
            return this;
        }


        public <IN> IfMatchNoResultBuilder<IN> and(IN instance) {
            return IfMatchNoResultBuilders.fromAnother(builders).and(instance);
        }


        public <IN, OUT> IfMatchNoResultBuilder<OUT> and(IN instance, Function<IN, OUT> convert) {
            return IfMatchNoResultBuilders.fromAnother(builders).and(instance).changeOrigin(convert);
        }

        /**
         * 执行所有if代码逻辑
         */
        public void executeAll() {
            StreamSupport.stream(builders.spliterator(), false).forEach(IfMatchNoResultBuilder::singleExecute);
        }


        /**
         * 执行当前链表的if代码逻辑
         */
        public void singleExecute() {
            matchFilter.execute();
        }

    }


    /**
     * 用于存放有返回值的 链表集合
     */
    public static class IfMatchResultBuilders implements Iterable<IfMatchResultBuilder> {

        @Getter
        private List<IfMatchResultBuilder> builders = new ArrayList<>();


        public <T, R> IfMatchResultBuilder<T, R> and(T instance) {
            IfMatchResultBuilder<T, R> resultBuilder = new IfMatchResultBuilder<>(this, instance);
            builders.add(resultBuilder);
            return resultBuilder;
        }

        @Override
        public Iterator<IfMatchResultBuilder> iterator() {
            return Iterators.unmodifiableIterator(builders.iterator());
        }


        public Stream<IfMatchResultBuilder> stream() {
            return StreamSupport.stream(builders.spliterator(), false);
        }

    }


    /**
     * 有返回结果的IF else判断
     * <p>
     * Builder 构造链
     */
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IfMatchResultBuilder<R, T> {


        private static final String unknownBuilder = "unknownBuilder";

        final IfMatchFilterFactory.IfMatchResultFilter matchFilter;

        final IfMatchResultBuilders resultBuilders;

        R instance;


        IfMatchResultBuilder(IfMatchResultBuilders resultBuilders, R instance) {
            this(IfMatchFilterFactory.resultFilter(), resultBuilders, instance);
        }


        IfMatchResultBuilder(IfMatchFilterFactory.IfMatchResultFilter matchFilter, IfMatchResultBuilders resultBuilders, R instance) {
            this.instance = instance;
            this.resultBuilders = resultBuilders;
            this.matchFilter = matchFilter;
        }


        public IfMatchResultBuilder<R, T> name(String name) {
            this.matchFilter.setName(name);
            return this;
        }


        /**
         * 会改变源头数据
         *
         * @param convert 对源头数据进行改变
         */
        public <IN> IfMatchResultBuilder<IN, T> changeNoNullOrigin(Function<R, IN> convert) {

            Assert.notNull(convert, "The function convert must not be null");

            try {

                IN apply = Objects.requireNonNull(convert.apply(instance), "Convert data must not be null");
                return new IfMatchResultBuilder<>(matchFilter, resultBuilders, apply);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        /**
         * 会改变源头数据
         *
         * @param convert 对源头数据进行改变
         */
        public <IN> IfMatchResultBuilder<IN, T> changeOrigin(Function<R, IN> convert) {

            Assert.notNull(convert, "The function convert must not be null");

            try {

                IN apply = convert.apply(instance);
                return new IfMatchResultBuilder<>(matchFilter, resultBuilders, apply);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        /**
         * @param filter  条件过滤
         * @param convert 业务逻辑转换
         * @return 有返回if else builder
         */
        public IfMatchResultBuilder<R, T> ifMatch(Predicate<R> filter, Function<R, T> convert) {
            matchFilter.addNext(filter, IfMatchFilterFactory.IfState.IF, convert, instance);
            return this;
        }


        public IfMatchResultBuilder<R, T> elseMatch(T instance) {
            matchFilter.addNext(true, IfMatchFilterFactory.IfState.ELSE, Function.identity(), instance);
            return this;
        }


        public <I> IfMatchResultBuilder<R, I> elseMatch(Function<R, I> function) {
            matchFilter.addNext(true, IfMatchFilterFactory.IfState.ELSE, function, instance);
            return new IfMatchResultBuilder<>(matchFilter, resultBuilders, instance);
        }

        public <IN, OUT> IfMatchResultBuilder<IN, OUT> and(IN instance) {
            return resultBuilders.and(instance);
        }


        public IfMatchResultBuilder<R, T> andOrigin() {
            return resultBuilders.and(instance);
        }


        public <E, IN, OUT> IfMatchResultBuilder<IN, OUT> and(E instance, Function<E, IN> convert) {
            return this.<E, OUT>and(instance).changeOrigin(convert);
        }


        public <IN, OUT> IfMatchResultBuilder<IN, OUT> joinAnd(String separator, IN instance) {
            return join(separator).and(instance);
        }

        public IfMatchResultBuilder<R, T> join(String separator) {
            this.matchFilter.setSeparator(separator);
            return this;
        }

        /**
         * 获取返回的 拼接类型
         */
        public String getJoinResult() {
            return resultBuilders.stream()
                    .reduce(new StringBuilder(), this::accumulator, StringBuilder::append).toString();
        }


        /**
         * 合并值
         */
        private StringBuilder accumulator(StringBuilder builder, IfMatchResultBuilder resultBuilder) {
            Object singleResult = resultBuilder.getSingleResult();

            if (singleResult == null)
                return builder;

            String result = singleResult.toString();
            return StringUtils.hasText(resultBuilder.matchFilter.getSeparator()) ? builder.append(result).append(resultBuilder.matchFilter.getSeparator()) : builder.append(result);
        }


        /**
         * 获取所有匹配的数据
         */
        public Map<String, Object> getAllResult() {

            Map<Boolean, List<IfMatchResultBuilder>> collectMap = resultBuilders.stream()
                    .collect(Collectors.partitioningBy(builder -> StringUtils.hasText(builder.matchFilter.getName())));

            Map<String, Object> builderMap = collectMap.getOrDefault(true, Collections.emptyList())
                    .stream()
                    .collect(Collectors.toMap(builder -> builder.matchFilter.getName(), IfMatchResultBuilder::getSingleResult));

            List<IfMatchResultBuilder> emptyNameBuilders = collectMap.getOrDefault(false, Collections.emptyList());

            if (!emptyNameBuilders.isEmpty()) {
                Supplier<List<Object>> unknownBuilders = () -> emptyNameBuilders
                        .stream().map(IfMatchResultBuilder::getSingleResult).collect(Collectors.toList());
                builderMap.put(unknownBuilder, unknownBuilders);
            }

            return builderMap;
        }


        /**
         * 只返回当前 if else 链的 结果
         */
        public T getSingleResult() {
            return matchFilter.execute();
        }


        /**
         * 返回 if else 中的原来的值
         */
        public Optional<R> getOrigin() {
            return Optional.ofNullable(instance);
        }

    }

}
