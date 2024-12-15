package com.dc.tools.task;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * 任务的上下文信息
 *
 * @author zy
 */
public class TaskContext {

    /**
     * 上下文 context
     */
    private final Map<String, Object> context = new ConcurrentHashMap<>();

    /**
     * task id attribute
     */
    public static final String TASK_ID = TaskContext.class.getName() + "_internal_task_id";


    /**
     * task worker attribute
     */
    public static final String TASK_WORKER = TaskContext.class.getName() + "_internal_task_worker";

    /**
     * 重试的次数
     */
    public static final String RETRY = TaskContext.class.getName() + "_internal_retry";

    /**
     * 当任务为内部流转时，会添加interval属性，用于对其进行标注
     */
    public static final String INTERNAL = TaskContext.class.getName() + "_internal";

    /**
     * 内部流转的任务类型
     */
    public static final String INTERNAL_TYPE = TaskContext.class.getName() + "_internal_type";


    /**
     * 任务的生命周期，因为任务可被重试，重试的任务不会回调其生命周期方法
     */
    public static final String TASK_LIFE_CYCLE = TaskContext.class.getName() + "_internal_life_cycle";

    /**
     * processor 生命周期方法回调，因为任务可被重试，重试的任务不会回调其生命周期方法
     */
    public static final String PROCESSOR_LIFE_CYCLE = TaskContext.class.getName() + "_internal_processor_life_cycle";

    /**
     * 存储当前用于执行任务的 taskManager. {@link TaskManager}
     */
    public static final String TASK_MANAGER = TaskContext.class.getName() + "_internal_task_manager";

    /**
     * 当任务执行完成后需要对任务进行触发回调
     */
    public static final String TASK_CALLBACK_PROPERTY = TaskContext.class.getName() + "_internal_task_callbacks";


    /**
     * 当任务执行完成后需要对TaskContext内部缓存的所有的属性进行清除, 默认为true
     */
    public static final String CLEAR_ALL_PROPERTY = TaskContext.class.getName() + "_clear_all";


    public static final TypeReference<TaskWorker<? extends Task>> WORKER_REFERENCE = new TypeReference<TaskWorker<? extends Task>>() {
    };

    public static final TypeReference<List<TaskCallback>> TASK_CALLBACKS = new TypeReference<List<TaskCallback>>() {
    };


    public TaskContext put(String key, Object value) {
        context.put(key, value);
        return this;
    }

    public TaskContext putIfAbsent(String key, Object value) {
        context.putIfAbsent(key, value);
        return this;
    }

    public TaskContext compute(String key, BiFunction<String, Object, Object> applyFunction) {
        context.compute(key, applyFunction);
        return this;
    }

    public boolean contains(String key) {
        return context.containsKey(key);
    }


    public TaskContext remove(String... keys) {
        Arrays.stream(keys).forEach(context::remove);
        return this;
    }

    TaskContext clear() {
        context.clear();
        return this;
    }


    public TaskContext putAll(Map<String, Object> another) {
        context.putAll(another);
        return this;
    }

    public TaskContext putAll(TaskContext taskContext) {
        if (taskContext != null) {
            context.putAll(taskContext.context);
        }
        return this;
    }


    public <V> V get(String key, Class<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }

    public String getString(String key) {
        return MapUtil.get(context, key, String.class);
    }

    public <V> V get(String key, TypeReference<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }

    public <V> V getOrDefault(String key, V defaultValue, Class<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }

    public <V> V getOrDefault(String key, V defaultValue, TypeReference<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }

    public Long taskId() {
        return get(TASK_ID, Long.class);
    }


    public TaskContext setTaskId(long taskId) {
        put(TASK_ID, taskId);
        return this;
    }

    public TaskWorker<? extends Task> taskWorker() {
        return get(TASK_WORKER, WORKER_REFERENCE);
    }

    public void setTaskType(int type) {
        put(INTERNAL_TYPE, new TaskType(type));
    }

    protected TaskType getTaskType() {
        return get(INTERNAL_TYPE, TaskType.class);
    }

    public TaskManager taskManager() {
        return get(TASK_MANAGER, TaskManager.class);
    }


    public List<TaskCallback> taskCallbacks() {
        return getOrDefault(TASK_CALLBACK_PROPERTY, Collections.emptyList(), TASK_CALLBACKS);
    }


    @SuppressWarnings("unchecked")
    public TaskContext addCallback(TaskCallback taskCallback) {
        List<TaskCallback> tasks = (List<TaskCallback>) context.compute(TASK_CALLBACK_PROPERTY, (key, value) -> {
            if (value == null) {
                value = new CopyOnWriteArrayList<>();
            }

            return value;
        });

        tasks.add(taskCallback);
        return this;
    }

    public String toString() {
        return context.toString();
    }

    public boolean isClearAll() {
        return getOrDefault(CLEAR_ALL_PROPERTY, true, Boolean.class);
    }

    public TaskContext setClearAll(boolean clear) {
        context.put(CLEAR_ALL_PROPERTY, clear);
        return this;
    }


    /**
     * 清除当前上下文中的所有信息
     */
    public void removeAll() {
        context.clear();
    }
}
