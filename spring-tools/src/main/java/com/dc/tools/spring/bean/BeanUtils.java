package com.dc.tools.spring.bean;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * {@link org.springframework.beans.annotation.AnnotationBeanUtils}
 * {@link org.springframework.beans.PropertyAccessorUtils}
 *
 * @author zhangyang
 */
@Slf4j
@UtilityClass
@SuppressWarnings("Duplicates")
public class BeanUtils extends org.springframework.beans.BeanUtils {

    @SuppressWarnings("rawtypes")
    private static <T> Constructor<T> defaultConstructor(Class<T> beanClass) {
        try {
            return beanClass.getDeclaredConstructor((Class[]) null);
        } catch (NoSuchMethodException e) {
            if (log.isDebugEnabled())
                log.debug("The beanClass dose not have default Constructor");
        }
        return null;
    }


    /**
     * org.springframework.beans.strategy.support.ConstructorResolver
     * <p>
     * {@link ConstructorArgumentValues}
     */
    public static <T, R> R copyProperties(T source, @NonNull Supplier<R> instanceSupplier, String... ignoreProperties) {
        R instance = instanceSupplier.get();
        Assert.notNull(instance, "The supplier produce instance must not be null");
        copyNotNullProperties(source, instance,false, ignoreProperties);
        return instance;
    }

    /**
     * 根据目标Class来复制属性值
     * @param source source instance
     * @param targetClass target class
     * @param ignoreProperties 忽略的属性值
     */
    public static <T, R> R copyPropertiesWithClass(T source, Class<R> targetClass, String... ignoreProperties) {
        Constructor<R> constructor = defaultConstructor(targetClass);
        Assert.notNull(constructor, "The targetClass" + targetClass + "have not default Constructor");
        R instance = org.springframework.beans.BeanUtils.instantiateClass(constructor);
        copyNotNullProperties(source, instance, false, ignoreProperties);
        return instance;
    }


    /**
     * 根据目标Class来复制属性值
     * @param source source instance
     * @param targetClass target class
     * @param ignoreProperties 忽略的属性值
     * @param callback 当复制完成后，用于后置处理
     */
    public static <T, R> R copyPropertiesWithClass(T source, Class<R> targetClass, CopyPropertyCallBack<T, R> callback, String... ignoreProperties) {
        Constructor<R> constructor = defaultConstructor(targetClass);
        Assert.notNull(constructor, "The targetClass" + targetClass + "have not default Constructor");
        R instance = BeanUtils.instantiateClass(constructor);
        copyProperties(source, instance, callback, ignoreProperties);
        return instance;
    }


    /**
     * 根据目标Class来复制属性值
     * @param sources source instances
     * @param targetClass target class
     * @param ignoreProperties 忽略的属性值
     */
    public static <T, R> List<R> copyPropertiesWithClass(List<T> sources, Class<R> targetClass, String... ignoreProperties) {
        List<R> ret = Lists.newArrayListWithCapacity(sources.size());
        sources.forEach((entity) -> ret.add(copyPropertiesWithClass(entity, targetClass, ignoreProperties)));
        return ret;
    }

    public static <T, R> List<R> copyPropertiesWithClass(List<T> sources, Class<R> targetClass, CopyPropertyCallBack<T, R> callback, String... ignoreProperties) {
        List<R> ret = Lists.newArrayListWithCapacity(sources.size());
        sources.forEach((entity) -> ret.add(copyPropertiesWithClass(entity, targetClass, callback, ignoreProperties)));
        return ret;
    }

    /**
     * 复制source实体中的属性值 到target中
     * @param source source instance
     * @param target target instance
     * @param ignoreProperties 忽略的属性值
     * @param callback 当复制完成后，用于后置处理
     */
    public static <T, R> void copyProperties(T source, R target, CopyPropertyCallBack<T, R> callback, String... ignoreProperties) throws BeansException {
        copyNotNullProperties(source, target, false, ignoreProperties);
        callback.copy(source, target);
    }

    /**
     * 复制source中非空的属性值到target中
     * @param source source instance
     * @param target target instance
     * @param ignoreProperties 忽略的属性值
     * @param callback 当复制完成后，用于后置处理
     */
    public static <T, R> void copyNotNullProperties(T source, R target, CopyPropertyCallBack<T, R> callback, String... ignoreProperties) {
        copyNotNullProperties(source, target, true, ignoreProperties);
        callback.copy(source, target);
    }

