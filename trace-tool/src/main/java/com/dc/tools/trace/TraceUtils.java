package com.dc.tools.trace;


import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Optional;


/**
 * 用于获取链路id，以及恢复链路id
 *
 * @author zhangyang
 */
@UtilityClass
public class TraceUtils {

    private static volatile String REQUEST_ID;

    public static final String LOG_TRACE_ID = "traceId";

    private static final String OLD_REQUEST_ID = "old_request_id";

    /**
     * 生成唯一ID，不需要保证服务的不重复，只作用于单个服务请求
     */
    private final Sequence sequence = new Sequence();

    public static void initTrace(String logPrefix) {
        REQUEST_ID = logPrefix;
    }

    /**
     * 获取当前链路的id
     */
    public String dumpTrace() {
        return Optional.ofNullable(MDC.get(REQUEST_ID))
                .filter(StringUtils::hasText)
                //只有在定时任务执行时才会缺少requestId
                .orElseGet(TraceUtils::decorate);
    }

    public Optional<String> getTraceId() {
        return Optional.ofNullable(MDC.get(REQUEST_ID));
    }

    /**
     * 当前链路是否包含链路id
     */
    public boolean containsTraceId() {
        return Optional.ofNullable(MDC.get(REQUEST_ID))
                .filter(StringUtils::hasText).isPresent();
    }

    /**
     * 获取当前链路的id
     */
    public String dumpScheduleTrace() {
        return Optional.ofNullable(MDC.get(REQUEST_ID))
                .filter(StringUtils::hasText)
                //只有在定时任务执行时才会缺少requestId
                .orElseGet(TraceUtils::decorate);
    }

    /**
     * 作用于定时任务的全局ID
     */
    private String decorate() {
        long uniqueId = sequence.nextId();
        return "" + uniqueId;
    }

    /**
     * 作用于定时任务的全局ID
     */
    private String decorateSchedule() {
        long uniqueId = sequence.nextId();
        return "schedule_" + uniqueId;
    }

    /**
     * 将链路id放入到 {@link MDC} 中
     *
     * @param asyncTrace 异步任务
     */
    public void intoTrace(AsyncTrace asyncTrace) {
        MDC.put(REQUEST_ID, asyncTrace.requestId());
    }


    /**
     * 将链路id放入到 {@link MDC} 中
     *
     * @param requestId 链路id
     */
    public void intoTraceIfNotEmpty(String requestId) {
        if (StringUtils.hasText(requestId))
            MDC.put(REQUEST_ID, requestId);
    }

    /**
     * 将链路id放入到 {@link MDC} 中, 并且将之前的链路id保存起来
     *
     * @param requestId 链路id
     */
    public void intoNewTrace(String requestId) {
        if (StringUtils.hasText(requestId)) {
            String oldTraceId = MDC.get(REQUEST_ID);
            if (StringUtils.hasText(oldTraceId)) {
                MDC.put(OLD_REQUEST_ID, oldTraceId);
            }

            MDC.put(REQUEST_ID, requestId);
        }

    }

    /**
     * 将旧的链路id放入到 {@link MDC} 中
     */
    public void restoreOldTraceId() {
        String oldTraceId = MDC.get(OLD_REQUEST_ID);
        if (StringUtils.hasText(oldTraceId)) {
            MDC.put(REQUEST_ID, oldTraceId);
        }
    }

    /**
     * 将链路id放入到 {@link MDC} 中
     *
     * @param requestId 链路id
     */
    public void intoTrace(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }

    /**
     * 清除 {@link MDC} 中的链路id
     */
    public void remove() {
        MDC.remove(REQUEST_ID);
    }




}
