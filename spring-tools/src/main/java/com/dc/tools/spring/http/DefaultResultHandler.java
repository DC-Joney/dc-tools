package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;

/**
 * Default result handler
 *
 * @author zhangyang
 */
public class DefaultResultHandler implements JsonResultHandler{

    @Override
    public JSONObject handleResult(JSONObject originResult,ResultHandlerChain chain) {
        return chain.handleResult(originResult);
    }
}
