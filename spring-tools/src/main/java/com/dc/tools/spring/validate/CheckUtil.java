package com.dc.tools.spring.validate;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 用于辅助校验 param
 * <p>
 * com.turing.pt.common.validate.CheckUtil
 *
 * @author zhangyang
 */
public class CheckUtil {

    public static final CheckUtil INSTANCE = new CheckUtil();

    /**
     * 判断文本是否有值
     */
    public boolean hasText(String message) {
        return StringUtils.hasText(message);
    }

    /**
     * 判断集合是否为空
     */
    public boolean isEmpty(List<?> collection) {
        return CollectionUtils.isEmpty(collection);
    }

    /**
     * 判断集合不为空
     */
    public boolean notEmpty(List<?> collection) {
        return !CollectionUtils.isEmpty(collection);
    }

    /**
     * 判断字符串是否为空
     */
    public boolean isEmpty(String message) {
        return StringUtils.isEmpty(message);
    }

    /**
     * 判断 instance 是否为null
     */
    public boolean notNull(Object instance) {
        return Objects.nonNull(instance);
    }

    /**
     * 判断 text 是否为 number
     */
    public boolean isNumber(String text) {
        return NumberUtil.isNumber(text);
    }

    /**
     * 判断 text 是否为 number 11
     */
    public boolean hasLength(String text, int length) {
        if (StringUtils.isEmpty(text))
            return false;

        return text.length() >= length;
    }

    /**
     * 判断 text 是否为 number 11
     */
    public boolean lessThan(String text, int length) {
        if (StringUtils.isEmpty(text))
            return false;

        return text.length() <= length;
    }

    /**
     * 判断字符串是否匹配当前时间格式
     *
     * @param timeStr   时间字符串
     * @param timeStyle 时间格式
     */
    public boolean matchTimeStyle(String timeStr, String timeStyle) {
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timeStyle);
            dateTimeFormatter.parse(timeStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断 text 是否为 number 11
     */
    public boolean numberIn(Integer number, Integer... value) {
        return Arrays.asList(value).contains(number);
    }

    /**
     * 两个必须有一个不为空
     */
    public boolean neitherEmpty(Object valueA, Object valueB) {
        return !ObjectUtil.isEmpty(valueA) || !ObjectUtil.isEmpty(valueB);
    }


    /**
     * 判断instance是否为空
     *
     * @param instance 传入的数据
     */
    public boolean isNull(Object instance) {
        return Objects.isNull(instance);
    }


}
