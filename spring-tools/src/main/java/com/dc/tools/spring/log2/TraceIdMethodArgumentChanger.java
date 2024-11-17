package com.dc.tools.spring.log2;

import com.dc.tools.trace.TraceUtils;
import com.turing.common.trace.TraceUtils;

import java.util.Optional;

/**
 * 用于串联 arms 与 log2
 * <p>
 * 将arms提供的traceId绑定到log2日志中，将其串联起来
 *
 * @author zy
 */
public class TraceIdMethodArgumentChanger implements MethodArgumentChanger {

    @Override
    public MethodInvocationArguments doChange(MethodInvocationArguments invocationArguments, MethodArgumentsChain chain) {
        Optional<String> traceId = TraceUtils.dumpTrace();
        if (invocationArguments.isProxyInvocation() && traceId.isPresent()) {
            invocationArguments.addArgument(traceId.get());
            return chain.doChange(invocationArguments);
        }

        return chain.doChange(invocationArguments);

    }

}
