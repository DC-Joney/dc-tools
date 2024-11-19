package com.dc.tools.spring.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.exception.ExcelCommonException;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.dc.tools.common.SequenceUtil;
import com.dc.tools.common.collection.ConcurrentHashSet;
import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.common.utils.ShareTempDir;
import com.dc.tools.io.buffer.AsyncInputStream;
import com.dc.tools.spring.excel.converter.LocalDateNumberConverter;
import com.dc.tools.spring.excel.converter.LocalDateStringConverter;
import com.dc.tools.spring.excel.exception.ExcelException;
import com.dc.tools.spring.excel.rule.ExcelReadRule;
import com.dc.tools.spring.excel.rule.ExcelReadSheetRule;
import com.dc.tools.spring.excel.rule.ExcelWriteRule;
import com.dc.tools.spring.exception.BusinessException;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import reactor.util.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.READ;

/**
 * Excel读取写入工具
 *
 * @author zhangyang
 */
@SuppressWarnings("Duplicates")
@Slf4j
public class ExcelUtils implements ApplicationContextAware, ApplicationEventPublisherAware {

    private static final ShareTempDir tempExcelDir = new ShareTempDir(false, "excel");

    private ApplicationContext applicationContext;

    private static final LocalDateStringConverter dateStringConverter = new LocalDateStringConverter();
    private static final LocalDateNumberConverter dateNumberConverter = new LocalDateNumberConverter();


    private static final Set<Converter<?>> converters = new ConcurrentHashSet<>();

    static {
        converters.add(dateNumberConverter);
        converters.add(dateStringConverter);
    }

    public static Set<Converter<?>> getConverters() {
        return converters;
    }

    private ExcelUtils() {

    }

    private ApplicationEventPublisher eventPublisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


    /**
     * 读取Excel 数据
     *
     * @param fileChannel     excel 文件channel
     * @param excelReadRules  读取excel文件的规则
     * @param convertFunction 读取的结果，将结果转成想要的结果
     */
    public static <T> T readExcel(FileChannel fileChannel,
                                  List<ExcelReadSheetRule<?>> excelReadRules,
                                  Function<Context, T> convertFunction) {

        InputStream inputStream = Channels.newInputStream(fileChannel);
        return readExcel(inputStream, excelReadRules, convertFunction);
    }

    /**
     * @param inputStream    读取的文件流
     * @param excelReadRules excel Sheet页面的读取规则
     */
    public static <T> T readExcel(InputStream inputStream,
                                  List<ExcelReadSheetRule<?>> excelReadRules,
                                  Function<Context, T> convertFunction) {

        Assert.notEmpty(excelReadRules, "Read excelReadRules must not be empty");

        ExcelReaderBuilder readerBuilder = EasyExcel.read(inputStream)
                .registerConverter(getSharedInstance().dateStringConverter)
                .registerConverter(getSharedInstance().dateNumberConverter);

        ExcelReader excelReader = null;

        try {
            excelReader = readerBuilder
                    .ignoreEmptyRow(true)
                    .build();

        } catch (Exception ex) {
            String exceptionMessage = ex.getMessage();

            if (ex instanceof ExcelCommonException && exceptionMessage.contains("You can try specifying the 'excelType' yourself"))
                throw new BusinessException("Excel文件不兼容,请从新另存为一个新的Excel文件后上传");

            throw new BusinessException("读取Excel失败，请及时联系管理员", ex);
        }

        //用于回调通知当前已经完成的数据处理
        ExtendedEventBus eventBus = new ExtendedEventBus("ImportExcel");

        //excel读取数据管理器
        SyncExcelReadManagement excelManagement = new SyncExcelReadManagement(eventBus, excelReader);
        excelManagement.setApplicationContext(getSharedInstance().applicationContext);
        excelManagement.setApplicationEventPublisher(getSharedInstance().eventPublisher);

        //返回读取的event事件
        return excelManagement.readExcel(excelReadRules).doSync(convertFunction);

    }


    /**
     * 异步读取excel文件
     *
     * @param inputStream   数据流
     * @param readSheetRule 读取的规则
     * @param executor      executor
     */
    public static <T> CompletableFuture<SheetValues<T>> readExcelSheetAsync(InputStream inputStream, ExcelReadSheetRule<T> readSheetRule, Executor executor) {
        return CompletableFuture.supplyAsync(() -> readExcelSheet(inputStream, readSheetRule), executor);
    }

    /**
     * 异步读取excel文件
     *
     * @param inputStream   异步数据流
     * @param readSheetRule 读取的规则
     * @param executor      executor
     */
    public static <T> CompletableFuture<SheetValues<T>> readExcelSheetAsync(AsyncInputStream inputStream, ExcelReadSheetRule<T> readSheetRule, Executor executor) {
        return inputStream.getInputStream()
                .thenApplyAsync(stream -> readExcelSheet(stream, readSheetRule), executor);
    }

