package com.dc.tools.task;

import java.util.concurrent.CompletableFuture;

/**
 * 异步任务
 * @param <T> 任务返回数据类型
 */
public interface AsyncTask<T> extends Task{

    /**
     * 执行具体的任务并且返回结果
     */
    CompletableFuture<T> getResult();


}
