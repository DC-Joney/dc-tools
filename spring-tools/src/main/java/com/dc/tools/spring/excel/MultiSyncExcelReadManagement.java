package com.dc.tools.spring.excel;

import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.exception.ExcelCommonException;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.spring.excel.event.ReadSheetCompleteEvent;
import com.dc.tools.spring.excel.exception.ExcelException;
import com.dc.tools.spring.excel.rule.ExcelReadRule;
import com.dc.tools.spring.excel.rule.ExcelReadSheetRule;
import com.dc.tools.spring.excel.sheet.ExcelReadListener;
import com.dc.tools.spring.exception.BusinessException;
import com.dc.tools.spring.utils.BusinessAssert;
import com.google.common.eventbus.Subscribe;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;
import reactor.util.context.Context;

import java.util.List;
import java.util.function.Function;

/**
 * TODO: 代码还没有完善需要进行完善
 */
@Deprecated
@Slf4j
public class MultiSyncExcelReadManagement implements ApplicationContextAware, ExcelReadManagement {

    private ApplicationEventPublisher publisher;


    private final ExtendedEventBus eventBus;

    @Setter
    private ApplicationContext applicationContext;

    private ExcelReaderBuilder readerBuilder;

    protected Context excelContext = Context.empty();


    private boolean readError;

    private ExcelReader excelReader;

    public MultiSyncExcelReadManagement(ExcelReaderBuilder readerBuilder) {
        this.eventBus = new ExtendedEventBus("Multi read excel");
        this.readerBuilder = readerBuilder;
    }

    @Override
    public MultiSyncExcelReadManagement readExcel(List<ExcelReadSheetRule<?>> readRules) {
        BusinessAssert.isTrue(!readRules.isEmpty(), "Read rules size must be greater than 0");
        ExcelReadRule<?> excelRule = readRules.get(0);
        ExcelReadListener<?> readListener = new ExcelReadListener<>(excelRule);

        try {
            this.excelReader = readerBuilder
                    .registerReadListener(readListener)
                    .build();

        } catch (Exception ex) {
            String exceptionMessage = ex.getMessage();

            if (ex instanceof ExcelCommonException && exceptionMessage.contains("You can try specifying the 'excelType' yourself"))
                throw new BusinessException("Excel文件不兼容,请从新另存为一个新的Excel文件后上传");

            throw new BusinessException("读取Excel失败，请及时联系管理员", ex);
        }


        Assert.notNull(excelRule, "The read excelRule must not be null");
        //规则应用到spring中
        this.initializeExcelRule(excelRule);
        //为每一个规则初始化一个ExcelReadListener
        ExcelReadListener<?> listener = new ExcelReadListener<>(excelRule);
        //将当前的Management 与 readRule 注册到 EventBus 中
        this.registerToEventBus(listener);

        return this;
    }


    public <T> T doSync(Function<Context, T> convertFunction) {



        excelReader.readAll();

        return null;
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

    protected void clearLocalRead() {
        //help gc
        excelContext = null;
        //将eventBus中所有的订阅者全部清空
        eventBus.unregisterAll();
        //关闭excel阅读器
        excelReader.close();
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
            return;
        }

        ExcelReadRule<?> readRule = completeEvent.getReadRule();


    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }
}