    /**
     * @param inputStream 读取的文件流
     * @param readRule    excel Sheet页面的读取规则
     */
    @SuppressWarnings("unchecked")
    public static <T> SheetValues<T> readExcelSheet(InputStream inputStream, ExcelReadSheetRule<T> readRule) {

        Assert.notNull(readRule, "Read excelReadRules must not be null");

        ExcelReaderBuilder readerBuilder = EasyExcel.read(inputStream)
                .registerConverter(getSharedInstance().dateStringConverter)
                .registerConverter(getSharedInstance().dateNumberConverter);

        ExcelReader excelReader = null;

        try {
            excelReader = readerBuilder
                    .ignoreEmptyRow(true)
                    .build();

        } catch (Exception ex) {
            String exceptionMessage = ex.getMessage();

            if (ex instanceof ExcelCommonException && exceptionMessage.contains("You can try specifying the 'excelType' yourself"))
                throw new BusinessException("Excel文件不兼容,请从新另存为一个新的Excel文件后上传");

            throw new BusinessException("读取Excel失败，请及时联系管理员", ex);
        }

        //用于回调通知当前已经完成的数据处理
        ExtendedEventBus eventBus = new ExtendedEventBus("ImportExcel");

        //excel读取数据管理器
        SyncExcelReadManagement excelManagement = new SyncExcelReadManagement(eventBus, excelReader);
        excelManagement.setApplicationContext(getSharedInstance().applicationContext);
        excelManagement.setApplicationEventPublisher(getSharedInstance().eventPublisher);

        Function<Context, SheetValues<T>> convertFunction = context ->
                (SheetValues<T>) context.get((Object) readRule.excelDataClass());


        //返回读取的event事件
        return excelManagement.readExcel(Collections.singletonList(readRule)).doSync(convertFunction);

    }

    /**
     * 读取excel文件的所有sheet页
     *
     * @param inputStream 数据流
     */
    public static List<ReadSheet> readSheets(InputStream inputStream) {
        try (CloseTasks closeTasks = new CloseTasks()) {

            ExcelReader excelReader = null;

            try {
                excelReader = EasyExcelFactory.read(inputStream)
                        .ignoreEmptyRow(true)
                        .build();

                closeTasks.addTask(excelReader::finish, "Close excel reader task");

            } catch (Exception ex) {
                String exceptionMessage = ex.getMessage();

                if (ex instanceof ExcelCommonException && exceptionMessage.contains("You can try specifying the 'excelType' yourself"))
                    throw new BusinessException("Excel文件不兼容,请从新另存为一个新的Excel文件后上传");

                throw new BusinessException("读取Excel失败，请及时联系管理员", ex);
            }


            return excelReader.excelExecutor().sheetList();
        }
    }


    /**
     * 读取单个Sheet页的内容
     *
     * @param inputStream 读取的数据流
     * @param readSheet   单个sheet页
     * @param readRule    读取的规则
     */
    @SuppressWarnings("Duplicated")
    public static <T> CompletableFuture<SheetValues<T>> readSheetAsync(InputStream inputStream, ReadSheet readSheet, ExcelReadRule<T> readRule, Executor executor) {
        return CompletableFuture.supplyAsync(() -> readSheet(inputStream, readSheet, readRule), executor);
    }


    /**
     * 读取单个Sheet页的内容
     *
     * @param inputStream 读取的数据流
     * @param readSheet   单个sheet页
     * @param readRule    读取的规则
     */
    @SuppressWarnings("Duplicated")
    public static <T> SheetValues<T> readSheet(InputStream inputStream, ReadSheet readSheet, ExcelReadRule<T> readRule) {

        Assert.notNull(readRule, "Read excelReadRules must not be null");

        ExcelReaderBuilder readerBuilder = EasyExcel.read(inputStream)
                .registerConverter(getSharedInstance().dateStringConverter)
                .registerConverter(getSharedInstance().dateNumberConverter);

        ExcelReader excelReader = null;

        try {
            excelReader = readerBuilder
                    .ignoreEmptyRow(true)
                    .build();

        } catch (Exception ex) {
            String exceptionMessage = ex.getMessage();

            if (ex instanceof ExcelCommonException && exceptionMessage.contains("You can try specifying the 'excelType' yourself"))
                throw new BusinessException("Excel文件不兼容,请从新另存为一个新的Excel文件后上传");

            throw new BusinessException("读取Excel失败，请及时联系管理员", ex);
        }


        //excel读取数据管理器
        SyncExcelSheet excelManagement = new SyncExcelSheet(excelReader, readSheet);
        excelManagement.setApplicationContext(getSharedInstance().applicationContext);

        return excelManagement.readExcel(readRule);
    }


