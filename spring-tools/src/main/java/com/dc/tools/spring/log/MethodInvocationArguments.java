package com.dc.tools.spring.log;

import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.spring.utils.AopTargetUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 用于包装MethodInvocation 信息
 *
 * @author zy
 * @apiNote copy from {@link org.springframework.web.method.HandlerMethod}
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MethodInvocationArguments {

    final MethodInvocation invocation;

    Object[] arguments;

    final MethodParameter[] parameters;

    final Method bridgedMethod;

    final Object bean;

    final Class<?> beanType;

    final MethodMetadata methodMetadata;

    public MethodInvocationArguments(MethodInvocation invocation) {
        this(invocation, invocation.getArguments());
    }

    public MethodInvocationArguments(MethodInvocation invocation, Object[] arguments) {
        this.invocation = invocation;
        this.bean = AopTargetUtils.getTarget(invocation.getThis());
        this.beanType = AopUtils.getTargetClass(bean);
        this.arguments = arguments;
        this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(invocation.getMethod());
        this.parameters = initMethodParameters();
        this.methodMetadata = new StandardMethodMetadata(bridgedMethod);
    }


    private MethodParameter[] initMethodParameters() {
        int count = this.bridgedMethod.getParameterCount();
        MethodParameter[] result = new MethodParameter[count];
        for (int i = 0; i < count; i++) {
            MethodInvocationParameter parameter = new MethodInvocationParameter(i);
            GenericTypeResolver.resolveParameterType(parameter, this.beanType);
            result[i] = parameter;
        }
        return result;
    }

    public Object[] originArguments() {
        if (arguments == null) {
            return invocation.getArguments();
        }

        return arguments;
    }

    public Object[] changedArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    public MethodInvocationArguments addArgument(Object argument) {
        Object[] newArguments = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, newArguments, 0, arguments.length);
        newArguments[arguments.length] = argument;
        this.arguments = newArguments;
        return this;
    }

    /**
     * Return the bean for this handler method.
     */
    public Object getBean() {
        return this.bean;
    }

    /**
     * Return the method for this handler method.
     */
    public Method getMethod() {
        return this.invocation.getMethod();
    }

    /**
     * This method returns the type of the handler for this handler method.
     * <p>Note that if the bean type is a CGLIB-generated class, the original
     * user-defined class is returned.
     */
    public Class<?> getBeanType() {
        return this.beanType;
    }

    /**
     * If the bean method is a bridge method, this method returns the bridged
     * (user-defined) method. Otherwise it returns the same method as {@link #getMethod()}.
     */
    protected Method getBridgedMethod() {
        return this.bridgedMethod;
    }

    /**
     * Return the method parameters for this handler method.
     */
    public MethodParameter[] getMethodParameters() {
        return this.parameters;
    }

    public MethodParameter getReturnTypeParameter() {
        return new MethodInvocationParameter(-1);
    }

    public Class<?> getReturnType() {
        return getReturnTypeParameter().getParameterType();
    }

    /**
     * Return {@code true} if the method return type is void, {@code false} otherwise.
     */
    public boolean isVoid() {
        return Void.TYPE.equals(getReturnTypeParameter().getParameterType());
    }

    /**
     * Return a single annotation on the underlying method traversing its super methods
     * if no annotation can be found on the given method itself.
     * <p>Also supports <em>merged</em> composed annotations with attribute
     * overrides as of Spring Framework 4.2.2.
     *
     * @param annotationType the type of annotation to introspect the method for
     * @return the annotation, or {@code null} if none found
     * @see AnnotatedElementUtils#findMergedAnnotation
     */
    @Nullable
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
        return AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), annotationType);
    }

    /**
     * Return whether the parameter is declared with the given annotation type.
     *
     * @param annotationType the annotation type to look for
     * @see AnnotatedElementUtils#hasAnnotation
     */
    public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
        return AnnotatedElementUtils.hasAnnotation(invocation.getMethod(), annotationType);
    }

    /**
     * Return method metadata information
     */
    public MethodMetadata getMethodMetadata() {
        return methodMetadata;
    }

    public MethodInvocation getInvocation() {
        return invocation;
    }

    public boolean isProxyInvocation() {
        return this.invocation instanceof ProxyMethodInvocation;
    }

    protected class MethodInvocationParameter extends SynthesizingMethodParameter {

        public MethodInvocationParameter(int index) {
            super(MethodInvocationArguments.this.bridgedMethod, index);
        }

        protected MethodInvocationParameter(MethodInvocationParameter original) {
            super(original);
        }

        @Override
        @NonNull
        public Class<?> getContainingClass() {
            return MethodInvocationArguments.this.getBeanType();
        }

        @Override
        public <T extends Annotation> T getMethodAnnotation(@NonNull Class<T> annotationType) {
            return MethodInvocationArguments.this.getMethodAnnotation(annotationType);
        }

        @Override
        public <T extends Annotation> boolean hasMethodAnnotation(@NonNull Class<T> annotationType) {
            return MethodInvocationArguments.this.hasMethodAnnotation(annotationType);
        }

        @Override
        public MethodInvocationParameter clone() {
            return new MethodInvocationParameter(this);
        }
    }


}
