package com.dc.tools.spring.validate.mvc;

import com.dc.tools.spring.validate.CheckExpressions;
import com.dc.tools.spring.validate.annotation.Check;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * Allows customizing the request before its body is read and converted into
 * an Object and also allows for processing of the resulting Object before
 * it is passed into a controller method as an @RequestBody or an HttpEntity method argument.
 *
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice
 * @see org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
 * @see org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodArgumentResolver
 *
 * @author zhangyang
 */
@ControllerAdvice
public class CheckAfterRequestAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasParameterAnnotation(Check.class);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        Check parameterAnnotation = parameter.getParameterAnnotation(Check.class);
        //获取注解相应的属性
        AnnotationAttributes annotationAttributes = AnnotationUtils.getAnnotationAttributes(parameterAnnotation, false, false);
        Class<?>[] groups = annotationAttributes.getClassArray("groups");
        //校验数据是否合法，或者是在afterRead完成后注入相应的值
        CheckExpressions.check(body, groups);
        return body;
    }
}
