package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

/**
 * Handler chain for result
 *
 * @author zy
 */
public class DefaultResultHandlerChain implements ResultHandlerChain {

    /**
     * JsonHandler 处理器
     */
    private final JsonResultHandler resultHandler;

    /**
     * handler chain
     */
    private final ResultHandlerChain nextChain;

    public DefaultResultHandlerChain(Collection<JsonResultHandler> handlers) {
        DefaultResultHandlerChain chain = initChain(handlers);
        this.resultHandler = chain.resultHandler;
        this.nextChain = chain.nextChain;
    }

    private DefaultResultHandlerChain(JsonResultHandler resultHandler, ResultHandlerChain chain) {
        this.resultHandler = resultHandler;
        this.nextChain = chain;
    }

    private DefaultResultHandlerChain initChain(Collection<JsonResultHandler> handlers) {
        DefaultResultHandlerChain chain = new DefaultResultHandlerChain(null, null);
        ArrayList<JsonResultHandler> resultHandlers = Lists.newArrayList(handlers);
        ListIterator<JsonResultHandler> iterator = resultHandlers.listIterator(handlers.size());
        while (iterator.hasPrevious()) {
            chain = new DefaultResultHandlerChain(iterator.previous(), chain);
        }

        return chain;
    }


    @Override
    public JSONObject handleResult(JSONObject originResult) {
        return this.resultHandler != null && this.nextChain != null
                ? resultHandler.handleResult(originResult, this.nextChain) : originResult;
    }


}
