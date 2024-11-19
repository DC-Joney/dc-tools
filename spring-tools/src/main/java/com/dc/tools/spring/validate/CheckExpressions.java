package com.dc.tools.spring.validate;

import cn.hutool.core.util.StrUtil;
import com.dc.tools.spring.exception.BusinessException;
import com.dc.tools.spring.exception.ParamException;
import com.dc.tools.spring.utils.BusinessAssert;
import com.dc.tools.spring.validate.annotation.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 用于校验参数，或者是为参数注入相应的数据 11
 * <p>
 * {@code @el(root: sourceReference)}
 * {@code @el(check: com.turing.pt.common.validate.CheckUtil)}
 * <p>
 * Example： {@link ValidateExample}
 *
 * @author zhangyang
 * @see ValidateExample
 */

@Slf4j
@Component
@SuppressWarnings("Duplicates")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckExpressions {

    private static final CheckExpressions INSTANCE = new CheckExpressions();
    private static final Map<Class<?>, List<FieldMetadata>> metadataCache = new ConcurrentHashMap<>();

    private static final String COLLECTION_SELF_VARIABLE_NAME = "collection";
    private static final String PARENT_VARIABLE_NAME = "parent";

    @Autowired
    CheckExpressionEvaluator evaluator;

    public static CheckExpressions getInstance() {
        return INSTANCE;
    }


    /**
     * 校验instance中的filed 是否符合规则
     *
     * @param instance 需要被校验的instance
     * @param scope    校验instance 关注的 scope 范围
     */
    public static <T> void check(T instance, Class<?>... scope) {
        check(instance, Collections.emptyMap(), scope);
    }


    /**
     * 校验instance中的filed 是否符合规则
     *
     * @param instance 需要被校验的instance
     * @param scope    校验instance 关注的 scope 范围
     */
    public static <T> void check(T instance, boolean onlyGroup, Class<?>... scope) {
        check(instance, Collections.emptyMap(), onlyGroup, scope);
    }


    /**
     * 校验instance中的filed 是否符合规则
     *
     * @param instance  需要被校验的instance
     * @param scope     校验instance 关注的 scope 范围
     * @param variables 自定义传入的 Spring el 变量
     */
    public static <T> void check(T instance, Map<String, Object> variables, Class<?>... scope) {
        check(instance, variables, false, scope);
    }

    /**
     * 校验instance中的filed 是否符合规则
     *
     * @param instance  需要被校验的instance
     * @param scope     校验instance 关注的 scope 范围
     * @param variables 自定义传入的 Spring el 变量
     */
    public static <T> void check(T instance, Map<String, Object> variables, boolean onlyGroup, Class<?>... scope) {
        BusinessAssert.notNull(instance, "The checked instance cannot be empty");

        //如果instance为集合类型的话，则遍历集合中的所有元素进行校验
        if (ClassUtils.isAssignable(Collection.class, instance.getClass())) {
            Collection<?> collection = (Collection<?>) instance;
            Map<String, Object> collectionVariables = buildNewVariables(variables);
            //添加集合变量到el中
            collectionVariables.put(COLLECTION_SELF_VARIABLE_NAME, instance);
            collection.forEach(element -> check(element, collectionVariables, scope));
            return;
        }

        //如果instance为非集合类型则正常校验
        checkInstance(instance, variables, onlyGroup, scope);
    }


    /**
     * 校验instance中的filed 是否符合规则
     *
     * @param instance  需要被校验的instance
     * @param scope     校验instance 关注的 scope 范围
     * @param variables 自定义传入的 Spring el 变量
     */
    private static <T, P> void checkInstance(T instance, Map<String, Object> variables, boolean onlyGroup, Class<?>... scope) {

        Class<?> instanceClass = instance.getClass();

        List<FieldMetadata> metadataList = metadataCache.get(instanceClass);
        if (metadataList == null) {
            metadataList = buildMetadata(instanceClass);
        }

        Set<Class<?>> scopes = Sets.newHashSet(scope);
        //校验所有的字段
        for (FieldMetadata fieldMetadata : metadataList) {
            //被校验的field
            Field checkField = fieldMetadata.getField();
            try {

                ParamValueMeta paramMeta = null;
                Set<CheckParamMeta> checkParams = fieldMetadata.checkParams;
                if (fieldMetadata.hasParamValue()) {
                    Set<ParamValueMeta> paramValues = fieldMetadata.getParamValues();
                    for (ParamValueMeta paramValue : paramValues) {
                        //获取condition条件，判断是否需要注入
                        String conditionValue = paramValue.condition;
                        //判断当前ParamValue是否需要执行
                        boolean condition = StringUtils.isEmpty(conditionValue)
                                || getInstance().evaluator.condition(conditionValue, instance, checkField, variables);

                        boolean groupContains = Sets.intersection(paramValue.scopes, scopes).size() > 0;

                        //如果当前scopes 是包含 scopeClass的则进行校验，否则不校验
                        if (condition &&
                                ((onlyGroup && groupContains) || (!onlyGroup && (paramValue.scopes.size() == 0 || groupContains)))) {
                            paramMeta = paramValue;
                            break;
                        }
                    }
                }


                if (paramMeta != null && paramMeta.checkBefore) {
                    //处理字段对应的值
                    handleParamValue(paramMeta, instance, checkField, variables);
                    //将ParamValue对应的checkParams信息添加到被校验的checkParam中
                    checkParams.addAll(paramMeta.paramMetas);
                }


                //通过condition + message 校验field是否合法
                for (CheckParamMeta checkParam : checkParams) {
                    String condition = checkParam.condition;
                    String message = checkParam.message;
                    if (StringUtils.isEmpty(message)) {
                        message = StrUtil.format("Check param error, param is: {}", checkField.getName());
                    }

                    boolean groupContains = Sets.intersection(Sets.newHashSet(checkParam.scopes), scopes).size() > 0;

                    //判断当前CheckParam是否需要执行
                    boolean beforeCondition = StringUtils.isEmpty(checkParam.before)
                            || getInstance().evaluator.condition(checkParam.before, instance, checkField, variables);


                    //如果当前scopes 是包含 scopeClass的则进行校验，否则不校验
                    if (beforeCondition && ((onlyGroup && groupContains) || (!onlyGroup && (checkParam.scopes.size() == 0 || groupContains)))) {
                        BusinessAssert.paramIsTrue(getInstance().evaluator.condition(condition, instance, checkField, variables), message);
                    }
                }

                //通过 check method 校验field是否合法
                CheckMethodMeta checkMethod = fieldMetadata.checkMethod;
                if (checkMethod != null) {

                    boolean groupContains = Sets.intersection(checkMethod.scopes, scopes).size() > 0;

                    //如果当前scopes 是包含 scopeClass的则进行校验，否则不校验
                    if ((onlyGroup && groupContains) || (!onlyGroup && (checkMethod.scopes.size() == 0 || groupContains))) {
                        getInstance().evaluator.conditionMethod(checkMethod.method, instance, checkField, variables);
                    }
                }

                //处理field字段赋值, 这里是在check完成后才会进行注入
                if (paramMeta != null && !paramMeta.checkBefore) {
                    //处理字段对应的值
                    handleParamValue(paramMeta, instance, checkField, variables);
                }


                //如果该字段需要被级联check，那么添加parent变量，并且被nested check的对象不能为null
                if (fieldMetadata.isNested()) {
                    ReflectionUtils.makeAccessible(fieldMetadata.field);
                    //获取字段对应的值
                    Object checkInstance = ReflectionUtils.getField(fieldMetadata.getField(), instance);
                    //确定fieldMeta 是nested类型后在校验对应的checkInstance是否为null
                    if (checkInstance != null && !(fieldMetadata.isCollection() && CollectionUtils.isEmpty((Collection<?>) checkInstance))) {
                        String shortClassName = ClassUtils.getShortName(instanceClass);
                        String deCapitalize = Introspector.decapitalize(shortClassName);
                        //这里构建是因为如果传入的map是unmodified类型的这不能再进行添加
                        Map<String, Object> nestedVariables = buildNewVariables(variables);
                        //将外层instance实例以variable的方式进行接入，不建议在check表达式中以这样的方式引入，因为这会导致在单独使用时抛出异常
                        nestedVariables.put(deCapitalize, instance);
                        nestedVariables.put(PARENT_VARIABLE_NAME, instance);
                        //校验nested object
                        check(checkInstance, nestedVariables, scope);
                    }
                }

            } catch (Exception cause) {
                log.error("Check instance error, instance is: {}, field is: {}, error is: ", ClassUtils.getShortName(instance.getClass()),
                        fieldMetadata.getField().getName(), cause);

                BusinessAssert.propagateException(cause, ParamException.class);
                throw new ParamException(cause, "Check param error, field is: {}, cause is: {}", checkField.getName(), cause.getMessage());
            }

        }

    }


    private static Map<String, Object> buildNewVariables(Map<String, Object> variables) {
        return Maps.newHashMap(variables);
    }

    private static <T> void handleParamValue(ParamValueMeta paramValue, T instance, Field checkField, Map<String, Object> variables) throws IllegalAccessException {

        //在check param之前，需要判断是否有ParamValue注解，如果存在则优先赋值再进行校验
        ReflectionUtils.makeAccessible(checkField);
        Object fieldValue = ReflectionUtils.getField(checkField, instance);
        //只有在whenNull为false或者是 whenNull == true 并且 fieldValue == null是才会注入
        if (!paramValue.whenNull || StringUtils.isEmpty(fieldValue)) {
            Object value = getInstance().evaluator.getValue(paramValue.value, instance, checkField, variables);
            ReflectionUtils.setField(checkField, instance, value);
        }
    }

    /**
     * 构建instanceClass 对应的元数据信息
     *
     * @param instance 需要校验的instance实例
     */
    @SuppressWarnings("all")
    private static <T> List<FieldMetadata> buildMetadata(Class<T> instanceClass) {
        synchronized (instanceClass) {
            //这里再加一层判断是要考虑到nested check这种情况出现-double check
            if (metadataCache.get(instanceClass) == null) {
                List<FieldMetadata> metadataList = Lists.newArrayList();
                //扫描实例中所有需要被check的元数据
                addFiledMetadata(metadataList, instanceClass);
                metadataCache.putIfAbsent(instanceClass, metadataList);
            }
        }

        return metadataCache.getOrDefault(instanceClass, Collections.emptyList());
    }


    /**
     * 级联读取所有的内部需要被Check的字段，然后将其添加在元数据中
     *
     * @param metadataList  用于收集元数据的集合
     * @param instanceClass 需要被扫描的 instanceClass
     */
    private static void addFiledMetadata(List<FieldMetadata> metadataList, Class<?> instanceClass) {
        //遍历所有的Fields构建元数据信息
        ReflectionUtils.doWithFields(instanceClass, declaredField -> scanField(metadataList, instanceClass, declaredField),
                field -> field.isAnnotationPresent(CheckParam.class) || field.isAnnotationPresent(CheckParams.class)
                        || field.isAnnotationPresent(CheckMethod.class) || field.isAnnotationPresent(ParamValue.class)
                        || field.isAnnotationPresent(ParamValues.class) || field.isAnnotationPresent(NestedCheck.class)
        );

    }


    private static void scanField(List<FieldMetadata> metadataList, Class<?> instanceClass, Field declaredField) {
        FieldMetadata metadata = new FieldMetadata();
        //获取所有的CheckParam注解
        Set<CheckParam> checkParams = findParams(declaredField);
        if (!CollectionUtils.isEmpty(checkParams)) {
            metadata.addCheckParams(checkParams);
        }

        //获取CheckMethod注解
        CheckMethod checkMethod = findCheckMethod(declaredField);
        if (checkMethod != null) {
            metadata.setCheckMethod(checkMethod);
        }

        //获取ParamValue注解,并且添加其元数据信息
        Set<ParamValue> paramValues = findParamValue(declaredField);
        addParamValues(metadata, paramValues);


        //判断Field是否需要继续级联扫描
        // 如果instance为空，则不在继续向下扫描
        if (needNestedCheck(declaredField)) {
            //获取内联校验的类型
            Class<?> nestedClass = declaredField.getType();
            //如果字段为collection类型
            if (ClassUtils.isAssignable(Collection.class, declaredField.getType())) {
                metadata.setCollection(true);
            }

            //表示当前字段需要被级联check
            metadata.setNested(true);
            //如果nested check是collection集合的话，则从collection中获取第一个元素来构建元数据，否则不再构建元数据
            if (metadata.isCollection()) {
                nestedClass = ResolvableType.forField(declaredField).resolveGeneric(0);
            }

            //构建nested实例相应的元数据信息，这样可以避免同样的类被扫描多次
            buildMetadata(nestedClass);
        }

        metadata.setField(declaredField);
        metadataList.add(metadata);
    }

    private static void addParamValues(FieldMetadata metadata, Set<ParamValue> paramValues) {
        if (CollectionUtils.isEmpty(paramValues)) {
            return;
        }

        for (ParamValue paramValue : paramValues) {
            ParamValueMeta valueMeta = metadata.addParamValue(paramValue);
            for (CheckParam checkParam : paramValue.checks()) {
                if (StringUtils.isEmpty(checkParam.condition())) {
                    throw new BusinessException("CheckParam for condition value must not be empty, field is {}", metadata.field);
                }

                CheckParamMeta paramMeta = new CheckParamMeta();
                paramMeta.setBefore(checkParam.before());
                paramMeta.setCondition(checkParam.condition());
                paramMeta.setMessage(checkParam.message());
                paramMeta.setScopes(Sets.newHashSet(checkParam.scope()));
                //如果checkParam的scope为空，则默认使用paramValue的scope
                //这样可以避免在param失效时checkParam依旧生肖
                if (checkParam.scope().length == 0) {
                    paramMeta.addScope(paramValue.scope());
                }


                valueMeta.addCheckParam(paramMeta);
            }
        }
    }


    private static boolean needNestedCheck(Field declaredField) {
        return declaredField.isAnnotationPresent(NestedCheck.class) &&
                //TODO: 是否需要添加SystemClassLoader加载的class不在check的判断
                !ClassUtils.isPrimitiveOrWrapper(declaredField.getType());

    }


    private static void addCheckParamValue(FieldMetadata metadata, Set<ParamValue> paramValues, Field declaredField) {


    }


    private static Set<CheckParam> findParams(Field declaredField) {
        Set<CheckParam> checkParams = AnnotationUtils.getRepeatableAnnotations(declaredField, CheckParam.class, CheckParams.class);
        for (CheckParam checkParam : checkParams) {
            if (StringUtils.isEmpty(checkParam.condition())) {
                throw new BusinessException("CheckParam for condition value must not be empty, field is {}", declaredField);
            }
        }

        return checkParams;
    }


    private static CheckMethod findCheckMethod(Field declaredField) {
        CheckMethod annotation = AnnotationUtils.getAnnotation(declaredField, CheckMethod.class);
        if (annotation != null && StringUtils.isEmpty(annotation.method())) {
            throw new BusinessException("CheckMethod for method value must not be empty, field is {}", declaredField);
        }

        return annotation;
    }


    private static Set<ParamValue> findParamValue(Field declaredField) {
        Set<ParamValue> annotations = AnnotationUtils.getRepeatableAnnotations(declaredField, ParamValue.class);
        if (!CollectionUtils.isEmpty(annotations)) {
            annotations.forEach(paramValue -> {
                if (StringUtils.isEmpty(paramValue.value())) {
                    throw new BusinessException("ParamValue for value must not be empty, field is {}", declaredField);
                }
            });
        }

        return annotations;
    }

    @Setter
    @Getter
    @EqualsAndHashCode
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Deprecated
    private static class InstanceMetadata implements Iterable<FieldMetadata> {

        /**
         * 基于当前实例的所有Field metadata
         */
        final List<FieldMetadata> fieldMetadata = new ArrayList<>();

        /**
         * 基于当前实例的内部 nested instance metadata
         */
        final List<InstanceMetadata> metadata = new ArrayList<>();


        @Override
        public Iterator<FieldMetadata> iterator() {
            return null;
        }
    }


    /**
     * Field 元数据信息，包括需要校验的field以及需要校验的注解等
     */
    @Setter
    @Getter
    @EqualsAndHashCode
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class FieldMetadata {

        /**
         * field
         */
        Field field;

        /**
         * 需要validate的 field
         */
        Set<CheckParamMeta> checkParams = Sets.newHashSet();

        /**
         * 需要通过method 校验的 field param
         */
        CheckMethodMeta checkMethod;

        /**
         * 為對應的字段設置相應的值
         */
        Set<ParamValueMeta> paramValues = Sets.newHashSet();

        /**
         * 是否是要级联check的
         */
        boolean nested;

        /**
         * 是否是集合
         */
        boolean collection;


        public boolean hasParamValue() {
            return !CollectionUtils.isEmpty(paramValues);
        }


        /**
         * 批量添加checkParam注解
         */
        public FieldMetadata addCheckParams(Collection<CheckParam> checkParams) {
            lazyInitCheckParams();
            checkParams.forEach(this::addCheckParam);
            return this;
        }


        /**
         * 添加Param校验
         */
        public FieldMetadata addCheckParam(CheckParam checkParam) {
            lazyInitCheckParams();
            CheckParamMeta paramMeta = new CheckParamMeta();
            paramMeta.setCondition(checkParam.condition());
            paramMeta.setBefore(checkParam.before());
            paramMeta.setMessage(checkParam.message());
            paramMeta.setScopes(Sets.newHashSet(checkParam.scope()));
            this.checkParams.add(paramMeta);
            return this;
        }

        /**
         * 添加Param校验
         */
        public FieldMetadata addCheckParam(CheckParamMeta checkParam) {
            lazyInitCheckParams();
            this.checkParams.add(checkParam);
            return this;
        }

        public void setCheckMethod(CheckMethod checkMethod) {
            this.checkMethod = CheckMethodMeta.values(checkMethod.method(), Sets.newHashSet(checkMethod.scope()));
        }

        public FieldMetadata addParamValues(Set<ParamValue> paramValues) {
            lazyInitParamValues();
            paramValues.forEach(this::addParamValue);
            return this;
        }

        public ParamValueMeta addParamValue(ParamValue paramValue) {
            lazyInitParamValues();
            ParamValueMeta valueMeta = new ParamValueMeta();
            valueMeta.setValue(paramValue.value());
            valueMeta.setScopes(Sets.newHashSet(paramValue.scope()));
            valueMeta.setCheckBefore(paramValue.checkBefore());
            valueMeta.setWhenNull(paramValue.whenNull());
            valueMeta.setCondition(paramValue.condition());
            this.paramValues.add(valueMeta);
            return valueMeta;
        }


        /**
         * 初始化checkParams集合，没有必要一开始就给其赋值为HashSet类型
         */
        private void lazyInitCheckParams() {
            if (this.checkParams.equals(Collections.emptySet())) {
                this.checkParams = new LinkedHashSet<>();
            }
        }

        /**
         * 初始化paramValues集合，没有必要一开始就给其赋值为HashSet类型
         */
        private void lazyInitParamValues() {
            if (this.paramValues.equals(Collections.emptySet())) {
                this.paramValues = new LinkedHashSet<>();
            }
        }

    }

    @ToString
    @Setter
    static class CheckParamMeta {

        /**
         * 是否需要被校验
         */
        String before;

        String condition;

        String message;

        Set<Class<?>> scopes;

        public void addScope(Class<?>... scopes) {
            this.scopes.addAll(Sets.newHashSet(scopes));
        }

    }

    @Setter
    @ToString
    @AllArgsConstructor(staticName = "values")
    static class CheckMethodMeta {

        String method;

        Set<Class<?>> scopes;

    }

    @ToString
    @Setter
    static class ParamValueMeta {


        /**
         * 将Spel表达式的值设置到对应的字段中
         */
        String value;


        /**
         * 用于判断是否应该注入对应的值, 默认值为空，表示condition成立
         */
        String condition;


        /**
         * 是否当字段值为null的时候才进行注入
         * <p>
         * false: 不管字段值是否为null，都会进行覆盖
         * <p>
         * true: 当field == null时，或者field字段类型为{@linkplain String}, 并且字段值为空字符串时才会进行注入
         */
        boolean whenNull;


        /**
         * 在 check 之前注入值，还是在check完成以后再进行注入
         */
        boolean checkBefore;

        /**
         * 注入数据时注意的范围11
         */
        Set<Class<?>> scopes;


        //用于保存ParamValue绑定的checkParam信息
        Set<CheckParamMeta> paramMetas = Sets.newHashSet();

        public ParamValueMeta addCheckParam(CheckParamMeta checkParamMeta) {
            paramMetas.add(checkParamMeta);
            return this;
        }

    }


}