    /**
     * @param inputStream    读取的文件流
     * @param excelReadRules excel Sheet页面的读取规则
     */
    @Deprecated
    public static <T> T readMultiExcel(InputStream inputStream,
                                       ExcelReadRule<?> readRule,
                                       Function<Context, T> convertFunction) {

        Assert.notNull(readRule, "Read excelReadRules must not be empty");

        ExcelReaderBuilder readerBuilder = EasyExcel.read(inputStream)
                .registerConverter(getSharedInstance().dateStringConverter)
                .registerConverter(getSharedInstance().dateNumberConverter);

        ExcelReaderBuilder excelReaderBuilder = readerBuilder
                .ignoreEmptyRow(true);


        //excel读取数据管理器
        MultiSyncExcelReadManagement excelManagement = new MultiSyncExcelReadManagement(excelReaderBuilder);
        excelManagement.setApplicationContext(getSharedInstance().applicationContext);
        excelManagement.setApplicationEventPublisher(getSharedInstance().eventPublisher);

        //返回读取的event事件
        return excelManagement.readExcel(Collections.emptyList()).doSync(null);

    }


    /**
     * 根据模板文件 导出 excel数据
     *
     * @param rules        导出规则
     * @param stream       将合并出的excel 数据写入带哪个输出流
     * @param templateFile 模版文件
     * @return
     */
    public static void writeExcel(OutputStream stream, Collection<ExcelWriteRule<?>> rules, Path templateFile) {

        try {

            Assert.notEmpty(rules, "Read excel is errored，write rules must not be null");
            Stopwatch stopwatch = Stopwatch.createStarted();

            ExcelWriterBuilder writerBuilder = EasyExcel.write(stream).withTemplate(Files.newInputStream(templateFile, READ));
            addCustomConverter(writerBuilder);

            ExcelWriter writer = writerBuilder.build();
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
            stopwatch.stop();

            log.info("Build ExcelWriter : {} ", stopwatch.elapsed());

            StopWatch stopWatch = new StopWatch("Write excel watch");
            rules.forEach(writeRule -> writeCoreExcel(writer, fillConfig, writeRule, stopWatch));
            log.info("\n" + stopWatch.prettyPrint());

            writer.finish();

        } catch (IOException ex) {
            throw new ExcelException("导出文件失败，错误原因", ex);
        }
    }

    public static Path writeExcel(Path templateFile, Collection<ExcelWriteRule<?>> rules) {
        Assert.notEmpty(rules, "Read excel is errored，write rules must not be null");
        String fileId = SequenceUtil.nextIdString();
        String extension = FilenameUtils.getExtension(templateFile.getFileName().toString());
        Path newExcelPath = tempExcelDir.newChild(Paths.get(fileId + "." + extension));
        CloseTasks closeTasks = new CloseTasks();

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            ExcelWriterBuilder writerBuilder = EasyExcel.write(newExcelPath.toFile())
                    .withTemplate(Files.newInputStream(templateFile, READ));
            addCustomConverter(writerBuilder);

            ExcelWriter writer = writerBuilder.build();

            closeTasks.addTask(writer::finish, "Close excel writer for {}", templateFile);
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
            stopwatch.stop();

            log.info("Build ExcelWriter : {} ", stopwatch.elapsed());

            StopWatch stopWatch = new StopWatch("Write excel watch");
            rules.forEach(writeRule -> writeCoreExcel(writer, fillConfig, writeRule, stopWatch));
            log.info("\n" + stopWatch.prettyPrint());

        } catch (IOException ex) {
            throw new ExcelException("导出文件失败，错误原因", ex);
        } finally {
            closeTasks.close();
        }

        return newExcelPath;
    }

    private static void addCustomConverter(ExcelWriterBuilder writerBuilder) {
        converters.forEach(writerBuilder::registerConverter);
    }


    /**
     * 根据模板文件 导出 excel数据
     *
     * @param rules        导出规则
     * @param templateFile 模版文件
     */
    public static CompletableFuture<Path> writeExcelAsync(Path templateFile, Collection<ExcelWriteRule<?>> rules, Executor executor) {
        CompletableFuture<Path> writeFuture = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                Path newExcelPath = writeExcel(templateFile, rules);
                writeFuture.complete(newExcelPath);
            } catch (Exception ex) {
                writeFuture.completeExceptionally(ex);
            }
        });

        return writeFuture;
    }

    private static void writeCoreExcel(ExcelWriter writer, FillConfig config, ExcelWriteRule<?> writeRule, StopWatch stopWatch) {

        WriteSheet writeSheet = EasyExcel.writerSheet(writeRule.sheetNo()).head(writeRule.headerClass())
                .registerWriteHandler(writeRule).build();

        stopWatch.start(String.format("sheet %s", writeSheet.getSheetNo()));
        writer.fill(writeRule.fillDataList(), config, writeSheet);

        if (!writeRule.getHeaderMap().isEmpty()) {
            writer.fill(writeRule.getHeaderMap(), writeSheet);
        }

        stopWatch.stop();
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    public static ExcelUtils getSharedInstance() {
        return ExcelUtilsHolder.INSTANCE;
    }

    public static <T> void addConverter(Converter<T> converter) {
        converters.add(converter);
    }

    public static  void addConverters(Collection<Converter<?>> converters) {
        ExcelUtils.converters.addAll(converters);
    }

    private static class ExcelUtilsHolder {
        private static final ExcelUtils INSTANCE = new ExcelUtils();
    }

}
