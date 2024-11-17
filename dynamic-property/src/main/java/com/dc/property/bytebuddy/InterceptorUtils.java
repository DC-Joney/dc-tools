package com.dc.property.bytebuddy;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.common.utils.ClassUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.pool.TypePool;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dc.property.bytebuddy.InterceptorSupport.getConstructInterceptor;
import static com.dc.property.bytebuddy.InterceptorSupport.getMethodInterceptor;
import static net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;
import static net.bytebuddy.jar.asm.Opcodes.ACC_VOLATILE;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * 用于包装class，并且添加Property change listener
 *
 * @author zy
 */
@Slf4j
public class InterceptorUtils {

    private static final Map<Class<?>, Class<?>> classesCache = new ConcurrentHashMap<>();

    private static final TypeDescription.Generic TYPE_DEFINITIONS = TypeDescription.Generic.Builder.parameterizedType(List.class, PropertyChangeListener.class)
            .build();

    /**
     * 是否只监听添加了 {@link PropertyEditor} 注解的字段，默认为true
     */
    private static volatile boolean onAnnotationChange = true;

     static final ByteBuddy byteBuddy;

    static {

        byteBuddy = new ByteBuddy()
                .with(new AuxiliaryType.NamingStrategy.SuffixingRandom("listener"))
                .with(TypeValidation.of(false));
    }


    public static void setOnAnnotationChange(boolean onAnnotationChange) {
        InterceptorUtils.onAnnotationChange = onAnnotationChange;
    }

    public static boolean isOnAnnotationChange() {
        return onAnnotationChange;
    }

    /**
     * 将bean class进行增强
     *
     * @param unwrapClass 需要包装的class
     * @param arguments   构造器对应的参数
     */
    public static <T> T interceptInstance(Class<T> unwrapClass, Object... arguments) {
        Class<? extends T> wrappedClass  = enhanceClass(unwrapClass);
        return newInstance(wrappedClass, arguments);
    }

    /**
     * 根据传入的arguments 创建对应的实力
     *
     * @param instanceClass 实例
     * @param arguments     构造器参数
     * @return 返回新创建的instance
     */
    public static <T> T newInstance(Class<T> instanceClass, Object... arguments) {
        Constructor<? extends T>[] constructors = ReflectUtil.getConstructors(instanceClass);
        Constructor<? extends T> constructorToUse = null;
        Constructor<? extends T> matchConstructor = null;
        Class<?>[] argumentClasses = ClassUtil.getClasses(arguments);

        for (Constructor<? extends T> constructor : constructors) {
            if (constructor.getParameterCount() == arguments.length) {
                matchConstructor = constructor;
                if (ReflectUtil.getConstructor(instanceClass, argumentClasses) != null) {
                    constructorToUse = constructor;
                    break;
                }
            }
        }

        if (constructorToUse == null && matchConstructor == null) {
            throw new InterceptWrapException("Cannot resolve constructor for arguments: {}", arguments);
        }

        try {
            constructorToUse = constructorToUse == null ? matchConstructor : constructorToUse;
            return constructorToUse.newInstance(arguments);
        } catch (Exception e) {
            throw new InterceptWrapException(e, "Cannot create instance for class {} and arguments [{}]", instanceClass, arguments);

        }
    }


    /**
     * 将bean class进行增强，在调用set实现时，会自动回调 {@link PropertyChangeListener} 接口
     * <p/>
     * 返回的新class实现了{@link PropertyListeners}接口，可以通过 {@link PropertyListeners#setListeners(List)}添加
     *
     * @param unwrapClass 未被包装过的class
     * @return 返回包装过的class
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> enhanceClass(Class<T> unwrapClass) {
        classesCache.computeIfAbsent(unwrapClass, classKey -> wrapClass(unwrapClass).getLoaded());
        return (Class<? extends T>) classesCache.get(unwrapClass);
    }

    private static DynamicType.Loaded<?> wrapClass(Class<?> unwrapClass) {
        boolean setter = false;

        DynamicType.Builder<?> builder = byteBuddy.subclass(unwrapClass)
                .defineField("dynamicField$interceptor", Object.class,ACC_PRIVATE | ACC_VOLATILE)
                .implement(DynamicInstance.class)
                .intercept(FieldAccessor.ofBeanProperty())

                .method(isToString().or(isHashCode()).or(isEquals()))
                .intercept(SuperMethodCall.INSTANCE);

        ConstructInterceptorAdaptor constructInterceptor = getConstructInterceptor(unwrapClass);
        builder = builder.constructor(isConstructor())
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(getConstructInterceptor(unwrapClass))));

        if (constructInterceptor.constructInterceptor instanceof SetterMethodInstanceInterceptor) {
            setter = true;
        }

        List<MethodInterceptorAdaptor> interceptors = getMethodInterceptor(unwrapClass);
        for (MethodInterceptorAdaptor interceptor : interceptors) {
            builder = builder.method(interceptor.methodInterceptor.methodMatcher())
                    .intercept(MethodDelegation.withDefaultConfiguration()
                            .withBinders(Morph.Binder.install(OverrideCallable.class)).to(interceptor));

            if (interceptor.methodInterceptor instanceof SetterMethodInstanceInterceptor) {
                setter = true;
            }
        }

        if (setter) {
            builder = builder.defineField("listeners", TYPE_DEFINITIONS)
                    .implement(PropertyListeners.class)
                    .intercept(FieldAccessor.ofBeanProperty());
        }

        return builder.make(TypePool.Default.of(ClassUtils.getDefaultClassLoader()))
                .load(ClassUtils.getDefaultClassLoader());
    }


    public static <T> void addListener(T instance, PropertyChangeListener... listeners) {
        if (instance instanceof PropertyListeners) {
            for (PropertyChangeListener listener : listeners) {
                ((PropertyListeners) instance).getListeners().add(listener);
            }
        }
    }


    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    public static class Writer {

        @PropertyEditor
        private String name;

        private Integer age;
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
//        Writer writer = PropertyUtils.propertyChangeInstance(Writer.class, "22", null);
//
//        PropertyListeners listeners = (PropertyListeners) writer;
//        listeners.getListeners().add(evt -> System.out.println(evt.getPropertyName()));
//
//
//        writer.setAge(10);
//        writer.setName("writer");

        InterceptorUtils.setOnAnnotationChange(false);

//        SimpleAsyncTaskExecutor taskExecutor = PropertyUtils.propertyChangeInstance(SimpleAsyncTaskExecutor.class);
//        PropertyListeners listeners1 = (PropertyListeners) taskExecutor;
//        listeners1.getListeners().add(evt -> System.out.println(evt));
//
//        taskExecutor.setThreadFactory(null);


    }
}
