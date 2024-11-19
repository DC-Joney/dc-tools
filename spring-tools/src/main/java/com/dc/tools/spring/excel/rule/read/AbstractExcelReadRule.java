package com.dc.tools.spring.excel.rule.read;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.spring.excel.ExcelValidateGroup;
import com.dc.tools.spring.excel.event.ReadSheetCompleteEvent;
import com.dc.tools.spring.excel.exception.ExcelParseException;
import com.dc.tools.spring.excel.exception.ExcelTitleParseException;
import com.dc.tools.spring.excel.rule.ExcelReadRule;
import com.dc.tools.spring.exception.ParamException;
import com.dc.tools.spring.validate.CheckExpressions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;


/**
 * excel 处理规则
 *
 * @author zhangyang
 */

@Slf4j
public abstract class AbstractExcelReadRule<T> implements ExcelReadRule<T>, InitializingBean, ApplicationContextAware {

    protected static final Function<Map<String, String>, Set<String>> functionConverter =
            headerMap -> Sets.newHashSet(Maps.filterValues(headerMap, StringUtils::hasText).values());

    private final Collection<HeaderKey> headerNames;

    /**
     * 读取的所有数据
     */
    @Getter
    private final List<T> records = new ArrayList<>();

    /**
     * 读取的头部数据
     */
    @Getter
    private final Map<String, String> headerMap = new HashMap<>(8);

    /**
     * 读取完成后发布事件
     */
    private ApplicationEventPublisher eventPublisher;

    /**
     * 读取的数据 bean class
     */
    private final Class<T> dataClass;


    @Setter
    @Getter
    private ApplicationContext applicationContext;

    @Getter
    private static final LoadingCache<Class<?>, Set<String>> excelClassCache = CacheBuilder.newBuilder()
            .maximumSize(100).initialCapacity(10)
            .softValues()
            .build(new ExcelCacheLoader());

    /**
     * 校验 注解分组
     */
    protected static final Class<?>[] validateExcelGroup = new Class[]{ExcelValidateGroup.class};

    protected static final String DEFAULT_VALIDATE_HEADER_NAME = "title";

    @Override
    public void afterPropertiesSet() {

    }

    public AbstractExcelReadRule(Collection<HeaderKey> headerNames, Class<T> dataClass) {
        this.dataClass = dataClass;
        this.headerNames = headerNames;
    }


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


    @Override
    public void handleExcelData(T data, AnalysisContext context) {
        if (isNeedCheck(data, context)) {
            try {
                validRecord(data, context);
            } catch (ParamException ex) {
                log.error("Validate excel data error, cause is: ", ex);
                throw new ExcelParseException(ex.getMessage(), ex);
            }
        }

        doAfterRead(data);
        records.add(data);
    }

    /**
     * 用于校验读取完成的数据
     *
     * @param data    读取完成的数据
     * @param context 读取数据的上下文
     */
    protected void validRecord(T data, AnalysisContext context) {
        CheckExpressions.check(data, true, validateGroup());
    }


    /**
     * 校验数据的 excel group，允许子类进行自定义操作
     */
    protected Class<?>[] validateGroup() {
        return validateExcelGroup;
    }


    /**
     * 处理校验头部数据
     *
     * @param headers excel header 数据
     */
    @Override
    public void handleHeaderExcelData(Map<Integer, String> headers) {

        log.info("ExcelRule is ready completable");
        if (headerNames == null || headerNames.size() <= 0) {
            return;
        }

        headerNames.forEach(headerKey -> headerMap.put(headerKey.headerName, headers.remove(headerKey.headerNum)));
        String title = getHeaderMap().get(DEFAULT_VALIDATE_HEADER_NAME);
        if (!StringUtils.hasText(title)) {
            throw new ExcelTitleParseException("上传的Excel文件标头与模板标头不符，请按照模板修改");
        }

        Set<String> titleSet = functionConverter.apply(Splitter.on(",").withKeyValueSeparator("=").split(title));
        Set<String> excelFieldSet = getExcelClassCache().getUnchecked(dataClass);
        Sets.SetView<String> intersection = Sets.intersection(titleSet, excelFieldSet);
        if (intersection.size() < excelFieldSet.size()) {
            throw new ExcelTitleParseException("差异信息为: " + Joiner.on(",").skipNulls().join(Sets.difference(titleSet, excelFieldSet)) + " 列不匹配,请按照模板修改");
        }
    }

    @Override
    public Class<T> excelDataClass() {
        return dataClass;
    }


    @Override
    public List<T> getSyncDataList() {
        return records;
    }

    /**
     * 需要发布的事件
     */
    @Override
    public ReadExcelEvent<T> getReadEvent() {
        return CollectionUtils.isEmpty(records) ? null : new ReadExcelEvent<>(records);
    }


    @Override
    public boolean ignoreError(AnalysisContext context) {
        return false;
    }

    /**
     * 判断数据是否需要进行校验、忽略
     *
     * @param data 判断改数据是否需要校验
     * @return true 为需要，false为不需要
     */
    protected boolean isNeedCheck(T data, AnalysisContext context) {
        return true;
    }


    /**
     * 处理数据添加部分数据
     */
    protected abstract T doAfterRead(T data);


    @Subscribe
    public void clearCache(ReadSheetCompleteEvent completeEvent) {
        headerMap.clear();
        headerNames.clear();
    }


    @RequiredArgsConstructor(staticName = "of")
    protected static class HeaderKey {
        private final Integer headerNum;
        private final String headerName;
    }


    /**
     * 获取 带头 {@link ExcelProperty} 注解的 java entity
     */
    private static class ExcelCacheLoader extends CacheLoader<Class<?>, Set<String>> {

        @Override
        @NonNull
        public Set<String> load(@NonNull Class<?> classKey) {
            HashSet<String> excelFileNames = Sets.newHashSet();
            ReflectionUtils.doWithFields(classKey, field -> {
                AnnotationAttributes attributes =
                        AnnotatedElementUtils.findMergedAnnotationAttributes(field, ExcelProperty.class,
                                false, true);

                String[] values;
                if (attributes != null && (values = attributes.getStringArray("value")) != null && values.length > 0) {

                    //合并字符串
                    // do something

                    Optional<String> first = Arrays.stream(values).filter(StringUtils::hasText).findFirst();
                    first.ifPresent(excelFileNames::add);
                }
            });
            return Collections.unmodifiableSet(excelFileNames);
        }
    }

}
