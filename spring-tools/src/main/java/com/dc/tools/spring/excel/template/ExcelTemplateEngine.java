package com.dc.tools.spring.excel.template;/*
package com.chamc.fundPosition.common.excel.template;

import com.chamc.boot.web.support.BussinessException;
import com.google.common.base.Charsets;
import com.google.common.io.Closer;
import freemarker.template.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.servlet.http.HttpServletRequest;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExcelTemplateEngine implements InitializingBean {

    Configuration configuration;

    final FreeMarkerConfigurer configurer;

    @Getter
    String encoding = "UTF-8";

*
     * 初始化状态


    AtomicBoolean tempFileState = new AtomicBoolean(false);

    private static final String DEFAULT_EXCEL_EXPORT_FILE_PREFIX = "EXPORT_EXCEL_";

    ThreadLocalRandom localRandom = ThreadLocalRandom.current();

*
     * 异常传递


    AtomicReference<Exception> rethrowReference = new AtomicReference<>();


*
     * 文件创建延迟


    AtomicBoolean delayReset = new AtomicBoolean(false);


    @Setter
    @Getter
    private volatile Path tempDirectory;

    @Override
    public void afterPropertiesSet() throws Exception {

        configuration = configurer.getConfiguration();
//        configuration = new Configuration(Configuration.VERSION_2_3_26);
        configuration.setDefaultEncoding(Charsets.UTF_8.name());
        configuration.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_26));
//        configuration.setSharedVariable("ComplexCodeUtile", ComplexCodeUtile.getInstance());
        configuration.setTemplateUpdateDelay(0);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    @SneakyThrows
    protected void initTemplateDir() {
        if (getTempDirectory() == null) {

            if (tempFileState.compareAndSet(false, true)) {
                try {

                    //初始化异常
                    rethrowReference.set(null);

                    Path tempDirectory = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), DEFAULT_EXCEL_EXPORT_FILE_PREFIX +
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".");

                    //创建临时文件
                    setTempDirectory(tempDirectory);

                } catch (Exception e) {

                    log.warn("Execute handle tempThread fail,The thread name is{}, error cause is {}", Thread.currentThread().getName(), e);

                    //将异常记录传播
                    rethrowReference.compareAndSet(null, e);

                }

            }
            for (; ; ) {

                Path tempDirectory = ExcelTemplateEngine.this.tempDirectory;

                Exception exception = rethrowReference.get();

                //判断是否成功
                if (tempFileState.get() && tempDirectory != null && Files.exists(tempDirectory)) break;

                //异常不为空 则表示创建出错
                if (exception != null) {

                    if (delayReset.compareAndSet(false, true) && tempFileState.get()) {

                        TimeUnit.MILLISECONDS.sleep(5);

                        //将文件初始化状态再次设置为能用
                        tempFileState.compareAndSet(true, false);

                        delayReset.set(false);
                    }

                    log.error("Execute write temp file failed,The error cause is {}", rethrowReference.get());

                    throw new BussinessException("交换文件创建失败");
                }
            }
        }
    }

    @SneakyThrows
    public String renderExcelTemplate(Map<String, Object> modelMap, String templateFileName,
                                      String fileName, NativeWebRequest webRequest) {

        if (log.isDebugEnabled()) {
            log.info("Generate template excel base dir is {}", DEFAULT_EXCEL_EXPORT_FILE_PREFIX);
        }

        CloseChain closeChain = CloseChain.create();

        int fileRandom = localRandom.nextInt(100000);

        //获取需要生成的文件名称
        Path tempFile = getTempDirectory().resolve(String.format("%s.%s.xls", fileName, fileRandom));

        try {

            //初始化临时目录
            initTemplateDir();

            //绑定数据
            SimpleHash simpleHash = buildTemplateModel(modelMap);

            //获取当前的语言格式
            Locale locale = RequestContextUtils.getLocale(webRequest.getNativeRequest(HttpServletRequest.class));

            //创建临时文件
            FileChannel fileChannel = FileChannel.open(tempFile, StandardOpenOption.CREATE);
            Writer writer = Channels.newWriter(fileChannel, Charset.forName(encoding).newEncoder(), -1);

            //注册关闭接口
            closeChain.register(fileChannel).register(writer);

            //填充数据
            processTemplate(getTemplate(locale, templateFileName), simpleHash, writer);

            log.info("Fill template {} excel data is success, Generate excel fileName is:{}", templateFileName, tempFile);

        } catch (Exception e) {

            log.error("Generate freemarker excel template is fail, The error is {}", e);
            throw new BussinessException("资金头寸数据错误");

        } finally {

            closeChain.closer().close();

        }

        return tempFile.toString();
    }


    private SimpleHash buildTemplateModel(Map<String, Object> model) {
        return new SimpleHash(model, getObjectWrapper());
    }


    private Template getTemplate(Locale locale, String templateName) throws IOException {
        return getTemplate(templateName, locale);
    }


    private Template getTemplate(String name, Locale locale) throws IOException {
        return (getEncoding() != null ?
                configuration.getTemplate(name, locale, getEncoding()) :
                configuration.getTemplate(name, locale));
    }


    private void processTemplate(Template template, SimpleHash model, Writer writer) throws IOException, TemplateException {
        template.process(model, writer);
    }


    private ObjectWrapper getObjectWrapper() {
        ObjectWrapper ow = configurer.getConfiguration().getObjectWrapper();
        return (ow != null ? ow :
                new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
    }

    @SuppressWarnings("all")
    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Accessors(fluent = true)
    private static class CloseChain {

        @Getter
        Closer closer;

        public static CloseChain create() {
            return new CloseChain(Closer.create());
        }

        public CloseChain register(Closeable closeable) {
            this.closer.register(closeable);
            return this;
        }
    }
}
*/
