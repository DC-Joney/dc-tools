package com.dc.property.bytebuddy.advice;

import com.dc.property.bytebuddy.DynamicInstance;
import com.dc.property.bytebuddy.OverrideCallable;
import com.dc.tools.common.annotaion.NonNull;
import lombok.AllArgsConstructor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.utility.JavaModule;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.List;

import static net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;

public class InterceptorTransformer implements AgentBuilder.Transformer {

    @Override
    @NonNull
    public DynamicType.Builder<?> transform(@NonNull DynamicType.Builder<?> builder, @NonNull TypeDescription typeDescription,
                                            ClassLoader classLoader, JavaModule module, @NonNull ProtectionDomain protectionDomain) {


        builder = builder.defineField("context$Interceptor", Object.class, ACC_PRIVATE)
                .implement(DynamicInstance.class)
                .intercept(FieldAccessor.ofBeanProperty());

        if (InterceptorFinder.contains(typeDescription)) {
            List<ClassAdvicePoint<?>> classAdvicePoints = InterceptorFinder.advicePoints(typeDescription);
            //处理advice
            for (ClassAdvicePoint<?> classAdvicePoint : classAdvicePoints) {
                builder = builder.visit(Advice.to(classAdvicePoint.getAdviceClass()).on(classAdvicePoint.getMatcher()));
            }

            List<InterceptorPoint> interceptorPoints = InterceptorFinder.interceptorPoints(typeDescription);
            //处理拦截器
            for (InterceptorPoint interceptorPoint : interceptorPoints) {
                Interceptor interceptor = interceptorPoint.getInterceptor();

                //处理构造器
                if (interceptor instanceof ConstructInterceptor) {
                    builder = builder.constructor(interceptorPoint.getMatcher())
                            .intercept(SuperMethodCall.INSTANCE
                                    .andThen(MethodDelegation.to(new ConstructInter((ConstructInterceptor) interceptor))));
                }

                //处理方法
                if (interceptor instanceof MethodInterceptor) {
                    builder = builder.method(interceptorPoint.getMatcher())
                            .intercept(MethodDelegation.withDefaultConfiguration()
                                    .withBinders(Morph.Binder.install(OverrideCallable.class))
                                    .to(new MethodInter((MethodInterceptor) interceptor)));
                }
            }
        }

        return builder;
    }

    @AllArgsConstructor
    public static class ConstructInter {

        private final ConstructInterceptor interceptor;

        @RuntimeType
        public void intercept(@This Object object, @AllArguments Object[] allArgs) throws Exception {
            interceptor.onConstruct(object, allArgs);
        }

    }

    @AllArgsConstructor
    public static class MethodInter {

        private final MethodInterceptor interceptor;

        @RuntimeType
        public Object intercept(@Morph OverrideCallable callable,
                                @This Object object,
                                @Origin Method method,
                                @AllArguments Object[] allArgs) throws Exception {
            return interceptor.intercept(object, callable, allArgs, method);
        }

    }
}