    /**
     * 复制source中非空的属性值到target中
     * @param source source instance
     * @param supplier target instance
     * @param ignoreProperties 忽略的属性值
     * @param callback 当复制完成后，用于后置处理
     */
    public static <T, R> R copyNotNullPropertiesWithSupplier(T source, Supplier<R> supplier, CopyPropertyCallBack<T, R> callback, String... ignoreProperties) {
        R target = Objects.requireNonNull(supplier.get());
        copyNotNullProperties(source, target, true, ignoreProperties);
        callback.copy(source, target);
        return target;
    }

    /**
     * 复制source中非空的属性值到target中
     * @param source source instance
     * @param supplier target instance
     * @param ignoreProperties 忽略的属性值
     */
    public static <T, R> R copyNotNullProperties(T source, Supplier<R> supplier, String... ignoreProperties) {
        R target = Objects.requireNonNull(supplier.get());
        copyNotNullProperties(source, target, true, ignoreProperties);
        return  target;
    }

    /**
     * 复制source中非空的属性值到target中
     * @param source source instance
     * @param target target instance
     * @param ignoreProperties 忽略的属性值
     * @param callback 当复制完成后，用于后置处理
     */
    public static <T, R> R copyNotNullPropertiesWithClass(T source, Class<R> targetClass, CopyPropertyCallBack<T, R> callback, String... ignoreProperties) {
        Constructor<R> constructor = defaultConstructor(targetClass);
        Assert.notNull(constructor, "The targetClass" + targetClass + "have not default Constructor");
        R instance = BeanUtils.instantiateClass(constructor);
        copyNotNullProperties(source, instance, true, ignoreProperties);
        callback.copy(source, instance);
        return instance;
    }



    /**
     * 复制source实体中的属性值 到target中
     * @param source source instance
     * @param target target instance
     * @param ignoreProperties 忽略的属性值
     * @param ignoreNull 是否需要忽略空值
     */
    public static void copyNotNullProperties(Object source, Object target, boolean ignoreNull, String... ignoreProperties) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");
        Class<?> actualEditable = target.getClass();

        PropertyDescriptor[] targetPds = BeanUtils.getPropertyDescriptors(actualEditable);

        List<String> ignoreList = ignoreProperties != null ? Arrays.asList(ignoreProperties) : null;

