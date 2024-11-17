package com.dc.tools.spring.quartz.support;

import com.dc.tools.common.annotaion.NonNull;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

/**
 * @author zhangyang
 * @date 2020-09-30
 * @see com.dc.tools.spring.quartz.annotation.ScheduleJob
 */
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class JobMetaData implements Comparable<JobMetaData> {

    /**
     * 任务名称
     */
    String jobName;

    /**
     * 组名称
     */
    String jobGroupName;

    /**
     * job bean名称
     */
    String jobRef;

    /**
     * 冗余  触发器名称
     */
    String triggerName;


    /**
     * 触发器 组名称
     */
    String triggerGroupName;

    /**
     * triggerBean名称
     */
    String triggerRef;

    /**
     * 定时任务表达式
     */
    String cron;

    String description;

    JobMethod jobMethod;


    @Override
    public int compareTo(@NonNull JobMetaData other) {
        return jobName.compareTo(other.jobName);
    }
}
