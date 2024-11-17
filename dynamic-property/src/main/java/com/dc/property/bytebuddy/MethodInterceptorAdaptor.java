package com.dc.property.bytebuddy;

import lombok.AllArgsConstructor;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;

/**
 * wrapped for {@link MethodInstanceInterceptor}
 *
 * @author zy
 */
@AllArgsConstructor
//@SuppressWarnings({"rawtypes", "unchecked"})
public class MethodInterceptorAdaptor {

    final MethodInstanceInterceptor methodInterceptor;

    @RuntimeType
    public Object intercept(@Morph OverrideCallable callable, @This Object obj, @AllArguments Object[] allArguments,// 注入目标方法的全部参数
                            @Origin Method method) throws Exception {

        DynamicInstance dynamicInstance = (DynamicInstance) obj;
        return methodInterceptor.intercept(dynamicInstance, callable, allArguments, method);
    }

}