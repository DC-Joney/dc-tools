package com.dc.property.bytebuddy.advice;


import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Collections;
import java.util.Set;

@Getter
@AllArgsConstructor
public class ClassInterceptorDefinition {

    private final ElementMatcher<TypeDescription> classMatcher;

    private final Set<ClassAdvicePoint<?>> advicePoints;

    private final Set<InterceptorPoint> interceptorPoints;

    static final ClassInterceptorDefinition INSTANCE = new ClassInterceptorDefinition(null, Collections.emptySet(), Collections.emptySet());

    public static Builder intercept(ElementMatcher<TypeDescription> classMatcher) {
        return new Builder(classMatcher);
    }

    public static final class Builder {

        private final Set<ClassAdvicePoint<?>> advicePoints = Sets.newHashSet();

        private final Set<InterceptorPoint> interceptorPoints = Sets.newHashSet();

        private final ElementMatcher<TypeDescription> classMatcher;

        private Builder(ElementMatcher<TypeDescription> classMatcher) {
            this.classMatcher = classMatcher;
        }

        public AdvicePointBuilder onAdvice(final ElementMatcher<? super MethodDescription> matcher) {
            return new AdvicePointBuilder(this, matcher);
        }

        public InterceptorEndpointBuilder onMethod(final ElementMatcher<? super MethodDescription> matcher) {
            return new InterceptorEndpointBuilder(this, matcher);
        }

        public ClassInterceptorDefinition build() {
            return new ClassInterceptorDefinition(classMatcher, advicePoints,interceptorPoints);
        }

        public static final class AdvicePointBuilder {

            private final Builder builder;

            private final ElementMatcher<? super MethodDescription> matcher;

            private Class<?> adviceClass;

            private AdvicePointBuilder(final Builder builder, final ElementMatcher<? super MethodDescription> matcher) {
                this.builder = builder;
                this.matcher = matcher;
            }

            public Builder adviceClass(final Class<?> adviceClass) {
                this.adviceClass = adviceClass;
                builder.advicePoints.add(new ClassAdvicePoint<>(matcher, adviceClass));
                return builder;
            }


        }

        public static final class InterceptorEndpointBuilder {

            private final Builder builder;

            private final ElementMatcher<? super MethodDescription> matcher;

            private Interceptor interceptor;

            private InterceptorEndpointBuilder(final Builder builder, final ElementMatcher<? super MethodDescription> matcher) {
                this.builder = builder;
                this.matcher = matcher;
            }

            public Builder interceptor(Interceptor interceptor) {
                this.interceptor = interceptor;
                builder.interceptorPoints.add(new InterceptorPoint(matcher, this.interceptor));
                return builder;
            }


        }
    }
}
