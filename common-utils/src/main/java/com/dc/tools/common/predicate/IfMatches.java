package com.dc.tools.common.predicate;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 用于替代if else
 *
 * @author zhangyang
 * @date 2020-08-24
 */

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IfMatches {

    @Getter
    private List<IfMatchFilterFactory.IfMatchFilter> filters = new ArrayList<>();

    public static IfMatches initChain() {
        return new IfMatches();
    }


    /**
     * @param state    是否为真
     * @param instance 带入的实例对象
     * @param consumer 业务逻辑
     * @return 返回无返回if else builder
     */
    public <T> IfMatchNoResultBuilder ifMatch(boolean state, T instance, Consumer<T> consumer) {
        IfMatchNoResultBuilder ifMatch = new IfMatchNoResultBuilder(this);
        return ifMatch.ifMatch(state, instance, consumer);
    }


    /**
     * @param state    是否为真
     * @param instance 带入的实例对象
     * @param consumer 业务逻辑
     * @return 有返回if else builder
     */
    public <T, R> IfMatchResultBuilder<R> ifMatchResult(boolean state, T instance, Function<T, R> consumer) {
        IfMatchResultBuilder<R> matchResult = new IfMatchResultBuilder<>(this);
        return matchResult.ifMatch(state, instance, consumer);
    }


    /**
     * 无返回结果的IF else判断
     */
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IfMatchNoResultBuilder {

        IfMatchFilterFactory.IfMatchNoResultFilter matchFilter = new IfMatchFilterFactory.IfMatchNoResultFilter();

        IfMatches matches;

        IfMatchNoResultBuilder(IfMatches ifMatches) {
            this.matches = ifMatches;
            this.matches.getFilters().add(matchFilter);
        }

        public <T> IfMatchNoResultBuilder ifMatch(boolean state, T instance, Consumer<T> consumer) {
            matchFilter.addNext(state, IfMatchFilterFactory.IfState.IF, consumer, instance);
            return this;
        }

        public <T> void elseMatch(T instance, Consumer<T> consumer) {
            matchFilter.addNext(true, IfMatchFilterFactory.IfState.ELSE, consumer, instance);
            endIfNoResult();
        }

        public IfMatchNoResultBuilder and() {
            return new IfMatchNoResultBuilder(matches);
        }

        public void endIfNoResult() {
            matches.filters.forEach(IfMatchFilterFactory.IfMatchFilter::execute);
        }

    }


    /**
     * 有返回结果的IF else判断
     */
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IfMatchResultBuilder<R> {

        IfMatchFilterFactory.IfMatchResultFilter matchFilter = new IfMatchFilterFactory.IfMatchResultFilter();

        IfMatches matches;

        IfMatchResultBuilder(IfMatches ifMatches) {
            this.matches = ifMatches;
            this.matches.getFilters().add(matchFilter);
        }

        public <T> IfMatchResultBuilder<R> ifMatch(boolean state, T instance, Function<T, R> convert) {
            matchFilter.addNext(state, IfMatchFilterFactory.IfState.IF, convert, instance);
            return this;
        }

        public <T> IfMatchResultBuilder<R> elseIfMatch(boolean state, T instance, Function<T, R> convert) {
            matchFilter.addNext(state, IfMatchFilterFactory.IfState.IF, convert, instance);
            return this;
        }

        public <T> R elseMatch(T instance, Function<T, R> convert) {
            matchFilter.addNext(true, IfMatchFilterFactory.IfState.ELSE, convert, instance);
            return getResult();
        }

        public R getResult() {
            return matchFilter.execute();
        }
    }

}
