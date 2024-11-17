package com.dc.property.bytebuddy.advice;

import com.dc.tools.common.annotaion.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.VisibilityBridgeStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

@Slf4j

public class InterceptorInitializer implements BootInitializer {


    @Override
    public void init(BootConfiguration configuration) {
        ByteBuddyAgent.install();

        AgentBuilder.Ignored ignored = createAgentBuilder(configuration)
                .with(new Listener())
                .ignore( //忽略的包，这些包不会注入
                        nameStartsWith("org.aspectj."))
                .or(nameStartsWith("org.groovy."))
                .or(nameStartsWith("com.sun."))
                .or(nameStartsWith("sun."))
                .or(nameStartsWith("jdk."))
                .or(nameStartsWith("org.springframework.asm."))
                .or(nameStartsWith("net.bytebuddy."))
                .or(ElementMatchers.isSynthetic());

        for (ElementMatcher<? super TypeDescription> ignoreMatcher : configuration.ignoreMatchers()) {
            ignored = ignored.or(ignoreMatcher);
        }

        ignored.type(InterceptorFinder.typeMatcher())
                .transform(new InterceptorTransformer())
                .installOnByteBuddyAgent();
    }

    private AgentBuilder createAgentBuilder(BootConfiguration configuration) {

        ByteBuddy byteBuddy = new ByteBuddy()
                .with(TypeValidation.of(configuration.isDebugCheck()))

//                .with(new NamingStrategy.SuffixingRandom("$interceptor"))
                .with(VisibilityBridgeStrategy.Default.ALWAYS);

        return new AgentBuilder.Default()
                .with(AgentBuilder.InjectionStrategy.UsingUnsafe.INSTANCE)
                .with(AgentBuilder.LambdaInstrumentationStrategy.DISABLED)
                .with(new AgentBuilder.TypeStrategy() {
                    @Override
                    @NonNull
                    public DynamicType.Builder<?> builder(@NonNull TypeDescription typeDescription,
                                                          @NonNull ByteBuddy byteBuddy,@NonNull ClassFileLocator classFileLocator,
                                                          @NonNull MethodNameTransformer methodNameTransformer,
                                                          @MaybeNull ClassLoader classLoader,
                                                          @MaybeNull JavaModule module,
                                                          @MaybeNull ProtectionDomain protectionDomain) {
                        return byteBuddy.rebase(typeDescription, classFileLocator);
//                        return byteBuddy.rebase(typeDescription, classFileLocator);
                    }
                })

                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(byteBuddy);

    }

    @Override
    public void destroy() {

    }

    private static class Listener implements AgentBuilder.Listener {
        @Override
        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
//            System.out.println(StrUtil.format("On onDiscovery class {}.", typeName));
        }

        @Override
        public void onTransformation(final TypeDescription typeDescription,
                                     final ClassLoader classLoader,
                                     final JavaModule module,
                                     final boolean loaded,
                                     final DynamicType dynamicType) {
            log.info("On Transformation class {}.", typeDescription.getName());
//            try {
//                dynamicType.saveIn(new File(typeDescription.getSimpleName()));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        }

        @Override
        public void onIgnored(final TypeDescription typeDescription,
                              final ClassLoader classLoader,
                              final JavaModule module,
                              final boolean loaded) {

//            System.out.println("onIgnored: " + typeDescription.getTypeName());
        }

        @Override
        public void onError(final String typeName,
                            final ClassLoader classLoader,
                            final JavaModule module,
                            final boolean loaded,
                            final Throwable throwable) {
            log.error("Enhance class " + typeName + " error.", throwable);
        }

        @Override
        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
//            System.out.println("onComplete: " + typeName);
        }
    }

}
