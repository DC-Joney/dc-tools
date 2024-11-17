package com.dc.property.bytebuddy.advice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
class ClassAdvicePoint<T> {

    private final ElementMatcher<? super MethodDescription> matcher;

    private final Class<T> adviceClass;
}
