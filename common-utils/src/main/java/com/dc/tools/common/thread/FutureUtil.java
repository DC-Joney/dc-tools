package com.dc.tools.common.thread;

import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;

/**
 * Future 工具类
 *
 * @author zy
 */
@UtilityClass
public class FutureUtil {

    public <T> CompletableFuture<T> exceptionally(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }


}