        for (PropertyDescriptor targetPd : targetPds) {
            Method writeMethod = targetPd.getWriteMethod();

            //字段对应的别名，如果在sourceClass找寻找不到对应的propertyDescriptor，则会寻找alias对应的readDescriptor
            String aliasName = null;
            Field writeField = ReflectionUtils.findField(actualEditable, targetPd.getName());

            //TODO 缓存相应的注解信息
            if (writeField != null) {
                Set<CopyAlias> copyAliases = AnnotationUtils.getRepeatableAnnotations(writeField, CopyAlias.class, CopyAliases.class);
                Optional<AnnotationAttributes> optionalCopyAlias = copyAliases.stream()
                        .map(copyAlias -> AnnotationUtils.getAnnotationAttributes(copyAlias,false,true))
                        .filter(copyAlias -> StringUtils.hasText(copyAlias.getString("name")))
                        .filter(copyAlias -> copyAlias.getClass("source").equals(source.getClass())
                                || Void.class.isAssignableFrom(copyAlias.getClass("source")))
                        .findFirst();

                if (optionalCopyAlias.isPresent())
                    aliasName = optionalCopyAlias.get().getString("name");

                Set<CopyIgnore> copyIgnores = AnnotationUtils.getRepeatableAnnotations(writeField, CopyIgnore.class, CopyIgnores.class);
                Optional<CopyIgnore> optionalCopyIgnore = copyIgnores.stream()
                        .filter(copyIgnore -> copyIgnore.source().equals(source.getClass()))
                        .findFirst();

                if (optionalCopyIgnore.isPresent() && optionalCopyIgnore.get().source().equals(source.getClass()))
                    continue;

                if (AnnotationUtils.getAnnotation(writeField, CopyIgnoreAll.class) != null)
                    continue;

            }

            if (writeMethod != null &&
                    (ignoreList == null || !ignoreList.contains(targetPd.getName()) || !ignoreList.contains(aliasName))) {

                PropertyDescriptor sourcePd = BeanUtils.getPropertyDescriptor(source.getClass(), targetPd.getName());

                //如果在原来的sourceClass中找不到对应的propertyDescriptor，则寻找alias对应的propertyDescriptor
                if (sourcePd == null && StringUtils.hasText(aliasName))
                    sourcePd = BeanUtils.getPropertyDescriptor(source.getClass(), aliasName);

                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null) {
                        Class<?> writeType = writeMethod.getParameterTypes()[0];
                        Class<?> readType = readMethod.getReturnType();
                        //判断读写类型是否一致
                        boolean assignable = ClassUtils.isAssignable(writeType, readType);
                        //通用转换器
                        ConversionService conversionService = DefaultConversionService.getSharedInstance();
                        if (assignable || conversionService.canConvert(readType,writeType)) {
                            try {
                                if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                    readMethod.setAccessible(true);
                                }

                                Object value = readMethod.invoke(source);
                                if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                    writeMethod.setAccessible(true);
                                }

                                //如果 writeType与readType不一致
                                if (!assignable) {
                                    value = conversionService.convert(value, writeType);
                                }

                                if (ignoreNull) {
                                    if (!Objects.isNull(value)) {
                                        writeMethod.invoke(target, value);
                                    }
                                    continue;
                                }

                                writeMethod.invoke(target, value);
                            } catch (Throwable ex) {
                                throw new FatalBeanException("Could not copy property '" + targetPd.getName() + "' from source to target", ex);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * 获取到对象中属性为null的属性名
     *
     * @param source 要拷贝的对象
     */
    public static String[] getNullPropertyNames(Object source) {
        BeanWrapper wrapper = new BeanWrapperImpl(source);
        //由Spring帮我们缓存过了，不需要每次都进行反射获取,从beanWrapper获取也是一样的
        PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(source.getClass());
        final Stream.Builder<String> builder = Stream.builder();

        //遍历propertyDescriptors并且获取所有的值
        for (PropertyDescriptor pd : propertyDescriptors) {
            Object srcValue = wrapper.getPropertyValue(pd.getName());
            if (srcValue == null) {
                builder.add(pd.getName());
            }
        }
        return builder.build().toArray(String[]::new);
    }


    /**
     * 将一个 bean 对象转为一个map对象
     *
     * @param bean 需要转为map的 bean 对象
     */
    public static Map<String, Object> beanToMap(Object bean) throws Exception {
        Map<String, Object> beanMap = new HashMap<>(16);

        //用spring缓存过的Descriptors，避免重复获取导致性能低下
        PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(bean.getClass());
        //对属性迭代
        for (PropertyDescriptor pd : descriptors) {
            //属性名称
            String propertyName = pd.getName();
            //属性值,用getter方法获取
            Method readMethod = pd.getReadMethod();

            //用对象执行getter方法获得属性值
            Object propertyValue = readMethod.invoke(bean);

            //把属性名-属性值 存到Map中
            if (propertyValue != null) {
                beanMap.put(propertyName, propertyValue);
            }
        }
        return beanMap;
    }


    /**
     * 注意:自己去校验赋值是否成功
     *
     * @param source map数据源
     * @param bean   需要复制的实际的bean信息
     */
    public static <T> T copyMapToBean(Map<?, ?> source, T bean) {
        Assert.notNull(source, "source must not be null");
        Assert.notNull(bean, "bean must not be null");
        PropertyDescriptor[] targetPds = BeanUtils.getPropertyDescriptors(bean.getClass());
        PropertyDescriptor errorPd = null;
        try {
            for (PropertyDescriptor pd : targetPds) {
                errorPd = pd;
                Object value = source.get(pd.getName());
                Method writeMethod = pd.getWriteMethod();
                if (Objects.nonNull(value) && ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], value.getClass())) {
                    if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                        writeMethod.setAccessible(true);
                    }
                    writeMethod.invoke(bean, value);
                }
            }
        } catch (Exception e) {
            if (errorPd != null) {
                log.error("BeansUtil.copyMapToBean property:{} property type:{}  error->", errorPd.getName(), errorPd.getPropertyType(), e);
            }
        }
        return bean;
    }


    /**
     * 当bean复制完成后，针对个别属性需要定制化设计，所以提供{@literal callback} 处理
     */
    public interface CopyPropertyCallBack<T, R> {

        /**
         * 将 origin 中的属性复制到 result 中
         * @param origin 原来的对象
         * @param result 需要复制的对象既要被返回的对象
         */
        void copy(T origin, R result);

    }

}
