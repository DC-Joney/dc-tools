package com.dc.tools.common.date;

import com.dc.tools.common.utils.Assert;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.*;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.*;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import static java.time.Instant.ofEpochMilli;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.*;

/**
 * <p>
 * java time 日期工具类
 *
 * @author zhangyang
 * @date 2020-06-23
 * @see ChronoField 单位字段值
 * @see ChronoUnit 时间单位
 * @see Temporal 新版日期时间父级接口
 * @see TemporalAdjuster 日期时间修改
 * @see TemporalQuery 日期类型转换
 * @see IsoFields  {@link TemporalField }的扩展信息，比如 季度字段等等
 * @see java.time.chrono.Chronology  用于表示日历系统，包括日本日历、民国日历、泰国日历 等等
 * @see IsoChronology 标准的日历实现类
 * @see java.time.chrono.IsoEra 用于表示公元前后
 * @see ValueRange 用于表示时间单位范围，比如 月份 1-12
 * @see TemporalAmount 在某个时间的基础上做加减操作，比如加一个月，减5分钟等等，{@link java.time.chrono.ChronoPeriod} 是对年月日 时间加减操作提供的接口
 * @see WeekFields 提供对 {年月日 <--  --> 星期} 的支持
 */
@UtilityClass
public class TemporalUtils {


    public static final ZoneOffset SHANG_HAI_OFFSET;

    static {
        SHANG_HAI_OFFSET = ZoneOffset.ofHours(8);
    }


    /**
     * 格式化 {@link YearMonth} 日期
     *
     * @see DateTimeFormatter
     * @see YearMonth#parse(CharSequence, DateTimeFormatter)
     * @see YearMonth#format(DateTimeFormatter)
     */
    public static final DateTimeFormatter ISO_YEAR_MONTH;

    static {
        ISO_YEAR_MONTH = new DateTimeFormatterBuilder()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(MONTH_OF_YEAR, 2)
                .parseDefaulting(DAY_OF_MONTH, 1)
                .toFormatter();
    }


    /**
     * 转换成数仓汇率文件的日期格式
     */
    public static final DateTimeFormatter FTP_DATE_PATH;

    static {
        FTP_DATE_PATH = new DateTimeFormatterBuilder()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("")
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral("")
                .appendValue(DAY_OF_MONTH, 2)
                .toFormatter();
    }


    /**
     * 格式化季度时间
     *
     * @see IsoFields#QUARTER_OF_YEAR
     */
    public static final DateTimeFormatter QUARTER_DATE_PATH;

    static {
        QUARTER_DATE_PATH = new DateTimeFormatterBuilder()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("年")
                .appendValue(IsoFields.QUARTER_OF_YEAR, 1)
                .appendLiteral("季度")
                .toFormatter();
    }


    /**
     * 格式化报表导出时间
     *
     * @see DateTimeFormatterBuilder
     */
    public static final DateTimeFormatter REPORT_FORMS_DATE_PATH;

    static {
        REPORT_FORMS_DATE_PATH = new DateTimeFormatterBuilder()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("年")
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral("月")
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral("日")
                .parseDefaulting(DAY_OF_MONTH, 1)
                .toFormatter();
    }


    /**
     * 格式化汇率时间
     *
     * @see IsoFields#QUARTER_OF_YEAR
     */
    public static final DateTimeFormatter RATE_DATE_PATH;

    static {
        RATE_DATE_PATH = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("年")
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral("月末")
                .optionalStart()
                .parseDefaulting(MONTH_OF_YEAR, 1)
                .toFormatter(Locale.getDefault());
    }


    /**
     * 格式化汇率时间
     *
     * @see IsoFields#QUARTER_OF_YEAR
     */
    public static final DateTimeFormatter RATE_DATE_PATH_STR;

    static {
        RATE_DATE_PATH_STR = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("年")
                .appendValue(MONTH_OF_YEAR, 2)
                .parseDefaulting(MONTH_OF_YEAR, 1)
                .appendLiteral("月末汇率")
                .toFormatter(Locale.getDefault());
    }


    /**
     * 格式化文件时间
     */
    public static final DateTimeFormatter FILE_DATE_PATH_STR;

    static {
        FILE_DATE_PATH_STR = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("")
                .appendValue(MONTH_OF_YEAR, 2)
                .parseDefaulting(MONTH_OF_YEAR, 1)
                .appendLiteral("")
                .appendValue(DAY_OF_MONTH, 2)
                .parseDefaulting(DAY_OF_MONTH, 1)
                .toFormatter(Locale.getDefault());
    }


    /**
     * 通用的yyyy-MM-dd HH:mm:ss
     */
    public static final DateTimeFormatter COMMON_LOCAL_DATE_TIME;

    static {
        COMMON_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .toFormatter(Locale.getDefault());
    }

