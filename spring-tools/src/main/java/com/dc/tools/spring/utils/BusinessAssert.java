package com.dc.tools.spring.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dc.tools.common.utils.CollectionUtils;
import com.dc.tools.common.utils.StringUtils;
import com.dc.tools.spring.exception.BusinessException;
import com.dc.tools.spring.exception.ParamException;
import com.google.common.base.Throwables;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 用于assert 断言
 *
 * @author zhangyang
 */
public abstract class BusinessAssert {

    /**
     * 校验字符串是否为空，如果为空则抛出 {@link BusinessException}
     *
     * @param text    需要校验的字符串
     * @param message 抛出异常时提示的错误信息
     */
    public static void hasText(String text, String message) {
        if (!com.dc.tools.common.utils.StringUtils.hasText(text)) {
            throw new BusinessException(message);
        }
    }


    /**
     * 校验 condition 是否为true {@link BusinessException}
     *
     * @param condition 校验条件
     * @param message   抛出异常时提示的错误信息
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验 condition 是否为true {@link BusinessException}
     *
     * @param condition 校验条件
     * @param message   抛出异常时提示的错误信息
     */
    public static void paramIsTrue(boolean condition, String message) {
        if (!condition) {
            throw new ParamException(message);
        }
    }

    /**
     * 校验 condition 是否为true {@link BusinessException}
     *
     * @param condition 校验条件
     * @param message   抛出异常时提示的错误信息
     */
    public static void isTrue(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new ParamException(StrUtil.format(message, args));
        }
    }


    /**
     * 校验 condition 是否为true {@link BusinessException}
     *
     * @param condition 校验条件
     * @param message   抛出异常时提示的错误信息
     */
    public static void validateField(boolean condition, String message) {
        if (!condition) {
            throw new ParamException(message);
        }
    }

    /**
     * 校验param 是否有效，如果无效则抛出 {@link ParamException}
     *
     * @param text    需要校验的参数
     * @param message 抛出异常时提示的错误信息
     */
    public static void validParam(String text, String message) {
        if (!com.dc.tools.common.utils.StringUtils.hasText(text)) {
            throw new ParamException(message);
        }
    }

    /**
     * 校验param 是否有效，如果无效则抛出 {@link ParamException}
     *
     * @param text    需要校验的参数
     * @param message 抛出异常时提示的错误信息
     */
    public static void validParam(String text, String message, Object... params) {
        if (StringUtils.isEmpty(text)) {
            message = StrUtil.format(message, params);
            throw new ParamException(message);
        }
    }


    /**
     * 校验instance 是否为null，如果为null则抛出 {@link ParamException}
     *
     * @param instance 需要校验的对象实例
     * @param message  抛出异常时提示的错误信息
     */
    public static void notNull(Object instance, String message) {
        if (Objects.isNull(instance)) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验instance 是否为null，如果为null则抛出 {@link ParamException}
     *
     * @param instance 需要校验的对象实例
     * @param message  抛出异常时提示的错误信息
     */
    public static void paramNotNull(Object instance, String message, Object...args) {
        if (Objects.isNull(instance)) {
            throw new ParamException(StrUtil.format(message, args));
        }
    }

    /**
     * 校验instance 是否为null，如果为null则抛出 {@link ParamException}
     *
     * @param instance 需要校验的对象实例
     * @param template 抛出异常时提示的错误信息模板
     * @param params   模板中对应的参数
     */
    public static void notNull(Object instance, String template, Object... params) {
        if (Objects.isNull(instance)) {
            throw new BusinessException(StrUtil.format(template, params));
        }
    }

    /**
     * 校验collection 是否为null，如果为null则抛出 {@link BusinessException}
     *
     * @param collection 需要校验的集合
     * @param template   抛出异常时提示的错误信息模板
     * @param params     模板中对应的参数
     */
    public static void notEmpty(Collection<?> collection, String template, Object... params) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BusinessException(StrUtil.format(template, params));
        }
    }

    /**
     * 校验array 是否为null，如果为null则抛出 {@link BusinessException}
     *
     * @param array    需要校验的数组
     * @param template 抛出异常时提示的错误信息模板
     * @param params   模板中对应的参数
     */
    public static void notEmpty(Object[] array, String template, Object... params) {
        if (ObjectUtil.isEmpty(array)) {
            throw new BusinessException(StrUtil.format(template, params));
        }
    }


    public static BusinessException wrapBusiness(Exception cause, String message) {
        return new BusinessException(message, cause);
    }


    public static BusinessException wrapBusiness(Exception cause, String message, Object... params) {
        return new BusinessException(StrUtil.format(message, params), cause);
    }


    public static BusinessException propagateBusiness(Exception cause, String message) {

        if (cause instanceof BusinessException)
            throw (BusinessException) cause;

        throw new BusinessException(message, cause);
    }


    public static void propagateException(Exception cause, Class<? extends Exception> exClass) {
        try {
            Throwables.propagateIfInstanceOf(cause, exClass);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }

            throw new BusinessException(e.getMessage(), e);
        }
    }


    @SuppressWarnings("all")
    public static void propagateIfPossible(Exception cause, Class<? extends Exception> exClass, String message) {
        List<Throwable> causalChain = Throwables.getCausalChain(cause);

        boolean find = false;
        Throwable findCause = null;

        for (Throwable throwable : causalChain) {
            if (exClass.isAssignableFrom(throwable.getClass())) {
                find = true;
                findCause = throwable;
                break;
            }
        }

        if (find) {
            BusinessException exception = exClass.isAssignableFrom(BusinessException.class)
                    ? (BusinessException) findCause : new BusinessException(message, cause);
            throw exception;
        }

    }

    /**
     * 校验参数是否为null
     *
     * @param param   需要被校验的参数
     * @param message 抛出的错误信息
     * @param params  当message为template时，需要通过params进行转换
     */
    public static void validParamNotNull(Object param, String message, Object... params) {
        if (Objects.isNull(param)) {
            String errorMessage = StrUtil.format(message, params);
            throw new ParamException(errorMessage);
        }

    }


}
