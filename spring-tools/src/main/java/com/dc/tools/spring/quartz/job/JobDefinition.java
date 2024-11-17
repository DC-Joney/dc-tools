package com.dc.tools.spring.quartz.job;

import com.dc.tools.spring.quartz.support.JobMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.quartz.JobDetail;
import org.quartz.Trigger;

/**
 * job 任务信息定义
 *
 * @author zhangyang
 * @date 2020-10-09
 */
@Getter
@AllArgsConstructor(staticName = "instance")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobDefinition {

    /**
     * 任务信息
     */
    JobDetail jobDetail;

    /**
     * 触发器信息
     */
    Trigger trigger;

    /**
     * job 执行方法
     */
    JobMethod jobMethod;


    public String getJobName() {
        return jobDetail.getKey().getName();
    }

    public String getMethodName() {
        return jobMethod.getMethodName();
    }

    public String getClassMethodName() {
        return String.format("%s: %s", jobMethod.getSimpleClassName(), jobMethod.getMethodName());
    }

}
