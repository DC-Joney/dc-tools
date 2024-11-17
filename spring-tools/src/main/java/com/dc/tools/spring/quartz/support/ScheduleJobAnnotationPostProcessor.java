package com.dc.tools.spring.quartz.support;

import com.dc.tools.spring.quartz.JobCreateException;
import com.dc.tools.spring.quartz.JobFailCallback;
import com.dc.tools.spring.quartz.JobSuccessCallback;
import com.dc.tools.spring.quartz.ProxyJob;
import com.dc.tools.spring.quartz.annotation.ScheduleJob;
import com.dc.tools.spring.quartz.job.ExtendedJobDetail;
import com.dc.tools.spring.quartz.job.JobDefinition;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static com.dc.tools.spring.quartz.annotation.ScheduleJob.*;


/**
 * ScheduleJob 注解后置处理器
 *
 * @author zhangyang
 * @date 2020-09-29
 * @see ScheduleJob
 * @see JobMethod
 * @see JobDefinition
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleJobAnnotationPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements SmartInitializingSingleton, BeanFactoryAware, DisposableBean, Ordered {

    private static final String JOB_ANNOTATION_NAME = ScheduleJob.class.getName();
    private static final String JOB_CRON_ATTRIBUTE = "cron";
    private static final String JOB_NAME_ATTRIBUTE = "name";
    private static final String JOB_GROUP_NAME_ATTRIBUTE = "jobGroup";
    private static final String TRIGGER_GROUP_NAME_ATTRIBUTE = "triggerGroup";
    private static final String TRIGGER_NAME_ATTRIBUTE = "trigger";
    private static final String JOB_REF_NAME_ATTRIBUTE = "jobRef";
    private static final String TRIGGER_REF_NAME_ATTRIBUTE = "triggerRef";
    private static final String DESCRIPTION_NAME_ATTRIBUTE = "description";
    private static final Function<Method, String> triggerFunction = generateName(TRIGGER_SUFFIX);

    private static final LoadingCache<Class<?>, Set<JobMethodInfo>> jobMethodCache = CacheBuilder.newBuilder()
            .initialCapacity(16)
            .maximumSize(64)
            .softValues()
            .concurrencyLevel(4)
            .build(new JobDefinitionCacheBuilder());

    @Setter
    BeanFactory beanFactory;

    final Scheduler scheduler;
    final ObjectProvider<List<JobSuccessCallback>> successCallbacksProvider;
    final ObjectProvider<List<JobFailCallback>> failCallbacksProvider;
    final Set<JobMetaData> jobMetaDataSet = new ConcurrentSkipListSet<>();
    final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    final PlatformTransactionManager transactionManager;

    public ScheduleJobAnnotationPostProcessor(Scheduler scheduler, PlatformTransactionManager transactionManager,
                                              ObjectProvider<List<JobSuccessCallback>> successCallbacksProvider,
                                              ObjectProvider<List<JobFailCallback>> failCallbacksProvider) {
        this.scheduler = scheduler;
        this.successCallbacksProvider = successCallbacksProvider;
        this.failCallbacksProvider = failCallbacksProvider;
        this.transactionManager = transactionManager;
    }


    private static Function<Method, String> generateName(String suffix) {
        return method -> method.toString() + suffix;
    }


    @Override
    @SuppressWarnings("Duplicates")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        Class<?> beanClass = bean.getClass();

        if (AopUtils.isAopProxy(bean))
            beanClass = ClassUtils.getUserClass(beanClass);

        Set<JobMethodInfo> methodInfos = jobMethodCache.getUnchecked(beanClass);

        if (CollectionUtils.isEmpty(methodInfos))
            return bean;

        for (JobMethodInfo methodInfo : methodInfos) {

            Method method = methodInfo.getMethod();
            MethodMetadata methodMetadata = methodInfo.getMethodMetadata();
            if (methodMetadata.isStatic() || methodMetadata.isAbstract()) {
                throw new UnsupportedOperationException("Unsupported annotation in static method or abstract method");
            }

            Map<String, Object> attributes = methodMetadata.getAnnotationAttributes(JOB_ANNOTATION_NAME, false);
            AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(attributes);
            String cronExpression = annotationAttributes.getString(JOB_CRON_ATTRIBUTE);
            String jobName = annotationAttributes.getString(JOB_NAME_ATTRIBUTE);
            String jobGroupName = annotationAttributes.getString(JOB_GROUP_NAME_ATTRIBUTE);
            String triggerName = annotationAttributes.getString(TRIGGER_NAME_ATTRIBUTE);
            String triggerGroupName = annotationAttributes.getString(TRIGGER_GROUP_NAME_ATTRIBUTE);
            String jobRefBeanName = annotationAttributes.getString(JOB_REF_NAME_ATTRIBUTE);
            String triggerRefBeanName = annotationAttributes.getString(TRIGGER_REF_NAME_ATTRIBUTE);
            String description = annotationAttributes.getString(DESCRIPTION_NAME_ATTRIBUTE);

            if (StringUtils.isEmpty(jobName) && StringUtils.isEmpty(jobRefBeanName))
                throw new IllegalArgumentException("Job name or job reference bean information cannot be empty");

            if (StringUtils.isEmpty(cronExpression) && StringUtils.isEmpty(triggerRefBeanName))
                throw new IllegalArgumentException("Cron expression or trigger reference bean information cannot be empty");

            //创建任务对应的job方法
            JobMethod jobMethod = new JobExecuteMethod(method, bean, beanFactory, discoverer, beanName);

            //创建任务元信息
            JobMetaData jobMetaData = JobMetaData.builder()
                    .jobName(jobName)
                    .jobGroupName(jobGroupName)
                    .description(description)
                    .triggerName(triggerName)
                    .triggerGroupName(triggerGroupName)
                    .triggerRef(triggerRefBeanName)
                    .cron(cronExpression)
                    .jobMethod(jobMethod)
                    .build();


            jobMetaDataSet.add(jobMetaData);


        }

        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {

        List<JobSuccessCallback> successCallbacks = Optional.ofNullable(successCallbacksProvider.getIfAvailable())
                .orElseGet(Collections::emptyList);
        List<JobFailCallback> failCallbacks = Optional.ofNullable(failCallbacksProvider.getIfAvailable())
                .orElseGet(Collections::emptyList);

        //排序回调方法
        AnnotationAwareOrderComparator.sort(successCallbacks);
        AnnotationAwareOrderComparator.sort(failCallbacks);

        for (JobMetaData jobMetaData : jobMetaDataSet) {

            //创建任务的处理器
            JobHandlerAdaptor jobHandlerAdaptor = JobHandlerAdaptor.builder()
                    .transactionManager(transactionManager)
                    .successHandlers(successCallbacks)
                    .failHandlers(failCallbacks)
                    .jobMetaData(jobMetaData).build();

            try {
                //job任务信息
                JobDetail jobDetail;

                //触发器信息
                Trigger trigger;

                //获取job的元信息
                JobMethod jobMethod = jobMetaData.getJobMethod();

                if (StringUtils.hasText(jobMetaData.getJobName())) {
                    if (StringUtils.isEmpty(jobMetaData.getJobGroupName()))
                        jobMetaData.setJobGroupName(jobMetaData.getJobName() + JOB_GROUP_SUFFIX);

                    //优先使用注解中的Job 定义信息
                    jobDetail = createJobDetail(jobHandlerAdaptor, jobMetaData);
                    if (StringUtils.hasText(jobMetaData.getJobRef()))
                        log.warn("Job definition exist on {}, the job bean reference will be ignored", jobMetaData.getJobMethod());

                } else {
                    jobDetail = beanFactory.getBean(jobMetaData.getJobRef(), JobDetail.class);
                }

                //创建Job任务信息
                jobDetail = ExtendedJobDetail.create(jobDetail, () -> JobProxyCreator.createJob(jobHandlerAdaptor, transactionManager), jobMethod.getMethod());
                JobKey jobKey = jobDetail.getKey();

                //判断job任务是否存在，如果已经存在则删除旧任务
                if (scheduler.checkExists(jobKey)) {
                    JobDetail jobDetail1 = scheduler.getJobDetail(jobKey);
                    String errorMethod = "";
                    if (jobDetail1 instanceof ExtendedJobDetail) {
                        ExtendedJobDetail detail = (ExtendedJobDetail) jobDetail1;
                        Method invokeMethod = detail.getInvokeMethod();
                        if (invokeMethod != null)
                            errorMethod = ",and the method in " + invokeMethod.getDeclaringClass() + ":" + invokeMethod.getName();
                    }
                    throw new UnsupportedOperationException("A task with the task name " + jobKey.getName() + " already exists. Please delete it and add it again " + errorMethod);
                }

                //添加任务
                scheduler.addJob(jobDetail, false);

                //优先使用注解中的 trigger注解定义信息
                if (StringUtils.hasText(jobMetaData.getCron())) {

                    //设置触发器名称
                    if (StringUtils.isEmpty(jobMetaData.getTriggerName()))
                        jobMetaData.setTriggerName(triggerFunction.apply(jobMetaData.getJobMethod().getMethod()));


                    //设置触发器组名称
                    if (StringUtils.isEmpty(jobMetaData.getTriggerGroupName())) {
                        String triggerName = jobMetaData.getTriggerName();
                        String triggerGroupName = triggerName.endsWith(TRIGGER_SUFFIX) ?
                                triggerName.replace(TRIGGER_SUFFIX, TRIGGER_GROUP_SUFFIX) :
                                triggerName + TRIGGER_GROUP_SUFFIX;
                        jobMetaData.setTriggerGroupName(triggerGroupName);
                    }

                    CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(jobMetaData.getCron());

                    trigger = TriggerBuilder.newTrigger()
                            .forJob(jobKey)
                            .withSchedule(cronScheduleBuilder).withIdentity(jobMetaData.getTriggerName(), jobMetaData.getTriggerGroupName()).build();

                    if (StringUtils.hasText(jobMetaData.getTriggerRef()))
                        log.warn("Trigger definition exist on {}, the trigger bean reference will be ignored", jobMetaData.getJobMethod());

                } else {
                    trigger = beanFactory.getBean(jobMetaData.getTriggerRef(), Trigger.class);
                }

                JobDefinition definition = JobDefinition.instance(jobDetail, trigger, jobMetaData.getJobMethod());
                jobHandlerAdaptor.setJobDefinition(definition);
                Trigger old = scheduler.getTrigger(trigger.getKey());

                if (Objects.nonNull(old)) {
                    scheduler.rescheduleJob(trigger.getKey(), trigger);
                    continue;
                }

                scheduler.scheduleJob(trigger);
            } catch (Exception ex) {
                throw new JobCreateException("Error creating timed job", ex);
            }
        }
    }


    private JobDetail createJobDetail(JobHandlerAdaptor jobHandlerAdaptor, JobMetaData metaData) {
        return JobBuilder.newJob(ProxyJob.class)
                .withIdentity(metaData.getJobName(), metaData.getJobGroupName()).storeDurably()
                .withDescription(metaData.getDescription())
                .build();
    }


    @Override
    public void destroy() throws Exception {
        jobMethodCache.invalidateAll();
        jobMetaDataSet.clear();
    }


    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private static class JobDefinitionCacheBuilder extends CacheLoader<Class<?>, Set<JobMethodInfo>> {

        @Override
        public Set<JobMethodInfo> load(@NonNull Class<?> classKey) throws Exception {
            Map<Method, JobMethodInfo> methodInfoMap = MethodIntrospector.selectMethods(classKey,
                    (MethodIntrospector.MetadataLookup<JobMethodInfo>) method -> {
                        MethodMetadata metadata = new StandardMethodMetadata(method, true);
                        return metadata.isAnnotated(ScheduleJob.class.getName())
                                ? JobMethodInfo.create(metadata, method) : null;
                    });
            return ImmutableSet.copyOf(methodInfoMap.values());
        }
    }


    @Getter
    @AllArgsConstructor(staticName = "create")
    @EqualsAndHashCode(exclude = "methodMetadata")
    private static class JobMethodInfo {
        MethodMetadata methodMetadata;
        Method method;
    }

}