    /**
     * 好未来接口时间格式
     */
    public static final DateTimeFormatter TAL_LOCAL_DATE_TIME;

    static {
        TAL_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(ISO_LOCAL_DATE)
                .appendLiteral("T")
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .toFormatter(Locale.getDefault());
    }


    /**
     * 格式化文件时间
     */
    public static final DateTimeFormatter FILE_DATE_TIME_PATH_STR = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//    ;
//
//    static {
//        FILE_DATE_TIME_PATH_STR = new DateTimeFormatterBuilder()
//                .parseCaseInsensitive()
//                .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
//                .appendLiteral("")
//                .appendValue(MONTH_OF_YEAR, 2)
//                .parseDefaulting(MONTH_OF_YEAR, 1)
//                .appendLiteral("")
//                .appendValue(DAY_OF_MONTH, 2)
//                .parseDefaulting(DAY_OF_MONTH, 1)
//                .appendLiteral("")
//                .appendValue(HOUR_OF_DAY,2)
//                .toFormatter(Locale.getDefault());
//    }


    /**
     * 获取上一个月的最后一天
     *
     * @see TemporalAdjuster
     */
    public static final TemporalAdjuster PRE_MONTH_LAST_DAY = temporal -> {
        Temporal preTemporal = temporal.minus(1, ChronoUnit.MONTHS);
        return preTemporal.with(DAY_OF_MONTH, preTemporal.range(DAY_OF_MONTH).getMaximum());
    };


    /**
     * 获取上一个月的第一天
     *
     * @see TemporalAdjuster
     */
    public static final TemporalAdjuster PRE_MONTH_FIRST_DAY = temporal -> {
        Temporal preTemporal = temporal.minus(1, ChronoUnit.MONTHS);
        return preTemporal.with(DAY_OF_MONTH, preTemporal.range(DAY_OF_MONTH).getMinimum());
    };


    /**
     * 获取当前星期的星期一
     *
     * @see TemporalAdjuster
     * @see DayOfWeek
     * @see WeekFields
     */
    public static final TemporalAdjuster MONDAY_OF_WEEK = temporal -> temporal.with(DAY_OF_WEEK, DayOfWeek.MONDAY.getValue());


    /**
     * 获取上个星期的星期一
     *
     * @see TemporalAdjuster
     * @see DayOfWeek
     * @see WeekFields
     */
    public static final TemporalAdjuster PRE_MONDAY_OF_WEEK = temporal -> temporal.with(DAY_OF_WEEK, DayOfWeek.MONDAY.getValue())
            .minus(1, ChronoUnit.WEEKS);


    /**
     * 获取上一个季度第一天
     *
     * @see IsoFields#DAY_OF_QUARTER
     * @see IsoFields#QUARTER_YEARS
     * @see TemporalField
     */
    public static final TemporalAdjuster preQuarterOfFirstDay = temporal ->
            temporal.with(IsoFields.DAY_OF_QUARTER, temporal.range(IsoFields.DAY_OF_QUARTER).getMinimum())
                    .minus(1, IsoFields.QUARTER_YEARS);


    /**
     * 获取上一个季度最后一天
     *
     * @see IsoFields#DAY_OF_QUARTER
     * @see IsoFields#QUARTER_YEARS
     * @see TemporalField
     */
    public static final TemporalAdjuster preQuarterOfLastDay = temporal ->
            temporal.with(IsoFields.DAY_OF_QUARTER, temporal.range(IsoFields.DAY_OF_QUARTER).getMaximum())
                    .minus(1, IsoFields.QUARTER_YEARS);


    /**
     * 获取当前季度最后一天
     *
     * @see IsoFields#DAY_OF_QUARTER
     */
    public static final TemporalAdjuster lastDayOfQuarter = temporal ->
            temporal.with(IsoFields.DAY_OF_QUARTER, temporal.range(IsoFields.DAY_OF_QUARTER).getMaximum());


    /**
     * 获取当前季度第一天
     *
     * @see IsoFields#DAY_OF_QUARTER
     */
    public static final TemporalAdjuster firstDayOfQuarter = temporal ->
            temporal.with(IsoFields.DAY_OF_QUARTER, temporal.range(IsoFields.DAY_OF_QUARTER).getMinimum());

    /**
     * 某一个季度第一天
     *
     * @see java.time.chrono.Chronology
     * @see IsoFields#QUARTER_OF_YEAR
     */
    public static final Function<Integer, LocalDate> quarterFirstDayFunction = quarterNum -> {
        return IsoChronology.INSTANCE
                .dateNow()
                .with(IsoFields.QUARTER_OF_YEAR, quarterNum)
                .with(firstDayOfQuarter);
    };


