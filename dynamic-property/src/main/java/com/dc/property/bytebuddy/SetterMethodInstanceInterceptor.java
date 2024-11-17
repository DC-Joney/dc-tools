package com.dc.property.bytebuddy;

import com.dc.tools.common.utils.ReflectionUtils;
import com.dc.tools.spring.bean.BeanUtils;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author zy
 */
public abstract class SetterMethodInstanceInterceptor implements MethodInstanceInterceptor, ConstructInstanceInterceptor {

    @Override
    public Object intercept(DynamicInstance instance, OverrideCallable callable, Object[] allArguments, Method method) throws Exception {
        PropertyDescriptor descriptor = BeanUtils.findPropertyForMethod(method);
        PropertyListeners listeners = (PropertyListeners) instance;
        Field field;

        if (descriptor != null &&
                (field = ReflectionUtils.findField(instance.getClass(), descriptor.getName())) != null &&
                (!InterceptorUtils.isOnAnnotationChange() || AnnotatedElementUtils.isAnnotated(field, PropertyEditor.class))) {
            String fieldName = descriptor.getName();
            ReflectionUtils.makeAccessible(field);
            Object value = ReflectionUtils.getField(field, instance);
            PropertyChangeEvent changeEvent = new PropertyChangeEvent(instance, fieldName, value, allArguments[0]);
            for (PropertyChangeListener listener : listeners.getListeners()) {
                try {
                    listener.propertyChange(changeEvent);
                } catch (Exception e) {
                    // ignore exception
                }
            }
        }

        return internalInterceptor(instance, callable, allArguments, method);
    }

    @Override
    public void onConstruct(Object obj, Object[] arguments) {
        PropertyListeners listeners = (PropertyListeners) obj;
        listeners.setListeners(new CopyOnWriteArrayList<>());
        internalOnConstruct(obj, arguments);
    }

    protected void internalOnConstruct(Object obj, Object[] arguments) {

    }


    protected abstract Object internalInterceptor(DynamicInstance instance, OverrideCallable callable, Object[] allArguments, Method method);


    @Override
    public ElementMatcher.Junction<MethodDescription> methodMatcher() {
        return nameStartsWith("set").and(takesArguments(1)).and(not(isAbstract()));
    }
}
