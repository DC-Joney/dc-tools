package com.dc.tools.task;

/**
 * 支持设置返回结果的 任务类型
 * @param <T>
 */
public interface ResultAsyncTask <T> extends AsyncTask<T>{

    /**
     * 设置返回结果
     * @param result 添加返回结果
     */
    void setResult(T result);

    /**
     * 设置异常信息
     * @param ex 异常信息
     */
    default void setException(Exception ex) {

    }
}