    /**
     * 某一个季度最后一天
     *
     * @see java.time.chrono.Chronology
     * @see IsoFields#QUARTER_OF_YEAR
     */
    public static final Function<Integer, LocalDate> quarterLastDayFunction = quarterNum -> {
        return IsoChronology.INSTANCE
                .dateNow()
                .with(IsoFields.QUARTER_OF_YEAR, quarterNum)
                .with(lastDayOfQuarter);
    };


    /**
     * 将 {@link LocalDate} 转换为 {@link YearMonth}
     *
     * @see TemporalQuery
     */
    public static final TemporalQuery<YearMonth> YEAR_MONTH_QUERY = temporal -> {
        if (temporal.isSupported(ChronoField.MONTH_OF_YEAR) && temporal.isSupported(ChronoField.YEAR)) {
            return YearMonth.from(temporal);
        }
        return null;
    };


    /**
     * 将 {@link YearMonth} 转换为 {@link LocalDate}, 并将天数设置为当月第一天
     *
     * @see TemporalQuery
     */
    public static final TemporalQuery<LocalDate> TO_LOCAL_DATE_OF_FIRST_DAY_QUERY = temporal -> {
        if (temporal.isSupported(ChronoField.MONTH_OF_YEAR) && temporal.isSupported(ChronoField.YEAR)) {
            return LocalDate.of(temporal.get(YEAR), temporal.get(MONTH_OF_YEAR), (int) DAY_OF_MONTH.range().getMinimum());
        }
        return null;
    };


    /**
     * 将 {@link YearMonth} 转换为 {@link LocalDate}，并将天数设置为当月最后一天
     *
     * @see TemporalQuery
     */
    public static final TemporalQuery<LocalDate> TO_LOCAL_DATE_OF_LAST_DAY_QUERY = temporal -> {
        if (temporal.isSupported(ChronoField.MONTH_OF_YEAR) && temporal.isSupported(ChronoField.YEAR)) {
            return YearMonth.from(temporal)
                    .atDay(1)
                    .with(TemporalAdjusters.lastDayOfMonth());
        }
        return null;
    };


    /**
     * 将 {@link YearMonth} 转换为 {@link LocalDate}，并将天数设置为上个月最后一天
     *
     * @see TemporalQuery
     */
    public static final TemporalQuery<LocalDate> TO_LOCAL_DATE_PRE_LAST_DAY_QUERY = temporal -> {
        if (temporal.isSupported(ChronoField.MONTH_OF_YEAR) && temporal.isSupported(ChronoField.YEAR)) {
            return YearMonth.from(temporal).atDay(1)
                    .with(PRE_MONTH_LAST_DAY);
        }
        return null;
    };


    /**
     * 将 {@link LocalDate} 转换为 {@link Date}，并将天数设置为上个月最后一天
     *
     * @see TemporalQuery
     */
    public static final TemporalQuery<Date> toDateQuery = temporal -> {
        Objects.requireNonNull(temporal, "The source time must not be null");
        if (temporal.isSupported(EPOCH_DAY)) {
            Instant instantTime = LocalDate.from(temporal).atStartOfDay(systemDefault()).toInstant();
            return Date.from(instantTime);

        }
        return null;
    };


    /**
     * 将 {@link Date} 转为 {@link LocalDate}
     */
    public enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

        INSTANCE;

