package com.dc.tools.spring.validate;

import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于参数校验的Spel Evaluator
 *
 * @author zhangyang
 */
public class CheckExpressionEvaluator extends CachedExpressionEvaluator implements BeanFactoryAware {

    private static final String CHECK_FIELD_NAME = "self";
    private static final String CHECK_UTIL_NAME = "check";

    private BeanFactory beanFactory;

    /**
     * 缓存expression表达式，避免每次parse，对性能有损耗
     */
    private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

    public CheckExpressionEvaluator(SpelExpressionParser parser) {
        super(parser);
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * 创建对应的Expression表达式
     *
     * @param cache      缓存expression表达式
     * @param elementKey 缓存elementKey
     * @param expression expression 表达式
     */
    @NonNull
    protected Expression getExpression(Map<ExpressionKey, Expression> cache,
                                       @NonNull AnnotatedElementKey elementKey, @NonNull String expression) {

        ExpressionKey expressionKey = createKey(elementKey, expression);
        Expression expr = cache.get(expressionKey);
        if (expr == null) {
            //判断是否需要加前后的表达式, 如果需要加可以通过ParserContext 进行设置
            expr = getParser().parseExpression(expression);
            cache.put(expressionKey, expr);
        }
        return expr;
    }


    private ExpressionKey createKey(AnnotatedElementKey elementKey, String expression) {
        return new FieldExpressionKey(elementKey, expression);
    }


    /**
     * 为不同的instance实例生成相应的 EvaluationContext
     *
     * @param instance 需要校验的实例
     * @param field    需要校验的field
     */
    private <T> EvaluationContext createEvaluationContext(T instance, Field field, Map<String, Object> variables) throws IllegalAccessException {
        ReflectionUtils.makeAccessible(field);
        Object fieldObj = ReflectionUtils.getField(field, instance);
        FieldEvaluationContext<T> evaluationContext = new FieldEvaluationContext<>(instance, variables);
        if (beanFactory != null) {
            evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }

        evaluationContext.setVariable(CHECK_FIELD_NAME, fieldObj);
        evaluationContext.setVariable(CHECK_UTIL_NAME, CheckUtil.INSTANCE);
        return evaluationContext;
    }

    /**
     * 校验字段相应的condition，通过返回结果判断校验是否合法
     *
     * @param condition 校验条件
     * @param instance  校验实例
     * @param field     校验字段
     */
    public <T> boolean condition(String condition, T instance, Field field, Map<String, Object> variables) throws IllegalAccessException {
        AnnotatedElementKey elementKey = new AnnotatedElementKey(field, field.getClass());
        EvaluationContext evaluationContext = createEvaluationContext(instance, field, variables);
        return (Boolean.TRUE.equals(getExpression(this.conditionCache, elementKey, condition).getValue(
                evaluationContext, Boolean.class)));
    }

    /**
     * 校验字段相应的 condition，无返回字段，如果校验有错，应该在字段内部抛出相应异常
     *
     * @param condition 校验条件
     * @param instance  校验实例
     * @param field     校验字段
     */
    public <T> void conditionMethod(String condition, T instance, Field field, Map<String, Object> variables) throws IllegalAccessException {
        AnnotatedElementKey elementKey = new AnnotatedElementKey(field, field.getClass());
        EvaluationContext evaluationContext = createEvaluationContext(instance, field, variables);
        getExpression(this.conditionCache, elementKey, condition).getValue(evaluationContext);
    }


    /**
     * 获取对应的值
     *
     * @param expression 获取对应数据的表达式
     * @param instance   rootObject
     * @param field      需要被注入的字段值
     */
    public <T> Object getValue(String expression, T instance, Field field, Map<String, Object> variables) throws IllegalAccessException {
        AnnotatedElementKey elementKey = new AnnotatedElementKey(field, field.getClass());
        EvaluationContext evaluationContext = createEvaluationContext(instance, field, variables);
        return getExpression(this.conditionCache, elementKey, expression).getValue(evaluationContext, field.getType());
    }


    /**
     * 用于针对field类型的 EvaluationContext
     *
     * @see org.springframework.context.expression.MethodBasedEvaluationContext
     * @see org.springframework.cache.interceptor.CacheEvaluationContext
     */
    static class FieldEvaluationContext<T> extends StandardEvaluationContext {

        private boolean fieldsLoaded = false;

        private boolean variablesLoaded = false;

        /**
         * field对应的instance实例
         */
        private final T instance;

        /**
         * 自定义需要加载的变量
         */
        private final Map<String, Object> variables;

        public FieldEvaluationContext(T instance, Map<String, Object> variables) {
            super(instance);
            this.instance = instance;
            this.variables = variables;
        }


        @Override
        public Object lookupVariable(@NonNull String name) {
            Object variable = super.lookupVariable(name);
            if (variable != null) {
                return variable;
            }

            if (!this.fieldsLoaded) {
                lazyAllFields();
                this.fieldsLoaded = true;
                variable = super.lookupVariable(name);
            }

            if (variable == null && !variablesLoaded) {
                loadCustomVariables();
                this.variablesLoaded = true;
                variable = super.lookupVariable(name);
            }

            return variable;
        }

        private void loadCustomVariables() {
            //将自定义的变量加载到el上下文中
            super.setVariables(variables);
        }

        /**
         * Load the field information only when needed.
         */
        protected void lazyAllFields() {
            Class<?> instanceClass = instance.getClass();
            //使用ReflectionUtils类获取所有的fields，因为内部自带缓存，避免性能消耗,
            // doWithClass会获取当前类以及所有顶层父类的字段，如果不需要则可以使用 doWithLocalFields
            ReflectionUtils.doWithFields(instanceClass, declaredField -> {
                ReflectionUtils.makeAccessible(declaredField);
                Object fieldValue = ReflectionUtils.getField(declaredField, instance);
                String fieldName = declaredField.getName();
                setVariable(fieldName, fieldValue);
            });
        }
    }


    protected static class FieldExpressionKey extends ExpressionKey {
        protected FieldExpressionKey(AnnotatedElementKey element, String expression) {
            super(element, expression);
        }
    }
}
