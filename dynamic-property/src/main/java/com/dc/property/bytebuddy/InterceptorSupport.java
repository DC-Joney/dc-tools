package com.dc.property.bytebuddy;

import com.dc.tools.common.spi.CommonServiceLoader;
import com.dc.tools.common.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("all")
class InterceptorSupport {

    static final Map<Class<?>, List<MethodInstanceInterceptor>> interceptorCache = new HashMap<>();

    static final Map<Class<?>, ConstructInstanceInterceptor> constructInterceptorCache = new HashMap<>();

    static {
        installInterceptors();
    }

    static void installInterceptors() {
        List<InstanceInterceptor> loadInstanceInterceptors = CommonServiceLoader.load(InstanceInterceptor.class).sort();
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        for (InstanceInterceptor instanceInterceptor : loadInstanceInterceptors) {
            try {
                String className = instanceInterceptor.interceptClass();
                Class<?> interceptClass = classLoader.loadClass(className);
                if (instanceInterceptor instanceof MethodInstanceInterceptor) {
                    interceptorCache.compute(interceptClass, (classKey, interceptors) -> {
                        if (interceptors == null) {
                            interceptors = new ArrayList<>();
                        }

                        interceptors.add((MethodInstanceInterceptor) instanceInterceptor);
                        return interceptors;
                    });
                }

                if (instanceInterceptor instanceof ConstructInstanceInterceptor) {
                    constructInterceptorCache.putIfAbsent(interceptClass, (ConstructInstanceInterceptor) instanceInterceptor);
                }
            } catch (ClassNotFoundException e) {
                throw new InterceptWrapException(e, "Cannot find intercept class for {}, please check it", instanceInterceptor.interceptClass());
            }
        }
    }


    static List<MethodInterceptorAdaptor> getMethodInterceptor(Class<?> classKey) {
        return interceptorCache.getOrDefault(classKey, Collections.singletonList(DefaultMethodInstanceInterceptor.INSTANCE))
                .stream().map(MethodInterceptorAdaptor::new).collect(Collectors.toList());
    }

    static ConstructInterceptorAdaptor getConstructInterceptor(Class<?> classKey) {
        ConstructInstanceInterceptor interceptor = constructInterceptorCache.getOrDefault(classKey, DefaultConstructInstanceInterceptor.INSTANCE);
        return new ConstructInterceptorAdaptor(interceptor);
    }

    static class DefaultMethodInstanceInterceptor implements MethodInstanceInterceptor {

        static final MethodInstanceInterceptor INSTANCE = new DefaultMethodInstanceInterceptor();

        @Override
        public Object intercept(DynamicInstance instance, OverrideCallable callable, Object[] allArguments, Method method) throws Exception {
            return callable.call(allArguments);
        }

        @Override
        public ElementMatcher.Junction<MethodDescription> methodMatcher() {
            return ElementMatchers.none();
        }

        @Override
        public String interceptClass() {
            return null;
        }
    }

    static class DefaultConstructInstanceInterceptor implements ConstructInstanceInterceptor {

        static final ConstructInstanceInterceptor INSTANCE = new DefaultConstructInstanceInterceptor();

        @Override
        public void onConstruct(Object obj, Object[] arguments) {

        }

        @Override
        public String interceptClass() {
            return null;
        }
    }


}
