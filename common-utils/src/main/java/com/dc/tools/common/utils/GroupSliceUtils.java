package com.dc.tools.common.utils;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@UtilityClass
public class GroupSliceUtils {

    /**
     * <pre>
     *     leftColl: [1,2,3]
     *     leftSlice: 2
     *     rightColl: [4,5,6]
     *     rightSlice: 2
     *
     *     执行结果为：[1,2] [4,5], [3] [4,5], [1,2] [6], [3] [6]
     * </pre>
     * <p>
     * <p>
     * 将带入的两个集合 以各自的分片数做笛卡尔乘积
     *
     * @param lSlice    leftColl 的分片size
     * @param rSlice    rightColl 的分片size
     * @return 返回分片后的笛卡尔积
     */
    public static <T, R> Flux<Slice<T, R>> groupSlice(Collection<T> leftColl, int lSlice, Collection<R> rightColl, int rSlice) {


        Flux<R> rSource = Mono.justOrEmpty(rightColl)
                .defaultIfEmpty(Collections.emptyList())
                .flatMapIterable(Function.identity());

        Flux<T> lSource = Mono.justOrEmpty(leftColl)
                .defaultIfEmpty(Collections.emptyList())
                .flatMapIterable(Function.identity());

        return lSource.buffer(lSlice)
                .groupJoin(rSource,
                        left -> Flux.never(),
                        right -> rSource.buffer(rSlice),
                        (left, right) -> right.buffer(rSlice).map(buf -> Slice.create(left, buf))
                ).flatMap(Function.identity());

    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "create")
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @ToString
    public static class Slice<LEFT, RIGHT> {
        List<LEFT> leftSlice;
        List<RIGHT> rightSlice;
    }

}
