package com.dc.tools.spring.log2;

import com.google.common.collect.Lists;
import org.aopalliance.intercept.MethodInvocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

public class MethodArgumentsChain {


    /**
     * JsonHandler 处理器
     */
    private final MethodArgumentChanger changer;

    /**
     * handler chain
     */
    private final MethodArgumentsChain nextChain;

    public MethodArgumentsChain(Collection<MethodArgumentChanger> handlers) {
        MethodArgumentsChain chain = initChain(handlers);
        this.changer = chain.changer;
        this.nextChain = chain.nextChain;
    }

    private MethodArgumentsChain(MethodArgumentChanger changer, MethodArgumentsChain chain) {
        this.changer = changer;
        this.nextChain = chain;
    }

    private MethodArgumentsChain initChain(Collection<MethodArgumentChanger> handlers) {
        MethodArgumentsChain chain = new MethodArgumentsChain(null, null);
        ArrayList<MethodArgumentChanger> resultHandlers = Lists.newArrayList(handlers);
        ListIterator<MethodArgumentChanger> iterator = resultHandlers.listIterator(handlers.size());
        while (iterator.hasPrevious()) {
            chain = new MethodArgumentsChain(iterator.previous(), chain);
        }

        return chain;
    }


    protected MethodInvocationArguments doChange(MethodInvocation invocation) {
        MethodInvocationArguments arguments = new MethodInvocationArguments(invocation);
        return changer.doChange(arguments, this.nextChain);
    }


    public MethodInvocationArguments doChange(MethodInvocationArguments invocationArguments) {
        if (this.changer != null && this.nextChain != null)
            return changer.doChange(invocationArguments, this.nextChain);

        return invocationArguments;
    }


}
