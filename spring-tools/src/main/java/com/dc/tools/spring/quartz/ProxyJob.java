package com.dc.tools.spring.quartz;

import org.quartz.Job;

/**
 * 空接口，用于标注自己的定时任务实现
 *
 * @author zhangyang
 * @date 2020-10-09
 * @see com.turing.common.quartz.support.JobHandlerAdaptor
 * @see com.turing.common.quartz.support.JobProxyCreator
 */
public interface ProxyJob extends Job {


}