        @Override
        public LocalDate convert(Date source) {
            return source == null ? null : ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalDate();
        }
    }

    /**
     * 将 {@link LocalDate} 转为 {@link Date}
     */
    public enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

        INSTANCE;

        @Override
        public Date convert(LocalDate source) {
            return source == null ? null : Date.from(source.atStartOfDay(systemDefault()).toInstant());
        }
    }


    /**
     * 将 {@link LocalDateTime} 转为 {@link Date}
     */
    public enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

        INSTANCE;

        @Override
        public Date convert(LocalDateTime source) {
            return source == null ? null : Date.from(source.atZone(systemDefault()).toInstant());
        }
    }


    /**
     * 将 {@link Date} 转为 {@link YearMonth}
     */
    public enum DateToYearMonthConverter implements Converter<Date, YearMonth> {

        INSTANCE;

        @Override
        public YearMonth convert(Date source) {
            return source == null ? null : DateToLocalDateConverter.INSTANCE.convert(source)
                    .query(YEAR_MONTH_QUERY);
        }
    }

    /**
     * 将 {@link YearMonth} 转为 {@link Date}, 转换为Date 以后，默认为当月第一天
     */
    public enum YearMonthToDateConverter implements Converter<YearMonth, Date> {

        INSTANCE;

        @Override
        public Date convert(YearMonth source) {
            return source == null ? null : LocalDateToDateConverter.INSTANCE.convert(source.query(TO_LOCAL_DATE_OF_FIRST_DAY_QUERY));
        }
    }

    /**
     * 将 {@link Date} 转换为 {@link LocalDate}
     */
    public static LocalDate convertToLocalDate(@NonNull Date source) {
        return DateToLocalDateConverter.INSTANCE.convert(source);
    }

    /**
     * 将 {@link Date} 转换为 {@link LocalDateTime}
     */
    public static LocalDateTime convertToLocalDateTime(@NonNull Date source) {
        return Jsr310Converters.DateToLocalDateTimeConverter.INSTANCE.convert(source);
    }


    /**
     * 将 {@link LocalDate} 转换为 {@link Date}
     */
    public static Date convertToDate(@NonNull LocalDate source) {
        return LocalDateToDateConverter.INSTANCE.convert(source);
    }

    /**
     * 将 {@link LocalDateTime} 转换为 {@link Date}
     */
    public static Date convertToDate(@NonNull LocalDateTime source) {
        return LocalDateTimeToDateConverter.INSTANCE.convert(source);
    }


    /**
     * 获取某一个季度的第一天
     *
     * @param quarter 4季度中的某个季度 {@example 1 2 3 4}
     */
    public static LocalDate ofQuarterFirstDay(int quarter) {
        Assert.isTrue(quarter >= 0, "The quarter of year must be >= 0");
        return quarterFirstDayFunction.apply(quarter);
    }


    /**
     * 获取某一个季度的最后一天
     *
     * @param quarter 4季度中的某个季度 {@example 1 2 3 4}
     */
    public static LocalDate ofQuarterLastDay(int quarter) {
        Assert.isTrue(quarter >= 0, "The quarter of year must be >= 0");
        return quarterLastDayFunction.apply(quarter);
    }


    /**
     * @param dateStr         要进行转换的时间字符串
     * @param sourceFormatter 使用 {@code sourceFormatter} 对字符串时间进行转换
     * @return 将日期转为 年-月-日类型
     */
    public static String convertStyle(String dateStr, DateTimeFormatter sourceFormatter) {
        Assert.hasText(dateStr, "The dateStr must be have length or not be null");
        return REPORT_FORMS_DATE_PATH.format(sourceFormatter.parse(dateStr));
    }


    /**
     * @param date            要进行转换的时间字符串
     * @param sourceFormatter 使用 {@code sourceFormatter} 对字符串时间进行转换
     * @return 将日期转为 年-月-日类型
     */
    public static String formatStyle(Date date, DateTimeFormatter sourceFormatter) {
        Assert.notNull(date, "The date must  not be null");
        return convertToLocalDate(date).format(sourceFormatter);
    }


    /**
     * @param dateStr 要进行转换的时间字符串
     * @return 将字符串日期 转为 通用的 date类型
     */
    public static Date parseDate(String dateStr) {
        Assert.hasText(dateStr, "The date must  not be null");
        return convertToDate(parseLocalDate(dateStr, ISO_LOCAL_DATE));
    }


    /**
     * @param dateStr         要进行转换的时间字符串
     * @param sourceFormatter 使用 {@code sourceFormatter} 对字符串时间进行转换
     * @return 将字符串日期 转为 通用的 date类型
     */
    public static Date parseDate(String dateStr, DateTimeFormatter sourceFormatter) {
        Assert.hasText(dateStr, "The date must  not be null");
        return convertToDate(parseLocalDate(dateStr, sourceFormatter));
    }


    /**
     * @param dateStr         要进行转换的时间字符串
     * @param sourceFormatter 使用 {@code sourceFormatter} 对字符串时间进行转换
     * @return 将字符串日期 转为 {@link LocalDate} 类型
     */
    public static LocalDate parseLocalDate(String dateStr, DateTimeFormatter sourceFormatter) {
        Assert.hasText(dateStr, "The date must  not be null");
        return LocalDate.parse(dateStr, sourceFormatter);
    }


    /**
     * @param localDateTime 需要转换的时间
     * @return 获取localDateTime对应的epoch second
     */
    public static long getEpochSecond(LocalDateTime localDateTime) {
        Assert.notNull(localDateTime, "The date must  not be null");
        return localDateTime.toInstant(ZoneOffset.of("+8")).getEpochSecond();
    }


    /**
     * @param localDateTime 需要转换的时间
     * @return 获取localDateTime对应的epoch second
     */
    public static long getEpochMilliSecond(LocalDateTime localDateTime) {
        Assert.notNull(localDateTime, "The date must  not be null");
        return localDateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }

    /**
     * 返回timestamp 对应的时间
     *
     * @param timestamp 毫秒值
     * @apiNote if timestamp is null or timestamp less than 0 or equal to 0 then return {@link  LocalDateTime#MIN}
     */
    public static LocalDateTime getLocalDateTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0)
            return LocalDateTime.MIN;


        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }


}
