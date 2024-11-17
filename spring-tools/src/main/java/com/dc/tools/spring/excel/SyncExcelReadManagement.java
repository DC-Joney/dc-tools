package com.dc.tools.spring.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.builder.ExcelReaderSheetBuilder;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.dc.tools.spring.excel.event.ReadSheetCompleteEvent;
import com.dc.tools.spring.excel.exception.ExcelException;
import com.dc.tools.spring.excel.rule.ExcelReadRule;
import com.dc.tools.spring.excel.rule.ExcelReadSheetRule;
import com.dc.tools.spring.excel.sheet.ExcelReadListener;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.util.context.Context;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 同步读取 单Excel 文件 多sheet页面
 *
 * @author zhangyang
 */
@Slf4j
@NotThreadSafe
public class SyncExcelReadManagement implements ExcelReadManagement, ApplicationContextAware {

    @Getter
    private boolean readError;

    protected final ExtendedEventBus eventBus;

    protected Context excelContext = Context.empty();

    @Setter
    @Getter
    protected ApplicationContext applicationContext;

    //事件发布器
    @Getter
    private ApplicationEventPublisher eventPublisher;

    /**
     * 构建每一个Sheet页的规则
     */
    @Getter
    private final Map<Class<? extends ExcelReadRule>, SheetRule> sheetRules;

    /**
     * 用于读取Excel数据
     */
    private final ExcelReader excelReader;

    public SyncExcelReadManagement(ExtendedEventBus eventBus, ExcelReader excelReader) {
        this.eventBus = eventBus;
        this.excelReader = excelReader;
        this.sheetRules = new HashMap<>(8);
    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        this.eventPublisher = publisher;
    }

    @Override
    public SyncExcelReadManagement readExcel(List<ExcelReadSheetRule<?>> readRules) {
        //循环读取所有Sheet页数据
        for (ExcelReadSheetRule<?> excelRule : readRules) {
            Assert.notNull(excelRule, "The read excelRule must not be null");
            //规则应用到spring中
            this.initializeExcelRule(excelRule);
            //为每一个规则初始化一个ExcelReadListener
            ExcelReadListener<?> listener = new ExcelReadListener<>(excelRule);
            //将当前的Management 与 readRule 注册到 EventBus 中
            this.registerToEventBus(listener);
            //构建每一个ReadSheet

            SheetRule sheetRule = new SheetRule(buildSheet(excelRule, listener), excelRule);
            sheetRules.put(excelRule.getClass(), sheetRule);
        }

        return this;
    }

    /**
     * 初始化 Excel Rule 绑定Spring Bean
     */
    public void initializeExcelRule(ExcelReadRule<?> excelReadRule) {
        applicationContext.getAutowireCapableBeanFactory()
                .autowireBeanProperties(excelReadRule, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        applicationContext.getAutowireCapableBeanFactory()
                .initializeBean(excelReadRule, excelReadRule.getClass().getSimpleName());
    }


    /**
     * 构建读取Sheet 页面
     *
     * @param excelReadRule 当前Sheet页面读取的规则
     * @param listener      挂载到哪个监听器上面
     */
    private ReadSheet buildSheet(ExcelReadSheetRule<?> excelReadRule, ExcelReadListener<?> listener) {

        ExcelReaderSheetBuilder sheetBuilder = null;

        if (excelReadRule.sheetIndex() >= 0) {
            sheetBuilder = EasyExcel.readSheet(excelReadRule.sheetIndex());
        }

        if (StringUtils.hasText(excelReadRule.sheetName()) && sheetBuilder == null) {
            sheetBuilder = EasyExcel.readSheet(excelReadRule.sheetName());
        } else if (sheetBuilder != null) {
            sheetBuilder = sheetBuilder.sheetName(excelReadRule.sheetName());
        }

        if (sheetBuilder == null) {
            throw new UnsupportedOperationException("Cannot find sheet index or sheet name");
        }

        return sheetBuilder
                .head(excelReadRule.excelDataClass())
                .autoTrim(false)
                .headRowNumber(excelReadRule.headerNum())
                .registerReadListener(listener).build();
    }


    /**
     * 将当前的Management 与 监听器注册到EventBus 中，当Excel 页面读取完成时，进行回调
     */
    private void registerToEventBus(ExcelReadListener<?> readListener) {
        try {
            readListener.register(this, eventBus);
        } catch (Exception e) {
            log.error("注册sheet单元出错，错误信息为 :", e);
            throw new ExcelException("解析EXCEL表格出错，请及时联系管理员");
        }
    }


    /**
     * 同步读取Excel 数据
     */
    public <T> T doSync(Function<Context, T> convertFunction) {
        try {

            for (SheetRule sheetRule : sheetRules.values()) {
                //进行数据读取
                excelReader.read(sheetRule.readSheet);
                //判断是否读取错误,如果读取错误则直接返回，不在读取
                if (isReadError())
                    break;


                //获取读取的结果
                List<?> syncDataList = sheetRule.readRule.getSyncDataList();

                //将数据添加到返回结果中
                excelContext = excelContext.put(sheetRule.readRule.excelDataClass(), SheetValues.create(syncDataList,sheetRule.sheetName));
            }

            if (!isReadError()) {
                Objects.requireNonNull(convertFunction, "The convertFunction must not be null");
                return Objects.requireNonNull(convertFunction.apply(excelContext), "Custom event must not be null");
            }

        } finally {
            //help gc
            excelContext = null;
            //清空所有的读取规则
            sheetRules.clear();
            //将eventBus中所有的订阅者全部清空
            eventBus.unregisterAll();
            //关闭excel阅读器
            excelReader.close();
        }

        throw new ExcelException("数据解析异常");
    }



    /**
     * 监听读取完成事件
     *
     * @param completeEvent 读取完成事件
     */
    @Subscribe
    public void readCompleted(ReadSheetCompleteEvent completeEvent) {
        eventBus.unregister(this);

        //判断读取是否出错，如果出错则抛出异常
        if (completeEvent.isErrorState()) {
            readError = true;
        }

        SheetRule sheetRule = sheetRules.get(completeEvent.getReadRule().getClass());

        //将当前sheet页的名称添加
        if (sheetRule != null) {
            sheetRule.setSheetName(completeEvent.getSheetName());
        }
    }


    @RequiredArgsConstructor
    private static class SheetRule {
        final ReadSheet readSheet;
        final ExcelReadRule<?> readRule;

        /**
         * sheet name
         */
        @Setter
        @Getter
        private String sheetName;
    }
}
