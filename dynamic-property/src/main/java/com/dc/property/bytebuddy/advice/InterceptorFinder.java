package com.dc.property.bytebuddy.advice;

import com.dc.tools.common.spi.CommonServiceLoader;
import com.google.common.collect.Lists;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InterceptorFinder {

    private static final Set<ClassInterceptorDefinition> definitionCache = new HashSet<>();

    private static ElementMatcher<? super TypeDescription> allClassMatcher;

    static {
        loadAllEndpoints();
    }


    static void loadAllEndpoints() {
        List<ClassInterceptorEndpoint> interceptorEndpoints = CommonServiceLoader.load(ClassInterceptorEndpoint.class).sort();
        interceptorEndpoints.forEach(interceptorEndpoint -> definitionCache.add(interceptorEndpoint.getDefinition()));
        ElementMatcher.Junction<TypeDescription> classMatcher = ElementMatchers.none();
        for (ClassInterceptorDefinition definition : definitionCache) {
            classMatcher = classMatcher.or(definition.getClassMatcher());
        }

        allClassMatcher = ElementMatchers.cached(classMatcher, new ConcurrentHashMap<>());
    }


    public static ElementMatcher<? super TypeDescription> typeMatcher() {
        return allClassMatcher;
    }

    public static boolean contains(TypeDescription description) {
        return allClassMatcher.matches(description);
    }

    public static List<ClassAdvicePoint<?>> advicePoints(TypeDescription description) {
        List<ClassAdvicePoint<?>> advicePoints = Lists.newArrayList();
        for (ClassInterceptorDefinition point : definitionCache) {
            if (point.getClassMatcher().matches(description)) {
                advicePoints.addAll(point.getAdvicePoints());
            }
        }

        return advicePoints;
    }


    public static List<InterceptorPoint> interceptorPoints(TypeDescription description) {
        List<InterceptorPoint> interceptorPoints = Lists.newArrayList();
        for (ClassInterceptorDefinition definition : definitionCache) {
            if (definition.getClassMatcher().matches(description)) {
                interceptorPoints.addAll(definition.getInterceptorPoints());
            }
        }

        return interceptorPoints;
    }

}
