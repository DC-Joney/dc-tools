package com.dc.tools.common.date;

import com.dc.tools.common.utils.ClassUtils;

import java.time.*;
import java.util.*;

import static java.time.Instant.ofEpochMilli;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;

/**
 * Helper class to register JSR-310 specific {@link Converter} implementations in case the we're running on Java 8.
 *
 * @author Oliver Gierke
 * @author Barak Schoster
 */
public class Jsr310Converters {

    private static final boolean JAVA_8_IS_PRESENT = ClassUtils.isPresent("java.time.LocalDateTime",
            Jsr310Converters.class.getClassLoader());

    /**
     * Returns the converters to be registered. Will only return converters in case we're running on Java 8.
     *
     * @return
     */
    public static Collection<Converter<?, ?>> getConvertersToRegister() {

        if (!JAVA_8_IS_PRESENT) {
            return Collections.emptySet();
        }

        List<Converter<?, ?>> converters = new ArrayList<Converter<?, ?>>();
        converters.add(DateToLocalDateTimeConverter.INSTANCE);
        converters.add(LocalDateTimeToDateConverter.INSTANCE);
        converters.add(DateToLocalDateConverter.INSTANCE);
        converters.add(LocalDateToDateConverter.INSTANCE);
        converters.add(DateToLocalTimeConverter.INSTANCE);
        converters.add(LocalTimeToDateConverter.INSTANCE);
        converters.add(DateToInstantConverter.INSTANCE);
        converters.add(InstantToDateConverter.INSTANCE);
        converters.add(ZoneIdToStringConverter.INSTANCE);
        converters.add(StringToZoneIdConverter.INSTANCE);
        converters.add(DurationToStringConverter.INSTANCE);
        converters.add(StringToDurationConverter.INSTANCE);
        converters.add(PeriodToStringConverter.INSTANCE);
        converters.add(StringToPeriodConverter.INSTANCE);

        return converters;
    }

    public static boolean supports(Class<?> type) {

        if (!JAVA_8_IS_PRESENT) {
            return false;
        }

        return Arrays.<Class<?>> asList(LocalDateTime.class, LocalDate.class, LocalTime.class, Instant.class)
                .contains(type);
    }

    public static enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

        INSTANCE;

        @Override
        public LocalDateTime convert(Date source) {
            return source == null ? null : ofInstant(source.toInstant(), systemDefault());
        }
    }

    public static enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

        INSTANCE;

        @Override
        public Date convert(LocalDateTime source) {
            return source == null ? null : Date.from(source.atZone(systemDefault()).toInstant());
        }
    }

    public static enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

        INSTANCE;

        @Override
        public LocalDate convert(Date source) {
            return source == null ? null : ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalDate();
        }
    }

    public static enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

        INSTANCE;

        @Override
        public Date convert(LocalDate source) {
            return source == null ? null : Date.from(source.atStartOfDay(systemDefault()).toInstant());
        }
    }

    public static enum DateToLocalTimeConverter implements Converter<Date, LocalTime> {

        INSTANCE;

        @Override
        public LocalTime convert(Date source) {
            return source == null ? null : ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalTime();
        }
    }

    public static enum LocalTimeToDateConverter implements Converter<LocalTime, Date> {

        INSTANCE;

        @Override
        public Date convert(LocalTime source) {
            return source == null ? null : Date.from(source.atDate(LocalDate.now()).atZone(systemDefault()).toInstant());
        }
    }

    public static enum DateToInstantConverter implements Converter<Date, Instant> {

        INSTANCE;

        @Override
        public Instant convert(Date source) {
            return source == null ? null : source.toInstant();
        }
    }

    public static enum InstantToDateConverter implements Converter<Instant, Date> {

        INSTANCE;

        @Override
        public Date convert(Instant source) {
            return source == null ? null : Date.from(source.atZone(systemDefault()).toInstant());
        }
    }

    public static enum ZoneIdToStringConverter implements Converter<ZoneId, String> {

        INSTANCE;

        @Override
        public String convert(ZoneId source) {
            return source.toString();
        }
    }


    public static enum StringToZoneIdConverter implements Converter<String, ZoneId> {

        INSTANCE;

        @Override
        public ZoneId convert(String source) {
            return ZoneId.of(source);
        }
    }

    public static enum DurationToStringConverter implements Converter<Duration, String> {

        INSTANCE;

        @Override
        public String convert(Duration duration) {
            return duration.toString();
        }
    }

    public static enum StringToDurationConverter implements Converter<String, Duration> {

        INSTANCE;

        @Override
        public Duration convert(String s) {
            return Duration.parse(s);
        }
    }

    public static enum PeriodToStringConverter implements Converter<Period, String> {

        INSTANCE;

        @Override
        public String convert(Period period) {
            return period.toString();
        }
    }

    public static enum StringToPeriodConverter implements Converter<String, Period> {

        INSTANCE;

        @Override
        public Period convert(String s) {
            return Period.parse(s);
        }
    }
}
