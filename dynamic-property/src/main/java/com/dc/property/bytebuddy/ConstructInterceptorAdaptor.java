package com.dc.property.bytebuddy;

import lombok.AllArgsConstructor;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

@AllArgsConstructor
public class ConstructInterceptorAdaptor {

    final ConstructInstanceInterceptor constructInterceptor;

    @RuntimeType
    public void intercept(@This Object obj, @AllArguments Object[] allArgs) {
        constructInterceptor.onConstruct(obj, allArgs);
    }

}