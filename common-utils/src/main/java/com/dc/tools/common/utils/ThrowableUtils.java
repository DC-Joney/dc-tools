package com.dc.tools.common.utils;

import com.google.common.base.Throwables;
import lombok.experimental.UtilityClass;

import java.util.Optional;

/**
 * 用于扩展{@link Throwables} 工具类
 *
 * @author dcjoney
 * @see Throwables
 */
@UtilityClass
public class ThrowableUtils {

    public Throwable rootCause(Throwable throwable) {
        return Throwables.getRootCause(throwable);
    }

    /**
     * 获取异常链中的某一个异常
     *
     * @param throwable    throwable
     * @param exceptedType 异常链中的异常类型
     */
    @SuppressWarnings("unchecked")
    public <T extends Throwable> Optional<T> getCauseForType(Throwable throwable, Class<T> exceptedType) {
        Throwable excepted = throwable;
        while (excepted != null) {
            if (exceptedType.isInstance(excepted)) {
                return Optional.of((T) excepted);
            }
            excepted = excepted.getCause();
        }
        return Optional.empty();
    }


    /**
     * 获取异常链中的某一个异常
     *
     * @param throwable    throwable
     * @param exceptedType 异常链中的异常类型
     */
    @SuppressWarnings("all")
    public <T extends Throwable> Optional<T> castType(Throwable throwable, Class<T> exceptedType) {
        if (throwable == null || exceptedType == null || !exceptedType.isInstance(throwable))
            return Optional.empty();

        T exception = exceptedType.cast(throwable);
        return Optional.of(exception);
    }


}
