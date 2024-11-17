package com.dc.tools.spring.excel.sheet;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.dc.tools.spring.excel.ExtendedEventBus;
import com.dc.tools.spring.excel.event.ReadSheetCompleteEvent;
import com.dc.tools.spring.excel.exception.ExcelException;
import com.dc.tools.spring.excel.exception.ExcelParseException;
import com.dc.tools.spring.excel.exception.ExcelTitleParseException;
import com.dc.tools.spring.excel.rule.ExcelReadRule;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * 一个sheet页面对应一个ReadListener
 *
 * @param <T>
 * @author zhangyang
 */
@Slf4j
@RequiredArgsConstructor
public class ExcelReadListener<T> extends AnalysisEventListener<T> {

    /**
     * excel 读取规则
     */
    @Getter
    private final ExcelReadRule<T> excelReadRule;

    /**
     * 是否读取结束
     */
    private boolean readCompleted;

    @Getter
    private ExtendedEventBus eventBus;

    @Getter
    private final Map<Integer, String> headerMap;

    /**
     * 是否已经初始化完成 sheet header
     */
    private boolean initHeader = false;

    /**
     * 每一行excel header
     */
    private int headerNum = 0;

    public ExcelReadListener(ExcelReadRule<T> excelReadRule) {
        this.excelReadRule = excelReadRule;
        this.headerMap = new HashMap<>(8);
    }

    /**
     * 做头部解析
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        Map<Integer, String> excelHeadMap = Maps.filterValues(headMap, StringUtils::hasText);
        String headerMap = Joiner.on(",").withKeyValueSeparator("=").join(excelHeadMap);
        //将每一行header的数据放入到map中，以保证在实际处理数据前来对header进行处理
        this.headerMap.put(headerNum++, headerMap);
    }


    @Override
    public void invoke(T data, AnalysisContext context) {

        //在实际处理数据前判断是否已经处理过header，如果没有则进行处理
        if (!initHeader) {
            try {
                excelReadRule.handleHeaderExcelData(headerMap);
            } finally {
                //处理完成后清除headerMap
                headerMap.clear();
                initHeader = true;
            }
        }

        //判断是否读取结束，在特定的场景下可能存在一些结尾结束的字符，但是由于easyexcel无法解析会抛出异常，这时候需要判断是否已经抛出过异常了
        if (!readCompleted) {
            excelReadRule.handleExcelData(data, context);
        }
    }


    /**
     * 读取完整个excel之后会进行回调
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (eventBus != null) {
            readComplete(false, context);
        }

        log.info("Excel 文件读取完成");
    }


    /**
     * 判断是否还有下一行数据
     * @param context 解析excel 行的上下文
     */
    @Override
    public boolean hasNext(AnalysisContext context) {

        //如果已经读取完成则返回false
        if (readCompleted) {
            return false;
        }

        //判断读取当前excel sheet 是否已经读取到底了，因为对于一些特殊的场景可能存在尾部出现特定字符串的情况
        if (excelReadRule.endCondition(context)) {
            return !postSuccess(context);
        }

        return true;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("Read excel error : {}", exception.getMessage());

        if (log.isErrorEnabled())
            log.error("Parse excel exceptions, exception : ", exception);

        //判断错误是否需要被忽略
        if (excelReadRule.ignoreError(context))
            return;

        try {
            ReadSheetHolder sheetHolder = context.readSheetHolder();
            ReadRowHolder rowHolder = context.readRowHolder();
            String message = "Sheet 页" + (sheetHolder.getSheetNo() + 1) + " ( " + sheetHolder.getSheetName() + " ) : ";
            if (exception instanceof ExcelParseException || exception instanceof ExcelTitleParseException) {
                if (exception instanceof ExcelParseException) {
                    message = message + "第" + (rowHolder.getRowIndex() + 1) + "行数据格式出错: " + exception.getMessage();
                }
                if (exception instanceof ExcelTitleParseException) {
                    message = message + "Excel表头错误: " + exception.getMessage();
                }
                throw new ExcelException(message);
            }

            //判断当前数据是否已经解析到最后一行
            boolean executeEnd = excelReadRule.endCondition(context);

            //如果没有解析到最后一行则抛出错误
            if (!executeEnd) {
                message = message + "第" + (rowHolder.getRowIndex() + 1) + "行信息格式出错，请比对校验";
                throw new ExcelException(message);
            }
        } catch (Exception ex) {
            //如果没有解析完成则释放缓存，卸载eventBus
            if (!readCompleted) {
                readComplete(true, context);
            }

            throw ex;
        }

        postSuccess(context);
    }


    /**
     * 通知 excelRule发布数据存储
     */
    private boolean postSuccess(AnalysisContext context) {
        doAfterAllAnalysed(context);
        return readCompleted = true;
    }

    /**
     * 注册eventBus，以及向EventBus 注册事件对象
     *
     * @param excelManagement excel文件读取管理器
     */
    public void register(Object excelManagement, ExtendedEventBus eventBus) throws Exception {
        this.eventBus = eventBus;
        eventBus.register(excelReadRule);
        eventBus.register(excelManagement);
    }

    /**
     * 清空缓存，然后卸载eventBus
     *
     * @param errorState 读取成功或者失败
     */
    private void readComplete(boolean errorState, AnalysisContext context) {
        log.info("unRegister method execute !!!!!!! =====> {}", excelReadRule.excelDataClass());

        //发送执行结束事件
        eventBus.post(ReadSheetCompleteEvent.instance(errorState, context.readSheetHolder().getSheetName(), excelReadRule));
        headerMap.clear();
        eventBus = null;
    }

}
