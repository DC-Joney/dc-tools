package com.dc.tools.spring.log;

import com.dc.tools.spring.log.annotation.LogTrace;
import com.dc.tools.trace.TraceUtils;

/**
 * 用于针对LogTrace注解添加 globalId到参数
 *
 * @author zy
 */
public class GlobalIdMethodArgumentChanger implements MethodArgumentChanger {

    @Override
    public MethodInvocationArguments doChange(MethodInvocationArguments invocationArguments, MethodArgumentsChain chain) {
        //这里被必要包装缓存，因为AnnotationUtils本身
        if (invocationArguments.hasMethodAnnotation(LogTrace.class)
                && invocationArguments.isProxyInvocation()
                && TraceUtils.containsTraceId()) {
            //最后的位置用来存放globalId
            invocationArguments.addArgument(TraceUtils.dumpTrace());
            return chain.doChange(invocationArguments);
        }


        return chain.doChange(invocationArguments);
    }
}
