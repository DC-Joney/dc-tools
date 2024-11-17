package com.dc.tools.spring.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.google.common.eventbus.Subscribe;
import com.turing.common.excel.event.ReadSheetCompleteEvent;
import com.turing.common.excel.exception.ExcelException;
import com.turing.common.excel.rule.ExcelReadRule;
import com.turing.common.excel.sheet.ExcelReadListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * 同步读取 单Excel 文件 多sheet页面
 *
 * @author zhangyang
 */
@Slf4j
@SuppressWarnings("Duplicated")
@NotThreadSafe
public class SyncExcelSheet implements ApplicationContextAware {

    /**
     * 是否读取错误
     */
    @Getter
    private boolean readError;

    @Getter
    private final ExtendedEventBus eventBus;

    @Setter
    @Getter
    protected ApplicationContext applicationContext;

    //事件发布器
    @Getter
    private ApplicationEventPublisher eventPublisher;

    /**
     * 构建每一个Sheet页的规则
     */
    private final ReadSheet readSheet;

    /**
     * 用于读取Excel数据
     */
    private final ExcelReader excelReader;

    public SyncExcelSheet(ExcelReader excelReader, ReadSheet readSheet) {
        this.eventBus = new ExtendedEventBus("Single sheet event bus");
        this.excelReader = excelReader;
        this.readSheet = readSheet;
    }


    public <T> SheetValues<T> readExcel(ExcelReadRule<T> readRule) {
        try {
            Assert.notNull(readRule, "The read excelRule must not be null");
            //规则应用到spring中
            this.initializeExcelRule(readRule);
            //为每一个规则初始化一个ExcelReadListener
            ExcelReadListener<?> listener = new ExcelReadListener<>(readRule);
            //将当前的Management 与 readRule 注册到 EventBus 中
            this.registerToEventBus(listener);

            //构建每一个ReadSheet
            ReadSheet newReadSheet = EasyExcel.readSheet(readSheet.getSheetNo(), readSheet.getSheetName())
                    .head(readRule.excelDataClass())
                    .autoTrim(false)
                    .headRowNumber(readRule.headerNum())
                    .registerReadListener(listener).build();

            excelReader.read(newReadSheet).finish();


            //判断是否读取错误,如果读取没有错误则返回读取后的数据
            if (!isReadError()) {
                //获取读取的结果
                List<T> syncDataList = readRule.getSyncDataList();
                return SheetValues.create(syncDataList, readSheet.getSheetName());
            }


        } finally {
            //将eventBus中所有的订阅者全部清空
            eventBus.unregisterAll();
            //关闭excel阅读器
            excelReader.close();
        }

        throw new ExcelException("数据解析异常");
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
        }

    }


}
